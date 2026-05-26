// src/main/java/com/diggs/fsm/GrievanceFSM.java
package com.diggs.fsm;

import com.diggs.model.Grievance;
import com.diggs.model.Grievance.GrievanceState;
import com.diggs.model.User;
import com.diggs.model.User.UserRole;
import com.diggs.fsm.exception.InvalidStateTransitionException;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GrievanceFSM {
    
    private static final Map<GrievanceState, Set<GrievanceState>> validTransitions = new EnumMap<>(GrievanceState.class);
    private static final Map<GrievanceState, Set<UserRole>> allowedActors = new EnumMap<>(GrievanceState.class);
    
    static {
        // Initialize valid state transitions
        validTransitions.put(GrievanceState.SUBMITTED, Set.of(
            GrievanceState.UNDER_REVIEW, GrievanceState.REJECTED
        ));
        
        validTransitions.put(GrievanceState.UNDER_REVIEW, Set.of(
            GrievanceState.RESOLVED, GrievanceState.ESCALATED, GrievanceState.REJECTED
        ));
        
        validTransitions.put(GrievanceState.ESCALATED, Set.of(
            GrievanceState.UNDER_REVIEW, GrievanceState.RESOLVED
        ));
        
        validTransitions.put(GrievanceState.RESOLVED, Set.of(
            GrievanceState.CLOSED
        ));
        
        validTransitions.put(GrievanceState.REJECTED, Set.of(
            GrievanceState.CLOSED
        ));
        
        validTransitions.put(GrievanceState.CLOSED, new HashSet<>()); // Terminal state
        
        // Initialize allowed actors for each state transition
        allowedActors.put(GrievanceState.SUBMITTED, Set.of(
            UserRole.CITIZEN, UserRole.OFFICER
        ));
        
        allowedActors.put(GrievanceState.UNDER_REVIEW, Set.of(
            UserRole.OFFICER, UserRole.AUTHORITY
        ));
        
        allowedActors.put(GrievanceState.ESCALATED, Set.of(
            UserRole.AUTHORITY
        ));
        
        allowedActors.put(GrievanceState.RESOLVED, Set.of(
            UserRole.OFFICER, UserRole.AUTHORITY
        ));
        
        allowedActors.put(GrievanceState.REJECTED, Set.of(
            UserRole.OFFICER, UserRole.AUTHORITY
        ));
        
        allowedActors.put(GrievanceState.CLOSED, Set.of(
            UserRole.OFFICER, UserRole.AUTHORITY, UserRole.AUDITOR
        ));
    }
    
    /**
     * Validates if a state transition is allowed
     */
    public static boolean isValidTransition(GrievanceState currentState, GrievanceState newState) {
        Set<GrievanceState> allowed = validTransitions.get(currentState);
        return allowed != null && allowed.contains(newState);
    }
    
    /**
     * Validates if a user is allowed to perform the transition
     */
    public static boolean isAllowedActor(GrievanceState targetState, UserRole role) {
        Set<UserRole> allowed = allowedActors.get(targetState);
        return allowed != null && allowed.contains(role);
    }
    
    /**
     * Performs state transition with validation
     */
    public static void transition(Grievance grievance, GrievanceState newState, User user) 
            throws InvalidStateTransitionException {
        
        GrievanceState currentState = grievance.getCurrentState();
        
        // Check if transition is valid
        if (!isValidTransition(currentState, newState)) {
            throw new InvalidStateTransitionException(
                String.format("Invalid transition from %s to %s", currentState, newState)
            );
        }
        
        // Check if user is allowed to perform this transition
        if (!isAllowedActor(newState, user.getRole())) {
            throw new InvalidStateTransitionException(
                String.format("User with role %s is not allowed to change state to %s", 
                user.getRole(), newState)
            );
        }
        
        // Perform the transition
        grievance.setCurrentState(newState);
        grievance.setUpdatedAt(java.time.LocalDateTime.now());
        
        // Special handling for escalated state
        if (newState == GrievanceState.ESCALATED) {
            grievance.setEscalationLevel(grievance.getEscalationLevel() + 1);
        }
    }
    
    /**
     * Get all possible next states from current state
     */
    public static Set<GrievanceState> getPossibleNextStates(GrievanceState currentState) {
        return validTransitions.getOrDefault(currentState, new HashSet<>());
    }
}
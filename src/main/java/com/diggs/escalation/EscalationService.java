// src/main/java/com/diggs/escalation/EscalationService.java
package com.diggs.escalation;

import com.diggs.model.Grievance;
import com.diggs.model.Grievance.GrievanceState;
import com.diggs.fsm.GrievanceFSM;
import com.diggs.fsm.exception.InvalidStateTransitionException;

public class EscalationService {
    
    private static final int MAX_ESCALATION_LEVEL = 3;
    
    /**
     * Escalate a grievance to next level
     */
    public static void escalateGrievance(Grievance grievance) {
        if (grievance.getEscalationLevel() >= MAX_ESCALATION_LEVEL) {
            // Cannot escalate further, mark for special handling
            grievance.setSlaViolated(true);
            return;
        }
        
        // Increase escalation level
        grievance.setEscalationLevel(grievance.getEscalationLevel() + 1);
        
        // Change state to ESCALATED if not already
        if (grievance.getCurrentState() != GrievanceState.ESCALATED) {
            try {
                // Note: In real implementation, we'd need a system user for auto-escalation
                // This is simplified for the example
                grievance.setCurrentState(GrievanceState.ESCALATED);
            } catch (Exception e) {
                System.err.println("Failed to escalate grievance: " + e.getMessage());
            }
        }
        
        // Assign new SLA for escalated state
        com.diggs.sla.SLAManager.assignSLADeadline(grievance);
    }
    
    /**
     * Check if grievance needs escalation
     */
    public static boolean needsEscalation(Grievance grievance) {
        // Check if SLA violated and not already escalated
        return grievance.isSlaViolated() && 
               grievance.getCurrentState() != GrievanceState.ESCALATED &&
               grievance.getCurrentState() != GrievanceState.CLOSED;
    }
    
    /**
     * Handle escalated grievance (called by Authority)
     */
    public static void handleEscalatedGrievance(Grievance grievance, String authorityId) {
        grievance.setAssignedTo(authorityId);
        // Reset SLA violation flag
        grievance.setSlaViolated(false);
        // Assign new SLA
        com.diggs.sla.SLAManager.assignSLADeadline(grievance);
    }
    
    /**
     * Get escalation level description
     */
    public static String getEscalationLevelDescription(int level) {
        switch(level) {
            case 0: return "Normal Processing";
            case 1: return "First Level Escalation - Section Officer";
            case 2: return "Second Level Escalation - Department Head";
            case 3: return "Third Level Escalation - Director";
            default: return "Critical - Special Handling Required";
        }
    }
}
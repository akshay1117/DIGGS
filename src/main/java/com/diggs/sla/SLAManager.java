// src/main/java/com/diggs/sla/SLAManager.java
package com.diggs.sla;

import com.diggs.model.Grievance;
import com.diggs.model.Grievance.GrievanceState;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

public class SLAManager {
    
    private static final Map<GrievanceState, Integer> slaTimeLimits = new EnumMap<>(GrievanceState.class);
    
    static {
        // Time limits in hours
        slaTimeLimits.put(GrievanceState.SUBMITTED, 24);      // 24 hours to review
        slaTimeLimits.put(GrievanceState.UNDER_REVIEW, 48);   // 48 hours to resolve/escalate
        slaTimeLimits.put(GrievanceState.ESCALATED, 24);      // 24 hours for authority to act
        slaTimeLimits.put(GrievanceState.RESOLVED, 72);       // 72 hours for citizen to close
        slaTimeLimits.put(GrievanceState.REJECTED, 72);       // 72 hours for appeal
        slaTimeLimits.put(GrievanceState.CLOSED, 0);          // No SLA for closed
    }
    
    /**
     * Assign SLA deadline for a grievance based on its current state
     */
    public static LocalDateTime assignSLADeadline(Grievance grievance) {
        GrievanceState state = grievance.getCurrentState();
        Integer timeLimitHours = slaTimeLimits.get(state);
        
        if (timeLimitHours != null && timeLimitHours > 0) {
            LocalDateTime deadline = LocalDateTime.now().plusHours(timeLimitHours);
            grievance.setSlaDeadline(deadline);
            return deadline;
        }
        
        grievance.setSlaDeadline(null);
        return null;
    }
    
    /**
     * Check if SLA is violated for a grievance
     */
    public static boolean isSLAViolated(Grievance grievance) {
        LocalDateTime deadline = grievance.getSlaDeadline();
        
        if (deadline == null) {
            return false;
        }
        
        boolean violated = LocalDateTime.now().isAfter(deadline);
        grievance.setSlaViolated(violated);
        
        return violated;
    }
    
    /**
     * Get remaining time for SLA compliance
     */
    public static Duration getRemainingTime(Grievance grievance) {
        LocalDateTime deadline = grievance.getSlaDeadline();
        
        if (deadline == null) {
            return null;
        }
        
        return Duration.between(LocalDateTime.now(), deadline);
    }
    
    /**
     * Get time limit for a specific state
     */
    public static Integer getTimeLimitForState(GrievanceState state) {
        return slaTimeLimits.get(state);
    }
    
    /**
     * Monitor all grievances for SLA violations
     * This would be called by a scheduled service
     */
    public static void monitorSLAs(java.util.List<Grievance> grievances) {
        for (Grievance grievance : grievances) {
            if (grievance.getCurrentState() != GrievanceState.CLOSED) {
                boolean violated = isSLAViolated(grievance);
                if (violated) {
                    // Trigger escalation if needed
                    triggerEscalationIfNeeded(grievance);
                }
            }
        }
    }
    
    private static void triggerEscalationIfNeeded(Grievance grievance) {
        if (grievance.getCurrentState() == GrievanceState.UNDER_REVIEW && 
            !grievance.isSlaViolated()) {
            // Mark for escalation
            grievance.setSlaViolated(true);
        }
    }
}
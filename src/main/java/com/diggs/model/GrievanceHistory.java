// src/main/java/com/diggs/model/GrievanceHistory.java
package com.diggs.model;

import java.time.LocalDateTime;

public class GrievanceHistory {
    private String historyId;
    private String grievanceId;
    private String previousState;
    private String newState;
    private String changedBy;
    private String changedByRole;
    private LocalDateTime changedAt;
    private String comments;
    private String action;
    
    public GrievanceHistory(String grievanceId, String previousState, 
                           String newState, String changedBy, String changedByRole, String action) {
        this.historyId = "HIST-" + System.currentTimeMillis();
        this.grievanceId = grievanceId;
        this.previousState = previousState;
        this.newState = newState;
        this.changedBy = changedBy;
        this.changedByRole = changedByRole;
        this.changedAt = LocalDateTime.now();
        this.action = action;
    }
    
    // Getters and Setters
    public String getHistoryId() { return historyId; }
    public void setHistoryId(String historyId) { this.historyId = historyId; }
    
    public String getGrievanceId() { return grievanceId; }
    public void setGrievanceId(String grievanceId) { this.grievanceId = grievanceId; }
    
    public String getPreviousState() { return previousState; }
    public void setPreviousState(String previousState) { this.previousState = previousState; }
    
    public String getNewState() { return newState; }
    public void setNewState(String newState) { this.newState = newState; }
    
    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }
    
    public String getChangedByRole() { return changedByRole; }
    public void setChangedByRole(String changedByRole) { this.changedByRole = changedByRole; }
    
    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
    
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}
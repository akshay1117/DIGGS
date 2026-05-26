// src/main/java/com/diggs/model/Grievance.java
package com.diggs.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Grievance {
    private String grievanceId;
    private String title;
    private String description;
    private String category;
    private String department;
    private String submittedBy;
    private String assignedTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private GrievanceState currentState;
    private List<GrievanceHistory> history;
    private List<String> attachments;
    private int escalationLevel;
    private boolean slaViolated;
    private LocalDateTime slaDeadline;
    
    public enum GrievanceState {
        SUBMITTED,
        UNDER_REVIEW,
        RESOLVED,
        ESCALATED,
        REJECTED,
        CLOSED
    }
    
    public Grievance() {
        this.grievanceId = "GRV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.currentState = GrievanceState.SUBMITTED;
        this.history = new ArrayList<>();
        this.attachments = new ArrayList<>();
        this.escalationLevel = 0;
        this.slaViolated = false;
    }
    
    // Getters and Setters
    public String getGrievanceId() { return grievanceId; }
    public void setGrievanceId(String grievanceId) { this.grievanceId = grievanceId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }
    
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public GrievanceState getCurrentState() { return currentState; }
    public void setCurrentState(GrievanceState currentState) { this.currentState = currentState; }
    
    public List<GrievanceHistory> getHistory() { return history; }
    public void setHistory(List<GrievanceHistory> history) { this.history = history; }
    
    public List<String> getAttachments() { return attachments; }
    public void setAttachments(List<String> attachments) { this.attachments = attachments; }
    
    public int getEscalationLevel() { return escalationLevel; }
    public void setEscalationLevel(int escalationLevel) { this.escalationLevel = escalationLevel; }
    
    public boolean isSlaViolated() { return slaViolated; }
    public void setSlaViolated(boolean slaViolated) { this.slaViolated = slaViolated; }
    
    public LocalDateTime getSlaDeadline() { return slaDeadline; }
    public void setSlaDeadline(LocalDateTime slaDeadline) { this.slaDeadline = slaDeadline; }
    
    public void addHistory(GrievanceHistory historyEntry) {
        this.history.add(historyEntry);
        this.updatedAt = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return "Grievance{" +
                "grievanceId='" + grievanceId + '\'' +
                ", title='" + title + '\'' +
                ", currentState=" + currentState +
                ", submittedBy='" + submittedBy + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
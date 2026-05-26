// src/main/java/com/diggs/model/Authority.java
package com.diggs.model;

import java.util.ArrayList;
import java.util.List;

public class Authority extends User {
    private String authorityLevel;
    private String jurisdiction;
    private String department;
    private List<String> escalatedGrievances;
    
    public Authority() {
        setRole(UserRole.AUTHORITY);
        this.escalatedGrievances = new ArrayList<>();
    }
    
    public String getAuthorityLevel() { return authorityLevel; }
    public void setAuthorityLevel(String authorityLevel) { this.authorityLevel = authorityLevel; }
    
    public String getJurisdiction() { return jurisdiction; }
    public void setJurisdiction(String jurisdiction) { this.jurisdiction = jurisdiction; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public List<String> getEscalatedGrievances() { return escalatedGrievances; }
    public void setEscalatedGrievances(List<String> escalatedGrievances) { this.escalatedGrievances = escalatedGrievances; }
    
    public void addEscalatedGrievance(String grievanceId) {
        this.escalatedGrievances.add(grievanceId);
    }
}
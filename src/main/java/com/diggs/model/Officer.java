// src/main/java/com/diggs/model/Officer.java
package com.diggs.model;

import java.util.ArrayList;
import java.util.List;

public class Officer extends User {
    private String department;
    private String designation;
    private String employeeId;
    private List<String> assignedGrievances;
    private List<String> resolvedGrievances;
    
    public Officer() {
        setRole(UserRole.OFFICER);
        this.assignedGrievances = new ArrayList<>();
        this.resolvedGrievances = new ArrayList<>();
    }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    
    public List<String> getAssignedGrievances() { return assignedGrievances; }
    public void setAssignedGrievances(List<String> assignedGrievances) { this.assignedGrievances = assignedGrievances; }
    
    public List<String> getResolvedGrievances() { return resolvedGrievances; }
    public void setResolvedGrievances(List<String> resolvedGrievances) { this.resolvedGrievances = resolvedGrievances; }
    
    public void assignGrievance(String grievanceId) {
        this.assignedGrievances.add(grievanceId);
    }
    
    public void resolveGrievance(String grievanceId) {
        this.assignedGrievances.remove(grievanceId);
        this.resolvedGrievances.add(grievanceId);
    }
}
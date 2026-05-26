// src/main/java/com/diggs/model/Auditor.java
package com.diggs.model;

public class Auditor extends User {
    private String auditId;
    private String department;
    
    public Auditor() {
        setRole(UserRole.AUDITOR);
    }
    
    public String getAuditId() { return auditId; }
    public void setAuditId(String auditId) { this.auditId = auditId; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
}
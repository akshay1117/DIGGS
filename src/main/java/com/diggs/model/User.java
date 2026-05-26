// src/main/java/com/diggs/model/User.java
package com.diggs.model;

import java.time.LocalDateTime;
import java.util.UUID;

public abstract class User {
    private String userId;
    private String email;
    private String phoneNumber;
    private String passwordHash;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private boolean isActive;
    private UserRole role;

    public enum UserRole {
        CITIZEN,
        OFFICER,
        AUTHORITY,
        AUDITOR
    }

    public User() {
        this.userId = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.isActive = true;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    
    private String firstName;
    private String lastName;
    private String status;

    public String getName() { return (firstName != null ? firstName : "") + (lastName != null && !lastName.isEmpty() ? " " + lastName : ""); }
    public void setName(String name) { 
        if (name != null) {
            String[] parts = name.trim().split(" ", 2);
            this.firstName = parts[0];
            this.lastName = parts.length > 1 ? parts[1] : "";
        }
    }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    
    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", email='" + email + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", name='" + name + '\'' +
                ", role=" + role +
                '}';
    }
}
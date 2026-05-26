// src/main/java/com/diggs/model/Citizen.java
package com.diggs.model;

import java.util.ArrayList;
import java.util.List;

public class Citizen extends User {
    private String address;
    private String city;
    private String state;
    private String pincode;
    private List<String> grievanceIds;
    
    public Citizen() {
        setRole(UserRole.CITIZEN);
        this.grievanceIds = new ArrayList<>();
    }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    
    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    
    public List<String> getGrievanceIds() { return grievanceIds; }
    public void setGrievanceIds(List<String> grievanceIds) { this.grievanceIds = grievanceIds; }
    
    public void addGrievanceId(String grievanceId) {
        this.grievanceIds.add(grievanceId);
    }
}
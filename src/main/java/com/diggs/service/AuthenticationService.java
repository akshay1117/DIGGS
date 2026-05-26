package com.diggs.service;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

import com.diggs.audit.AuditTrail;
import com.diggs.dao.MySQLDatabaseService;
import com.diggs.model.Citizen;
import com.diggs.model.User;

public class AuthenticationService {
    private static User currentUser = null;

    public static User login(String username, String password) throws Exception {
        if (username == null || username.trim().isEmpty())
            throw new Exception("Username cannot be empty");
        if (password == null || password.trim().isEmpty())
            throw new Exception("Password cannot be empty");
        if (!MySQLDatabaseService.isConnected())
            MySQLDatabaseService.connect();

        User user = MySQLDatabaseService.findUserByEmail(username);
        if (user == null)
            user = MySQLDatabaseService.findUserByPhone(username);
        if (user == null)
            throw new Exception("User not found with username: " + username);

        if (!user.isActive() || user.getStatus() == null || !"ACTIVE".equalsIgnoreCase(user.getStatus().trim())) {
            AuditTrail.recordAction("LOGIN_FAILED", user, user.getUserId(), "USER",
                    "Attempted login while account inactive or soft-banned");
            throw new Exception("This account is not active. Please contact support.");
        }

        // Plain text comparison
        if (!user.getPasswordHash().equals(password)) {
            AuditTrail.recordAction("LOGIN_FAILED", user, user.getUserId(), "USER", "Failed login attempt");
            throw new Exception("Invalid password");
        }

        user.setLastLogin(LocalDateTime.now());
        MySQLDatabaseService.updateUserLastLogin(user.getUserId());
        currentUser = user;
        AuditTrail.recordAction("LOGIN_SUCCESS", user, user.getUserId(), "USER", "Successful login");
        return user;
    }

    public static Citizen registerCitizen(String email, String phone, String password, String firstName,
            String lastName,
            String address, String city, String state, String pincode) throws Exception {
        validateEmail(email);
        validatePhone(phone);
        validatePassword(password);
        if (!MySQLDatabaseService.isConnected())
            MySQLDatabaseService.connect();

        if (MySQLDatabaseService.findUserByEmail(email) != null)
            throw new Exception("Email already registered");
        if (MySQLDatabaseService.findUserByPhone(phone) != null)
            throw new Exception("Phone number already registered");

        Citizen citizen = new Citizen();
        citizen.setUserId("CIT-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        citizen.setRole(User.UserRole.CITIZEN);
        citizen.setCreatedAt(java.time.LocalDateTime.now());
        citizen.setEmail(email);
        citizen.setPhoneNumber(phone);
        citizen.setPasswordHash(password); // plain text
        // Set name to firstName
        citizen.setFirstName(firstName);
        citizen.setLastName(lastName);
        citizen.setAddress(address);
        citizen.setCity(city);
        citizen.setState(state);
        citizen.setPincode(pincode);

        if (MySQLDatabaseService.saveUser(citizen))
            return citizen;
        else
            throw new Exception("Failed to register user");
    }

    public static boolean resetPassword(String email, String newPassword, String confirmPassword) throws Exception {
        if (!MySQLDatabaseService.isConnected())
            MySQLDatabaseService.connect();
        User user = MySQLDatabaseService.findUserByEmail(email);
        if (user == null)
            throw new Exception("User not found");
        if (!newPassword.equals(confirmPassword))
            throw new Exception("Passwords do not match");
        validatePassword(newPassword);

        user.setPasswordHash(newPassword);
        if (!MySQLDatabaseService.updateUserPassword(email, newPassword)) {
            throw new Exception("Failed to update password in database");
        }

        AuditTrail.recordAction("PASSWORD_RESET", user, user.getUserId(), "USER", "Password reset successful");
        return true;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static void logout() {
        if (currentUser != null) {
            AuditTrail.recordAction("LOGOUT", currentUser, currentUser.getUserId(), "USER", "User logged out");
            currentUser = null;
        }
    }

    private static void validateEmail(String email) throws Exception {
        if (!Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$").matcher(email).matches())
            throw new Exception("Invalid email format");
    }

    private static void validatePhone(String phone) throws Exception {
        if (!Pattern.compile("^\\d{10}$").matcher(phone).matches())
            throw new Exception("Phone number must be 10 digits");
    }

    private static void validatePassword(String password) throws Exception {
        if (password.length() < 8)
            throw new Exception("Password must be at least 8 characters");
    }
}
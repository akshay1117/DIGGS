// src/main/java/com/diggs/notification/NotificationService.java
package com.diggs.notification;

import com.diggs.dao.MySQLDatabaseService;
import com.diggs.model.User;
import com.diggs.model.Grievance;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NotificationService {

    private static final Map<String, List<Notification>> userNotifications = new ConcurrentHashMap<>();

    public static class Notification {
        private final String notificationId;
        private final String userId;
        private final String title;
        private final String message;
        private final NotificationType type;
        private final LocalDateTime timestamp;
        private boolean isRead;
        private String relatedGrievanceId;

        public enum NotificationType {
            GRIEVANCE_UPDATE,
            SLA_WARNING,
            ESCALATION,
            ASSIGNMENT,
            RESOLUTION,
            SYSTEM_ALERT
        }

        public Notification(String userId, String title, String message,
                NotificationType type, String relatedGrievanceId) {
            this.notificationId = "NOTIF-" + System.currentTimeMillis() + "-" + userId.hashCode();
            this.userId = userId;
            this.title = title;
            this.message = message;
            this.type = type;
            this.timestamp = LocalDateTime.now();
            this.isRead = false;
            this.relatedGrievanceId = relatedGrievanceId;
        }

        public Notification(String notificationId, String userId, String title, String message,
                NotificationType type, LocalDateTime timestamp, boolean isRead, String relatedGrievanceId) {
            this.notificationId = notificationId;
            this.userId = userId;
            this.title = title;
            this.message = message;
            this.type = type;
            this.timestamp = timestamp;
            this.isRead = isRead;
            this.relatedGrievanceId = relatedGrievanceId;
        }

        // Getters
        public String getNotificationId() {
            return notificationId;
        }

        public String getUserId() {
            return userId;
        }

        public String getTitle() {
            return title;
        }

        public String getMessage() {
            return message;
        }

        public NotificationType getType() {
            return type;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public boolean isRead() {
            return isRead;
        }

        public void setRead(boolean read) {
            isRead = read;
        }

        public String getRelatedGrievanceId() {
            return relatedGrievanceId;
        }
    }

    /**
     * Send notification to a user
     */
    public static void sendNotification(String userId, String title, String message,
            Notification.NotificationType type, String grievanceId) {
        Notification notif = new Notification(userId, title, message, type, grievanceId);

        userNotifications.computeIfAbsent(userId, k -> new ArrayList<>()).add(notif);
        MySQLDatabaseService.saveNotification(notif);

        // In real app, also send email/SMS
        sendEmailNotification(userId, title, message);
        sendSMSNotification(userId, message);

        System.out.println("NOTIFICATION sent to " + userId + ": " + title);
    }

    /**
     * Send notification about grievance update
     */
    public static void notifyGrievanceUpdate(User user, Grievance grievance, String updateType) {
        String title = "Grievance " + updateType;
        String message = String.format("Your grievance %s has been %s",
                grievance.getGrievanceId(), updateType.toLowerCase());

        sendNotification(user.getUserId(), title, message,
                Notification.NotificationType.GRIEVANCE_UPDATE, grievance.getGrievanceId());
    }

    /**
     * Send SLA warning notification
     */
    public static void notifySLAWarning(User officer, Grievance grievance) {
        String title = "SLA Warning";
        String message = String.format("Grievance %s is approaching SLA deadline",
                grievance.getGrievanceId());

        sendNotification(officer.getUserId(), title, message,
                Notification.NotificationType.SLA_WARNING, grievance.getGrievanceId());
    }

    /**
     * Send escalation notification
     */
    public static void notifyEscalation(User authority, Grievance grievance, int level) {
        String title = "Grievance Escalated";
        String message = String.format("Grievance %s has been escalated to level %d",
                grievance.getGrievanceId(), level);

        sendNotification(authority.getUserId(), title, message,
                Notification.NotificationType.ESCALATION, grievance.getGrievanceId());
    }

    /**
     * Get unread notifications for a user
     */
    public static List<Notification> getUnreadNotifications(String userId) {
        try {
            return MySQLDatabaseService.getNotificationsForUser(userId).stream()
                    .filter(n -> !n.isRead())
                    .toList();
        } catch (Exception e) {
            List<Notification> userNotifs = userNotifications.getOrDefault(userId, new ArrayList<>());
            return userNotifs.stream()
                    .filter(n -> !n.isRead())
                    .toList();
        }
    }

    /**
     * Get all notifications for a user
     */
    public static List<Notification> getAllNotifications(String userId) {
        try {
            return MySQLDatabaseService.getNotificationsForUser(userId);
        } catch (Exception e) {
            return userNotifications.getOrDefault(userId, new ArrayList<>());
        }
    }

    /**
     * Mark notification as read
     */
    public static void markAsRead(String userId, String notificationId) {
        List<Notification> userNotifs = userNotifications.get(userId);
        if (userNotifs != null) {
            userNotifs.stream()
                    .filter(n -> n.getNotificationId().equals(notificationId))
                    .findFirst()
                    .ifPresent(n -> n.setRead(true));
        }
        MySQLDatabaseService.markNotificationAsRead(notificationId);
    }

    /**
     * Mark all notifications as read for a user
     */
    public static void markAllAsRead(String userId) {
        List<Notification> userNotifs = userNotifications.get(userId);
        if (userNotifs != null) {
            userNotifs.forEach(n -> {
                n.setRead(true);
                MySQLDatabaseService.markNotificationAsRead(n.getNotificationId());
            });
        }
    }

    // Mock methods for email/SMS (in real app, integrate with services)
    private static void sendEmailNotification(String userId, String title, String message) {
        // Integrate with email service
        System.out.println("EMAIL to " + userId + ": " + title + " - " + message);
    }

    private static void sendSMSNotification(String userId, String message) {
        // Integrate with SMS service
        System.out.println("SMS to " + userId + ": " + message);
    }
}
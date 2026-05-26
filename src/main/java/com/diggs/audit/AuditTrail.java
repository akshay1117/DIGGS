// src/main/java/com/diggs/audit/AuditTrail.java
package com.diggs.audit;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.diggs.dao.MySQLDatabaseService;
import com.diggs.model.User;

public class AuditTrail {

    private static final ConcurrentLinkedQueue<AuditEntry> auditLog = new ConcurrentLinkedQueue<>();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static class AuditEntry {
        private final String logId;
        private final String action;
        private final String actor;
        private final String actorRole;
        private final String targetId;
        private final String targetType;
        private final String details;
        private final LocalDateTime timestamp;
        private final String ipAddress;

        public AuditEntry(String action, String actor, String actorRole,
                String targetId, String targetType, String details) {
            this.logId = "AUDIT-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 1000);
            this.action = action;
            this.actor = actor;
            this.actorRole = actorRole;
            this.targetId = targetId;
            this.targetType = targetType;
            this.details = details;
            this.timestamp = LocalDateTime.now();
            this.ipAddress = "127.0.0.1"; // In real app, get from request
        }

        // Constructor for creating from database
        public AuditEntry(String logId, String action, String actor, String actorRole,
                String targetId, String targetType, String details,
                LocalDateTime timestamp, String ipAddress) {
            this.logId = logId;
            this.action = action;
            this.actor = actor;
            this.actorRole = actorRole;
            this.targetId = targetId;
            this.targetType = targetType;
            this.details = details;
            this.timestamp = timestamp;
            this.ipAddress = ipAddress;
        }

        // Getters
        public String getLogId() {
            return logId;
        }

        public String getAction() {
            return action;
        }

        public String getActor() {
            return actor;
        }

        public String getActorRole() {
            return actorRole;
        }

        public String getTargetId() {
            return targetId;
        }

        public String getTargetType() {
            return targetType;
        }

        public String getDetails() {
            return details;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s by %s (%s) on %s: %s",
                    timestamp.format(formatter), action, actor, actorRole, targetId, details);
        }
    }

    /**
     * Record an action in the audit trail
     */
    public static void recordAction(String action, User user, String targetId,
            String targetType, String details) {
        if (user == null) {
            // Handle system actions without a user
            AuditEntry entry = new AuditEntry(
                    action,
                    "SYSTEM",
                    "SYSTEM",
                    targetId,
                    targetType,
                    details);
            auditLog.offer(entry);
            System.out.println("AUDIT: " + entry);
            MySQLDatabaseService.saveAuditLog(entry);
            return;
        }

        AuditEntry entry = new AuditEntry(
                action,
                user.getUserId(),
                user.getRole().toString(),
                targetId,
                targetType,
                details);
        auditLog.offer(entry);
        MySQLDatabaseService.saveAuditLog(entry);

        // For demonstration, print to console
        System.out.println("AUDIT: " + entry);
    }

    /**
     * Record an action with specific actor (for system actions)
     */
    public static void recordSystemAction(String action, String actor, String actorRole,
            String targetId, String targetType, String details) {
        AuditEntry entry = new AuditEntry(
                action,
                actor,
                actorRole,
                targetId,
                targetType,
                details);
        auditLog.offer(entry);
        MySQLDatabaseService.saveAuditLog(entry);
        System.out.println("AUDIT: " + entry);
    }

    /**
     * View audit trail with filters
     */
    public static List<AuditEntry> viewAuditTrail(String actorId, String targetId,
            LocalDateTime from, LocalDateTime to) {
        List<AuditEntry> filtered = new ArrayList<>();

        for (AuditEntry entry : auditLog) {
            boolean matches = true;

            if (actorId != null && !actorId.isEmpty() && !actorId.equals(entry.getActor())) {
                matches = false;
            }
            if (targetId != null && !targetId.isEmpty() && !targetId.equals(entry.getTargetId())) {
                matches = false;
            }
            if (from != null && entry.getTimestamp().isBefore(from)) {
                matches = false;
            }
            if (to != null && entry.getTimestamp().isAfter(to)) {
                matches = false;
            }

            if (matches) {
                filtered.add(entry);
            }
        }

        return filtered;
    }

    /**
     * Get all audit entries
     */
    public static List<AuditEntry> getAllAuditEntries() {
        return new ArrayList<>(auditLog);
    }

    /**
     * Get audit entries by actor
     */
    public static List<AuditEntry> getAuditEntriesByActor(String actorId) {
        return viewAuditTrail(actorId, null, null, null);
    }

    /**
     * Get audit entries by target
     */
    public static List<AuditEntry> getAuditEntriesByTarget(String targetId) {
        return viewAuditTrail(null, targetId, null, null);
    }

    /**
     * Get audit entries by date range
     */
    public static List<AuditEntry> getAuditEntriesByDateRange(LocalDateTime from, LocalDateTime to) {
        return viewAuditTrail(null, null, from, to);
    }

    /**
     * Get audit entries by action type
     */
    public static List<AuditEntry> getAuditEntriesByAction(String action) {
        List<AuditEntry> filtered = new ArrayList<>();

        for (AuditEntry entry : auditLog) {
            if (entry.getAction().equalsIgnoreCase(action)) {
                filtered.add(entry);
            }
        }

        return filtered;
    }

    /**
     * Generate compliance report
     */
    public static String generateComplianceReport(LocalDateTime startDate, LocalDateTime endDate) {
        List<AuditEntry> entries = viewAuditTrail(null, null, startDate, endDate);

        StringBuilder report = new StringBuilder();
        report.append("╔════════════════════════════════════════════════════════════════╗\n");
        report.append("║                    COMPLIANCE REPORT                          ║\n");
        report.append("╠════════════════════════════════════════════════════════════════╣\n");
        report.append("║ Period: ")
                .append(padRight(startDate.format(formatter) + " to " + endDate.format(formatter), 48)).append("║\n");
        report.append("║ Total Actions: ").append(padRight(String.valueOf(entries.size()), 45)).append("║\n");
        report.append("╚════════════════════════════════════════════════════════════════╝\n\n");

        if (entries.isEmpty()) {
            report.append("No audit entries found for the specified period.\n");
            return report.toString();
        }

        // Group by action type
        long grievanceActions = entries.stream()
                .filter(e -> "GRIEVANCE".equals(e.getTargetType()) ||
                        (e.getDetails() != null && e.getDetails().contains("grievance")))
                .count();

        long userActions = entries.stream()
                .filter(e -> "USER".equals(e.getTargetType()) ||
                        (e.getDetails() != null && e.getDetails().contains("user")))
                .count();

        long systemActions = entries.stream()
                .filter(e -> "SYSTEM".equals(e.getTargetType()) ||
                        (e.getDetails() != null && e.getDetails().contains("system")))
                .count();

        long loginActions = entries.stream()
                .filter(e -> e.getAction().contains("LOGIN") || e.getAction().contains("LOGOUT"))
                .count();

        report.append("📊 SUMMARY STATISTICS\n");
        report.append("────────────────────\n");
        report.append(String.format("Grievance Related Actions: %d\n", grievanceActions));
        report.append(String.format("User Management Actions: %d\n", userActions));
        report.append(String.format("System Actions: %d\n", systemActions));
        report.append(String.format("Login/Logout Actions: %d\n\n", loginActions));

        // Actions by role
        report.append("👥 ACTIONS BY ROLE\n");
        report.append("──────────────────\n");

        long citizenActions = entries.stream()
                .filter(e -> "CITIZEN".equals(e.getActorRole()))
                .count();
        long officerActions = entries.stream()
                .filter(e -> "OFFICER".equals(e.getActorRole()))
                .count();
        long authorityActions = entries.stream()
                .filter(e -> "AUTHORITY".equals(e.getActorRole()))
                .count();
        long auditorActions = entries.stream()
                .filter(e -> "AUDITOR".equals(e.getActorRole()))
                .count();
        long systemActorActions = entries.stream()
                .filter(e -> "SYSTEM".equals(e.getActorRole()))
                .count();

        report.append(String.format("Citizens: %d actions\n", citizenActions));
        report.append(String.format("Officers: %d actions\n", officerActions));
        report.append(String.format("Authorities: %d actions\n", authorityActions));
        report.append(String.format("Auditors: %d actions\n", auditorActions));
        report.append(String.format("System: %d actions\n\n", systemActorActions));

        // SLA Compliance section
        report.append("⏱️ SLA COMPLIANCE METRICS\n");
        report.append("────────────────────────\n");

        long slaViolations = entries.stream()
                .filter(e -> e.getAction().contains("SLA") ||
                        (e.getDetails() != null && e.getDetails().contains("violat")))
                .count();

        long escalations = entries.stream()
                .filter(e -> e.getAction().contains("ESCALAT") ||
                        (e.getDetails() != null && e.getDetails().contains("escalat")))
                .count();

        report.append(String.format("SLA Violations Detected: %d\n", slaViolations));
        report.append(String.format("Escalations Triggered: %d\n\n", escalations));

        // Detailed log
        report.append("📋 DETAILED AUDIT LOG\n");
        report.append("─────────────────────\n");

        int count = 1;
        for (AuditEntry entry : entries) {
            report.append(String.format("%d. [%s] %s\n", count++,
                    entry.getTimestamp().format(formatter), entry.getAction()));
            report.append(String.format("   Actor: %s (%s)\n", entry.getActor(), entry.getActorRole()));
            report.append(String.format("   Target: %s [%s]\n", entry.getTargetId(), entry.getTargetType()));
            report.append(String.format("   Details: %s\n", entry.getDetails()));
            report.append("\n");
        }

        // Compliance score
        report.append("📈 COMPLIANCE SCORE\n");
        report.append("───────────────────\n");

        double complianceScore = 100.0;
        if (!entries.isEmpty()) {
            long issues = slaViolations + escalations;
            complianceScore = 100.0 - ((issues * 100.0) / entries.size());
            if (complianceScore < 0)
                complianceScore = 0;
        }

        report.append(String.format("Overall Compliance Score: %.2f%%\n", complianceScore));

        if (complianceScore >= 90) {
            report.append("Rating: 🟢 EXCELLENT\n");
        } else if (complianceScore >= 70) {
            report.append("Rating: 🟡 GOOD\n");
        } else if (complianceScore >= 50) {
            report.append("Rating: 🟠 FAIR\n");
        } else {
            report.append("Rating: 🔴 POOR - Immediate attention required\n");
        }

        return report.toString();
    }

    /**
     * Generate summary report for dashboard
     */
    public static String generateSummaryReport() {
        List<AuditEntry> entries = getAllAuditEntries();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime today = now.toLocalDate().atStartOfDay();
        LocalDateTime weekAgo = now.minusDays(7);
        LocalDateTime monthAgo = now.minusMonths(1);

        long todayCount = entries.stream()
                .filter(e -> e.getTimestamp().isAfter(today))
                .count();

        long weekCount = entries.stream()
                .filter(e -> e.getTimestamp().isAfter(weekAgo))
                .count();

        long monthCount = entries.stream()
                .filter(e -> e.getTimestamp().isAfter(monthAgo))
                .count();

        StringBuilder report = new StringBuilder();
        report.append("📊 AUDIT SUMMARY\n");
        report.append("════════════════\n");
        report.append(String.format("Last 24 hours: %d actions\n", todayCount));
        report.append(String.format("Last 7 days: %d actions\n", weekCount));
        report.append(String.format("Last 30 days: %d actions\n", monthCount));
        report.append(String.format("Total Lifetime: %d actions\n", entries.size()));

        return report.toString();
    }

    /**
     * Clear audit log (for testing purposes only)
     */
    public static void clearAuditLog() {
        auditLog.clear();
        System.out.println("Audit log cleared.");
    }

    /**
     * Get audit log size
     */
    public static int getAuditLogSize() {
        return auditLog.size();
    }

    /**
     * Helper method to pad strings for report formatting
     */
    private static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }

    /**
     * Export audit log to CSV format
     */
    public static String exportToCSV() {
        StringBuilder csv = new StringBuilder();
        csv.append("Log ID,Timestamp,Action,Actor,Actor Role,Target ID,Target Type,Details,IP Address\n");

        for (AuditEntry entry : auditLog) {
            csv.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                    entry.getLogId(),
                    entry.getTimestamp().format(formatter),
                    entry.getAction(),
                    entry.getActor(),
                    entry.getActorRole(),
                    entry.getTargetId(),
                    entry.getTargetType(),
                    entry.getDetails().replace("\"", "\"\""),
                    entry.getIpAddress()));
        }

        return csv.toString();
    }

    /**
     * Search audit log by keyword
     */
    public static List<AuditEntry> searchAuditLog(String keyword) {
        List<AuditEntry> results = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();

        for (AuditEntry entry : auditLog) {
            if (entry.getAction().toLowerCase().contains(lowerKeyword) ||
                    entry.getActor().toLowerCase().contains(lowerKeyword) ||
                    (entry.getActorRole() != null && entry.getActorRole().toLowerCase().contains(lowerKeyword)) ||
                    (entry.getTargetId() != null && entry.getTargetId().toLowerCase().contains(lowerKeyword)) ||
                    (entry.getTargetType() != null && entry.getTargetType().toLowerCase().contains(lowerKeyword)) ||
                    (entry.getDetails() != null && entry.getDetails().toLowerCase().contains(lowerKeyword))) {
                results.add(entry);
            }
        }

        return results;
    }
}
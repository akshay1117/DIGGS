package com.diggs.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.diggs.audit.AuditTrail;
import com.diggs.model.Auditor;
import com.diggs.model.Authority;
import com.diggs.model.Citizen;
import com.diggs.model.Grievance;
import com.diggs.model.GrievanceHistory;
import com.diggs.model.Officer;
import com.diggs.model.User;
import com.diggs.notification.NotificationService;
import com.diggs.service.AuthenticationService;

public class MySQLDatabaseService {
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/diggs_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "akshayjr10";
    private static Connection connection = null;
    private static boolean connected = false;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found: " + e.getMessage());
        }
    }

    public static boolean connect() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
                connected = true;
                System.out.println("✅ Connected to MySQL database!");
            }
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Failed to connect: " + e.getMessage());
            connected = false;
            return false;
        }
    }

    public static void disconnect() {
        try {
            if (connection != null && !connection.isClosed())
                connection.close();
            connected = false;
        } catch (SQLException e) {
        }
    }

    public static boolean isConnected() {
        try {
            return connected && connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    // User operations
    public static boolean saveUser(User user) {
        String sql = "INSERT INTO users (user_id, first_name, last_name, email, phone_number, password, role, status, create_date, address, city, state, pincode) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, user.getUserId());
            pstmt.setString(2, user.getFirstName() != null ? user.getFirstName() : "");
            pstmt.setString(3, user.getLastName() != null ? user.getLastName() : "");
            pstmt.setString(4, user.getEmail());
            pstmt.setString(5, user.getPhoneNumber());
            pstmt.setString(6, user.getPasswordHash());
            pstmt.setString(7, user.getRole().toString());
            pstmt.setString(8, "ACTIVE");
            pstmt.setTimestamp(9, Timestamp.valueOf(user.getCreatedAt()));

            if (user instanceof Citizen) {
                Citizen c = (Citizen) user;
                pstmt.setString(10, c.getAddress());
                pstmt.setString(11, c.getCity());
                pstmt.setString(12, c.getState());
                pstmt.setString(13, c.getPincode());
            } else {
                pstmt.setNull(10, Types.VARCHAR);
                pstmt.setNull(11, Types.VARCHAR);
                pstmt.setNull(12, Types.VARCHAR);
                pstmt.setNull(13, Types.VARCHAR);
            }

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                AuditTrail.recordAction("USER_CREATED", user, user.getUserId(), "USER", "New user created");
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Error saving user: " + e.getMessage());
            return false;
        }
    }

    public static User findUserByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next())
                return mapResultSetToUser(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static User findUserByPhone(String phone) {
        String sql = "SELECT * FROM users WHERE phone_number = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, phone);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next())
                return mapResultSetToUser(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static User findUserById(String userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next())
                return mapResultSetToUser(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean softDeleteUser(String userId) {
        String sql = "UPDATE users SET status = 'INACTIVE' WHERE user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                AuditTrail.recordAction("USER_SOFT_DELETED", AuthenticationService.getCurrentUser(), userId, "USER",
                        "User account deactivated");
                return true;
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean hardDeleteUser(String userId) {
        try {
            // Check if user has grievances, delete them first
            List<Grievance> userGrievances = getGrievancesByCitizen(userId);
            for (Grievance g : userGrievances) {
                deleteGrievance(g.getGrievanceId());
            }

            // Remove relations in handles
            connection.prepareStatement("DELETE FROM handles WHERE user_id = '" + userId + "'").executeUpdate();

            // Remove notifications
            connection.prepareStatement("DELETE FROM notifications WHERE user_id = '" + userId + "'").executeUpdate();

            // Remove audit logs related to this user
            connection.prepareStatement("DELETE FROM auditlogs WHERE user_id = '" + userId + "'").executeUpdate();

            String sql = "DELETE FROM users WHERE user_id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static User mapResultSetToUser(ResultSet rs) throws SQLException {
        String role = rs.getString("role");
        User user = null;
        switch (role) {
            case "CITIZEN":
                Citizen c = new Citizen();
                c.setAddress(rs.getString("address"));
                c.setCity(rs.getString("city"));
                c.setState(rs.getString("state"));
                c.setPincode(rs.getString("pincode"));
                user = c;
                break;
            case "OFFICER":
                Officer o = new Officer();
                o.setEmployeeId(rs.getString("user_id"));
                user = o;
                break;
            case "AUTHORITY":
                Authority a = new Authority();
                a.setAuthorityLevel("Level 1");
                user = a;
                break;
            case "AUDITOR":
                user = new Auditor();
                break;
            default:
                user = new Citizen();
                break;
        }
        user.setUserId(rs.getString("user_id"));
        user.setEmail(rs.getString("email"));
        user.setPhoneNumber(rs.getString("phone_number"));
        user.setPasswordHash(rs.getString("password"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setRole(User.UserRole.valueOf(role));
        user.setCreatedAt(rs.getTimestamp("create_date").toLocalDateTime());
        user.setStatus(rs.getString("status"));
        user.setActive("ACTIVE".equals(rs.getString("status")));
        return user;
    }

    public static boolean updateUserLastLogin(String userId) {
        // last_login column is removed in the new schema, so we do nothing here to
        // prevent SQL exceptions
        return true;
    }

    public static boolean updateUserPassword(String email, String newPassword) {
        String sql = "UPDATE users SET password = ? WHERE email = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, newPassword);
            pstmt.setString(2, email);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next())
                list.add(mapResultSetToUser(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<User> getUsersByRole(User.UserRole role) {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, role.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next())
                list.add(mapResultSetToUser(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Grievance operations
    public static boolean saveGrievance(Grievance g) {
        String sql = "INSERT INTO grievances (grievance_id, user_id, department_id, case_number, title, description, priority, status, date_submitted, due_date) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, g.getGrievanceId());
            pstmt.setString(2, g.getSubmittedBy());
            pstmt.setString(3, g.getDepartment() != null ? g.getDepartment() : "DEPT001");
            pstmt.setString(4, "CASE-" + UUID.randomUUID().toString().substring(0, 8)); // dummy case number
            pstmt.setString(5, g.getTitle());
            pstmt.setString(6, g.getDescription());
            pstmt.setString(7, "MEDIUM");
            pstmt.setString(8, g.getCurrentState().toString());
            pstmt.setTimestamp(9, Timestamp.valueOf(g.getCreatedAt()));
            if (g.getSlaDeadline() != null)
                pstmt.setTimestamp(10, Timestamp.valueOf(g.getSlaDeadline()));
            else
                pstmt.setNull(10, Types.TIMESTAMP);
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                AuditTrail.recordAction("GRIEVANCE_SUBMITTED", findUserById(g.getSubmittedBy()), g.getGrievanceId(),
                        "GRIEVANCE", "New grievance submitted");
                return true;
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Grievance findGrievanceById(String id) {
        String sql = "SELECT * FROM grievances WHERE grievance_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next())
                return mapResultSetToGrievance(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Grievance mapResultSetToGrievance(ResultSet rs) throws SQLException {
        Grievance g = new Grievance();
        g.setGrievanceId(rs.getString("grievance_id"));
        g.setTitle(rs.getString("title"));
        g.setDescription(rs.getString("description"));
        // map department_id to department for now
        g.setDepartment(rs.getString("department_id"));
        g.setSubmittedBy(rs.getString("user_id"));
        g.setCurrentState(Grievance.GrievanceState.valueOf(rs.getString("status")));
        g.setCreatedAt(rs.getTimestamp("date_submitted").toLocalDateTime());
        Timestamp deadline = rs.getTimestamp("due_date");
        if (deadline != null)
            g.setSlaDeadline(deadline.toLocalDateTime());
        loadGrievanceHistory(g);
        return g;
    }

    private static void loadGrievanceHistory(Grievance g) {
        String sql = "SELECT * FROM workflowlogs WHERE grievance_id = ? ORDER BY change_date ASC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, g.getGrievanceId());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                GrievanceHistory h = new GrievanceHistory(
                        rs.getString("grievance_id"),
                        rs.getString("previous_status"),
                        rs.getString("new_status"),
                        "Unknown", // changed_by is not in workflowlogs anymore
                        "Unknown",
                        rs.getString("remarks"));
                h.setHistoryId(rs.getString("log_id"));
                h.setComments(rs.getString("remarks"));
                h.setChangedAt(rs.getTimestamp("change_date").toLocalDateTime());
                g.getHistory().add(h);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean deleteGrievance(String grievanceId) {
        try {
            connection.prepareStatement("DELETE FROM escalations WHERE grievance_id = '" + grievanceId + "'")
                    .executeUpdate();
            connection.prepareStatement("DELETE FROM handles WHERE grievance_id = '" + grievanceId + "'")
                    .executeUpdate();
            connection.prepareStatement("DELETE FROM workflowlogs WHERE grievance_id = '" + grievanceId + "'")
                    .executeUpdate();
            connection.prepareStatement("DELETE FROM attachment WHERE grievance_id = '" + grievanceId + "'")
                    .executeUpdate();
            connection.prepareStatement("DELETE FROM notifications WHERE related_grievance_id = '" + grievanceId + "'")
                    .executeUpdate();

            String sql = "DELETE FROM grievances WHERE grievance_id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, grievanceId);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateGrievance(Grievance g) {
        String sql = "UPDATE grievances SET title=?, description=?, status=?, due_date=? WHERE grievance_id=?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, g.getTitle());
            pstmt.setString(2, g.getDescription());
            pstmt.setString(3, g.getCurrentState().toString());
            if (g.getSlaDeadline() != null)
                pstmt.setTimestamp(4, Timestamp.valueOf(g.getSlaDeadline()));
            else
                pstmt.setNull(4, Types.TIMESTAMP);
            pstmt.setString(5, g.getGrievanceId());
            boolean result = pstmt.executeUpdate() > 0;

            // Also insert into handles if newly assigned
            if (g.getAssignedTo() != null && !g.getAssignedTo().isEmpty()) {
                String handleSql = "INSERT IGNORE INTO handles (user_id, grievance_id) VALUES (?, ?)";
                try (PreparedStatement handleStmt = connection.prepareStatement(handleSql)) {
                    handleStmt.setString(1, g.getAssignedTo());
                    handleStmt.setString(2, g.getGrievanceId());
                    handleStmt.executeUpdate();
                }
            }
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean saveGrievanceHistory(GrievanceHistory h) {
        String sql = "INSERT INTO workflowlogs (log_id, grievance_id, previous_status, new_status, remarks, change_date) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, h.getHistoryId());
            pstmt.setString(2, h.getGrievanceId());
            pstmt.setString(3, h.getPreviousState());
            pstmt.setString(4, h.getNewState());
            pstmt.setString(5, h.getAction() + " " + (h.getComments() != null ? h.getComments() : ""));
            pstmt.setTimestamp(6, Timestamp.valueOf(h.getChangedAt()));
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean saveAuditLog(AuditTrail.AuditEntry entry) {
        if (!isConnected())
            connect();
        String sql = "INSERT INTO auditlogs (audit_id, user_id, entity_name, record_id, action_type, action_timestamp, ip_address) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, entry.getLogId());
            pstmt.setString(2, entry.getActor());
            pstmt.setString(3, entry.getTargetType());
            pstmt.setString(4, entry.getTargetId());
            pstmt.setString(5, entry.getAction());
            pstmt.setTimestamp(6, Timestamp.valueOf(entry.getTimestamp()));
            pstmt.setString(7, entry.getIpAddress());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean saveNotification(NotificationService.Notification notif) {
        if (!isConnected())
            connect();
        String sql = "INSERT INTO notifications (notification_id, user_id, title, message, type, related_grievance_id, timestamp, is_read) VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, notif.getNotificationId());
            pstmt.setString(2, notif.getUserId());
            pstmt.setString(3, notif.getTitle());
            pstmt.setString(4, notif.getMessage());
            pstmt.setString(5, notif.getType().toString());
            pstmt.setString(6, notif.getRelatedGrievanceId());
            pstmt.setTimestamp(7, Timestamp.valueOf(notif.getTimestamp()));
            pstmt.setBoolean(8, notif.isRead());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<NotificationService.Notification> getNotificationsForUser(String userId) {
        List<NotificationService.Notification> notifications = new ArrayList<>();
        if (!isConnected())
            connect();
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY timestamp DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                NotificationService.Notification notif = new NotificationService.Notification(
                        rs.getString("notification_id"),
                        rs.getString("user_id"),
                        rs.getString("title"),
                        rs.getString("message"),
                        NotificationService.Notification.NotificationType.valueOf(rs.getString("type")),
                        rs.getTimestamp("timestamp").toLocalDateTime(),
                        rs.getBoolean("is_read"),
                        rs.getString("related_grievance_id"));
                notifications.add(notif);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return notifications;
    }

    public static boolean markNotificationAsRead(String notificationId) {
        if (!isConnected())
            connect();
        String sql = "UPDATE notifications SET is_read = TRUE WHERE notification_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, notificationId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean saveEscalation(String escalationId, String grievanceId, int level, String status,
            String reason) {
        if (!isConnected())
            connect();
        String sql = "INSERT INTO escalations (escalation_id, grievance_id, escalation_level, escalation_date, escalation_status, reason) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, escalationId);
            pstmt.setString(2, grievanceId);
            pstmt.setInt(3, level);
            pstmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            pstmt.setString(5, status);
            pstmt.setString(6, reason);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean saveEvidence(String evidenceId, String fileName, String fileType, String filePath) {
        if (!isConnected())
            connect();
        String sql = "INSERT INTO evidence (evidence_id, file_name, file_type, file_path, uploaded_date) VALUES (?,?,?,?,?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, evidenceId);
            pstmt.setString(2, fileName);
            pstmt.setString(3, fileType);
            pstmt.setString(4, filePath);
            pstmt.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean saveAttachment(String grievanceId, String evidenceId) {
        if (!isConnected())
            connect();
        String sql = "INSERT INTO attachment (grievance_id, evidence_id) VALUES (?,?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, grievanceId);
            pstmt.setString(2, evidenceId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<Grievance> getAllGrievances() {
        List<Grievance> list = new ArrayList<>();
        String sql = "SELECT * FROM grievances";
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next())
                list.add(mapResultSetToGrievance(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<Grievance> getGrievancesByCitizen(String citizenId) {
        List<Grievance> list = new ArrayList<>();
        String sql = "SELECT * FROM grievances WHERE user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, citizenId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next())
                list.add(mapResultSetToGrievance(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<Grievance> getGrievancesByOfficer(String officerId) {
        List<Grievance> list = new ArrayList<>();
        String sql = "SELECT g.* FROM grievances g LEFT JOIN handles h ON g.grievance_id = h.grievance_id WHERE h.user_id = ? OR h.user_id IS NULL";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, officerId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next())
                list.add(mapResultSetToGrievance(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<Grievance> getGrievancesByState(Grievance.GrievanceState state) {
        List<Grievance> list = new ArrayList<>();
        String sql = "SELECT * FROM grievances WHERE status = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, state.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next())
                list.add(mapResultSetToGrievance(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}

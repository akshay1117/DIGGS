package com.diggs.config;

public class DatabaseConfig {
    private static final String DB_URL = System.getenv().getOrDefault("DB_URL", 
        "jdbc:mysql://localhost:3306/diggs_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
    private static final String DB_USER = System.getenv().getOrDefault("DB_USER", "root");
    private static final String DB_PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "akshayjr10");
    
    public static String getUrl() {
        return DB_URL;
    }
    
    public static String getUser() {
        return DB_USER;
    }
    
    public static String getPassword() {
        return DB_PASSWORD;
    }
}
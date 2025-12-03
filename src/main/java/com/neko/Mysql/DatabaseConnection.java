package com.neko.Mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.io.InputStream;

public class DatabaseConnection {
    private static Properties properties = new Properties();
    
    static {
        try (InputStream input = DatabaseConnection.class.getClassLoader().getResourceAsStream("application.properties")) {
            properties.load(input);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static Connection getLocalConnection() throws SQLException {
        String url = properties.getProperty("local.db.url");
        String username = properties.getProperty("local.db.username");
        String password = properties.getProperty("local.db.password");
        String driver = properties.getProperty("local.db.driver");
        
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            System.err.println("本地数据库驱动未找到: " + e.getMessage());
            throw new SQLException("数据库驱动未找到", e);
        }
        
        return DriverManager.getConnection(url, username, password);
    }
    
    public static Connection getRemoteConnection() throws SQLException {
        String url = properties.getProperty("remote.db.url");
        String username = properties.getProperty("remote.db.username");
        String password = properties.getProperty("remote.db.password");
        String driver = properties.getProperty("remote.db.driver");
        
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            System.err.println("远程数据库驱动未找到: " + e.getMessage());
            throw new SQLException("数据库驱动未找到", e);
        }
        
        return DriverManager.getConnection(url, username, password);
    }
    
    /**
     * 验证本地数据库连接
     * @return 连接是否成功
     */
    public static boolean validateLocalConnection() {
        try (Connection conn = getLocalConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("本地数据库连接验证失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 验证远程数据库连接
     * @return 连接是否成功
     */
    public static boolean validateRemoteConnection() {
        try (Connection conn = getRemoteConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("远程数据库连接验证失败: " + e.getMessage());
            return false;
        }
    }
}
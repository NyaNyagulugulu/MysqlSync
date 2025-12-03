package com.neko.Mysql;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.io.InputStream;

public class DatabaseSync {
    private static Properties properties = new Properties();
    
    static {
        try (InputStream input = DatabaseSync.class.getClassLoader().getResourceAsStream("application.properties")) {
            properties.load(input);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void syncDatabases() {
        try (Connection remoteConn = DatabaseConnection.getRemoteConnection();
             Connection localConn = DatabaseConnection.getLocalConnection()) {
            
            System.out.println("Starting database instance synchronization...");
            
            // 获取远程数据库实例中的所有数据库名（排除系统数据库）
            List<String> databaseNames = getDatabaseNames(remoteConn);
            
            for (String dbName : databaseNames) {
                syncDatabase(remoteConn, localConn, dbName);
            }
            
            System.out.println("Database instance synchronization completed!");
            
        } catch (SQLException e) {
            System.err.println("Database instance synchronization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private List<String> getDatabaseNames(Connection conn) throws SQLException {
        List<String> databaseNames = new ArrayList<>();
        String query = "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME NOT IN ('information_schema', 'mysql', 'performance_schema', 'sys')";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                databaseNames.add(rs.getString("SCHEMA_NAME"));
            }
        }
        
        return databaseNames;
    }
    
    private void syncDatabase(Connection remoteConn, Connection localConn, String dbName) {
        try {
            // Check if the database exists in the local database instance
            if (!databaseExists(localConn, dbName)) {
                // 如果不存在，则创建数据库
                createDatabase(localConn, dbName);
            }
            
            // Switch to the corresponding database
            try (Statement remoteStmt = remoteConn.createStatement();
                 Statement localStmt = localConn.createStatement()) {
                
                remoteStmt.execute("USE `" + dbName + "`");
                localStmt.execute("USE `" + dbName + "`");
                
                // Get all table names from the remote database
                List<String> tableNames = getTableNames(remoteConn, dbName);
                
                for (String tableName : tableNames) {
                    syncTable(remoteConn, localConn, dbName, tableName);
                }
            }
            
            System.out.println("Database " + dbName + " synchronization completed");
            
        } catch (SQLException e) {
            System.err.println("Error synchronizing database " + dbName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private boolean databaseExists(Connection conn, String dbName) throws SQLException {
        String query = "SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, dbName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        
        return false;
    }
    
    private void createDatabase(Connection conn, String dbName) throws SQLException {
        String createDatabaseSQL = "CREATE DATABASE IF NOT EXISTS `" + dbName + "`";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createDatabaseSQL);
        }
    }
    
    private List<String> getTableNames(Connection conn, String dbName) throws SQLException {
        List<String> tableNames = new ArrayList<>();
        String query = "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_TYPE = 'BASE TABLE'";
        
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, dbName);
            try (ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    tableNames.add(rs.getString("TABLE_NAME"));
                }
            }
        }
        
        return tableNames;
    }
    
    private void syncTable(Connection remoteConn, Connection localConn, String dbName, String tableName) {
        try {
            // Check if the table exists in the local database
            if (!tableExists(localConn, dbName, tableName)) {
                // 如果不存在，则创建表结构
                createTableFromRemote(remoteConn, localConn, dbName, tableName);
            }
            
            // Clear local table data
            truncateTable(localConn, dbName, tableName);
            
            // Copy data from remote database to local
            copyTableData(remoteConn, localConn, dbName, tableName);
            
            System.out.println("  Table " + tableName + " in database " + dbName + " synchronization completed");
            
        } catch (SQLException e) {
            System.err.println("Error synchronizing table " + tableName + " in database " + dbName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private boolean tableExists(Connection conn, String dbName, String tableName) throws SQLException {
        String query = "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, dbName);
            stmt.setString(2, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        
        return false;
    }
    
    private void createTableFromRemote(Connection remoteConn, Connection localConn, String dbName, String tableName) throws SQLException {
        String originalDb = getCurrentDatabase(remoteConn);
        String showCreateTable = "SHOW CREATE TABLE `" + tableName + "`";
        
        try (Statement remoteStmt = remoteConn.createStatement();
             ResultSet rs = remoteStmt.executeQuery(showCreateTable)) {
            
            if (rs.next()) {
                String createTableSQL = rs.getString(2);
                String cleanSQL = createTableSQL.replace("`", "");
                
                try (Statement localStmt = localConn.createStatement()) {
                    localStmt.execute("DROP TABLE IF EXISTS `" + tableName + "`");
                    localStmt.execute(cleanSQL);
                }
            }
        }
    }
    
    private String getCurrentDatabase(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT DATABASE()")) {
            
            if (rs.next()) {
                return rs.getString(1);
            }
        }
        
        return null;
    }
    
    private void truncateTable(Connection conn, String dbName, String tableName) throws SQLException {
        String truncateSQL = "TRUNCATE TABLE `" + dbName + "`.`" + tableName + "`";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(truncateSQL);
        }
    }
    
    private void copyTableData(Connection remoteConn, Connection localConn, String dbName, String tableName) throws SQLException {
        String selectSQL = "SELECT * FROM `" + tableName + "`";
        
        try (Statement remoteStmt = remoteConn.createStatement();
             ResultSet rs = remoteStmt.executeQuery(selectSQL)) {
            
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            // 构建INSERT语句，获取远程表的列信息
            StringBuilder insertSQL = new StringBuilder("INSERT INTO `" + dbName + "`.`" + tableName + "` VALUES (");
            for (int i = 1; i <= columnCount; i++) {
                insertSQL.append("?");
                if (i < columnCount) {
                    insertSQL.append(", ");
                }
            }
            insertSQL.append(")");
            
            try (PreparedStatement insertStmt = localConn.prepareStatement(insertSQL.toString())) {
                
                while (rs.next()) {
                    for (int i = 1; i <= columnCount; i++) {
                        insertStmt.setObject(i, rs.getObject(i));
                    }
                    insertStmt.addBatch();
                }
                
                insertStmt.executeBatch();
            }
        } catch (SQLException e) {
            // 如果插入数据失败，删除本地表并重新从远程创建表结构，然后复制数据
            System.err.println("Error inserting data into local table, recreating table structure from remote: " + e.getMessage());
            try {
                // 重新创建表结构
                createTableFromRemote(remoteConn, localConn, dbName, tableName);
                // 清空表后重新复制数据
                truncateTable(localConn, dbName, tableName);
                
                // 重新执行数据复制
                String recreateSelectSQL = "SELECT * FROM `" + tableName + "`";
                try (Statement remoteStmt = remoteConn.createStatement();
                     ResultSet rs = remoteStmt.executeQuery(recreateSelectSQL)) {
                    
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    
                    StringBuilder insertSQL = new StringBuilder("INSERT INTO `" + dbName + "`.`" + tableName + "` VALUES (");
                    for (int i = 1; i <= columnCount; i++) {
                        insertSQL.append("?");
                        if (i < columnCount) {
                            insertSQL.append(", ");
                        }
                    }
                    insertSQL.append(")");
                    
                    try (PreparedStatement insertStmt = localConn.prepareStatement(insertSQL.toString())) {
                        
                        while (rs.next()) {
                            for (int i = 1; i <= columnCount; i++) {
                                insertStmt.setObject(i, rs.getObject(i));
                            }
                            insertStmt.addBatch();
                        }
                        
                        insertStmt.executeBatch();
                    }
                }
            } catch (SQLException recreateError) {
                System.err.println("Error recreating table structure: " + recreateError.getMessage());
                throw recreateError;
            }
        }
    }
}
package com.neko.Mysql;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Properties;
import java.io.InputStream;

public class Main {
    private static Properties properties = new Properties();
    
    static {
        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("application.properties")) {
            properties.load(input);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.out.println("正在启动数据库同步服务...");
        System.out.println("正在验证数据库连接...");
        
        // 在启动时验证连接
        if (!DatabaseConnection.validateLocalConnection()) {
            System.err.println("本地数据库连接验证失败，程序退出");
            return;
        }
        System.out.println("本地数据库连接验证成功");
        
        if (!DatabaseConnection.validateRemoteConnection()) {
            System.err.println("远程数据库连接验证失败，程序退出");
            return;
        }
        System.out.println("远程数据库连接验证成功");
        
        System.out.println("数据库连接验证完成，启动同步服务...");
        DatabaseSync sync = new DatabaseSync();
        
        // Get sync interval time, default is 5 minutes (300000 milliseconds)
        long syncInterval = Long.parseLong(properties.getProperty("sync.interval", "300000"));
        
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sync.syncDatabases();
            }
        }, 0, syncInterval);
        
        System.out.println("数据库同步服务已启动，同步间隔: " + (syncInterval / 1000 / 60) + " 分钟");
    }
}

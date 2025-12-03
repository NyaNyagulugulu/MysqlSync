# MySQL数据库同步工具

这个工具用于定时从远程MySQL数据库同步数据到本地MySQL数据库。

## 功能特性

- 定时同步远程数据库到本地数据库
- 自动创建不存在的表
- 如果表已存在则替换原有数据
- 支持配置同步间隔时间

## 配置文件

编辑 `src/main/resources/application.properties` 文件：

```properties
# 本地数据库配置
local.db.url=jdbc:mysql://localhost:3306/local_db
local.db.username=root
local.db.password=your_password
local.db.driver=com.mysql.cj.jdbc.Driver

# 远端数据库配置
remote.db.url=jdbc:mysql://remote_host:3306/remote_db
remote.db.username=remote_user
remote.db.password=remote_password
remote.db.driver=com.mysql.cj.jdbc.Driver

# 同步配置（毫秒，默认5分钟）
sync.interval=300000
```

## 编译和运行

使用Gradle编译项目：

```bash
./gradlew build
```

运行应用程序：

```bash
./gradlew run
```

## 工作原理

1. 读取配置文件中的数据库连接信息
2. 连接到远程和本地数据库
3. 获取远程数据库中的所有表名
4. 对每个表：
   - 检查本地是否存在该表
   - 如果不存在，从远程复制表结构
   - 清空本地表数据
   - 从远程复制所有数据到本地
5. 按配置的时间间隔重复执行

## 注意事项

- 确保远程和本地数据库的连接信息正确
- 确保本地数据库有足够权限创建表和插入数据
- 同步过程会清空本地表的原有数据
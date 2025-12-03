# MysqlSync

一个用于同步MySQL数据库的工具，将远程数据库同步到本地数据库。

## 功能特性

- 自动同步远程数据库到本地数据库
- 支持多个数据库和表的同步
- 当本地表结构与远程不匹配时，自动重建表结构
- 定时同步功能

## 依赖

- Java 8+
- MySQL Connector/J 8.0.33

## 构建

使用Gradle构建项目：

```bash
# 构建包含所有依赖的可执行JAR文件（推荐）
./gradlew shadowJar

# 或在Windows上
gradlew.bat shadowJar
```

生成的JAR文件位于 `build/libs/MysqlSync-1.0-SNAPSHOT.jar`

## 配置

在 `src/main/resources/application.properties` 文件中配置数据库连接信息：

```properties
# 本地数据库配置
local.db.url=jdbc:mysql://127.0.0.1:3306
local.db.username=your_username
local.db.password=your_password
local.db.driver=com.mysql.cj.jdbc.Driver

# 远端数据库配置
remote.db.url=jdbc:mysql://your_remote_host:3306
remote.db.username=your_username
remote.db.password=your_password
remote.db.driver=com.mysql.cj.jdbc.Driver

# 同步配置（毫秒）
sync.interval=300000  # 默认5分钟同步一次
```

## 运行

```bash
java -jar MysqlSync-1.0-SNAPSHOT.jar
```

## 重要说明

1. **使用shadowJar构建**：必须使用`shadowJar`任务构建项目，而不是标准的`jar`任务，以确保MySQL驱动包含在JAR文件中。
   - 错误做法：`./gradlew jar` - 这样生成的JAR缺少依赖
   - 正确做法：`./gradlew shadowJar` - 这样生成的JAR包含所有依赖

2. **错误处理**：当遇到表结构不匹配等问题时，程序会自动删除本地表并从远程重新创建表结构，然后同步数据。

3. **权限要求**：确保数据库用户具有足够的权限来读取远程数据库和写入本地数据库。

## 工作原理

- 程序会遍历远程数据库实例中的所有数据库（排除系统数据库）
- 对于每个数据库，检查本地是否存在，如果不存在则创建
- 对于每个表，检查本地是否存在，如果不存在则从远程创建表结构
- 同步数据时，先清空本地表，然后从远程复制数据
- 如果在同步过程中遇到错误，会自动重建表结构并重新同步
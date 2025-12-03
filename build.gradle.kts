plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "com.neko.Web"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("mysql:mysql-connector-java:8.0.33")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

// 设置Java编译选项，支持UTF-8编码
tasks.compileJava {
    options.encoding = "UTF-8"
}

tasks.compileTestJava {
    options.encoding = "UTF-8"
}

// 配置shadowJar任务以创建包含依赖的fat jar
tasks.shadowJar {
    manifest {
        attributes["Main-Class"] = "com.neko.Mysql.Main"
    }
    archiveBaseName.set("MysqlSync")
    archiveClassifier.set("") // 移除默认的"all"分类器
    archiveVersion.set(project.version.toString())
}
# 使用官方 OpenJDK 17 slim 镜像
FROM openjdk:17-jdk-slim

# 设置时区和编码
ENV LANG=C.UTF-8
ENV TZ=Asia/Shanghai

# 设置工作目录
WORKDIR /app

# 复制 Maven 打包后的 jar 文件
COPY score-service-1.0.0.jar app.jar

# 对外暴露 Spring Boot 默认端口
EXPOSE 10003

# 启动程序，并指定使用 cloud 配置文件
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=cloud"]
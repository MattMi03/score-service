#!/bin/bash

# === 配置参数 ===
PROJECT_DIR="/Users/matt/IdeaProjects/score_service"
JAR_PATH="$PROJECT_DIR/target/score-service-1.0.0.jar"
SERVER="root@47.120.37.33"
REMOTE_DIR="/home/score-service"
CONTAINER_NAME="admin-score-service-5011"
IMAGE_NAME="admin-score-service"

# === Step 1: 打包 ===
echo ">>> 开始打包项目..."
/Users/matt/.m2/wrapper/dists/apache-maven-3.9.11-bin/6mqf5t809d9geo83kj4ttckcbc/apache-maven-3.9.11/bin/mvn \
  clean package -DskipTests -f $PROJECT_DIR/pom.xml

if [ $? -ne 0 ]; then
  echo "❌ Maven 打包失败，终止部署"
  exit 1
fi

# === Step 2: 上传 ===
echo ">>> 上传 JAR 包到服务器..."
scp $JAR_PATH $SERVER:$REMOTE_DIR/

# === Step 3: 远程部署 ===
echo ">>> 登录服务器并部署容器..."
ssh $SERVER "
  docker rm -f $CONTAINER_NAME 2>/dev/null || true &&
  docker build -t $IMAGE_NAME $REMOTE_DIR &&
  docker run -d \
    --name $CONTAINER_NAME \
    --ulimit nofile=65535:65535 \
    --network host \
    --restart unless-stopped \
    $IMAGE_NAME \
    --spring.profiles.active=cloud &&
  docker logs -f --tail 100 $CONTAINER_NAME
"
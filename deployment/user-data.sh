#!/bin/bash

set -e
IMAGE_TAG="__IMAGE_TAG__"
exec > >(tee /var/log/payment-service-user-data.log | logger -t payment-service-user-data -s 2>/dev/console) 2>&1

echo "===== Payment Service bootstrap started ====="

# -----------------------------
# Install Docker
# -----------------------------

dnf update -y
dnf install -y docker jq

systemctl enable docker
systemctl start docker

# -----------------------------
# Docker network
# -----------------------------

docker network create payment-network || true

# -----------------------------
# Login to ECR
# -----------------------------

aws ecr get-login-password \
    --region ap-south-1 | \
docker login \
    --username AWS \
    --password-stdin \
    774118824657.dkr.ecr.ap-south-1.amazonaws.com

# -----------------------------
# Start Redis
# -----------------------------

docker run -d \
    --name payment-redis \
    --network payment-network \
    --restart unless-stopped \
    redis:7-alpine

# -----------------------------
# Start Kafka
# -----------------------------

docker run -d \
    --name payment-kafka \
    --network payment-network \
    --restart unless-stopped \
    -e KAFKA_NODE_ID=1 \
    -e KAFKA_PROCESS_ROLES=broker,controller \
    -e KAFKA_LISTENERS=INTERNAL://:29092,CONTROLLER://:9093 \
    -e KAFKA_ADVERTISED_LISTENERS=INTERNAL://payment-kafka:29092 \
    -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=INTERNAL:PLAINTEXT,CONTROLLER:PLAINTEXT \
    -e KAFKA_INTER_BROKER_LISTENER_NAME=INTERNAL \
    -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
    -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@payment-kafka:9093 \
    -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
    -e KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1 \
    -e KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1 \
    apache/kafka:4.0.0

# -----------------------------
# Pull Payment Service
# -----------------------------

docker pull \
    774118824657.dkr.ecr.ap-south-1.amazonaws.com/payment-service:${IMAGE_TAG}

# -----------------------------
# Get DB credentials
# -----------------------------

SECRET=$(aws secretsmanager get-secret-value \
    --secret-id payment-service/rds \
    --region ap-south-1 \
    --query SecretString \
    --output text)

DB_USERNAME=$(echo "$SECRET" | jq -r '.username')
DB_PASSWORD=$(echo "$SECRET" | jq -r '.password')

# -----------------------------
# Start Payment Service
# -----------------------------

docker run -d \
    --name payment-service \
    --network payment-network \
    --restart unless-stopped \
    -p 4000:4000 \
    -e SPRING_PROFILES_ACTIVE=prod \
    -e DATASOURCE_URL="jdbc:postgresql://payment-postgres.cxeseem0ax4c.ap-south-1.rds.amazonaws.com:5432/postgres" \
    -e DATASOURCE_USERNAME="$DB_USERNAME" \
    -e DATASOURCE_PASSWORD="$DB_PASSWORD" \
    -e REDIS_URL="payment-redis" \
    -e REDIS_PORT="6379" \
    -e KAFKA_URL="payment-kafka:29092" \
    774118824657.dkr.ecr.ap-south-1.amazonaws.com/payment-service:${IMAGE_TAG}

echo "===== Payment Service bootstrap completed ====="
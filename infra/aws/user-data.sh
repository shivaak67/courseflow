#!/bin/bash
set -euo pipefail

# Bootstraps Prioritize on a fresh Amazon Linux 2023 EC2 instance (free-tier t2.micro).
REPO_URL="${PRIORITIZE_REPO_URL:-https://github.com/shivaak67/courseflow.git}"
REPO_BRANCH="${PRIORITIZE_REPO_BRANCH:-develop}"
APP_DIR="/opt/prioritize"

dnf update -y
dnf install -y docker git curl openssl

systemctl enable docker
systemctl start docker

mkdir -p /usr/local/lib/docker/cli-plugins
curl -fsSL "https://github.com/docker/compose/releases/download/v2.32.4/docker-compose-linux-x86_64" \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

rm -rf "${APP_DIR}"
git clone --depth 1 --branch "${REPO_BRANCH}" "${REPO_URL}" "${APP_DIR}"
cd "${APP_DIR}"

PUBLIC_IP="$(curl -fsS http://169.254.169.254/latest/meta-data/public-ipv4)"
APP_URL="http://${PUBLIC_IP}"
JWT_SECRET="$(openssl rand -base64 48 | tr -d '\n')"
POSTGRES_PASSWORD="$(openssl rand -base64 24 | tr -d '\n')"

cat > .env <<EOF
POSTGRES_HOST=db
POSTGRES_PORT=5432
POSTGRES_DB=prioritize
POSTGRES_USER=prioritize
POSTGRES_PASSWORD=${POSTGRES_PASSWORD}

SPRING_PROFILES_ACTIVE=prod
JWT_SECRET=${JWT_SECRET}
JWT_EXPIRATION_MS=86400000
APP_CORS_ORIGINS=${APP_URL}

GOOGLE_OAUTH_ENABLED=false
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
APP_OAUTH_SUCCESS_REDIRECT=${APP_URL}/auth/callback

AI_ENABLED=false
AI_API_KEY=
AI_MODEL=gpt-4o-mini
AI_BASE_URL=https://api.openai.com/v1
AI_WARMUP_ENABLED=false

NOTIFICATIONS_EMAIL_ENABLED=false
NOTIFICATIONS_SMS_ENABLED=false
EOF

docker compose -f docker-compose.prod.yml up -d --build

echo "Prioritize deployed at ${APP_URL}" > /var/log/prioritize-deploy.log
echo "Health check: ${APP_URL}/api/auth/login (POST) or open ${APP_URL} in a browser." >> /var/log/prioritize-deploy.log

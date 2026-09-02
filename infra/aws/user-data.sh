#!/bin/bash
# Do not use set -e: a single docker failure must not abort the whole bootstrap.
set -uo pipefail
exec > >(tee /var/log/prioritize-user-data.log) 2>&1

REPO_URL="${PRIORITIZE_REPO_URL:-https://github.com/shivaak67/courseflow.git}"
REPO_BRANCH="${PRIORITIZE_REPO_BRANCH:-develop}"
APP_DIR="/opt/prioritize"
COMPOSE_FILE="docker-compose.prod.yml"

echo "=== Prioritize bootstrap starting at $(date -Is) ==="

if ! swapon --show | grep -q /swapfile; then
  fallocate -l 4G /swapfile 2>/dev/null || dd if=/dev/zero of=/swapfile bs=1M count=4096
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  grep -q '/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
  echo "Swap enabled: $(swapon --show)"
fi

dnf install -y docker git openssl
systemctl enable docker
systemctl start docker

mkdir -p /usr/local/lib/docker/cli-plugins
curl -fsSL "https://github.com/docker/compose/releases/download/v2.32.4/docker-compose-linux-x86_64" \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

rm -rf "${APP_DIR}"
git clone --depth 1 --branch "${REPO_BRANCH}" "${REPO_URL}" "${APP_DIR}"
cd "${APP_DIR}"

# Runtime tuning for t2.micro (cloned branch may lag behind local fixes).
sed -i '/container_name: prioritize-frontend/,/ports:/ s/condition: service_healthy/condition: service_started/' "${COMPOSE_FILE}"
sed -i 's/start_period: 120s/start_period: 300s/' "${COMPOSE_FILE}"
if ! grep -q 'JAVA_TOOL_OPTIONS' "${COMPOSE_FILE}"; then
  sed -i '/NOTIFICATIONS_SMS_ENABLED: \${NOTIFICATIONS_SMS_ENABLED:-false}/a\      JAVA_TOOL_OPTIONS: ${JAVA_TOOL_OPTIONS:--Xmx256m -XX:+UseSerialGC}' "${COMPOSE_FILE}"
fi
if ! grep -q 'MANAGEMENT_HEALTH_MAIL_ENABLED' "${COMPOSE_FILE}"; then
  sed -i '/NOTIFICATIONS_SMS_ENABLED/a\      MANAGEMENT_HEALTH_MAIL_ENABLED: "false"' "${COMPOSE_FILE}"
fi
if ! grep -q 'SPRING_APPLICATION_JSON' "${COMPOSE_FILE}"; then
  sed -i '/MANAGEMENT_HEALTH_MAIL_ENABLED/a\      SPRING_APPLICATION_JSON: '"'"'{"management":{"health":{"mail":{"enabled":false}}}}'"'"'' "${COMPOSE_FILE}"
fi
sed -i 's/GOOGLE_CLIENT_ID: ${GOOGLE_CLIENT_ID:-}/GOOGLE_CLIENT_ID: ${GOOGLE_CLIENT_ID:-disabled}/' "${COMPOSE_FILE}"
sed -i 's/GOOGLE_CLIENT_SECRET: ${GOOGLE_CLIENT_SECRET:-}/GOOGLE_CLIENT_SECRET: ${GOOGLE_CLIENT_SECRET:-disabled}/' "${COMPOSE_FILE}"

PUBLIC_IP="$(TOKEN=$(curl -fsS -X PUT "http://169.254.169.254/latest/api/token" -H "X-aws-ec2-metadata-token-ttl-seconds: 21600"); curl -fsS -H "X-aws-ec2-metadata-token: ${TOKEN}" http://169.254.169.254/latest/meta-data/public-ipv4)"
APP_PUBLIC_URL="${PRIORITIZE_APP_PUBLIC_URL:-}"
if [ -n "${APP_PUBLIC_URL}" ]; then
  APP_URL="${APP_PUBLIC_URL}"
else
  APP_URL="http://${PUBLIC_IP}"
fi
echo "Public app URL: ${APP_URL}"

cat > .env <<EOF
POSTGRES_HOST=db
POSTGRES_PORT=5432
POSTGRES_DB=prioritize
POSTGRES_USER=prioritize
POSTGRES_PASSWORD=$(openssl rand -hex 16)

SPRING_PROFILES_ACTIVE=prod
JWT_SECRET=$(openssl rand -hex 48)
JWT_EXPIRATION_MS=86400000
APP_CORS_ORIGINS=${APP_URL}
JAVA_TOOL_OPTIONS=-Xmx256m -XX:+UseSerialGC
MANAGEMENT_HEALTH_MAIL_ENABLED=false
SPRING_APPLICATION_JSON={"management":{"health":{"mail":{"enabled":false}}}}

GOOGLE_OAUTH_ENABLED=${PRIORITIZE_GOOGLE_OAUTH_ENABLED:-false}
GOOGLE_CLIENT_ID=${PRIORITIZE_GOOGLE_CLIENT_ID:-disabled}
GOOGLE_CLIENT_SECRET=${PRIORITIZE_GOOGLE_CLIENT_SECRET:-disabled}
APP_OAUTH_SUCCESS_REDIRECT=${APP_URL}/auth/callback

AI_ENABLED=${PRIORITIZE_AI_ENABLED:-false}
AI_API_KEY=${PRIORITIZE_AI_API_KEY:-}
AI_MODEL=${PRIORITIZE_AI_MODEL:-gpt-4o-mini}
AI_BASE_URL=${PRIORITIZE_AI_BASE_URL:-https://api.openai.com/v1}
AI_WARMUP_ENABLED=${PRIORITIZE_AI_WARMUP_ENABLED:-false}

NOTIFICATIONS_EMAIL_ENABLED=false
NOTIFICATIONS_SMS_ENABLED=false
EOF

if [ -n "${PRIORITIZE_GITHUB_TOKEN:-}" ]; then
  echo "${PRIORITIZE_GITHUB_TOKEN}" | docker login ghcr.io -u shivaak67 --password-stdin || true
fi

echo "=== Pulling pre-built images from GHCR ==="
docker compose -f "${COMPOSE_FILE}" pull db || true

SHOULD_BUILD_FRONTEND="${PRIORITIZE_BUILD_FRONTEND:-false}"
SHOULD_BUILD_BACKEND="${PRIORITIZE_BUILD_BACKEND:-false}"

if [ "${SHOULD_BUILD_BACKEND}" = "true" ]; then
  echo "=== Building backend image from source (skipping GHCR pull) ==="
  docker compose -f "${COMPOSE_FILE}" build backend
else
  if ! docker compose -f "${COMPOSE_FILE}" pull backend; then
    echo "Backend pull failed; building locally (slow on t2.micro)..."
    docker compose -f "${COMPOSE_FILE}" build backend
  fi
fi

if [ "${SHOULD_BUILD_FRONTEND}" = "true" ]; then
  echo "=== Building frontend image from source (skipping GHCR pull) ==="
  export NODE_OPTIONS="--max-old-space-size=384"
  export DOCKER_BUILDKIT=1
  export COMPOSE_DOCKER_CLI_BUILD=1
  if ! grep -q 'location /actuator/' frontend/nginx.prod.conf 2>/dev/null; then
    awk '
      /location \/api\// && !done {
        print "    location /actuator/ {"
        print "        proxy_pass http://backend:8080/actuator/;"
        print "        proxy_http_version 1.1;"
        print "        proxy_set_header Host $host;"
        print "        proxy_set_header X-Real-IP $remote_addr;"
        print "        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;"
        print "        proxy_set_header X-Forwarded-Proto $forwarded_proto;"
        print "    }"
        print ""
        done=1
      }
      { print }
    ' frontend/nginx.prod.conf > frontend/nginx.prod.conf.tmp
    mv frontend/nginx.prod.conf.tmp frontend/nginx.prod.conf
  fi
  if ! grep -q 'forwarded_proto' frontend/nginx.prod.conf 2>/dev/null; then
    cat > /tmp/nginx-forwarded-proto.map <<'NGINX_MAP'
map $http_x_forwarded_proto $forwarded_proto {
    default $http_x_forwarded_proto;
    ''      $scheme;
}

NGINX_MAP
    cat /tmp/nginx-forwarded-proto.map frontend/nginx.prod.conf > frontend/nginx.prod.conf.tmp
    mv frontend/nginx.prod.conf.tmp frontend/nginx.prod.conf
    sed -i 's/proxy_set_header X-Forwarded-Proto \$scheme;/proxy_set_header X-Forwarded-Proto $forwarded_proto;/g' frontend/nginx.prod.conf
  fi
  docker compose -f "${COMPOSE_FILE}" build frontend
else
  if ! docker compose -f "${COMPOSE_FILE}" pull frontend; then
    echo "Frontend pull failed; building locally with reduced Node heap..."
    export NODE_OPTIONS="--max-old-space-size=384"
    export DOCKER_BUILDKIT=1
    export COMPOSE_DOCKER_CLI_BUILD=1
    docker compose -f "${COMPOSE_FILE}" build frontend
  fi
fi

echo "=== Starting stack ==="
docker compose -f "${COMPOSE_FILE}" up -d --pull never

echo "=== Waiting for Postgres ==="
for i in $(seq 1 60); do
  if docker compose -f "${COMPOSE_FILE}" exec -T db pg_isready -U prioritize -d prioritize >/dev/null 2>&1; then
    echo "Postgres ready (attempt ${i})"
    break
  fi
  sleep 2
done

echo "=== Waiting for backend health ==="
for i in $(seq 1 120); do
  if docker compose -f "${COMPOSE_FILE}" exec -T backend curl -fsS http://localhost:8080/actuator/health 2>/dev/null | grep -q '"status":"UP"'; then
    echo "Backend healthy (attempt ${i})"
    break
  fi
  if [ "$i" -eq 120 ]; then
    echo "Backend not healthy after 120 attempts; continuing anyway"
    docker compose -f "${COMPOSE_FILE}" logs backend --tail 80 || true
  fi
  sleep 5
done

echo "=== Waiting for frontend on port 80 ==="
for i in $(seq 1 60); do
  if curl -fsS -o /dev/null -w "%{http_code}" http://127.0.0.1/ 2>/dev/null | grep -q '200'; then
    echo "Frontend responding (attempt ${i})"
    break
  fi
  if [ "$i" -eq 60 ]; then
    echo "Frontend not responding after 60 attempts"
    docker compose -f "${COMPOSE_FILE}" logs frontend --tail 50 || true
  fi
  sleep 5
done

docker compose -f "${COMPOSE_FILE}" ps
docker compose -f "${COMPOSE_FILE}" logs backend --tail 30 || true

echo "Prioritize deployed at ${APP_URL}" | tee /var/log/prioritize-deploy.log
echo "=== Prioritize bootstrap complete at $(date -Is) ==="

# Build stage — Angular production build (Node 20)
FROM node:20-alpine AS build
WORKDIR /app

ARG API_BASE_URL=http://localhost:8080

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
# Local compose keeps localhost:8080; prod compose passes API_BASE_URL="" for same-origin nginx proxy.
RUN sed -i "s|apiBaseUrl: 'http://localhost:8080'|apiBaseUrl: '${API_BASE_URL}'|" src/environments/environment.ts \
    && npm run build

# Runtime — static SPA via nginx
FROM nginx:alpine
ARG NGINX_CONF=nginx.conf
COPY ${NGINX_CONF} /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/prioritize/browser /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]

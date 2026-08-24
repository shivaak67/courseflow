# Build stage — Angular production build (Node 20)
FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
# apiBaseUrl stays http://localhost:8080 (environment.ts) so the browser
# talks to the host-mapped backend; Google OAuth redirect remains :8080.
RUN npm run build

# Runtime — static SPA via nginx
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/prioritize/browser /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]

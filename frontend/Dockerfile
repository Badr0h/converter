# Multi-stage Dockerfile pour Angular

# Stage 1: Dependencies (cache layer)
FROM node:20-alpine AS deps
WORKDIR /app
COPY package*.json ./
RUN npm ci

# Stage 2: Development (avec hot reload)
FROM node:20-alpine AS development
WORKDIR /app

# Copie des dépendances depuis le stage deps
COPY --from=deps /app/node_modules ./node_modules
COPY . .

# Installation d'angular/cli globalement
RUN npm install -g @angular/cli

EXPOSE 4200

# Démarrage avec hot reload et polling (pour Docker)
CMD ["ng", "serve", "--host", "0.0.0.0", "--poll", "2000", "--disable-host-check"]

# Stage 3: Build (pour la production)
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build -- --configuration production

# Stage 4: Production avec Nginx
FROM nginx:alpine AS production
COPY --from=builder /app/dist/frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
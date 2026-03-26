# ---------- STAGE 1: build ----------
FROM node:20-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .
RUN npm run build


# ---------- STAGE 2: nginx ----------
FROM nginx:alpine

# удаляем дефолтный конфиг
RUN rm -rf /etc/nginx/conf.d/default.conf

# копируем билд
COPY --from=builder /app/dist /usr/share/nginx/html

# копируем свой nginx конфиг
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
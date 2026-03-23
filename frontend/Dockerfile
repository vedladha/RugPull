#---Build Stage---
FROM node:20-slim AS build
WORKDIR /app

# Copy dependencies
COPY package*.json ./
RUN npm install

COPY . .
RUN npm run build

#---Run Stage---
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Remove default nginx files
RUN rm -rf ./*

# Get the app build from the Build Stage
COPY --from=build /app/dist .

COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]

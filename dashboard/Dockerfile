# Dashboard Dockerfile
FROM node:18-alpine

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies (if package.json exists)
RUN if [ -f package.json ]; then npm install; fi

# Copy source code
COPY . .

# Install http-server for serving static files
RUN npm install -g http-server

# Expose port
EXPOSE 3000

# Serve the dashboard
CMD ["http-server", ".", "-p", "3000", "--cors"]

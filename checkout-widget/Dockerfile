# Checkout Widget Dockerfile
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
EXPOSE 3001

# Serve the checkout widget
CMD ["http-server", "src", "-p", "3001", "--cors"]

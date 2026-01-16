# Test Merchant Webhook Receiver
FROM node:18-alpine

WORKDIR /app

# Create a simple webhook receiver
RUN echo 'const express = require("express"); \n\
const app = express(); \n\
const port = process.env.PORT || 4000; \n\
const webhookSecret = process.env.WEBHOOK_SECRET || "whsec_test_abc123"; \n\
\n\
app.use(express.json()); \n\
\n\
app.post("/webhook", (req, res) => { \n\
  console.log("Received webhook:", JSON.stringify(req.body, null, 2)); \n\
  console.log("Headers:", JSON.stringify(req.headers, null, 2)); \n\
  res.json({ received: true, timestamp: new Date().toISOString() }); \n\
}); \n\
\n\
app.get("/health", (req, res) => { \n\
  res.json({ status: "ok", secret: webhookSecret }); \n\
}); \n\
\n\
app.listen(port, () => { \n\
  console.log(`Test merchant webhook receiver listening on port ${port}`); \n\
  console.log(`Webhook secret: ${webhookSecret}`); \n\
});' > server.js

# Install express
RUN npm init -y && npm install express

# Expose port
EXPOSE 4000

# Run the webhook receiver
CMD ["node", "server.js"]

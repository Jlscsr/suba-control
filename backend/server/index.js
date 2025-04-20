// server/index.js
const express = require("express");
const http = require("http");
const { info } = require("../utils/logger.js");
const { PORT, HOST } = require("../config/constant.js");

const { startMouseTracking } = require("./input/mouseHandler.js");
const createWebSocketServer = require("./ws/wsServer.js");

const app = express();
const server = http.createServer(app);

// Add test page for WebSocket testing
app.get("/", (req, res) => {
  res.send(`
    <!DOCTYPE html>
    <html>
    <head>
      <title>WebSocket Test</title>
    </head>
    <body>
      <h1>WebSocket Test</h1>
      <div id="status">Not connected</div>
      <button onclick="connect()">Connect</button>
      <div id="log"></div>
      
      <script>
        function connect() {
          const log = document.getElementById('log');
          const status = document.getElementById('status');
          
          status.textContent = "Connecting...";
          log.innerHTML += "<div>Attempting connection to ws://${HOST}:${PORT}</div>";
          
          const ws = new WebSocket("ws://${HOST}:${PORT}");
          
          ws.onopen = () => {
            status.textContent = "Connected!";
            log.innerHTML += "<div>Connection established!</div>";
          };
          
          ws.onmessage = (e) => {
            log.innerHTML += "<div>Received: " + e.data + "</div>";
          };
          
          ws.onerror = (e) => {
            status.textContent = "Error";
            log.innerHTML += "<div>Error: " + e + "</div>";
          };
          
          ws.onclose = () => {
            status.textContent = "Closed";
            log.innerHTML += "<div>Connection closed</div>";
          };
        }
      </script>
    </body>
    </html>
  `);
});

// Add a basic healthcheck endpoint
app.get("/healthcheck", (req, res) => {
  res.json({ status: "ok", timestamp: new Date().toISOString() });
});

// 🧠 Setup WebSocket server
const ws = createWebSocketServer(server);

// 🖱️ Start native mouse tracking and pipe data to WebSocket
startMouseTracking(ws.broadcast);

// 🌐 Launch server
info(`Attempting to start server on 0.0.0.0:${PORT}`);
// Force it to listen on all interfaces instead of just the HOST value
server.listen(PORT, "0.0.0.0", () => {
  info(`SubaControl backend listening on ws://0.0.0.0:${PORT}`);
  info(`Make sure your phone can reach ${HOST}:${PORT}`);
  info(`Ensure your firewall allows incoming connections on port ${PORT}`);
  info(`Test WebSocket connection at http://${HOST}:${PORT}/`);
});

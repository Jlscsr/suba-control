// server/index.js
const express = require("express");
const http = require("http");
const { info } = require("../utils/logger.js");
const { PORT, HOST } = require("../config/constant.js");

const { startMouseTracking } = require("./input/mouseHandler.js");
const createWebSocketServer = require("./ws/wsServer.js");

const app = express();
const server = http.createServer(app);

// 🧠 Setup WebSocket server
const ws = createWebSocketServer(server);

// 🖱️ Start native mouse tracking and pipe data to WebSocket
startMouseTracking(ws.broadcast);

// 🌐 Launch server
server.listen(PORT, HOST, () => {
  info(`SubaControl backend listening on ws://${HOST}:${PORT}`);
});

// server/ws/wsServer.js
const WebSocket = require("ws");

function createWebSocketServer(server) {
  const wss = new WebSocket.Server({ server });

  const clients = [];

  wss.on("connection", (ws) => {
    console.log("[WS] Phone connected ✅");
    clients.push(ws);

    ws.on("close", () => {
      const index = clients.indexOf(ws);
      if (index !== -1) clients.splice(index, 1);
      console.log("[WS] Phone disconnected ❌");
    });
  });

  function broadcast(data) {
    const json = JSON.stringify(data);
    clients.forEach((client) => client.send(json));
  }

  return { broadcast };
}

module.exports = createWebSocketServer;

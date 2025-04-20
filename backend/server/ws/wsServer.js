// server/ws/wsServer.js
const WebSocket = require("ws");

function createWebSocketServer(server) {
  // Instead of attaching to existing server, create direct WebSocket server
  // that explicitly listens on all network interfaces
  const wss = new WebSocket.Server({
    server: server,
    // Make sure to allow connections from external devices
    perMessageDeflate: false,
  });

  console.log("[WS] WebSocket server started and listening for connections");

  const clients = [];

  wss.on("connection", (ws) => {
    console.log("[WS] Phone connected ✅");
    clients.push(ws);

    // Send test message to confirm connection
    try {
      ws.send(
        JSON.stringify({
          type: "connection_test",
          message: "Connection established",
        })
      );
      console.log("[WS] Sent test message to new client");
    } catch (err) {
      console.log("[WS] Error sending test message:", err);
    }

    // Add message handler for testing
    ws.on("message", (message) => {
      console.log("[WS] Received message:", message.toString());
    });

    ws.on("close", () => {
      const index = clients.indexOf(ws);
      if (index !== -1) clients.splice(index, 1);
      console.log("[WS] Phone disconnected ❌");
    });

    ws.on("error", (error) => {
      console.log("[WS] WebSocket error:", error);
    });
  });

  wss.on("error", (error) => {
    console.log("[WS] Server error:", error);
  });

  function broadcast(data) {
    if (clients.length === 0) {
      console.log("[WS] No clients connected, skipping broadcast");
      return;
    }

    const json = JSON.stringify(data);
    console.log(
      `[WS] Broadcasting to ${clients.length} clients:`,
      json.substring(0, 50) + (json.length > 50 ? "..." : "")
    );

    clients.forEach((client) => {
      if (client.readyState === WebSocket.OPEN) {
        try {
          client.send(json);
        } catch (err) {
          console.log("[WS] Error sending to client:", err);
        }
      }
    });
  }

  return { broadcast };
}

module.exports = createWebSocketServer;

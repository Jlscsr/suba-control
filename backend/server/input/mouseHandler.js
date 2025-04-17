// server/input/mouseHandler.js
const path = require("path");
const nativeMouse = require(path.resolve(
  __dirname,
  "../../hooks/mousehook/build/Release/mousehook.node"
));

let lastMove = { x: 0, y: 0 };

function startMouseTracking(broadcast) {
  nativeMouse.startHook((event) => {
    broadcast(event);

    if (event.type === "move") {
      lastMove = { x: event.x, y: event.y };
      console.log(`[Native Move] at (${event.x}, ${event.y})`);
    } else if (event.type === "click") {
      const dx = event.x - lastMove.x;
      const dy = event.y - lastMove.y;
      const distance = Math.sqrt(dx * dx + dy * dy);
      console.log(
        `[Native Click] at (${event.x}, ${
          event.y
        }) – Distance from last move: ${distance.toFixed(2)} px`
      );
    }
  });
}

module.exports = { startMouseTracking };

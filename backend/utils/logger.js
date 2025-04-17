// utils/logger.js
function log(type, message) {
  const timestamp = new Date().toISOString();
  console.log(`[${type}] ${timestamp} - ${message}`);
}

module.exports = {
  info: (msg) => log("INFO", msg),
  error: (msg) => log("ERROR", msg),
  debug: (msg) => log("DEBUG", msg),
};

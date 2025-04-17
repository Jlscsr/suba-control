const { startHook } = require("bindings")("mousehook");

module.exports = {
  start: (onClick) => startHook(onClick),
};

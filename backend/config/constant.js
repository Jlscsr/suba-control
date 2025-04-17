// config/constants.js
require("dotenv").config(); // Load .env

const HOST = process.env.HOST;
const PORT = process.env.PORT || 8080;

module.exports = { HOST, PORT };

// routes/weatherRoutes.js
const express = require('express');
const router = express.Router();
const { getPersonaInsights } = require('../controllers/weatherController');

router.get('/persona-insights', getPersonaInsights);

module.exports = router;
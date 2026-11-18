// backend/routes/adoptionRoutes.js
const express = require('express');
const router = express.Router();
const { getAdoptions, createAdoption, updateAdoptionStatus } = require('../controllers/adoptionController');

router.get('/', getAdoptions);
router.post('/', createAdoption);
router.put('/:id/status', updateAdoptionStatus);

module.exports = router;
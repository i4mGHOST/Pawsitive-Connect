const express = require('express');
const router = express.Router();
const pool = require('../db');

// GET /api/medical - Retrieve all medical records
router.get('/', async (req, res) => {
    try {
        const result = await pool.query(`
            SELECT id,
                   pet_name,
                   treatment,
                   TO_CHAR(date_administered, 'YYYY-MM-DD') AS date_administered,
                   veterinarian,
                   COALESCE(notes, '') AS notes
            FROM medical_records
            ORDER BY date_administered DESC, id DESC
        `);
        res.status(200).json(result.rows);
    } catch (err) {
        console.error('Error fetching medical records:', err.message);
        res.status(500).json({ error: 'Server error while fetching medical records' });
    }
});

// POST /api/medical - Record a treatment/vaccination for a registered shelter pet
router.post('/', async (req, res) => {
    const { pet_name, treatment, date_administered, veterinarian, notes } = req.body;
    const cleanPet = (pet_name || '').trim();
    const cleanTreatment = (treatment || '').trim();
    const cleanVet = (veterinarian || '').trim();
    const safeDate = date_administered || new Date().toISOString().split('T')[0];

    if (!cleanPet || !cleanTreatment || !cleanVet) {
        return res.status(400).json({ error: 'Pet name, treatment, and veterinarian are required.' });
    }

    try {
        const petCheck = await pool.query(
            'SELECT name FROM pets WHERE LOWER(TRIM(name)) = LOWER(TRIM($1))',
            [cleanPet]
        );
        if (petCheck.rows.length === 0) {
            return res.status(404).json({ error: 'Selected pet is not registered in the shelter database.' });
        }

        const canonicalPetName = petCheck.rows[0].name;
        const result = await pool.query(
            `INSERT INTO medical_records (pet_name, treatment, date_administered, veterinarian, notes)
             VALUES ($1, $2, $3, $4, $5)
             RETURNING id, pet_name, treatment, TO_CHAR(date_administered, 'YYYY-MM-DD') AS date_administered, veterinarian, notes`,
            [canonicalPetName, cleanTreatment, safeDate, cleanVet, notes || '']
        );
        res.status(201).json(result.rows[0]);
    } catch (err) {
        console.error('Error creating medical record:', err);
        res.status(500).json({ error: err.message });
    }
});

module.exports = router;
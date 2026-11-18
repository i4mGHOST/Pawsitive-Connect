const express = require('express');
const router = express.Router();
const pool = require('../db');

function parseRupeeFee(rawFee) {
    if (rawFee === null || rawFee === undefined || rawFee === '') {
        return null;
    }
    const cleaned = String(rawFee).replace(/[₹,\s]/g, '');
    const numeric = Number(cleaned);
    return Number.isFinite(numeric) ? numeric : NaN;
}

function formatPetRow(row) {
    const fee = row.adoption_fee !== null && row.adoption_fee !== undefined
        ? Number(row.adoption_fee)
        : 0;
    return {
        ...row,
        formatted_fee: row.adoption_ready && fee > 0 ? `₹${fee.toFixed(2)}` : null
    };
}

// GET /api/pets - Fetch all shelter pets
router.get('/', async (req, res) => {
    try {
        const result = await pool.query('SELECT * FROM pets ORDER BY id ASC');
        res.status(200).json(result.rows.map(formatPetRow));
    } catch (err) {
        console.error('Error fetching pets:', err);
        res.status(500).json({ error: 'Failed to fetch pets' });
    }
});

// POST /api/pets - Add a unique pet (Admin)
router.post('/', async (req, res) => {
    const { name, species, breed, age, status, image_url, adoption_ready, adoption_fee } = req.body;
    try {
        if (!name || !name.trim()) {
            return res.status(400).json({ error: 'Pet name is required.' });
        }

        const dupCheck = await pool.query(
            'SELECT id FROM pets WHERE LOWER(TRIM(name)) = LOWER(TRIM($1))',
            [name]
        );
        if (dupCheck.rows.length > 0) {
            return res.status(409).json({ error: 'Duplicate pet name already exists.' });
        }

        const isReady = Boolean(adoption_ready);
        const parsedFee = isReady ? parseRupeeFee(adoption_fee) : null;

        if (isReady && (!Number.isFinite(parsedFee) || parsedFee <= 0)) {
            return res.status(400).json({ error: 'Please provide a valid adoption fee greater than ₹0.00.' });
        }

        const result = await pool.query(
            `INSERT INTO pets (name, species, breed, age, status, image_url, adoption_ready, adoption_fee)
             VALUES ($1, $2, $3, $4, $5, $6, $7, $8) RETURNING *`,
            [
                name.trim(),
                species || 'Dog',
                breed || 'Mixed',
                age || 1,
                status || 'Available',
                image_url || null,
                isReady,
                isReady ? parsedFee : null
            ]
        );
        res.status(201).json(formatPetRow(result.rows[0]));
    } catch (err) {
        console.error('Error creating pet:', err);
        res.status(500).json({ error: 'Failed to create pet record' });
    }
});

// PUT /api/pets/:id/adoption - Update adoption readiness & fee (₹)
router.put('/:id/adoption', async (req, res) => {
    const { id } = req.params;
    const { adoption_ready, adoption_fee, status } = req.body;
    try {
        const isReady = Boolean(adoption_ready);
        const parsedFee = isReady ? parseRupeeFee(adoption_fee) : null;

        if (isReady && (!Number.isFinite(parsedFee) || parsedFee <= 0)) {
            return res.status(400).json({ error: 'Please enter a valid adoption fee greater than ₹0.00.' });
        }

        const result = await pool.query(
            `UPDATE pets
             SET adoption_ready = $1,
                 adoption_fee = $2,
                 status = COALESCE($3, status)
             WHERE id = $4
             RETURNING *`,
            [isReady, isReady ? parsedFee : null, status || null, id]
        );
        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'Pet not found' });
        }
        res.status(200).json(formatPetRow(result.rows[0]));
    } catch (err) {
        console.error('Error updating pet adoption config:', err);
        res.status(500).json({ error: 'Failed to update pet adoption settings' });
    }
});

module.exports = router;
const express = require('express');
const router = express.Router();
const pool = require('../db');

// GET /api/rescue - Retrieve all rescue dispatches
router.get('/', async (req, res) => {
    try {
        const result = await pool.query(
            'SELECT * FROM rescue_dispatches ORDER BY report_time DESC, id DESC'
        );
        res.status(200).json(result.rows);
    } catch (err) {
        console.error('Error fetching rescue dispatches:', err.message);
        res.status(500).json({ error: 'Server error while fetching rescue dispatches' });
    }
});

// POST /api/rescue - Create a new emergency dispatch report
router.post('/', async (req, res) => {
    const {
        reporter_name,
        contact_number,
        animal_description,
        location_coordinates,
        urgency_level,
        status
    } = req.body;

    if (!animal_description || !location_coordinates) {
        return res.status(400).json({ error: 'Animal description and location coordinates are required.' });
    }

    try {
        const result = await pool.query(
            `INSERT INTO rescue_dispatches
                (reporter_name, contact_number, animal_description, location_coordinates, urgency_level, status)
             VALUES ($1, $2, $3, $4, $5, $6)
             RETURNING *`,
            [
                (reporter_name || 'Hotline Caller').trim(),
                (contact_number || 'N/A').trim(),
                animal_description.trim(),
                location_coordinates.trim(),
                (urgency_level || 'Medium').trim(),
                (status || 'REPORTED').toUpperCase()
            ]
        );
        res.status(201).json(result.rows[0]);
    } catch (err) {
        console.error('Error creating rescue dispatch:', err);
        res.status(500).json({ error: err.message });
    }
});

// POST /api/rescue/:id/assign - Assign mobile rescue unit to an incident
router.post('/:id/assign', async (req, res) => {
    const { id } = req.params;
    const { status, assigned_unit } = req.body;
    try {
        const result = await pool.query(
            `UPDATE rescue_dispatches
             SET status = $1, assigned_unit = $2
             WHERE id = $3
             RETURNING *`,
            [(status || 'DISPATCHED').toUpperCase(), assigned_unit || 'Unit 03', id]
        );
        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'Rescue incident not found' });
        }
        res.status(200).json({ message: 'Unit assigned successfully', incident: result.rows[0] });
    } catch (err) {
        console.error('Error assigning unit:', err);
        res.status(500).json({ error: err.message });
    }
});

module.exports = router;
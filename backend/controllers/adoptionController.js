const pool = require('../db');

const getAdoptions = async (req, res) => {
    try {
        const query = `
            SELECT application_id,
                   applicant_name,
                   pet_name,
                   UPPER(status) AS status,
                   TO_CHAR(application_date, 'YYYY-MM-DD') AS application_date,
                   COALESCE(fee_amount, 1500.00) AS fee_amount,
                   COALESCE(location, 'Kolkata Metro Shelter') AS location,
                   COALESCE(notes, housing_type, 'Residential & Tenant Permission Verified') AS notes
            FROM adoption_applications
            ORDER BY application_id DESC;
        `;
        const result = await pool.query(query);
        res.status(200).json(result.rows);
    } catch (err) {
        console.error('Error fetching adoption applications:', err);
        res.status(500).json({ error: 'Failed to fetch adoption applications' });
    }
};

const createAdoption = async (req, res) => {
    const { applicant_name, pet_name, status, fee_amount, housing_type, location, notes } = req.body;
    const cleanApplicant = (applicant_name || '').trim();
    const cleanPet = (pet_name || '').trim();

    if (!cleanApplicant || !cleanPet) {
        return res.status(400).json({ error: 'Applicant name and pet name are required.' });
    }

    const client = await pool.connect();
    try {
        await client.query('BEGIN');

        // 1. Verify pet exists and is cleared for adoption
        const petRes = await client.query(
            `SELECT id, name, status, adoption_ready, adoption_fee
             FROM pets
             WHERE LOWER(TRIM(name)) = LOWER(TRIM($1))
             FOR UPDATE`,
            [cleanPet]
        );

        if (petRes.rows.length === 0) {
            await client.query('ROLLBACK');
            return res.status(404).json({ error: 'Selected pet does not exist in the shelter registry.' });
        }

        const pet = petRes.rows[0];
        if (!pet.adoption_ready || Number(pet.adoption_fee) <= 0 || pet.status.toLowerCase() !== 'available') {
            await client.query('ROLLBACK');
            return res.status(400).json({ error: `${pet.name} is not currently available for adoption.` });
        }

        // 2. Prevent duplicate active applications for the SAME pet
        // (The same adopter can still apply for multiple DIFFERENT pets)
        const activeAppCheck = await client.query(
            `SELECT application_id
             FROM adoption_applications
             WHERE LOWER(TRIM(pet_name)) = LOWER(TRIM($1))
               AND UPPER(status) IN ('PENDING', 'APPROVED', 'COMPLETED')`,
            [pet.name]
        );

        if (activeAppCheck.rows.length > 0) {
            await client.query('ROLLBACK');
            return res.status(409).json({
                error: `An active adoption application already exists for ${pet.name}.`
            });
        }

        const verifiedFee = Number(pet.adoption_fee) > 0
            ? Number(pet.adoption_fee)
            : (Number(fee_amount) || 1500.00);
        const safeHousing = (housing_type || 'Single-Family Home with Yard').trim();
        const safeLocation = (location || 'Kolkata Metro Shelter').trim();
        const safeNotes = (notes || safeHousing).trim();
        const initialStatus = (status || 'PENDING').toUpperCase();

        const insertRes = await client.query(
            `INSERT INTO adoption_applications
                (applicant_name, pet_name, status, fee_amount, housing_type, location, notes)
             VALUES ($1, $2, $3, $4, $5, $6, $7)
             RETURNING *`,
            [cleanApplicant, pet.name, initialStatus, verifiedFee, safeHousing, safeLocation, safeNotes]
        );

        // 3. Mark pet as Pending Adoption while application is active
        await client.query(
            `UPDATE pets SET status = 'Pending Adoption' WHERE id = $1`,
            [pet.id]
        );

        await client.query('COMMIT');
        res.status(201).json(insertRes.rows[0]);
    } catch (err) {
        await client.query('ROLLBACK');
        console.error('Error creating adoption application:', err);
        res.status(500).json({ error: 'Failed to create adoption application' });
    } finally {
        client.release();
    }
};

const updateAdoptionStatus = async (req, res) => {
    const { id } = req.params;
    const { status, fee_amount } = req.body;
    const nextStatus = (status || '').toUpperCase();

    if (!nextStatus) {
        return res.status(400).json({ error: 'Status is required.' });
    }

    const client = await pool.connect();
    try {
        await client.query('BEGIN');

        const existingRes = await client.query(
            'SELECT * FROM adoption_applications WHERE application_id = $1 FOR UPDATE',
            [id]
        );
        if (existingRes.rows.length === 0) {
            await client.query('ROLLBACK');
            return res.status(404).json({ error: 'Adoption application not found.' });
        }

        const appRow = existingRes.rows[0];
        const hasNewFee = fee_amount !== undefined && fee_amount !== null && !Number.isNaN(Number(fee_amount));

        if (hasNewFee) {
            await client.query(
                `UPDATE adoption_applications SET status = $1, fee_amount = $2 WHERE application_id = $3`,
                [nextStatus, Number(fee_amount), id]
            );
        } else {
            await client.query(
                `UPDATE adoption_applications SET status = $1 WHERE application_id = $2`,
                [nextStatus, id]
            );
        }

        // Synchronize pet registry status with application status
        if (nextStatus === 'APPROVED') {
            if (hasNewFee) {
                await client.query(
                    `UPDATE pets
                     SET status = 'Pending Adoption', adoption_fee = $1
                     WHERE LOWER(TRIM(name)) = LOWER(TRIM($2))`,
                    [Number(fee_amount), appRow.pet_name]
                );
            } else {
                await client.query(
                    `UPDATE pets
                     SET status = 'Pending Adoption'
                     WHERE LOWER(TRIM(name)) = LOWER(TRIM($1))`,
                    [appRow.pet_name]
                );
            }
        } else if (nextStatus === 'REJECTED' || nextStatus === 'CANCELLED') {
            await client.query(
                `UPDATE pets
                 SET status = CASE WHEN adoption_ready = TRUE THEN 'Available' ELSE status END
                 WHERE LOWER(TRIM(name)) = LOWER(TRIM($1))
                   AND LOWER(status) = 'pending adoption'`,
                [appRow.pet_name]
            );
        } else if (nextStatus === 'COMPLETED') {
            await client.query(
                `UPDATE pets
                 SET status = 'Adopted', adoption_ready = FALSE
                 WHERE LOWER(TRIM(name)) = LOWER(TRIM($1))`,
                [appRow.pet_name]
            );
        }

        await client.query('COMMIT');
        res.status(200).json({ message: 'Status updated successfully to ' + nextStatus });
    } catch (err) {
        await client.query('ROLLBACK');
        console.error('Error updating status:', err);
        res.status(500).json({ error: 'Failed to update status' });
    } finally {
        client.release();
    }
};

module.exports = { getAdoptions, createAdoption, updateAdoptionStatus };
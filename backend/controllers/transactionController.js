const pool = require('../db');

const getTransactions = async (req, res) => {
    try {
        const result = await pool.query(`
            SELECT transaction_id,
                   application_id,
                   payer_name,
                   amount,
                   payment_method,
                   COALESCE(category, 'Donation') AS category,
                   UPPER(status) AS status,
                   TO_CHAR(transaction_date, 'YYYY-MM-DD') AS transaction_date
            FROM transactions
            ORDER BY transaction_id DESC;
        `);
        res.status(200).json(result.rows);
    } catch (err) {
        console.error('Error fetching transactions:', err);
        res.status(500).json({ error: 'Failed to fetch transactions' });
    }
};

const createTransaction = async (req, res) => {
    const { payer_name, amount, payment_method, category, status } = req.body;
    const cleanPayer = (payer_name || '').trim();
    const parsedAmount = Number(amount);

    if (!cleanPayer || Number.isNaN(parsedAmount) || parsedAmount <= 0) {
        return res.status(400).json({ error: 'Valid payer name and positive amount are required.' });
    }

    try {
        const result = await pool.query(
            `INSERT INTO transactions (payer_name, amount, payment_method, category, status)
             VALUES ($1, $2, $3, $4, $5)
             RETURNING *`,
            [
                cleanPayer,
                parsedAmount,
                payment_method || 'UPI',
                category || 'Donation',
                (status || 'PENDING').toUpperCase()
            ]
        );
        res.status(201).json(result.rows[0]);
    } catch (err) {
        console.error('Error creating transaction:', err);
        res.status(500).json({ error: 'Failed to record transaction' });
    }
};

const payPendingTransaction = async (req, res) => {
    const { id } = req.params;
    const { payment_method } = req.body;
    const client = await pool.connect();

    try {
        await client.query('BEGIN');

        const result = await client.query(
            `UPDATE transactions
             SET status = 'COMPLETED',
                 payment_method = COALESCE($1, payment_method),
                 transaction_date = CURRENT_TIMESTAMP
             WHERE transaction_id = $2
               AND UPPER(status) = 'PENDING'
             RETURNING *`,
            [payment_method || null, id]
        );

        if (result.rows.length === 0) {
            await client.query('ROLLBACK');
            return res.status(400).json({ error: 'Pending transaction not found or already processed.' });
        }

        const txn = result.rows[0];
        if (txn.application_id) {
            const appRes = await client.query(
                `UPDATE adoption_applications
                 SET status = 'COMPLETED'
                 WHERE application_id = $1
                 RETURNING pet_name`,
                [txn.application_id]
            );
            if (appRes.rows.length > 0) {
                await client.query(
                    `UPDATE pets
                     SET status = 'Adopted', adoption_ready = FALSE
                     WHERE LOWER(TRIM(name)) = LOWER(TRIM($1))`,
                    [appRes.rows[0].pet_name]
                );
            }
        }

        await client.query('COMMIT');
        res.status(200).json(txn);
    } catch (err) {
        await client.query('ROLLBACK');
        console.error('Error settling pending transaction:', err);
        res.status(500).json({ error: 'Failed to process pending payment' });
    } finally {
        client.release();
    }
};

const cancelPendingTransaction = async (req, res) => {
    const { id } = req.params;
    try {
        const result = await pool.query(
            `UPDATE transactions
             SET status = 'CANCELLED'
             WHERE transaction_id = $1
               AND UPPER(status) = 'PENDING'
             RETURNING *`,
            [id]
        );
        if (result.rows.length === 0) {
            return res.status(400).json({ error: 'Pending transaction not found.' });
        }
        res.status(200).json(result.rows[0]);
    } catch (err) {
        console.error('Error cancelling pending transaction:', err);
        res.status(500).json({ error: 'Failed to cancel payment' });
    }
};

const processPayment = async (req, res) => {
    const { application_id, payer_name, amount, payment_method } = req.body;
    const client = await pool.connect();

    try {
        await client.query('BEGIN');

        const appCheck = await client.query(
            `SELECT status, pet_name, fee_amount
             FROM adoption_applications
             WHERE application_id = $1
             FOR UPDATE`,
            [application_id]
        );

        if (appCheck.rows.length === 0 || appCheck.rows[0].status.toUpperCase() !== 'APPROVED') {
            await client.query('ROLLBACK');
            return res.status(400).json({
                error: 'Payment rejected: Application is not approved or pet is unavailable.'
            });
        }

        const petName = appCheck.rows[0].pet_name;
        const settledAmount = Number(amount) > 0 ? Number(amount) : Number(appCheck.rows[0].fee_amount || 1500.00);

        await client.query(
            `INSERT INTO transactions (application_id, payer_name, amount, payment_method, category, status)
             VALUES ($1, $2, $3, $4, 'Adoption Fee', 'COMPLETED')`,
            [application_id, payer_name, settledAmount, payment_method || 'UPI']
        );

        await client.query(
            `UPDATE adoption_applications SET status = 'COMPLETED' WHERE application_id = $1`,
            [application_id]
        );

        await client.query(
            `UPDATE pets
             SET status = 'Adopted', adoption_ready = FALSE
             WHERE LOWER(TRIM(name)) = LOWER(TRIM($1))`,
            [petName]
        );

        await client.query('COMMIT');
        res.status(201).json({ message: 'Payment processed successfully!' });
    } catch (err) {
        await client.query('ROLLBACK');
        console.error('Transaction error:', err);
        res.status(500).json({ error: 'Payment processing failed due to server error.' });
    } finally {
        client.release();
    }
};

module.exports = {
    getTransactions,
    createTransaction,
    payPendingTransaction,
    cancelPendingTransaction,
    processPayment
};
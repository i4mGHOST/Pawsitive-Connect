CREATE TABLE IF NOT EXISTS transactions (
    transaction_id SERIAL PRIMARY KEY,
    application_id INT REFERENCES adoption_applications(application_id) ON DELETE SET NULL,
    payer_name VARCHAR(120) NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    payment_method VARCHAR(60) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'COMPLETED',
    category VARCHAR(60) NOT NULL DEFAULT 'Adoption Fee',
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE transactions ADD COLUMN IF NOT EXISTS application_id INT;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS category VARCHAR(60) NOT NULL DEFAULT 'Adoption Fee';
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS status VARCHAR(50) NOT NULL DEFAULT 'COMPLETED';
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Ensure foreign key always uses ON DELETE SET NULL even on pre-existing tables
ALTER TABLE transactions DROP CONSTRAINT IF EXISTS transactions_application_id_fkey;
ALTER TABLE transactions
    ADD CONSTRAINT transactions_application_id_fkey
    FOREIGN KEY (application_id)
    REFERENCES adoption_applications(application_id)
    ON DELETE SET NULL;

INSERT INTO transactions (application_id, payer_name, amount, payment_method, status, category)
SELECT NULL, 'Rohan Deshmukh', 2500.00, 'UPI', 'COMPLETED', 'Donation'
WHERE NOT EXISTS (
    SELECT 1 FROM transactions WHERE payer_name = 'Rohan Deshmukh' AND amount = 2500.00
);

INSERT INTO transactions (application_id, payer_name, amount, payment_method, status, category)
SELECT NULL, 'Priya Chatterjee', 1800.00, 'Net Banking / IMPS', 'PENDING', 'Medical Sponsorship'
WHERE NOT EXISTS (
    SELECT 1 FROM transactions WHERE payer_name = 'Priya Chatterjee' AND amount = 1800.00
);
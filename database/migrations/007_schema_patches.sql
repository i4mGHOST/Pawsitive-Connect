-- 1. Guarantee ON DELETE SET NULL on transactions -> adoption_applications before any cleanup
ALTER TABLE transactions DROP CONSTRAINT IF EXISTS transactions_application_id_fkey;
ALTER TABLE transactions
    ADD CONSTRAINT transactions_application_id_fkey
    FOREIGN KEY (application_id)
    REFERENCES adoption_applications(application_id)
    ON DELETE SET NULL;

-- 2. Remove duplicate rows in pets, medical_records, and rescue_dispatches
DELETE FROM pets a USING pets b
WHERE a.id > b.id AND LOWER(TRIM(a.name)) = LOWER(TRIM(b.name));

DELETE FROM medical_records a USING medical_records b
WHERE a.id > b.id
  AND LOWER(TRIM(a.pet_name)) = LOWER(TRIM(b.pet_name))
  AND LOWER(TRIM(a.treatment)) = LOWER(TRIM(b.treatment))
  AND a.date_administered = b.date_administered;

DELETE FROM rescue_dispatches a USING rescue_dispatches b
WHERE a.id > b.id
  AND LOWER(TRIM(a.reporter_name)) = LOWER(TRIM(b.reporter_name))
  AND LOWER(TRIM(a.animal_description)) = LOWER(TRIM(b.animal_description));

-- 3. Upgrade any legacy Western seed names in PostgreSQL to Indian names
UPDATE medical_records SET veterinarian = 'Dr. Ananya Verma' WHERE veterinarian = 'Dr. Sarah Jenkins';
UPDATE medical_records SET veterinarian = 'Dr. Rohan Deshmukh' WHERE veterinarian = 'Dr. Mark Sloan';
UPDATE medical_records SET veterinarian = 'Dr. Kavita Nair' WHERE veterinarian = 'Dr. Emily Chen';

UPDATE rescue_dispatches SET reporter_name = 'Vikram Malhotra' WHERE reporter_name = 'Marcus Chen';
UPDATE rescue_dispatches SET reporter_name = 'Priya Chatterjee' WHERE reporter_name = 'Elena Rodriguez';
UPDATE rescue_dispatches SET reporter_name = 'Arjun Kulkarni' WHERE reporter_name = 'David Kim';
UPDATE rescue_dispatches SET reporter_name = 'Sneha Iyer' WHERE reporter_name = 'Sarah Jenkins';

UPDATE adoption_applications SET applicant_name = 'Aarav Sharma' WHERE applicant_name = 'Sarah Williams';
UPDATE adoption_applications SET applicant_name = 'Meera Krishnan' WHERE applicant_name = 'Daniel Brooks';
UPDATE transactions SET payer_name = 'Aarav Sharma' WHERE payer_name = 'Sarah Williams';

-- Remove legacy unpaid seed transaction if Aarav Sharma's adoption application is still APPROVED
DELETE FROM transactions
WHERE payer_name = 'Aarav Sharma'
  AND category = 'Adoption Fee'
  AND amount = 1500.00
  AND EXISTS (
      SELECT 1 FROM adoption_applications
      WHERE applicant_name = 'Aarav Sharma'
        AND UPPER(status) = 'APPROVED'
  );

-- 4. Detach transaction references for orphaned/duplicate applications, then clean them up
UPDATE transactions
SET application_id = NULL
WHERE application_id IN (
    SELECT application_id FROM adoption_applications
    WHERE LOWER(TRIM(pet_name)) NOT IN (SELECT LOWER(TRIM(name)) FROM pets)
);

DELETE FROM adoption_applications
WHERE LOWER(TRIM(pet_name)) NOT IN (SELECT LOWER(TRIM(name)) FROM pets);

-- Keep only the earliest active (PENDING/APPROVED/COMPLETED) application per pet
-- (One adopter can still apply for multiple different pets)
UPDATE transactions
SET application_id = NULL
WHERE application_id IN (
    SELECT a.application_id
    FROM adoption_applications a
    JOIN adoption_applications b
      ON a.application_id > b.application_id
     AND LOWER(TRIM(a.pet_name)) = LOWER(TRIM(b.pet_name))
     AND UPPER(a.status) IN ('PENDING', 'APPROVED', 'COMPLETED')
     AND UPPER(b.status) IN ('PENDING', 'APPROVED', 'COMPLETED')
);

DELETE FROM adoption_applications a USING adoption_applications b
WHERE a.application_id > b.application_id
  AND LOWER(TRIM(a.pet_name)) = LOWER(TRIM(b.pet_name))
  AND UPPER(a.status) IN ('PENDING', 'APPROVED', 'COMPLETED')
  AND UPPER(b.status) IN ('PENDING', 'APPROVED', 'COMPLETED');

-- 5. Add missing columns, unique pet name index, and synchronize pet statuses
ALTER TABLE pets ADD COLUMN IF NOT EXISTS image_url TEXT;
ALTER TABLE pets ADD COLUMN IF NOT EXISTS adoption_ready BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE pets ADD COLUMN IF NOT EXISTS adoption_fee NUMERIC(10, 2);
ALTER TABLE rescue_dispatches ADD COLUMN IF NOT EXISTS assigned_unit VARCHAR(120);

CREATE UNIQUE INDEX IF NOT EXISTS idx_pets_unique_name ON pets (LOWER(TRIM(name)));

UPDATE pets SET adoption_ready = TRUE, adoption_fee = 1500.00
WHERE LOWER(TRIM(name)) = 'max' AND (adoption_fee IS NULL OR adoption_fee = 150.00);

UPDATE pets SET adoption_ready = TRUE, adoption_fee = 1200.00
WHERE LOWER(TRIM(name)) = 'luna' AND (adoption_fee IS NULL OR adoption_fee = 120.00);

UPDATE pets
SET status = 'Pending Adoption'
WHERE LOWER(TRIM(name)) IN (
    SELECT LOWER(TRIM(pet_name))
    FROM adoption_applications
    WHERE UPPER(status) IN ('PENDING', 'APPROVED')
)
AND LOWER(status) NOT IN ('in medical care', 'adopted');

UPDATE pets
SET status = 'Adopted', adoption_ready = FALSE
WHERE LOWER(TRIM(name)) IN (
    SELECT LOWER(TRIM(pet_name))
    FROM adoption_applications
    WHERE UPPER(status) = 'COMPLETED'
);
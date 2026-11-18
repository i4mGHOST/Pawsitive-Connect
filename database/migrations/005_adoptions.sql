CREATE TABLE IF NOT EXISTS adoption_applications (
    application_id SERIAL PRIMARY KEY,
    applicant_name VARCHAR(120) NOT NULL,
    pet_name VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    application_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fee_amount NUMERIC(10, 2) NOT NULL DEFAULT 1500.00,
    housing_type VARCHAR(120) DEFAULT 'Standard',
    location VARCHAR(150) DEFAULT 'Kolkata Shelter Ward',
    notes TEXT DEFAULT 'Residential & Tenant Permission Verified'
);

ALTER TABLE adoption_applications ADD COLUMN IF NOT EXISTS fee_amount NUMERIC(10, 2) NOT NULL DEFAULT 1500.00;
ALTER TABLE adoption_applications ADD COLUMN IF NOT EXISTS housing_type VARCHAR(120) DEFAULT 'Standard';
ALTER TABLE adoption_applications ADD COLUMN IF NOT EXISTS location VARCHAR(150) DEFAULT 'Kolkata Shelter Ward';
ALTER TABLE adoption_applications ADD COLUMN IF NOT EXISTS notes TEXT DEFAULT 'Residential & Tenant Permission Verified';
ALTER TABLE adoption_applications ADD COLUMN IF NOT EXISTS application_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

INSERT INTO adoption_applications (applicant_name, pet_name, status, fee_amount, housing_type, location, notes)
SELECT 'Aarav Sharma', 'Max', 'APPROVED', 1500.00, 'Independent House with Courtyard', 'Kolkata Shelter Ward', 'Residential & Tenant Permission Verified'
WHERE NOT EXISTS (
    SELECT 1 FROM adoption_applications WHERE LOWER(TRIM(pet_name)) = 'max' AND UPPER(status) IN ('PENDING', 'APPROVED', 'COMPLETED')
);

INSERT INTO adoption_applications (applicant_name, pet_name, status, fee_amount, housing_type, location, notes)
SELECT 'Meera Krishnan', 'Luna', 'PENDING', 1200.00, 'Apartment', 'Salt Lake Foster Hub', 'Pending society pet-policy verification'
WHERE NOT EXISTS (
    SELECT 1 FROM adoption_applications WHERE LOWER(TRIM(pet_name)) = 'luna' AND UPPER(status) IN ('PENDING', 'APPROVED', 'COMPLETED')
);
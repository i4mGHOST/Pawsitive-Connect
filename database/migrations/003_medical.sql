CREATE TABLE IF NOT EXISTS medical_records (
    id SERIAL PRIMARY KEY,
    pet_name VARCHAR(100) NOT NULL,
    treatment VARCHAR(150) NOT NULL,
    date_administered DATE NOT NULL,
    veterinarian VARCHAR(100) NOT NULL,
    notes TEXT
);

INSERT INTO medical_records (pet_name, treatment, date_administered, veterinarian, notes)
SELECT 'Max', 'Rabies Vaccine', '2026-08-15', 'Dr. Ananya Verma', 'Annual booster. Healthy condition.'
WHERE NOT EXISTS (SELECT 1 FROM medical_records WHERE LOWER(TRIM(pet_name)) = 'max' AND treatment = 'Rabies Vaccine');

INSERT INTO medical_records (pet_name, treatment, date_administered, veterinarian, notes)
SELECT 'Luna', 'Flea & Tick Treatment', '2026-09-10', 'Dr. Rohan Deshmukh', 'Applied topical treatment.'
WHERE NOT EXISTS (SELECT 1 FROM medical_records WHERE LOWER(TRIM(pet_name)) = 'luna' AND treatment = 'Flea & Tick Treatment');

INSERT INTO medical_records (pet_name, treatment, date_administered, veterinarian, notes)
SELECT 'Milo', 'X-Ray & Leg Splint', '2026-09-22', 'Dr. Ananya Verma', 'Fractured front left paw. Resting.'
WHERE NOT EXISTS (SELECT 1 FROM medical_records WHERE LOWER(TRIM(pet_name)) = 'milo' AND treatment = 'X-Ray & Leg Splint');

INSERT INTO medical_records (pet_name, treatment, date_administered, veterinarian, notes)
SELECT 'Bella', 'Spay Surgery', '2026-09-05', 'Dr. Kavita Nair', 'Routine surgery, recovered beautifully.'
WHERE NOT EXISTS (SELECT 1 FROM medical_records WHERE LOWER(TRIM(pet_name)) = 'bella' AND treatment = 'Spay Surgery');

INSERT INTO medical_records (pet_name, treatment, date_administered, veterinarian, notes)
SELECT 'Rocky', 'Deworming', '2026-09-18', 'Dr. Rohan Deshmukh', 'Standard intake procedure.'
WHERE NOT EXISTS (SELECT 1 FROM medical_records WHERE LOWER(TRIM(pet_name)) = 'rocky' AND treatment = 'Deworming');
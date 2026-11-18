CREATE TABLE IF NOT EXISTS pets (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    species VARCHAR(50) NOT NULL,
    breed VARCHAR(100) NOT NULL,
    age INT NOT NULL,
    status VARCHAR(50) DEFAULT 'Available',
    image_url TEXT,
    adoption_ready BOOLEAN NOT NULL DEFAULT FALSE,
    adoption_fee NUMERIC(10, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE pets ADD COLUMN IF NOT EXISTS image_url TEXT;
ALTER TABLE pets ADD COLUMN IF NOT EXISTS adoption_ready BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE pets ADD COLUMN IF NOT EXISTS adoption_fee NUMERIC(10, 2);

INSERT INTO pets (name, species, breed, age, status, adoption_ready, adoption_fee)
SELECT 'Max', 'Dog', 'Golden Retriever', 2, 'Pending Adoption', TRUE, 1500.00
WHERE NOT EXISTS (SELECT 1 FROM pets WHERE LOWER(TRIM(name)) = 'max');

INSERT INTO pets (name, species, breed, age, status, adoption_ready, adoption_fee)
SELECT 'Luna', 'Cat', 'Siamese', 1, 'Pending Adoption', TRUE, 1200.00
WHERE NOT EXISTS (SELECT 1 FROM pets WHERE LOWER(TRIM(name)) = 'luna');

INSERT INTO pets (name, species, breed, age, status, adoption_ready, adoption_fee)
SELECT 'Bella', 'Dog', 'Indian Pariah', 3, 'Available', TRUE, 1800.00
WHERE NOT EXISTS (SELECT 1 FROM pets WHERE LOWER(TRIM(name)) = 'bella');

INSERT INTO pets (name, species, breed, age, status, adoption_ready, adoption_fee)
SELECT 'Rocky', 'Dog', 'Beagle', 2, 'Available', TRUE, 1400.00
WHERE NOT EXISTS (SELECT 1 FROM pets WHERE LOWER(TRIM(name)) = 'rocky');

INSERT INTO pets (name, species, breed, age, status, adoption_ready, adoption_fee)
SELECT 'Milo', 'Cat', 'Persian Mix', 1, 'In Medical Care', FALSE, NULL
WHERE NOT EXISTS (SELECT 1 FROM pets WHERE LOWER(TRIM(name)) = 'milo');
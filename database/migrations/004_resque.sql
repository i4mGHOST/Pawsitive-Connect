CREATE TABLE IF NOT EXISTS rescue_dispatches (
    id SERIAL PRIMARY KEY,
    reporter_name VARCHAR(100) NOT NULL,
    contact_number VARCHAR(20),
    animal_description VARCHAR(255) NOT NULL,
    location_coordinates VARCHAR(100) NOT NULL,
    urgency_level VARCHAR(50) NOT NULL,
    status VARCHAR(50) DEFAULT 'REPORTED',
    assigned_unit VARCHAR(120),
    report_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE rescue_dispatches ADD COLUMN IF NOT EXISTS assigned_unit VARCHAR(120);
ALTER TABLE rescue_dispatches ADD COLUMN IF NOT EXISTS report_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

INSERT INTO rescue_dispatches (reporter_name, contact_number, animal_description, location_coordinates, urgency_level, status)
SELECT 'Vikram Malhotra', '+91 98201 44192', 'Stray Golden Retriever mix, visibly limping', '22.5726, 88.3639', 'High', 'REPORTED'
WHERE NOT EXISTS (SELECT 1 FROM rescue_dispatches WHERE reporter_name = 'Vikram Malhotra' AND animal_description = 'Stray Golden Retriever mix, visibly limping');

INSERT INTO rescue_dispatches (reporter_name, contact_number, animal_description, location_coordinates, urgency_level, status)
SELECT 'Priya Chatterjee', '+91 98304 55834', 'Kitten trapped inside storm drain', '22.5867, 88.4172', 'Critical', 'DISPATCHED'
WHERE NOT EXISTS (SELECT 1 FROM rescue_dispatches WHERE reporter_name = 'Priya Chatterjee' AND animal_description = 'Kitten trapped inside storm drain');

INSERT INTO rescue_dispatches (reporter_name, contact_number, animal_description, location_coordinates, urgency_level, status)
SELECT 'Arjun Kulkarni', '+91 98112 77211', 'Injured street dog near highway junction', '22.5448, 88.3426', 'Medium', 'REPORTED'
WHERE NOT EXISTS (SELECT 1 FROM rescue_dispatches WHERE reporter_name = 'Arjun Kulkarni' AND animal_description = 'Injured street dog near highway junction');

INSERT INTO rescue_dispatches (reporter_name, contact_number, animal_description, location_coordinates, urgency_level, status)
SELECT 'Sneha Iyer', '+91 98409 66999', 'Injured kite bird on roadside', '22.5179, 88.3834', 'High', 'DISPATCHED'
WHERE NOT EXISTS (SELECT 1 FROM rescue_dispatches WHERE reporter_name = 'Sneha Iyer' AND animal_description = 'Injured kite bird on roadside');
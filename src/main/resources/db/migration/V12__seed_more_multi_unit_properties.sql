-- More seed data to round out the catalogue:
--   * 10 Midrand apartment blocks (premium estates / lifestyle complexes),
--     each with 3-4 available units of varying size + price.
--   * 5 Pretoria-central apartment blocks (Sunnyside / Arcadia / Hatfield /
--     Brooklyn / Menlyn), each with 3-4 units.
--   * 5 Midrand plots / smallholding "farms" (Glen Austin, Beaulieu,
--     Bushwillow Park, President Park, Klipdrift) — PropertyType=PLOT,
--     each with a main HOUSE unit plus 1-3 COTTAGE / FLATLET secondaries.
--
-- All listings continue to point at user #2 (Thandi) as landlord + manager.
-- Idempotent via md5-derived UUIDs and ON CONFLICT DO NOTHING.

-- ── Properties ───────────────────────────────────────────────────────
INSERT INTO properties (
    id, landlord_id, manager_id, title, description, property_type, status,
    address_line, suburb, city, province, postal_code, latitude, longitude,
    created_at, updated_at
) VALUES
-- Midrand apartment blocks (props 31–40)
(md5('hab-prop-31')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Carlswald Apartments',         'Boutique block in Carlswald with gym, fibre, and a residents'' rooftop braai.',
 'APARTMENT_BLOCK', 'LISTED', '5 New Road', 'Carlswald',        'Midrand', 'Gauteng', '1684', -26.0001, 28.1118, NOW(), NOW()),
(md5('hab-prop-32')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Halfway House Residence',      'Modern complex in Halfway House. Concierge, secure parking.',
 'APARTMENT_BLOCK', 'LISTED', '12 Allandale Road', 'Halfway House', 'Midrand', 'Gauteng', '1685', -25.9928, 28.1267, NOW(), NOW()),
(md5('hab-prop-33')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'The Vorna · Vorna Valley',     'Lifestyle block in Vorna Valley walking distance to the Gautrain feeder.',
 'APARTMENT_BLOCK', 'LISTED', '40 Le Roux Avenue', 'Vorna Valley', 'Midrand', 'Gauteng', '1686', -25.9994, 28.1224, NOW(), NOW()),
(md5('hab-prop-34')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Waterval Estate Apartments',   'Apartment cluster in Waterval Equestrian Estate. Backup power across the block.',
 'APARTMENT_BLOCK', 'LISTED', '8 Waterval Road', 'Waterval Estate', 'Midrand', 'Gauteng', '1685', -25.9952, 28.0984, NOW(), NOW()),
(md5('hab-prop-35')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Crowthorne Heights',           'Quiet apartment block on the edge of Crowthorne smallholdings.',
 'APARTMENT_BLOCK', 'LISTED', '11 Acorn Road', 'Crowthorne', 'Midrand', 'Gauteng', '1684', -25.9678, 28.0879, NOW(), NOW()),
(md5('hab-prop-36')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Country View Apartments',      'Family-oriented block in Country View. Pool, playground, clubhouse.',
 'APARTMENT_BLOCK', 'LISTED', '22 Country View Drive', 'Country View', 'Midrand', 'Gauteng', '1685', -25.9418, 28.1037, NOW(), NOW()),
(md5('hab-prop-37')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Noordwyk Residence',           'New-build apartment block in Noordwyk. Inverter system, secure access.',
 'APARTMENT_BLOCK', 'LISTED', '7 Olympic Duel Road', 'Noordwyk', 'Midrand', 'Gauteng', '1687', -26.0078, 28.1442, NOW(), NOW()),
(md5('hab-prop-38')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Halfway Gardens Apartments',   'Established block in Halfway Gardens with mature trees and a quiet courtyard.',
 'APARTMENT_BLOCK', 'LISTED', '15 Limpopo Street', 'Halfway Gardens', 'Midrand', 'Gauteng', '1685', -26.0058, 28.1310, NOW(), NOW()),
(md5('hab-prop-39')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Kyalami Residences',           'Upmarket block bordering the Kyalami racetrack. Apartment finishes throughout.',
 'APARTMENT_BLOCK', 'LISTED', '3 Kyalami Boulevard', 'Kyalami', 'Midrand', 'Gauteng', '1684', -26.0227, 28.0654, NOW(), NOW()),
(md5('hab-prop-40')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'President Park Apartments',    'Compact block in President Park. Walking distance to amenities.',
 'APARTMENT_BLOCK', 'LISTED', '9 Andries Pretorius Road', 'President Park', 'Midrand', 'Gauteng', '1685', -25.9608, 28.1457, NOW(), NOW()),
-- Pretoria central apartment blocks (props 41–45)
(md5('hab-prop-41')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Sunnyside Apartments',         'Established block in Sunnyside. Walking distance to UP main campus.',
 'APARTMENT_BLOCK', 'LISTED', '120 Esselen Street', 'Sunnyside', 'Pretoria', 'Gauteng', '0002', -25.7568, 28.2069, NOW(), NOW()),
(md5('hab-prop-42')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Arcadia Court',                'Quiet block in Arcadia with embassy-quarter proximity.',
 'APARTMENT_BLOCK', 'LISTED', '457 Park Street', 'Arcadia', 'Pretoria', 'Gauteng', '0083', -25.7459, 28.2316, NOW(), NOW()),
(md5('hab-prop-43')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Hatfield Heights',             'High-rise block at the heart of Hatfield. Gautrain station two minutes away.',
 'APARTMENT_BLOCK', 'LISTED', '1102 Burnett Street', 'Hatfield', 'Pretoria', 'Gauteng', '0083', -25.7479, 28.2390, NOW(), NOW()),
(md5('hab-prop-44')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Brooklyn Square Residences',   'Boutique block above the Brooklyn Square shopping centre.',
 'APARTMENT_BLOCK', 'LISTED', '337 Bronkhorst Street', 'Brooklyn', 'Pretoria', 'Gauteng', '0181', -25.7665, 28.2386, NOW(), NOW()),
(md5('hab-prop-45')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Menlyn Maine Apartments',      'New-build block in Menlyn Maine. Gym, pool, business lounge on-site.',
 'APARTMENT_BLOCK', 'LISTED', '12 Aramist Avenue', 'Menlyn', 'Pretoria', 'Gauteng', '0181', -25.7846, 28.2769, NOW(), NOW()),
-- Midrand plots / smallholdings (props 46–50)
(md5('hab-prop-46')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Glen Austin Country Estate',   'Three-house Glen Austin smallholding. Main house plus two flatlets — borehole, paddocks, dam.',
 'PLOT', 'LISTED', '32 Plot Road', 'Glen Austin', 'Midrand', 'Gauteng', '1685', -25.9572, 28.1378, NOW(), NOW()),
(md5('hab-prop-47')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Beaulieu Equestrian Plot',     '4ha plot in Beaulieu with a main farmhouse and three rentable cottages. Stables on-site.',
 'PLOT', 'LISTED', '14 Beaulieu Road', 'Beaulieu', 'Midrand', 'Gauteng', '1684', -25.9714, 28.0712, NOW(), NOW()),
(md5('hab-prop-48')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Bushwillow Park Smallholding', '2ha plot in Bushwillow Park. Main house plus a one-bed cottage. Generator-ready.',
 'PLOT', 'LISTED', '6 Bushwillow Crescent', 'Bushwillow Park', 'Midrand', 'Gauteng', '1685', -25.9495, 28.0987, NOW(), NOW()),
(md5('hab-prop-49')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'President Park Smallholding',  '3ha smallholding in President Park. Three rentable units across the plot.',
 'PLOT', 'LISTED', '21 Andries Pretorius Road', 'President Park', 'Midrand', 'Gauteng', '1685', -25.9601, 28.1466, NOW(), NOW()),
(md5('hab-prop-50')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Klipdrift Farm Estate',        '8ha working smallholding bordering Klipdrift. Main farmhouse plus three cottages.',
 'PLOT', 'LISTED', '4 Klipdrift Road', 'Klipdrift', 'Midrand', 'Gauteng', '1685', -25.9303, 28.0623, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ── Units ────────────────────────────────────────────────────────────
INSERT INTO units (
    id, property_id, unit_number, title, description, unit_type, status,
    furnishing, price, payment_frequency, deposit, bedrooms, bathrooms, sqm,
    water_included, electricity_included, pets_allowed, available_from,
    created_at, updated_at
) VALUES
-- Carlswald (prop 31): 4 units
(md5('hab-prop-31-unit-1')::uuid, md5('hab-prop-31')::uuid, '101', 'Carlswald Studio',          'Compact studio.',                       'STUDIO',    'AVAILABLE', 'FURNISHED',      9500.00,  'MONTHLY', 19000.00, 1, 1,  42, TRUE,  FALSE, FALSE, CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-31-unit-2')::uuid, md5('hab-prop-31')::uuid, '203', 'Carlswald 1-Bed',           'One-bedroom apartment.',                'APARTMENT', 'AVAILABLE', 'SEMI_FURNISHED', 12500.00, 'MONTHLY', 25000.00, 1, 1,  58, TRUE,  FALSE, FALSE, CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-31-unit-3')::uuid, md5('hab-prop-31')::uuid, '305', 'Carlswald 2-Bed',           'Two-bedroom apartment.',                'APARTMENT', 'AVAILABLE', 'UNFURNISHED',    17500.00, 'MONTHLY', 35000.00, 2, 2,  88, TRUE,  FALSE, FALSE, CURRENT_DATE + INTERVAL '14 days', NOW(), NOW()),
(md5('hab-prop-31-unit-4')::uuid, md5('hab-prop-31')::uuid, '402', 'Carlswald 2-Bed Penthouse', 'Top-floor two-bedroom.',                'APARTMENT', 'AVAILABLE', 'FURNISHED',      21000.00, 'MONTHLY', 42000.00, 2, 2, 102, TRUE,  TRUE,  FALSE, CURRENT_DATE,                NOW(), NOW()),
-- Halfway House (prop 32): 3 units
(md5('hab-prop-32-unit-1')::uuid, md5('hab-prop-32')::uuid, '12',  'Halfway House 1-Bed',       'One-bedroom apartment.',                'APARTMENT', 'AVAILABLE', 'SEMI_FURNISHED', 11000.00, 'MONTHLY', 22000.00, 1, 1,  55, TRUE,  FALSE, FALSE, CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-32-unit-2')::uuid, md5('hab-prop-32')::uuid, '24',  'Halfway House 2-Bed',       'Two-bedroom apartment.',                'APARTMENT', 'AVAILABLE', 'UNFURNISHED',    16500.00, 'MONTHLY', 33000.00, 2, 2,  82, TRUE,  FALSE, FALSE, CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-32-unit-3')::uuid, md5('hab-prop-32')::uuid, '36',  'Halfway House 2-Bed +',     'Two-bedroom with study.',               'APARTMENT', 'AVAILABLE', 'SEMI_FURNISHED', 18500.00, 'MONTHLY', 37000.00, 2, 2,  95, TRUE,  TRUE,  FALSE, CURRENT_DATE + INTERVAL '7 days',  NOW(), NOW()),
-- Vorna Valley (prop 33): 4 units
(md5('hab-prop-33-unit-1')::uuid, md5('hab-prop-33')::uuid, '4A',  'The Vorna Studio',          'Compact studio.',                       'STUDIO',    'AVAILABLE', 'FURNISHED',      9000.00,  'MONTHLY', 18000.00, 1, 1,  40, TRUE,  FALSE, FALSE, CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-33-unit-2')::uuid, md5('hab-prop-33')::uuid, '6B',  'The Vorna 1-Bed',           'One-bedroom apartment.',                'APARTMENT', 'AVAILABLE', 'SEMI_FURNISHED', 12000.00, 'MONTHLY', 24000.00, 1, 1,  56, TRUE,  FALSE, FALSE, CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-33-unit-3')::uuid, md5('hab-prop-33')::uuid, '8C',  'The Vorna 2-Bed',           'Two-bedroom apartment.',                'APARTMENT', 'AVAILABLE', 'UNFURNISHED',    16000.00, 'MONTHLY', 32000.00, 2, 1,  80, TRUE,  FALSE, FALSE, CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-33-unit-4')::uuid, md5('hab-prop-33')::uuid, '10D', 'The Vorna 3-Bed',           'Three-bedroom apartment.',              'APARTMENT', 'AVAILABLE', 'UNFURNISHED',    22000.00, 'MONTHLY', 44000.00, 3, 2, 110, TRUE,  FALSE, FALSE, CURRENT_DATE + INTERVAL '14 days', NOW(), NOW()),
-- Waterval (prop 34): 3 units
(md5('hab-prop-34-unit-1')::uuid, md5('hab-prop-34')::uuid, '12',  'Waterval 2-Bed',            'Two-bedroom on the ground floor.',      'APARTMENT', 'AVAILABLE', 'UNFURNISHED',    17500.00, 'MONTHLY', 35000.00, 2, 2,  92, FALSE, FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-34-unit-2')::uuid, md5('hab-prop-34')::uuid, '22',  'Waterval 2-Bed +',          'Two-bedroom upper floor.',              'APARTMENT', 'AVAILABLE', 'SEMI_FURNISHED', 19500.00, 'MONTHLY', 39000.00, 2, 2, 100, FALSE, FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-34-unit-3')::uuid, md5('hab-prop-34')::uuid, '32',  'Waterval 3-Bed',            'Three-bedroom family unit.',            'APARTMENT', 'AVAILABLE', 'UNFURNISHED',    25000.00, 'MONTHLY', 50000.00, 3, 2, 130, FALSE, FALSE, TRUE,  CURRENT_DATE + INTERVAL '21 days', NOW(), NOW()),
-- Crowthorne Heights (prop 35): 3 units
(md5('hab-prop-35-unit-1')::uuid, md5('hab-prop-35')::uuid, '1',   'Crowthorne 1-Bed',          'One-bedroom apartment.',                'APARTMENT', 'AVAILABLE', 'SEMI_FURNISHED', 10500.00, 'MONTHLY', 21000.00, 1, 1,  54, TRUE,  FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-35-unit-2')::uuid, md5('hab-prop-35')::uuid, '7',   'Crowthorne 2-Bed',          'Two-bedroom apartment.',                'APARTMENT', 'AVAILABLE', 'UNFURNISHED',    15500.00, 'MONTHLY', 31000.00, 2, 1,  78, TRUE,  FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-35-unit-3')::uuid, md5('hab-prop-35')::uuid, '12',  'Crowthorne 2-Bed +',        'Two-bedroom with garden.',              'APARTMENT', 'AVAILABLE', 'SEMI_FURNISHED', 17500.00, 'MONTHLY', 35000.00, 2, 2,  90, TRUE,  FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
-- Country View (prop 36): 3 units
(md5('hab-prop-36-unit-1')::uuid, md5('hab-prop-36')::uuid, 'A1',  'Country View 1-Bed',        'One-bedroom apartment.',                'APARTMENT', 'AVAILABLE', 'UNFURNISHED',    10000.00, 'MONTHLY', 20000.00, 1, 1,  52, TRUE,  FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-36-unit-2')::uuid, md5('hab-prop-36')::uuid, 'B4',  'Country View 2-Bed',        'Two-bedroom apartment.',                'APARTMENT', 'AVAILABLE', 'UNFURNISHED',    14500.00, 'MONTHLY', 29000.00, 2, 2,  82, TRUE,  FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-36-unit-3')::uuid, md5('hab-prop-36')::uuid, 'C8',  'Country View 2-Bed Garden', 'Two-bedroom with private garden.',      'APARTMENT', 'AVAILABLE', 'SEMI_FURNISHED', 16500.00, 'MONTHLY', 33000.00, 2, 2,  92, TRUE,  FALSE, TRUE,  CURRENT_DATE + INTERVAL '14 days', NOW(), NOW()),
-- Noordwyk (prop 37): 4 units
(md5('hab-prop-37-unit-1')::uuid, md5('hab-prop-37')::uuid, '102', 'Noordwyk Studio',           'Compact studio.',                       'STUDIO',    'AVAILABLE', 'FURNISHED',      8500.00,  'MONTHLY', 17000.00, 1, 1,  38, TRUE,  TRUE,  FALSE, CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-37-unit-2')::uuid, md5('hab-prop-37')::uuid, '203', 'Noordwyk 1-Bed',            'One-bedroom apartment.',                'APARTMENT', 'AVAILABLE', 'SEMI_FURNISHED', 11500.00, 'MONTHLY', 23000.00, 1, 1,  55, TRUE,  FALSE, FALSE, CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-37-unit-3')::uuid, md5('hab-prop-37')::uuid, '304', 'Noordwyk 1-Bed +',          'One-bedroom upper floor.',              'APARTMENT', 'AVAILABLE', 'SEMI_FURNISHED', 12500.00, 'MONTHLY', 25000.00, 1, 1,  60, TRUE,  FALSE, FALSE, CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-37-unit-4')::uuid, md5('hab-prop-37')::uuid, '405', 'Noordwyk 2-Bed',            'Two-bedroom apartment.',                'APARTMENT', 'AVAILABLE', 'UNFURNISHED',    16000.00, 'MONTHLY', 32000.00, 2, 2,  85, TRUE,  FALSE, FALSE, CURRENT_DATE,                NOW(), NOW()),
-- Halfway Gardens (prop 38): 3 units
(md5('hab-prop-38-unit-1')::uuid, md5('hab-prop-38')::uuid, '5',   'Halfway Gardens 1-Bed',     'One-bedroom apartment.',                'APARTMENT', 'AVAILABLE', 'SEMI_FURNISHED', 11500.00, 'MONTHLY', 23000.00, 1, 1,  58, TRUE,  FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-38-unit-2')::uuid, md5('hab-prop-38')::uuid, '12',  'Halfway Gardens 2-Bed',     'Two-bedroom apartment.',                'APARTMENT', 'AVAILABLE', 'UNFURNISHED',    16500.00, 'MONTHLY', 33000.00, 2, 2,  88, TRUE,  FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-38-unit-3')::uuid, md5('hab-prop-38')::uuid, '20',  'Halfway Gardens 2-Bed +',   'Two-bedroom with study.',               'APARTMENT', 'AVAILABLE', 'UNFURNISHED',    18500.00, 'MONTHLY', 37000.00, 2, 2,  98, TRUE,  FALSE, TRUE,  CURRENT_DATE + INTERVAL '14 days', NOW(), NOW()),
-- Kyalami (prop 39): 4 units
(md5('hab-prop-39-unit-1')::uuid, md5('hab-prop-39')::uuid, '8',   'Kyalami 1-Bed',             'One-bedroom apartment.',                'APARTMENT', 'AVAILABLE', 'SEMI_FURNISHED', 13500.00, 'MONTHLY', 27000.00, 1, 1,  60, TRUE,  FALSE, FALSE, CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-39-unit-2')::uuid, md5('hab-prop-39')::uuid, '14',  'Kyalami 2-Bed',             'Two-bedroom apartment.',                'APARTMENT', 'AVAILABLE', 'UNFURNISHED',    19000.00, 'MONTHLY', 38000.00, 2, 2,  90, TRUE,  FALSE, FALSE, CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-39-unit-3')::uuid, md5('hab-prop-39')::uuid, '21',  'Kyalami 2-Bed Garden',      'Two-bedroom with private garden.',      'APARTMENT', 'AVAILABLE', 'FURNISHED',      22000.00, 'MONTHLY', 44000.00, 2, 2,  98, TRUE,  TRUE,  FALSE, CURRENT_DATE + INTERVAL '7 days',  NOW(), NOW()),
(md5('hab-prop-39-unit-4')::uuid, md5('hab-prop-39')::uuid, 'PH',  'Kyalami 3-Bed Penthouse',   'Three-bedroom penthouse.',              'APARTMENT', 'AVAILABLE', 'FURNISHED',      30000.00, 'MONTHLY', 60000.00, 3, 3, 140, TRUE,  TRUE,  FALSE, CURRENT_DATE + INTERVAL '21 days', NOW(), NOW()),
-- President Park (prop 40): 3 units
(md5('hab-prop-40-unit-1')::uuid, md5('hab-prop-40')::uuid, '102', 'President Park Studio',     'Compact studio.',                       'STUDIO',    'AVAILABLE', 'FURNISHED',      8000.00,  'MONTHLY', 16000.00, 1, 1,  36, TRUE,  TRUE,  FALSE, CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-40-unit-2')::uuid, md5('hab-prop-40')::uuid, '204', 'President Park 1-Bed',      'One-bedroom apartment.',                'APARTMENT', 'AVAILABLE', 'SEMI_FURNISHED', 11000.00, 'MONTHLY', 22000.00, 1, 1,  52, TRUE,  FALSE, FALSE, CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-40-unit-3')::uuid, md5('hab-prop-40')::uuid, '306', 'President Park 2-Bed',      'Two-bedroom apartment.',                'APARTMENT', 'AVAILABLE', 'UNFURNISHED',    14500.00, 'MONTHLY', 29000.00, 2, 2,  78, TRUE,  FALSE, FALSE, CURRENT_DATE,                NOW(), NOW()),

-- Pretoria · Sunnyside (prop 41): 4 units
(md5('hab-prop-41-unit-1')::uuid, md5('hab-prop-41')::uuid, '203', 'Sunnyside Studio',          'Student-friendly studio.',              'STUDIO',    'AVAILABLE', 'FURNISHED',      6500.00,  'MONTHLY', 13000.00, 1, 1,  32, TRUE,  TRUE,  FALSE, CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-41-unit-2')::uuid, md5('hab-prop-41')::uuid, '305', 'Sunnyside 1-Bed',           'One-bedroom apartment.',                'APARTMENT', 'AVAILABLE', 'SEMI_FURNISHED', 8500.00,  'MONTHLY', 17000.00, 1, 1,  48, TRUE,  FALSE, FALSE, CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-41-unit-3')::uuid, md5('hab-prop-41')::uuid, '408', 'Sunnyside 2-Bed',           'Two-bedroom apartment.',                'APARTMENT', 'AVAILABLE', 'UNFURNISHED',    11500.00, 'MONTHLY', 23000.00, 2, 1,  68, TRUE,  FALSE, FALSE, CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-41-unit-4')::uuid, md5('hab-prop-41')::uuid, '512', 'Sunnyside 2-Bed +',         'Two-bedroom with balcony.',             'APARTMENT', 'AVAILABLE', 'SEMI_FURNISHED', 13000.00, 'MONTHLY', 26000.00, 2, 2,  78, TRUE,  FALSE, FALSE, CURRENT_DATE + INTERVAL '14 days', NOW(), NOW()),
-- Pretoria · Arcadia (prop 42): 3 units
(md5('hab-prop-42-unit-1')::uuid, md5('hab-prop-42')::uuid, '6',   'Arcadia 1-Bed',             'One-bedroom apartment.',                'APARTMENT', 'AVAILABLE', 'SEMI_FURNISHED', 9000.00,  'MONTHLY', 18000.00, 1, 1,  50, TRUE,  FALSE, FALSE, CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-42-unit-2')::uuid, md5('hab-prop-42')::uuid, '14',  'Arcadia 2-Bed',             'Two-bedroom apartment.',                'APARTMENT', 'AVAILABLE', 'UNFURNISHED',    12500.00, 'MONTHLY', 25000.00, 2, 1,  72, TRUE,  FALSE, FALSE, CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-42-unit-3')::uuid, md5('hab-prop-42')::uuid, '22',  'Arcadia 2-Bed +',           'Two-bedroom with study.',               'APARTMENT', 'AVAILABLE', 'SEMI_FURNISHED', 14000.00, 'MONTHLY', 28000.00, 2, 2,  82, TRUE,  TRUE,  FALSE, CURRENT_DATE,                NOW(), NOW()),
-- Pretoria · Hatfield (prop 43): 4 units
(md5('hab-prop-43-unit-1')::uuid, md5('hab-prop-43')::uuid, '503', 'Hatfield 1-Bed',            'One-bedroom apartment.',                'APARTMENT', 'AVAILABLE', 'SEMI_FURNISHED', 9500.00,  'MONTHLY', 19000.00, 1, 1,  52, TRUE,  FALSE, FALSE, CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-43-unit-2')::uuid, md5('hab-prop-43')::uuid, '702', 'Hatfield 1-Bed +',          'One-bedroom upper floor.',              'APARTMENT', 'AVAILABLE', 'SEMI_FURNISHED', 11000.00, 'MONTHLY', 22000.00, 1, 1,  58, TRUE,  FALSE, FALSE, CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-43-unit-3')::uuid, md5('hab-prop-43')::uuid, '905', 'Hatfield 2-Bed',            'Two-bedroom apartment.',                'APARTMENT', 'AVAILABLE', 'UNFURNISHED',    14000.00, 'MONTHLY', 28000.00, 2, 2,  75, TRUE,  FALSE, FALSE, CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-43-unit-4')::uuid, md5('hab-prop-43')::uuid, '1201','Hatfield 3-Bed',            'Three-bedroom corner apartment.',       'APARTMENT', 'AVAILABLE', 'FURNISHED',      19500.00, 'MONTHLY', 39000.00, 3, 2, 105, TRUE,  TRUE,  FALSE, CURRENT_DATE + INTERVAL '7 days',  NOW(), NOW()),
-- Pretoria · Brooklyn (prop 44): 3 units
(md5('hab-prop-44-unit-1')::uuid, md5('hab-prop-44')::uuid, '101', 'Brooklyn 2-Bed',            'Two-bedroom apartment.',                'APARTMENT', 'AVAILABLE', 'SEMI_FURNISHED', 15000.00, 'MONTHLY', 30000.00, 2, 2,  85, TRUE,  FALSE, FALSE, CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-44-unit-2')::uuid, md5('hab-prop-44')::uuid, '204', 'Brooklyn 2-Bed +',          'Two-bedroom with study.',               'APARTMENT', 'AVAILABLE', 'UNFURNISHED',    17000.00, 'MONTHLY', 34000.00, 2, 2,  95, TRUE,  FALSE, FALSE, CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-44-unit-3')::uuid, md5('hab-prop-44')::uuid, '308', 'Brooklyn 3-Bed',            'Three-bedroom apartment.',              'APARTMENT', 'AVAILABLE', 'FURNISHED',      22000.00, 'MONTHLY', 44000.00, 3, 2, 115, TRUE,  TRUE,  FALSE, CURRENT_DATE + INTERVAL '14 days', NOW(), NOW()),
-- Pretoria · Menlyn (prop 45): 4 units
(md5('hab-prop-45-unit-1')::uuid, md5('hab-prop-45')::uuid, '402', 'Menlyn 1-Bed',              'One-bedroom apartment.',                'APARTMENT', 'AVAILABLE', 'FURNISHED',      11500.00, 'MONTHLY', 23000.00, 1, 1,  55, TRUE,  FALSE, FALSE, CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-45-unit-2')::uuid, md5('hab-prop-45')::uuid, '603', 'Menlyn 2-Bed',              'Two-bedroom apartment.',                'APARTMENT', 'AVAILABLE', 'SEMI_FURNISHED', 16500.00, 'MONTHLY', 33000.00, 2, 2,  85, TRUE,  FALSE, FALSE, CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-45-unit-3')::uuid, md5('hab-prop-45')::uuid, '805', 'Menlyn 2-Bed +',            'Two-bedroom upper floor.',              'APARTMENT', 'AVAILABLE', 'FURNISHED',      18500.00, 'MONTHLY', 37000.00, 2, 2,  95, TRUE,  TRUE,  FALSE, CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-45-unit-4')::uuid, md5('hab-prop-45')::uuid, '1102','Menlyn 3-Bed Penthouse',    'Three-bedroom penthouse with views.',   'APARTMENT', 'AVAILABLE', 'FURNISHED',      26000.00, 'MONTHLY', 52000.00, 3, 3, 130, TRUE,  TRUE,  FALSE, CURRENT_DATE + INTERVAL '21 days', NOW(), NOW()),

-- Glen Austin Country Estate (prop 46): 3 units
(md5('hab-prop-46-unit-1')::uuid, md5('hab-prop-46')::uuid, 'Main',    'Main Farmhouse',             'Four-bedroom farmhouse with pool.',  'HOUSE',     'AVAILABLE', 'UNFURNISHED', 30000.00, 'MONTHLY', 60000.00, 4, 3, 280, FALSE, FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-46-unit-2')::uuid, md5('hab-prop-46')::uuid, 'CtgA',    'Garden Cottage',             'One-bedroom cottage.',               'COTTAGE',   'AVAILABLE', 'FURNISHED',    9500.00, 'MONTHLY', 19000.00, 1, 1,  55, TRUE,  FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-46-unit-3')::uuid, md5('hab-prop-46')::uuid, 'CtgB',    'Stable-side Flatlet',        'Studio flatlet near the stables.',   'FLATLET',   'AVAILABLE', 'FURNISHED',    7500.00, 'MONTHLY', 15000.00, 1, 1,  40, TRUE,  FALSE, TRUE,  CURRENT_DATE + INTERVAL '14 days', NOW(), NOW()),
-- Beaulieu Equestrian Plot (prop 47): 4 units
(md5('hab-prop-47-unit-1')::uuid, md5('hab-prop-47')::uuid, 'Main',    'Main Farmhouse',             'Five-bedroom homestead.',            'HOUSE',     'AVAILABLE', 'UNFURNISHED', 38000.00, 'MONTHLY', 76000.00, 5, 4, 360, FALSE, FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-47-unit-2')::uuid, md5('hab-prop-47')::uuid, 'CtgA',    'Pool Cottage',               'Two-bed cottage near the pool.',     'COTTAGE',   'AVAILABLE', 'FURNISHED',   13000.00, 'MONTHLY', 26000.00, 2, 1,  80, TRUE,  FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-47-unit-3')::uuid, md5('hab-prop-47')::uuid, 'CtgB',    'Paddock Cottage',            'One-bed cottage by the paddocks.',   'COTTAGE',   'AVAILABLE', 'SEMI_FURNISHED', 10500.00, 'MONTHLY', 21000.00, 1, 1,  60, TRUE,  FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-47-unit-4')::uuid, md5('hab-prop-47')::uuid, 'CtgC',    'Vineyard Flatlet',           'Studio flatlet on the south fence.', 'FLATLET',   'AVAILABLE', 'FURNISHED',    8500.00, 'MONTHLY', 17000.00, 1, 1,  42, TRUE,  TRUE,  TRUE,  CURRENT_DATE + INTERVAL '21 days', NOW(), NOW()),
-- Bushwillow Park Smallholding (prop 48): 2 units
(md5('hab-prop-48-unit-1')::uuid, md5('hab-prop-48')::uuid, 'Main',    'Main House',                 'Four-bedroom main residence.',       'HOUSE',     'AVAILABLE', 'UNFURNISHED', 28000.00, 'MONTHLY', 56000.00, 4, 3, 240, FALSE, FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-48-unit-2')::uuid, md5('hab-prop-48')::uuid, 'Cottage', 'Bushwillow Cottage',         'One-bed cottage with private deck.', 'COTTAGE',   'AVAILABLE', 'FURNISHED',   11000.00, 'MONTHLY', 22000.00, 1, 1,  65, TRUE,  FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
-- President Park Smallholding (prop 49): 3 units
(md5('hab-prop-49-unit-1')::uuid, md5('hab-prop-49')::uuid, 'Main',    'Main House',                 'Three-bed home on the plot.',        'HOUSE',     'AVAILABLE', 'UNFURNISHED', 24000.00, 'MONTHLY', 48000.00, 3, 2, 200, FALSE, FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-49-unit-2')::uuid, md5('hab-prop-49')::uuid, 'CtgA',    'Garden Cottage',             'One-bed cottage.',                   'COTTAGE',   'AVAILABLE', 'SEMI_FURNISHED', 9000.00, 'MONTHLY', 18000.00, 1, 1,  55, TRUE,  FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-49-unit-3')::uuid, md5('hab-prop-49')::uuid, 'CtgB',    'Backroom Flatlet',           'Studio flatlet.',                    'FLATLET',   'AVAILABLE', 'FURNISHED',    6500.00, 'MONTHLY', 13000.00, 1, 1,  32, TRUE,  TRUE,  TRUE,  CURRENT_DATE + INTERVAL '7 days', NOW(), NOW()),
-- Klipdrift Farm Estate (prop 50): 4 units
(md5('hab-prop-50-unit-1')::uuid, md5('hab-prop-50')::uuid, 'Main',    'Main Farmhouse',             'Five-bed homestead with veranda.',   'HOUSE',     'AVAILABLE', 'UNFURNISHED', 35000.00, 'MONTHLY', 70000.00, 5, 3, 320, FALSE, FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-50-unit-2')::uuid, md5('hab-prop-50')::uuid, 'CtgA',    'Orchard Cottage',            'Two-bed cottage by the orchard.',    'COTTAGE',   'AVAILABLE', 'FURNISHED',   12000.00, 'MONTHLY', 24000.00, 2, 1,  85, TRUE,  FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-50-unit-3')::uuid, md5('hab-prop-50')::uuid, 'CtgB',    'Dam Cottage',                'One-bed cottage by the dam.',        'COTTAGE',   'AVAILABLE', 'SEMI_FURNISHED', 10000.00, 'MONTHLY', 20000.00, 1, 1,  60, TRUE,  FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-50-unit-4')::uuid, md5('hab-prop-50')::uuid, 'CtgC',    'Stable Flatlet',             'Studio flatlet near the stables.',   'FLATLET',   'AVAILABLE', 'FURNISHED',    8000.00, 'MONTHLY', 16000.00, 1, 1,  40, TRUE,  TRUE,  TRUE,  CURRENT_DATE + INTERVAL '14 days', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ── Property images (cover + alt) ────────────────────────────────────
-- Cycles Unsplash exteriors across the new properties.
INSERT INTO property_images (id, property_id, url, is_cover, sort_order, created_at, updated_at)
SELECT
    md5('hab-prop-' || gs || '-img-' || idx)::uuid,
    md5('hab-prop-' || gs)::uuid,
    CASE (gs + idx) % 8
        WHEN 0 THEN 'https://images.unsplash.com/photo-1564013799919-ab600027ffc6?w=1600&q=80'
        WHEN 1 THEN 'https://images.unsplash.com/photo-1568605114967-8130f3a36994?w=1600&q=80'
        WHEN 2 THEN 'https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=1600&q=80'
        WHEN 3 THEN 'https://images.unsplash.com/photo-1613490493576-7fde63acd811?w=1600&q=80'
        WHEN 4 THEN 'https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?w=1600&q=80'
        WHEN 5 THEN 'https://images.unsplash.com/photo-1605276374104-dee2a0ed3cd6?w=1600&q=80'
        WHEN 6 THEN 'https://images.unsplash.com/photo-1583608205776-bfd35f0d9f83?w=1600&q=80'
        WHEN 7 THEN 'https://images.unsplash.com/photo-1518883240204-f0a32e478486?w=1600&q=80'
    END,
    idx = 1,
    idx - 1,
    NOW(), NOW()
FROM generate_series(31, 50) AS gs
CROSS JOIN generate_series(1, 2) AS idx
ON CONFLICT (id) DO NOTHING;

-- ── Unit images (3 interior shots per unit) ──────────────────────────
INSERT INTO unit_images (id, unit_id, url, is_cover, sort_order, created_at, updated_at)
SELECT
    md5(u.unit_slug || '-img-' || idx)::uuid,
    md5(u.unit_slug)::uuid,
    CASE (idx + u.salt) % 8
        WHEN 0 THEN 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=1600&q=80'
        WHEN 1 THEN 'https://images.unsplash.com/photo-1505691938895-1758d7feb511?w=1600&q=80'
        WHEN 2 THEN 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=1600&q=80'
        WHEN 3 THEN 'https://images.unsplash.com/photo-1540518614846-7eded433c457?w=1600&q=80'
        WHEN 4 THEN 'https://images.unsplash.com/photo-1600210492486-724fe5c67fb0?w=1600&q=80'
        WHEN 5 THEN 'https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=1600&q=80'
        WHEN 6 THEN 'https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=1600&q=80'
        WHEN 7 THEN 'https://images.unsplash.com/photo-1554995207-c18c203602cb?w=1600&q=80'
    END,
    idx = 1,
    idx - 1,
    NOW(), NOW()
FROM (
    -- Every (property, unit-index) pair that exists in this seed migration.
    SELECT 'hab-prop-' || gs || '-unit-' || u AS unit_slug, (gs * 7 + u) AS salt
    FROM generate_series(31, 50) AS gs
    CROSS JOIN generate_series(1, 4) AS u
    WHERE
        -- Only emit unit rows that the units INSERT above actually created.
        (gs IN (31, 33, 37, 39) AND u <= 4)
     OR (gs IN (32, 35, 36, 38, 40, 42, 44) AND u <= 3)
     OR (gs IN (34) AND u <= 3)
     OR (gs IN (41, 43, 45) AND u <= 4)
     OR (gs IN (46) AND u <= 3)
     OR (gs IN (47) AND u <= 4)
     OR (gs IN (48) AND u <= 2)
     OR (gs IN (49) AND u <= 3)
     OR (gs IN (50) AND u <= 4)
) u
CROSS JOIN generate_series(1, 3) AS idx
ON CONFLICT (id) DO NOTHING;

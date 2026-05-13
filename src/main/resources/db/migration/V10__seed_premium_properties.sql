-- Seed 20 premium properties across SA's wealthy suburbs to give /browse
-- something real to render against the hero search.
--
-- Layout:
--   * 7 in Joburg North (Sandton / Bryanston / Hyde Park / Rosebank / Morningside)
--   * 6 in Cape Town  (Camps Bay / Clifton / Sea Point / Constantia / Bishopscourt)
--   * 4 in KZN        (Umhlanga / Ballito / Durban North)
--   * 3 in Winelands  (Stellenbosch / Franschhoek)
--
-- Most properties have one unit; a handful of apartment blocks / complexes
-- have 2-3 units to exercise the multi-unit list endpoints.
--
-- Photos are Unsplash URLs (real CC0 imagery) cycled across an exterior set
-- for property cover shots and an interior set for unit shots. Real photo
-- upload via StorageService is a follow-up slice.
--
-- All landlord + manager assignments point at user #2 (Thandi Mokoena, the
-- LANDLORD demo seed) — keeps the seed small. Agent-managed listings come
-- with the mandate slice later.
--
-- Idempotent: every UUID is md5-derived from a stable slug, every INSERT
-- has ON CONFLICT (id) DO NOTHING. Safe to re-run on partial-state DBs.

-- ── Properties ───────────────────────────────────────────────────────
INSERT INTO properties (
    id, landlord_id, manager_id, title, description, property_type, status,
    address_line, suburb, city, province, postal_code, latitude, longitude,
    created_at, updated_at
) VALUES
-- Joburg North
(md5('hab-prop-1')::uuid,  '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Modern Family Home in Sandton',         'Spacious modern home walking distance to Sandton City. North-facing pool, double-volume entrance, full backup power.',
 'HOUSE',              'LISTED',
 '5 Rivonia Road', 'Sandton',     'Johannesburg', 'Gauteng', '2196', -26.1075,  28.0567, NOW(), NOW()),
(md5('hab-prop-2')::uuid,  '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'The Atrium · Sandton Apartments',       'Boutique apartment block in Sandton CBD. Concierge, gym, rooftop pool. Three apartments currently available.',
 'APARTMENT_BLOCK',    'LISTED',
 '12 West Street', 'Sandton',     'Johannesburg', 'Gauteng', '2196', -26.1080,  28.0540, NOW(), NOW()),
(md5('hab-prop-3')::uuid,  '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Bryanston Townhouse with Garden',       'Three-bedroom townhouse in a secure estate. Generous garden, fibre-ready, two covered parking bays.',
 'TOWNHOUSE_COMPLEX',  'LISTED',
 '21 Cumberland Road', 'Bryanston', 'Johannesburg', 'Gauteng', '2191', -26.0508,  28.0233, NOW(), NOW()),
(md5('hab-prop-4')::uuid,  '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Hyde Park Penthouse Residences',        'Luxury apartment block overlooking Hyde Park. Two flagship units currently listed.',
 'APARTMENT_BLOCK',    'LISTED',
 '1 Jan Smuts Avenue', 'Hyde Park',  'Johannesburg', 'Gauteng', '2196', -26.1283,  28.0353, NOW(), NOW()),
(md5('hab-prop-5')::uuid,  '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Rosebank Loft Apartment',                'Two-bedroom apartment in the heart of Rosebank. Steps from The Zone, gym in building, north-facing balcony.',
 'APARTMENT_BLOCK',    'LISTED',
 '173 Oxford Road', 'Rosebank',     'Johannesburg', 'Gauteng', '2196', -26.1426,  28.0395, NOW(), NOW()),
(md5('hab-prop-6')::uuid,  '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Morningside Family Townhouse',          'Three-bedroom townhouse in a 24-hour-secured estate. Communal pool, kids'' play area, fibre.',
 'TOWNHOUSE_COMPLEX',  'LISTED',
 '8 Outspan Road', 'Morningside',   'Johannesburg', 'Gauteng', '2057', -26.0651,  28.0530, NOW(), NOW()),
(md5('hab-prop-7')::uuid,  '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Sandton Central Studio Loft',           'Studio loft in Sandton CBD. Industrial finishes, walking distance to Gautrain, all-inclusive utilities.',
 'APARTMENT_BLOCK',    'LISTED',
 '146 West Street', 'Sandton',      'Johannesburg', 'Gauteng', '2196', -26.1095,  28.0540, NOW(), NOW()),
-- Cape Town
(md5('hab-prop-8')::uuid,  '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Camps Bay Ocean Villa',                 'Four-bedroom villa with full ocean and Table Mountain views. Heated pool, sun deck, two-car garage.',
 'HOUSE',              'LISTED',
 '14 Geneva Drive', 'Camps Bay',     'Cape Town', 'Western Cape', '8005', -33.9519,  18.3787, NOW(), NOW()),
(md5('hab-prop-9')::uuid,  '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Clifton Penthouse',                     'Two-bedroom penthouse on the slopes above Clifton 4th. Wraparound balcony, private lift, full sea view.',
 'APARTMENT_BLOCK',    'LISTED',
 '7 Kloof Road', 'Clifton',          'Cape Town', 'Western Cape', '8005', -33.9425,  18.3771, NOW(), NOW()),
(md5('hab-prop-10')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Sea Point Compact Apartment',           'One-bedroom apartment two blocks from the promenade. Renovated 2024, pet-friendly.',
 'APARTMENT_BLOCK',    'LISTED',
 '88 Beach Road', 'Sea Point',       'Cape Town', 'Western Cape', '8005', -33.9091,  18.3868, NOW(), NOW()),
(md5('hab-prop-11')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'The Aurora · Sea Point',                'Boutique block on Main Road with views to Signal Hill. Studio and one-bedroom apartments available.',
 'APARTMENT_BLOCK',    'LISTED',
 '241 Main Road', 'Sea Point',       'Cape Town', 'Western Cape', '8005', -33.9145,  18.3893, NOW(), NOW()),
(md5('hab-prop-12')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Constantia Garden House',               'Three-bedroom home on a treed plot in Upper Constantia. Borehole, vegetable garden, koi pond.',
 'HOUSE',              'LISTED',
 '32 Brommersvlei Road', 'Constantia', 'Cape Town', 'Western Cape', '7806', -34.0241,  18.4438, NOW(), NOW()),
(md5('hab-prop-13')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Bishopscourt Estate',                   'Four-bedroom estate home backing onto Kirstenbosch. Library, gym, two-bedroom guest cottage on-site.',
 'HOUSE',              'LISTED',
 '11 Hohenort Avenue', 'Bishopscourt', 'Cape Town', 'Western Cape', '7708', -33.9952,  18.4441, NOW(), NOW()),
-- KZN
(md5('hab-prop-14')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Umhlanga Ocean Apartment',              'Two-bedroom apartment with uninterrupted Indian Ocean views. Direct beach access.',
 'APARTMENT_BLOCK',    'LISTED',
 '12 Lighthouse Road', 'Umhlanga',   'Durban', 'KwaZulu-Natal', '4319', -29.7270,  31.0840, NOW(), NOW()),
(md5('hab-prop-15')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Pearls of Umhlanga Penthouse',          'Three-bedroom penthouse in the Pearls precinct. Private rooftop pool, concierge, golf-course access.',
 'APARTMENT_BLOCK',    'LISTED',
 '6 Pearls Drive', 'Umhlanga',       'Durban', 'KwaZulu-Natal', '4319', -29.7298,  31.0851, NOW(), NOW()),
(md5('hab-prop-16')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Ballito Townhouse',                     'Three-bedroom townhouse in a coastal estate. Pool, communal braai, five-minute drive to Salt Rock.',
 'TOWNHOUSE_COMPLEX',  'LISTED',
 '40 Sandra Road', 'Ballito',        'Durban', 'KwaZulu-Natal', '4420', -29.5343,  31.2174, NOW(), NOW()),
(md5('hab-prop-17')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Durban North Family Home',              'Four-bedroom family home on a generous corner stand. Pool, double garage, separate office wing.',
 'HOUSE',              'LISTED',
 '18 Adelaide Tambo Drive', 'Durban North', 'Durban', 'KwaZulu-Natal', '4051', -29.7993,  31.0421, NOW(), NOW()),
-- Winelands
(md5('hab-prop-18')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Stellenbosch Vineyard Cottage',         'Restored two-bedroom cottage on a working wine estate outside Stellenbosch. Vineyard view, fireplace.',
 'HOUSE',              'LISTED',
 '7 Dorp Street', 'Stellenbosch',    'Cape Town', 'Western Cape', '7600', -33.9333,  18.8602, NOW(), NOW()),
(md5('hab-prop-19')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Mostertsdrift Family Home',             'Three-bedroom Cape Dutch home in a quiet Stellenbosch street. Walk to the university and Eikestad Mall.',
 'HOUSE',              'LISTED',
 '23 Krige Street', 'Mostertsdrift', 'Cape Town', 'Western Cape', '7600', -33.9341,  18.8627, NOW(), NOW()),
(md5('hab-prop-20')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Franschhoek Vineyard Retreat',          'Two-bedroom cottage on a Franschhoek vineyard. Outdoor fireplace, wine cellar access, hiking on the doorstep.',
 'HOUSE',              'LISTED',
 '15 La Provence Road', 'Franschhoek', 'Cape Town', 'Western Cape', '7690', -33.9095,  19.1234, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ── Units ────────────────────────────────────────────────────────────
-- Single-unit properties get one unit; the four apartment blocks/complexes
-- with multiple listings get 2-3 units each (props 2, 4, 11).
INSERT INTO units (
    id, property_id, unit_number, title, description, unit_type, status,
    furnishing, price, payment_frequency, deposit, bedrooms, bathrooms, sqm,
    water_included, electricity_included, pets_allowed, available_from,
    created_at, updated_at
) VALUES
-- Single-unit properties (1, 3, 5, 6, 7, 8, 9, 10, 12, 13, 14, 15, 16, 17, 18, 19, 20)
(md5('hab-prop-1-unit-1')::uuid,  md5('hab-prop-1')::uuid,  NULL,  'Modern Family Home in Sandton',  'Full-house listing.',                'HOUSE',     'AVAILABLE', 'UNFURNISHED',   45000.00, 'MONTHLY', 90000.00,  4, 3, 320, FALSE, FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-3-unit-1')::uuid,  md5('hab-prop-3')::uuid,  NULL,  'Bryanston Townhouse',            'Whole townhouse, end unit.',         'TOWNHOUSE', 'AVAILABLE', 'UNFURNISHED',   28000.00, 'MONTHLY', 56000.00,  3, 2, 220, FALSE, FALSE, TRUE,  CURRENT_DATE + INTERVAL '14 days', NOW(), NOW()),
(md5('hab-prop-5-unit-1')::uuid,  md5('hab-prop-5')::uuid,  '12A', 'Rosebank Loft Apartment',        'North-facing two-bedroom.',          'APARTMENT', 'AVAILABLE', 'SEMI_FURNISHED', 22000.00, 'MONTHLY', 44000.00,  2, 2, 110, TRUE,  FALSE, FALSE, CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-6-unit-1')::uuid,  md5('hab-prop-6')::uuid,  '8',   'Morningside Family Townhouse',   'End unit in secure estate.',         'TOWNHOUSE', 'AVAILABLE', 'UNFURNISHED',   26000.00, 'MONTHLY', 52000.00,  3, 2, 190, FALSE, FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-7-unit-1')::uuid,  md5('hab-prop-7')::uuid,  '404', 'Sandton Central Studio Loft',    'Open-plan studio, all utilities in.', 'STUDIO',    'AVAILABLE', 'FURNISHED',     14000.00, 'MONTHLY', 14000.00,  1, 1,  55, TRUE,  TRUE,  FALSE, CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-8-unit-1')::uuid,  md5('hab-prop-8')::uuid,  NULL,  'Camps Bay Ocean Villa',          'Full villa rental.',                  'HOUSE',     'AVAILABLE', 'FURNISHED',     65000.00, 'MONTHLY', 130000.00, 4, 4, 450, FALSE, FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-9-unit-1')::uuid,  md5('hab-prop-9')::uuid,  'PH',  'Clifton Penthouse',              'Two-bedroom penthouse.',              'APARTMENT', 'AVAILABLE', 'FURNISHED',     48000.00, 'MONTHLY', 96000.00,  2, 2, 175, TRUE,  FALSE, FALSE, CURRENT_DATE + INTERVAL '7 days',  NOW(), NOW()),
(md5('hab-prop-10-unit-1')::uuid, md5('hab-prop-10')::uuid, '5B',  'Sea Point One-Bedroom',          'Compact, renovated.',                 'APARTMENT', 'AVAILABLE', 'SEMI_FURNISHED', 15000.00, 'MONTHLY', 30000.00,  1, 1,  62, TRUE,  FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-12-unit-1')::uuid, md5('hab-prop-12')::uuid, NULL,  'Constantia Garden House',        'Three-bedroom with borehole.',        'HOUSE',     'AVAILABLE', 'UNFURNISHED',   38000.00, 'MONTHLY', 76000.00,  3, 2, 280, TRUE,  FALSE, TRUE,  CURRENT_DATE + INTERVAL '30 days', NOW(), NOW()),
(md5('hab-prop-13-unit-1')::uuid, md5('hab-prop-13')::uuid, NULL,  'Bishopscourt Estate',            'Main house. Guest cottage separate.', 'HOUSE',     'AVAILABLE', 'UNFURNISHED',   75000.00, 'MONTHLY', 150000.00, 4, 4, 520, FALSE, FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-14-unit-1')::uuid, md5('hab-prop-14')::uuid, '602', 'Umhlanga Ocean Apartment',       'Direct sea views.',                   'APARTMENT', 'AVAILABLE', 'FURNISHED',     24000.00, 'MONTHLY', 48000.00,  2, 2, 130, TRUE,  FALSE, FALSE, CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-15-unit-1')::uuid, md5('hab-prop-15')::uuid, 'PH3', 'Pearls of Umhlanga Penthouse',   'Private rooftop pool.',               'APARTMENT', 'AVAILABLE', 'FURNISHED',     38000.00, 'MONTHLY', 76000.00,  3, 3, 240, TRUE,  TRUE,  FALSE, CURRENT_DATE + INTERVAL '14 days', NOW(), NOW()),
(md5('hab-prop-16-unit-1')::uuid, md5('hab-prop-16')::uuid, '40',  'Ballito Coastal Townhouse',      'Three-bed townhouse near Salt Rock.', 'TOWNHOUSE', 'AVAILABLE', 'SEMI_FURNISHED', 22000.00, 'MONTHLY', 44000.00,  3, 2, 180, FALSE, FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-17-unit-1')::uuid, md5('hab-prop-17')::uuid, NULL,  'Durban North Family Home',       'Corner-stand family home.',           'HOUSE',     'AVAILABLE', 'UNFURNISHED',   28000.00, 'MONTHLY', 56000.00,  4, 3, 290, FALSE, FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-18-unit-1')::uuid, md5('hab-prop-18')::uuid, NULL,  'Stellenbosch Vineyard Cottage',  'Restored cottage with fireplace.',    'COTTAGE',   'AVAILABLE', 'FURNISHED',     18000.00, 'MONTHLY', 36000.00,  2, 1,  95, TRUE,  FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-19-unit-1')::uuid, md5('hab-prop-19')::uuid, NULL,  'Mostertsdrift Family Home',      'Three-bed Cape Dutch.',               'HOUSE',     'AVAILABLE', 'UNFURNISHED',   25000.00, 'MONTHLY', 50000.00,  3, 2, 175, FALSE, FALSE, TRUE,  CURRENT_DATE + INTERVAL '21 days', NOW(), NOW()),
(md5('hab-prop-20-unit-1')::uuid, md5('hab-prop-20')::uuid, NULL,  'Franschhoek Vineyard Retreat',   'Two-bed cottage with cellar access.', 'COTTAGE',   'AVAILABLE', 'FURNISHED',     22000.00, 'MONTHLY', 44000.00,  2, 1, 110, TRUE,  TRUE,  TRUE,  CURRENT_DATE,                NOW(), NOW()),
-- Property 2 (Sandton apartment block): 3 units
(md5('hab-prop-2-unit-1')::uuid,  md5('hab-prop-2')::uuid,  '301', 'Atrium 1-Bedroom',               'North-facing one-bedroom apartment.', 'APARTMENT', 'AVAILABLE', 'SEMI_FURNISHED', 12000.00, 'MONTHLY', 24000.00,  1, 1,  58, TRUE,  FALSE, FALSE, CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-2-unit-2')::uuid,  md5('hab-prop-2')::uuid,  '702', 'Atrium 2-Bedroom',               'Higher floor two-bedroom.',           'APARTMENT', 'AVAILABLE', 'SEMI_FURNISHED', 18000.00, 'MONTHLY', 36000.00,  2, 2,  95, TRUE,  FALSE, FALSE, CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-2-unit-3')::uuid,  md5('hab-prop-2')::uuid,  '901', 'Atrium 2-Bedroom Corner',        'Corner two-bedroom with city view.',  'APARTMENT', 'AVAILABLE', 'FURNISHED',     20000.00, 'MONTHLY', 40000.00,  2, 2, 105, TRUE,  TRUE,  FALSE, CURRENT_DATE + INTERVAL '14 days', NOW(), NOW()),
-- Property 4 (Hyde Park): 2 units
(md5('hab-prop-4-unit-1')::uuid,  md5('hab-prop-4')::uuid,  '12A', 'Hyde Park 2-Bedroom',            'Two-bedroom with study.',             'APARTMENT', 'AVAILABLE', 'FURNISHED',     32000.00, 'MONTHLY', 64000.00,  2, 2, 140, TRUE,  FALSE, FALSE, CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-4-unit-2')::uuid,  md5('hab-prop-4')::uuid,  'PH',  'Hyde Park Penthouse',            'Three-bedroom penthouse.',            'APARTMENT', 'AVAILABLE', 'FURNISHED',     55000.00, 'MONTHLY', 110000.00, 3, 3, 230, TRUE,  TRUE,  FALSE, CURRENT_DATE + INTERVAL '30 days', NOW(), NOW()),
-- Property 11 (Sea Point): 2 units
(md5('hab-prop-11-unit-1')::uuid, md5('hab-prop-11')::uuid, '4B',  'Aurora Studio',                  'Open-plan studio with sea peek.',     'STUDIO',    'AVAILABLE', 'FURNISHED',     11000.00, 'MONTHLY', 22000.00,  1, 1,  48, TRUE,  FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-11-unit-2')::uuid, md5('hab-prop-11')::uuid, '8A',  'Aurora One-Bedroom',             'One-bedroom, Signal Hill view.',      'APARTMENT', 'AVAILABLE', 'SEMI_FURNISHED', 16000.00, 'MONTHLY', 32000.00,  1, 1,  72, TRUE,  FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ── Property images (exterior shots) ─────────────────────────────────
-- Two cover-ish images per property, cycling through a curated exterior set.
INSERT INTO property_images (id, property_id, url, is_cover, sort_order, created_at, updated_at) VALUES
(md5('hab-prop-1-img-1')::uuid,  md5('hab-prop-1')::uuid,  'https://images.unsplash.com/photo-1564013799919-ab600027ffc6?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-1-img-2')::uuid,  md5('hab-prop-1')::uuid,  'https://images.unsplash.com/photo-1568605114967-8130f3a36994?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-2-img-1')::uuid,  md5('hab-prop-2')::uuid,  'https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-2-img-2')::uuid,  md5('hab-prop-2')::uuid,  'https://images.unsplash.com/photo-1605276374104-dee2a0ed3cd6?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-3-img-1')::uuid,  md5('hab-prop-3')::uuid,  'https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-3-img-2')::uuid,  md5('hab-prop-3')::uuid,  'https://images.unsplash.com/photo-1613490493576-7fde63acd811?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-4-img-1')::uuid,  md5('hab-prop-4')::uuid,  'https://images.unsplash.com/photo-1613490493576-7fde63acd811?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-4-img-2')::uuid,  md5('hab-prop-4')::uuid,  'https://images.unsplash.com/photo-1583608205776-bfd35f0d9f83?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-5-img-1')::uuid,  md5('hab-prop-5')::uuid,  'https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-5-img-2')::uuid,  md5('hab-prop-5')::uuid,  'https://images.unsplash.com/photo-1518883240204-f0a32e478486?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-6-img-1')::uuid,  md5('hab-prop-6')::uuid,  'https://images.unsplash.com/photo-1568605114967-8130f3a36994?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-6-img-2')::uuid,  md5('hab-prop-6')::uuid,  'https://images.unsplash.com/photo-1564013799919-ab600027ffc6?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-7-img-1')::uuid,  md5('hab-prop-7')::uuid,  'https://images.unsplash.com/photo-1605276374104-dee2a0ed3cd6?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-7-img-2')::uuid,  md5('hab-prop-7')::uuid,  'https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-8-img-1')::uuid,  md5('hab-prop-8')::uuid,  'https://images.unsplash.com/photo-1518883240204-f0a32e478486?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-8-img-2')::uuid,  md5('hab-prop-8')::uuid,  'https://images.unsplash.com/photo-1583608205776-bfd35f0d9f83?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-9-img-1')::uuid,  md5('hab-prop-9')::uuid,  'https://images.unsplash.com/photo-1583608205776-bfd35f0d9f83?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-9-img-2')::uuid,  md5('hab-prop-9')::uuid,  'https://images.unsplash.com/photo-1613490493576-7fde63acd811?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-10-img-1')::uuid, md5('hab-prop-10')::uuid, 'https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-10-img-2')::uuid, md5('hab-prop-10')::uuid, 'https://images.unsplash.com/photo-1605276374104-dee2a0ed3cd6?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-11-img-1')::uuid, md5('hab-prop-11')::uuid, 'https://images.unsplash.com/photo-1605276374104-dee2a0ed3cd6?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-11-img-2')::uuid, md5('hab-prop-11')::uuid, 'https://images.unsplash.com/photo-1518883240204-f0a32e478486?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-12-img-1')::uuid, md5('hab-prop-12')::uuid, 'https://images.unsplash.com/photo-1564013799919-ab600027ffc6?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-12-img-2')::uuid, md5('hab-prop-12')::uuid, 'https://images.unsplash.com/photo-1568605114967-8130f3a36994?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-13-img-1')::uuid, md5('hab-prop-13')::uuid, 'https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-13-img-2')::uuid, md5('hab-prop-13')::uuid, 'https://images.unsplash.com/photo-1583608205776-bfd35f0d9f83?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-14-img-1')::uuid, md5('hab-prop-14')::uuid, 'https://images.unsplash.com/photo-1613490493576-7fde63acd811?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-14-img-2')::uuid, md5('hab-prop-14')::uuid, 'https://images.unsplash.com/photo-1518883240204-f0a32e478486?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-15-img-1')::uuid, md5('hab-prop-15')::uuid, 'https://images.unsplash.com/photo-1583608205776-bfd35f0d9f83?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-15-img-2')::uuid, md5('hab-prop-15')::uuid, 'https://images.unsplash.com/photo-1568605114967-8130f3a36994?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-16-img-1')::uuid, md5('hab-prop-16')::uuid, 'https://images.unsplash.com/photo-1568605114967-8130f3a36994?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-16-img-2')::uuid, md5('hab-prop-16')::uuid, 'https://images.unsplash.com/photo-1605276374104-dee2a0ed3cd6?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-17-img-1')::uuid, md5('hab-prop-17')::uuid, 'https://images.unsplash.com/photo-1564013799919-ab600027ffc6?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-17-img-2')::uuid, md5('hab-prop-17')::uuid, 'https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-18-img-1')::uuid, md5('hab-prop-18')::uuid, 'https://images.unsplash.com/photo-1605276374104-dee2a0ed3cd6?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-18-img-2')::uuid, md5('hab-prop-18')::uuid, 'https://images.unsplash.com/photo-1518883240204-f0a32e478486?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-19-img-1')::uuid, md5('hab-prop-19')::uuid, 'https://images.unsplash.com/photo-1564013799919-ab600027ffc6?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-19-img-2')::uuid, md5('hab-prop-19')::uuid, 'https://images.unsplash.com/photo-1568605114967-8130f3a36994?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-20-img-1')::uuid, md5('hab-prop-20')::uuid, 'https://images.unsplash.com/photo-1583608205776-bfd35f0d9f83?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-20-img-2')::uuid, md5('hab-prop-20')::uuid, 'https://images.unsplash.com/photo-1613490493576-7fde63acd811?w=1600&q=80', FALSE, 1, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ── Unit images (interior shots) ─────────────────────────────────────
-- Three interior shots per unit, cycling through an interior set. First is
-- the cover.
INSERT INTO unit_images (id, unit_id, url, is_cover, sort_order, created_at, updated_at) VALUES
-- prop 1
(md5('hab-prop-1-unit-1-img-1')::uuid,  md5('hab-prop-1-unit-1')::uuid,  'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-1-unit-1-img-2')::uuid,  md5('hab-prop-1-unit-1')::uuid,  'https://images.unsplash.com/photo-1505691938895-1758d7feb511?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-1-unit-1-img-3')::uuid,  md5('hab-prop-1-unit-1')::uuid,  'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=1600&q=80', FALSE, 2, NOW(), NOW()),
-- prop 2 units
(md5('hab-prop-2-unit-1-img-1')::uuid,  md5('hab-prop-2-unit-1')::uuid,  'https://images.unsplash.com/photo-1505691938895-1758d7feb511?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-2-unit-1-img-2')::uuid,  md5('hab-prop-2-unit-1')::uuid,  'https://images.unsplash.com/photo-1600210492486-724fe5c67fb0?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-2-unit-1-img-3')::uuid,  md5('hab-prop-2-unit-1')::uuid,  'https://images.unsplash.com/photo-1540518614846-7eded433c457?w=1600&q=80', FALSE, 2, NOW(), NOW()),
(md5('hab-prop-2-unit-2-img-1')::uuid,  md5('hab-prop-2-unit-2')::uuid,  'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-2-unit-2-img-2')::uuid,  md5('hab-prop-2-unit-2')::uuid,  'https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-2-unit-2-img-3')::uuid,  md5('hab-prop-2-unit-2')::uuid,  'https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=1600&q=80', FALSE, 2, NOW(), NOW()),
(md5('hab-prop-2-unit-3-img-1')::uuid,  md5('hab-prop-2-unit-3')::uuid,  'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-2-unit-3-img-2')::uuid,  md5('hab-prop-2-unit-3')::uuid,  'https://images.unsplash.com/photo-1554995207-c18c203602cb?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-2-unit-3-img-3')::uuid,  md5('hab-prop-2-unit-3')::uuid,  'https://images.unsplash.com/photo-1600210492486-724fe5c67fb0?w=1600&q=80', FALSE, 2, NOW(), NOW()),
-- prop 3
(md5('hab-prop-3-unit-1-img-1')::uuid,  md5('hab-prop-3-unit-1')::uuid,  'https://images.unsplash.com/photo-1540518614846-7eded433c457?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-3-unit-1-img-2')::uuid,  md5('hab-prop-3-unit-1')::uuid,  'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-3-unit-1-img-3')::uuid,  md5('hab-prop-3-unit-1')::uuid,  'https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=1600&q=80', FALSE, 2, NOW(), NOW()),
-- prop 4 units
(md5('hab-prop-4-unit-1-img-1')::uuid,  md5('hab-prop-4-unit-1')::uuid,  'https://images.unsplash.com/photo-1554995207-c18c203602cb?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-4-unit-1-img-2')::uuid,  md5('hab-prop-4-unit-1')::uuid,  'https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-4-unit-1-img-3')::uuid,  md5('hab-prop-4-unit-1')::uuid,  'https://images.unsplash.com/photo-1600210492486-724fe5c67fb0?w=1600&q=80', FALSE, 2, NOW(), NOW()),
(md5('hab-prop-4-unit-2-img-1')::uuid,  md5('hab-prop-4-unit-2')::uuid,  'https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-4-unit-2-img-2')::uuid,  md5('hab-prop-4-unit-2')::uuid,  'https://images.unsplash.com/photo-1540518614846-7eded433c457?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-4-unit-2-img-3')::uuid,  md5('hab-prop-4-unit-2')::uuid,  'https://images.unsplash.com/photo-1505691938895-1758d7feb511?w=1600&q=80', FALSE, 2, NOW(), NOW()),
-- prop 5
(md5('hab-prop-5-unit-1-img-1')::uuid,  md5('hab-prop-5-unit-1')::uuid,  'https://images.unsplash.com/photo-1505691938895-1758d7feb511?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-5-unit-1-img-2')::uuid,  md5('hab-prop-5-unit-1')::uuid,  'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-5-unit-1-img-3')::uuid,  md5('hab-prop-5-unit-1')::uuid,  'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=1600&q=80', FALSE, 2, NOW(), NOW()),
-- prop 6
(md5('hab-prop-6-unit-1-img-1')::uuid,  md5('hab-prop-6-unit-1')::uuid,  'https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-6-unit-1-img-2')::uuid,  md5('hab-prop-6-unit-1')::uuid,  'https://images.unsplash.com/photo-1554995207-c18c203602cb?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-6-unit-1-img-3')::uuid,  md5('hab-prop-6-unit-1')::uuid,  'https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=1600&q=80', FALSE, 2, NOW(), NOW()),
-- prop 7
(md5('hab-prop-7-unit-1-img-1')::uuid,  md5('hab-prop-7-unit-1')::uuid,  'https://images.unsplash.com/photo-1600210492486-724fe5c67fb0?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-7-unit-1-img-2')::uuid,  md5('hab-prop-7-unit-1')::uuid,  'https://images.unsplash.com/photo-1540518614846-7eded433c457?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-7-unit-1-img-3')::uuid,  md5('hab-prop-7-unit-1')::uuid,  'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=1600&q=80', FALSE, 2, NOW(), NOW()),
-- prop 8
(md5('hab-prop-8-unit-1-img-1')::uuid,  md5('hab-prop-8-unit-1')::uuid,  'https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-8-unit-1-img-2')::uuid,  md5('hab-prop-8-unit-1')::uuid,  'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-8-unit-1-img-3')::uuid,  md5('hab-prop-8-unit-1')::uuid,  'https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=1600&q=80', FALSE, 2, NOW(), NOW()),
-- prop 9
(md5('hab-prop-9-unit-1-img-1')::uuid,  md5('hab-prop-9-unit-1')::uuid,  'https://images.unsplash.com/photo-1554995207-c18c203602cb?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-9-unit-1-img-2')::uuid,  md5('hab-prop-9-unit-1')::uuid,  'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-9-unit-1-img-3')::uuid,  md5('hab-prop-9-unit-1')::uuid,  'https://images.unsplash.com/photo-1540518614846-7eded433c457?w=1600&q=80', FALSE, 2, NOW(), NOW()),
-- prop 10
(md5('hab-prop-10-unit-1-img-1')::uuid, md5('hab-prop-10-unit-1')::uuid, 'https://images.unsplash.com/photo-1505691938895-1758d7feb511?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-10-unit-1-img-2')::uuid, md5('hab-prop-10-unit-1')::uuid, 'https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-10-unit-1-img-3')::uuid, md5('hab-prop-10-unit-1')::uuid, 'https://images.unsplash.com/photo-1600210492486-724fe5c67fb0?w=1600&q=80', FALSE, 2, NOW(), NOW()),
-- prop 11 units
(md5('hab-prop-11-unit-1-img-1')::uuid, md5('hab-prop-11-unit-1')::uuid, 'https://images.unsplash.com/photo-1540518614846-7eded433c457?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-11-unit-1-img-2')::uuid, md5('hab-prop-11-unit-1')::uuid, 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-11-unit-1-img-3')::uuid, md5('hab-prop-11-unit-1')::uuid, 'https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=1600&q=80', FALSE, 2, NOW(), NOW()),
(md5('hab-prop-11-unit-2-img-1')::uuid, md5('hab-prop-11-unit-2')::uuid, 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-11-unit-2-img-2')::uuid, md5('hab-prop-11-unit-2')::uuid, 'https://images.unsplash.com/photo-1554995207-c18c203602cb?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-11-unit-2-img-3')::uuid, md5('hab-prop-11-unit-2')::uuid, 'https://images.unsplash.com/photo-1600210492486-724fe5c67fb0?w=1600&q=80', FALSE, 2, NOW(), NOW()),
-- prop 12
(md5('hab-prop-12-unit-1-img-1')::uuid, md5('hab-prop-12-unit-1')::uuid, 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-12-unit-1-img-2')::uuid, md5('hab-prop-12-unit-1')::uuid, 'https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-12-unit-1-img-3')::uuid, md5('hab-prop-12-unit-1')::uuid, 'https://images.unsplash.com/photo-1505691938895-1758d7feb511?w=1600&q=80', FALSE, 2, NOW(), NOW()),
-- prop 13
(md5('hab-prop-13-unit-1-img-1')::uuid, md5('hab-prop-13-unit-1')::uuid, 'https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-13-unit-1-img-2')::uuid, md5('hab-prop-13-unit-1')::uuid, 'https://images.unsplash.com/photo-1554995207-c18c203602cb?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-13-unit-1-img-3')::uuid, md5('hab-prop-13-unit-1')::uuid, 'https://images.unsplash.com/photo-1540518614846-7eded433c457?w=1600&q=80', FALSE, 2, NOW(), NOW()),
-- prop 14
(md5('hab-prop-14-unit-1-img-1')::uuid, md5('hab-prop-14-unit-1')::uuid, 'https://images.unsplash.com/photo-1600210492486-724fe5c67fb0?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-14-unit-1-img-2')::uuid, md5('hab-prop-14-unit-1')::uuid, 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-14-unit-1-img-3')::uuid, md5('hab-prop-14-unit-1')::uuid, 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=1600&q=80', FALSE, 2, NOW(), NOW()),
-- prop 15
(md5('hab-prop-15-unit-1-img-1')::uuid, md5('hab-prop-15-unit-1')::uuid, 'https://images.unsplash.com/photo-1554995207-c18c203602cb?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-15-unit-1-img-2')::uuid, md5('hab-prop-15-unit-1')::uuid, 'https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-15-unit-1-img-3')::uuid, md5('hab-prop-15-unit-1')::uuid, 'https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=1600&q=80', FALSE, 2, NOW(), NOW()),
-- prop 16
(md5('hab-prop-16-unit-1-img-1')::uuid, md5('hab-prop-16-unit-1')::uuid, 'https://images.unsplash.com/photo-1540518614846-7eded433c457?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-16-unit-1-img-2')::uuid, md5('hab-prop-16-unit-1')::uuid, 'https://images.unsplash.com/photo-1505691938895-1758d7feb511?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-16-unit-1-img-3')::uuid, md5('hab-prop-16-unit-1')::uuid, 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=1600&q=80', FALSE, 2, NOW(), NOW()),
-- prop 17
(md5('hab-prop-17-unit-1-img-1')::uuid, md5('hab-prop-17-unit-1')::uuid, 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-17-unit-1-img-2')::uuid, md5('hab-prop-17-unit-1')::uuid, 'https://images.unsplash.com/photo-1600210492486-724fe5c67fb0?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-17-unit-1-img-3')::uuid, md5('hab-prop-17-unit-1')::uuid, 'https://images.unsplash.com/photo-1554995207-c18c203602cb?w=1600&q=80', FALSE, 2, NOW(), NOW()),
-- prop 18
(md5('hab-prop-18-unit-1-img-1')::uuid, md5('hab-prop-18-unit-1')::uuid, 'https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-18-unit-1-img-2')::uuid, md5('hab-prop-18-unit-1')::uuid, 'https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-18-unit-1-img-3')::uuid, md5('hab-prop-18-unit-1')::uuid, 'https://images.unsplash.com/photo-1540518614846-7eded433c457?w=1600&q=80', FALSE, 2, NOW(), NOW()),
-- prop 19
(md5('hab-prop-19-unit-1-img-1')::uuid, md5('hab-prop-19-unit-1')::uuid, 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-19-unit-1-img-2')::uuid, md5('hab-prop-19-unit-1')::uuid, 'https://images.unsplash.com/photo-1554995207-c18c203602cb?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-19-unit-1-img-3')::uuid, md5('hab-prop-19-unit-1')::uuid, 'https://images.unsplash.com/photo-1505691938895-1758d7feb511?w=1600&q=80', FALSE, 2, NOW(), NOW()),
-- prop 20
(md5('hab-prop-20-unit-1-img-1')::uuid, md5('hab-prop-20-unit-1')::uuid, 'https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-20-unit-1-img-2')::uuid, md5('hab-prop-20-unit-1')::uuid, 'https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-20-unit-1-img-3')::uuid, md5('hab-prop-20-unit-1')::uuid, 'https://images.unsplash.com/photo-1600210492486-724fe5c67fb0?w=1600&q=80', FALSE, 2, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

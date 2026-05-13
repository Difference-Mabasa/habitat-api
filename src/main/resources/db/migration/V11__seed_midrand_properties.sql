-- 10 additional premium properties in Midrand to balance the V10 seed
-- (which skewed heavily to Joburg North, Cape Town and KZN).
--
-- Suburbs targeted are the premium / lifestyle-estate clusters in Midrand:
-- Steyn City, Waterfall Estate, Waterfall Country Estate, Blue Hills,
-- Carlswald, Glen Austin, Crowthorne, Vorna Valley.
--
-- All listings continue to point at user #2 (Thandi) as landlord + manager —
-- agent-managed listings land with the mandate slice later. Idempotent via
-- md5-derived UUIDs + ON CONFLICT DO NOTHING.

INSERT INTO properties (
    id, landlord_id, manager_id, title, description, property_type, status,
    address_line, suburb, city, province, postal_code, latitude, longitude,
    created_at, updated_at
) VALUES
(md5('hab-prop-21')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Steyn City Mansion',                  'Four-bedroom mansion in Steyn City. Lifestyle estate with the parkland golf course, padel courts and on-site spa.',
 'HOUSE',              'LISTED',
 '15 Inverness Drive', 'Steyn City', 'Midrand', 'Gauteng', '2191', -25.9831, 28.0140, NOW(), NOW()),
(md5('hab-prop-22')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Steyn City Lakeside Townhouse',       'Three-bedroom townhouse on the Steyn City lake edge. Fibre, two-car garage, walking distance to the City Centre amenities.',
 'TOWNHOUSE_COMPLEX',  'LISTED',
 '8 Lakeshore Boulevard', 'Steyn City', 'Midrand', 'Gauteng', '2191', -25.9802, 28.0123, NOW(), NOW()),
(md5('hab-prop-23')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Waterfall Estate Family Home',        'Four-bedroom family home in Waterfall Equestrian. Pool, study, generator-ready inverter system.',
 'HOUSE',              'LISTED',
 '24 Polo Lane', 'Waterfall Estate', 'Midrand', 'Gauteng', '1685', -25.9988, 28.1023, NOW(), NOW()),
(md5('hab-prop-24')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Waterfall Country Estate Townhouse',  'Three-bedroom townhouse in Waterfall Country Estate. Clubhouse access, Mall of Africa five minutes away.',
 'TOWNHOUSE_COMPLEX',  'LISTED',
 '11 Waterfall Drive', 'Waterfall Country Estate', 'Midrand', 'Gauteng', '1685', -25.9924, 28.1098, NOW(), NOW()),
(md5('hab-prop-25')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Blue Hills Family Home',              'Four-bedroom home on a generous Blue Hills stand. Borehole, pool, separate cottage suitable for a domestic worker.',
 'HOUSE',              'LISTED',
 '21 Hawk Avenue', 'Blue Hills', 'Midrand', 'Gauteng', '1685', -25.9282, 28.1051, NOW(), NOW()),
(md5('hab-prop-26')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Carlswald Equestrian Estate',         'Five-bedroom estate house in Carlswald. Stable yard for two horses, paddocks, riding trail access.',
 'HOUSE',              'LISTED',
 '3 Bridle Path', 'Carlswald', 'Midrand', 'Gauteng', '1684', -26.0026, 28.1142, NOW(), NOW()),
(md5('hab-prop-27')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Crowthorne Country Home',             'Three-bedroom country home on a 1ha smallholding in Crowthorne. Vegetable garden, chicken run, four-car garage.',
 'HOUSE',              'LISTED',
 '7 Acorn Road', 'Crowthorne', 'Midrand', 'Gauteng', '1684', -25.9651, 28.0843, NOW(), NOW()),
(md5('hab-prop-28')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Vorna Valley Townhouse',              'Two-bedroom townhouse in a secure Vorna Valley complex. Communal pool, fibre, walking distance to Gautrain feeder.',
 'TOWNHOUSE_COMPLEX',  'LISTED',
 '14 Birchwood Crescent', 'Vorna Valley', 'Midrand', 'Gauteng', '1686', -25.9985, 28.1247, NOW(), NOW()),
(md5('hab-prop-29')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'The Reserve · Waterfall Apartments',  'New-build apartment block in Waterfall City. Concierge, gym, walking distance to Mall of Africa. Two flagship units currently listed.',
 'APARTMENT_BLOCK',    'LISTED',
 '1 Magwa Crescent', 'Waterfall City', 'Midrand', 'Gauteng', '1685', -25.9961, 28.1107, NOW(), NOW()),
(md5('hab-prop-30')::uuid, '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002',
 'Glen Austin Smallholding',            'Three-bedroom home on a 2ha Glen Austin smallholding. Stables, dam, north-facing pool, perfect for hobbyist farming.',
 'HOUSE',              'LISTED',
 '42 Plot Road', 'Glen Austin', 'Midrand', 'Gauteng', '1685', -25.9558, 28.1389, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ── Units ────────────────────────────────────────────────────────────
-- Most properties have one unit; The Reserve (prop 29) has 2.
INSERT INTO units (
    id, property_id, unit_number, title, description, unit_type, status,
    furnishing, price, payment_frequency, deposit, bedrooms, bathrooms, sqm,
    water_included, electricity_included, pets_allowed, available_from,
    created_at, updated_at
) VALUES
(md5('hab-prop-21-unit-1')::uuid, md5('hab-prop-21')::uuid, NULL,  'Steyn City Mansion',                'Full house listing.',                  'HOUSE',     'AVAILABLE', 'UNFURNISHED',   55000.00, 'MONTHLY', 110000.00, 4, 4, 380, FALSE, FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-22-unit-1')::uuid, md5('hab-prop-22')::uuid, NULL,  'Lakeside Townhouse',                'End unit, lake-facing.',               'TOWNHOUSE', 'AVAILABLE', 'SEMI_FURNISHED', 35000.00, 'MONTHLY', 70000.00,  3, 3, 220, TRUE,  FALSE, TRUE,  CURRENT_DATE + INTERVAL '14 days', NOW(), NOW()),
(md5('hab-prop-23-unit-1')::uuid, md5('hab-prop-23')::uuid, NULL,  'Waterfall Estate Home',             'Four-bedroom with pool.',              'HOUSE',     'AVAILABLE', 'UNFURNISHED',   48000.00, 'MONTHLY', 96000.00,  4, 3, 320, FALSE, FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-24-unit-1')::uuid, md5('hab-prop-24')::uuid, '11',  'Country Estate Townhouse',          'Three-bed in secure estate.',          'TOWNHOUSE', 'AVAILABLE', 'UNFURNISHED',   32000.00, 'MONTHLY', 64000.00,  3, 2, 195, FALSE, FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-25-unit-1')::uuid, md5('hab-prop-25')::uuid, NULL,  'Blue Hills Family Home',            'Main house. Cottage rents separately.','HOUSE',     'AVAILABLE', 'UNFURNISHED',   28000.00, 'MONTHLY', 56000.00,  4, 3, 290, TRUE,  FALSE, TRUE,  CURRENT_DATE + INTERVAL '21 days', NOW(), NOW()),
(md5('hab-prop-26-unit-1')::uuid, md5('hab-prop-26')::uuid, NULL,  'Carlswald Equestrian Estate',       'Five-bed with stables.',               'HOUSE',     'AVAILABLE', 'UNFURNISHED',   45000.00, 'MONTHLY', 90000.00,  5, 4, 410, FALSE, FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-27-unit-1')::uuid, md5('hab-prop-27')::uuid, NULL,  'Crowthorne Country Home',           'Three-bed smallholding.',              'HOUSE',     'AVAILABLE', 'UNFURNISHED',   22000.00, 'MONTHLY', 44000.00,  3, 2, 220, FALSE, FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-28-unit-1')::uuid, md5('hab-prop-28')::uuid, '14',  'Vorna Valley Townhouse',            'Two-bed townhouse.',                   'TOWNHOUSE', 'AVAILABLE', 'SEMI_FURNISHED', 18000.00, 'MONTHLY', 36000.00,  2, 2, 130, TRUE,  FALSE, TRUE,  CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-30-unit-1')::uuid, md5('hab-prop-30')::uuid, NULL,  'Glen Austin Smallholding',          'Three-bed on a 2ha plot.',             'HOUSE',     'AVAILABLE', 'UNFURNISHED',   32000.00, 'MONTHLY', 64000.00,  3, 2, 260, FALSE, FALSE, TRUE,  CURRENT_DATE + INTERVAL '7 days',  NOW(), NOW()),
-- Property 29 (Waterfall City apartment block): 2 units
(md5('hab-prop-29-unit-1')::uuid, md5('hab-prop-29')::uuid, '305', 'Reserve 1-Bedroom',                 'Compact one-bedroom apartment.',       'APARTMENT', 'AVAILABLE', 'SEMI_FURNISHED', 14000.00, 'MONTHLY', 28000.00,  1, 1,  62, TRUE,  FALSE, FALSE, CURRENT_DATE,                NOW(), NOW()),
(md5('hab-prop-29-unit-2')::uuid, md5('hab-prop-29')::uuid, '801', 'Reserve 2-Bedroom',                 'Two-bedroom with city views.',         'APARTMENT', 'AVAILABLE', 'FURNISHED',     22000.00, 'MONTHLY', 44000.00,  2, 2, 105, TRUE,  TRUE,  FALSE, CURRENT_DATE + INTERVAL '14 days', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ── Property images ──────────────────────────────────────────────────
INSERT INTO property_images (id, property_id, url, is_cover, sort_order, created_at, updated_at) VALUES
(md5('hab-prop-21-img-1')::uuid, md5('hab-prop-21')::uuid, 'https://images.unsplash.com/photo-1564013799919-ab600027ffc6?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-21-img-2')::uuid, md5('hab-prop-21')::uuid, 'https://images.unsplash.com/photo-1568605114967-8130f3a36994?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-22-img-1')::uuid, md5('hab-prop-22')::uuid, 'https://images.unsplash.com/photo-1568605114967-8130f3a36994?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-22-img-2')::uuid, md5('hab-prop-22')::uuid, 'https://images.unsplash.com/photo-1613490493576-7fde63acd811?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-23-img-1')::uuid, md5('hab-prop-23')::uuid, 'https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-23-img-2')::uuid, md5('hab-prop-23')::uuid, 'https://images.unsplash.com/photo-1583608205776-bfd35f0d9f83?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-24-img-1')::uuid, md5('hab-prop-24')::uuid, 'https://images.unsplash.com/photo-1605276374104-dee2a0ed3cd6?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-24-img-2')::uuid, md5('hab-prop-24')::uuid, 'https://images.unsplash.com/photo-1518883240204-f0a32e478486?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-25-img-1')::uuid, md5('hab-prop-25')::uuid, 'https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-25-img-2')::uuid, md5('hab-prop-25')::uuid, 'https://images.unsplash.com/photo-1564013799919-ab600027ffc6?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-26-img-1')::uuid, md5('hab-prop-26')::uuid, 'https://images.unsplash.com/photo-1583608205776-bfd35f0d9f83?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-26-img-2')::uuid, md5('hab-prop-26')::uuid, 'https://images.unsplash.com/photo-1613490493576-7fde63acd811?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-27-img-1')::uuid, md5('hab-prop-27')::uuid, 'https://images.unsplash.com/photo-1568605114967-8130f3a36994?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-27-img-2')::uuid, md5('hab-prop-27')::uuid, 'https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-28-img-1')::uuid, md5('hab-prop-28')::uuid, 'https://images.unsplash.com/photo-1518883240204-f0a32e478486?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-28-img-2')::uuid, md5('hab-prop-28')::uuid, 'https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-29-img-1')::uuid, md5('hab-prop-29')::uuid, 'https://images.unsplash.com/photo-1605276374104-dee2a0ed3cd6?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-29-img-2')::uuid, md5('hab-prop-29')::uuid, 'https://images.unsplash.com/photo-1583608205776-bfd35f0d9f83?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-30-img-1')::uuid, md5('hab-prop-30')::uuid, 'https://images.unsplash.com/photo-1564013799919-ab600027ffc6?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-30-img-2')::uuid, md5('hab-prop-30')::uuid, 'https://images.unsplash.com/photo-1568605114967-8130f3a36994?w=1600&q=80', FALSE, 1, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ── Unit images ──────────────────────────────────────────────────────
INSERT INTO unit_images (id, unit_id, url, is_cover, sort_order, created_at, updated_at) VALUES
(md5('hab-prop-21-unit-1-img-1')::uuid, md5('hab-prop-21-unit-1')::uuid, 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-21-unit-1-img-2')::uuid, md5('hab-prop-21-unit-1')::uuid, 'https://images.unsplash.com/photo-1505691938895-1758d7feb511?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-21-unit-1-img-3')::uuid, md5('hab-prop-21-unit-1')::uuid, 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=1600&q=80', FALSE, 2, NOW(), NOW()),
(md5('hab-prop-22-unit-1-img-1')::uuid, md5('hab-prop-22-unit-1')::uuid, 'https://images.unsplash.com/photo-1540518614846-7eded433c457?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-22-unit-1-img-2')::uuid, md5('hab-prop-22-unit-1')::uuid, 'https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-22-unit-1-img-3')::uuid, md5('hab-prop-22-unit-1')::uuid, 'https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=1600&q=80', FALSE, 2, NOW(), NOW()),
(md5('hab-prop-23-unit-1-img-1')::uuid, md5('hab-prop-23-unit-1')::uuid, 'https://images.unsplash.com/photo-1554995207-c18c203602cb?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-23-unit-1-img-2')::uuid, md5('hab-prop-23-unit-1')::uuid, 'https://images.unsplash.com/photo-1600210492486-724fe5c67fb0?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-23-unit-1-img-3')::uuid, md5('hab-prop-23-unit-1')::uuid, 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=1600&q=80', FALSE, 2, NOW(), NOW()),
(md5('hab-prop-24-unit-1-img-1')::uuid, md5('hab-prop-24-unit-1')::uuid, 'https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-24-unit-1-img-2')::uuid, md5('hab-prop-24-unit-1')::uuid, 'https://images.unsplash.com/photo-1505691938895-1758d7feb511?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-24-unit-1-img-3')::uuid, md5('hab-prop-24-unit-1')::uuid, 'https://images.unsplash.com/photo-1540518614846-7eded433c457?w=1600&q=80', FALSE, 2, NOW(), NOW()),
(md5('hab-prop-25-unit-1-img-1')::uuid, md5('hab-prop-25-unit-1')::uuid, 'https://images.unsplash.com/photo-1600210492486-724fe5c67fb0?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-25-unit-1-img-2')::uuid, md5('hab-prop-25-unit-1')::uuid, 'https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-25-unit-1-img-3')::uuid, md5('hab-prop-25-unit-1')::uuid, 'https://images.unsplash.com/photo-1554995207-c18c203602cb?w=1600&q=80', FALSE, 2, NOW(), NOW()),
(md5('hab-prop-26-unit-1-img-1')::uuid, md5('hab-prop-26-unit-1')::uuid, 'https://images.unsplash.com/photo-1505691938895-1758d7feb511?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-26-unit-1-img-2')::uuid, md5('hab-prop-26-unit-1')::uuid, 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-26-unit-1-img-3')::uuid, md5('hab-prop-26-unit-1')::uuid, 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=1600&q=80', FALSE, 2, NOW(), NOW()),
(md5('hab-prop-27-unit-1-img-1')::uuid, md5('hab-prop-27-unit-1')::uuid, 'https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-27-unit-1-img-2')::uuid, md5('hab-prop-27-unit-1')::uuid, 'https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-27-unit-1-img-3')::uuid, md5('hab-prop-27-unit-1')::uuid, 'https://images.unsplash.com/photo-1554995207-c18c203602cb?w=1600&q=80', FALSE, 2, NOW(), NOW()),
(md5('hab-prop-28-unit-1-img-1')::uuid, md5('hab-prop-28-unit-1')::uuid, 'https://images.unsplash.com/photo-1540518614846-7eded433c457?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-28-unit-1-img-2')::uuid, md5('hab-prop-28-unit-1')::uuid, 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-28-unit-1-img-3')::uuid, md5('hab-prop-28-unit-1')::uuid, 'https://images.unsplash.com/photo-1600210492486-724fe5c67fb0?w=1600&q=80', FALSE, 2, NOW(), NOW()),
(md5('hab-prop-29-unit-1-img-1')::uuid, md5('hab-prop-29-unit-1')::uuid, 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-29-unit-1-img-2')::uuid, md5('hab-prop-29-unit-1')::uuid, 'https://images.unsplash.com/photo-1505691938895-1758d7feb511?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-29-unit-1-img-3')::uuid, md5('hab-prop-29-unit-1')::uuid, 'https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=1600&q=80', FALSE, 2, NOW(), NOW()),
(md5('hab-prop-29-unit-2-img-1')::uuid, md5('hab-prop-29-unit-2')::uuid, 'https://images.unsplash.com/photo-1554995207-c18c203602cb?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-29-unit-2-img-2')::uuid, md5('hab-prop-29-unit-2')::uuid, 'https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-29-unit-2-img-3')::uuid, md5('hab-prop-29-unit-2')::uuid, 'https://images.unsplash.com/photo-1540518614846-7eded433c457?w=1600&q=80', FALSE, 2, NOW(), NOW()),
(md5('hab-prop-30-unit-1-img-1')::uuid, md5('hab-prop-30-unit-1')::uuid, 'https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=1600&q=80', TRUE,  0, NOW(), NOW()),
(md5('hab-prop-30-unit-1-img-2')::uuid, md5('hab-prop-30-unit-1')::uuid, 'https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=1600&q=80', FALSE, 1, NOW(), NOW()),
(md5('hab-prop-30-unit-1-img-3')::uuid, md5('hab-prop-30-unit-1')::uuid, 'https://images.unsplash.com/photo-1505691938895-1758d7feb511?w=1600&q=80', FALSE, 2, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

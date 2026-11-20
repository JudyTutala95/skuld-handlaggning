-- Exempeldata. ALLA personer är påhittade. Personnumren kommer från Skatteverkets
-- officiella lista med testpersonnummer, som inte tillhör riktiga personer.

INSERT INTO galdenar (personnummer, fornamn, efternamn, adress, nettoinkomst_per_manad, boendekostnad_per_manad, sammanlevande) VALUES
    ('198503152384', 'Anna',  'Exempelsson', 'Testgatan 1, 111 11 Stockholm', 28000.00, 7500.00, FALSE),
    ('199207042392', 'Bertil', 'Påhittad',   'Provvägen 2, 222 22 Lund',      34000.00, 11000.00, TRUE),
    ('197802112388', 'Cecilia', 'Fiktiv',    'Låtsasgränd 3, 333 33 Umeå',    16500.00, 6200.00, FALSE);

-- Bertil bor med sambo och två barn
INSERT INTO galdenar_barn (galdenar_id, rad, fodelsear) VALUES (2, 0, 2018), (2, 1, 2012);

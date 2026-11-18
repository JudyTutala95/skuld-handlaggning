-- Exempeldata. ALLA personer är påhittade och personnumren är fiktiva testnummer
-- (födelsenummer 98x, som inte delas ut till riktiga personer).

INSERT INTO galdenar (personnummer, fornamn, efternamn, adress, nettoinkomst_per_manad, boendekostnad_per_manad, sammanlevande) VALUES
    ('198503152384', 'Anna',  'Exempelsson', 'Testgatan 1, 111 11 Stockholm', 28000.00, 7500.00, FALSE),
    ('199207042392', 'Bertil', 'Påhittad',   'Provvägen 2, 222 22 Lund',      34000.00, 11000.00, TRUE),
    ('197802112388', 'Cecilia', 'Fiktiv',    'Låtsasgränd 3, 333 33 Umeå',    16500.00, 6200.00, FALSE);

-- Bertil bor med sambo och två barn
INSERT INTO galdenar_barn (galdenar_id, rad, fodelsear) VALUES (2, 0, 2018), (2, 1, 2012);

-- Ärende 1: betalningsföreläggande, nyss registrerat
INSERT INTO arende (typ, status, maltyp, galdenar_id, borgenar, kapitalbelopp, forfallodag, konsumentfordran, handlaggare, registrerad) VALUES
    ('BETALNINGSFORELAGGANDE', 'REGISTRERAT', 'ENSKILT', 1, 'Exempel Inkasso AB', 12500.00, DATE '2026-03-31', TRUE, 'handlaggare.lisa', DATE '2026-05-04');
INSERT INTO arende_dagbok (arende_id, rad, datum, handelse) VALUES
    (1, 0, DATE '2026-05-04', 'Ärendet registrerat (BETALNINGSFORELAGGANDE, enskilt mål). Borgenär: Exempel Inkasso AB. Belopp: 12500.00 kr.');

-- Ärende 2: allmänt mål (skatteskuld) under utredning
INSERT INTO arende (typ, status, maltyp, galdenar_id, borgenar, kapitalbelopp, forfallodag, konsumentfordran, handlaggare, registrerad) VALUES
    ('UTSOKNING', 'UTREDNING', 'ALLMANT', 2, 'Skatteverket', 48300.00, DATE '2025-05-12', FALSE, 'handlaggare.omar', DATE '2025-09-01');
INSERT INTO arende_dagbok (arende_id, rad, datum, handelse) VALUES
    (2, 0, DATE '2025-09-01', 'Ärendet registrerat (UTSOKNING, allmant mål). Borgenär: Skatteverket. Belopp: 48300.00 kr.'),
    (2, 1, DATE '2025-09-15', 'Utredning av gäldenärens tillgångar och inkomster påbörjad.');

-- Ärende 3: ansökan om skuldsanering
INSERT INTO arende (typ, status, maltyp, galdenar_id, borgenar, kapitalbelopp, forfallodag, konsumentfordran, handlaggare, registrerad) VALUES
    ('SKULDSANERING', 'REGISTRERAT', 'ENSKILT', 3, 'Flera borgenärer', 612000.00, DATE '2019-01-31', FALSE, 'handlaggare.lisa', DATE '2026-10-20');
INSERT INTO arende_dagbok (arende_id, rad, datum, handelse) VALUES
    (3, 0, DATE '2026-10-20', 'Ärendet registrerat (SKULDSANERING, enskilt mål). Borgenär: Flera borgenärer. Belopp: 612000.00 kr.');

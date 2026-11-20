-- Exempeldata. galdenar_id 1–3 är Anna, Bertil och Cecilia i gäldenärstjänstens databas.

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

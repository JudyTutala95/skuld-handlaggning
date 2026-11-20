/// Ärendetjänsten. Underpaketen är lagren `web` → `service` → `repository`, med `dto`,
/// `domain` och `regelverk` vid sidan av. `integration` pratar med andra tjänster.
/// Se `LagerArkitekturTest`.
///
/// Typerna direkt i det här paketet (`ArendeStatusAndrad`, `Arendestatus`, `Arendetyp`)
/// beskriver händelsen som skickas till Kafka.
@NullMarked
package se.jtutala.handlaggning.arende;

import org.jspecify.annotations.NullMarked;

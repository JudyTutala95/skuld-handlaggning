package se.jtutala.handlaggning.arende;

import static se.jtutala.handlaggning.arende.Arendestatus.AVSKRIVET;
import static se.jtutala.handlaggning.arende.Arendestatus.BESTRITT;
import static se.jtutala.handlaggning.arende.Arendestatus.BETALT;
import static se.jtutala.handlaggning.arende.Arendestatus.DELGIVET;
import static se.jtutala.handlaggning.arende.Arendestatus.LONEUTMATNING;
import static se.jtutala.handlaggning.arende.Arendestatus.OVERLAMNAT_TILL_TINGSRATT;
import static se.jtutala.handlaggning.arende.Arendestatus.PRESKRIBERAT;
import static se.jtutala.handlaggning.arende.Arendestatus.REGISTRERAT;
import static se.jtutala.handlaggning.arende.Arendestatus.SKULDSANERING_AVSLAG;
import static se.jtutala.handlaggning.arende.Arendestatus.SKULDSANERING_PAGAR;
import static se.jtutala.handlaggning.arende.Arendestatus.SKULDSANERING_SLUTFORD;
import static se.jtutala.handlaggning.arende.Arendestatus.UTREDNING;
import static se.jtutala.handlaggning.arende.Arendestatus.UTSLAG_MEDDELAT;

import java.util.Map;
import java.util.Set;

/// De tre ärendetyperna och deras **statusflöden** (tillståndsmaskiner).
///
/// All kunskap om "vilken status får följa på vilken" finns på ETT ställe – här.
/// Vill man ändra processen ändrar man i tabellerna nedan.
public enum Arendetyp {

    /// **Betalningsföreläggande** – en snabb, förenklad process för att få fastställt att
    /// en skuld finns. Borgenären ansöker, gäldenären delges och får (normalt) 10 dagar
    /// att bestrida. Bestrider gäldenären inte meddelas ett *utslag*, som är en
    /// exekutionstitel – samma verkan som en dom. Bestrids kravet kan borgenären begära
    /// att målet överlämnas till tingsrätten.
    ///
    /// ```
    /// REGISTRERAT ─▶ DELGIVET ─┬─▶ UTSLAG_MEDDELAT        (inget bestridande inom fristen)
    ///                          └─▶ BESTRITT ─┬─▶ OVERLAMNAT_TILL_TINGSRATT
    ///                                        └─▶ AVSKRIVET
    /// ```
    BETALNINGSFORELAGGANDE("BF", Map.of(
            REGISTRERAT, Set.of(DELGIVET, AVSKRIVET),
            DELGIVET, Set.of(BESTRITT, UTSLAG_MEDDELAT, AVSKRIVET),
            BESTRITT, Set.of(OVERLAMNAT_TILL_TINGSRATT, AVSKRIVET))),

    /// **Utsökning (indrivning)** – Kronofogden driver in en skuld som är fastställd.
    /// Utreder gäldenärens tillgångar och inkomster och kan besluta om t.ex. löneutmätning:
    /// arbetsgivaren drar en del av lönen och betalar till Kronofogden.
    ///
    /// ```
    /// REGISTRERAT ─▶ UTREDNING ─▶ LONEUTMATNING ─▶ BETALT
    ///      (alla aktiva statusar kan också gå till BETALT, PRESKRIBERAT eller AVSKRIVET)
    /// ```
    UTSOKNING("UTS", Map.of(
            REGISTRERAT, Set.of(UTREDNING, BETALT, PRESKRIBERAT, AVSKRIVET),
            UTREDNING, Set.of(LONEUTMATNING, BETALT, PRESKRIBERAT, AVSKRIVET),
            LONEUTMATNING, Set.of(BETALT, PRESKRIBERAT, AVSKRIVET))),

    /// **Skuldsanering** – för den som är så skuldsatt att den inte kan betala inom
    /// överskådlig tid. Beviljas skuldsanering betalar personen enligt en plan i 5 år,
    /// därefter är skulderna som ingick borta.
    ///
    /// ```
    /// REGISTRERAT ─▶ UTREDNING ─┬─▶ SKULDSANERING_PAGAR ─▶ SKULDSANERING_SLUTFORD  (efter 5 år)
    ///                           └─▶ SKULDSANERING_AVSLAG
    /// ```
    SKULDSANERING("SKS", Map.of(
            REGISTRERAT, Set.of(UTREDNING, AVSKRIVET),
            UTREDNING, Set.of(SKULDSANERING_PAGAR, SKULDSANERING_AVSLAG, AVSKRIVET),
            SKULDSANERING_PAGAR, Set.of(SKULDSANERING_SLUTFORD)));

    private final String prefix;
    private final Map<Arendestatus, Set<Arendestatus>> overgangar;

    Arendetyp(String prefix, Map<Arendestatus, Set<Arendestatus>> overgangar) {
        this.prefix = prefix;
        this.overgangar = overgangar;
    }

    /// Används i ärendenumret, t.ex. `UTS-2026-000042`.
    public String prefix() {
        return prefix;
    }

    public Set<Arendestatus> tillatnaOvergangar(Arendestatus fran) {
        return overgangar.getOrDefault(fran, Set.of());
    }
}

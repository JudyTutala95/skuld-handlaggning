package se.jtutala.handlaggning.arende;

/// Alla statusar ett ärende kan ha. Vilka övergångar som är tillåtna beror på
/// ärendetypen – se `Arendetyp#tillatnaOvergangar`.
public enum Arendestatus {

    // Gemensamma
    REGISTRERAT("Registrerat", false),
    UTREDNING("Under utredning", false),
    AVSKRIVET("Avskrivet", true),

    // Betalningsföreläggande
    DELGIVET("Delgivet – väntar på svar", false),
    BESTRITT("Bestritt av gäldenären", false),
    UTSLAG_MEDDELAT("Utslag meddelat", true),
    OVERLAMNAT_TILL_TINGSRATT("Överlämnat till tingsrätten", true),

    // Utsökning
    LONEUTMATNING("Löneutmätning pågår", false),
    BETALT("Skulden är betald", true),
    PRESKRIBERAT("Skulden är preskriberad", true),

    // Skuldsanering
    SKULDSANERING_PAGAR("Skuldsanering pågår", false),
    SKULDSANERING_AVSLAG("Ansökan om skuldsanering avslagen", true),
    SKULDSANERING_SLUTFORD("Skuldsanering slutförd", true);

    private final String beskrivning;
    private final boolean avslutad;

    Arendestatus(String beskrivning, boolean avslutad) {
        this.beskrivning = beskrivning;
        this.avslutad = avslutad;
    }

    public String beskrivning() {
        return beskrivning;
    }

    /// Ett avslutat ärende kan inte ändras längre.
    public boolean avslutad() {
        return avslutad;
    }
}

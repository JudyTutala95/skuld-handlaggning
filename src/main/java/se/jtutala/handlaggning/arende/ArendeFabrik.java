package se.jtutala.handlaggning.arende;

import java.time.LocalDate;

import se.jtutala.handlaggning.regelverk.Maltyp;

/// Ansvar: skapa **rätt sorts ärende** utifrån ärendetypen.
final class ArendeFabrik {

    private ArendeFabrik() {
    }

    static Arende skapa(Arendetyp typ, Maltyp maltyp, long galdenarId, Fordran fordran, String handlaggare, LocalDate idag) {
        return switch (typ) {
            case BETALNINGSFORELAGGANDE -> new Betalningsforelaggande(maltyp, galdenarId, fordran, handlaggare, idag);
            case UTSOKNING -> new Utsokningsarende(maltyp, galdenarId, fordran, handlaggare, idag);
            case SKULDSANERING -> new Skuldsaneringsarende(maltyp, galdenarId, fordran, handlaggare, idag);
        };
    }
}

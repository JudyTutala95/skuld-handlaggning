package se.jtutala.handlaggning.arende.domain;

import java.time.LocalDate;

import se.jtutala.handlaggning.arende.Arendetyp;
import se.jtutala.handlaggning.regelverk.Maltyp;

/// Ansvar: skapa **rätt sorts ärende** utifrån ärendetypen.
public final class ArendeFabrik {

    private ArendeFabrik() {
    }

    public static Arende skapa(Arendetyp typ, Maltyp maltyp, long galdenarId, Fordran fordran, String handlaggare, LocalDate idag) {
        return switch (typ) {
            case BETALNINGSFORELAGGANDE -> new Betalningsforelaggande(maltyp, galdenarId, fordran, handlaggare, idag);
            case UTSOKNING -> new Utsokningsarende(maltyp, galdenarId, fordran, handlaggare, idag);
            case SKULDSANERING -> new Skuldsaneringsarende(maltyp, galdenarId, fordran, handlaggare, idag);
        };
    }
}

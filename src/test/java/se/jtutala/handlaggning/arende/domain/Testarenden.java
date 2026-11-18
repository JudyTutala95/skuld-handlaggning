package se.jtutala.handlaggning.arende.domain;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;

import se.jtutala.handlaggning.regelverk.Maltyp;

/// Skapar ärenden för enhetstester – en metod per ärendetyp.
final class Testarenden {

    static final LocalDate IDAG = LocalDate.of(2026, 3, 1);
    static final long ID = 10L;

    private Testarenden() {
    }

    static Betalningsforelaggande betalningsforelaggande() {
        return sparat(new Betalningsforelaggande(Maltyp.ENSKILT, 1, fordran(5000, LocalDate.of(2026, 1, 31), true), "lisa", IDAG));
    }

    static Utsokningsarende utsokning() {
        return sparat(new Utsokningsarende(Maltyp.ALLMANT, 2, fordran(10_000, LocalDate.of(2025, 5, 12), false), "omar", IDAG));
    }

    static Skuldsaneringsarende skuldsanering() {
        return sparat(new Skuldsaneringsarende(Maltyp.ENSKILT, 3, fordran(600_000, LocalDate.of(2019, 1, 31), false), "lisa", IDAG));
    }

    static Fordran fordran(int belopp, LocalDate forfallodag, boolean konsument) {
        return new Fordran("Borgenär AB", kr(belopp), forfallodag, konsument);
    }

    static BigDecimal kr(int belopp) {
        return BigDecimal.valueOf(belopp);
    }

    /// Låtsas att ärendet sparats: sätter det id som databasen annars skulle tilldela.
    static <T extends Arende> T sparat(T arende) {
        try {
            Field id = Arende.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(arende, ID);
            return arende;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}

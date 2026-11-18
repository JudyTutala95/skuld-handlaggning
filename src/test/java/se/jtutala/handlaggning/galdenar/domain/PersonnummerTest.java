package se.jtutala.handlaggning.galdenar.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PersonnummerTest {

    @Test
    void tolkarMedOchUtanBindestreck() {
        assertThat(Personnummer.parse("19850315-2384")).isEqualTo(Personnummer.parse("198503152384"));
        assertThat(Personnummer.parse(" 19850315-2384 ").formaterat()).isEqualTo("19850315-2384");
    }

    @Test
    void raknarUtFodelsedatum() {
        assertThat(Personnummer.parse("19850315-2384").fodelsedatum()).isEqualTo(LocalDate.of(1985, 3, 15));
    }

    @ParameterizedTest
    @ValueSource(strings = {"19850315-2385", "19850315-238", "abc", "19851315-2384", "19850230-2384"})
    void avvisarOgiltiga(String text) {
        assertThatIllegalArgumentException().isThrownBy(() -> Personnummer.parse(text));
    }

    @Test
    void samordningsnummerHarDagPlus60() {
        // 15 mars 1985 → dag 15 + 60 = 75
        var samordningsnummer = new Personnummer(medKontrollsiffra("198503752380".substring(0, 11)));
        assertThat(samordningsnummer.arSamordningsnummer()).isTrue();
        assertThat(samordningsnummer.fodelsedatum()).isEqualTo(LocalDate.of(1985, 3, 15));
    }

    @Test
    void maskerasIToStringSaAttDetInteLackerILoggar() {
        var pnr = Personnummer.parse("19850315-2384");
        assertThat(pnr.maskerat()).isEqualTo("19850315-****");
        assertThat(pnr).hasToString("19850315-****");
    }

    /// Hjälpmetod: lägger till rätt kontrollsiffra på 11 siffror.
    private static String medKontrollsiffra(String elvaSiffror) {
        for (int k = 0; k <= 9; k++) {
            if (Luhn.arGiltigt(elvaSiffror.substring(2) + k)) return elvaSiffror + k;
        }
        throw new IllegalStateException();
    }
}

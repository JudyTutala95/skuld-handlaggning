package se.jtutala.handlaggning.galdenar.regelverk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/// Rena enhetstester av regelverket. Varje test visar en uträkning man kan följa med papper och penna.
class ForbehallsbeloppTest {

    Normalbelopp nb = Normalbelopp.AR_2026;

    @Test
    @DisplayName("Ensamstående: 28 000 netto − (6 243 normalbelopp + 7 500 hyra) = 14 257 kr utmätningsbart")
    void ensamstaende() {
        var hushall = new Hushall(kr(28_000), kr(7_500), false, List.of());

        var resultat = Forbehallsbelopp.berakna(hushall, nb);

        assertThat(resultat.forbehallsbelopp()).isEqualByComparingTo("13743");
        assertThat(resultat.utmatningsbartBelopp()).isEqualByComparingTo("14257");
        assertThat(resultat.finnsUtmatningsbartBelopp()).isTrue();
        assertThat(resultat.normalbeloppsar()).isEqualTo(2026);
    }

    @Test
    @DisplayName("Sambo med två barn (8 och 14 år): hushållets kostnader delas lika")
    void sammanlevandeMedBarn() {
        // Hushållets normalbelopp: 10 314 + 4 004 (barn 8 år) + 4 672 (barn 14 år) = 18 990 → hälften 9 495
        // Boende: 11 000 → hälften 5 500
        // Förbehåll: 9 495 + 5 500 = 14 995 → utmätningsbart 34 000 − 14 995 = 19 005
        var hushall = new Hushall(kr(34_000), kr(11_000), true, List.of(8, 14));

        var resultat = Forbehallsbelopp.berakna(hushall, nb);

        assertThat(resultat.normalbelopp()).isEqualByComparingTo("9495");
        assertThat(resultat.boendekostnad()).isEqualByComparingTo("5500");
        assertThat(resultat.utmatningsbartBelopp()).isEqualByComparingTo("19005");
    }

    @Test
    void lagInkomstGerIngetUtmatningsbartBelopp() {
        var hushall = new Hushall(kr(12_000), kr(6_500), false, List.of());

        var resultat = Forbehallsbelopp.berakna(hushall, nb);

        assertThat(resultat.utmatningsbartBelopp()).isEqualByComparingTo("0");
        assertThat(resultat.finnsUtmatningsbartBelopp()).isFalse();
    }

    @Test
    void avrundasNedatTillHelaKronor() {
        var hushall = new Hushall(new BigDecimal("20000.99"), kr(5_000), false, List.of());

        assertThat(Forbehallsbelopp.berakna(hushall, nb).utmatningsbartBelopp()).isEqualByComparingTo("8757");
    }

    @ParameterizedTest(name = "barn {0} år → {1} kr")
    @CsvSource({"0, 3336", "6, 3336", "7, 4004", "10, 4004", "11, 4672", "14, 4672", "15, 5339", "19, 5339"})
    void normalbeloppForBarnPerAlder(int alder, int forvantat) {
        assertThat(nb.forBarn(alder)).isEqualByComparingTo(kr(forvantat));
    }

    @Test
    void negativaBeloppAvvisas() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Hushall(kr(-1), kr(0), false, List.of()));
    }

    private static BigDecimal kr(int belopp) {
        return BigDecimal.valueOf(belopp);
    }
}

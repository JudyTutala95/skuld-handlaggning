package se.jtutala.handlaggning.arende.regelverk;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class PreskriptionTest {

    LocalDate forfallodag = LocalDate.of(2025, 5, 12);

    @Test
    void allmantMalPreskriberasFemArEfterArsskiftet() {
        assertThat(Preskription.preskriptionsdag(Maltyp.ALLMANT, forfallodag, false, null))
                .isEqualTo(LocalDate.of(2030, 12, 31));
    }

    @Test
    void enskiltMalPreskriberasEfterTioAr() {
        assertThat(Preskription.preskriptionsdag(Maltyp.ENSKILT, forfallodag, false, null))
                .isEqualTo(LocalDate.of(2035, 5, 12));
    }

    @Test
    void konsumentfordranPreskriberasEfterTreAr() {
        assertThat(Preskription.preskriptionsdag(Maltyp.ENSKILT, forfallodag, true, null))
                .isEqualTo(LocalDate.of(2028, 5, 12));
    }

    @Test
    void preskriptionsavbrottStartarOmTiden() {
        LocalDate avbrott = LocalDate.of(2027, 1, 20);
        assertThat(Preskription.preskriptionsdag(Maltyp.ENSKILT, forfallodag, true, avbrott))
                .isEqualTo(LocalDate.of(2030, 1, 20));
    }

    @Test
    void avbrottForeForfallodagenRaknasInte() {
        assertThat(Preskription.preskriptionsdag(Maltyp.ENSKILT, forfallodag, false, LocalDate.of(2024, 1, 1)))
                .isEqualTo(LocalDate.of(2035, 5, 12));
    }

    @Test
    void arPreskriberadForstDagenEfter() {
        LocalDate dag = LocalDate.of(2030, 12, 31);
        assertThat(Preskription.arPreskriberad(dag, dag)).isFalse();
        assertThat(Preskription.arPreskriberad(dag, dag.plusDays(1))).isTrue();
    }
}

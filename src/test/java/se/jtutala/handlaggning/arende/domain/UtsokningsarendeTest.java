package se.jtutala.handlaggning.arende.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static se.jtutala.handlaggning.arende.domain.Testarenden.IDAG;
import static se.jtutala.handlaggning.arende.domain.Testarenden.kr;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import se.jtutala.handlaggning.arende.Arendestatus;
import se.jtutala.handlaggning.common.RegelbrottException;

/// Reglerna för utsökning – utan Spring och utan databas.
class UtsokningsarendeTest {

    Utsokningsarende uts = Testarenden.utsokning();

    @Test
    void loneutmatningKraverUtredningForst() {
        assertThatThrownBy(() -> uts.beslutaLoneutmatning(kr(3000), IDAG))
                .isInstanceOf(RegelbrottException.class)
                .hasMessageContaining("kan inte gå från 'Registrerat' till 'Löneutmätning pågår'");
    }

    @Test
    void loneutmatningSparasBelopp() {
        uts.paborjaUtredning(IDAG);

        uts.beslutaLoneutmatning(kr(4000), IDAG);

        assertThat(uts.getStatus()).isEqualTo(Arendestatus.LONEUTMATNING);
        assertThat(uts.getLoneutmatningPerManad()).isEqualByComparingTo("4000");
    }

    @Test
    void ingenLoneutmatningOmInkomstenArForLag() {
        uts.paborjaUtredning(IDAG);

        assertThatThrownBy(() -> uts.beslutaLoneutmatning(BigDecimal.ZERO, IDAG))
                .hasMessageContaining("understiger förbehållsbeloppet");
    }

    @Test
    void inbetalningarMinskarSkulden() {
        uts.registreraInbetalning(new Inbetalning(kr(4000), IDAG), IDAG);

        assertThat(uts.inbetaltTotalt()).isEqualByComparingTo("4000");
        assertThat(uts.kvarstaendeSkuld()).isEqualByComparingTo("6000");
        assertThat(uts.getStatus()).isEqualTo(Arendestatus.REGISTRERAT);
    }

    @Test
    void sistaInbetalningenAvslutarArendet() {
        uts.registreraInbetalning(new Inbetalning(kr(4000), IDAG), IDAG);
        uts.registreraInbetalning(new Inbetalning(kr(6000), IDAG), IDAG);

        assertThat(uts.getStatus()).isEqualTo(Arendestatus.BETALT);
        assertThat(uts.mojligaNastaStatusar()).isEmpty();
    }

    @Test
    void kanInteBetalaMerAnSkulden() {
        assertThatThrownBy(() -> uts.registreraInbetalning(new Inbetalning(kr(10_001), IDAG), IDAG))
                .hasMessageContaining("större än kvarstående skuld");
    }

    @Test
    void avslutatArendeTarInteEmotInbetalningar() {
        uts.avskriv("Återkallad", IDAG);

        assertThatThrownBy(() -> uts.registreraInbetalning(new Inbetalning(kr(100), IDAG), IDAG))
                .hasMessageContaining("avslutat");
    }

    @Test
    void preskriptionForAllmantMal() {
        LocalDate preskriptionsdag = LocalDate.of(2030, 12, 31);
        assertThat(uts.preskriptionsdag()).isEqualTo(preskriptionsdag);
        assertThatThrownBy(() -> uts.markeraPreskriberat(preskriptionsdag)).isInstanceOf(RegelbrottException.class);

        uts.markeraPreskriberat(preskriptionsdag.plusDays(1));

        assertThat(uts.getStatus()).isEqualTo(Arendestatus.PRESKRIBERAT);
    }

    @Test
    void inbetalningMedNollKronorAvvisas() {
        assertThatThrownBy(() -> new Inbetalning(BigDecimal.ZERO, IDAG)).isInstanceOf(RegelbrottException.class);
    }
}

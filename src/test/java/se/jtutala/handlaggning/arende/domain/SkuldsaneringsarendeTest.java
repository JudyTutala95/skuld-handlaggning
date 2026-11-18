package se.jtutala.handlaggning.arende.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static se.jtutala.handlaggning.arende.domain.Testarenden.IDAG;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import se.jtutala.handlaggning.arende.Arendestatus;
import se.jtutala.handlaggning.common.RegelbrottException;

/// Reglerna för skuldsanering – utan Spring och utan databas.
class SkuldsaneringsarendeTest {

    Skuldsaneringsarende sks = Testarenden.skuldsanering();

    @BeforeEach
    void utred() {
        sks.paborjaUtredning(IDAG);
    }

    @Test
    void beviljadSkuldsaneringPagarIFemAr() {
        sks.bevilja("Kvalificerat insolvent.", IDAG);

        assertThat(sks.getStatus()).isEqualTo(Arendestatus.SKULDSANERING_PAGAR);
        assertThat(sks.period()).isNotNull().extracting(Skuldsaneringsperiod::slutar).isEqualTo(LocalDate.of(2031, 3, 1));
    }

    @Test
    void kanSlutforasForstEfterFemAr() {
        sks.bevilja("Kvalificerat insolvent.", IDAG);

        assertThatThrownBy(() -> sks.slutfor(LocalDate.of(2031, 2, 28))).hasMessage("Skuldsaneringen pågår till 2031-03-01");

        sks.slutfor(LocalDate.of(2031, 3, 1));
        assertThat(sks.getStatus()).isEqualTo(Arendestatus.SKULDSANERING_SLUTFORD);
    }

    @Test
    void avslag() {
        sks.avsla("Inte kvalificerat insolvent.", IDAG);

        assertThat(sks.getStatus()).isEqualTo(Arendestatus.SKULDSANERING_AVSLAG);
        assertThat(sks.period()).isNull();
    }

    @Test
    void kanInteSlutforasUtanBeslut() {
        assertThatThrownBy(() -> sks.slutfor(IDAG)).isInstanceOf(RegelbrottException.class);
    }
}

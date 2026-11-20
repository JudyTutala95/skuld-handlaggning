package se.jtutala.handlaggning.notifiering;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;


class BrevmallTest {

    @Test
    void delgivningGerBrevMedFristen() {
        var h = new ArendeStatusAndrad(1, "BF-2026-000001", 7, "BETALNINGSFORELAGGANDE", "REGISTRERAT", "DELGIVET");

        assertThat(Brevmall.textFor(h)).hasValueSatisfying(text -> assertThat(text)
                .contains("BF-2026-000001").contains("10 dagar"));
    }

    @Test
    void allaStatusbytenGerInteBrev() {
        var h = new ArendeStatusAndrad(1, "UTS-2026-000001", 7, "UTSOKNING", "REGISTRERAT", "UTREDNING");

        assertThat(Brevmall.textFor(h)).isEmpty();
    }

    @Test
    void okandStatusGerInteBrevOchKraschar() {
        var h = new ArendeStatusAndrad(1, "UTS-2026-000001", 7, "UTSOKNING", "UTREDNING", "EN_NY_STATUS");

        assertThat(Brevmall.textFor(h)).isEmpty();
    }
}

package se.jtutala.handlaggning.notifiering;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import se.jtutala.handlaggning.arende.ArendeStatusAndrad;
import se.jtutala.handlaggning.arende.Arendestatus;
import se.jtutala.handlaggning.arende.Arendetyp;

class BrevmallTest {

    @Test
    void delgivningGerBrevMedFristen() {
        var h = new ArendeStatusAndrad(1, "BF-2026-000001", 7, Arendetyp.BETALNINGSFORELAGGANDE,
                Arendestatus.REGISTRERAT, Arendestatus.DELGIVET);

        assertThat(Brevmall.textFor(h)).hasValueSatisfying(text -> assertThat(text)
                .contains("BF-2026-000001").contains("10 dagar"));
    }

    @Test
    void allaStatusbytenGerInteBrev() {
        var h = new ArendeStatusAndrad(1, "UTS-2026-000001", 7, Arendetyp.UTSOKNING,
                Arendestatus.REGISTRERAT, Arendestatus.UTREDNING);

        assertThat(Brevmall.textFor(h)).isEmpty();
    }
}

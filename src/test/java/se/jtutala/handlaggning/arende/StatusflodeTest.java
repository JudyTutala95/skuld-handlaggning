package se.jtutala.handlaggning.arende;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StatusflodeTest {

    @Test
    void tillaterBaraDefinieradeOvergangar() {
        Statusflode flode = Betalningsforelaggande.FLODE;

        assertThat(flode.tillater(Arendestatus.REGISTRERAT, Arendestatus.DELGIVET)).isTrue();
        assertThat(flode.tillater(Arendestatus.REGISTRERAT, Arendestatus.UTSLAG_MEDDELAT)).isFalse();
    }

    @Test
    void avslutadStatusHarIngaNastaSteg() {
        assertThat(Utsokningsarende.FLODE.nastaFran(Arendestatus.BETALT)).isEmpty();
    }

    @Test
    void nastaStegArSorterade() {
        assertThat(Utsokningsarende.FLODE.nastaFran(Arendestatus.UTREDNING))
                .containsExactly(Arendestatus.AVSKRIVET, Arendestatus.LONEUTMATNING, Arendestatus.BETALT, Arendestatus.PRESKRIBERAT);
    }
}

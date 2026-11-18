package se.jtutala.handlaggning.arende.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static se.jtutala.handlaggning.arende.domain.Testarenden.IDAG;
import static se.jtutala.handlaggning.arende.domain.Testarenden.fordran;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import se.jtutala.handlaggning.arende.ArendeStatusAndrad;
import se.jtutala.handlaggning.arende.Arendestatus;
import se.jtutala.handlaggning.arende.Arendetyp;
import se.jtutala.handlaggning.common.RegelbrottException;
import se.jtutala.handlaggning.regelverk.Maltyp;

/// Reglerna för betalningsföreläggande – utan Spring och utan databas.
class BetalningsforelaggandeTest {

    static final LocalDate DELGIVEN = LocalDate.of(2026, 3, 2);
    static final LocalDate SISTA_DAG = LocalDate.of(2026, 3, 12);

    Betalningsforelaggande bf = Testarenden.betalningsforelaggande();

    @Test
    void nyttArendeArRegistreratMedDagbok() {
        assertThat(bf.getStatus()).isEqualTo(Arendestatus.REGISTRERAT);
        assertThat(bf.getDagbok()).singleElement().extracting(Dagboksanteckning::handelse)
                .asString().startsWith("Ärendet registrerat");
        assertThat(bf.mojligaNastaStatusar()).containsExactly(Arendestatus.AVSKRIVET, Arendestatus.DELGIVET);
    }

    @Test
    void delgivningGerTioDagarsFrist() {
        bf.delge(DELGIVEN, DELGIVEN);

        assertThat(bf.getStatus()).isEqualTo(Arendestatus.DELGIVET);
        assertThat(bf.bestridandefrist()).isEqualTo(new Bestridandefrist(DELGIVEN));
    }

    @Test
    void utslagNarFristenGattUtUtanBestridande() {
        bf.delge(DELGIVEN, DELGIVEN);

        bf.meddelaUtslag(SISTA_DAG.plusDays(1));

        assertThat(bf.getStatus()).isEqualTo(Arendestatus.UTSLAG_MEDDELAT);
    }

    @Test
    void utslagFarInteMeddelasInnanFristenGattUt() {
        bf.delge(DELGIVEN, DELGIVEN);

        assertThatThrownBy(() -> bf.meddelaUtslag(SISTA_DAG))
                .isInstanceOf(RegelbrottException.class)
                .hasMessageContaining("bestridandefristen (2026-03-12)");
    }

    @Test
    void bestridandeIntimeStopparUtslaget() {
        bf.delge(DELGIVEN, DELGIVEN);
        bf.bestrid(SISTA_DAG, "Varan levererades aldrig", SISTA_DAG);

        assertThat(bf.getStatus()).isEqualTo(Arendestatus.BESTRITT);
        assertThatThrownBy(() -> bf.meddelaUtslag(SISTA_DAG.plusDays(5))).isInstanceOf(RegelbrottException.class);
    }

    @Test
    void bestrittArendeKanOverlamnasTillTingsratten() {
        bf.delge(DELGIVEN, DELGIVEN);
        bf.bestrid(SISTA_DAG, "Fel belopp", SISTA_DAG);

        bf.overlamnaTillTingsratt(SISTA_DAG.plusDays(3));

        assertThat(bf.getStatus()).isEqualTo(Arendestatus.OVERLAMNAT_TILL_TINGSRATT);
    }

    @Test
    void bestridandeEfterFristenAvvisas() {
        bf.delge(DELGIVEN, DELGIVEN);

        assertThatThrownBy(() -> bf.bestrid(SISTA_DAG.plusDays(1), "för sent", SISTA_DAG.plusDays(1)))
                .hasMessage("Bestridandet kom in 2026-03-13, efter fristen 2026-03-12");
    }

    @Test
    void bestridandeInnanDelgivningAvvisas() {
        assertThatThrownBy(() -> bf.bestrid(IDAG, "tidigt", IDAG)).hasMessage("Föreläggandet är inte delgivet ännu");
    }

    @Test
    void delgivningKanInteLiggaIFramtiden() {
        assertThatThrownBy(() -> bf.delge(IDAG.plusDays(1), IDAG)).isInstanceOf(RegelbrottException.class);
    }

    @Test
    void utslagKanVerkstallasSomNyttUtsokningsarende() {
        bf.delge(DELGIVEN, DELGIVEN);
        bf.meddelaUtslag(SISTA_DAG.plusDays(1));

        Utsokningsarende utsokning = bf.verkstall("omar", SISTA_DAG.plusDays(8));

        assertThat(utsokning.getMaltyp()).isEqualTo(Maltyp.ENSKILT);
        assertThat(utsokning.getFordran()).isEqualTo(bf.getFordran());
        assertThat(utsokning.getUrsprungsarendeId()).isEqualTo(bf.getId());
    }

    @Test
    void verkstallighetKraverUtslag() {
        assertThatThrownBy(() -> bf.verkstall("omar", IDAG)).hasMessage("Verkställighet kräver ett meddelat utslag");
    }

    @Test
    void delgivningAvbryterPreskriptionen() {
        assertThat(bf.preskriptionsdag()).isEqualTo(LocalDate.of(2029, 1, 31)); // konsumentfordran: 3 år

        bf.delge(DELGIVEN, DELGIVEN);

        assertThat(bf.preskriptionsdag()).isEqualTo(DELGIVEN.plusYears(3));
    }

    @Test
    void statusbyteRegistrerarHandelse() {
        bf.delge(DELGIVEN, DELGIVEN);

        assertThat(bf.vantandeHandelser()).containsExactly(new ArendeStatusAndrad(10L, "BF-2026-000010", 1L,
                Arendetyp.BETALNINGSFORELAGGANDE, Arendestatus.REGISTRERAT, Arendestatus.DELGIVET));
    }

    @Test
    void allmantMalKanInteVaraBetalningsforelaggande() {
        assertThatThrownBy(() -> new Betalningsforelaggande(Maltyp.ALLMANT, 1, fordran(100, IDAG, false), "lisa", IDAG))
                .hasMessageContaining("Allmänna mål behöver inget betalningsföreläggande");
    }

    @Test
    void skuldenMasteHaForfallit() {
        assertThatThrownBy(() -> new Betalningsforelaggande(Maltyp.ENSKILT, 1, fordran(100, IDAG.plusDays(1), false), "lisa", IDAG))
                .hasMessageContaining("inte förfallit");
    }
}

package se.jtutala.handlaggning.arende;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import se.jtutala.handlaggning.common.RegelbrottException;
import se.jtutala.handlaggning.regelverk.Maltyp;

/// Enhetstester av affärsreglerna i `Arende` – utan Spring och utan databas.
///
/// Testerna läses som en beskrivning av processen: "ett betalningsföreläggande som
/// inte bestrids inom 10 dagar leder till utslag" osv.
class ArendeTest {

    static final LocalDate IDAG = LocalDate.of(2026, 3, 1);

    @Nested
    @DisplayName("Betalningsföreläggande")
    class Betalningsforelaggande {

        Arende bf = sparat(Arende.registrera(Arendetyp.BETALNINGSFORELAGGANDE, Maltyp.ENSKILT, 1, "Inkasso AB",
                kr(5000), LocalDate.of(2026, 1, 31), true, "lisa", IDAG));

        @Test
        void nyttArendeArRegistreratOchHarDagbok() {
            assertThat(bf.getStatus()).isEqualTo(Arendestatus.REGISTRERAT);
            assertThat(bf.getDagbok()).singleElement().extracting(Dagboksanteckning::handelse)
                    .asString().startsWith("Ärendet registrerat");
            assertThat(bf.mojligaNastaStatusar()).containsExactlyInAnyOrder(Arendestatus.DELGIVET, Arendestatus.AVSKRIVET);
        }

        @Test
        void delgivningGerTioDagarsBestridandefrist() {
            bf.delge(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 2));

            assertThat(bf.getStatus()).isEqualTo(Arendestatus.DELGIVET);
            assertThat(bf.bestridandefrist()).isEqualTo(LocalDate.of(2026, 3, 12));
        }

        @Test
        void inget_bestridande_ger_utslag_efter_fristen() {
            bf.delge(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 2));

            bf.meddelaUtslag(LocalDate.of(2026, 3, 13));

            assertThat(bf.getStatus()).isEqualTo(Arendestatus.UTSLAG_MEDDELAT);
            assertThat(bf.getStatus().avslutad()).isTrue();
        }

        @Test
        void utslagFarInteMeddelasInnanFristenGattUt() {
            bf.delge(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 2));

            assertThatThrownBy(() -> bf.meddelaUtslag(LocalDate.of(2026, 3, 12)))
                    .isInstanceOf(RegelbrottException.class)
                    .hasMessageContaining("bestridandefristen (2026-03-12)");
        }

        @Test
        void bestridandeInomFristenStopparUtslaget() {
            bf.delge(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 2));
            bf.bestrid(LocalDate.of(2026, 3, 12), "Varan levererades aldrig", LocalDate.of(2026, 3, 12));

            assertThat(bf.getStatus()).isEqualTo(Arendestatus.BESTRITT);
            assertThatThrownBy(() -> bf.meddelaUtslag(LocalDate.of(2026, 4, 1))).isInstanceOf(RegelbrottException.class);

            bf.overlamnaTillTingsratt(LocalDate.of(2026, 3, 20));
            assertThat(bf.getStatus()).isEqualTo(Arendestatus.OVERLAMNAT_TILL_TINGSRATT);
        }

        @Test
        void bestridandeEfterFristenAvvisas() {
            bf.delge(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 2));

            assertThatThrownBy(() -> bf.bestrid(LocalDate.of(2026, 3, 13), "för sent", LocalDate.of(2026, 3, 13)))
                    .isInstanceOf(RegelbrottException.class)
                    .hasMessage("Bestridandet kom in 2026-03-13, efter fristen 2026-03-12");
        }

        @Test
        void utslagKanVerkstallasSomNyttUtsokningsarende() {
            bf.delge(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 2));
            bf.meddelaUtslag(LocalDate.of(2026, 3, 13));

            Arende utsokning = bf.begarVerkstallighet("omar", LocalDate.of(2026, 3, 20));

            assertThat(utsokning.getTyp()).isEqualTo(Arendetyp.UTSOKNING);
            assertThat(utsokning.getMaltyp()).isEqualTo(Maltyp.ENSKILT);
            assertThat(utsokning.getKapitalbelopp()).isEqualByComparingTo("5000");
            assertThat(utsokning.getUrsprungsarendeId()).isEqualTo(bf.getId());
        }

        @Test
        void verkstallighetKraverUtslag() {
            assertThatThrownBy(() -> bf.begarVerkstallighet("omar", IDAG)).isInstanceOf(RegelbrottException.class);
        }

        @Test
        void statusbyteRegistrerarHandelse() {
            bf.delge(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 2));

            assertThat(bf.vantandeHandelser()).containsExactly(new ArendeStatusAndrad(10L, "BF-2026-000010", 1L,
                    Arendetyp.BETALNINGSFORELAGGANDE, Arendestatus.REGISTRERAT, Arendestatus.DELGIVET));
        }
    }

    @Nested
    @DisplayName("Utsökning")
    class Utsokning {

        Arende uts = sparat(Arende.registrera(Arendetyp.UTSOKNING, Maltyp.ALLMANT, 2, "Skatteverket",
                kr(10_000), LocalDate.of(2025, 5, 12), false, "omar", IDAG));

        @Test
        void loneutmatningKraverUtredningForst() {
            assertThatThrownBy(() -> uts.beslutaLoneutmatning(kr(3000), IDAG))
                    .isInstanceOf(RegelbrottException.class)
                    .hasMessageContaining("kan inte gå från 'Registrerat' till 'Löneutmätning pågår'");
        }

        @Test
        void loneutmatningOchInbetalningarTillsSkuldenArBetald() {
            uts.paborjaUtredning(IDAG);
            uts.beslutaLoneutmatning(kr(4000), IDAG);
            uts.registreraInbetalning(kr(4000), LocalDate.of(2026, 3, 25), IDAG);
            uts.registreraInbetalning(kr(4000), LocalDate.of(2026, 4, 25), IDAG);

            assertThat(uts.kvarstaendeSkuld()).isEqualByComparingTo("2000");
            assertThat(uts.getStatus()).isEqualTo(Arendestatus.LONEUTMATNING);

            uts.registreraInbetalning(kr(2000), LocalDate.of(2026, 5, 25), IDAG);

            assertThat(uts.getStatus()).isEqualTo(Arendestatus.BETALT);
            assertThat(uts.kvarstaendeSkuld()).isEqualByComparingTo("0");
        }

        @Test
        void kanInteBetalaMerAnSkulden() {
            assertThatThrownBy(() -> uts.registreraInbetalning(kr(10_001), IDAG, IDAG))
                    .isInstanceOf(RegelbrottException.class);
        }

        @Test
        void ingenLoneutmatningOmInkomstenArForLag() {
            uts.paborjaUtredning(IDAG);
            assertThatThrownBy(() -> uts.beslutaLoneutmatning(BigDecimal.ZERO, IDAG))
                    .hasMessageContaining("understiger förbehållsbeloppet");
        }

        @Test
        void preskriptionForAllmantMal() {
            assertThat(uts.preskriptionsdag()).isEqualTo(LocalDate.of(2030, 12, 31));
            assertThatThrownBy(() -> uts.markeraPreskriberat(LocalDate.of(2030, 12, 31))).isInstanceOf(RegelbrottException.class);

            uts.markeraPreskriberat(LocalDate.of(2031, 1, 1));
            assertThat(uts.getStatus()).isEqualTo(Arendestatus.PRESKRIBERAT);
        }

        @Test
        void avslutatArendeKanInteAndras() {
            uts.avskriv("Återkallad", IDAG);

            assertThatThrownBy(() -> uts.registreraInbetalning(kr(100), IDAG, IDAG))
                    .hasMessageContaining("avslutat");
            assertThatThrownBy(() -> uts.paborjaUtredning(IDAG)).isInstanceOf(RegelbrottException.class);
        }

        @Test
        void betalningsforelaggandeAtgarderGallerInteHar() {
            assertThatThrownBy(() -> uts.delge(IDAG, IDAG))
                    .hasMessage("Åtgärden gäller bara ärenden av typen BETALNINGSFORELAGGANDE");
        }
    }

    @Nested
    @DisplayName("Skuldsanering")
    class Skuldsanering {

        Arende sks = sparat(Arende.registrera(Arendetyp.SKULDSANERING, Maltyp.ENSKILT, 3, "Flera borgenärer",
                kr(600_000), LocalDate.of(2019, 1, 31), false, "lisa", IDAG));

        @Test
        void beviljadSkuldsaneringPagarIFemAr() {
            sks.paborjaUtredning(IDAG);
            sks.beviljaSkuldsanering("Kvalificerat insolvent.", IDAG);

            assertThat(sks.skuldsaneringSlutar()).isEqualTo(LocalDate.of(2031, 3, 1));
            assertThatThrownBy(() -> sks.slutforSkuldsanering(LocalDate.of(2031, 2, 28))).isInstanceOf(RegelbrottException.class);

            sks.slutforSkuldsanering(LocalDate.of(2031, 3, 1));
            assertThat(sks.getStatus()).isEqualTo(Arendestatus.SKULDSANERING_SLUTFORD);
        }

        @Test
        void avslag() {
            sks.paborjaUtredning(IDAG);
            sks.avslaSkuldsanering("Inte kvalificerat insolvent.", IDAG);

            assertThat(sks.getStatus()).isEqualTo(Arendestatus.SKULDSANERING_AVSLAG);
        }
    }

    @Nested
    @DisplayName("Registrering")
    class Registrering {

        @Test
        void skuldenMasteHaForfallit() {
            assertThatThrownBy(() -> Arende.registrera(Arendetyp.UTSOKNING, Maltyp.ENSKILT, 1, "X",
                    kr(100), IDAG.plusDays(1), false, "lisa", IDAG))
                    .hasMessageContaining("inte förfallit");
        }

        @Test
        void allmantMalKanInteVaraBetalningsforelaggande() {
            assertThatThrownBy(() -> Arende.registrera(Arendetyp.BETALNINGSFORELAGGANDE, Maltyp.ALLMANT, 1, "Skatteverket",
                    kr(100), IDAG, false, "lisa", IDAG))
                    .isInstanceOf(RegelbrottException.class);
        }

        @Test
        void beloppMasteVaraPositivt() {
            assertThatThrownBy(() -> Arende.registrera(Arendetyp.UTSOKNING, Maltyp.ENSKILT, 1, "X",
                    BigDecimal.ZERO, IDAG, false, "lisa", IDAG))
                    .isInstanceOf(RegelbrottException.class);
        }
    }

    // --- Hjälpmetoder ---

    private static BigDecimal kr(int belopp) {
        return BigDecimal.valueOf(belopp);
    }

    /// Låtsas att ärendet sparats: sätter id som databasen annars skulle tilldela.
    private static Arende sparat(Arende arende) {
        try {
            Field id = Arende.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(arende, 10L);
            return arende;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}

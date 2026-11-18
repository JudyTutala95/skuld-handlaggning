package se.jtutala.handlaggning.arende;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import se.jtutala.handlaggning.common.HittadesInteException;
import se.jtutala.handlaggning.common.RegelbrottException;

/// Webbslice-test: HTTP, JSON, validering och felsvar. Användningsfallen är mockade.
@WebMvcTest({ArendeController.class, BetalningsforelaggandeController.class})
class ArendeControllerTest {

    @Autowired
    MockMvcTester mvc;

    @MockitoBean
    ArendeRegistrering registrering;
    @MockitoBean
    ArendeUppslag uppslag;
    @MockitoBean
    GemensammaAtgarder atgarder;
    @MockitoBean
    BetalningsforelaggandeHandlaggning betalningsforelaggande;

    @Test
    void registreringMedSaknadeFaltGer400MedFaltlista() {
        var svar = mvc.post().uri("/api/arenden")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "typ": "UTSOKNING", "kapitalbelopp": -5 }
                        """)
                .exchange();

        assertThat(svar).hasStatus(HttpStatus.BAD_REQUEST).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(svar).bodyJson().extractingPath("$.fel").asMap()
                .containsKeys("maltyp", "galdenarId", "borgenar", "kapitalbelopp", "forfallodag", "handlaggare");
        then(registrering).shouldHaveNoInteractions();
    }

    @Test
    void okandArendetypGer400() {
        assertThat(mvc.post().uri("/api/arenden").contentType(MediaType.APPLICATION_JSON)
                .content("{\"typ\": \"FINNS_INTE\"}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void regelbrottGer409MedBegripligtMeddelande() {
        given(betalningsforelaggande.bestrid(1L, LocalDate.of(2026, 3, 13), "för sent"))
                .willThrow(new RegelbrottException("Bestridandet kom in 2026-03-13, efter fristen 2026-03-12"));

        var svar = mvc.post().uri("/api/arenden/1/bestridande")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "datum": "2026-03-13", "grund": "för sent" }
                        """)
                .exchange();

        assertThat(svar).hasStatus(HttpStatus.CONFLICT);
        assertThat(svar).bodyJson().extractingPath("$.title").isEqualTo("Åtgärden är inte tillåten");
        assertThat(svar).bodyJson().extractingPath("$.detail").isEqualTo("Bestridandet kom in 2026-03-13, efter fristen 2026-03-12");
    }

    @Test
    void okantArendeGer404() {
        given(uppslag.hamta(99L)).willThrow(new HittadesInteException("Ärende", 99L));

        assertThat(mvc.get().uri("/api/arenden/99"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.type").isEqualTo("urn:problem:hittades-inte");
    }

    @Test
    void arbetslistaUtanFilter() {
        given(uppslag.arbetslista(null, null)).willReturn(List.of());

        assertThat(mvc.get().uri("/api/arenden")).hasStatusOk().bodyJson().isLenientlyEqualTo("[]");
    }

    @Test
    void arbetslistaMedStatus() {
        given(uppslag.arbetslista(null, Arendestatus.DELGIVET)).willReturn(List.of());

        assertThat(mvc.get().uri("/api/arenden?status=DELGIVET")).hasStatusOk();
        then(uppslag).should().arbetslista(null, Arendestatus.DELGIVET);
    }
}

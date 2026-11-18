package se.jtutala.handlaggning.arende;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import se.jtutala.handlaggning.arende.ArendeDtos.ArendeRad;
import se.jtutala.handlaggning.arende.ArendeDtos.ArendeSvar;
import se.jtutala.handlaggning.arende.ArendeDtos.Avskrivning;
import se.jtutala.handlaggning.arende.ArendeDtos.Bestridande;
import se.jtutala.handlaggning.arende.ArendeDtos.Delgivning;
import se.jtutala.handlaggning.arende.ArendeDtos.RegistreraArende;
import se.jtutala.handlaggning.arende.ArendeDtos.RegistreraInbetalning;
import se.jtutala.handlaggning.arende.ArendeDtos.Skuldsaneringsbeslut;
import se.jtutala.handlaggning.arende.ArendeDtos.Verkstallighet;

/// REST-API för ärenden.
///
/// **Åtgärder som egna resurser:** i stället för att låta klienten skriva `"status": "BESTRITT"`
/// (och själv hålla reda på reglerna) finns en endpoint per åtgärd. Servern bestämmer
/// vilken status det blir. Det kallas ibland ett *task-based API*.
@RestController
@RequestMapping("/api/arenden")
class ArendeController {

    private final ArendeService service;

    ArendeController(ArendeService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<ArendeSvar> registrera(@Valid @RequestBody RegistreraArende kommando) {
        ArendeSvar svar = service.registrera(kommando);
        return ResponseEntity.created(URI.create("/api/arenden/" + svar.id())).body(svar);
    }

    /// Arbetslista: `GET /api/arenden?status=DELGIVET` eller `?galdenarId=1`.
    @GetMapping
    List<ArendeRad> sok(@RequestParam(required = false) @Nullable Long galdenarId,
                        @RequestParam(required = false) @Nullable Arendestatus status) {
        return service.sok(galdenarId, status);
    }

    @GetMapping("/{id}")
    ArendeSvar hamta(@PathVariable long id) {
        return service.hamta(id);
    }

    // --- Betalningsföreläggande ---

    @PostMapping("/{id}/delgivning")
    ArendeSvar delge(@PathVariable long id, @Valid @RequestBody Delgivning k) {
        return service.delge(id, k.datum());
    }

    @PostMapping("/{id}/bestridande")
    ArendeSvar bestrid(@PathVariable long id, @Valid @RequestBody Bestridande k) {
        return service.bestrid(id, k.datum(), k.grund());
    }

    @PostMapping("/{id}/utslag")
    ArendeSvar utslag(@PathVariable long id) {
        return service.meddelaUtslag(id);
    }

    @PostMapping("/{id}/overlamning-tingsratt")
    ArendeSvar overlamna(@PathVariable long id) {
        return service.overlamnaTillTingsratt(id);
    }

    /// Skapar ett NYTT utsökningsärende från ett utslag → 201 Created.
    @PostMapping("/{id}/verkstallighet")
    @ResponseStatus(HttpStatus.CREATED)
    ArendeSvar verkstallighet(@PathVariable long id, @Valid @RequestBody Verkstallighet k) {
        return service.begarVerkstallighet(id, k.handlaggare());
    }

    // --- Utsökning ---

    @PostMapping("/{id}/utredning")
    ArendeSvar utredning(@PathVariable long id) {
        return service.paborjaUtredning(id);
    }

    @PostMapping("/{id}/loneutmatning")
    ArendeSvar loneutmatning(@PathVariable long id) {
        return service.beslutaLoneutmatning(id);
    }

    @PostMapping("/{id}/inbetalningar")
    ArendeSvar inbetalning(@PathVariable long id, @Valid @RequestBody RegistreraInbetalning k) {
        return service.registreraInbetalning(id, k.belopp(), k.datum());
    }

    @PostMapping("/{id}/preskription")
    ArendeSvar preskription(@PathVariable long id) {
        return service.markeraPreskriberat(id);
    }

    // --- Skuldsanering ---

    @PostMapping("/{id}/skuldsanering/beslut")
    ArendeSvar skuldsaneringsbeslut(@PathVariable long id, @Valid @RequestBody Skuldsaneringsbeslut k) {
        return service.beslutaSkuldsanering(id, k.beviljas(), k.motivering());
    }

    @PostMapping("/{id}/skuldsanering/slutfor")
    ArendeSvar slutfor(@PathVariable long id) {
        return service.slutforSkuldsanering(id);
    }

    // --- Gemensamt ---

    @PostMapping("/{id}/avskrivning")
    ArendeSvar avskriv(@PathVariable long id, @Valid @RequestBody Avskrivning k) {
        return service.avskriv(id, k.orsak());
    }
}

package se.jtutala.handlaggning.arende.web;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import se.jtutala.handlaggning.arende.Arendestatus;
import se.jtutala.handlaggning.arende.dto.ArendeRadResponse;
import se.jtutala.handlaggning.arende.dto.ArendeResponse;
import se.jtutala.handlaggning.arende.dto.AvskrivningRequest;
import se.jtutala.handlaggning.arende.dto.RegistreraArendeRequest;
import se.jtutala.handlaggning.arende.service.ArendeAtgardService;
import se.jtutala.handlaggning.arende.service.ArendeRegistreringService;
import se.jtutala.handlaggning.arende.service.ArendeSokService;

/// HTTP för det som gäller **alla ärendetyper**: registrera, läsa, utreda och avskriva.
@RestController
@RequestMapping("/api/arenden")
class ArendeController {

    private final ArendeRegistreringService registreringService;
    private final ArendeSokService sokService;
    private final ArendeAtgardService atgardService;

    ArendeController(ArendeRegistreringService registreringService, ArendeSokService sokService,
                     ArendeAtgardService atgardService) {
        this.registreringService = registreringService;
        this.sokService = sokService;
        this.atgardService = atgardService;
    }

    @PostMapping
    ResponseEntity<ArendeResponse> registrera(@Valid @RequestBody RegistreraArendeRequest request) {
        ArendeResponse response = registreringService.registrera(request);
        return ResponseEntity.created(URI.create("/api/arenden/" + response.id())).body(response);
    }

    /// Arbetslista: `GET /api/arenden?status=DELGIVET` eller `?galdenarId=1`.
    @GetMapping
    List<ArendeRadResponse> arbetslista(@RequestParam(required = false) @Nullable Long galdenarId,
                                        @RequestParam(required = false) @Nullable Arendestatus status) {
        return sokService.arbetslista(galdenarId, status);
    }

    @GetMapping("/{id}")
    ArendeResponse hamta(@PathVariable long id) {
        return sokService.hamta(id);
    }

    @PostMapping("/{id}/utredning")
    ArendeResponse utredning(@PathVariable long id) {
        return atgardService.paborjaUtredning(id);
    }

    @PostMapping("/{id}/avskrivning")
    ArendeResponse avskriv(@PathVariable long id, @Valid @RequestBody AvskrivningRequest request) {
        return atgardService.avskriv(id, request);
    }
}

package se.jtutala.handlaggning.arende.web;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import se.jtutala.handlaggning.arende.dto.ArendeResponse;
import se.jtutala.handlaggning.arende.dto.InbetalningRequest;
import se.jtutala.handlaggning.arende.service.UtsokningService;

/// HTTP för **utsökning**.
@RestController
@RequestMapping("/api/arenden/{id}")
class UtsokningController {

    private final UtsokningService service;

    UtsokningController(UtsokningService service) {
        this.service = service;
    }

    @PostMapping("/loneutmatning")
    ArendeResponse loneutmatning(@PathVariable long id) {
        return service.beslutaLoneutmatning(id);
    }

    @PostMapping("/inbetalningar")
    ArendeResponse inbetalning(@PathVariable long id, @Valid @RequestBody InbetalningRequest request) {
        return service.registreraInbetalning(id, request);
    }

    @PostMapping("/preskription")
    ArendeResponse preskription(@PathVariable long id) {
        return service.markeraPreskriberat(id);
    }
}

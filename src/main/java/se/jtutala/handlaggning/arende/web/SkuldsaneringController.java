package se.jtutala.handlaggning.arende.web;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import se.jtutala.handlaggning.arende.dto.ArendeResponse;
import se.jtutala.handlaggning.arende.dto.SkuldsaneringsbeslutRequest;
import se.jtutala.handlaggning.arende.service.SkuldsaneringService;

/// HTTP för **skuldsanering**.
@RestController
@RequestMapping("/api/arenden/{id}/skuldsanering")
class SkuldsaneringController {

    private final SkuldsaneringService service;

    SkuldsaneringController(SkuldsaneringService service) {
        this.service = service;
    }

    @PostMapping("/beslut")
    ArendeResponse beslut(@PathVariable long id, @Valid @RequestBody SkuldsaneringsbeslutRequest request) {
        return service.besluta(id, request);
    }

    @PostMapping("/slutfor")
    ArendeResponse slutfor(@PathVariable long id) {
        return service.slutfor(id);
    }
}

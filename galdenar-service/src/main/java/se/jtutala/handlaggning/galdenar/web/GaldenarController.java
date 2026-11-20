package se.jtutala.handlaggning.galdenar.web;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import se.jtutala.handlaggning.galdenar.dto.GaldenarResponse;
import se.jtutala.handlaggning.galdenar.dto.RegistreraGaldenarRequest;
import se.jtutala.handlaggning.galdenar.dto.SokGaldenarRequest;
import se.jtutala.handlaggning.galdenar.service.GaldenarRegistreringService;
import se.jtutala.handlaggning.galdenar.service.GaldenarSokService;

/// HTTP för att **registrera och hitta** gäldenärer.
@RestController
@RequestMapping("/api/galdenarer")
class GaldenarController {

    private final GaldenarRegistreringService registreringService;
    private final GaldenarSokService sokService;

    GaldenarController(GaldenarRegistreringService registreringService, GaldenarSokService sokService) {
        this.registreringService = registreringService;
        this.sokService = sokService;
    }

    @PostMapping
    ResponseEntity<GaldenarResponse> registrera(@Valid @RequestBody RegistreraGaldenarRequest request) {
        GaldenarResponse response = registreringService.registrera(request);
        return ResponseEntity.created(URI.create("/api/galdenarer/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    GaldenarResponse hamta(@PathVariable long id) {
        return sokService.hamta(id);
    }

    @PostMapping("/sok")
    GaldenarResponse sok(@Valid @RequestBody SokGaldenarRequest request) {
        return sokService.sok(request);
    }
}

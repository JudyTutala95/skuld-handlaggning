package se.jtutala.handlaggning.galdenar;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// HTTP för att **registrera och hitta** gäldenärer.
@RestController
@RequestMapping("/api/galdenarer")
class GaldenarController {

    private final GaldenarRegistrering registrering;
    private final GaldenarUppslag uppslag;

    GaldenarController(GaldenarRegistrering registrering, GaldenarUppslag uppslag) {
        this.registrering = registrering;
        this.uppslag = uppslag;
    }

    @PostMapping
    ResponseEntity<GaldenarSvar> registrera(@Valid @RequestBody RegistreraGaldenar kommando) {
        GaldenarSvar svar = registrering.registrera(kommando);
        return ResponseEntity.created(URI.create("/api/galdenarer/" + svar.id())).body(svar);
    }

    @GetMapping("/{id}")
    GaldenarSvar hamta(@PathVariable long id) {
        return uppslag.hamta(id);
    }

    @PostMapping("/sok")
    GaldenarSvar sok(@Valid @RequestBody SokGaldenar fraga) {
        return uppslag.sokPaPersonnummer(fraga.personnummer());
    }
}

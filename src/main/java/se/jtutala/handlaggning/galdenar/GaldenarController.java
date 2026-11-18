package se.jtutala.handlaggning.galdenar;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import se.jtutala.handlaggning.galdenar.GaldenarDtos.GaldenarSvar;
import se.jtutala.handlaggning.galdenar.GaldenarDtos.RegistreraGaldenar;
import se.jtutala.handlaggning.galdenar.GaldenarDtos.SokGaldenar;
import se.jtutala.handlaggning.galdenar.GaldenarDtos.UppdateraEkonomi;
import se.jtutala.handlaggning.regelverk.Forbehallsberakning;

/// REST-API för gäldenärer. Tunn: tar emot HTTP, validerar, delegerar till servicen.
@RestController
@RequestMapping("/api/galdenarer")
class GaldenarController {

    private final GaldenarService service;

    GaldenarController(GaldenarService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<GaldenarSvar> registrera(@Valid @RequestBody RegistreraGaldenar kommando) {
        GaldenarSvar svar = service.registrera(kommando);
        return ResponseEntity.created(URI.create("/api/galdenarer/" + svar.id())).body(svar);
    }

    @GetMapping("/{id}")
    GaldenarSvar hamta(@PathVariable long id) {
        return service.hamta(id);
    }

    @PostMapping("/sok")
    GaldenarSvar sok(@Valid @RequestBody SokGaldenar sok) {
        return service.sok(sok.personnummer());
    }

    @PutMapping("/{id}/ekonomi")
    GaldenarSvar uppdateraEkonomi(@PathVariable long id, @Valid @RequestBody UppdateraEkonomi kommando) {
        return service.uppdateraEkonomi(id, kommando);
    }

    /// Visar hur mycket av lönen som kan utmätas – och hur det räknats fram.
    @GetMapping("/{id}/forbehallsbelopp")
    Forbehallsberakning forbehallsbelopp(@PathVariable long id) {
        return service.beraknaForbehallsbelopp(id);
    }
}

package se.jtutala.handlaggning.galdenar;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import se.jtutala.handlaggning.regelverk.Forbehallsberakning;

/// HTTP för **gäldenärens ekonomi**.
@RestController
@RequestMapping("/api/galdenarer/{id}")
class GaldenarEkonomiController {

    private final GaldenarEkonomi ekonomi;

    GaldenarEkonomiController(GaldenarEkonomi ekonomi) {
        this.ekonomi = ekonomi;
    }

    @PutMapping("/ekonomi")
    GaldenarSvar uppdatera(@PathVariable long id, @Valid @RequestBody UppdateraEkonomi kommando) {
        return ekonomi.uppdatera(id, kommando);
    }

    /// Visar hur mycket av lönen som kan utmätas – och hur det räknats fram.
    @GetMapping("/forbehallsbelopp")
    Forbehallsberakning forbehallsbelopp(@PathVariable long id) {
        return ekonomi.forbehallsberakning(id);
    }
}

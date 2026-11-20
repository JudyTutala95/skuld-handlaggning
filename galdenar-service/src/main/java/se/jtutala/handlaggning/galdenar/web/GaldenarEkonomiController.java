package se.jtutala.handlaggning.galdenar.web;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import se.jtutala.handlaggning.galdenar.dto.GaldenarResponse;
import se.jtutala.handlaggning.galdenar.dto.UppdateraEkonomiRequest;
import se.jtutala.handlaggning.galdenar.service.GaldenarEkonomiService;
import se.jtutala.handlaggning.galdenar.regelverk.Forbehallsberakning;

/// HTTP för **gäldenärens ekonomi**.
@RestController
@RequestMapping("/api/galdenarer/{id}")
class GaldenarEkonomiController {

    private final GaldenarEkonomiService ekonomiService;

    GaldenarEkonomiController(GaldenarEkonomiService ekonomiService) {
        this.ekonomiService = ekonomiService;
    }

    @PutMapping("/ekonomi")
    GaldenarResponse uppdatera(@PathVariable long id, @Valid @RequestBody UppdateraEkonomiRequest request) {
        return ekonomiService.uppdatera(id, request);
    }

    /// Visar hur mycket av lönen som kan utmätas – och hur det räknats fram.
    @GetMapping("/forbehallsbelopp")
    Forbehallsberakning forbehallsbelopp(@PathVariable long id) {
        return ekonomiService.forbehallsberakning(id);
    }
}

package se.jtutala.handlaggning.arende;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// HTTP för **utsökning**.
@RestController
@RequestMapping("/api/arenden/{id}")
class UtsokningController {

    private final UtsokningHandlaggning handlaggning;

    UtsokningController(UtsokningHandlaggning handlaggning) {
        this.handlaggning = handlaggning;
    }

    @PostMapping("/loneutmatning")
    ArendeSvar loneutmatning(@PathVariable long id) {
        return handlaggning.beslutaLoneutmatning(id);
    }

    @PostMapping("/inbetalningar")
    ArendeSvar inbetalning(@PathVariable long id, @Valid @RequestBody RegistreraInbetalning kommando) {
        return handlaggning.registreraInbetalning(id, kommando.inbetalning());
    }

    @PostMapping("/preskription")
    ArendeSvar preskription(@PathVariable long id) {
        return handlaggning.markeraPreskriberat(id);
    }
}

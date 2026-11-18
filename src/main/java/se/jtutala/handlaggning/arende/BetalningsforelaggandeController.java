package se.jtutala.handlaggning.arende;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/// HTTP för **betalningsförelägganden**. En endpoint per åtgärd – servern avgör vilken
/// status det blir (task-based API).
@RestController
@RequestMapping("/api/arenden/{id}")
class BetalningsforelaggandeController {

    private final BetalningsforelaggandeHandlaggning handlaggning;

    BetalningsforelaggandeController(BetalningsforelaggandeHandlaggning handlaggning) {
        this.handlaggning = handlaggning;
    }

    @PostMapping("/delgivning")
    ArendeSvar delge(@PathVariable long id, @Valid @RequestBody Delgivning kommando) {
        return handlaggning.delge(id, kommando.datum());
    }

    @PostMapping("/bestridande")
    ArendeSvar bestrid(@PathVariable long id, @Valid @RequestBody Bestridande kommando) {
        return handlaggning.bestrid(id, kommando.datum(), kommando.grund());
    }

    @PostMapping("/utslag")
    ArendeSvar utslag(@PathVariable long id) {
        return handlaggning.meddelaUtslag(id);
    }

    @PostMapping("/overlamning-tingsratt")
    ArendeSvar overlamna(@PathVariable long id) {
        return handlaggning.overlamnaTillTingsratt(id);
    }

    /// Skapar ett NYTT utsökningsärende → 201 Created.
    @PostMapping("/verkstallighet")
    @ResponseStatus(HttpStatus.CREATED)
    ArendeSvar verkstallighet(@PathVariable long id, @Valid @RequestBody Verkstallighet kommando) {
        return handlaggning.verkstall(id, kommando.handlaggare());
    }
}

package se.jtutala.handlaggning.arende.web;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import se.jtutala.handlaggning.arende.dto.ArendeResponse;
import se.jtutala.handlaggning.arende.dto.BestridandeRequest;
import se.jtutala.handlaggning.arende.dto.DelgivningRequest;
import se.jtutala.handlaggning.arende.dto.VerkstallighetRequest;
import se.jtutala.handlaggning.arende.service.BetalningsforelaggandeService;

/// HTTP för **betalningsförelägganden**. En endpoint per åtgärd – servern avgör vilken
/// status det blir (task-based API).
@RestController
@RequestMapping("/api/arenden/{id}")
class BetalningsforelaggandeController {

    private final BetalningsforelaggandeService service;

    BetalningsforelaggandeController(BetalningsforelaggandeService service) {
        this.service = service;
    }

    @PostMapping("/delgivning")
    ArendeResponse delge(@PathVariable long id, @Valid @RequestBody DelgivningRequest request) {
        return service.delge(id, request);
    }

    @PostMapping("/bestridande")
    ArendeResponse bestrid(@PathVariable long id, @Valid @RequestBody BestridandeRequest request) {
        return service.bestrid(id, request);
    }

    @PostMapping("/utslag")
    ArendeResponse utslag(@PathVariable long id) {
        return service.meddelaUtslag(id);
    }

    @PostMapping("/overlamning-tingsratt")
    ArendeResponse overlamna(@PathVariable long id) {
        return service.overlamnaTillTingsratt(id);
    }

    /// Skapar ett NYTT utsökningsärende → 201 Created.
    @PostMapping("/verkstallighet")
    @ResponseStatus(HttpStatus.CREATED)
    ArendeResponse verkstallighet(@PathVariable long id, @Valid @RequestBody VerkstallighetRequest request) {
        return service.verkstall(id, request);
    }
}

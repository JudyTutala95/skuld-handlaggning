package se.jtutala.handlaggning.arende;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/// HTTP för det som gäller **alla ärendetyper**: registrera, läsa, utreda och avskriva.
///
/// Processpecifika åtgärder finns i `BetalningsforelaggandeController`,
/// `UtsokningController` och `SkuldsaneringController`.
@RestController
@RequestMapping("/api/arenden")
class ArendeController {

    private final ArendeRegistrering registrering;
    private final ArendeUppslag uppslag;
    private final GemensammaAtgarder atgarder;

    ArendeController(ArendeRegistrering registrering, ArendeUppslag uppslag, GemensammaAtgarder atgarder) {
        this.registrering = registrering;
        this.uppslag = uppslag;
        this.atgarder = atgarder;
    }

    @PostMapping
    ResponseEntity<ArendeSvar> registrera(@Valid @RequestBody RegistreraArende kommando) {
        ArendeSvar svar = registrering.registrera(kommando);
        return ResponseEntity.created(URI.create("/api/arenden/" + svar.id())).body(svar);
    }

    /// Arbetslista: `GET /api/arenden?status=DELGIVET` eller `?galdenarId=1`.
    @GetMapping
    List<ArendeRad> arbetslista(@RequestParam(required = false) @Nullable Long galdenarId,
                                @RequestParam(required = false) @Nullable Arendestatus status) {
        return uppslag.arbetslista(galdenarId, status);
    }

    @GetMapping("/{id}")
    ArendeSvar hamta(@PathVariable long id) {
        return uppslag.hamta(id);
    }

    @PostMapping("/{id}/utredning")
    ArendeSvar utredning(@PathVariable long id) {
        return atgarder.paborjaUtredning(id);
    }

    @PostMapping("/{id}/avskrivning")
    ArendeSvar avskriv(@PathVariable long id, @Valid @RequestBody Avskrivning kommando) {
        return atgarder.avskriv(id, kommando.orsak());
    }
}

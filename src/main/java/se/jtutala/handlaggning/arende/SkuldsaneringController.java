package se.jtutala.handlaggning.arende;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// HTTP för **skuldsanering**.
@RestController
@RequestMapping("/api/arenden/{id}/skuldsanering")
class SkuldsaneringController {

    private final SkuldsaneringHandlaggning handlaggning;

    SkuldsaneringController(SkuldsaneringHandlaggning handlaggning) {
        this.handlaggning = handlaggning;
    }

    @PostMapping("/beslut")
    ArendeSvar beslut(@PathVariable long id, @Valid @RequestBody Skuldsaneringsbeslut beslut) {
        return beslut.beviljas()
                ? handlaggning.bevilja(id, beslut.motivering())
                : handlaggning.avsla(id, beslut.motivering());
    }

    @PostMapping("/slutfor")
    ArendeSvar slutfor(@PathVariable long id) {
        return handlaggning.slutfor(id);
    }
}

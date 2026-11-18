package se.jtutala.handlaggning.galdenar.service;

import org.springframework.stereotype.Service;

import se.jtutala.handlaggning.galdenar.GaldenarApi;
import se.jtutala.handlaggning.regelverk.Forbehallsberakning;

/// Implementerar modulens publika API genom att delegera till rätt service.
@Service
class GaldenarApiService implements GaldenarApi {

    private final GaldenarSokService sokService;
    private final GaldenarEkonomiService ekonomiService;

    GaldenarApiService(GaldenarSokService sokService, GaldenarEkonomiService ekonomiService) {
        this.sokService = sokService;
        this.ekonomiService = ekonomiService;
    }

    @Override
    public void kontrolleraAttFinns(long galdenarId) {
        sokService.kontrolleraAttFinns(galdenarId);
    }

    @Override
    public Forbehallsberakning forbehallsberakning(long galdenarId) {
        return ekonomiService.forbehallsberakning(galdenarId);
    }
}

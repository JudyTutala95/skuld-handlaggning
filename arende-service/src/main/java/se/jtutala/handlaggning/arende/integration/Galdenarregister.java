package se.jtutala.handlaggning.arende.integration;

import java.math.BigDecimal;

import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import se.jtutala.handlaggning.arende.common.HittadesInteException;

/// Det enda service-lagret ser av **gäldenärstjänsten**.
///
/// Ansvar: översätta nätverksanrop till ärendetjänstens eget språk.
///
/// - **404** från gäldenärstjänsten → `HittadesInteException` (blir 404 hos oss också).
/// - **Nere eller 5xx** → nytt försök två gånger med 200 ms, sedan 400 ms väntan
///   (`@Retryable`, Spring Framework 7). Går det ändå inte svarar vi 503 (`GlobalFelhanterare`).
///
/// Bara läsande anrop görs om. Ett anrop som ändrar något får man inte upprepa hur som helst.
@Component
public class Galdenarregister {

    private final GaldenarKlient klient;

    Galdenarregister(GaldenarKlient klient) {
        this.klient = klient;
    }

    @Retryable(includes = {ResourceAccessException.class, HttpServerErrorException.class},
            maxRetries = 2, delay = 200, multiplier = 2)
    public void kontrolleraAttFinns(long galdenarId) {
        try {
            klient.hamta(galdenarId);
        } catch (HttpClientErrorException.NotFound e) {
            throw new HittadesInteException("Gäldenär", galdenarId);
        }
    }

    @Retryable(includes = {ResourceAccessException.class, HttpServerErrorException.class},
            maxRetries = 2, delay = 200, multiplier = 2)
    public BigDecimal utmatningsbartBelopp(long galdenarId) {
        try {
            return klient.forbehallsbelopp(galdenarId).utmatningsbartBelopp();
        } catch (HttpClientErrorException.NotFound e) {
            throw new HittadesInteException("Gäldenär", galdenarId);
        }
    }
}

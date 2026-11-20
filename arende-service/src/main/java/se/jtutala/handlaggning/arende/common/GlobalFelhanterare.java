package se.jtutala.handlaggning.arende.common;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/// Ansvar: **översätta undantag till HTTP-felsvar**. En metod per feltyp.
///
/// Själva svarsformatet skapas av `Problemsvar`.
@RestControllerAdvice
public class GlobalFelhanterare extends ResponseEntityExceptionHandler {

    @ExceptionHandler(HittadesInteException.class)
    ProblemDetail hittadesInte(HittadesInteException ex) {
        return Problemsvar.skapa(HttpStatus.NOT_FOUND, "Hittades inte", ex.getMessage(), "hittades-inte");
    }

    @ExceptionHandler(RegelbrottException.class)
    ProblemDetail regelbrott(RegelbrottException ex) {
        return Problemsvar.skapa(HttpStatus.CONFLICT, "Åtgärden är inte tillåten", ex.getMessage(), "regelbrott");
    }

    /// En annan tjänst som behövs svarar inte: den är nere, för långsam (timeout) eller
    /// svarar med 5xx. Klienten kan försöka igen senare, därför 503 och inte 500.
    @ExceptionHandler({ResourceAccessException.class, HttpServerErrorException.class})
    ProblemDetail annanTjanstSvararInte(RestClientException ex) {
        return Problemsvar.skapa(HttpStatus.SERVICE_UNAVAILABLE, "En tjänst svarar inte",
                "En tjänst som behövs för åtgärden svarar inte just nu. Försök igen om en stund.", "tjanst-svarar-inte");
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail samtidigAndring(OptimisticLockingFailureException ex) {
        return Problemsvar.skapa(HttpStatus.CONFLICT, "Samtidig ändring",
                "Ärendet ändrades av någon annan samtidigt. Ladda om och försök igen.", "samtidig-andring");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail ogiltigtVarde(IllegalArgumentException ex) {
        String detalj = Objects.requireNonNullElse(ex.getMessage(), "Ogiltigt värde");
        return Problemsvar.skapa(HttpStatus.BAD_REQUEST, "Ogiltigt värde", detalj, "ogiltigt-varde");
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail body = ex.getBody();
        body.setTitle("Valideringsfel");
        body.setDetail("Ett eller flera fält är ogiltiga.");
        body.setType(Problemsvar.typUri("validering"));
        body.setProperty("fel", faltfel(ex.getBindingResult()));
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    /// Fältnamn → första felmeddelandet för fältet.
    private static Map<String, String> faltfel(BindingResult resultat) {
        Map<String, String> fel = new LinkedHashMap<>();
        for (FieldError f : resultat.getFieldErrors()) {
            fel.putIfAbsent(f.getField(), Objects.requireNonNullElse(f.getDefaultMessage(), "ogiltigt värde"));
        }
        return fel;
    }
}

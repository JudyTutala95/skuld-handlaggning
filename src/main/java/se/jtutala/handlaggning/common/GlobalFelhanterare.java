package se.jtutala.handlaggning.common;

import java.net.URI;
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
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/// Översätter fel till **Problem Details (RFC 9457)** – ett standardformat för fel i HTTP-API:er.
///
/// ```json
/// { "type": "urn:problem:regelbrott", "title": "Åtgärden är inte tillåten", "status": 409,
///   "detail": "Bestridandet kom in 2026-02-20, efter fristen 2026-02-15" }
/// ```
@RestControllerAdvice
public class GlobalFelhanterare extends ResponseEntityExceptionHandler {

    @ExceptionHandler(HandlaggningException.class)
    ProblemDetail hantera(HandlaggningException ex) {
        // Uttömmande switch över en sealed klass – ingen default behövs.
        return switch (ex) {
            case HittadesInteException e -> problem(HttpStatus.NOT_FOUND, "Hittades inte", e.getMessage(), "hittades-inte");
            case RegelbrottException e -> problem(HttpStatus.CONFLICT, "Åtgärden är inte tillåten", e.getMessage(), "regelbrott");
        };
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail samtidigAndring(OptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT, "Samtidig ändring",
                "Ärendet ändrades av någon annan samtidigt. Ladda om och försök igen.", "samtidig-andring");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail ogiltigtVarde(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Ogiltigt värde", ex.getMessage(), "ogiltigt-varde");
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> fel = new LinkedHashMap<>();
        for (FieldError f : ex.getBindingResult().getFieldErrors()) {
            fel.putIfAbsent(f.getField(), Objects.requireNonNullElse(f.getDefaultMessage(), "ogiltigt värde"));
        }
        ProblemDetail body = ex.getBody();
        body.setTitle("Valideringsfel");
        body.setDetail("Ett eller flera fält är ogiltiga.");
        body.setType(URI.create("urn:problem:validering"));
        body.setProperty("fel", fel);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    private static ProblemDetail problem(HttpStatus status, String titel, @Nullable String detalj, String typ) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, Objects.requireNonNullElse(detalj, titel));
        p.setTitle(titel);
        p.setType(URI.create("urn:problem:" + typ));
        return p;
    }
}

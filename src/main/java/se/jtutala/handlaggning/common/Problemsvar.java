package se.jtutala.handlaggning.common;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/// Skapar felsvar i formatet **Problem Details (RFC 9457)**. Inget annat.
final class Problemsvar {

    private Problemsvar() {
    }

    static ProblemDetail skapa(HttpStatus status, String titel, String detalj, String typ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detalj);
        problem.setTitle(titel);
        problem.setType(typUri(typ));
        return problem;
    }

    static URI typUri(String typ) {
        return URI.create("urn:problem:" + typ);
    }
}

package se.jtutala.handlaggning.arende.common;

/// Något som efterfrågades finns inte. Blir HTTP 404.
public final class HittadesInteException extends HandlaggningException {

    public HittadesInteException(String vad, Object id) {
        super("%s med id %s hittades inte".formatted(vad, id));
    }
}

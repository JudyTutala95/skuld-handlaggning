package se.jtutala.handlaggning.arende.common;

/// En åtgärd bryter mot en regel i handläggningen, t.ex. "bestridandet kom in för sent"
/// eller "ett avslutat ärende kan inte ändras". Blir HTTP 409 Conflict.
///
/// Meddelandet ska vara begripligt för handläggaren – det visas i gränssnittet.
public final class RegelbrottException extends HandlaggningException {

    public RegelbrottException(String message) {
        super(message);
    }
}

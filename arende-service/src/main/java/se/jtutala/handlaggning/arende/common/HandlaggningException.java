package se.jtutala.handlaggning.arende.common;

/// Basklass för alla förväntade fel i systemet. `sealed` = bara subtyperna nedan finns,
/// så felhanteraren kan hantera alla fall i en uttömmande `switch`.
public abstract sealed class HandlaggningException extends RuntimeException
        permits HittadesInteException, RegelbrottException {

    protected HandlaggningException(String message) {
        super(message);
    }
}

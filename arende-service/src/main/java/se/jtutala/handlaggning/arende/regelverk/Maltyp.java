package se.jtutala.handlaggning.arende.regelverk;

/// Typ av utsökningsmål.
public enum Maltyp {

    /// Skulder till staten/det allmänna, t.ex. skatter, böter, CSN-skulder, felparkeringsavgifter.
    /// Staten (t.ex. Skatteverket) lämnar över skulden direkt för indrivning – ingen ansökan behövs.
    ALLMANT,

    /// Skulder till privata borgenärer, t.ex. obetalda fakturor eller lån.
    /// Borgenären måste ha en *exekutionstitel* (t.ex. ett utslag om betalningsföreläggande
    /// eller en dom) och ansöka om verkställighet.
    ENSKILT
}

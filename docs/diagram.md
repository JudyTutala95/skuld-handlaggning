# Diagram – så hänger allt ihop

Diagrammen är skrivna i [Mermaid](https://mermaid.js.org) och visas direkt på GitHub och i
IntelliJ/VS Code (med Mermaid-tillägg). Läs dem uppifrån och ned: från helheten till detaljerna.

1. [Helhetsbild](#1-helhetsbild)
2. [Moduler och beroenden](#2-moduler-och-beroenden)
3. [Lagren i ett anrop](#3-lagren-i-ett-anrop)
4. [Processkedjan: från krav till betald skuld](#4-processkedjan-från-krav-till-betald-skuld)
5. [Statusflöden](#5-statusflöden)
6. [Klassdiagram: ärenden](#6-klassdiagram-ärenden)
7. [Klassdiagram: användningsfall och controllers](#7-klassdiagram-användningsfall-och-controllers)
8. [Klassdiagram: gäldenär, regelverk och notifiering](#8-klassdiagram-gäldenär-regelverk-och-notifiering)
9. [Sekvens: delgivning och brev](#9-sekvens-delgivning-och-brev)
10. [Sekvens: löneutmätning över modulgränsen](#10-sekvens-löneutmätning-över-modulgränsen)
11. [Databas](#11-databas)

---

## 1. Helhetsbild

Vem använder systemet, och vad finns innanför?

```mermaid
flowchart LR
    H(["👩‍💼 Handläggare"])
    B(["🏢 Borgenär<br/>t.ex. inkassobolag"])
    G(["🧑 Gäldenär"])

    subgraph APP["Spring Boot-applikationen"]
        direction TB
        API["REST-API<br/>/api/galdenarer<br/>/api/arenden"]
        MOD["Moduler<br/>galdenar · arende · regelverk · notifiering · common"]
        API --> MOD
    end

    DB[("H2-databas<br/>schema via Flyway")]
    LOG["📬 Brev<br/>(loggas)"]

    H -->|registrerar och handlägger ärenden| API
    B -.->|ansöker – via handläggaren| H
    MOD --> DB
    MOD --> LOG
    LOG -.->|skickas till| G
```

---

## 2. Moduler och beroenden

Varje modul är ett paket. Pilarna visar vem som **får** anropa vem – Spring Modulith
kontrollerar det i `ModulStrukturTest`. Inga cykler: `arende` vet inte att `notifiering` finns.

```mermaid
flowchart TB
    subgraph arende["📁 arende"]
        A_API["Publikt API<br/>ArendeRegistrering · ArendeUppslag<br/>…Handlaggning · ArendeStatusAndrad"]
    end

    subgraph galdenar["📁 galdenar"]
        G_API["Publikt API<br/>GaldenarRegistrering · GaldenarUppslag<br/>GaldenarEkonomi · Personnummer"]
    end

    subgraph regelverk["📁 regelverk (ren Java)"]
        R_API["Forbehallsbelopp · Normalbelopp<br/>Preskription · Hushall · Maltyp"]
    end

    subgraph notifiering["📁 notifiering"]
        N["BrevUtskick · Brevmall · Brevlada"]
    end

    subgraph common["📁 common (OPEN)"]
        C["Kalender · GlobalFelhanterare<br/>RegelbrottException · HittadesInteException"]
    end

    arende -->|"kontrollerar gäldenär,<br/>hämtar förbehållsbelopp"| galdenar
    arende -->|preskription| regelverk
    galdenar -->|förbehållsbelopp| regelverk
    arende -. "händelse: ArendeStatusAndrad" .-> notifiering
    arende --> common
    galdenar --> common
```

---

## 3. Lagren i ett anrop

Samma väg för alla anrop. Varje låda är en egen klass med ett ansvar.

```mermaid
flowchart LR
    HTTP(["HTTP-anrop<br/>JSON"])
    CTRL["Controller<br/>tar emot DTO<br/>validerar med @Valid"]
    UC["Användningsfall<br/>t.ex. BetalningsforelaggandeHandlaggning"]
    LAGER["Arendelager<br/>hämta / spara"]
    DOM["Domänobjekt<br/>t.ex. Betalningsforelaggande<br/>⚖️ affärsreglerna"]
    REPO["ArendeRepository<br/>Spring Data JPA"]
    DB[("Databas")]
    MAP["ArendeSvarMapper"]
    SVAR(["HTTP-svar<br/>ArendeSvar som JSON"])
    FEL["GlobalFelhanterare<br/>→ Problem Details"]

    HTTP --> CTRL --> UC
    UC -->|1. hämta| LAGER --> REPO --> DB
    UC -->|2. utför| DOM
    UC -->|3. spara| LAGER
    UC --> MAP --> SVAR
    DOM -. "RegelbrottException" .-> FEL
    FEL -. "409 Conflict" .-> SVAR
```

---

## 4. Processkedjan: från krav till betald skuld

Hur de tre ärendetyperna hänger ihop i verkligheten. Ett utslag i ett betalningsföreläggande
kan bli ett **nytt** utsökningsärende (`ursprungsarendeId` pekar tillbaka).

```mermaid
flowchart TB
    subgraph BF["Betalningsföreläggande"]
        direction TB
        bf1["Borgenären ansöker"] --> bf2["Gäldenären delges"]
        bf2 --> bf3{"Bestridande<br/>inom 10 dagar?"}
        bf3 -->|nej| bf4["Utslag<br/>= exekutionstitel"]
        bf3 -->|ja| bf5["Tingsrätten<br/>eller avskrivning"]
    end

    subgraph UTS["Utsökning"]
        direction TB
        u1["Registrerat"] --> u2["Utredning av<br/>inkomster och tillgångar"]
        u2 --> u3["Löneutmätning<br/>nettolön − förbehållsbelopp"]
        u3 --> u4["Inbetalningar"]
        u4 --> u5(["Betalt ✅"])
        u2 -.-> u6(["Preskriberat"])
    end

    subgraph SKS["Skuldsanering"]
        direction TB
        s1["Ansökan"] --> s2["Utredning"]
        s2 --> s3{"Beviljas?"}
        s3 -->|ja| s4["Betalningsplan i 5 år"] --> s5(["Skulderna borta ✅"])
        s3 -->|nej| s6(["Avslag"])
    end

    ALLM["Allmänt mål<br/>t.ex. skatteskuld"] -->|lämnas direkt för indrivning| u1
    bf4 -->|verkställighet| u1
    u3 -. "orimligt stora skulder" .-> s1
```

---

## 5. Statusflöden

Varje ärendeklass har en konstant `FLODE` (`Statusflode`) med exakt dessa övergångar.
Allt annat avvisas med 409.

### Betalningsföreläggande

```mermaid
stateDiagram-v2
    [*] --> REGISTRERAT
    REGISTRERAT --> DELGIVET : delge
    REGISTRERAT --> AVSKRIVET : avskriv
    DELGIVET --> UTSLAG_MEDDELAT : utslag efter fristen
    DELGIVET --> BESTRITT : bestrid inom 10 dagar
    DELGIVET --> AVSKRIVET : avskriv
    BESTRITT --> OVERLAMNAT_TILL_TINGSRATT : överlämna
    BESTRITT --> AVSKRIVET : avskriv
    UTSLAG_MEDDELAT --> [*] : verkställighet skapar utsökningsärende
    OVERLAMNAT_TILL_TINGSRATT --> [*]
    AVSKRIVET --> [*]
```

### Utsökning

```mermaid
stateDiagram-v2
    [*] --> REGISTRERAT
    REGISTRERAT --> UTREDNING : utredning
    UTREDNING --> LONEUTMATNING : löneutmätning
    REGISTRERAT --> BETALT : sista inbetalningen
    UTREDNING --> BETALT : sista inbetalningen
    LONEUTMATNING --> BETALT : sista inbetalningen
    REGISTRERAT --> PRESKRIBERAT : preskriptionsdagen passerad
    UTREDNING --> PRESKRIBERAT : preskriptionsdagen passerad
    LONEUTMATNING --> PRESKRIBERAT : preskriptionsdagen passerad
    REGISTRERAT --> AVSKRIVET
    UTREDNING --> AVSKRIVET
    LONEUTMATNING --> AVSKRIVET
    BETALT --> [*]
    PRESKRIBERAT --> [*]
    AVSKRIVET --> [*]
```

### Skuldsanering

```mermaid
stateDiagram-v2
    [*] --> REGISTRERAT
    REGISTRERAT --> UTREDNING : utredning
    REGISTRERAT --> AVSKRIVET : avskriv
    UTREDNING --> SKULDSANERING_PAGAR : bevilja
    UTREDNING --> SKULDSANERING_AVSLAG : avslå
    UTREDNING --> AVSKRIVET : avskriv
    SKULDSANERING_PAGAR --> SKULDSANERING_SLUTFORD : efter 5 år
    SKULDSANERING_SLUTFORD --> [*]
    SKULDSANERING_AVSLAG --> [*]
    AVSKRIVET --> [*]
```

---

## 6. Klassdiagram: ärenden

Basklassen har det gemensamma. Varje subklass har reglerna för sin process.
Små värdeobjekt bär var sin regel.

```mermaid
classDiagram
    direction TB

    class Arende {
        <<abstract>>
        -Long id
        -Arendestatus status
        -Maltyp maltyp
        -long galdenarId
        -String handlaggare
        -LocalDate registrerad
        +typ() Arendetyp*
        #statusflode() Statusflode*
        #senastePreskriptionsavbrott() LocalDate
        +paborjaUtredning(idag)
        +avskriv(orsak, idag)
        +preskriptionsdag() LocalDate
        +mojligaNastaStatusar() List
        +arendenummer() String
        #bytStatus(ny, idag, text)
    }

    class Betalningsforelaggande {
        -LocalDate delgivningsdatum
        +delge(dag, idag)
        +bestrid(inkom, grund, idag)
        +meddelaUtslag(idag)
        +overlamnaTillTingsratt(idag)
        +verkstall(handlaggare, idag) Utsokningsarende
    }

    class Utsokningsarende {
        -BigDecimal loneutmatningPerManad
        -Long ursprungsarendeId
        +beslutaLoneutmatning(belopp, idag)
        +registreraInbetalning(inbetalning, idag)
        +markeraPreskriberat(idag)
        +kvarstaendeSkuld() BigDecimal
    }

    class Skuldsaneringsarende {
        -LocalDate beviljad
        +bevilja(motivering, idag)
        +avsla(motivering, idag)
        +slutfor(idag)
    }

    class Fordran {
        <<record>>
        borgenar
        kapitalbelopp
        forfallodag
        konsumentfordran
        +harForfallit(idag) boolean
    }
    class Dagbok {
        +anteckna(datum, text)
    }
    class Dagboksanteckning {
        <<record>>
        datum
        handelse
    }
    class Inbetalningar {
        +registrera(inbetalning)
        +summa() BigDecimal
    }
    class Inbetalning {
        <<record>>
        belopp
        datum
    }
    class Bestridandefrist {
        <<record>>
        delgivningsdag
        +sistaDag() LocalDate
        +arIntime(datum) boolean
        +harGattUt(idag) boolean
    }
    class Skuldsaneringsperiod {
        <<record>>
        beviljad
        +slutar() LocalDate
    }
    class Statusflode {
        +tillater(fran, till) boolean
        +nastaFran(status) List
    }
    class ArendeStatusAndrad {
        <<domänhändelse>>
        arendeId
        fran
        till
    }
    class AbstractAggregateRoot {
        <<Spring Data>>
        #registerEvent(handelse)
    }

    AbstractAggregateRoot <|-- Arende
    Arende <|-- Betalningsforelaggande
    Arende <|-- Utsokningsarende
    Arende <|-- Skuldsaneringsarende

    Arende *-- Fordran
    Arende *-- Dagbok
    Dagbok *-- "0..*" Dagboksanteckning
    Utsokningsarende *-- Inbetalningar
    Inbetalningar *-- "0..*" Inbetalning

    Arende ..> Statusflode : frågar
    Arende ..> ArendeStatusAndrad : registrerar
    Betalningsforelaggande ..> Bestridandefrist : skapar
    Betalningsforelaggande ..> Utsokningsarende : verkstall skapar
    Skuldsaneringsarende ..> Skuldsaneringsperiod : skapar
```

---

## 7. Klassdiagram: användningsfall och controllers

En controller och ett användningsfall per process. Alla delar på `Arendelager` (hämta/spara)
och `ArendeSvarMapper` (svar).

```mermaid
classDiagram
    direction LR

    class ArendeController
    class BetalningsforelaggandeController
    class UtsokningController
    class SkuldsaneringController

    class ArendeRegistrering {
        +registrera(kommando) ArendeSvar
    }
    class ArendeUppslag {
        +hamta(id) ArendeSvar
        +arbetslista(galdenarId, status) List
    }
    class GemensammaAtgarder {
        +paborjaUtredning(id)
        +avskriv(id, orsak)
    }
    class BetalningsforelaggandeHandlaggning {
        +delge(id, datum)
        +bestrid(id, datum, grund)
        +meddelaUtslag(id)
        +overlamnaTillTingsratt(id)
        +verkstall(id, handlaggare)
    }
    class UtsokningHandlaggning {
        +beslutaLoneutmatning(id)
        +registreraInbetalning(id, inbetalning)
        +markeraPreskriberat(id)
    }
    class SkuldsaneringHandlaggning {
        +bevilja(id, motivering)
        +avsla(id, motivering)
        +slutfor(id)
    }

    class Arendelager {
        +hamta(id) Arende
        +hamta(id, typ) T
        +spara(arende) T
    }
    class ArendeFabrik {
        +skapa(typ, ...) Arende
    }
    class ArendeSvarMapper {
        +tillSvar(arende) ArendeSvar
        +tillRad(arende) ArendeRad
    }
    class ArendeRepository {
        <<Spring Data JPA>>
    }
    class Kalender {
        <<common>>
        +idag() LocalDate
    }
    class GaldenarUppslag {
        <<galdenar>>
    }
    class GaldenarEkonomi {
        <<galdenar>>
    }

    ArendeController --> ArendeRegistrering
    ArendeController --> ArendeUppslag
    ArendeController --> GemensammaAtgarder
    BetalningsforelaggandeController --> BetalningsforelaggandeHandlaggning
    UtsokningController --> UtsokningHandlaggning
    SkuldsaneringController --> SkuldsaneringHandlaggning

    ArendeRegistrering --> ArendeFabrik
    ArendeRegistrering --> GaldenarUppslag
    UtsokningHandlaggning --> GaldenarEkonomi

    ArendeRegistrering --> Arendelager
    ArendeUppslag --> Arendelager
    GemensammaAtgarder --> Arendelager
    BetalningsforelaggandeHandlaggning --> Arendelager
    UtsokningHandlaggning --> Arendelager
    SkuldsaneringHandlaggning --> Arendelager
    Arendelager --> ArendeRepository

    BetalningsforelaggandeHandlaggning ..> ArendeSvarMapper
    UtsokningHandlaggning ..> ArendeSvarMapper
    SkuldsaneringHandlaggning ..> ArendeSvarMapper
    BetalningsforelaggandeHandlaggning ..> Kalender
    UtsokningHandlaggning ..> Kalender
    SkuldsaneringHandlaggning ..> Kalender
```

---

## 8. Klassdiagram: gäldenär, regelverk och notifiering

```mermaid
classDiagram
    direction LR

    namespace galdenar {
        class GaldenarController
        class GaldenarEkonomiController
        class GaldenarRegistrering {
            +registrera(kommando) GaldenarSvar
        }
        class GaldenarUppslag {
            +hamta(id) GaldenarSvar
            +sokPaPersonnummer(text) GaldenarSvar
            +kontrolleraAttFinns(id)
        }
        class GaldenarEkonomi {
            +uppdatera(id, kommando) GaldenarSvar
            +forbehallsberakning(id) Forbehallsberakning
        }
        class Galdenar {
            <<entity>>
            -String fornamn
            -String efternamn
            -String adress
        }
        class Personnummer {
            <<record>>
            +parse(text)$ Personnummer
            +maskerat() String
        }
        class Luhn {
            +arGiltigt(siffror)$ boolean
        }
        class Ekonomi {
            <<embeddable>>
            +av(...)$ Ekonomi
            +somHushall(idag) Hushall
        }
    }

    namespace regelverk {
        class Forbehallsbelopp {
            +berakna(hushall, normalbelopp)$ Forbehallsberakning
        }
        class Normalbelopp {
            <<record>>
            +AR_2026$
            +forBarn(alder) BigDecimal
        }
        class Hushall {
            <<record>>
        }
        class Forbehallsberakning {
            <<record>>
            utmatningsbartBelopp
        }
        class Preskription {
            +preskriptionsdag(...)$ LocalDate
        }
    }

    namespace notifiering {
        class BrevUtskick {
            +nar(ArendeStatusAndrad)
        }
        class Brevmall {
            +textFor(handelse)$ Optional
        }
        class Brevlada {
            <<interface>>
            +skicka(galdenarId, text)
        }
        class LoggandeBrevlada
    }

    GaldenarController --> GaldenarRegistrering
    GaldenarController --> GaldenarUppslag
    GaldenarEkonomiController --> GaldenarEkonomi
    GaldenarEkonomi --> GaldenarUppslag
    Galdenar *-- Personnummer
    Galdenar *-- Ekonomi
    Personnummer ..> Luhn
    Ekonomi ..> Hushall : skapar
    GaldenarEkonomi ..> Forbehallsbelopp
    Forbehallsbelopp ..> Normalbelopp
    Forbehallsbelopp ..> Hushall
    Forbehallsbelopp ..> Forbehallsberakning : skapar

    BrevUtskick --> Brevmall
    BrevUtskick --> Brevlada
    Brevlada <|.. LoggandeBrevlada
```

---

## 9. Sekvens: delgivning och brev

Vad händer när handläggaren registrerar en delgivning? Notera att brevet skickas
**efter** att transaktionen committats, på en annan tråd.

```mermaid
sequenceDiagram
    autonumber
    actor H as Handläggare
    participant C as BetalningsforelaggandeController
    participant UC as BetalningsforelaggandeHandlaggning
    participant L as Arendelager
    participant BF as Betalningsforelaggande
    participant DB as Databas
    participant N as BrevUtskick
    participant BL as Brevlada

    H->>C: POST /api/arenden/1/delgivning {"datum": "2026-05-10"}
    C->>C: @Valid Delgivning
    C->>UC: delge(1, 2026-05-10)
    activate UC
    Note over UC,DB: @Transactional startar
    UC->>L: hamta(1, Betalningsforelaggande.class)
    L->>DB: SELECT … FROM arende WHERE id = 1
    DB-->>L: rad med typ = BETALNINGSFORELAGGANDE
    L-->>UC: Betalningsforelaggande
    UC->>BF: delge(2026-05-10, idag)
    activate BF
    BF->>BF: kontrolleraInteIFramtiden()
    BF->>BF: bytStatus(DELGIVET) – kontrollerar Statusflode
    BF->>BF: dagbok.anteckna("Gäldenären delgiven …")
    BF->>BF: registerEvent(ArendeStatusAndrad)
    deactivate BF
    UC->>L: spara(bf)
    L->>DB: UPDATE arende … / INSERT arende_dagbok
    Note over L: Spring Data publicerar ArendeStatusAndrad
    Note over UC,DB: COMMIT
    UC-->>C: ArendeSvar (via ArendeSvarMapper)
    deactivate UC
    C-->>H: 200 OK + JSON

    N-)N: @ApplicationModuleListener – asynkront efter commit
    N->>N: Brevmall.textFor(handelse)
    N->>BL: skicka(galdenarId, "Du har fått ett betalningsföreläggande …")
```

### Om en regel bryts

```mermaid
sequenceDiagram
    actor H as Handläggare
    participant C as BetalningsforelaggandeController
    participant BF as Betalningsforelaggande
    participant F as GlobalFelhanterare

    H->>C: POST /api/arenden/1/bestridande {"datum": "2026-05-25"}
    C->>BF: bestrid(…) via användningsfallet
    BF-->>C: RegelbrottException "Bestridandet kom in 2026-05-25, efter fristen 2026-05-20"
    Note over C: transaktionen rullas tillbaka – inget sparas
    C->>F: undantaget fångas
    F-->>H: 409 Conflict, application/problem+json
```

---

## 10. Sekvens: löneutmätning över modulgränsen

Ärendemodulen frågar gäldenärsmodulen, som frågar regelverket. Ingen modul rör en annan
moduls entiteter – bara deras publika tjänster.

```mermaid
sequenceDiagram
    autonumber
    actor H as Handläggare
    participant UC as UtsokningHandlaggning<br/>(arende)
    participant UTS as Utsokningsarende<br/>(arende)
    participant GE as GaldenarEkonomi<br/>(galdenar)
    participant E as Ekonomi<br/>(galdenar)
    participant FB as Forbehallsbelopp<br/>(regelverk)

    H->>UC: POST /api/arenden/2/loneutmatning
    UC->>UC: hamta(2) som Utsokningsarende
    UC->>GE: forbehallsberakning(galdenarId)
    GE->>E: somHushall(idag)
    E-->>GE: Hushall (inkomst, boende, sambo, barnens åldrar)
    GE->>FB: berakna(hushall, Normalbelopp.AR_2026)
    Note right of FB: 34 000 − (9 495 + 5 500)<br/>= 19 005 kr/mån
    FB-->>GE: Forbehallsberakning
    GE-->>UC: utmätningsbart belopp = 19 005
    UC->>UTS: beslutaLoneutmatning(19 005, idag)
    UTS->>UTS: kontrolleraUtmatningsbart() + bytStatus(LONEUTMATNING)
    UC-->>H: 200 OK, status LONEUTMATNING
```

---

## 11. Databas

Tabellerna skapas av Flyway (`V1__schema.sql`). Alla tre ärendetyper ligger i **samma tabell**
`arende`; kolumnen `typ` avgör vilken Java-klass raden blir (JPA `SINGLE_TABLE`).
Det finns ingen foreign key mellan `arende` och `galdenar` – modulerna äger sina egna tabeller.

```mermaid
erDiagram
    GALDENAR ||--o{ GALDENAR_BARN : "har barn"
    GALDENAR ||..o{ ARENDE : "galdenar_id (ingen FK)"
    ARENDE ||--o{ ARENDE_DAGBOK : "dagbok"
    ARENDE ||--o{ ARENDE_INBETALNING : "inbetalningar (utsökning)"
    ARENDE |o--o{ ARENDE : "ursprungsarende_id (utslag → utsökning)"

    GALDENAR {
        bigint id PK
        varchar personnummer UK "12 siffror"
        varchar fornamn
        varchar efternamn
        varchar adress
        numeric nettoinkomst_per_manad
        numeric boendekostnad_per_manad
        boolean sammanlevande
        bigint version "optimistisk låsning"
    }
    GALDENAR_BARN {
        bigint galdenar_id PK, FK
        int rad PK
        int fodelsear
    }
    ARENDE {
        bigint id PK
        varchar typ "BETALNINGSFORELAGGANDE / UTSOKNING / SKULDSANERING"
        varchar status
        varchar maltyp "ALLMANT / ENSKILT"
        bigint galdenar_id
        varchar borgenar
        numeric kapitalbelopp
        date forfallodag
        boolean konsumentfordran
        varchar handlaggare
        date registrerad
        date delgivningsdatum "bara BF"
        numeric loneutmatning_per_manad "bara UTS"
        bigint ursprungsarende_id FK "bara UTS"
        date skuldsanering_beviljad "bara SKS"
        bigint version
    }
    ARENDE_DAGBOK {
        bigint arende_id PK, FK
        int rad PK
        date datum
        varchar handelse
    }
    ARENDE_INBETALNING {
        bigint arende_id PK, FK
        int rad PK
        numeric belopp
        date datum
    }
```

---

> Spring Modulith genererar dessutom egna PlantUML-diagram över modulerna när testerna körs:
> `target/spring-modulith-docs/components.puml` (se `ModulStrukturTest`).

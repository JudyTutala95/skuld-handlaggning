# Diagram – så hänger allt ihop

Diagrammen är skrivna i [Mermaid](https://mermaid.js.org) och visas direkt på GitHub och i
IntelliJ/VS Code (med Mermaid-tillägg). Läs dem uppifrån och ned: från helheten till detaljerna.

1. [Helhetsbild](#1-helhetsbild)
2. [Moduler och beroenden](#2-moduler-och-beroenden)
3. [Lagren i ett anrop](#3-lagren-i-ett-anrop)
4. [Processkedjan: från krav till betald skuld](#4-processkedjan-från-krav-till-betald-skuld)
5. [Statusflöden](#5-statusflöden)
6. [Klassdiagram: ärenden](#6-klassdiagram-ärenden)
7. [Klassdiagram: ärendemodulens lager](#7-klassdiagram-ärendemodulens-lager)
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

Varje modul är ett paket. Det som ligger **direkt i modulens paket** är dess publika API;
allt i underpaketen (`web`, `service`, `domain` …) är internt. Pilarna visar vem som **får**
anropa vem – Spring Modulith kontrollerar det i `ModulStrukturTest`.

```mermaid
flowchart TB
    subgraph arende["📁 arende"]
        A_API["Publikt API<br/>ArendeStatusAndrad · Arendestatus · Arendetyp"]
        A_INT["internt: web · dto · service · domain · repository"]
    end

    subgraph galdenar["📁 galdenar"]
        G_API["Publikt API<br/>GaldenarApi"]
        G_INT["internt: web · dto · service · domain · repository"]
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

    A_INT -->|"GaldenarApi:<br/>kontrolleraAttFinns,<br/>forbehallsberakning"| G_API
    A_INT -->|preskription| regelverk
    G_INT -->|förbehållsbelopp| regelverk
    A_API -. "händelse: ArendeStatusAndrad" .-> N
    arende --> common
    galdenar --> common
```

---

## 3. Lagren i ett anrop

Varje modul är uppdelad i samma lager. Pilarna är de **enda** tillåtna beroendena –
`LagerArkitekturTest` (ArchUnit) underkänner bygget om någon bryter mot dem.

```mermaid
flowchart TB
    HTTP(["HTTP-anrop<br/>JSON"])

    subgraph web["web"]
        CTRL["*Controller<br/>tar emot Request-DTO, @Valid"]
    end
    subgraph dto["dto"]
        REQ["*Request · *Response<br/>records"]
    end
    subgraph service["service"]
        SVC["*Service<br/>ett användningsfall, @Transactional"]
        MAP["*Mapper<br/>DTO ⇄ domän"]
        HAMT["ArendeHamtare<br/>hämta med typkontroll"]
    end
    subgraph domain["domain"]
        DOM["Entiteter och värdeobjekt<br/>⚖️ affärsreglerna"]
    end
    subgraph repository["repository"]
        REPO["*Repository<br/>Spring Data JPA"]
    end
    DB[("Databas")]
    FEL["GlobalFelhanterare<br/>(common) → Problem Details"]

    HTTP --> CTRL
    CTRL --> SVC
    CTRL -.-> REQ
    SVC --> MAP
    SVC --> HAMT --> REPO --> DB
    SVC -->|"låter domänen<br/>göra jobbet"| DOM
    MAP -.-> REQ
    MAP -.-> DOM
    REPO -.-> DOM
    DOM -. "RegelbrottException" .-> FEL
    FEL -. "409 Conflict" .-> HTTP
```

| Lager | Får använda | Får inte använda |
|---|---|---|
| `web` | `service`, `dto` | `repository`, `domain` |
| `service` | `repository`, `domain`, `dto`, andra modulers API | `web` |
| `repository` | `domain` | `service`, `web`, `dto` |
| `domain` | `regelverk`, `common` | alla lager ovan |
| `dto` | – | alla lager |

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

## 7. Klassdiagram: ärendemodulens lager

En controller och en service per process. Alla services delar på `ArendeHamtare` (hämta/spara via
repository) och `ArendeMapper` (DTO ⇄ domän). Gäldenärsmodulen nås bara via `GaldenarApi`.

```mermaid
classDiagram
    direction LR

    namespace web {
        class ArendeController
        class BetalningsforelaggandeController
        class UtsokningController
        class SkuldsaneringController
    }

    namespace service {
        class ArendeRegistreringService {
            +registrera(RegistreraArendeRequest) ArendeResponse
        }
        class ArendeSokService {
            +hamta(id) ArendeResponse
            +arbetslista(galdenarId, status) List
        }
        class ArendeAtgardService {
            +paborjaUtredning(id) ArendeResponse
            +avskriv(id, AvskrivningRequest) ArendeResponse
        }
        class BetalningsforelaggandeService {
            +delge(id, DelgivningRequest)
            +bestrid(id, BestridandeRequest)
            +meddelaUtslag(id)
            +overlamnaTillTingsratt(id)
            +verkstall(id, VerkstallighetRequest)
        }
        class UtsokningService {
            +beslutaLoneutmatning(id)
            +registreraInbetalning(id, InbetalningRequest)
            +markeraPreskriberat(id)
        }
        class SkuldsaneringService {
            +besluta(id, SkuldsaneringsbeslutRequest)
            +slutfor(id)
        }
        class ArendeHamtare {
            +hamta(id) Arende
            +hamta(id, typ) T
            +spara(arende) T
        }
        class ArendeMapper {
            +tillFordran(request)$ Fordran
            +tillInbetalning(request)$ Inbetalning
            +tillResponse(arende)$ ArendeResponse
            +tillRad(arende)$ ArendeRadResponse
        }
    }

    namespace repository {
        class ArendeRepository {
            <<interface>>
            +sok(galdenarId, status) List
        }
    }

    namespace domain {
        class ArendeFabrik {
            +skapa(typ, ...)$ Arende
        }
        class Arende {
            <<abstract>>
        }
    }

    class GaldenarApi {
        <<interface>>
        +kontrolleraAttFinns(id)
        +forbehallsberakning(id)
    }

    ArendeController --> ArendeRegistreringService
    ArendeController --> ArendeSokService
    ArendeController --> ArendeAtgardService
    BetalningsforelaggandeController --> BetalningsforelaggandeService
    UtsokningController --> UtsokningService
    SkuldsaneringController --> SkuldsaneringService

    ArendeRegistreringService --> ArendeFabrik
    ArendeRegistreringService --> GaldenarApi
    UtsokningService --> GaldenarApi

    ArendeRegistreringService --> ArendeHamtare
    ArendeSokService --> ArendeHamtare
    ArendeAtgardService --> ArendeHamtare
    BetalningsforelaggandeService --> ArendeHamtare
    UtsokningService --> ArendeHamtare
    SkuldsaneringService --> ArendeHamtare
    ArendeSokService --> ArendeRepository
    ArendeHamtare --> ArendeRepository
    ArendeRepository ..> Arende

    BetalningsforelaggandeService ..> ArendeMapper
    UtsokningService ..> ArendeMapper
    SkuldsaneringService ..> ArendeMapper
```

---

## 8. Klassdiagram: gäldenär, regelverk och notifiering

`GaldenarApi` ligger direkt i modulens paket och är det enda andra moduler ser.

```mermaid
classDiagram
    direction LR

    class GaldenarApi {
        <<interface>>
        +kontrolleraAttFinns(id)
        +forbehallsberakning(id) Forbehallsberakning
    }

    namespace galdenar_web {
        class GaldenarController
        class GaldenarEkonomiController
    }

    namespace galdenar_service {
        class GaldenarApiService
        class GaldenarRegistreringService {
            +registrera(RegistreraGaldenarRequest) GaldenarResponse
        }
        class GaldenarSokService {
            +hamta(id) GaldenarResponse
            +sok(SokGaldenarRequest) GaldenarResponse
        }
        class GaldenarEkonomiService {
            +uppdatera(id, UppdateraEkonomiRequest) GaldenarResponse
            +forbehallsberakning(id) Forbehallsberakning
        }
        class GaldenarMapper
    }

    namespace galdenar_repository {
        class GaldenarRepository {
            <<interface>>
        }
    }

    namespace galdenar_domain {
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

    GaldenarController --> GaldenarRegistreringService
    GaldenarController --> GaldenarSokService
    GaldenarEkonomiController --> GaldenarEkonomiService
    GaldenarEkonomiService --> GaldenarSokService
    GaldenarApi <|.. GaldenarApiService
    GaldenarApiService --> GaldenarSokService
    GaldenarApiService --> GaldenarEkonomiService
    GaldenarRegistreringService --> GaldenarRepository
    GaldenarSokService --> GaldenarRepository
    GaldenarRegistreringService ..> GaldenarMapper
    GaldenarRepository ..> Galdenar
    Galdenar *-- Personnummer
    Galdenar *-- Ekonomi
    Personnummer ..> Luhn
    Ekonomi ..> Hushall : skapar
    GaldenarEkonomiService ..> Forbehallsbelopp
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
    participant UC as BetalningsforelaggandeService
    participant L as ArendeHamtare
    participant BF as Betalningsforelaggande
    participant DB as Databas
    participant N as BrevUtskick
    participant BL as Brevlada

    H->>C: POST /api/arenden/1/delgivning {"datum": "2026-05-10"}
    C->>C: @Valid DelgivningRequest
    C->>UC: delge(1, DelgivningRequest)
    activate UC
    Note over UC,DB: @Transactional startar
    UC->>L: hamta(1, Betalningsforelaggande.class)
    L->>DB: SELECT … FROM arende WHERE id = 1
    DB-->>L: rad med typ = BETALNINGSFORELAGGANDE
    L-->>UC: Betalningsforelaggande
    UC->>BF: delge(request.datum(), idag)
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
    UC-->>C: ArendeResponse (via ArendeMapper)
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
moduls entiteter eller services – bara dess publika API (`GaldenarApi`).

```mermaid
sequenceDiagram
    autonumber
    actor H as Handläggare
    participant UC as UtsokningService<br/>(arende)
    participant UTS as Utsokningsarende<br/>(arende)
    participant API as GaldenarApi<br/>(galdenar, publikt)
    participant GE as GaldenarEkonomiService<br/>(galdenar.service)
    participant E as Ekonomi<br/>(galdenar)
    participant FB as Forbehallsbelopp<br/>(regelverk)

    H->>UC: POST /api/arenden/2/loneutmatning
    UC->>UC: hamta(2) som Utsokningsarende
    UC->>API: forbehallsberakning(galdenarId)
    API->>GE: forbehallsberakning(galdenarId)
    GE->>E: somHushall(idag)
    E-->>GE: Hushall (inkomst, boende, sambo, barnens åldrar)
    GE->>FB: berakna(hushall, Normalbelopp.AR_2026)
    Note right of FB: 34 000 − (9 495 + 5 500)<br/>= 19 005 kr/mån
    FB-->>GE: Forbehallsberakning
    GE-->>API: Forbehallsberakning
    API-->>UC: utmätningsbart belopp = 19 005
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

# Diagram – så hänger allt ihop

Diagrammen är skrivna i [Mermaid](https://mermaid.js.org) och visas direkt på GitHub och i
IntelliJ/VS Code (med Mermaid-tillägg). Läs dem uppifrån och ned: från helheten till detaljerna.

1. [Helhetsbild](#1-helhetsbild)
2. [Tjänster och hur de pratar](#2-tjänster-och-hur-de-pratar)
3. [Lagren i ett anrop](#3-lagren-i-ett-anrop)
4. [Processkedjan: från krav till betald skuld](#4-processkedjan-från-krav-till-betald-skuld)
5. [Statusflöden](#5-statusflöden)
6. [Klassdiagram: ärenden](#6-klassdiagram-ärenden)
7. [Klassdiagram: ärendetjänstens lager](#7-klassdiagram-ärendetjänstens-lager)
8. [Klassdiagram: gäldenärstjänsten och notifieringstjänsten](#8-klassdiagram-gäldenärstjänsten-och-notifieringstjänsten)
9. [Sekvens: delgivning och brev via Kafka](#9-sekvens-delgivning-och-brev-via-kafka)
10. [Sekvens: löneutmätning över tjänstegränsen](#10-sekvens-löneutmätning-över-tjänstegränsen)
11. [Databaser](#11-databaser)

---

## 1. Helhetsbild

Vem använder systemet, och vad finns innanför?

```mermaid
flowchart LR
    H(["👩‍💼 Handläggare"])
    B(["🏢 Borgenär<br/>t.ex. inkassobolag"])
    G(["🧑 Gäldenär"])

    subgraph SYS["Docker Compose"]
        direction TB
        GW["gateway<br/>:8080"]
        GS["galdenar-service"]
        AS["arende-service"]
        NS["notifiering-service"]
        K[["Kafka"]]
        GW --> GS
        GW --> AS
        AS --> GS
        AS --> K --> NS
    end

    LOG["📬 Brev<br/>(loggas)"]
    J["🔍 Jaeger<br/>spårning"]

    H -->|registrerar och handlägger ärenden| GW
    B -.->|ansöker – via handläggaren| H
    NS --> LOG
    LOG -.->|skickas till| G
    SYS -.->|traces| J
```

---

## 2. Tjänster och hur de pratar

Fyra fristående tjänster. Heldragna pilar är **synkrona** HTTP-anrop (svaret behövs direkt).
Den streckade vägen via Kafka är **asynkron**: ärendetjänsten vet inte vem som lyssnar.

```mermaid
flowchart TB
    klient(["Klient"])

    subgraph gateway["gateway :8080"]
        R["Rutter<br/>/api/galdenarer/** → galdenar-service<br/>/api/arenden/** → arende-service"]
    end

    subgraph galdenar["galdenar-service :8081"]
        G_API["REST-API<br/>GET /api/galdenarer/{id}<br/>GET /api/galdenarer/{id}/forbehallsbelopp"]
        G_INT["web · dto · service · domain · regelverk · repository"]
    end

    subgraph arende["arende-service :8082"]
        A_INT["web · dto · service · domain · regelverk · repository"]
        A_KL["integration<br/>Galdenarregister → GaldenarKlient"]
        A_OUT[("event_publication<br/>outbox")]
    end

    subgraph notifiering["notifiering-service :8083"]
        N["BrevUtskick · Brevmall · Brevlada"]
    end

    K[["Kafka<br/>ämne: arende-status<br/>3 partitioner, nyckel = ärende-id"]]
    GDB[("galdenar-db")]
    ADB[("arende-db")]

    klient --> R
    R --> G_API
    R --> A_INT
    A_INT --> A_KL
    A_KL -->|"HTTP + retry"| G_API
    G_API --- G_INT --- GDB
    A_INT --- ADB
    A_INT -->|"samma transaktion"| A_OUT
    A_OUT -. "ArendeStatusAndrad (JSON)" .-> K
    K -. "@KafkaListener" .-> N
```

| Tjänst | Databas | Tar emot | Skickar |
|---|---|---|---|
| `gateway` | – | alla klientanrop | vidare till rätt tjänst |
| `galdenar-service` | `galdenar-db` | REST | – |
| `arende-service` | `arende-db` | REST | REST till gäldenärstjänsten, händelser till Kafka |
| `notifiering-service` | – | händelser från Kafka | brev |

---

## 3. Lagren i ett anrop

Ärende- och gäldenärstjänsten är uppdelade i samma lager. Pilarna är de **enda** tillåtna
beroendena – `LagerArkitekturTest` (ArchUnit) underkänner bygget om någon bryter mot dem.

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
    subgraph integration["integration (bara arende-service)"]
        INT["Galdenarregister<br/>retry, 404 → HittadesInteException"]
    end
    DB[("Databas")]
    EXT(["gäldenärstjänsten"])
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
    SVC --> INT -->|HTTP| EXT
    DOM -. "RegelbrottException" .-> FEL
    FEL -. "409 Conflict" .-> HTTP
```

| Lager | Får använda | Får inte använda |
|---|---|---|
| `web` | `service`, `dto` | `repository`, `domain` |
| `service` | `repository`, `domain`, `dto`, `integration` | `web` |
| `repository` | `domain` | `service`, `web`, `dto` |
| `domain` | `regelverk`, `common` | alla lager ovan |
| `integration` | – | används bara av `service` |
| `regelverk` | inget annat lager | – |
| `dto` | `regelverk` | alla lager |

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

## 7. Klassdiagram: ärendetjänstens lager

En controller och en service per process. Alla services delar på `ArendeHamtare` (hämta/spara via
repository) och `ArendeMapper` (DTO ⇄ domän). Gäldenärstjänsten nås bara via `Galdenarregister`.

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

    namespace integration {
        class Galdenarregister {
            +kontrolleraAttFinns(id)
            +utmatningsbartBelopp(id) BigDecimal
        }
        class GaldenarKlient {
            <<HTTP Service Client>>
            +hamta(id)
            +forbehallsbelopp(id) ForbehallsberakningSvar
        }
    }

    ArendeController --> ArendeRegistreringService
    ArendeController --> ArendeSokService
    ArendeController --> ArendeAtgardService
    BetalningsforelaggandeController --> BetalningsforelaggandeService
    UtsokningController --> UtsokningService
    SkuldsaneringController --> SkuldsaneringService

    ArendeRegistreringService --> ArendeFabrik
    ArendeRegistreringService --> Galdenarregister
    UtsokningService --> Galdenarregister
    Galdenarregister --> GaldenarKlient

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

## 8. Klassdiagram: gäldenärstjänsten och notifieringstjänsten

Två separata tjänster i samma bild. Gäldenärstjänsten har inget Java-API för andra – bara REST.
Notifieringstjänsten har sin egen kopia av `ArendeStatusAndrad` och delar bara JSON-formatet
med ärendetjänsten.

```mermaid
classDiagram
    direction LR

    namespace galdenar_web {
        class GaldenarController
        class GaldenarEkonomiController
    }

    namespace galdenar_service {
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

    namespace galdenar_regelverk {
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
    }

    namespace notifiering_service {
        class BrevUtskick {
            +nar(String json)
        }
        class ArendeStatusAndrad {
            <<record>>
            till : String
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

    BrevUtskick ..> ArendeStatusAndrad : läser JSON till
    BrevUtskick --> Brevmall
    BrevUtskick --> Brevlada
    Brevlada <|.. LoggandeBrevlada
```

---

## 9. Sekvens: delgivning och brev via Kafka

Vad händer när handläggaren registrerar en delgivning? Händelsen sparas i **samma transaktion**
som ärendet (outbox). Först efter commit skickas den till Kafka, och notifieringstjänsten tar
emot den i sin egen takt.

```mermaid
sequenceDiagram
    autonumber
    actor H as Handläggare
    participant GW as gateway
    participant UC as BetalningsforelaggandeService<br/>(arende-service)
    participant BF as Betalningsforelaggande
    participant DB as arende-db
    participant M as Spring Modulith
    participant K as Kafka<br/>arende-status
    participant N as BrevUtskick<br/>(notifiering-service)
    participant BL as Brevlada

    H->>GW: POST /api/arenden/1/delgivning {"datum": "2026-05-10"}
    GW->>UC: vidare till arende-service
    activate UC
    Note over UC,DB: @Transactional startar
    UC->>DB: SELECT … FROM arende WHERE id = 1
    UC->>BF: delge(request.datum(), idag)
    BF->>BF: bytStatus(DELGIVET) + dagbok + registerEvent(ArendeStatusAndrad)
    UC->>DB: UPDATE arende / INSERT arende_dagbok
    UC->>DB: INSERT event_publication (händelsen som JSON)
    Note over UC,DB: COMMIT – ärende och händelse sparas tillsammans
    UC-->>GW: 200 OK + ArendeResponse
    deactivate UC
    GW-->>H: 200 OK

    M-)K: send(nyckel "1", JSON) – efter commit
    M->>DB: event_publication: COMPLETED
    K-)N: @KafkaListener(groupId = "notifiering")
    N->>N: JSON → ArendeStatusAndrad, Brevmall.textFor(...)
    N->>BL: skicka(galdenarId, "Du har fått ett betalningsföreläggande …")
```

Är Kafka nere ligger händelsen kvar i `event_publication` och skickas när ärendetjänsten startar
om. Är notifieringstjänsten nere väntar meddelandet i Kafka tills den är uppe igen.

### Om en regel bryts

```mermaid
sequenceDiagram
    actor H as Handläggare
    participant C as BetalningsforelaggandeController
    participant BF as Betalningsforelaggande
    participant F as GlobalFelhanterare<br/>(common)

    H->>C: POST /api/arenden/1/bestridande {"datum": "2026-05-25"}
    C->>BF: bestrid(…) via användningsfallet
    BF-->>C: RegelbrottException "Bestridandet kom in 2026-05-25, efter fristen 2026-05-20"
    Note over C: transaktionen rullas tillbaka – inget sparas, ingen händelse skickas
    C->>F: undantaget fångas
    F-->>H: 409 Conflict, application/problem+json
```

---

## 10. Sekvens: löneutmätning över tjänstegränsen

Ärendetjänsten frågar gäldenärstjänsten över HTTP. Service-lagret vet inte om det – det anropar
bara `Galdenarregister`. Nätverket, retry och felöversättning ligger i `integration`.

```mermaid
sequenceDiagram
    autonumber
    actor H as Handläggare
    participant UC as UtsokningService<br/>(arende-service)
    participant UTS as Utsokningsarende
    participant REG as Galdenarregister<br/>(integration)
    participant KL as GaldenarKlient<br/>(HTTP Service Client)
    participant API as GaldenarEkonomiController<br/>(galdenar-service)
    participant GE as GaldenarEkonomiService
    participant FB as Forbehallsbelopp<br/>(regelverk)

    H->>UC: POST /api/arenden/2/loneutmatning
    UC->>UC: hamta(2) som Utsokningsarende
    UC->>REG: utmatningsbartBelopp(galdenarId)
    REG->>KL: forbehallsbelopp(galdenarId)
    KL->>API: GET /api/galdenarer/2/forbehallsbelopp
    API->>GE: forbehallsberakning(2)
    GE->>FB: berakna(hushall, Normalbelopp.AR_2026)
    Note right of FB: 34 000 − (9 495 + 5 500)<br/>= 19 005 kr/mån
    FB-->>GE: Forbehallsberakning
    GE-->>API: Forbehallsberakning
    API-->>KL: 200 OK + JSON
    KL-->>REG: ForbehallsberakningSvar
    REG-->>UC: 19 005
    UC->>UTS: beslutaLoneutmatning(19 005, idag)
    UTS->>UTS: kontrolleraUtmatningsbart() + bytStatus(LONEUTMATNING)
    UC-->>H: 200 OK, status LONEUTMATNING
```

### Om gäldenärstjänsten inte svarar

```mermaid
sequenceDiagram
    participant UC as UtsokningService
    participant REG as Galdenarregister
    participant G as galdenar-service
    participant F as GlobalFelhanterare

    UC->>REG: utmatningsbartBelopp(2)
    REG-xG: försök 1 – ResourceAccessException
    Note over REG: vänta 200 ms (@Retryable)
    REG-xG: försök 2
    Note over REG: vänta 400 ms
    REG-xG: försök 3
    REG-->>F: ResourceAccessException
    F-->>UC: 503 Service Unavailable, "En tjänst svarar inte"
    Note over UC: inget sparas – handläggaren kan försöka igen
```

Svarar gäldenärstjänsten **404** görs inget nytt försök. `Galdenarregister` gör om det till
`HittadesInteException`, och ärendetjänsten svarar också 404.

---

## 11. Databaser

Varje tjänst har en **egen databas** med egna Flyway-migreringar (`db/migration/galdenar` och
`db/migration/arende`). Ärendet har bara gäldenärens id – ingen foreign key, eftersom gäldenären
finns i en annan databas. Alla tre ärendetyper ligger i **samma tabell** `arende`; kolumnen `typ`
avgör vilken Java-klass raden blir (JPA `SINGLE_TABLE`).

`event_publication` är outbox-tabellen. Spring Modulith skapar den och sparar varje händelse där
innan den skickas till Kafka.

```mermaid
erDiagram
    GALDENAR ||--o{ GALDENAR_BARN : "har barn"
    GALDENAR ||..o{ ARENDE : "galdenar_id – annan databas"
    ARENDE ||--o{ ARENDE_DAGBOK : "dagbok"
    ARENDE ||--o{ ARENDE_INBETALNING : "inbetalningar (utsökning)"
    ARENDE |o--o{ ARENDE : "ursprungsarende_id (utslag → utsökning)"
    ARENDE ||..o{ EVENT_PUBLICATION : "händelser (outbox)"

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
    EVENT_PUBLICATION {
        uuid id PK
        varchar event_type
        varchar serialized_event "JSON"
        timestamp publication_date
        timestamp completion_date "satt när Kafka tagit emot"
        varchar status
    }
```

| Databas | Tabeller |
|---|---|
| `galdenar-db` | `galdenar`, `galdenar_barn` |
| `arende-db` | `arende`, `arende_dagbok`, `arende_inbetalning`, `event_publication` |

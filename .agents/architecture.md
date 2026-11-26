# Arquitectura del Servicio `pubmed-integration`

> **Documento para Agentes de IA y Desarrolladores**  
> **Servicio:** `pubmed-integration` (`artifactId: pubmed-integration`, `groupId: org.biopatternsg`)  
> **Ecosistema:** BioPatternsG Microservices  
> **Framework:** Quarkus 3.31.4 (Java 21)  
> **Estilo de Arquitectura:** Arquitectura Hexagonal (Ports & Adapters) / Clean Architecture

---

## 1. Propósito del Servicio

`pubmed-integration` es el microservicio encargado de la **generación de combinaciones de términos biológicos, recuperación asíncrona de literatura científica desde NCBI PubMed y PubTator3, construcción y consulta de la Base de Conocimiento (KB), y alineación de objetos expertos** dentro del ecosistema **BioPatternsG**. Sus responsabilidades principales incluyen:

1. **Generación de Pares de Búsqueda (`COMBINATIONS`):** Consulta los objetos biológicos por nivel y sus relaciones jerárquicas (padre, hermanos e hijos) en `search-biological-objects` (`POST /pubmed/build-pairs`), genera pares únicos de nombres/sinónimos por nivel (limitados por `pubmed.max-synonyms-per-object`), los persiste en MongoDB (`pairs`) y reporta métricas de finalización a `config-and-control`.
2. **Búsqueda Asíncrona de PubMed IDs (`SEARCH_PUBMED_IDS`):** Lee los pares generados en `pairs`, inicializa el contador de progreso en `search_progress` y encola cada consulta (`firstTerm AND secondTerm`) en RabbitMQ (`ncbi-out` / `ncbi-pubmed-queue`) (`POST /pubmed/search-pubmed-ids-by-pairs`). El consumidor `NcbiRabbitConsumer` (`ncbi-in`) consulta **NCBI ESearch** (`esearch.fcgi`) con control de tasa (`SimpleRateLimiter`), guarda los PMIDs en `pubmed_results` y, al completar todos los pares, limpia `pairs` y `search_progress` y notifica el paso a `config-and-control`.
3. **Anotación Asíncrona con PubTator3 (`SEARCH_PUBTATOR`):** Lee los PMIDs numéricos únicos de `pubmed_results`, los agrupa en lotes (`pubtator.batch-size`) y los publica en RabbitMQ (`pubtator-out` / `pubtator-queue`) (`POST /pubmed/search-pubtator-by-pmids`). El consumidor `PubtatorRabbitConsumer` (`pubtator-in`) filtra PMIDs ya cacheados en `pubtator_results`, consulta **PubTator3 API** (`publications/export/biocjson`) con rate limiting, parsea los documentos BioC JSON con eventos y los persiste en `pubtator_results`, reportando métricas finales a `config-and-control`.
4. **Construcción de la Base de Conocimiento (`BUILD_KNOWLEDGE_BASE`):** Procesa en paralelo los PMIDs del pipeline (`POST /pubmed/generate-kb`), reutilizando el bloque `knowledgeBase` ya cacheado en `pubtator_results` o invocando al microservicio `build-knowledge-base` (`POST /generate-kb`). Persiste relaciones únicas en `kb_events` (con sus `pubmedIds` de soporte) y objetos/sinónimos/biotipos en `kb_objects`, limpia `pubmed_results` del pipeline y actualiza el paso en `config-and-control`.
5. **Alineación de Objetos Expertos (`GENERATE_ALIGNED_OBJECTS`):** Compara la lista de objetos expertos (`ExpertObjectConfig`) contra el diccionario de sinónimos de `kb_objects` (`POST /pubmed/generate-aligned-objects`), clasificando los resultados en `aligned`, `alignedAs` (IDs alternativos), `noAligned` y `alignedAndAlternatives`, los guarda en `alignedResults` y notifica a `config-and-control`.
6. **Consulta y Gestión de KB, Roles, Alineación y Publicaciones:** Expone endpoints REST síncronos para consultar objetos KB y sus sinónimos (paginados, por nombre o completos), actualizar o reiniciar roles sobre `kb_objects`, consultar eventos KB (por pipeline, por término o por nivel de restricción sobre objetos alineados), consultar resultados de alineación y recuperar título/resumen de publicaciones por lista de PMIDs.
7. **Reanudación Autónoma al Arranque:** Al iniciar el servicio, `AppLifecycleBean` programa una llamada asíncrona a `config-and-control` (`POST /config-and-control/pipelines/resume`) para reanudar pipelines pendientes de `PUBMED_INTEGRATION`.

---

## 2. Diagrama Conceptual de Capas (Arquitectura Hexagonal)

```
+---------------------------------------------------------------------------------------------------+
|                                          INFRASTRUCTURE                                           |
|                                                                                                   |
|   [Inbound Adapters & Lifecycle]                                                                  |
|   - REST Controller: PubmedController (@Path("/pubmed"))                                          |
|   - RabbitMQ Consumers: NcbiRabbitConsumer (@Incoming("ncbi-in")),                                |
|                         PubtatorRabbitConsumer (@Incoming("pubtator-in"))                         |
|   - HTTP Context & Startup: ContextInterceptor, SessionUtils, AppLifecycleBean                    |
|                                                |                                                  |
|                                                v (invoca a través de Ports IN)                    |
|   +--------------------------------------------+----------------------------------------------+   |
|   |                                            |                                              |   |
|   |         +----------------------------------v----------------------------------+           |   |
|   |         |                             APPLICATION                             |           |   |
|   |         |                                                                     |           |   |
|   |         |   [Application Services, Utils & Use Cases]                         |           |   |
|   |         |   - Services: BiologicalObjectsService                              |           |   |
|   |         |   - Utils: PairsGenerator                                           |           |   |
|   |         |   - Use Cases (application.usecase):                                |           |   |
|   |         |     Implementan los puertos de entrada (domain.ports.in)            |           |   |
|   |         |                                      |                              |           |   |
|   |         |                                      v (utiliza)                    |           |   |
|   |         |   +----------------------------------v--------------------------+   |           |   |
|   |         |   |                           DOMAIN                            |   |           |   |
|   |         |   |                                                             |   |           |   |
|   |         |   |   - Models (records & enums): BiologicalObject, Pair,       |   |           |   |
|   |         |   |     NcbiSearchRequest/Result, PubtatorSearchRequest/Result, |   |           |   |
|   |         |   |     KbEvent, KbObject, AlignedResult, AlignedResultSummary, |   |           |   |
|   |         |   |     AlignedAs, ExpertObjectConfig, Publication,             |   |           |   |
|   |         |   |     PaginatedResult, PipelineSteps, Status                  |   |           |   |
|   |         |   |   - Utils: LogSanitizer (domain.util)                       |   |           |   |
|   |         |   |   - Ports IN (domain.ports.in)                              |   |           |   |
|   |         |   |   - Ports OUT (repositories, external_repositories,         |   |           |   |
|   |         |   |     producers)                                              |   |           |   |
|   |         |   +----------------------------------^--------------------------+   |           |   |
|   |         |                                      |                              |           |   |
|   |         +--------------------------------------+------------------------------+           |   |
|   |                                                ^                                          |   |
|   |                                                | (implementan Ports OUT)                  |   |
|   +------------------------------------------------+------------------------------------------+   |
|                                                ^                                                  |
|                                                |                                                  |
|   [Outbound Adapters (Persistence, Messaging, Internal & External Services)]                      |
|   - MongoDB Panache (com.biopatternsg.infrastructure.mongo.*): Pair, PubmedResult, SearchProgress,               |
|     PubtatorPmidReader, PubtatorResult, PubtatorSearchProgress, KbEvent, KbObject, AlignedResult  |
|   - RabbitMQ Producers: NcbiQueueSenderRabbitAdapter (@Channel("ncbi-out")),                      |
|                         PubtatorQueueSenderRabbitAdapter (@Channel("pubtator-out"))               |
|   - Internal Microservices: BiologicalObjectsRepositoryAdapter, ConfigAndControlRepositoryAdapter,|
|     BuildKnowledgeBaseAdapter                                                                     |
|   - External APIs: NcbiSearchAdapter (NCBI ESearch), PubtatorSearchAdapter (PubTator3)            |
+---------------------------------------------------------------------------------------------------+
```

---

## 3. Estructura de Paquetes (`com.biopatternsg.*`)

```text
com.biopatternsg/
├── Main.java                                <-- Punto de entrada de la aplicación Quarkus (@QuarkusMain)
├── mongo/                                   <-- Entidades MongoDB Panache (PairsCollection, PubmedResultCollection, SearchProgressCollection, PubtatorResultCollection, PubtatorSearchProgressCollection, KbEventCollection, KbObjectCollection, AlignedResultCollection, AlignedAsEmbedded)
├── domain/                                  <-- Capa de Dominio (Pura, sin dependencias de frameworks)
│   ├── model/                               <-- Java records y enums del dominio
│   │   ├── BiologicalObject / Pair          <-- Objetos biológicos y pares genéricos
│   │   ├── NcbiSearchRequest / Result       <-- Solicitud y resultado de búsqueda en NCBI ESearch
│   │   ├── PubtatorSearchRequest / Result   <-- Solicitud por lotes y documento anotado de PubTator3 (con KnowledgeBase embebido)
│   │   ├── KbEvent / KbObject               <-- Eventos y objetos de la Base de Conocimiento
│   │   ├── AlignedResult / Summary / As     <-- Resultados de alineación con objetos expertos
│   │   ├── ExpertObjectConfig               <-- Configuración de objeto experto para alineación
│   │   ├── Publication / PaginatedResult    <-- Publicaciones (PMID, título, texto) y paginación genérica
│   │   └── PipelineSteps / Status           <-- Enums de pasos y estados del pipeline
│   ├── util/                                <-- Utilidades puras de dominio (LogSanitizer)
│   └── ports/
│       ├── in/                              <-- Puertos de Entrada (15 interfaces de Casos de Uso)
│       └── out/                             <-- Puertos de Salida
│           ├── producers/                   <-- Puertos de publicación RabbitMQ (NcbiQueueSender, PubtatorQueueSender)
│           ├── repositories/                <-- Puertos de persistencia MongoDB (PairRepository, PubmedResultRepository, SearchProgressRepository, PubtatorPmidReaderRepository, PubtatorResultRepository, PubtatorSearchProgressRepository, KbEventRepository, KbObjectRepository, AlignedResultRepository)
│           └── external_repositories/       <-- Puertos hacia APIs externas y servicios internos (NcbiSearchRepoWeb, PubtatorSearchRepoWeb, BiologicalObjectsRepository, ConfigAndControlRepository, BuildKnowledgeBaseRepoWeb, GenerateKbResult)
│
├── application/                             <-- Capa de Aplicación
│   ├── services/                            <-- Interfaces de servicios de aplicación (BiologicalObjectsService)
│   │   └── impl/                            <-- Implementaciones (@ApplicationScoped: BiologicalObjectsServiceImpl)
│   ├── util/                                <-- Utilidades de aplicación (PairsGenerator)
│   └── usecase/                             <-- Casos de uso concretos (*UseCase.java)
│
└── infrastructure/                          <-- Capa de Infraestructura (Quarkus, MongoDB, RabbitMQ, HTTP)
    ├── adapters/
    │   ├── dtos/                            <-- DTOs Java records de entrada/salida (BuildPairsRequest, SearchPairsByPipelineRequest, SearchPubtatorByPipelineRequest, GenerateAlignedObjectsRequest, KbEventsByRestrictionRequest, GetPublicationsByPmidsRequest, NameAndSynonymRequest, FatherBrothersAndSonsRequest, PipelineStepRequest, ResumePipelineRequest, pubtator/PubtatorApiResponse)
    │   ├── in/
    │   │   ├── consumers/                   <-- Consumidores RabbitMQ y parser (NcbiRabbitConsumer, PubtatorRabbitConsumer, PubtatorResponseParser)
    │   │   └── restcontrollers/             <-- Controladores REST (*Controller.java: PubmedController)
    │   └── out/
    │       ├── *RepositoryAdapter           <-- Adaptadores MongoDB Panache e internos (Pair, PubmedResult, SearchProgress, PubtatorPmidReader, PubtatorResult, PubtatorSearchProgress, KbEvent, KbObject, AlignedResult, BiologicalObjects, ConfigAndControl)
    │       ├── external_repositories/       <-- Adaptadores hacia APIs externas y build-knowledge-base (NcbiSearchAdapter, PubtatorSearchAdapter, BuildKnowledgeBaseAdapter)
    │       └── producers/                   <-- Adaptadores productores RabbitMQ (NcbiQueueSenderRabbitAdapter, PubtatorQueueSenderRabbitAdapter)
    ├── boot_functions/                      <-- Tareas de arranque (AppLifecycleBean)
    ├── clients/
    │   ├── dtos/                            <-- DTOs de clientes HTTP (NcbiESearchResponse, NcbiESearchResultDto, buildknowledgebase/*)
    │   ├── external/                        <-- Clientes HTTP externos MicroProfile RestClient (NcbiESearchClient, PubtatorClient)
    │   └── internal/                        <-- Clientes HTTP internos MicroProfile RestClient (BiologicalObjectsHttpClient, ConfigAndControlHttpClient, BuildKnowledgeBaseHttpClient)
    ├── config/interceptors/                 <-- Filtros JAX-RS (ContextInterceptor)
    ├── external_services/                   <-- Wrappers con rate limiting para APIs externas (QueryNcbiESearch, QueryPubtator, impl/*)
    ├── internal_services/                   <-- Wrappers de servicios internos (QueryBiologicalObjects, QueryConfigAndControl, impl/*)
    ├── mapper/                              <-- Mappers entre capas (PubtatorResultMapper)
    ├── session/                             <-- Contexto de sesión por petición (SessionUtils)
    └── util/                                <-- Utilidades de infraestructura (SimpleRateLimiter)
```

---

## 4. Reglas Arquitectónicas Estrictas (Enforced by ArchUnit)

Cualquier cambio de código realizado por un agente **DEBE** respetar las reglas validadas en `src/test/java/archtest/ArchitectureLayerTest.java`:

| Regla | Descripción |
|---|---|
| **Aislamiento de Dominio (`domainLayerIsolationTest`)** | `com.biopatternsg.domain..` **NO** puede depender de `application..` ni de `infrastructure..`. |
| **Aislamiento de Aplicación (`applicationLayerIsolationTest`)** | `com.biopatternsg.application..` **NO** puede depender de `infrastructure..`. |
| **Encapsulamiento de Aplicación (`layerApplicationTest`)** | `com.biopatternsg.application..` solo puede ser accedida por clases de la misma capa Application (la comunicación desde Infrastructure se realiza exclusivamente mediante interfaces en `domain.ports.in`). |
| **Acceso a Casos de Uso (`layerApplicationUsesCasesTest`)** | Las implementaciones en `application.usecase..` no pueden ser accedidas por ninguna capa; se interactúa exclusivamente a través de sus puertos `domain.ports.in`. |
| **Nomenclatura UseCase (`useCasesNamingConventionTest`)** | Toda clase en `application.usecase..` debe terminar en `UseCase` (ej. `BuildPubmedPairsUseCase`, `GenerateKbForPipelineUseCase`). |
| **Nomenclatura Controller (`controllersNamingConventionTest`)** | Toda clase en `infrastructure.adapters.in.restcontrollers..` debe terminar en `Controller` (ej. `PubmedController`). |
| **Nomenclatura Adapter (`adaptersNamingConventionTest`)** | Toda clase en `infrastructure.adapters.out..` debe terminar en `Adapter` (ej. `KbEventRepositoryAdapter`, `NcbiSearchAdapter`, `BuildKnowledgeBaseAdapter`, `NcbiQueueSenderRabbitAdapter`). |

---

## 5. Casos de Uso Principales

Ubicación: `com.biopatternsg.application.usecase.*` (Catálogo e índice general disponible en [`.agents/use_cases/README.md`](use_cases/README.md)):

| Caso de Uso | Puerto In (`domain.ports.in`) | Canal de Entrada (`PubmedController`) | Responsabilidad | Documento |
|---|---|---|---|---|
| `BuildPubmedPairsUseCase` | `BuildPubmedPairs` | `POST /pubmed/build-pairs` (asíncrono `202`) | Consulta objetos biológicos por nivel en `search-biological-objects`, construye pares únicos de términos/sinónimos, guarda en `pairs` y notifica `COMBINATIONS`. | [`BuildPubmedPairsUseCase.md`](use_cases/BuildPubmedPairsUseCase.md) |
| `SearchPubmedByPairsUseCase` | `SearchPubmedByPairs` | `POST /pubmed/search-pubmed-ids-by-pairs` (asíncrono `202`) | Lee pares del pipeline, inicializa `search_progress` y encola consultas `firstTerm AND secondTerm` en RabbitMQ (`ncbi-out`). | [`SearchPubmedByPairsUseCase.md`](use_cases/SearchPubmedByPairsUseCase.md) |
| `SearchPubtatorByPmidsUseCase` | `SearchPubtatorByPmids` | `POST /pubmed/search-pubtator-by-pmids` (asíncrono `202`) | Filtra PMIDs numéricos del pipeline, particiona en lotes (`pubtator.batch-size`), inicializa `pubtator_search_progress` y encola en RabbitMQ (`pubtator-out`). | [`SearchPubtatorByPmidsUseCase.md`](use_cases/SearchPubtatorByPmidsUseCase.md) |
| `GenerateKbForPipelineUseCase` | `GenerateKbForPipeline` | `POST /pubmed/generate-kb` (asíncrono `202`) | Procesa en paralelo los PMIDs del pipeline, resuelve la KB (caché en `pubtator_results` o `build-knowledge-base`), persiste `kb_events` y `kb_objects`, limpia `pubmed_results` y notifica `BUILD_KNOWLEDGE_BASE`. | [`GenerateKbForPipelineUseCase.md`](use_cases/GenerateKbForPipelineUseCase.md) |
| `GenerateAlignedObjectsUseCase` | `GenerateAlignedObjects` | `POST /pubmed/generate-aligned-objects` (asíncrono `202`) | Alinea objetos expertos contra el diccionario de sinónimos de `kb_objects`, persiste el resultado en `alignedResults` y notifica `GENERATE_ALIGNED_OBJECTS`. | [`GenerateAlignedObjectsUseCase.md`](use_cases/GenerateAlignedObjectsUseCase.md) |
| `GetPaginatedKbObjectsUseCase` | `GetPaginatedKbObjects` | `GET /pubmed/kb-objects/{pipelineId}` y `GET /pubmed/synonyms/{pipelineId}` | Retorna los objetos KB del pipeline de forma paginada (`page`, `size`). | [`GetPaginatedKbObjectsUseCase.md`](use_cases/GetPaginatedKbObjectsUseCase.md) |
| `GetKbObjectByNameUseCase` | `GetKbObjectByName` | `GET /pubmed/kb-objects/{pipelineId}/by-name/{name}` y `GET /pubmed/synonyms/{pipelineId}/by-name/{name}` | Busca un objeto KB específico por `pipelineId` y `name`. | [`GetKbObjectByNameUseCase.md`](use_cases/GetKbObjectByNameUseCase.md) |
| `GetAllKbObjectsByPipelineUseCase` | `GetAllKbObjectsByPipeline` | `GET /pubmed/kb-objects/{pipelineId}/all` | Retorna la lista completa de objetos KB de un pipeline. | [`GetAllKbObjectsByPipelineUseCase.md`](use_cases/GetAllKbObjectsByPipelineUseCase.md) |
| `UpdateKbObjectRolesUseCase` | `UpdateKbObjectRoles` | `PATCH /pubmed/kb-objects/{pipelineId}/roles` | Actualiza los roles asignados a los objetos en `kb_objects` para un pipeline. | [`UpdateKbObjectRolesUseCase.md`](use_cases/UpdateKbObjectRolesUseCase.md) |
| `ResetKbObjectRolesUseCase` | `ResetKbObjectRoles` | `DELETE /pubmed/kb-objects/{pipelineId}/roles` | Reinicia los roles de todos los objetos en `kb_objects` de un pipeline. | [`ResetKbObjectRolesUseCase.md`](use_cases/ResetKbObjectRolesUseCase.md) |
| `GetKbEventsByTermUseCase` | `GetKbEventsByTerm` | `GET /pubmed/kb-events/{pipelineId}/by-term/{term}` | Obtiene los eventos de la KB de un pipeline asociados a un término dado. | [`GetKbEventsByTermUseCase.md`](use_cases/GetKbEventsByTermUseCase.md) |
| `GetKbEventsByPipelineUseCase` | `GetKbEventsByPipeline` | `GET /pubmed/kb-events/{pipelineId}` | Obtiene todos los eventos de la KB pertenecientes a un pipeline. | [`GetKbEventsByPipelineUseCase.md`](use_cases/GetKbEventsByPipelineUseCase.md) |
| `GetKbEventsByRestrictionUseCase` | `GetKbEventsByRestriction` | `POST /pubmed/kb-events/{pipelineId}/by-restriction` | Filtra eventos de la KB de un pipeline según `restrictionLevel` y la lista de `alignedObjects`. | [`GetKbEventsByRestrictionUseCase.md`](use_cases/GetKbEventsByRestrictionUseCase.md) |
| `GetAlignedResultsUseCase` | `GetAlignedResults` | `GET /pubmed/aligned-results/{pipelineId}` | Obtiene el resumen de alineación (`AlignedResultSummary`) para un pipeline. | [`GetAlignedResultsUseCase.md`](use_cases/GetAlignedResultsUseCase.md) |
| `GetPublicationsByPmidsUseCase` | `GetPublicationsByPmids` | `POST /pubmed/publications/by-pmids` | Recupera título y texto de publicaciones desde `pubtator_results` a partir de una lista deduplicada de PMIDs. | [`GetPublicationsByPmidsUseCase.md`](use_cases/GetPublicationsByPmidsUseCase.md) |
| `ProcessNcbiSearchRequestUseCase` | `ProcessNcbiSearchRequest` | RabbitMQ (`@Incoming("ncbi-in")`) | Consulta NCBI ESearch para un par de términos, guarda PMIDs en `pubmed_results`, actualiza `search_progress`, limpia `pairs` y notifica `SEARCH_PUBMED_IDS`. | [`ProcessNcbiSearchRequestUseCase.md`](use_cases/ProcessNcbiSearchRequestUseCase.md) |
| `ProcessPubtatorBatchUseCase` | `ProcessPubtatorBatch` | RabbitMQ (`@Incoming("pubtator-in")`) | Filtra PMIDs no cacheados, obtiene anotaciones de PubTator3, persiste documentos con eventos en `pubtator_results`, actualiza `pubtator_search_progress` y notifica `SEARCH_PUBTATOR`. | [`ProcessPubtatorBatchUseCase.md`](use_cases/ProcessPubtatorBatchUseCase.md) |

> **Nota sobre flujos asíncronos en consumidores RabbitMQ:** Los pasos de cierre de `SEARCH_PUBMED_IDS` (`NcbiRabbitConsumer`) y `SEARCH_PUBTATOR` (`PubtatorRabbitConsumer`) procesan los mensajes encolados por `SearchPubmedByPairsUseCase` y `SearchPubtatorByPmidsUseCase`, actualizan progreso atómico en MongoDB y notifican a `config-and-control` al completarse todos los mensajes del pipeline.

---

## 6. Persistencia y Mensajería

### 6.1. Persistencia (MongoDB Panache)
- **Motor:** MongoDB (base de datos por defecto: `pubmed-integration`).
- **Acceso a Datos:** Quarkus MongoDB con Panache (`PanacheMongoRepository` / `PanacheMongoEntity`).
- **Colecciones Principales (`com.biopatternsg.infrastructure.mongo.*`):**
    - `PairsCollection` (`pairs`): Almacena los pares de términos (`firstTerm`, `secondTerm`) generados por `pipelineId`. Se elimina al completar la búsqueda en PubMed.
    - `PubmedResultCollection` (`pubmed_results`): Almacena los `pubmedId` recuperados de NCBI ESearch por `pipelineId`. Se elimina al completar la generación de la KB.
    - `SearchProgressCollection` (`search_progress`): Rastrea `completedCount`, `totalCount` y `userId` para la fase `SEARCH_PUBMED_IDS`.
    - `PubtatorResultCollection` (`pubtator_results`): Caché global por `pmid` de publicaciones anotadas por PubTator3 (`title`, `text`, `objects`, `events` y el subdocumento `knowledgeBase` cacheado).
    - `PubtatorSearchProgressCollection` (`pubtator_search_progress`): Rastrea `completedCount`, `totalCount` y `userId` para los lotes de la fase `SEARCH_PUBTATOR`.
    - `KbEventCollection` (`kb_events`): Almacena relaciones biológicas únicas (`pipelineId`, `first`, `relation`, `second`) con su lista de `pubmedIds` de soporte (índice único compuesto sobre `{pipelineId, first, relation, second}`).
    - `KbObjectCollection` (`kb_objects`): Almacena objetos de la KB por `pipelineId` (`name`, `synonyms`, `biotypes`, `roles`).
    - `AlignedResultCollection` (`alignedResults`): Almacena el resultado de alineación por `pipelineId` (`aligned`, `noAligned`, `alignedAs` usando `AlignedAsEmbedded`, `alignedAndAlternatives`).

### 6.2. Mensajería Asíncrona (RabbitMQ)
- **Conector:** SmallRye Reactive Messaging RabbitMQ (`quarkus-messaging-rabbitmq`).
- **Topología (`src/main/resources/application.properties`):**
    1. **Pipeline NCBI ESearch:**
        - **Exchange:** `exchange-ncbi-pubmed`
        - **Cola / Routing Key:** `ncbi-pubmed-queue` (`x-queue-mode=lazy`, `max-outstanding-messages=2`)
        - **Productor (`ncbi-out`):** `NcbiQueueSenderRabbitAdapter` publica mensajes `NcbiSearchRequest` (`OnOverflow.Strategy.UNBOUNDED_BUFFER`).
        - **Consumidor (`ncbi-in`):** `NcbiRabbitConsumer` (`@Blocking(ordered = false)`) consulta NCBI ESearch, guarda en `pubmed_results` y reporta cierre de `SEARCH_PUBMED_IDS`.
    2. **Pipeline PubTator3:**
        - **Exchange:** `exchange-pubtator`
        - **Cola / Routing Key:** `pubtator-queue` (`x-queue-mode=lazy`, `max-outstanding-messages=3`)
        - **Productor (`pubtator-out`):** `PubtatorQueueSenderRabbitAdapter` publica lotes `PubtatorSearchRequest` (`OnOverflow.Strategy.UNBOUNDED_BUFFER`).
        - **Consumidor (`pubtator-in`):** `PubtatorRabbitConsumer` (`@Blocking(ordered = false)`) consulta PubTator3 para PMIDs no cacheados, parsea con `PubtatorResponseParser`, guarda en `pubtator_results` y reporta cierre de `SEARCH_PUBTATOR`.

---

## 7. Integraciones Internas y Externas (MicroProfile REST Clients)

Configuradas en `src/main/resources/application.properties`:

### Servicios Internos del Ecosistema
- **`search-biological-objects` (`biological-objects-api`):** Consulta objetos biológicos por nivel (`POST /biological-object/name-and-synonyms`), relaciones jerárquicas (`POST /biological-object/father-brothers-and-sons`) y objetos expertos (`POST /biological-object/get-expert-objects`). (`BiologicalObjectsHttpClient` -> `QueryBiologicalObjects` -> `BiologicalObjectsRepositoryAdapter`).
- **`config-and-control` (`config-and-control-api`):** Actualiza el estado y métricas de cada paso del pipeline (`PATCH /config-and-control/pipelines/update-step`, con header `x-user-id`) (`ConfigAndControlHttpClient` -> `QueryConfigAndControl` -> `ConfigAndControlRepositoryAdapter`) y solicita la reanudación de pipelines al arranque (`POST /config-and-control/pipelines/resume` desde `AppLifecycleBean`).
- **`build-knowledge-base` (`build-knowledge-base-api`):** Genera eventos, sinónimos y biotipos de la KB a partir de documentos PubTator3 (`POST /generate-kb`) (`BuildKnowledgeBaseHttpClient` -> `BuildKnowledgeBaseAdapter`).

### APIs Biológicas Externas
- **NCBI E-utilities ESearch (`ncbi-esearch-api`, `https://eutils.ncbi.nlm.nih.gov/entrez/eutils`):** Ejecuta búsquedas en PubMed vía `POST /esearch.fcgi` (`db=pubmed`, `retmode=json`, `sort=relevance`) con control de tasa mediante `SimpleRateLimiter` (`ncbi.rate-limit.with-key` / `ncbi.rate-limit.without-key`) (`NcbiESearchClient` -> `QueryNcbiESearch` -> `NcbiSearchAdapter`).
- **PubTator3 API (`pubtator-api`, `https://www.ncbi.nlm.nih.gov/research/pubtator3-api`):** Exporta publicaciones anotadas en BioC JSON vía `GET /publications/export/biocjson?pmids=...` en lotes (`pubtator.batch-size`) con control de tasa mediante `SimpleRateLimiter` (`pubtator.rate-limit`) (`PubtatorClient` -> `QueryPubtator` -> `PubtatorSearchAdapter`).

---

## 8. Guía de Intervención y Estándares de Código

Para el conjunto completo de reglas mandatorias de arquitectura y estándares de lenguaje Java, consulte los documentos:
- 👉 [**`.agents/architecture-rules.md`**](architecture-rules.md) — Reglas de Arquitectura Hexagonal, límites de capas, casos de uso, servicios y adaptadores REST/RabbitMQ.
- 👉 [**`.agents/java-rules.md`**](java-rules.md) — Reglas de Java 21, Clean Code, lambdas, límite de parámetros, sanitización de logs y mappers estáticos.

### Resumen Rápido para Nuevas Funcionalidades:
1. **Nuevo Caso de Uso:**
    1. Crear la interfaz del puerto de entrada en `domain/ports/in/MiOperacion.java`.
    2. Implementar la clase en `application/usecase/MiOperacionUseCase.java` anotada con `@ApplicationScoped`.
    3. Documentar el caso de uso en `.agents/use_cases/MiOperacionUseCase.md` (en inglés) y actualizar `.agents/use_cases/README.md` y la tabla de la sección 5 de `.agents/architecture.md`.
    4. Exponerlo en el controlador REST (`infrastructure/adapters/in/restcontrollers/`) o consumidor (`infrastructure/adapters/in/consumers/`) inyectando únicamente la interfaz `MiOperacion`.
2. **Nueva Integración / Repositorio / Productor:**
    1. Crear la interfaz del puerto de salida en `domain/ports/out/repositories/`, `domain/ports/out/external_repositories/` o `domain/ports/out/producers/`.
    2. Implementar el adaptador correspondiente bajo `infrastructure/adapters/out/` terminando en `Adapter`.
3. **Verificación Obligatoria:**
    - Ejecutar `./mvnw clean compile` para asegurar compilación exitosa y actualizar `target/classes`.
    - Ejecutar `./mvnw test -Dtest=ArchitectureLayerTest,DocumentationGovernanceTest` para comprobar que las reglas de arquitectura y gobernanza documental no se hayan violado.
    - Ejecutar `./mvnw test` para validar la suite completa de pruebas unitarias.

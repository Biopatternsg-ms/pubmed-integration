# Pull Request: Add GetPublicationsByPmids Use Case and REST Endpoint

## 📝 Descripción
Este PR introduce el caso de uso `GetPublicationsByPmids` junto con su endpoint REST asociado (`POST /publications/by-pmids`) en el microservicio `pubmed-integration`. Esta funcionalidad permite consultar y recuperar la información esencial de publicaciones científicas (PMID, título y texto/abstract) a partir de un listado de identificadores PMID previamente procesados y almacenados en la base de datos (MongoDB / PubTator).

## 🛠️ Cambios Realizados

### 1. Capa de Dominio (Domain)
* **Modelo**: Creado el record `Publication` (`pmid`, `title`, `text`) que encapsula la representación de dominio de una publicación científica.
* **Puerto de Entrada (Inbound Port)**: Definida la interfaz `GetPublicationsByPmids` con el método `List<Publication> execute(List<String> pmids)`.
* **Puerto de Salida (Outbound Repository)**: Agregado el método `List<PubtatorResult> findByPmids(List<String> pmids)` a la interfaz `PubtatorResultRepository`.

### 2. Capa de Aplicación (Application)
* **Caso de Uso**: Creado `GetPublicationsByPmidsUseCase`:
  * Limpieza, filtrado de nulos/en blanco y deduplicación de PMIDs de entrada.
  * Consulta en lote al repositorio `PubtatorResultRepository`.
  * Mapeo de `PubtatorResult` al modelo de dominio `Publication`.

### 3. Capa de Infraestructura (Infrastructure & Adapters)
* **Controlador REST**:
  * Agregado el endpoint `POST /publications/by-pmids` en `PubmedController`.
  * Creado el DTO de entrada `GetPublicationsByPmidsRequest` con validación `@NotEmpty` para la lista de PMIDs.
* **Adaptador de Persistencia**:
  * Implementado `findByPmids` en `PubtatorResultRepositoryAdapter` utilizando consultas de MongoDB con el operador `$in` (`{ 'pmid': { '$in': ?1 } }`).
  * Optimizado el método existente `findExistingPmids` utilizando la misma sintaxis `$in`.
* **Configuración**:
  * Definidos valores por defecto para variables de entorno en `application.properties` (`DB_NAME`, `NCBI_EMAIL`, `NCBI_API_KEY`).

### 4. Pruebas Unitarias (Tests)
* **`GetPublicationsByPmidsUseCaseTest`**:
  * Prueba de recuperación y mapeo exitoso de publicaciones con deduplicación y limpieza de espacios en los PMIDs.
  * Pruebas de casos borde (listas vacías, valores nulos o cadenas con espacios en blanco).

---

## 📌 Endpoint Añadido

| Método | Ruta | Descripción | Payload |
|---|---|---|---|
| `POST` | `/publications/by-pmids` | Obtiene el título y texto de publicaciones por sus PMIDs | `{"pmids": ["10523821", "12345678"]}` |

---

## 🧪 Verificación Realizada
* ✅ Pruebas unitarias ejecutadas para el caso de uso (`GetPublicationsByPmidsUseCaseTest`).
* ✅ Manejo defensivo ante entradas nulas, vacías o PMIDs duplicados.

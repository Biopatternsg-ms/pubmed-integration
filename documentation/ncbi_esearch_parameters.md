# Parámetros de la API NCBI ESearch

Documentación de los parámetros para realizar consultas en el servicio de búsqueda de NCBI.

## URL Base
`https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi`

---

## Tabla de Parámetros

| Parámetro | Requerido | Tipo | Valor por Defecto | Descripción |
| :--- | :---: | :---: | :---: | :--- |
| **`db`** | **Sí** | Texto | - | Base de datos de NCBI a consultar. Ejemplos: `pubmed`, `gene`, `nuccore`, `protein`. |
| **`term`** | **Sí** | Texto | - | Término o query de búsqueda (debe ser codificado para URL; ej: espacios como `+` o `%20`). Soporta operadores lógicos (AND, OR, NOT) y campos específicos (ej. `cancer[title]`). |
| **`retmode`** | No | Texto | `xml` | Formato de la respuesta del servidor. Valores soportados: `xml` o `json`. |
| **`retmax`** | No | Entero | `20` | Cantidad máxima de UIDs (identificadores únicos) a retornar. El máximo permitido es `10000`. |
| **`retstart`** | No | Entero | `0` | Índice inicial de los resultados (útil para paginación). |
| **`usehistory`** | No | Texto | `n` | Si se establece en `y`, los resultados se almacenan en el servidor de historial de NCBI. Devuelve un `WebEnv` y `query_key` para recuperaciones posteriores con `efetch`. |
| **`sort`** | No | Texto | *Variable* | Criterio de ordenamiento. Los valores válidos dependen de la base de datos (para `pubmed`: `relevance`, `pub+date`, `most+recent`, `title`, `journal`). |
| **`api_key`** | No | Texto | - | Clave de API provista por NCBI. Aumenta el límite de solicitudes permitidas por segundo de 3 a 10. Configurable en `application.properties` mediante la propiedad `ncbi.esearch.api-key=${NCBI_API_KEY:}`. |
| **`tool`** | No | Texto | - | Nombre de la aplicación cliente que realiza la petición. Ayuda a NCBI a monitorear y depurar el tráfico. |
| **`email`** | No | Texto | - | Correo electrónico de contacto del responsable del cliente. Requerido por NCBI. |

---

## Opciones de Ordenamiento (`sort`) para PubMed

- **`relevance`**: Ordena los resultados por el grado de coincidencia (algoritmo "Best Match" de NCBI).
- **`pub+date`**: Ordena cronológicamente por la fecha de publicación (más reciente primero).
- **`most+recent`**: Ordena cronológicamente por la fecha en la que el artículo fue indexado/añadido en PubMed (más reciente primero).
- **`title`**: Ordena alfabéticamente por el título del artículo.
- **`journal`**: Ordena alfabéticamente por el nombre de la revista.
- **`first+author`**: Ordena alfabéticamente por el primer autor del artículo.

---

## Buenas Prácticas y Límites de Tasa (Rate Limiting)

1. **Límite de solicitudes**: 
   - Sin `api_key`: Máximo **3 peticiones por segundo**.
   - Con `api_key`: Máximo **10 peticiones por segundo**.
2. **Peticiones masivas**: Siempre proveer los parámetros `tool` y `email` para identificarse y evitar bloqueos por parte de los administradores de NCBI.
3. **Uso de Historial**: Para recuperar miles de IDs de manera eficiente, use `usehistory=y` y combine las herramientas `esearch` y `efetch` utilizando el entorno web devuelto (`WebEnv`).

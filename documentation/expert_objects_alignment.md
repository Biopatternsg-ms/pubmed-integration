# Alineación de Expert Objects (Objetos del Experto)

Este documento detalla el funcionamiento del servicio de alineación de **Expert Objects** (objetos biológicos sin información de factor de transcripción) en base a los sinónimos persistidos por pipeline en el microservicio `pubmed-integration`.

La lógica implementada replica fielmente el comportamiento de la función `print_aligned_objs` del script legacy `kb_generator.py` para generar reportes estructurados de alineación.

---

## Conceptos y Categorías de Alineación

El proceso compara la lista de **expert objects** (obtenida con `level = 2` por defecto) contra el mapa de **sinónimos** registrados para un `pipelineId` específico, distribuyendo los objetos en las siguientes cuatro categorías principales:

| Categoría | Descripción | Lógica de Negocio |
| :--- | :--- | :--- |
| **`aligned`** | Objetos con coincidencia directa | El nombre del *expert object* (en mayúsculas) existe directamente como una **llave (main ID)** en el diccionario de sinónimos. |
| **`aligned_as`** | Coincidencias alternativas | El nombre del *expert object* (en mayúsculas) aparece dentro de la lista de **valores (sinónimos)** de otra llave (main ID). Se excluyen los casos en los que la única llave alternativa coincide con el propio nombre del objeto. |
| **`no_aligned`** | Objetos sin coincidencia | Objetos que no aparecen ni como llave directa ni como sinónimo en ninguna de las entradas del diccionario. |
| **`aligned_and_alternatives`** | Consolidación global | La unión del conjunto de objetos de **`aligned`** con todos los identificadores alternativos (valores de `alternativeIds`) mapeados en **`aligned_as`**. |

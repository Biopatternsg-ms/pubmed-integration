# Guía para Obtener una API Key de NCBI

Esta guía describe los pasos necesarios para obtener una clave de API (API Key) personal de NCBI, lo cual permite aumentar el límite de solicitudes de 3 a 10 llamadas por segundo.

---

## Paso a Paso para Obtener la API Key

1. **Acceder a NCBI**:
   - Ingresa al sitio oficial de NCBI: [https://www.ncbi.nlm.nih.gov/](https://www.ncbi.nlm.nih.gov/).

2. **Iniciar Sesión o Registro**:
   - Haz clic en el botón **Sign in to NCBI** en la esquina superior derecha.
   - Si no tienes cuenta, crea una (puedes registrarte de forma rápida enlazando tu cuenta de Google, ORCID o credenciales universitarias/científicas).

3. **Configuración de Cuenta**:
   - Una vez iniciada la sesión, haz clic sobre tu **nombre de usuario** en la esquina superior derecha.
   - En el menú desplegable, selecciona **Account settings** (Configuración de la cuenta).

4. **Generar la Clave**:
   - Desplázate hacia abajo hasta la sección **API Key Management** (Gestión de Claves de API).
   - Haz clic en el botón **Create an API Key** (Crear clave de API).

5. **Guardar la Clave**:
   - Se generará un código alfanumérico único. Cópialo y resguárdalo en un lugar seguro.

---

## Consideraciones Importantes

* **Límite de Claves**: NCBI permite únicamente **una clave de API activa por usuario**. Si generas una nueva clave, la anterior quedará inhabilitada de forma permanente.
* **Beneficio de Tasa de Uso**:
  - **Sin API Key**: Máximo 3 peticiones por segundo.
  - **Con API Key**: Máximo **10 peticiones por segundo**.
* **Integración**: Una vez obtenida la clave, esta se puede inyectar en las peticiones agregando el parámetro query `&api_key=TU_CLAVE` o configurándola en las propiedades del microservicio.

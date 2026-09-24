# Colección Postman — API de Gestión de Pólizas

Colección completa para probar todos los endpoints de la API, incluyendo happy path, reglas de negocio, seguridad y manejo de errores. Cada request trae **tests automáticos** que validan la respuesta.

## Archivos

- `Gestion_Polizas_API.postman_collection.json` — la colección con todas las peticiones.
- `Gestion_Polizas_Local.postman_environment.json` — environment con las variables para entorno local.

## Variables

| Variable | Valor por defecto | Uso |
|----------|-------------------|-----|
| `baseUrl` | `http://localhost:8080` | URL base de la API |
| `apiKey` | `123456` | Valor del header `x-api-key` |
| `polizaIndividualId` | `1` | Id de la póliza individual de ejemplo |
| `polizaColectivaId` | `2` | Id de la póliza colectiva de ejemplo |
| `riesgoCreadoId` | _(se autocompleta)_ | Id del riesgo creado en tiempo de ejecución |

## Cómo usarla en Postman

1. Levanta la API (`./mvnw spring-boot:run`, ver README principal).
2. En Postman: **Import** → arrastra los dos archivos `.json`.
3. Selecciona el environment **"Gestión Pólizas - Local"** (arriba a la derecha).
4. Ejecuta las peticiones una a una, o usa el **Collection Runner** para correr toda la colección de una vez.

## Cómo usarla por línea de comandos (Newman)

Sin instalar nada (usa `npx`):

```bash
npx newman run postman/Gestion_Polizas_API.postman_collection.json \
    -e postman/Gestion_Polizas_Local.postman_environment.json
```

## Contenido de la colección

| Carpeta | Qué prueba |
|---------|------------|
| 1. Consultas (GET) | Listar pólizas (con y sin filtros de tipo/estado), detalle, riesgos |
| 2. Riesgos (COLECTIVA) | Agregar riesgo (201), rechazo en individual (409), validación (400), cancelar riesgo |
| 3. Renovación y cancelación | Renovar (+IPC, RENOVADA), cancelar en cascada, no renovar cancelada (409) |
| 4. Seguridad (API key) | 401 sin api-key y con api-key incorrecta |
| 5. Errores (404) | Póliza y riesgo inexistentes |
| 6. Mock del CORE | `POST /core-mock/evento` |

## Nota sobre los datos

La API usa **H2 en memoria** y algunas pruebas modifican el estado (cancelar/renovar). La colección es **re-ejecutable**: los tests toleran las re-corridas sin marcar fallos. Aun así, para ver el flujo completo (todas las creaciones y cascadas), lo ideal es **reiniciar la aplicación** antes de una corrida completa, de modo que se carguen de nuevo las pólizas de ejemplo frescas.

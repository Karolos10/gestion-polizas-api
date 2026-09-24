# API de Gestión de Pólizas — Seguros Bolívar

API REST para la gestión de pólizas de arrendamiento de inmuebles (Individuales y Colectivas) y sus riesgos, con integración simulada al CORE de seguros.

Corresponde al **Módulo 2 (Prueba Técnica Práctica)** de la prueba técnica para el cargo de Desarrollador TI en Seguros Bolívar. Autor: **Carlos Miguel Rodríguez Botero**.

---

## Especificaciones técnicas

| Componente | Versión / Detalle |
|------------|-------------------|
| Lenguaje | Java 17 |
| Framework | Spring Boot 4.1.1 |
| Módulos Spring | Spring Web, Spring Data JPA, Bean Validation |
| Base de datos | H2 en memoria |
| Utilidades | Lombok |
| Build | Maven (wrapper `mvnw` incluido, no necesitas instalar Maven) |
| Puerto | 8080 |

---

## Requisitos previos

Para ejecutar el proyecto solo necesitas:

- **JDK 17** instalado ([Adoptium/Temurin](https://adoptium.net/) o el de tu preferencia).
- **Git** para clonar el repositorio.

> No necesitas instalar Maven: el proyecto trae el *Maven Wrapper* (`mvnw` / `mvnw.cmd`) que lo descarga automáticamente.

Verifica tu versión de Java:

```bash
java -version
```

Debe mostrar la versión 17. Si tienes varias versiones de Java instaladas, apunta `JAVA_HOME` al JDK 17 (ver notas más abajo).

---

## Cómo clonar y ejecutar

### 1. Clonar el repositorio

```bash
git clone <URL-DEL-REPOSITORIO>
cd polizas
```

### 2. Ejecutar la aplicación

**Windows (PowerShell):**

```powershell
.\mvnw.cmd spring-boot:run
```

**Linux / macOS:**

```bash
./mvnw spring-boot:run
```

La aplicación queda disponible en `http://localhost:8080`.

Al arrancar se cargan automáticamente **2 pólizas de ejemplo** (una INDIVIDUAL con 1 riesgo y una COLECTIVA con 2 riesgos) para poder probar de inmediato.

### 3. Compilar / empaquetar / probar

```bash
# Ejecutar las pruebas
./mvnw test

# Generar el .jar ejecutable en target/
./mvnw clean package

# Ejecutar el jar generado
java -jar target/polizas-0.0.1-SNAPSHOT.jar
```

### Nota si tienes varias versiones de Java

El wrapper de Maven usa la variable `JAVA_HOME`. Si tu Java por defecto no es el 17, apúntalo antes de ejecutar:

**Windows (PowerShell):**

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-17"
.\mvnw.cmd spring-boot:run
```

**Linux / macOS:**

```bash
export JAVA_HOME=/ruta/al/jdk-17
./mvnw spring-boot:run
```

---

## Seguridad (API key)

Todos los endpoints de negocio requieren enviar el header:

```
x-api-key: 123456
```

Sin el header o con un valor incorrecto, la API responde **401 Unauthorized**.
Quedan exentos el mock del CORE (`/core-mock/**`) y la consola H2 (`/h2-console`).

La clave es configurable en `src/main/resources/application.properties` (propiedad `app.api-key`).

---

## Endpoints

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET  | `/polizas?tipo=&estado=` | Lista pólizas; filtros `tipo` y `estado` opcionales |
| GET  | `/polizas/{id}` | Detalle de una póliza |
| GET  | `/polizas/{id}/riesgos` | Riesgos de una póliza |
| POST | `/polizas/{id}/renovar` | Renueva: +IPC en canon y prima, estado → RENOVADA |
| POST | `/polizas/{id}/cancelar` | Cancela la póliza y todos sus riesgos (cascada) |
| POST | `/polizas/{id}/riesgos` | Agrega un riesgo (solo si tipo = COLECTIVA) |
| POST | `/riesgos/{id}/cancelar` | Cancela un riesgo puntual |
| POST | `/core-mock/evento` | Mock del CORE: registra el evento en logs |

`tipo`: `INDIVIDUAL` \| `COLECTIVA`  ·  `estado`: `VIGENTE` \| `RENOVADA` \| `CANCELADA`

### Ejemplos (curl)

```bash
# Listar colectivas vigentes
curl -H "x-api-key: 123456" "http://localhost:8080/polizas?tipo=COLECTIVA&estado=VIGENTE"

# Renovar la póliza 1
curl -X POST -H "x-api-key: 123456" http://localhost:8080/polizas/1/renovar

# Agregar riesgo a póliza colectiva (id 2)
curl -X POST -H "x-api-key: 123456" -H "Content-Type: application/json" \
  -d '{"descripcion":"Bodega 8","arrendatario":"Logistica Norte"}' \
  http://localhost:8080/polizas/2/riesgos

# Cancelar póliza 2 (cascada a sus riesgos)
curl -X POST -H "x-api-key: 123456" http://localhost:8080/polizas/2/cancelar

# Cancelar un riesgo
curl -X POST -H "x-api-key: 123456" http://localhost:8080/riesgos/3/cancelar

# Mock del CORE (no requiere api-key)
curl -X POST -H "Content-Type: application/json" \
  -d '{"evento":"ACTUALIZACION","polizaId":555}' \
  http://localhost:8080/core-mock/evento
```

---

## Reglas de negocio

- Una póliza **individual** solo puede tener **1 riesgo** (se crea con él; no se admite agregar más).
- Solo las pólizas **colectivas** admiten agregar riesgos.
- **No se puede renovar** una póliza cancelada.
- Cancelar una póliza **cancela en cascada** todos sus riesgos.
- La **prima** = canon mensual × meses de vigencia. Al renovar, el canon sube por IPC y la prima se recalcula.
- Toda acción que modifica el estado de pólizas/riesgos notifica al CORE vía `CoreIntegrationService` (queda registrado en logs).

> El IPC usado en la renovación es una constante de ejemplo (`0.125` = 12,5%) definida en `PolizaService`.

---

## Manejo de errores

| Situación | HTTP |
|-----------|------|
| Recurso no encontrado | 404 |
| Violación de regla de negocio | 409 |
| Error de validación de entrada | 400 |
| Falta / api-key inválida | 401 |

---

## Estructura del proyecto

```
com.segurosbolivar.polizas
├── controller    → PolizaController, RiesgoController (capa REST)
├── service       → PolizaService (reglas de negocio)
├── repository    → PolizaRepository, RiesgoRepository (Spring Data JPA)
├── domain        → Poliza, Riesgo + enums (TipoPoliza, EstadoPoliza, EstadoRiesgo)
├── dto           → Requests/Responses (records)
├── core          → CoreIntegrationService + CoreMockController (mock del CORE)
├── security      → ApiKeyFilter (seguridad por header x-api-key)
├── exception     → Manejo global de errores
└── config        → DataSeeder (datos de ejemplo)
```

La carpeta `docs/` contiene las respuestas escritas de los Módulos 1, 3 y 4 de la prueba.

---

## Consola H2

Disponible en `http://localhost:8080/h2-console`

- **JDBC URL:** `jdbc:h2:mem:polizasdb`
- **Usuario:** `sa`
- **Contraseña:** _(vacía)_

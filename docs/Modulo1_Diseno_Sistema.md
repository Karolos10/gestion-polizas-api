# Módulo 1 — Diseño de Sistema

## Plataforma de Gestión de Pólizas de Arrendamiento

---

## 1. Arquitectura de alto nivel

La plataforma se plantea como un conjunto de **microservicios orientados a dominio**, desacoplados mediante un **bus de eventos**, y expuestos al exterior a través de un **API Gateway**. La integración con el CORE legado se aísla detrás de un **adaptador** (patrón anticorrupción) que consume la capa media en WebLogic.

```
                          ┌─────────────────────────┐
        Clientes (Front,  │        API Gateway       │
        móvil, terceros)  │  - Enrutamiento          │
        ───────────────►  │  - AuthN/AuthZ (api-key, │
                          │    OAuth2/JWT)           │
                          │  - Rate limiting         │
                          │  - Versionado (/v1, /v2) │
                          └───────────┬─────────────┘
                                      │ REST/HTTPS
             ┌────────────────────────┼───────────────────────────┐
             ▼                        ▼                            ▼
   ┌──────────────────┐    ┌──────────────────┐        ┌──────────────────────┐
   │ Servicio Pólizas │    │ Servicio Riesgos │        │ Servicio Notificac.  │
   │ - CRUD pólizas   │    │ - Alta/cancelac. │        │ - Email / SMS        │
   │ - Renovación     │    │   de riesgos     │        │ - Suscrito a eventos │
   │ - Reglas negocio │    │ - Validación x   │        └──────────┬───────────┘
   └───────┬──────────┘    │   tipo de póliza │                   │
           │               └───────┬──────────┘                   │
           │                       │                              │
           │   publica/consume     │   publica/consume            │ consume
           ▼                       ▼                              ▼
   ┌───────────────────────────────────────────────────────────────────────┐
   │                    Bus de Eventos (Kafka / RabbitMQ)                    │
   │   Topics: poliza-creada, poliza-renovada, poliza-cancelada,             │
   │           riesgo-agregado, riesgo-cancelado                             │
   └───────────────────────────────┬───────────────────────────────────────┘
                                    │ consume
                                    ▼
                       ┌──────────────────────────┐
                       │  Adapter integración CORE │  ← patrón anticorrupción
                       │  (traduce dominio ⇄ CORE) │
                       └────────────┬─────────────┘
                                    │ SOAP/REST
                                    ▼
                       ┌──────────────────────────┐
                       │ Capa media WebLogic       │
                       │ (servicio agnóstico de    │
                       │  edición) → CORE seguros  │
                       └──────────────────────────┘

   ┌──────────────────┐    ┌──────────────────┐
   │ BD Pólizas       │    │ BD Riesgos       │   (persistencia por servicio)
   │ (PostgreSQL)     │    │ (PostgreSQL)     │
   └──────────────────┘    └──────────────────┘
```

**Flujo típico (renovar una póliza):**
1. El front llama `POST /v1/polizas/{id}/renovar` vía API Gateway.
2. El Gateway valida credenciales/rate limit y enruta al Servicio de Pólizas.
3. El Servicio de Pólizas aplica la regla (no renovar canceladas), recalcula canon + IPC y prima, persiste el nuevo estado.
4. Publica el evento `poliza-renovada` en el bus.
5. El **Adapter de CORE** consume el evento y llama a la capa media WebLogic para mantener sincronizado el CORE.
6. El **Servicio de Notificaciones** consume el mismo evento y envía correo/SMS.

Este desacople permite que la respuesta al usuario no dependa de la latencia del CORE ni del envío de notificaciones.

---

## 2. Patrones de arquitectura seleccionados (3)

### 2.1 Arquitectura Hexagonal (Puertos y Adaptadores)
**Por qué:** el dominio (pólizas, riesgos, reglas de renovación) debe permanecer independiente de la tecnología de entrega (REST) y de la infraestructura (BD, CORE legado). Se definen *puertos* (interfaces) y *adaptadores* concretos. El requisito de consumir un "servicio agnóstico de edición" a través de WebLogic encaja perfectamente como un **adaptador de salida**: si mañana el CORE cambia de SOAP a REST o se reemplaza, solo se toca el adaptador, no la lógica de negocio. Esto también hace el dominio altamente testeable con mocks.

### 2.2 Event-Driven (arquitectura orientada a eventos)
**Por qué:** la creación y renovación deben disparar **notificaciones (correo/SMS)** y **sincronización con el CORE**, que son procesos secundarios y potencialmente lentos. Ejecutarlos de forma síncrona acoplaría la disponibilidad de la API a la del CORE y del proveedor de mensajería. Publicando eventos (`poliza-creada`, `poliza-renovada`, etc.) en un bus (Kafka/RabbitMQ), los consumidores (Notificaciones, Adapter CORE) reaccionan de forma **asíncrona y desacoplada**. Aporta resiliencia (reintentos, colas de reproceso), escalabilidad independiente de cada consumidor y facilita agregar nuevos consumidores sin tocar el productor.

### 2.3 API Gateway
**Por qué:** hay múltiples clientes (front, móvil, terceros) y varios servicios. El Gateway centraliza **seguridad** (validación de api-key/JWT, como la seguridad mínima que pide la prueba), **enrutamiento**, **rate limiting**, **versionado de APIs** y **observabilidad** (punto único de logging/tracing de entrada). Evita que cada microservicio reimplemente estas responsabilidades transversales y expone una superficie única y estable a los consumidores.

> Patrones complementarios que también aplico: **CQRS ligero** (separar consultas de listado —`GET /polizas`— de los comandos de cambio de estado), y **Database per Service** para autonomía de datos.

---

## 3. Modelo de datos principal (conceptual)

**Entidad: Póliza**
- `id`, `tipo` (INDIVIDUAL | COLECTIVA), `estado` (VIGENTE | RENOVADA | CANCELADA)
- `tomador`, `asegurado`, `beneficiario`
- `vigenciaInicio`, `vigenciaFin`, `mesesVigencia`
- `canonMensual`, `prima` (= canonMensual × mesesVigencia)

**Entidad: Riesgo**
- `id`, `descripcion`, `arrendatario`, `estado` (ACTIVO | CANCELADO)
- `polizaId` (FK)

**Relación:** Póliza `1 — N` Riesgo.
- Regla de cardinalidad: si `tipo = INDIVIDUAL`, máximo 1 riesgo; si `tipo = COLECTIVA`, N riesgos.

```
   Poliza (1) ───────< (N) Riesgo
   - INDIVIDUAL: N = 1
   - COLECTIVA:  N >= 1
```

Semántica de negocio de las partes:
- **Individual:** tomador = asegurado = arrendatario; beneficiario = arrendador.
- **Colectiva:** tomador = inmobiliaria/administración; asegurados = arrendatarios; beneficiarios = arrendadores.

Para trazabilidad puede añadirse una entidad **EventoPoliza** (histórico de cambios de estado y de envíos al CORE), útil para auditoría y para reproceso ante fallos.

---

## 4. Atributos de calidad

### Escalabilidad
- **Escalado horizontal** de cada microservicio (contenedores en Kubernetes con HPA por CPU/latencia).
- Servicios **stateless**: la sesión no vive en el proceso, se escala replicando instancias detrás de un balanceador.
- **Consumidores de eventos escalables** de forma independiente (p. ej. más réplicas del Servicio de Notificaciones en campañas de renovación masiva) usando particiones de Kafka / prefetch de RabbitMQ.
- **Separación lectura/escritura (CQRS)**: las consultas (`GET /polizas`) pueden ir a réplicas de lectura o a una vista materializada, evitando contención con las escrituras.

### Logs y observabilidad
- **Logs estructurados** (JSON) con `correlationId`/`traceId` propagado desde el Gateway a través de todos los servicios y eventos.
- **Trazabilidad distribuida** con OpenTelemetry → exportado a Jaeger/Tempo.
- **Métricas** con Micrometer → Prometheus, visualizadas en Grafana (latencia p95/p99, tasa de error, throughput por endpoint, lag de consumo de eventos).
- **Alertas** sobre tasa de errores del Adapter CORE y sobre acumulación en colas (DLQ).
- Centralización de logs en ELK/Loki.

### Tolerancia a fallos
- **Resiliencia en la integración con el CORE** (el punto más frágil): patrones **Retry con backoff**, **Circuit Breaker** y **Timeout** (Resilience4j) en el Adapter.
- **Comunicación asíncrona por eventos**: si el CORE o el proveedor de SMS caen, los mensajes quedan en la cola y se reprocesan; **Dead Letter Queue** para los que fallan repetidamente.
- **Patrón Outbox** para garantizar que "persistir cambio + publicar evento" sea atómico y no se pierdan eventos.
- **Idempotencia** en los consumidores (una renovación reintentada no debe duplicar notificaciones ni dobles envíos al CORE).
- **Health checks** (`/actuator/health`) y readiness/liveness probes en Kubernetes; múltiples réplicas y despliegue multi-AZ para disponibilidad 24/7.

### Versionamiento de APIs
- **Versionado por URI** (`/v1/polizas`, `/v2/polizas`) gestionado en el API Gateway: explícito, fácil de enrutar y de documentar.
- Cambios **retrocompatibles** (agregar campos opcionales) no cambian de versión; solo los *breaking changes* generan `v2`.
- Convivencia temporal de versiones con **política de deprecación** anunciada (headers `Deprecation`/`Sunset`).
- Contrato documentado con **OpenAPI/Swagger** por versión.

---

## 5. Diagrama de componentes principales

```
┌───────────────────────────────────────────────────────────────────────────┐
│                                API GATEWAY                                  │
│         (seguridad api-key/JWT · enrutamiento · rate limit · versión)       │
└───────┬───────────────────────┬────────────────────────────┬───────────────┘
        │                       │                            │
        ▼                       ▼                            ▼
┌───────────────┐      ┌────────────────┐          ┌──────────────────────┐
│  SERVICIO DE  │      │  SERVICIO DE   │          │   SERVICIO DE         │
│    PÓLIZAS    │      │    RIESGOS     │          │   NOTIFICACIONES      │
│               │      │                │          │   (email / SMS)       │
│ CRUD, renovar,│      │ agregar/cancelar│         │  consumidor de eventos│
│ cancelar      │      │ riesgos; valida │         └──────────┬───────────┘
└───┬────────┬──┘      │ tipo de póliza  │                    │
    │        │         └───┬────────┬────┘                    │
    │        │             │        │                         │
    │        ▼             │        ▼                         │
    │  ┌──────────┐        │   ┌──────────┐                   │
    │  │ BD Pólizas│       │   │ BD Riesgos│                  │
    │  │(PostgreSQL)│      │   │(PostgreSQL)│                 │
    │  └──────────┘        │   └──────────┘                   │
    │                      │                                  │
    └──────────┬───────────┴──────────────────────────────────┘
               ▼           (publican/consumen eventos)
    ┌───────────────────────────────────────────────────────┐
    │            BUS DE EVENTOS (Kafka / RabbitMQ)            │
    └───────────────────────────┬───────────────────────────┘
                                 ▼
                   ┌──────────────────────────────┐
                   │  ADAPTER DE INTEGRACIÓN CORE  │
                   │  (anticorrupción; Retry +     │
                   │   Circuit Breaker + Timeout)  │
                   └───────────────┬──────────────┘
                                   ▼
                   ┌──────────────────────────────┐
                   │  Capa media WebLogic (servicio │
                   │  agnóstico de edición)         │
                   │            ↓                   │
                   │      CORE de seguros (legado)  │
                   └──────────────────────────────┘
```

**Responsabilidades por componente:**

| Componente | Responsabilidad |
|------------|-----------------|
| API Gateway | Puerta única: seguridad, enrutamiento, rate limit, versionado |
| Servicio de Pólizas | Ciclo de vida de la póliza: crear, consultar, modificar, renovar, cancelar |
| Servicio de Riesgos | Alta/cancelación de riesgos y validación según el tipo de póliza |
| Servicio de Notificaciones | Envío de correo/SMS ante eventos de creación y renovación |
| Adapter de integración CORE | Traduce el dominio al contrato de la capa media WebLogic y protege el sistema con patrones de resiliencia |
| Base de datos | Persistencia por servicio (Database per Service) |
| Bus de eventos | Desacople asíncrono entre productores y consumidores |

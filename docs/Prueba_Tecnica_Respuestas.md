# Prueba Técnica — Desarrollador TI Seguros Bolívar

**Candidato:** Carlos Miguel Rodríguez Botero
**Cargo:** Desarrollador TI Seguros Bolívar
**Fecha:** Septiembre 2026

---

## Contenido

Este documento consolida las respuestas escritas de los módulos 1, 3 y 4.
El **Módulo 2 (Prueba Técnica Práctica)** se entrega como código fuente en el repositorio público de GitHub (ver enlace más abajo).

1. [Módulo 1 — Diseño de Sistema](#módulo-1--diseño-de-sistema)
2. Módulo 2 — Prueba Técnica Práctica → **Repositorio GitHub:** _[pega aquí el enlace público]_
3. [Módulo 3 — Conocimientos en Bases de Datos](#módulo-3--conocimientos-en-bases-de-datos)
4. [Módulo 4 — Conocimientos en Versionamiento (Git)](#módulo-4--conocimientos-en-versionamiento-git)

---

## Módulo 2 — Resumen de la solución práctica

API REST en **Spring Boot 4.1.1 / Java 17** con arquitectura por capas (controller · service · repository), entidades `Poliza` y `Riesgo`, base de datos H2 en memoria y seguridad por header `x-api-key`.

**Endpoints implementados:**
- `GET /polizas` (filtro por `tipo` y `estado`)
- `GET /polizas/{id}/riesgos`
- `POST /polizas/{id}/renovar` (incrementa canon y prima por IPC, estado → RENOVADA)
- `POST /polizas/{id}/cancelar` (cancela póliza y sus riesgos en cascada)
- `POST /polizas/{id}/riesgos` (solo pólizas COLECTIVAS)
- `POST /riesgos/{id}/cancelar`
- `POST /core-mock/evento` (registra en logs el intento de envío al CORE)

**Reglas de negocio:** póliza individual = 1 riesgo; no se renueva una cancelada; cancelar póliza cancela sus riesgos; agregar riesgo valida el tipo de póliza.

Instrucciones de ejecución completas en el `README.md` del repositorio.

---

> Las respuestas detalladas de cada módulo escrito se encuentran en los documentos:
> - `Modulo1_Diseno_Sistema.md`
> - `Modulo3_Optimizacion_BBDD.md`
> - `Modulo4_Git.md`
>
> Para el PDF final de entrega, este documento y los tres anteriores se combinan en un único archivo
> **`Nombre_Apellido_Prueba_Tecnica.pdf`** (ver instrucciones de generación al final).

---

## Generación del PDF de entrega

Los documentos están en Markdown. Para producir el PDF con el nombre requerido
(`Nombre_Apellido_Prueba_Tecnica.pdf`) puedes usar cualquiera de estas opciones:

**Opción A — VS Code:** instalar la extensión *Markdown PDF* y exportar cada `.md`, o
concatenarlos primero en un solo archivo.

**Opción B — Pandoc (línea de comandos):**

```bash
pandoc docs/Modulo1_Diseno_Sistema.md docs/Modulo2_Resumen.md ^
       docs/Modulo3_Optimizacion_BBDD.md docs/Modulo4_Git.md ^
       -o Nombre_Apellido_Prueba_Tecnica.pdf
```

**Recuerda antes de enviar:**
- El PDF final debe nombrarse `Carlos_Rodriguez_Prueba_Tecnica.pdf`.
- Pegar el **enlace del repositorio público de GitHub** en el Módulo 2.
- Verificar que el PDF no esté protegido con contraseña ni vacío.

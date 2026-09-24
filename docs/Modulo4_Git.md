# Módulo 4 — Conocimientos en Versionamiento (Git y GitHub)

## Escenario

Estoy trabajando en la rama `feature/new-login`. Un compañero fusionó en `main` un **cambio crítico de seguridad**. Necesito incorporar **urgentemente ese commit específico** de `main` en mi rama, **sin traer el resto** de cambios que hay en `main`.

## Comando principal: `git cherry-pick`

`cherry-pick` aplica **un commit puntual** (o un rango) de otra rama sobre la rama actual, sin fusionar toda la historia. Es exactamente lo que pide el escenario.

```bash
# 1. Traer la información más reciente del remoto (para tener el commit disponible localmente)
git fetch origin

# 2. Identificar el hash del commit de seguridad en main
git log origin/main --oneline

# 3. Situarme en mi rama de trabajo
git checkout feature/new-login

# 4. Aplicar únicamente ese commit sobre mi rama
git cherry-pick <hash_del_commit_de_seguridad>
```

### Justificación

- **`git fetch origin`**: actualiza las referencias remotas (`origin/main`) sin modificar mis ramas locales. Necesito que el commit exista en mi repositorio local para poder aplicarlo.
- **`git log origin/main --oneline`**: localizo el hash exacto del commit de seguridad. Es el identificador que necesita `cherry-pick`.
- **`git checkout feature/new-login`**: `cherry-pick` aplica el commit sobre la rama **activa**, por lo que debo estar parado en mi feature.
- **`git cherry-pick <hash>`**: copia **solo** ese commit y crea uno nuevo equivalente encima de mi rama. Así incorporo el fix de seguridad **sin arrastrar** el resto de la historia de `main` (que es justo lo que se busca; un `merge` o `rebase` traería todo).

## Manejo de conflictos

Si el `cherry-pick` genera conflictos:

```bash
# Editar los archivos en conflicto, luego marcarlos como resueltos
git add <archivos_resueltos>
git cherry-pick --continue

# Para abortar y dejar todo como estaba
git cherry-pick --abort
```

## Variantes útiles

```bash
# Aplicar varios commits específicos
git cherry-pick <hash1> <hash2>

# Aplicar un rango de commits (excluyendo el primero .. incluyendo el último)
git cherry-pick <hashInicial>..<hashFinal>

# Aplicar el commit SIN crear commit automáticamente (para revisar/ajustar antes)
git cherry-pick -n <hash>
```

## Publicar el cambio

```bash
git push origin feature/new-login
```

---

## ¿Por qué NO usar `merge` o `rebase` aquí?

- `git merge main` traería **todos** los commits de `main`, no solo el fix de seguridad → contradice el requerimiento.
- `git rebase main` reescribiría mi rama sobre toda la punta de `main`, arrastrando igualmente el resto de cambios.

`cherry-pick` es la herramienta correcta cuando se necesita **un commit concreto** y no la integración completa de otra rama.

## Alternativa según el caso

Si el fix estuviera en **varios commits dispersos** y quisiera empaquetarlos limpiamente, otra opción sería crear una rama a partir del punto adecuado y aplicar los commits necesarios; pero para **un único commit crítico**, `cherry-pick` es la solución más directa y de menor riesgo.

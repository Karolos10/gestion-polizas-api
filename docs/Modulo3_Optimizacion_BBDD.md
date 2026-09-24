# Módulo 3 — Conocimientos en Bases de Datos

## Contexto

Tabla `orders` con **10 millones** de registros y tabla `customers` con **500.000** registros. La consulta que se ejecuta con frecuencia y resulta lenta es:

```sql
SELECT
    o.order_id,
    o.order_date,
    c.customer_name,
    o.total_amount
FROM
    orders o
JOIN
    customers c ON o.customer_id = c.customer_id
WHERE
    c.country = 'México';
```

## Diagnóstico previo

Antes de optimizar, analizaría el plan de ejecución (`EXPLAIN PLAN` / `EXPLAIN ANALYZE`) para confirmar dónde está el costo. Los síntomas típicos aquí son: **Full Table Scan** sobre `orders` (10M filas), un **filtro sin índice** por `c.country`, y un **join** que obliga a leer muchas más filas de las que finalmente se devuelven.

A continuación, tres estrategias diferentes.

---

## Estrategia 1 — Índices adecuados en las columnas de filtro y de join

Es la causa más común de lentitud: sin índices el motor recorre las tablas completas.

```sql
-- Índice sobre la columna de filtro de customers
CREATE INDEX idx_customers_country ON customers (country);

-- Índice sobre la FK usada en el JOIN en orders
CREATE INDEX idx_orders_customer_id ON orders (customer_id);
```

**Por qué:** El índice en `customers.country` permite localizar rápidamente solo los clientes de México en lugar de escanear las 500.000 filas. El índice en `orders.customer_id` acelera el join (el motor puede usar un *index nested loop* o *hash join* eficiente) evitando el full scan de los 10M de pedidos.

**Cuándo aplica bien:** cuando 'México' representa una fracción pequeña/media del total (buena selectividad). Si casi todos los clientes fueran de México, el índice sobre `country` aportaría poco y convendría otra estrategia.

---

## Estrategia 2 — Índice de cobertura (covering index)

Crear un índice que contenga **todas las columnas que la consulta necesita**, de modo que el motor resuelva la consulta leyendo solo el índice, sin volver a la tabla ("index-only scan").

```sql
-- orders: incluir las columnas seleccionadas junto a la clave de join
CREATE INDEX idx_orders_cover
    ON orders (customer_id, order_id, order_date, total_amount);

-- customers: incluir el nombre para no visitar la tabla base
CREATE INDEX idx_customers_country_cover
    ON customers (country, customer_id, customer_name);
```

En PostgreSQL puede usarse la cláusula `INCLUDE` para columnas no filtrables:

```sql
CREATE INDEX idx_orders_cover
    ON orders (customer_id) INCLUDE (order_id, order_date, total_amount);
```

**Por qué:** elimina los accesos aleatorios a la tabla (los *table lookups* por cada fila encontrada), que en 10M de registros son muy costosos. Al tener toda la información en el índice, se reduce drásticamente la I/O.

**Contrapartida:** los índices ocupan espacio y encarecen las escrituras (INSERT/UPDATE). Se justifica porque la consulta es **frecuente** y de solo lectura.

---

## Estrategia 3 — Vista materializada / tabla desnormalizada precalculada

Si la consulta se ejecuta constantemente y los datos no cambian a cada segundo, precalcular el resultado:

```sql
CREATE MATERIALIZED VIEW mv_pedidos_mexico AS
SELECT
    o.order_id,
    o.order_date,
    c.customer_name,
    o.total_amount
FROM orders o
JOIN customers c ON o.customer_id = c.customer_id
WHERE c.country = 'México';

-- Refresco periódico (idealmente concurrente para no bloquear lecturas)
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_pedidos_mexico;
```

**Por qué:** el join y el filtro se calculan **una sola vez** por refresco, y las consultas posteriores leen un conjunto ya reducido y preparado. Ideal para dashboards/reportes donde se tolera una latencia de datos de minutos.

**Contrapartida:** los datos no son 100% en tiempo real; hay que definir la frecuencia de refresco según el negocio.

---

## Estrategias complementarias

- **Particionamiento de `orders`**: particionar por rango de `order_date` (o por `country` si estuviera en la tabla) permite *partition pruning*, leyendo solo las particiones relevantes en lugar de los 10M de filas.
- **Actualizar estadísticas** (`ANALYZE` / `DBMS_STATS`): el optimizador necesita estadísticas frescas para elegir el mejor plan; en tablas grandes que cambian mucho, estadísticas desactualizadas producen planes malos.
- **Seleccionar solo lo necesario y paginar**: evitar traer millones de filas al cliente; usar `LIMIT`/`OFFSET` o paginación por keyset si la UI no necesita todo el resultado.
- **Denormalizar `country` en `orders`** (si el patrón de acceso lo amerita): guardar el país directamente en la orden evita el join, a costa de redundancia controlada.

## Cómo decidiría

1. Ejecutar `EXPLAIN ANALYZE` para ver el plan real y las filas estimadas vs. reales.
2. Empezar por la **estrategia 1 (índices)**, que suele dar la mayor mejora con el menor costo.
3. Si tras indexar el acceso a la tabla base sigue pesando, añadir un **índice de cobertura (estrategia 2)**.
4. Si es una consulta de reporting muy repetida y se tolera algo de latencia, montar la **vista materializada (estrategia 3)**.
5. Medir de nuevo con `EXPLAIN ANALYZE` después de cada cambio: la optimización se valida con datos, no por intuición.

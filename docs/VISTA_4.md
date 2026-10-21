# Vista 4: Cierre e historial

`CierreFragment` se muestra dentro de `MainActivity`, con las seis opciones de navegación. `CierreActivity` permanece como contenedor compatible.

## Datos conectados

Cierre consulta `AtencionesStore.resumenes(fecha)`. Registrar guarda en ese mismo almacén; Servicios consulta sus registros y permite corregir el importe o anular con motivo; Inicio calcula las cifras del día desde los mismos datos.

El selector muestra empleados activos y empleados inactivos con atenciones del día. La selección inicial prioriza un empleado con atenciones y se conserva por ID al recrear la vista. Cada resumen incluye el conteo y la suma en centavos de las atenciones válidas de ese empleado y fecha. Las anuladas no suman y los precios históricos se mantienen aunque cambie el catálogo.

No hace falta enviar `EXTRA_RESUMENES`: los totales se calculan localmente. `EXTRA_FECHA` sigue disponible para consultar otra fecha mediante `CierreActivity`; solo se permite guardar cierres del día actual.

## Cálculos y guardado

- Esperado = suma de las atenciones válidas del empleado, sin fondo inicial. Se muestra una sola vez.
- El único importe que se captura es **Efectivo entregado**.
- Diferencia = entregado menos esperado, con BigDecimal.
- El importe entregado admite cero, punto o coma decimal y hasta dos decimales.
- Se revisan los totales antes de guardar. Si cambiaron, se actualiza la pantalla y se pide revisarlos.
- Se guarda un solo cierre por fecha y empleado en SharedPreferences (`cierres`, clave `historial`).
- Una caja cerrada no admite nuevas atenciones, correcciones ni anulaciones para ese empleado y fecha.
- El historial de entregas de Empleados consulta estos mismos cierres.
- Los nuevos cierres guardan esperado y entregado. Los cierres anteriores conservan su valor contado y su cálculo original; no se reescriben datos históricos.

## Datos de demostración

Las doce atenciones que antes estaban fijas en Servicios se migran una sola vez a `AtencionesStore`, para conservar los ejemplos y conectar sus totales. No se vuelven a crear al cambiar de día. Se señalan como ejemplos en Servicios y Cierre. No se utilizan los cierres de ayer para calcular los importes de hoy.

## Diseño y verificación

Se comparte la paleta y el estilo de las vistas de Descargas. `Orilla.SectionTabs` se utiliza tanto en Cierre como en la selección Carro/Moto de Registrar. Los campos y el formulario tienen scroll y el botón queda sobre la barra de navegación.

Compilación y Lint sin errores. Seis pruebas de datos pasaron en el emulador con almacenamiento aislado. La captura de los totales conectados está en `docs/capturas/cierre-conectado.png`.

# Integración de vistas de Carwash La Orilla

Origen: `C:\Users\marco\Downloads\Carwash_LaOrilla\Carwash_LaOrilla`.

Destino: este proyecto, `Car_wash_La_Orilla`.

## Organización

- `app/src/main/java/com/example/car_wash_la_orilla/`: se incorporaron `InicioFragment`, `RegistrarServicioFragment` y `ServiciosRegistradosFragment`. Se copió el código de `MainActivity` del origen y se adaptó al paquete del destino, a las barras del sistema y a las actividades existentes.
- `app/src/main/res/layout/`: pantalla principal, las tres vistas de fragmentos y los diálogos `dialog_confirmar_atencion` y `dialog_motivo`.
- `app/src/main/res/drawable/`: iconos y fondos de las vistas importadas; se conservaron los recursos del lanzador del destino.
- `app/src/main/res/values/`: colores `app_*`, etiquetas de navegación y colores del tema del origen. Se mantuvo el nombre del tema de este proyecto y se configuraron los mismos colores en `values-night`.

## Navegación

La barra inferior muestra seis accesos en una sola fila: Inicio, Registrar, Servicios, Cierre, Catálogo y Empleados. Todos cambian el fragment dentro de `MainActivity`, por lo que la barra permanece visible y resalta la opción actual. El botón Empleados de Inicio también utiliza esta navegación. Las actividades anteriores se mantienen como contenedores compatibles.

## Paleta compartida

Las vistas anteriores usan los colores `app_*` del proyecto de Descargas a través de alias: `ink`, `gold`, `bg`, `line` e `ink_secondary`. Las tarjetas activas y los campos tienen fondo blanco; las tarjetas inactivas usan gris claro. Los títulos usan sans-serif-condensed a 22sp, márgenes horizontales de 16dp y margen superior de 24dp. Las tarjetas tienen radio de 10dp, borde gris de 1.5dp y ninguna sombra. Agregar usa dorado con texto negro; guardar usa negro con texto blanco; editar, historial y cancelar usan borde y texto negros. Los diálogos comparten estos estilos. Catálogo usa un botón de ancho completo, como las vistas de Descargas.

Las actividades, fragmentos, almacenes y recursos existentes de Catálogo, Cierre y Empleados se conservaron. No se requieren dependencias nuevas.

## Alcance funcional de las vistas importadas

Registrar, Servicios, Inicio y Cierre consultan `AtencionesStore`, que conserva las atenciones en SharedPreferences (`atenciones`, clave `registros`). Registrar guarda el ID del empleado y del servicio, sus nombres, vehículo, fecha/hora y precio histórico en centavos. Inicio calcula sus cifras y últimas atenciones; Servicios carga los registros del día y permite corregir el importe o anular con motivo. Las anuladas se conservan para consulta y no suman en Inicio ni Cierre.

Cierre obtiene sus resúmenes por empleado directamente de esos registros, incluyendo empleados inactivos que ya tengan atenciones del día. Revisa los totales antes de guardar y conserva el cierre en el historial existente. Las atenciones de una caja cerrada no se pueden modificar, anular ni incrementar. Los importes históricos no cambian al editar un precio del catálogo. Cambiar importes mantiene un historial de cambios con motivo y valor anterior.

El formulario de Cierre tiene un solo importe editable, **Efectivo entregado**. La diferencia es entregado menos esperado. Se eliminó Efectivo contado y el recuadro que repetía el esperado. Los nuevos cierres guardan esperado y entregado; los cierres anteriores conservan sus importes y cálculo originales.

La lista fija de doce atenciones de demostración se migra una sola vez a registros persistidos, vinculados por ID a los empleados y servicios. Se marcan como ejemplos en Servicios y Cierre. Sus sumas son 12 atenciones y $132.00; Inicio deja de mostrar las cifras fijas que no coincidían con esa lista. Al cambiar el día, los resúmenes consultan la fecha actual y no repiten los ejemplos.

## Catálogo del documento

Fuente: sección 3 de `C:\Users\marco\Downloads\Propuesta_Carwash_La_Orilla (1).md`, identificada en el chat anterior “Analiza los archivos Markdown”. `CatalogoInicial` agrega una sola vez los servicios faltantes sin reemplazar precios ni estados existentes:

| Vehículo | Servicio | Precio de ejemplo |
| --- | --- | --- |
| Carro | Lavado básico | $5.00 |
| Carro | Lavado completo | $10.00 |
| Carro | Lavado con encerado | $15.00 |
| Carro | Limpieza de tapicería | $25.00 |
| Moto | Lavado básico | $3.00 |
| Moto | Lavado detallado | $8.00 |

El documento no define tarifas. Los importes iniciales provienen de Registrar en el proyecto de Descargas y se pueden editar en Catálogo.

## Datos de ejemplo

`DatosEjemplo.cargar()` incorpora una sola vez cinco empleados (Carlos Mendoza, Ana Gómez, Luis Rodríguez, Mariana Torres y José Hernández) y cinco cierres correspondientes al día anterior a la primera carga. Los cierres incluyen caja cuadrada, faltante y sobrante y están vinculados a los empleados para consultar sus historiales de entregas. Se conservan los registros previos, se reutilizan empleados del mismo nombre y se evita repetir cierres del mismo empleado y fecha. No se modifica el catálogo ni la lista de atenciones existentes.

## Respaldo de la importación

Los archivos que existían y fueron modificados se copiaron previamente a `.migration-backup/antes-importar-vistas/`, conservando sus rutas relativas. El proyecto de origen no fue modificado.

## Validación

Se ejecutó `gradlew.bat :app:assembleDebug :app:lintDebug --offline` tras simplificar el formulario, con resultado `BUILD SUCCESSFUL`. Lint informó 0 errores y 114 advertencias. El APK se generó en `app/build/outputs/apk/debug/app-debug.apk`.

Se verificó en el emulador que, con $30.00 esperados, entregar $28.00 muestra −$2.00, $30.00 muestra $0.00 y $32.00 muestra +$2.00. Guardar se habilita con un solo monto válido. No se guardaron cierres de prueba; se conservaron los cinco cierres existentes. Captura: `docs/capturas/cierre-simplificado.png`.

Las seis pruebas de `AtencionesStoreTest` pasaron en el emulador, ejecutadas directamente con ADB y AndroidJUnitRunner. Usan SharedPreferences aisladas y no modifican los registros de la app: persistencia y sumas por empleado, anulación con auditoría, precios históricos y corrección, bloqueo tras cierre, filtrado por fecha y conservación de datos dañados, migración de ejemplos sin duplicados.

Se recorrieron las seis opciones en el emulador, comprobando el título de cada pantalla y la presencia de los seis accesos. Se verificó que el formulario de servicio vacío muestre errores sin cerrarse. El catálogo conservó el servicio previo y los seis del documento, sin IDs duplicados tras reiniciar. Capturas: `docs/capturas/catalogo-integrado.png`, `empleados-integrados.png`, `cierre-integrado.png` y `dialogo-catalogo.png`.

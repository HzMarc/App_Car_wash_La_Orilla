# Perfil, QR del negocio y apertura/cierre de caja

La pestaña Perfil sustituye el acceso a Empleados. Muestra el nombre y usuario de la sesión, sin contraseña. Inicio ya no ofrece el botón Empleados.

## Identidad y registro

`SesionStore` persiste únicamente el ID autenticado y consulta el estado actual del empleado antes de usarlo. Main, Perfil, Registrar y Cierre redirigen al login cuando falta la sesión o el empleado está inactivo. `AtencionesStore.registrarActual` asigna los nuevos servicios al empleado conectado; no elige el primer empleado activo. Todas las variantes de registro exigen una apertura para ese empleado y fecha, tanto en efectivo como en transferencia.

Los servicios conservan el método `efectivo` o `transferencia`. Un servicio antiguo sin método se interpreta como efectivo. Los métodos desconocidos impiden calcular el cierre del empleado afectado.

## Apertura desde Inicio

Inicio mantiene sus tarjetas y accesos, agrega una tarjeta compacta debajo de la fecha y permite desplazamiento vertical. Todas las estadísticas y últimas atenciones corresponden al empleado conectado.

`AperturasStore` persiste `empleadoId`, `fecha`, `fechaHora` y `montoCentavos` en el archivo de preferencias `aperturas`, clave `registros`. La apertura exige sesión activa, fecha actual, monto no negativo con hasta dos decimales y ausencia de apertura o cierre para ese empleado y día. El diálogo comienza vacío, valida el monto y pide confirmación antes de guardarlo; conserva el estado al recrear la pantalla. El fondo no puede editarse ni se arrastra a otra fecha.

Inicio muestra Sin abrir, Abierta (fondo/hora) o Caja cerrada por hoy. Antes de abrir el efectivo esperado es «—»; después se obtiene del mismo resumen usado por Caja, y al cerrar se utiliza el importe guardado. Registrar y Caja ofrecen regresar a Inicio cuando falta apertura.

## Caja y cierre

`AtencionesStore.resumenEfectivo` calcula por empleado y fecha el precio cobrado y la cantidad de servicios en efectivo no anulados. El efectivo recibido antes de devolver cambio no incrementa el importe del servicio.

`CierresStore.resumenActual` combina fondo inicial y ventas en efectivo para obtener el total esperado. Cuenta las transferencias no anuladas de ese empleado y fecha, sin sumar ni mostrar sus importes en Caja o su historial. `CierresStore.guardar` vuelve a comprobar sesión, apertura, fecha actual, ausencia de otro cierre y coincidencia con el resumen confirmado, incluso si cambió únicamente el número de transferencias.

Los nuevos cierres conservan `tipo: "efectivo"`, `esperado` y `entregado`, y agregan `aperturaCentavos`, `fechaApertura`, `cobrosEfectivoCentavos` y `transferencias` (cantidad). La diferencia es entregado menos esperado. El historial muestra solo al empleado conectado, con las tarjetas compactas originales: fecha, atenciones, diferencia, estado y esperado/entregado. Al tocar una tarjeta se abre el desglose guardado, incluyendo apertura, efectivo y cantidad de transferencias. Los registros ficticios se identifican en ese detalle. Los datos antiguos sin estos campos se indican como «Sin apertura registrada» y «Sin dato de transferencias», sin alterar sus importes.

Se mantiene el bloqueo de registros y correcciones de cualquier método de pago después del cierre diario. No se incluyen gastos, retiros, reaperturas ni arrastre automático entre fechas. Los servicios existentes de hoy se incluyen cuando el empleado abre caja.

## Historial de demostración

`DatosEjemplo` agrega una sola vez seis cierres históricos adicionales por cada uno de los cinco empleados de ejemplo, usando los días 2 a 7 anteriores a la carga. Incluyen apertura, efectivo, cantidad de transferencias y estados de caja cuadrada, faltante y sobrante. La marca `historial_empleados_v2_cargado` permite actualizar instalaciones existentes sin duplicar registros. Se respetan los cierres ya guardados para un empleado/fecha y no se cierra la jornada actual. El historial se ordena por fecha descendente.

## Historial anterior

La migración `efectivo_v1` conserva el JSON original en `historial_antes_efectivo_v1`. Solo recalcula un cierre si la cantidad y el total de sus servicios disponibles coinciden con el cierre original y sus métodos son conocidos. Entonces actualiza `esperado` y la cantidad de atenciones en efectivo, conservando `entregado` y `contado` cuando exista. La diferencia se calcula al mostrarlo.

Si el detalle no permite reconstruir el cierre, conserva sus importes y agrega `pendienteRevision: true`, visible en el historial. Esto incluye los cierres ficticios sin servicios de la misma fecha. La migración se aplica una vez y solo se marca completada al guardar correctamente el respaldo y el historial.

## QR compartido

Perfil muestra automáticamente un QR estático generado e incluido como recurso (`qr_transferencias_demo.png`). Todos los empleados muestran el mismo código, incluso sin conexión o después de reiniciar. Ya no hay selector ni reemplazo de imágenes, y las imágenes importadas en versiones anteriores no se utilizan.

El QR contiene el texto fijo `LA ORILLA CAR WASH`, `TRANSFERENCIAS - DEMOSTRACION`, `CODIGO: LA-ORILLA-DEMO-001` y `NO VALIDO PARA PAGOS REALES`, separados por saltos de línea. La pantalla lo identifica como demostración y permite ampliarlo al tocarlo. No contiene cuentas bancarias ni enlaces de cobro.

## Verificación

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

Las pruebas cubren apertura obligatoria, montos inválidos y cero, persistencia y cambio de fecha, duplicados, confirmación desactualizada, apertura $10 más ventas $30 y dos transferencias (esperado $40), faltantes y sobrantes, identidad e inactivación, efectivo separado de transferencias, cierres en cero, duplicados, datos históricos incompletos, respaldo e idempotencia, disponibilidad del QR estático sin configuración. Las pruebas de interfaz recorren apertura y confirmación desde Inicio, recreación del diálogo, pantalla estrecha, bloqueo de registro sin apertura, login, registro, cierre personal, perfil recreado y QR automático, ampliación, recreación de la pantalla y el mismo código al cambiar de empleado. La imagen generada se verificó mediante decodificación con OpenCV, sin datos bancarios.

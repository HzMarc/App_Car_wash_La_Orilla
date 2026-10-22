package com.example.car_wash_la_orilla;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Fuente común de atenciones y totales. Los precios históricos se guardan en centavos. */
final class AtencionesStore {
    private final Context context;
    private final SharedPreferences preferences;

    AtencionesStore(Context context) {
        this.context = context.getApplicationContext();
        preferences = this.context.getSharedPreferences("atenciones", Context.MODE_PRIVATE);
    }

    static final class Atencion {
        final String id, servicioId, servicio, vehiculo, empleadoId, empleado, cliente, metodoPago;
        final LocalDateTime fechaHora;
        final long precioCentavos;
        final boolean anulada, ejemplo;
        Atencion(JSONObject row) throws JSONException {
            id = row.getString("id");
            servicioId = row.getString("servicioId");
            servicio = row.getString("servicio");
            vehiculo = row.getString("vehiculo");
            empleadoId = row.getString("empleadoId");
            empleado = row.getString("empleado");
            cliente = row.optString("cliente", "");
            metodoPago = row.optString("metodoPago", "efectivo").trim().toLowerCase(java.util.Locale.ROOT);
            precioCentavos = row.getLong("precioCentavos");
            anulada = row.getBoolean("anulada");
            ejemplo = row.optBoolean("ejemplo", false);
            try { fechaHora = LocalDateTime.parse(row.getString("fechaHora")); }
            catch (RuntimeException error) { throw new JSONException("Invalid date"); }
            if (id.isEmpty() || empleadoId.isEmpty() || servicioId.isEmpty() || servicio.trim().isEmpty()
                    || empleado.trim().isEmpty() || precioCentavos <= 0 || precioCentavos > 99999999
                    || !(vehiculo.equals("carro") || vehiculo.equals("moto"))) throw new JSONException("Invalid attention");
        }
        String precio() { return BigDecimal.valueOf(precioCentavos, 2).toPlainString(); }
        String hora() { return fechaHora.format(DateTimeFormatter.ofPattern("HH:mm")); }
    }

    List<Atencion> getAll() throws JSONException {
        JSONArray rows = rows();
        List<Atencion> result = new ArrayList<>();
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (int i = 0; i < rows.length(); i++) {
            Atencion atencion = new Atencion(rows.getJSONObject(i));
            if (!ids.add(atencion.id)) throw new JSONException("Duplicate attention");
            result.add(atencion);
        }
        result.sort(Comparator.comparing((Atencion a) -> a.fechaHora).reversed());
        return result;
    }

    List<Atencion> delDia(LocalDate fecha) throws JSONException {
        List<Atencion> result = new ArrayList<>();
        for (Atencion a : getAll()) if (a.fechaHora.toLocalDate().equals(fecha)) result.add(a);
        return result;
    }

    JSONArray resumenes(LocalDate fecha) throws JSONException {
        Map<String, JSONObject> resumen = new LinkedHashMap<>();
        for (EmpleadosStore.Empleado empleado : new EmpleadosStore(context).getAll()) {
            if (empleado.activo) resumen.put(empleado.id, resumen(empleado.id, empleado.nombre));
        }
        for (Atencion a : delDia(fecha)) {
            if (a.anulada) continue;
            validarMetodo(a.metodoPago);
            if (!"efectivo".equals(a.metodoPago)) continue;
            JSONObject fila = resumen.get(a.empleadoId);
            if (fila == null) { fila = resumen(a.empleadoId, a.empleado); resumen.put(a.empleadoId, fila); }
            fila.put("atenciones", fila.getInt("atenciones") + 1);
            fila.put("esperadoCentavos", Math.addExact(fila.getLong("esperadoCentavos"), a.precioCentavos));
            if (a.ejemplo) fila.put("ejemplo", true);
        }
        JSONArray result = new JSONArray();
        for (JSONObject fila : resumen.values()) result.put(fila);
        return result;
    }

    JSONObject resumenEfectivo(String empleadoId, LocalDate fecha) throws JSONException {
        EmpleadosStore.Empleado empleado = new EmpleadosStore(context).find(empleadoId);
        if (empleado == null) return null;
        JSONObject fila = resumen(empleado.id, empleado.nombre);
        for (Atencion a : delDia(fecha)) {
            if (!empleadoId.equals(a.empleadoId) || a.anulada) continue;
            validarMetodo(a.metodoPago);
            if (!"efectivo".equals(a.metodoPago)) continue;
            fila.put("atenciones", fila.getInt("atenciones") + 1);
            fila.put("esperadoCentavos", Math.addExact(fila.getLong("esperadoCentavos"), a.precioCentavos));
            if (a.ejemplo) fila.put("ejemplo", true);
        }
        return fila;
    }

    private static void validarMetodo(String metodo) throws JSONException {
        if (!"efectivo".equals(metodo) && !"transferencia".equals(metodo))
            throw new JSONException("Método de pago desconocido");
    }

    boolean registrarActual(String servicioId, String cliente, String metodo, long precio) throws JSONException {
        EmpleadosStore.Empleado empleado = new SesionStore(context).actual();
        return empleado != null && registrar(servicioId, empleado.id, cliente, metodo, precio);
    }

    private static JSONObject resumen(String id, String nombre) throws JSONException {
        return new JSONObject().put("empleadoId", id).put("empleado", nombre)
                .put("atenciones", 0).put("esperadoCentavos", 0L);
    }

    boolean registrar(String servicioId, String empleadoId, long precioConfirmado) throws JSONException {
        return registrar(servicioId, empleadoId, null, "efectivo", precioConfirmado);
    }

    boolean registrar(String servicioId, String empleadoId, String cliente, long precioConfirmado) throws JSONException {
        return registrar(servicioId, empleadoId, cliente, "efectivo", precioConfirmado);
    }

    boolean registrar(String servicioId, String empleadoId, String cliente, String metodoPago, long precioConfirmado) throws JSONException {
        String metodo = metodoPago == null ? "efectivo" : metodoPago.trim().toLowerCase(java.util.Locale.ROOT);
        validarMetodo(metodo);
        CatalogoStore.Servicio servicio = new CatalogoStore(context).find(servicioId);
        EmpleadosStore.Empleado empleado = new EmpleadosStore(context).find(empleadoId);
        if (servicio == null || !servicio.activo || empleado == null || !empleado.activo
                || servicio.precioCentavos != precioConfirmado) return false;
        LocalDateTime ahora = LocalDateTime.now();
        if (cerrado(empleadoId, ahora.toLocalDate())
                || new AperturasStore(context).buscar(empleadoId, ahora.toLocalDate()) == null) return false;
        getAll(); // Validar antes de modificar los datos persistidos.
        JSONArray rows = rows();
        JSONObject row = new JSONObject().put("id", UUID.randomUUID().toString())
                .put("fechaHora", ahora.toString()).put("servicioId", servicio.id)
                .put("servicio", servicio.nombre).put("vehiculo", servicio.vehiculo)
                .put("empleadoId", empleado.id).put("empleado", empleado.nombre)
                .put("precioCentavos", servicio.precioCentavos).put("anulada", false)
                .put("metodoPago", metodo);
        if (cliente != null && !cliente.trim().isEmpty()) {
            row.put("cliente", cliente.trim());
        }
        rows.put(row);
        return write(rows);
    }

    boolean cambiar(String id, String motivo, Long nuevoPrecio) throws JSONException {
        EmpleadosStore.Empleado empleado = new SesionStore(context).actual();
        if (empleado == null) return false;
        if (motivo == null || motivo.trim().isEmpty()
                || (nuevoPrecio != null && (nuevoPrecio <= 0 || nuevoPrecio > 99999999))) return false;
        getAll();
        JSONArray rows = rows();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            if (!id.equals(row.getString("id"))) continue;
            Atencion a = new Atencion(row);
            if (!empleado.id.equals(a.empleadoId) || a.anulada || !a.fechaHora.toLocalDate().equals(LocalDate.now())
                    || cerrado(a.empleadoId, a.fechaHora.toLocalDate())) return false;
            JSONArray cambios = row.optJSONArray("cambios");
            if (cambios == null) cambios = new JSONArray();
            cambios.put(new JSONObject().put("fechaHora", LocalDateTime.now().toString())
                    .put("motivo", motivo.trim()).put("precioAnteriorCentavos", a.precioCentavos)
                    .put("accion", nuevoPrecio == null ? "anular" : "corregir")
                    .put("precioNuevoCentavos", nuevoPrecio == null ? a.precioCentavos : nuevoPrecio));
            row.put("cambios", cambios);
            if (nuevoPrecio == null) row.put("anulada", true);
            else row.put("precioCentavos", nuevoPrecio);
            return write(rows);
        }
        return false;
    }

    boolean cerrado(String empleadoId, LocalDate fecha) throws JSONException {
        JSONArray cierres = new JSONArray(context.getSharedPreferences("cierres", Context.MODE_PRIVATE).getString("historial", "[]"));
        for (int i = 0; i < cierres.length(); i++) {
            JSONObject row = cierres.getJSONObject(i);
            if (empleadoId.equals(row.getString("empleadoId")) && fecha.toString().equals(row.getString("fecha"))) return true;
        }
        return false;
    }

    /** Migra una vez la lista de demostración que antes estaba fija en Servicios. */
    boolean cargarEjemplos() throws JSONException {
        if (preferences.contains("registros")) return true;
        String[] horas = {"14:30", "14:10", "13:45", "13:15", "12:50", "12:00", "11:30", "11:00", "10:20", "09:45", "09:10", "08:30"};
        String[] nombres = {"Carlos Mendoza", "Ana Gómez", "Mariana Torres", "Ana Gómez", "José Hernández", "Carlos Mendoza", "Luis Rodríguez", "Luis Rodríguez", "Mariana Torres", "Ana Gómez", "Carlos Mendoza", "José Hernández"};
        String[] servicios = {"Lavado completo", "Lavado detallado", "Limpieza de tapicería", "Lavado básico", "Lavado con encerado", "Lavado con encerado", "Lavado básico", "Lavado básico", "Lavado completo", "Lavado detallado", "Lavado básico", "Limpieza de tapicería"};
        long[] precios = {1000, 800, 2500, 300, 1500, 1500, 300, 500, 1000, 800, 500, 2500};
        List<EmpleadosStore.Empleado> empleados = new EmpleadosStore(context).getAll();
        List<CatalogoStore.Servicio> catalogo = new CatalogoStore(context).getAll();
        JSONArray rows = new JSONArray();
        LocalDate fecha = LocalDate.now();
        for (int i = 0; i < horas.length; i++) {
            String vehiculo = i == 1 || i == 3 || i == 6 || i == 9 ? "moto" : "carro";
            String empleadoId = null, servicioId = null;
            for (EmpleadosStore.Empleado empleado : empleados) if (empleado.nombre.equals(nombres[i])) empleadoId = empleado.id;
            for (CatalogoStore.Servicio servicio : catalogo) if (servicio.nombre.equals(servicios[i]) && servicio.vehiculo.equals(vehiculo)) servicioId = servicio.id;
            if (empleadoId == null || servicioId == null || cerrado(empleadoId, fecha)) continue;
            rows.put(new JSONObject().put("id", "ejemplo-atencion-" + (i + 1)).put("fechaHora", fecha + "T" + horas[i])
                    .put("empleadoId", empleadoId).put("empleado", nombres[i]).put("servicioId", servicioId)
                    .put("servicio", servicios[i]).put("vehiculo", vehiculo).put("precioCentavos", precios[i])
                    .put("anulada", false).put("ejemplo", true));
        }
        return write(rows);
    }

    private JSONArray rows() throws JSONException { return new JSONArray(preferences.getString("registros", "[]")); }
    private boolean write(JSONArray rows) { return preferences.edit().putString("registros", rows.toString()).commit(); }
}

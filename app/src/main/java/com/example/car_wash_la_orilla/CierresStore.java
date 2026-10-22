package com.example.car_wash_la_orilla;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Cierres de efectivo, filtrados por sesión, con migración conservadora del historial. */
final class CierresStore {
    private final Context context;
    private final SharedPreferences prefs;
    CierresStore(Context context) {
        this.context = context.getApplicationContext();
        prefs = this.context.getSharedPreferences("cierres", 0);
    }

    boolean migrar() throws JSONException {
        if (prefs.getBoolean("efectivo_v1", false)) return true;
        String original = prefs.getString("historial", "[]");
        JSONArray rows = new JSONArray(original);
        List<AtencionesStore.Atencion> atenciones = new AtencionesStore(context).getAll();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            if ("efectivo".equals(row.optString("tipo"))) continue;
            String id = row.getString("empleadoId"), fecha = row.getString("fecha");
            long total = 0, efectivo = 0;
            int cantidad = 0, cantidadEfectivo = 0;
            boolean metodoValido = true;
            for (AtencionesStore.Atencion a : atenciones) {
                if (!id.equals(a.empleadoId) || !fecha.equals(a.fechaHora.toLocalDate().toString()) || a.anulada) continue;
                cantidad++;
                total = Math.addExact(total, a.precioCentavos);
                if ("efectivo".equals(a.metodoPago)) {
                    efectivo = Math.addExact(efectivo, a.precioCentavos);
                    cantidadEfectivo++;
                } else if (!"transferencia".equals(a.metodoPago)) metodoValido = false;
            }
            // Sin detalle completo no se puede interpretar un cero como ausencia de efectivo.
            boolean completo = cantidad > 0 && cantidad == row.optInt("atenciones", -1)
                    && BigDecimal.valueOf(total, 2).compareTo(new BigDecimal(row.getString("esperado"))) == 0;
            if (completo && metodoValido) {
                row.put("esperado", BigDecimal.valueOf(efectivo, 2).toPlainString());
                row.put("atenciones", cantidadEfectivo).put("tipo", "efectivo");
                row.remove("pendienteRevision");
            } else row.put("pendienteRevision", true);
        }
        SharedPreferences.Editor editor = prefs.edit();
        if (!prefs.contains("historial_antes_efectivo_v1")) editor.putString("historial_antes_efectivo_v1", original);
        return editor.putString("historial", rows.toString()).putBoolean("efectivo_v1", true).commit();
    }

    JSONArray historialActual() throws JSONException {
        EmpleadosStore.Empleado empleado = new SesionStore(context).actual();
        if (empleado == null) throw new JSONException("Sesión inválida");
        if (!migrar()) throw new JSONException("No se pudo migrar el historial");
        JSONArray result = new JSONArray(), rows = new JSONArray(prefs.getString("historial", "[]"));
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            if (empleado.id.equals(row.getString("empleadoId"))) result.put(row);
        }
        return result;
    }

    JSONObject cierreActual(LocalDate fecha) throws JSONException {
        JSONArray rows = historialActual();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            if (fecha.toString().equals(row.getString("fecha"))) return row;
        }
        return null;
    }

    /** Fuente compartida por Inicio, Caja y la validación final del guardado. */
    JSONObject resumenActual(LocalDate fecha) throws JSONException {
        EmpleadosStore.Empleado empleado = new SesionStore(context).actual();
        if (empleado == null) throw new JSONException("Sesión inválida");
        JSONObject cerrado = cierreActual(fecha);
        if (cerrado != null) {
            JSONObject result = new JSONObject().put("empleadoId", empleado.id).put("empleado", empleado.nombre)
                    .put("estado", "cerrada").put("entregado", cerrado.getString("entregado"))
                    .put("atenciones", cerrado.getInt("atenciones"))
                    .put("esperadoCentavos", new BigDecimal(cerrado.getString("esperado")).movePointRight(2).longValueExact());
            for (String key : new String[]{"aperturaCentavos", "cobrosEfectivoCentavos", "transferencias", "fechaApertura"})
                if (cerrado.has(key)) result.put(key, cerrado.get(key));
            return result;
        }
        JSONObject result = new AtencionesStore(context).resumenEfectivo(empleado.id, fecha);
        if (result == null) throw new JSONException("Empleado no disponible");
        long ventas = result.getLong("esperadoCentavos");
        result.put("cobrosEfectivoCentavos", ventas);
        int transferencias = 0;
        for (AtencionesStore.Atencion a : new AtencionesStore(context).delDia(fecha)) {
            if (empleado.id.equals(a.empleadoId) && !a.anulada && "transferencia".equals(a.metodoPago)) transferencias++;
        }
        result.put("transferencias", transferencias);
        JSONObject apertura = new AperturasStore(context).buscar(empleado.id, fecha);
        result.put("estado", apertura == null ? "sin_abrir" : "abierta");
        if (apertura != null) {
            long fondo = apertura.getLong("montoCentavos");
            result.put("aperturaCentavos", fondo).put("fechaApertura", apertura.getString("fechaHora"))
                    .put("esperadoCentavos", Math.addExact(fondo, ventas));
        }
        return result;
    }

    boolean guardar(LocalDate fecha, BigDecimal entregado, JSONObject confirmado) throws JSONException {
        EmpleadosStore.Empleado empleado = new SesionStore(context).actual();
        if (empleado == null || fecha == null || !fecha.equals(LocalDate.now()) || confirmado == null
                || !AperturasStore.importeValido(entregado) || !migrar()) return false;
        AtencionesStore store = new AtencionesStore(context);
        JSONObject resumen = resumenActual(fecha);
        if (!"abierta".equals(resumen.getString("estado")) || !resumen.toString().equals(confirmado.toString())
                || store.cerrado(empleado.id, fecha)) return false;
        JSONObject row = new JSONObject().put("fecha", fecha.toString())
                .put("empleadoId", empleado.id).put("empleado", empleado.nombre)
                .put("atenciones", resumen.getInt("atenciones"))
                .put("esperado", BigDecimal.valueOf(resumen.getLong("esperadoCentavos"), 2).toPlainString())
                .put("entregado", entregado.setScale(2).toPlainString()).put("tipo", "efectivo");
        row.put("aperturaCentavos", resumen.getLong("aperturaCentavos"))
                .put("fechaApertura", resumen.getString("fechaApertura"))
                .put("cobrosEfectivoCentavos", resumen.getLong("cobrosEfectivoCentavos"))
                .put("transferencias", resumen.getInt("transferencias"));
        JSONArray rows = new JSONArray(prefs.getString("historial", "[]"));
        rows.put(row);
        return prefs.edit().putString("historial", rows.toString()).commit();
    }
}

package com.example.car_wash_la_orilla;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/** Fondo inicial inmutable por empleado y fecha; no se arrastra entre jornadas. */
final class AperturasStore {
    private final Context context;
    private final SharedPreferences prefs;

    AperturasStore(Context context) {
        this.context = context.getApplicationContext();
        prefs = this.context.getSharedPreferences("aperturas", Context.MODE_PRIVATE);
    }

    JSONObject buscar(String empleadoId, LocalDate fecha) throws JSONException {
        JSONArray rows = registros();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            if (empleadoId.equals(row.getString("empleadoId")) && fecha.toString().equals(row.getString("fecha")))
                return row;
        }
        return null;
    }

    boolean abrir(LocalDate fecha, BigDecimal monto) throws JSONException {
        EmpleadosStore.Empleado empleado = new SesionStore(context).actual();
        LocalDateTime ahora = LocalDateTime.now();
        if (empleado == null || fecha == null || !fecha.equals(ahora.toLocalDate()) || !importeValido(monto)
                || buscar(empleado.id, fecha) != null || new AtencionesStore(context).cerrado(empleado.id, fecha)) return false;
        JSONArray rows = registros();
        rows.put(new JSONObject().put("empleadoId", empleado.id).put("fecha", fecha.toString())
                .put("fechaHora", ahora.toString()).put("montoCentavos", monto.movePointRight(2).longValueExact()));
        return prefs.edit().putString("registros", rows.toString()).commit();
    }

    static boolean importeValido(BigDecimal monto) {
        return monto != null && monto.signum() >= 0 && monto.scale() <= 2
                && monto.compareTo(new BigDecimal("999999999.99")) <= 0;
    }

    static BigDecimal importe(String texto) {
        String valor = texto.trim().replace(',', '.');
        return valor.matches("[0-9]{1,9}(\\.[0-9]{1,2})?") ? new BigDecimal(valor).setScale(2) : null;
    }

    private JSONArray registros() throws JSONException {
        JSONArray rows = new JSONArray(prefs.getString("registros", "[]"));
        Set<String> claves = new HashSet<>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            try {
                String id = row.getString("empleadoId");
                LocalDate fecha = LocalDate.parse(row.getString("fecha"));
                LocalDateTime hora = LocalDateTime.parse(row.getString("fechaHora"));
                long monto = row.getLong("montoCentavos");
                if (id.isEmpty() || !hora.toLocalDate().equals(fecha) || monto < 0 || monto > 99999999999L
                        || !claves.add(id + ":" + fecha)) throw new JSONException("Apertura inválida");
            } catch (java.time.DateTimeException error) { throw new JSONException("Fecha de apertura inválida"); }
        }
        return rows;
    }
}

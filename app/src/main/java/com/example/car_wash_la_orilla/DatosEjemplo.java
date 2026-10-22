package com.example.car_wash_la_orilla;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;

/** Carga única de empleados y cierres ficticios para revisar las vistas. */
final class DatosEjemplo {
    private static final String MARCA = "ejemplos_v1_cargados";
    private static final String[] NOMBRES = {
            "Carlos Mendoza", "Ana Gómez", "Luis Rodríguez", "Mariana Torres", "José Hernández"
    };
    private static final String[] PASSWORDS = {
            "carlos123", "ana123", "luis123", "mariana123", "jose123"
    };

    private DatosEjemplo() { }

    static boolean cargar(Context context) {
        try {
            SharedPreferences empleados = context.getSharedPreferences("empleados", Context.MODE_PRIVATE);
            JSONArray lista = new JSONArray(empleados.getString("empleados", "[]"));
            boolean actualizado = false;
            for (int j = 0; j < lista.length(); j++) {
                JSONObject empleado = lista.getJSONObject(j);
                String nombre = empleado.getString("nombre");
                if (!nombre.equals(empleado.optString("user"))) {
                    empleado.put("user", nombre);
                    actualizado = true;
                }
                if (!empleado.has("password")) {
                    empleado.put("password", "");
                    actualizado = true;
                }
            }
            String[] ids = new String[NOMBRES.length];
            for (int i = 0; i < NOMBRES.length; i++) {
                for (int j = 0; j < lista.length(); j++) {
                    JSONObject empleado = lista.getJSONObject(j);
                    if (NOMBRES[i].equalsIgnoreCase(empleado.getString("nombre").trim())) {
                        ids[i] = empleado.getString("id");
                        if (empleado.optString("password", "").isEmpty()) {
                            empleado.put("password", PASSWORDS[i]);
                            actualizado = true;
                        }
                        break;
                    }
                }
                if (ids[i] == null && !empleados.getBoolean(MARCA, false)) {
                    ids[i] = "ejemplo-empleado-" + (i + 1);
                    lista.put(new JSONObject().put("id", ids[i])
                            .put("nombre", NOMBRES[i]).put("activo", true)
                            .put("user", NOMBRES[i]).put("password", PASSWORDS[i]));
                }
            }
            if ((!empleados.getBoolean(MARCA, false) || actualizado)
                    && !empleados.edit().putString("empleados", lista.toString())
                    .putBoolean(MARCA, true).commit()) return false;

            SharedPreferences cierres = context.getSharedPreferences("cierres", Context.MODE_PRIVATE);
            if (!cierres.getBoolean(MARCA, false)) {
                JSONArray historial = new JSONArray(cierres.getString("historial", "[]"));
                String fecha = LocalDate.now().minusDays(1).toString();
                int[] atenciones = {3, 3, 2, 2, 2};
                String[] esperado = {"30.00", "19.00", "8.00", "35.00", "40.00"};
                String[] contado = {"30.00", "18.00", "9.00", "35.00", "40.00"};
                for (int i = 0; i < NOMBRES.length; i++) {
                    if (ids[i] == null) return false;
                    boolean existe = false;
                    for (int j = 0; j < historial.length(); j++) {
                        JSONObject cierre = historial.getJSONObject(j);
                        if (ids[i].equals(cierre.getString("empleadoId"))
                                && fecha.equals(cierre.getString("fecha"))) {
                            existe = true;
                            break;
                        }
                    }
                    if (!existe) {
                        historial.put(new JSONObject().put("fecha", fecha)
                                .put("empleadoId", ids[i]).put("empleado", NOMBRES[i])
                                .put("atenciones", atenciones[i]).put("esperado", esperado[i])
                                .put("contado", contado[i]).put("entregado", contado[i])
                                .put("ejemplo", true));
                    }
                }
                if (!cierres.edit().putString("historial", historial.toString())
                        .putBoolean(MARCA, true).commit()) return false;
            }
            return ampliarHistorial(cierres, ids);
        } catch (JSONException error) {
            // Conservar los datos almacenados si no se pueden interpretar.
            return false;
        }
    }
    /** Agrega seis jornadas por empleado sin modificar cierres existentes ni cerrar el día actual. */
    private static boolean ampliarHistorial(SharedPreferences prefs, String[] ids) throws JSONException {
        String marca = "historial_empleados_v2_cargado";
        if (prefs.getBoolean(marca, false)) return true;
        JSONArray historial = new JSONArray(prefs.getString("historial", "[]"));
        LocalDate hoy = LocalDate.now();
        for (int dia = 2; dia <= 7; dia++) {
            String fecha = hoy.minusDays(dia).toString();
            for (int i = 0; i < NOMBRES.length; i++) {
                if (ids[i] == null) continue;
                boolean existe = false;
                for (int j = 0; j < historial.length(); j++) {
                    JSONObject row = historial.getJSONObject(j);
                    if (ids[i].equals(row.getString("empleadoId")) && fecha.equals(row.getString("fecha"))) {
                        existe = true;
                        break;
                    }
                }
                if (existe) continue;
                long fondo = 1000L;
                long ventas = (i + 1) * 500L + dia * 300L;
                long esperado = fondo + ventas;
                int estado = (i + dia) % 3;
                long entregado = esperado + (estado == 0 ? 0 : estado == 1 ? -100 : 150);
                historial.put(new JSONObject().put("fecha", fecha).put("empleadoId", ids[i])
                        .put("empleado", NOMBRES[i]).put("atenciones", 3 + (i + dia) % 6)
                        .put("esperado", java.math.BigDecimal.valueOf(esperado, 2).toPlainString())
                        .put("entregado", java.math.BigDecimal.valueOf(entregado, 2).toPlainString())
                        .put("aperturaCentavos", fondo).put("fechaApertura", fecha + "T08:00:00")
                        .put("cobrosEfectivoCentavos", ventas).put("transferencias", (i + dia) % 4)
                        .put("tipo", "efectivo").put("ejemplo", true));
            }
        }
        return prefs.edit().putString("historial", historial.toString()).putBoolean(marca, true).commit();
    }

}

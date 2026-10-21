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

    private DatosEjemplo() { }

    static boolean cargar(Context context) {
        try {
            SharedPreferences empleados = context.getSharedPreferences("empleados", Context.MODE_PRIVATE);
            JSONArray lista = new JSONArray(empleados.getString("empleados", "[]"));
            String[] ids = new String[NOMBRES.length];
            for (int i = 0; i < NOMBRES.length; i++) {
                for (int j = 0; j < lista.length(); j++) {
                    JSONObject empleado = lista.getJSONObject(j);
                    if (NOMBRES[i].equalsIgnoreCase(empleado.getString("nombre").trim())) {
                        ids[i] = empleado.getString("id");
                        break;
                    }
                }
                if (ids[i] == null && !empleados.getBoolean(MARCA, false)) {
                    ids[i] = "ejemplo-empleado-" + (i + 1);
                    lista.put(new JSONObject().put("id", ids[i])
                            .put("nombre", NOMBRES[i]).put("activo", true));
                }
            }
            if (!empleados.getBoolean(MARCA, false)
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
                return cierres.edit().putString("historial", historial.toString())
                        .putBoolean(MARCA, true).commit();
            }
            return true;
        } catch (JSONException error) {
            // Conservar los datos almacenados si no se pueden interpretar.
            return false;
        }
    }
}

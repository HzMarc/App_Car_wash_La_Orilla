package com.example.car_wash_la_orilla;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Servicios de la sección 3 de Propuesta_Carwash_La_Orilla (1).md. */
final class CatalogoInicial {
    private CatalogoInicial() { }

    static boolean cargar(Context context) {
        SharedPreferences preferencias = context.getSharedPreferences("catalogo_servicios", Context.MODE_PRIVATE);
        if (preferencias.getBoolean("catalogo_propuesta_v1", false)) return true;
        String[] nombres = {"Lavado básico", "Lavado completo", "Lavado con encerado",
                "Limpieza de tapicería", "Lavado básico", "Lavado detallado"};
        // Precios ilustrativos de Registrar; el documento no fija tarifas.
        long[] precios = {500, 1000, 1500, 2500, 300, 800};
        try {
            JSONArray servicios = new JSONArray(preferencias.getString("servicios", "[]"));
            for (int i = 0; i < nombres.length; i++) {
                String vehiculo = i < 4 ? "carro" : "moto";
                boolean existe = false;
                for (int j = 0; j < servicios.length(); j++) {
                    JSONObject servicio = servicios.getJSONObject(j);
                    if (vehiculo.equals(servicio.getString("vehiculo"))
                            && nombres[i].equalsIgnoreCase(servicio.getString("nombre").trim())) {
                        existe = true;
                        break;
                    }
                }
                if (!existe) servicios.put(new JSONObject().put("id", "propuesta-servicio-" + (i + 1))
                        .put("nombre", nombres[i]).put("vehiculo", vehiculo)
                        .put("precioCentavos", precios[i]).put("activo", true));
            }
            return preferencias.edit().putString("servicios", servicios.toString())
                    .putBoolean("catalogo_propuesta_v1", true).commit();
        } catch (JSONException error) {
            return false;
        }
    }
}

package com.example.car_wash_la_orilla;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DatosEjemploHistorialTest {
    private Context context;
    private final Set<String> archivos = new HashSet<>();

    @Before public void preparar() {
        Context real = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String prefijo = "prueba_historial_" + UUID.randomUUID() + "_";
        context = new ContextWrapper(real) {
            @Override public Context getApplicationContext() { return this; }
            @Override public SharedPreferences getSharedPreferences(String name, int mode) {
                archivos.add(prefijo + name);
                return real.getSharedPreferences(prefijo + name, mode);
            }
        };
    }

    @After public void limpiar() {
        Context real = InstrumentationRegistry.getInstrumentation().getTargetContext();
        for (String archivo : archivos) real.deleteSharedPreferences(archivo);
    }

    @Test public void agregaSieteCierresPorEmpleadoUnaVezConEstadosYTotalesCoherentes() throws Exception {
        assertTrue(DatosEjemplo.cargar(context));
        JSONArray historial = new JSONArray(context.getSharedPreferences("cierres", 0).getString("historial", "[]"));
        assertEquals(35, historial.length());
        for (EmpleadosStore.Empleado empleado : new EmpleadosStore(context).getAll()) {
            int cantidad = 0, nuevos = 0;
            Set<Integer> estados = new HashSet<>();
            Set<String> fechas = new HashSet<>();
            for (int i = 0; i < historial.length(); i++) {
                JSONObject row = historial.getJSONObject(i);
                if (!empleado.id.equals(row.getString("empleadoId"))) continue;
                cantidad++;
                assertTrue(fechas.add(row.getString("fecha")));
                assertTrue(LocalDate.parse(row.getString("fecha")).isBefore(LocalDate.now()));
                if (!row.has("aperturaCentavos")) continue;
                nuevos++;
                assertTrue(row.getBoolean("ejemplo"));
                assertEquals(row.getLong("aperturaCentavos") + row.getLong("cobrosEfectivoCentavos"),
                        new BigDecimal(row.getString("esperado")).movePointRight(2).longValueExact());
                estados.add(new BigDecimal(row.getString("entregado")).compareTo(new BigDecimal(row.getString("esperado"))));
                assertTrue(row.getInt("transferencias") >= 0);
                assertTrue(row.getInt("transferencias") <= 3);
            }
            assertEquals(7, cantidad);
            assertEquals(6, nuevos);
            assertEquals(3, estados.size());
            assertFalse(new AtencionesStore(context).cerrado(empleado.id, LocalDate.now()));
        }
        assertTrue(DatosEjemplo.cargar(context));
        assertEquals(historial.toString(), context.getSharedPreferences("cierres", 0).getString("historial", "[]"));
    }

    @Test public void actualizaInstalacionExistenteSinSobrescribirCierreReal() throws Exception {
        assertTrue(DatosEjemplo.cargar(context));
        SharedPreferences prefs = context.getSharedPreferences("cierres", 0);
        String id = new EmpleadosStore(context).getAll().get(0).id;
        JSONObject real = new JSONObject().put("fecha", LocalDate.now().minusDays(2).toString())
                .put("empleadoId", id).put("empleado", "Carlos Mendoza").put("atenciones", 1)
                .put("esperado", "99.00").put("entregado", "98.00").put("tipo", "efectivo");
        assertTrue(prefs.edit().putString("historial", new JSONArray().put(real).toString())
                .remove("historial_empleados_v2_cargado").commit());
        assertTrue(DatosEjemplo.cargar(context));
        JSONArray historial = new JSONArray(prefs.getString("historial", "[]"));
        assertEquals(30, historial.length());
        assertEquals(real.toString(), historial.getJSONObject(0).toString());
        assertTrue(DatosEjemplo.cargar(context));
        assertEquals(historial.toString(), prefs.getString("historial", "[]"));
    }
}

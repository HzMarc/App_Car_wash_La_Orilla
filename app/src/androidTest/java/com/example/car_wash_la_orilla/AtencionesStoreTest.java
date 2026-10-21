package com.example.car_wash_la_orilla;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AtencionesStoreTest {
    private Context context;
    private final Set<String> archivos = new HashSet<>();
    private String empleado, segundoEmpleado, servicio, segundoServicio;
    private AtencionesStore store;

    @Before public void preparar() throws Exception {
        Context real = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String prefijo = "prueba_atenciones_" + UUID.randomUUID() + "_";
        context = new ContextWrapper(real) {
            @Override public Context getApplicationContext() { return this; }
            @Override public SharedPreferences getSharedPreferences(String name, int mode) {
                archivos.add(prefijo + name);
                return real.getSharedPreferences(prefijo + name, mode);
            }
        };
        EmpleadosStore empleados = new EmpleadosStore(context);
        assertTrue(empleados.save(null, "Empleado Uno", true));
        assertTrue(empleados.save(null, "Empleado Dos", true));
        empleado = empleados.getAll().get(0).id;
        segundoEmpleado = empleados.getAll().get(1).id;
        CatalogoStore catalogo = new CatalogoStore(context);
        assertTrue(catalogo.save(null, "Lavado carro", "carro", 500));
        assertTrue(catalogo.save(null, "Lavado moto", "moto", 300));
        servicio = catalogo.getAll().get(0).id;
        segundoServicio = catalogo.getAll().get(1).id;
        store = new AtencionesStore(context);
    }

    @After public void limpiar() {
        Context real = InstrumentationRegistry.getInstrumentation().getTargetContext();
        for (String archivo : archivos) real.deleteSharedPreferences(archivo);
    }

    private JSONObject resumen(String id) throws JSONException {
        JSONArray resumenes = store.resumenes(LocalDate.now());
        for (int i = 0; i < resumenes.length(); i++) {
            JSONObject fila = resumenes.getJSONObject(i);
            if (id.equals(fila.getString("empleadoId"))) return fila;
        }
        throw new AssertionError("Falta el empleado");
    }

    @Test public void persisteYSumaPorEmpleadoInclusoInactivo() throws Exception {
        assertTrue(store.registrar(servicio, empleado, 500));
        assertTrue(store.registrar(segundoServicio, empleado, 300));
        assertTrue(store.registrar(segundoServicio, segundoEmpleado, 300));
        store = new AtencionesStore(context);
        assertEquals(3, store.getAll().size());
        assertEquals(2, resumen(empleado).getInt("atenciones"));
        assertEquals(800, resumen(empleado).getLong("esperadoCentavos"));
        assertEquals(300, resumen(segundoEmpleado).getLong("esperadoCentavos"));
        assertTrue(new EmpleadosStore(context).setActive(empleado, false));
        assertEquals(800, resumen(empleado).getLong("esperadoCentavos"));
        assertFalse(store.registrar(servicio, empleado, 500));
    }

    @Test public void anularExcluyeDelTotalYConservaMotivo() throws Exception {
        assertTrue(store.registrar(servicio, empleado, 500));
        String id = store.getAll().get(0).id;
        assertFalse(store.cambiar(id, " ", null));
        assertTrue(store.cambiar(id, "Servicio duplicado", null));
        assertTrue(store.getAll().get(0).anulada);
        assertEquals(0, resumen(empleado).getLong("esperadoCentavos"));
        assertEquals(0, resumen(empleado).getInt("atenciones"));
        assertFalse(store.cambiar(id, "Otra vez", 700L));
        JSONArray rows = new JSONArray(context.getSharedPreferences("atenciones", 0).getString("registros", "[]"));
        assertEquals("Servicio duplicado", rows.getJSONObject(0).getJSONArray("cambios").getJSONObject(0).getString("motivo"));
    }

    @Test public void preciosHistoricosYCorreccionesNoDependenDelCatalogo() throws Exception {
        assertTrue(store.registrar(servicio, empleado, 500));
        String id = store.getAll().get(0).id;
        assertTrue(new CatalogoStore(context).save(servicio, "Lavado carro nuevo", "carro", 950));
        assertEquals(500, store.getAll().get(0).precioCentavos);
        assertFalse(store.registrar(servicio, empleado, 500));
        assertTrue(store.cambiar(id, "Cobro corregido", 700L));
        assertTrue(store.registrar(servicio, empleado, 950));
        assertEquals(1650, resumen(empleado).getLong("esperadoCentavos"));
    }

    @Test public void cierreBloqueaRegistrosYModificacionesDelEmpleado() throws Exception {
        assertTrue(store.registrar(servicio, empleado, 500));
        String id = store.getAll().get(0).id;
        JSONArray cierre = new JSONArray().put(new JSONObject().put("fecha", LocalDate.now().toString()).put("empleadoId", empleado));
        assertTrue(context.getSharedPreferences("cierres", 0).edit().putString("historial", cierre.toString()).commit());
        assertFalse(store.registrar(servicio, empleado, 500));
        assertFalse(store.cambiar(id, "Anular", null));
        assertFalse(store.cambiar(id, "Corregir", 600L));
        assertTrue(store.registrar(servicio, segundoEmpleado, 500));
        assertEquals(500, resumen(empleado).getLong("esperadoCentavos"));
    }

    @Test public void filtraLaFechaYNoSobrescribeDatosDañados() throws Exception {
        assertTrue(store.registrar(servicio, empleado, 500));
        SharedPreferences preferencias = context.getSharedPreferences("atenciones", 0);
        JSONArray rows = new JSONArray(preferencias.getString("registros", "[]"));
        rows.getJSONObject(0).put("fechaHora", LocalDate.now().minusDays(1) + "T10:00");
        assertTrue(preferencias.edit().putString("registros", rows.toString()).commit());
        assertEquals(0, resumen(empleado).getLong("esperadoCentavos"));
        assertEquals(1, store.delDia(LocalDate.now().minusDays(1)).size());
        assertTrue(preferencias.edit().putString("registros", "datos dañados").commit());
        try { store.registrar(servicio, empleado, 500); fail("Debe rechazar los datos dañados"); }
        catch (JSONException expected) { assertEquals("datos dañados", preferencias.getString("registros", "")); }
    }

    @Test public void migraEjemplosUnaVezConTotalesConsistentes() throws Exception {
        assertTrue(DatosEjemplo.cargar(context));
        assertTrue(CatalogoInicial.cargar(context));
        assertTrue(store.cargarEjemplos());
        assertTrue(store.cargarEjemplos());
        assertEquals(12, store.getAll().size());
        long total = 0;
        JSONArray resumenes = store.resumenes(LocalDate.now());
        for (int i = 0; i < resumenes.length(); i++) total += resumenes.getJSONObject(i).getLong("esperadoCentavos");
        assertEquals(13200, total);
    }
}

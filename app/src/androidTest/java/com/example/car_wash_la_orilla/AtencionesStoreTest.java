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
        entrar(empleado);
        assertTrue(new AperturasStore(context).abrir(LocalDate.now(), java.math.BigDecimal.ZERO));
        entrar(segundoEmpleado);
        assertTrue(new AperturasStore(context).abrir(LocalDate.now(), java.math.BigDecimal.ZERO));
        context.getSharedPreferences("sesion", 0).edit().clear().commit();
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
        entrar(empleado);
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
        entrar(empleado);
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
        entrar(empleado);
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

    @Test public void loginValidaCredencialesYEstadoSinPerderPasswordAlEditar() throws Exception {
        EmpleadosStore empleados = new EmpleadosStore(context);
        assertNotNull(empleados.autenticar(" empleado uno ", ""));
        assertNull(empleados.autenticar("", ""));
        assertNull(empleados.autenticar("Desconocido", ""));
        assertNull(empleados.autenticar("Empleado Uno", "incorrecta"));
        SharedPreferences prefs = context.getSharedPreferences("empleados", 0);
        JSONArray rows = new JSONArray(prefs.getString("empleados", "[]"));
        rows.getJSONObject(0).put("password", "Clave de prueba");
        assertTrue(prefs.edit().putString("empleados", rows.toString()).commit());
        assertTrue(empleados.save(empleado, "Nombre Nuevo", true));
        assertNull(empleados.autenticar("Nombre Nuevo", ""));
        assertNotNull(empleados.autenticar("Nombre Nuevo", "Clave de prueba"));
        assertTrue(empleados.setActive(empleado, false));
        assertNull(empleados.autenticar("Nombre Nuevo", "Clave de prueba"));
        assertTrue(empleados.setActive(empleado, true));
        assertNotNull(empleados.autenticar("Nombre Nuevo", "Clave de prueba"));
    }

    @Test public void agregaCredencialesAEjemplosYDatosExistentesSinDuplicar() throws Exception {
        assertTrue(DatosEjemplo.cargar(context));
        EmpleadosStore empleados = new EmpleadosStore(context);
        int cantidad = empleados.getAll().size();
        String[] nombres = {"Carlos Mendoza", "Ana Gómez", "Luis Rodríguez", "Mariana Torres", "José Hernández"};
        String[] passwords = {"carlos123", "ana123", "luis123", "mariana123", "jose123"};
        for (int i = 0; i < nombres.length; i++) {
            assertNotNull(empleados.autenticar(nombres[i], passwords[i]));
            assertNull(empleados.autenticar(nombres[i], ""));
        }
        SharedPreferences prefs = context.getSharedPreferences("empleados", 0);
        JSONArray rows = new JSONArray(prefs.getString("empleados", "[]"));
        rows.getJSONObject(0).put("password", "Conservar");
        rows.getJSONObject(1).remove("user");
        rows.getJSONObject(1).remove("password");
        for (int i = 0; i < rows.length(); i++) {
            if ("Carlos Mendoza".equals(rows.getJSONObject(i).getString("nombre"))) {
                rows.getJSONObject(i).put("password", "");
            }
        }
        assertTrue(prefs.edit().putString("empleados", rows.toString()).commit());
        assertTrue(DatosEjemplo.cargar(context));
        assertEquals(cantidad, empleados.getAll().size());
        rows = new JSONArray(prefs.getString("empleados", "[]"));
        assertEquals("Conservar", rows.getJSONObject(0).getString("password"));
        assertEquals("Empleado Dos", rows.getJSONObject(1).getString("user"));
        assertEquals("", rows.getJSONObject(1).getString("password"));
        assertNotNull(empleados.autenticar("Carlos Mendoza", "carlos123"));
        assertNull(empleados.autenticar("Carlos Mendoza", ""));
    }

    private void entrar(String id) throws Exception {
        assertTrue(new SesionStore(context).iniciar(new EmpleadosStore(context).find(id)));
    }

    @Test public void rechazaCorreccionesYAnulacionesAjenasSinModificarDatos() throws Exception {
        assertTrue(store.registrar(servicio, empleado, 500));
        assertTrue(store.registrar(segundoServicio, segundoEmpleado, 300));
        String propio = store.getAll().stream().filter(a -> empleado.equals(a.empleadoId)).findFirst().get().id;
        String ajeno = store.getAll().stream().filter(a -> segundoEmpleado.equals(a.empleadoId)).findFirst().get().id;
        SharedPreferences prefs = context.getSharedPreferences("atenciones", 0);
        entrar(empleado);
        String original = prefs.getString("registros", "");
        assertFalse(store.cambiar(ajeno, "Corrección ajena", 700L));
        assertFalse(store.cambiar(ajeno, "Anulación ajena", null));
        assertEquals(original, prefs.getString("registros", ""));
        assertTrue(store.cambiar(propio, "Corrección propia", 600L));
        entrar(segundoEmpleado);
        original = prefs.getString("registros", "");
        assertFalse(store.cambiar(propio, "Corrección ajena", 800L));
        assertFalse(store.cambiar(propio, "Anulación ajena", null));
        assertEquals(original, prefs.getString("registros", ""));
        assertTrue(store.cambiar(ajeno, "Corrección propia", 400L));
        assertTrue(store.cambiar(ajeno, "Anulación propia", null));
    }

    @Test public void rechazaCambiosSinSesionOConEmpleadoInactivo() throws Exception {
        assertTrue(store.registrar(servicio, empleado, 500));
        String id = store.getAll().get(0).id;
        SharedPreferences prefs = context.getSharedPreferences("atenciones", 0);
        String original = prefs.getString("registros", "");
        assertFalse(store.cambiar(id, "Sin sesión", 600L));
        assertFalse(store.cambiar(id, "Sin sesión", null));
        entrar(empleado);
        assertTrue(new EmpleadosStore(context).setActive(empleado, false));
        assertFalse(store.cambiar(id, "Inactivo", 600L));
        assertFalse(store.cambiar(id, "Inactivo", null));
        assertEquals(original, prefs.getString("registros", ""));
    }

    @Test public void propietarioNoPuedeModificarServiciosDeOtraFecha() throws Exception {
        entrar(empleado);
        assertTrue(store.registrar(servicio, empleado, 500));
        SharedPreferences prefs = context.getSharedPreferences("atenciones", 0);
        JSONArray rows = new JSONArray(prefs.getString("registros", "[]"));
        String id = rows.getJSONObject(0).getString("id");
        rows.getJSONObject(0).put("fechaHora", LocalDate.now().minusDays(1) + "T10:00");
        String original = rows.toString();
        assertTrue(prefs.edit().putString("registros", original).commit());
        assertFalse(store.cambiar(id, "Corrección anterior", 600L));
        assertFalse(store.cambiar(id, "Anulación anterior", null));
        assertEquals(original, prefs.getString("registros", ""));
    }

    @Test public void sesionAsignaServiciosYValidaEmpleadoActivo() throws Exception {
        assertFalse(store.registrarActual(servicio, "Cliente", "efectivo", 500));
        entrar(segundoEmpleado);
        assertTrue(store.registrarActual(servicio, "Cliente", "transferencia", 500));
        assertEquals(segundoEmpleado, store.getAll().get(0).empleadoId);
        assertEquals(segundoEmpleado, new SesionStore(context).actual().id);
        assertTrue(new EmpleadosStore(context).setActive(segundoEmpleado, false));
        assertNull(new SesionStore(context).actual());
        assertFalse(store.registrarActual(servicio, "Cliente", "efectivo", 500));
    }

    @Test public void cierreSoloSumaEfectivoYNoPermiteGuardarParaOtroEmpleado() throws Exception {
        CatalogoStore catalogo = new CatalogoStore(context);
        assertTrue(catalogo.save(servicio, "Lavado carro", "carro", 3000));
        assertTrue(catalogo.save(segundoServicio, "Lavado moto", "moto", 2000));
        assertTrue(store.registrar(servicio, empleado, "Cliente", "Efectivo", 3000));
        assertTrue(store.registrar(segundoServicio, empleado, "Cliente", "Transferencia", 2000));
        entrar(empleado);
        JSONObject resumen = new CierresStore(context).resumenActual(LocalDate.now());
        assertEquals(3000, resumen.getLong("esperadoCentavos"));
        assertEquals(1, resumen.getInt("atenciones"));
        CierresStore cierres = new CierresStore(context);
        entrar(segundoEmpleado);
        assertFalse(cierres.guardar(LocalDate.now(), new java.math.BigDecimal("30.00"), resumen));
        entrar(empleado);
        assertTrue(cierres.guardar(LocalDate.now(), new java.math.BigDecimal("30.00"), resumen));
        JSONObject cierre = cierres.historialActual().getJSONObject(0);
        assertEquals("30.00", cierre.getString("esperado"));
        assertEquals("30.00", cierre.getString("entregado"));
        assertEquals("efectivo", cierre.getString("tipo"));
        assertFalse(cierres.guardar(LocalDate.now(), new java.math.BigDecimal("30.00"), resumen));
        assertFalse(store.registrarActual(servicio, "Cliente", "transferencia", 3000));
        assertFalse(store.cambiar(store.getAll().get(0).id, "Corrección", 100L));
        entrar(segundoEmpleado);
        assertEquals(0, cierres.historialActual().length());
    }

    @Test public void soloTransferenciasPermiteCierreEnCeroYCorreccionesNoSumanEfectivo() throws Exception {
        entrar(empleado);
        assertTrue(store.registrarActual(servicio, "Cliente", "transferencia", 500));
        String id = store.getAll().get(0).id;
        assertTrue(store.cambiar(id, "Corrección", 700L));
        assertEquals(0, resumen(empleado).getLong("esperadoCentavos"));
        assertTrue(store.cambiar(id, "Anular", null));
        assertTrue(store.registrarActual(segundoServicio, "Cliente", "transferencia", 300));
        assertTrue(new CierresStore(context).guardar(LocalDate.now(), new java.math.BigDecimal("0.00"),
                new CierresStore(context).resumenActual(LocalDate.now())));
    }

    @Test public void metodoDesconocidoBloqueaCierreYDatosAntiguosSonEfectivo() throws Exception {
        assertTrue(store.registrar(servicio, empleado, 500));
        SharedPreferences prefs = context.getSharedPreferences("atenciones", 0);
        JSONArray rows = new JSONArray(prefs.getString("registros", "[]"));
        rows.getJSONObject(0).remove("metodoPago");
        assertTrue(prefs.edit().putString("registros", rows.toString()).commit());
        assertEquals(500, store.resumenEfectivo(empleado, LocalDate.now()).getLong("esperadoCentavos"));
        entrar(empleado);
        JSONObject confirmado = new CierresStore(context).resumenActual(LocalDate.now());
        rows.getJSONObject(0).put("metodoPago", "otro");
        assertTrue(prefs.edit().putString("registros", rows.toString()).commit());
        entrar(empleado);
        try {
            new CierresStore(context).guardar(LocalDate.now(), new java.math.BigDecimal("5.00"), confirmado);
            fail("No debe cerrar con método desconocido");
        } catch (JSONException expected) { }
        assertEquals(0, new CierresStore(context).historialActual().length());
        assertEquals(0, store.resumenEfectivo(segundoEmpleado, LocalDate.now()).getLong("esperadoCentavos"));
    }

    @Test public void migraCierreConRespaldoConservaEntregadoYMarcaFaltaDeDetalle() throws Exception {
        assertTrue(store.registrar(servicio, empleado, "Cliente", "efectivo", 500));
        assertTrue(store.registrar(segundoServicio, empleado, "Cliente", "transferencia", 300));
        JSONArray rows = new JSONArray(context.getSharedPreferences("atenciones", 0).getString("registros", "[]"));
        for (int i = 0; i < rows.length(); i++) rows.getJSONObject(i).put("fechaHora", LocalDate.now().minusDays(1) + "T10:00");
        assertTrue(context.getSharedPreferences("atenciones", 0).edit().putString("registros", rows.toString()).commit());
        JSONObject viejo = new JSONObject().put("fecha", LocalDate.now().minusDays(1).toString())
                .put("empleadoId", empleado).put("empleado", "Empleado Uno").put("atenciones", 2)
                .put("esperado", "8.00").put("entregado", "7.00").put("contado", "6.00");
        JSONObject sinDetalle = new JSONObject(viejo.toString()).put("fecha", LocalDate.now().minusDays(2).toString());
        String original = new JSONArray().put(viejo).put(sinDetalle).toString();
        SharedPreferences prefs = context.getSharedPreferences("cierres", 0);
        assertTrue(prefs.edit().putString("historial", original).commit());
        entrar(empleado);
        CierresStore cierres = new CierresStore(context);
        JSONObject corregido = cierres.historialActual().getJSONObject(0);
        assertEquals("5.00", corregido.getString("esperado"));
        assertEquals("7.00", corregido.getString("entregado"));
        assertEquals("6.00", corregido.getString("contado"));
        assertEquals(1, corregido.getInt("atenciones"));
        assertTrue(cierres.historialActual().getJSONObject(1).getBoolean("pendienteRevision"));
        assertEquals("8.00", cierres.historialActual().getJSONObject(1).getString("esperado"));
        assertEquals(original, prefs.getString("historial_antes_efectivo_v1", ""));
        String migrado = prefs.getString("historial", "");
        assertTrue(cierres.migrar());
        assertEquals(migrado, prefs.getString("historial", ""));
        entrar(segundoEmpleado);
        assertEquals(0, cierres.historialActual().length());
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

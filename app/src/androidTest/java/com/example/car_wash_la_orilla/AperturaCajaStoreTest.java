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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AperturaCajaStoreTest {
    private Context context;
    private final Set<String> archivos = new HashSet<>();
    private String empleado, otro, efectivo, transferencia;
    private AtencionesStore atenciones;
    private AperturasStore aperturas;
    private CierresStore cierres;

    @Before public void preparar() throws Exception {
        Context real = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String prefijo = "prueba_apertura_" + UUID.randomUUID() + "_";
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
        otro = empleados.getAll().get(1).id;
        CatalogoStore catalogo = new CatalogoStore(context);
        assertTrue(catalogo.save(null, "Efectivo prueba", "carro", 3000));
        assertTrue(catalogo.save(null, "Transferencia prueba", "moto", 2000));
        efectivo = catalogo.getAll().get(0).id;
        transferencia = catalogo.getAll().get(1).id;
        atenciones = new AtencionesStore(context);
        aperturas = new AperturasStore(context);
        cierres = new CierresStore(context);
        entrar(empleado);
    }

    @After public void limpiar() {
        Context real = InstrumentationRegistry.getInstrumentation().getTargetContext();
        for (String archivo : archivos) real.deleteSharedPreferences(archivo);
    }

    private void entrar(String id) throws Exception {
        assertTrue(new SesionStore(context).iniciar(new EmpleadosStore(context).find(id)));
    }

    private JSONObject resumen() throws Exception { return cierres.resumenActual(LocalDate.now()); }
    private boolean abrir(String monto) throws Exception { return aperturas.abrir(LocalDate.now(), new BigDecimal(monto)); }

    @Test public void sinAperturaBloqueaAmbosMetodosYCierreEnAlmacenamiento() throws Exception {
        assertEquals("sin_abrir", resumen().getString("estado"));
        assertFalse(atenciones.registrarActual(efectivo, "Cliente", "efectivo", 3000));
        assertFalse(atenciones.registrarActual(transferencia, "Cliente", "transferencia", 2000));
        assertFalse(cierres.guardar(LocalDate.now(), BigDecimal.ZERO, resumen()));
        assertEquals(0, atenciones.getAll().size());
        assertEquals(0, cierres.historialActual().length());
    }

    @Test public void ejemploDiezMasTreintaYDosTransferenciasCierraEnCuarenta() throws Exception {
        assertTrue(abrir("10.00"));
        assertTrue(atenciones.registrarActual(efectivo, "Cliente", "efectivo", 3000));
        assertTrue(atenciones.registrarActual(transferencia, "Cliente", "transferencia", 2000));
        assertTrue(atenciones.registrarActual(transferencia, "Otro cliente", "transferencia", 2000));
        JSONObject summary = resumen();
        assertEquals(1000, summary.getLong("aperturaCentavos"));
        assertEquals(3000, summary.getLong("cobrosEfectivoCentavos"));
        assertEquals(4000, summary.getLong("esperadoCentavos"));
        assertEquals(2, summary.getInt("transferencias"));
        assertTrue(cierres.guardar(LocalDate.now(), new BigDecimal("40.00"), summary));
        JSONObject guardado = cierres.historialActual().getJSONObject(0);
        assertEquals("40.00", guardado.getString("esperado"));
        assertEquals("40.00", guardado.getString("entregado"));
        assertEquals(2, guardado.getInt("transferencias"));
        assertEquals("cerrada", resumen().getString("estado"));
        assertFalse(abrir("50.00"));
        assertFalse(cierres.guardar(LocalDate.now(), new BigDecimal("40.00"), summary));
        assertFalse(atenciones.registrarActual(efectivo, "Cliente", "efectivo", 3000));
        assertFalse(atenciones.cambiar(atenciones.getAll().get(0).id, "Cambio", 2500L));
    }

    @Test public void validaImportesYConservaFondoSinEdicionNiDuplicados() throws Exception {
        assertFalse(abrir("-1"));
        assertFalse(abrir("0.001"));
        assertFalse(abrir("1000000000"));
        assertNull(AperturasStore.importe(""));
        assertNull(AperturasStore.importe("1.234"));
        assertNull(AperturasStore.importe("1e2"));
        assertEquals(new BigDecimal("10.50"), AperturasStore.importe("10,50"));
        assertTrue(abrir("0.00"));
        assertFalse(abrir("10.00"));
        assertEquals(0, new AperturasStore(context).buscar(empleado, LocalDate.now()).getLong("montoCentavos"));
        assertTrue(cierres.guardar(LocalDate.now(), BigDecimal.ZERO, resumen()));
        assertEquals("0.00", cierres.historialActual().getJSONObject(0).getString("esperado"));
    }

    @Test public void sinVentasYConSoloTransferenciasNoGeneraDeudaAdicional() throws Exception {
        assertTrue(abrir("10.00"));
        assertEquals(1000, resumen().getLong("esperadoCentavos"));
        assertEquals(0, resumen().getInt("transferencias"));
        assertTrue(atenciones.registrarActual(transferencia, "Cliente", "transferencia", 2000));
        assertEquals(1000, resumen().getLong("esperadoCentavos"));
        assertTrue(cierres.guardar(LocalDate.now(), new BigDecimal("10.00"), resumen()));
        JSONObject cierre = cierres.historialActual().getJSONObject(0);
        assertEquals("10.00", cierre.getString("entregado"));
        assertEquals("10.00", cierre.getString("esperado"));
        assertEquals(1, cierre.getInt("transferencias"));
    }

    @Test public void anulacionYCorreccionDeCadaMetodoRecalculaSoloLoCorrespondiente() throws Exception {
        assertTrue(abrir("10.00"));
        assertTrue(atenciones.registrarActual(transferencia, "Cliente", "transferencia", 2000));
        String id = atenciones.getAll().get(0).id;
        assertTrue(atenciones.cambiar(id, "Precio", 1000L));
        assertEquals(1000, resumen().getLong("esperadoCentavos"));
        assertTrue(atenciones.cambiar(id, "Anulada", null));
        assertEquals(0, resumen().getInt("transferencias"));
        assertTrue(atenciones.registrarActual(efectivo, "Cliente", "efectivo", 3000));
        id = atenciones.getAll().get(0).id;
        assertTrue(atenciones.cambiar(id, "Precio", 2500L));
        assertEquals(3500, resumen().getLong("esperadoCentavos"));
        assertTrue(atenciones.cambiar(id, "Anulada", null));
        assertEquals(1000, resumen().getLong("esperadoCentavos"));
        assertEquals(0, resumen().getInt("atenciones"));
    }

    @Test public void aperturasSonPersonalesPersistenYNoSeArrastranDeFecha() throws Exception {
        assertTrue(abrir("10.00"));
        entrar(otro);
        assertEquals("sin_abrir", resumen().getString("estado"));
        assertTrue(abrir("20.00"));
        assertEquals(2000, new CierresStore(context).resumenActual(LocalDate.now()).getLong("esperadoCentavos"));
        entrar(empleado);
        assertEquals(1000, resumen().getLong("esperadoCentavos"));
        SharedPreferences prefs = context.getSharedPreferences("aperturas", 0);
        JSONArray rows = new JSONArray(prefs.getString("registros", "[]"));
        rows.getJSONObject(0).put("fecha", LocalDate.now().minusDays(1).toString())
                .put("fechaHora", LocalDate.now().minusDays(1) + "T08:00");
        assertTrue(prefs.edit().putString("registros", rows.toString()).commit());
        assertEquals("sin_abrir", resumen().getString("estado"));
        assertFalse(aperturas.abrir(LocalDate.now().minusDays(1), BigDecimal.ZERO));
        assertTrue(abrir("5.00"));
        assertEquals(500, resumen().getLong("esperadoCentavos"));
    }

    @Test public void cambiosDeTotalesIncluyendoConteoImpidenGuardarConfirmacionVieja() throws Exception {
        assertTrue(abrir("10.00"));
        JSONObject viejo = resumen();
        assertTrue(atenciones.registrarActual(transferencia, "Cliente", "transferencia", 2000));
        assertFalse(cierres.guardar(LocalDate.now(), new BigDecimal("10.00"), viejo));
        assertTrue(cierres.guardar(LocalDate.now(), new BigDecimal("8.00"), resumen()));
        JSONObject cierre = cierres.historialActual().getJSONObject(0);
        assertEquals(new BigDecimal("-2.00"), new BigDecimal(cierre.getString("entregado"))
                .subtract(new BigDecimal(cierre.getString("esperado"))));
        entrar(otro);
        assertTrue(abrir("10.00"));
        assertTrue(cierres.guardar(LocalDate.now(), new BigDecimal("12.00"), resumen()));
        cierre = cierres.historialActual().getJSONObject(0);
        assertEquals(new BigDecimal("2.00"), new BigDecimal(cierre.getString("entregado"))
                .subtract(new BigDecimal(cierre.getString("esperado"))));
    }

    @Test public void incluyeServiciosExistentesSinInventarFondosHistoricos() throws Exception {
        assertTrue(abrir("0.00"));
        assertTrue(atenciones.registrarActual(efectivo, "Cliente", "efectivo", 3000));
        assertTrue(context.getSharedPreferences("aperturas", 0).edit().clear().commit());
        assertEquals("sin_abrir", resumen().getString("estado"));
        assertTrue(abrir("10.00"));
        assertEquals(4000, resumen().getLong("esperadoCentavos"));
        JSONObject viejo = new JSONObject().put("fecha", LocalDate.now().minusDays(1).toString())
                .put("empleadoId", empleado).put("empleado", "Empleado Uno").put("tipo", "efectivo")
                .put("atenciones", 1).put("esperado", "7.00").put("entregado", "8.00");
        SharedPreferences prefs = context.getSharedPreferences("cierres", 0);
        assertTrue(prefs.edit().putString("historial", new JSONArray().put(viejo).toString()).commit());
        JSONObject row = cierres.historialActual().getJSONObject(0);
        assertEquals("7.00", row.getString("esperado"));
        assertFalse(row.has("aperturaCentavos"));
        assertFalse(row.has("transferencias"));
        assertEquals(700, cierres.resumenActual(LocalDate.now().minusDays(1)).getLong("esperadoCentavos"));
    }

    @Test public void sesionInactivaYDatosDanadosNoPermitenAbrirNiSobrescribir() throws Exception {
        assertTrue(new EmpleadosStore(context).setActive(empleado, false));
        assertFalse(abrir("10.00"));
        assertTrue(new EmpleadosStore(context).setActive(empleado, true));
        SharedPreferences prefs = context.getSharedPreferences("aperturas", 0);
        assertTrue(prefs.edit().putString("registros", "datos dañados").commit());
        try { abrir("10.00"); fail("Debe conservar datos dañados"); } catch (JSONException expected) { }
        assertEquals("datos dañados", prefs.getString("registros", ""));
    }
}

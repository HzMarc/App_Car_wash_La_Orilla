package com.example.car_wash_la_orilla;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.view.View;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/** Pruebas de las pantallas reales; restaura los datos de la instalación al terminar. */
@RunWith(AndroidJUnit4.class)
public class PerfilCierreFlowTest {
    private final Map<String, Map<String, ?>> respaldo = new HashMap<>();
    private Context context;
    private ActivityScenario<?> scenario;
    private String carlos, ana, servicio, moto;

    @Before public void preparar() throws Exception {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        for (String name : new String[]{"empleados", "cierres", "catalogo_servicios", "atenciones", "sesion", "aperturas"}) {
            SharedPreferences prefs = context.getSharedPreferences(name, 0);
            respaldo.put(name, new HashMap<>(prefs.getAll()));
            assertTrue(prefs.edit().clear().commit());
        }
        EmpleadosStore empleados = new EmpleadosStore(context);
        assertTrue(empleados.save(null, "Carlos Mendoza", true));
        assertTrue(empleados.save(null, "Ana Gómez", true));
        carlos = empleados.getAll().get(0).id;
        ana = empleados.getAll().get(1).id;
        assertTrue(context.getSharedPreferences("empleados", 0).edit().putBoolean("ejemplos_v1_cargados", true).commit());
        assertTrue(context.getSharedPreferences("cierres", 0).edit().putBoolean("ejemplos_v1_cargados", true)
                .putBoolean("historial_empleados_v2_cargado", true).commit());
        assertTrue(context.getSharedPreferences("catalogo_servicios", 0).edit().putBoolean("catalogo_propuesta_v1", true).commit());
        assertTrue(context.getSharedPreferences("atenciones", 0).edit().putString("registros", "[]").commit());
        assertTrue(DatosEjemplo.cargar(context));
        CatalogoStore catalogo = new CatalogoStore(context);
        assertTrue(catalogo.save(null, "Lavado de prueba", "carro", 500));
        assertTrue(catalogo.save(null, "Moto de prueba", "moto", 300));
        servicio = catalogo.getAll().get(0).id;
        moto = catalogo.getAll().get(1).id;
    }

    @SuppressWarnings("unchecked")
    @After public void restaurar() throws Exception {
        if (scenario != null) scenario.close();
        for (Map.Entry<String, Map<String, ?>> archivo : respaldo.entrySet()) {
            SharedPreferences.Editor editor = context.getSharedPreferences(archivo.getKey(), 0).edit().clear();
            for (Map.Entry<String, ?> entry : archivo.getValue().entrySet()) {
                Object value = entry.getValue();
                if (value instanceof String) editor.putString(entry.getKey(), (String) value);
                else if (value instanceof Boolean) editor.putBoolean(entry.getKey(), (Boolean) value);
                else if (value instanceof Long) editor.putLong(entry.getKey(), (Long) value);
                else if (value instanceof Integer) editor.putInt(entry.getKey(), (Integer) value);
                else if (value instanceof Float) editor.putFloat(entry.getKey(), (Float) value);
                else if (value instanceof Set) editor.putStringSet(entry.getKey(), (Set<String>) value);
            }
            assertTrue(editor.commit());
        }
    }

    private void entrarCarlos() throws Exception {
        assertTrue(new SesionStore(context).iniciar(new EmpleadosStore(context).find(carlos)));
        scenario = ActivityScenario.launch(MainActivity.class);
    }

    @Test public void loginRegistroCierrePersonalYRecreacion() throws Exception {
        // Ana tiene un cierre que Carlos no debe ver.
        assertTrue(new SesionStore(context).iniciar(new EmpleadosStore(context).find(ana)));
        AtencionesStore store = new AtencionesStore(context);
        assertTrue(new AperturasStore(context).abrir(java.time.LocalDate.now(), java.math.BigDecimal.ZERO));
        assertTrue(new CierresStore(context).guardar(java.time.LocalDate.now(), new java.math.BigDecimal("0.00"),
                new CierresStore(context).resumenActual(java.time.LocalDate.now())));
        context.getSharedPreferences("sesion", 0).edit().clear().commit();
        scenario = ActivityScenario.launch(LoginActivity.class);
        onView(withId(R.id.login_user)).perform(typeText("Carlos Mendoza"));
        onView(withId(R.id.login_password)).perform(typeText("carlos123"), closeSoftKeyboard());
        onView(withId(R.id.login_entrar)).perform(click());
        onView(withId(R.id.nav_perfil)).check(matches(withText("Perfil")));
        onView(withId(R.id.nav_cierre)).check(matches(withText("Caja")));
        onView(withText("Empleados")).check(doesNotExist());
        onView(withId(R.id.inicio_esperado)).check(matches(withText("—")));
        scenario.close();
        scenario = ActivityScenario.launch(MainActivity.class);
        capturar("inicio-sin-abrir");
        onView(withId(R.id.nav_registrar)).perform(click());
        onView(withId(R.id.botonGuardarAtencion)).check(matches(not(isEnabled())));
        onView(withId(R.id.registrar_ir_inicio)).perform(scrollTo(), click());
        onView(withId(R.id.inicio_caja_accion)).perform(scrollTo(), click());
        onView(withId(R.id.apertura_monto)).check(matches(withText("")));
        onView(withId(R.id.apertura_monto)).perform(typeText("10.00"), closeSoftKeyboard());
        onView(withText("Continuar")).perform(click());
        onView(withText("Confirmar apertura")).perform(click());
        onView(withId(R.id.inicio_caja_estado)).check(matches(withText("Abierta")));
        onView(withId(R.id.inicio_esperado)).check(matches(withText("$10.00")));
        capturar("inicio-caja-abierta");
        onView(withId(R.id.nav_registrar)).perform(click());
        onView(withId(R.id.registrar_empleado)).check(matches(withText("Empleado: Carlos Mendoza")));
        onView(withId(R.id.campoCliente)).perform(scrollTo(), typeText("Cliente prueba"), closeSoftKeyboard());
        onView(withId(R.id.campoMontoRecibido)).perform(scrollTo(), typeText("10.00"), closeSoftKeyboard());
        onView(withId(R.id.botonGuardarAtencion)).perform(scrollTo(), click());
        onView(withId(R.id.botonDialogoGuardar)).perform(click());
        assertEquals(carlos, store.getAll().get(0).empleadoId);
        assertEquals(500, store.getAll().get(0).precioCentavos); // Cambio de $5, no $10 en caja.
        assertTrue(store.registrarActual(moto, "Cliente transferencia", "transferencia", 300));
        onView(withId(R.id.nav_cierre)).perform(click());
        onView(withId(R.id.cierre_employee)).check(matches(withText("Carlos Mendoza")));
        onView(withId(R.id.cierre_expected)).check(matches(withText("$15.00")));
        onView(withId(R.id.caja_fondo)).check(matches(withText("$10.00")));
        onView(withId(R.id.caja_transferencias)).check(matches(withText("1")));
        onView(withId(R.id.cierre_count)).check(matches(withText("1")));
        onView(withId(R.id.cierre_delivered)).perform(scrollTo(), typeText("15.00"), closeSoftKeyboard());
        onView(withId(R.id.cierre_save)).perform(click());
        onView(withId(R.id.history_count)).check(matches(withText(containsString("Carlos Mendoza"))));
        onView(withText(containsString("Ana Gómez"))).check(doesNotExist());
        assertEquals(1, new CierresStore(context).historialActual().length());
        onView(withId(R.id.history_detail)).check(matches(withText(containsString("Entregado $15.00"))));
        onView(withId(R.id.history_detail)).perform(click());
        onView(withText(containsString("Cantidad de transferencias: 1"))).check(matches(isDisplayed()));
        onView(withText("Cerrar")).perform(click());
        onView(withId(R.id.nav_inicio)).perform(click());
        onView(withId(R.id.inicio_caja_estado)).check(matches(withText("Caja cerrada por hoy")));
        onView(withId(R.id.inicio_esperado)).check(matches(withText("$15.00")));
        capturar("inicio-caja-cerrada");
        // La ActivityScenario del login terminó; abrir Main otra vez comprueba la sesión persistida.
        scenario.close();
        scenario = ActivityScenario.launch(MainActivity.class);
        onView(withId(R.id.nav_perfil)).perform(click());
        onView(withId(R.id.perfil_nombre)).check(matches(withText("Carlos Mendoza")));
        scenario.recreate();
        onView(withId(R.id.perfil_user)).check(matches(withText("Carlos Mendoza")));
        capturar("perfil-empleado");
        onView(withId(R.id.nav_cierre)).perform(click());
        onView(withId(R.id.cierre_save)).check(matches(not(isEnabled())));
        capturar("cierre-efectivo");
    }

    @Test public void aperturaValidaMontoConservaConfirmacionYFuncionaEnPantallaEstrecha() throws Exception {
        entrarCarlos();
        scenario.onActivity(activity -> {
            float d = activity.getResources().getDisplayMetrics().density;
            activity.getWindow().setLayout((int) (320 * d), (int) (480 * d));
        });
        onView(withId(R.id.inicio_caja_accion)).perform(scrollTo(), click());
        onView(withId(R.id.apertura_monto)).check(matches(withText("")));
        onView(withId(R.id.apertura_monto)).perform(typeText("1.234"), closeSoftKeyboard());
        onView(withText("Continuar")).perform(click());
        assertNull(new AperturasStore(context).buscar(carlos, java.time.LocalDate.now()));
        onView(withId(R.id.apertura_monto)).perform(replaceText("0.00"), closeSoftKeyboard());
        onView(withText("Continuar")).perform(click());
        scenario.recreate();
        onView(withText("Confirmar apertura")).check(matches(isDisplayed()));
        onView(withText("Editar monto")).perform(click());
        onView(withId(R.id.apertura_monto)).check(matches(withText("0.00")));
        onView(withText("Continuar")).perform(click());
        onView(withText("Confirmar apertura")).perform(click());
        onView(withId(R.id.inicio_caja_estado)).check(matches(withText("Abierta")));
        scenario.onActivity(activity -> {
            float d = activity.getResources().getDisplayMetrics().density;
            activity.getWindow().setLayout((int) (320 * d), (int) (480 * d));
        });
        onView(withId(R.id.botonRegistrarServicio)).perform(scrollTo());
        capturar("inicio-pantalla-estrecha");
        assertEquals(0, new AperturasStore(context).buscar(carlos, java.time.LocalDate.now()).getLong("montoCentavos"));
    }

    @Test public void historialDemoMantieneTarjetasCompactasYDetalleAlTocar() throws Exception {
        assertTrue(context.getSharedPreferences("cierres", 0).edit().remove("historial_empleados_v2_cargado").commit());
        assertTrue(DatosEjemplo.cargar(context));
        entrarCarlos();
        onView(withId(R.id.nav_cierre)).perform(click());
        onView(withText("Historial")).perform(click());
        // El primer cierre tiene diseño compacto; el desglose aparece únicamente al abrirlo.
        onView(detalleMasReciente()).check(matches(withText(containsString("Entregado"))));
        onView(withText(containsString("Cantidad de transferencias:"))).check(doesNotExist());
        capturar("historial-caja-compacto");
        onView(detalleMasReciente()).perform(click());
        onView(withText(containsString("Cantidad de transferencias:"))).check(matches(isDisplayed()));
        onView(withText(containsString("Registro de ejemplo"))).check(matches(isDisplayed()));
        onView(withText("Cerrar")).perform(click());
        assertEquals(6, new CierresStore(context).historialActual().length());
    }

    @Test public void qrSoloEnTransferenciaSePuedeReabrirYConservaMetodoAlRecrear() throws Exception {
        entrarCarlos();
        onView(withId(R.id.nav_perfil)).perform(click());
        onView(withText(R.string.transferencia_qr_title)).check(doesNotExist());
        onView(withId(R.id.transferencia_qr)).check(doesNotExist());
        onView(withId(R.id.nav_registrar)).perform(click());
        onView(withId(R.id.transferencia_qr)).check(doesNotExist());
        onView(withText("Transferencia")).perform(scrollTo(), click());
        onView(withId(R.id.transferencia_qr)).check(matches(isDisplayed()));
        onView(withText("Seleccionar QR")).check(doesNotExist());
        onView(withText("Reemplazar QR")).check(doesNotExist());
        onView(withText(containsString("No permite pagos reales"))).check(matches(isDisplayed()));
        scenario.recreate();
        onView(withId(R.id.transferencia_qr)).check(matches(isDisplayed()));
        onView(withId(R.id.transferencia_qr_cerrar)).perform(scrollTo(), click());
        onView(withId(R.id.contenedorEfectivo)).check(matches(withEffectiveVisibility(Visibility.GONE)));
        onView(withText("Transferencia")).perform(scrollTo(), click());
        onView(withId(R.id.transferencia_qr)).check(matches(isDisplayed()));
        onView(withId(R.id.transferencia_qr_cerrar)).perform(scrollTo(), click());
        onView(withText("Efectivo")).perform(scrollTo(), click());
        onView(withId(R.id.transferencia_qr)).check(doesNotExist());
        onView(withId(R.id.contenedorEfectivo)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
        Bitmap primero = new QrNegocioStore(context).cargar();
        assertTrue(new SesionStore(context).iniciar(new EmpleadosStore(context).find(ana)));
        scenario.close();
        scenario = ActivityScenario.launch(MainActivity.class);
        onView(withId(R.id.nav_perfil)).perform(click());
        onView(withId(R.id.perfil_nombre)).check(matches(withText("Ana Gómez")));
        onView(withText(R.string.transferencia_qr_title)).check(doesNotExist());
        onView(withId(R.id.nav_registrar)).perform(click());
        onView(withText("Transferencia")).perform(scrollTo(), click());
        onView(withId(R.id.transferencia_qr)).check(matches(isDisplayed()));
        Bitmap segundo = new QrNegocioStore(context).cargar();
        assertTrue(primero.sameAs(segundo));
        primero.recycle();
        segundo.recycle();
    }

    @Test public void serviciosFiltraEmpleadoBusquedaYConservaAnuladosAlRecrear() throws Exception {
        AtencionesStore store = new AtencionesStore(context);
        assertTrue(new SesionStore(context).iniciar(new EmpleadosStore(context).find(carlos)));
        assertTrue(new AperturasStore(context).abrir(java.time.LocalDate.now(), java.math.BigDecimal.ZERO));
        assertTrue(store.registrarActual(servicio, "Cliente Carlos", "efectivo", 500));
        assertTrue(store.registrarActual(moto, "Cliente Carlos", "transferencia", 300));
        assertTrue(store.cambiar(store.getAll().stream().filter(a -> moto.equals(a.servicioId)).findFirst().get().id,
                "Duplicado", null));
        assertTrue(new SesionStore(context).iniciar(new EmpleadosStore(context).find(ana)));
        assertTrue(new AperturasStore(context).abrir(java.time.LocalDate.now(), java.math.BigDecimal.ZERO));
        assertTrue(store.registrarActual(servicio, "Cliente Ana", "efectivo", 500));
        entrarCarlos();
        onView(withId(R.id.nav_servicios)).perform(click());
        comprobarListaPersonal("Carlos Mendoza", "Ana Gómez", 2);
        onView(withId(R.id.contenedorListaServicios)).check(matches(hasDescendant(withText(containsString(
                context.getString(R.string.atencion_anulada))))));
        scenario.onActivity(activity -> ((androidx.appcompat.widget.SearchView) activity.findViewById(
                R.id.vistaBusqueda)).setQuery("Ana", false));
        onView(withText("No se encontraron resultados para la búsqueda")).check(matches(isDisplayed()));
        onView(withId(R.id.textoDetalleAtencion)).check(matches(withText("Sin selección")));
        scenario.onActivity(activity -> ((androidx.appcompat.widget.SearchView) activity.findViewById(
                R.id.vistaBusqueda)).setQuery("Moto", false));
        comprobarListaPersonal("Carlos Mendoza", "Ana Gómez", 1);
        scenario.onActivity(activity -> ((androidx.appcompat.widget.SearchView) activity.findViewById(
                R.id.vistaBusqueda)).setQuery("", false));
        scenario.recreate();
        comprobarListaPersonal("Carlos Mendoza", "Ana Gómez", 2);
        scenario.close();
        assertTrue(new SesionStore(context).iniciar(new EmpleadosStore(context).find(ana)));
        scenario = ActivityScenario.launch(MainActivity.class);
        onView(withId(R.id.nav_servicios)).perform(click());
        comprobarListaPersonal("Ana Gómez", "Carlos Mendoza", 1);
        onView(withId(R.id.botonAnular)).perform(click());
        onView(withId(R.id.campoMotivo)).perform(typeText("Duplicado"), closeSoftKeyboard());
        onView(withId(R.id.botonConfirmarMotivo)).perform(click());
        assertTrue(store.getAll().stream().filter(a -> ana.equals(a.empleadoId)).findFirst().get().anulada);
        assertEquals(500, store.resumenEfectivo(carlos, java.time.LocalDate.now()).getLong("esperadoCentavos"));
    }

    private void comprobarListaPersonal(String propio, String ajeno, int cantidad) {
        onView(withId(R.id.contenedorListaServicios)).check(matches(allOf(
                hasDescendant(withText(containsString(propio))),
                not(hasDescendant(withText(containsString(ajeno)))))));
        scenario.onActivity(activity -> assertEquals(cantidad, ((android.widget.LinearLayout)
                activity.findViewById(R.id.contenedorListaServicios)).getChildCount()));
    }

    @Test public void serviciosSinSesionRegresaAlLoginAntesDeAnular() throws Exception {
        entrarCarlos();
        onView(withId(R.id.nav_servicios)).perform(click());
        String original = context.getSharedPreferences("atenciones", 0).getString("registros", "");
        assertTrue(context.getSharedPreferences("sesion", 0).edit().clear().commit());
        onView(withId(R.id.botonAnular)).perform(click());
        esperar(R.id.login_entrar, isDisplayed());
        assertEquals(original, context.getSharedPreferences("atenciones", 0).getString("registros", ""));
    }

    @Test public void empleadoInactivoDuranteCorreccionRegresaAlLoginSinGuardar() throws Exception {
        assertTrue(new SesionStore(context).iniciar(new EmpleadosStore(context).find(carlos)));
        assertTrue(new AperturasStore(context).abrir(java.time.LocalDate.now(), java.math.BigDecimal.ZERO));
        assertTrue(new AtencionesStore(context).registrarActual(servicio, "Cliente", "efectivo", 500));
        String original = context.getSharedPreferences("atenciones", 0).getString("registros", "");
        entrarCarlos();
        onView(withId(R.id.nav_servicios)).perform(click());
        onView(withId(R.id.botonCorregir)).perform(click());
        assertTrue(new EmpleadosStore(context).setActive(carlos, false));
        onView(withId(R.id.botonConfirmarMotivo)).perform(click());
        esperar(R.id.login_entrar, isDisplayed());
        assertEquals(original, context.getSharedPreferences("atenciones", 0).getString("registros", ""));
    }

    @Test public void sesionInactivaRegresaAlLogin() throws Exception {
        entrarCarlos();
        assertTrue(new EmpleadosStore(context).setActive(carlos, false));
        scenario.onActivity(Activity::recreate);
        esperar(R.id.login_entrar, isDisplayed());
        long limite = SystemClock.uptimeMillis() + 10000;
        while (scenario.getState() != androidx.lifecycle.Lifecycle.State.DESTROYED
                && SystemClock.uptimeMillis() < limite) SystemClock.sleep(50);
    }

    private org.hamcrest.Matcher<View> detalleMasReciente() {
        String fecha = java.time.LocalDate.now().minusDays(2).format(
                java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy", new java.util.Locale("es", "SV")));
        return allOf(withId(R.id.history_detail), withParent(hasDescendant(allOf(withId(R.id.history_date), withText(fecha)))));
    }

    private void esperar(int id, Matcher<View> condicion) {
        long limite = SystemClock.uptimeMillis() + 10000;
        Throwable ultimo = null;
        do {
            try { onView(withId(id)).check(matches(condicion)); return; }
            catch (AssertionError | androidx.test.espresso.NoMatchingViewException
                    | androidx.test.espresso.NoMatchingRootException error) { ultimo = error; }
            SystemClock.sleep(50);
        } while (SystemClock.uptimeMillis() < limite);
        throw new AssertionError("No se alcanzó el estado esperado", ultimo);
    }

    private void capturar(String nombre) {
        scenario.onActivity(activity -> {
            View view = activity.getWindow().getDecorView();
            Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
            view.draw(new Canvas(bitmap));
            File carpeta = context.getExternalFilesDir("verification");
            try (FileOutputStream output = new FileOutputStream(new File(carpeta, nombre + ".png"))) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
            } catch (Exception error) { throw new AssertionError(error); }
            bitmap.recycle();
        });
    }
}

package com.example.car_wash_la_orilla;

import android.content.Context;
import android.graphics.Bitmap;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class QrNegocioStoreTest {
    @Test public void qrIncluidoSiempreDisponibleSinConfiguracion() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Bitmap primero = new QrNegocioStore(context).cargar();
        Bitmap segundo = new QrNegocioStore(context).cargar();
        assertNotNull(primero);
        assertNotNull(segundo);
        assertEquals(primero.getWidth(), primero.getHeight());
        assertTrue(primero.sameAs(segundo));
        primero.recycle();
        segundo.recycle();
    }
}

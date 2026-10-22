package com.example.car_wash_la_orilla;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/** QR fijo de demostración, incluido en la app y común a todos los empleados. */
final class QrNegocioStore {
    private final Context context;
    QrNegocioStore(Context context) { this.context = context.getApplicationContext(); }

    Bitmap cargar() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        return BitmapFactory.decodeResource(context.getResources(), R.drawable.qr_transferencias_demo, options);
    }
}

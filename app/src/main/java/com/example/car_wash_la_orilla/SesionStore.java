package com.example.car_wash_la_orilla;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import org.json.JSONException;

/** Guarda solo el identificador; vuelve a validar el empleado en cada operación. */
final class SesionStore {
    private final Context context;
    SesionStore(Context context) { this.context = context.getApplicationContext(); }

    boolean iniciar(EmpleadosStore.Empleado empleado) {
        return empleado != null && empleado.activo && context.getSharedPreferences("sesion", 0)
                .edit().putString("empleadoId", empleado.id).commit();
    }

    EmpleadosStore.Empleado actual() throws JSONException {
        String id = context.getSharedPreferences("sesion", 0).getString("empleadoId", null);
        EmpleadosStore.Empleado empleado = id == null ? null : new EmpleadosStore(context).find(id);
        return empleado != null && empleado.activo ? empleado : null;
    }

    static EmpleadosStore.Empleado requerir(Activity activity) {
        try {
            EmpleadosStore.Empleado empleado = new SesionStore(activity).actual();
            if (empleado != null) return empleado;
        } catch (JSONException ignored) { }
        if (!activity.isFinishing()) {
            activity.startActivity(new Intent(activity, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            activity.finish();
        }
        return null;
    }
}

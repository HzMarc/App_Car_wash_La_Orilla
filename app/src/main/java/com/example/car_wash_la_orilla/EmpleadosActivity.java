package com.example.car_wash_la_orilla;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** Contenedor de la vista 6. */
public class EmpleadosActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_empleados);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.empleados_container), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets keyboard = insets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, keyboard.bottom));
            return insets;
        });
        if (state == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.empleados_container, new EmpleadosFragment()).commit();
        }
    }
}

package com.example.car_wash_la_orilla;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** Contenedor compatible para abrir el cierre desde otras actividades. */
public class CierreActivity extends AppCompatActivity {
    public static final String EXTRA_RESUMENES = CierreFragment.EXTRA_RESUMENES;
    public static final String EXTRA_FECHA = CierreFragment.EXTRA_FECHA;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cierre);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cierre_root), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        if (state == null) getSupportFragmentManager().beginTransaction()
                .replace(R.id.cierre_root, new CierreFragment()).commit();
    }
}
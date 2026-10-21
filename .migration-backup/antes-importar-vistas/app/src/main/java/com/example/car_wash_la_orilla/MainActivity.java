package com.example.car_wash_la_orilla;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_cierre).setOnClickListener(v ->
                startActivity(new Intent(this, CierreActivity.class)));
        findViewById(R.id.btn_catalogo).setOnClickListener(v ->
                startActivity(new Intent(this, CatalogoActivity.class)));
        findViewById(R.id.btn_empleados).setOnClickListener(v ->
                startActivity(new Intent(this, EmpleadosActivity.class)));
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}

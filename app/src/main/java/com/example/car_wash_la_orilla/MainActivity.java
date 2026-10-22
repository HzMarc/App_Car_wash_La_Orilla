package com.example.car_wash_la_orilla;

import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity {

    private TextView textoNavInicio;
    private TextView textoNavRegistrar;
    private TextView textoNavServicios;
    private TextView textoNavCierre;
    private TextView textoNavCatalogo;
    private TextView textoNavPerfil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (SesionStore.requerir(this) == null) return;
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        boolean catalogoCargado = CatalogoInicial.cargar(this);
        if (!DatosEjemplo.cargar(this) || !catalogoCargado) {
            Toast.makeText(this, R.string.ejemplos_error, Toast.LENGTH_LONG).show();
        }
        try {
            if (!new AtencionesStore(this).cargarEjemplos()) throw new IllegalStateException();
        } catch (org.json.JSONException | IllegalStateException error) {
            Toast.makeText(this, R.string.atenciones_error, Toast.LENGTH_LONG).show();
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.principal), (view, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });

        textoNavInicio = findViewById(R.id.nav_inicio);
        textoNavRegistrar = findViewById(R.id.nav_registrar);
        textoNavServicios = findViewById(R.id.nav_servicios);
        textoNavCierre = findViewById(R.id.nav_cierre);
        textoNavCatalogo = findViewById(R.id.nav_catalogo);
        textoNavPerfil = findViewById(R.id.nav_perfil);

        if (savedInstanceState == null) {
            mostrarInicio();
        } else {
            Fragment fragmento = getSupportFragmentManager().findFragmentById(R.id.contenedor_fragmento);
            if (fragmento instanceof RegistrarServicioFragment) {
                resaltarPestana(textoNavRegistrar);
            } else if (fragmento instanceof ServiciosRegistradosFragment) {
                resaltarPestana(textoNavServicios);
            } else if (fragmento instanceof CierreFragment) {
                resaltarPestana(textoNavCierre);
            } else if (fragmento instanceof CatalogoFragment) {
                resaltarPestana(textoNavCatalogo);
            } else if (fragmento instanceof PerfilFragment) {
                resaltarPestana(textoNavPerfil);
            } else {
                resaltarPestana(textoNavInicio);
            }
        }

        textoNavInicio.setOnClickListener(v -> mostrarInicio());
        textoNavRegistrar.setOnClickListener(v -> mostrarRegistrar());
        textoNavServicios.setOnClickListener(v -> mostrarServicios());
        textoNavCierre.setOnClickListener(v -> mostrarCierre());
        textoNavCatalogo.setOnClickListener(v -> mostrarCatalogo());
        textoNavPerfil.setOnClickListener(v -> mostrarPerfil());
    }

    @Override protected void onResume() {
        super.onResume();
        if (!isFinishing()) SesionStore.requerir(this);
    }

    public void mostrarCierre() {
        cargarFragmento(new CierreFragment());
        resaltarPestana(textoNavCierre);
    }

    public void mostrarCatalogo() {
        cargarFragmento(new CatalogoFragment());
        resaltarPestana(textoNavCatalogo);
    }

    public void mostrarPerfil() {
        cargarFragmento(new PerfilFragment());
        resaltarPestana(textoNavPerfil);
    }

    public void mostrarInicio() {
        cargarFragmento(new InicioFragment());
        resaltarPestana(textoNavInicio);
    }

    public void mostrarRegistrar() {
        cargarFragmento(new RegistrarServicioFragment());
        resaltarPestana(textoNavRegistrar);
    }

    public void mostrarServicios() {
        cargarFragmento(new ServiciosRegistradosFragment());
        resaltarPestana(textoNavServicios);
    }

    private void cargarFragmento(Fragment fragmento) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.contenedor_fragmento, fragmento)
                .commit();
    }

    private void resaltarPestana(TextView pestanaSeleccionada) {
        restablecerPestana(textoNavInicio);
        restablecerPestana(textoNavRegistrar);
        restablecerPestana(textoNavServicios);
        restablecerPestana(textoNavCierre);
        restablecerPestana(textoNavCatalogo);
        restablecerPestana(textoNavPerfil);

        pestanaSeleccionada.setTextColor(ContextCompat.getColor(this, R.color.app_black));
        pestanaSeleccionada.setTypeface(pestanaSeleccionada.getTypeface(), Typeface.BOLD);
        pestanaSeleccionada.setSelected(true);
    }

    private void restablecerPestana(TextView pestana) {
        pestana.setTextColor(ContextCompat.getColor(this, R.color.app_text_secondary));
        pestana.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
        pestana.setSelected(false);
    }
}

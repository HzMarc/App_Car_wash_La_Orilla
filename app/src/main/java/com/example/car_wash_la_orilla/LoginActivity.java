package com.example.car_wash_la_orilla;

import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText userInput, passwordInput;
    private TextInputLayout userLayout, passwordLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_root), (view, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });
        userInput = findViewById(R.id.login_user);
        passwordInput = findViewById(R.id.login_password);
        userLayout = findViewById(R.id.login_user_layout);
        passwordLayout = findViewById(R.id.login_password_layout);
        Button entrar = findViewById(R.id.login_entrar);
        entrar.setOnClickListener(v -> iniciarSesion());
        passwordInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE) return false;
            iniciarSesion();
            return true;
        });
    }

    private void iniciarSesion() {
        userLayout.setError(null);
        passwordLayout.setError(null);
        String user = userInput.getText() == null ? "" : userInput.getText().toString().trim();
        String password = passwordInput.getText() == null ? "" : passwordInput.getText().toString();
        if (user.isEmpty()) {
            userLayout.setError(getString(R.string.login_user_required));
            userInput.requestFocus();
            return;
        }
        try {
            if (!DatosEjemplo.cargar(this)) throw new IllegalStateException();
            EmpleadosStore.Empleado empleado = new EmpleadosStore(this).autenticar(user, password);
            if (empleado == null) {
                passwordLayout.setError(getString(R.string.login_invalid));
                return;
            }
            if (!new SesionStore(this).iniciar(empleado)) throw new IllegalStateException();
            startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        } catch (JSONException | IllegalStateException error) {
            Toast.makeText(this, R.string.login_error, Toast.LENGTH_LONG).show();
        }
    }
}

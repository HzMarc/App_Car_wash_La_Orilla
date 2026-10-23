package com.example.car_wash_la_orilla;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.view.ViewCompat;
import java.util.Locale;

public class PerfilFragment extends Fragment {
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup parent, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_perfil, parent, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        ViewCompat.setAccessibilityHeading(view.findViewById(R.id.perfil_titulo), true);
        ViewCompat.setAccessibilityHeading(view.findViewById(R.id.perfil_cuenta_titulo), true);
        ViewCompat.setAccessibilityHeading(view.findViewById(R.id.perfil_accesos_titulo), true);
        view.findViewById(R.id.perfil_ver_servicios).setOnClickListener(v -> {
            if (SesionStore.requerir(requireActivity()) != null && getActivity() instanceof MainActivity)
                ((MainActivity) requireActivity()).mostrarServicios();
        });
        view.findViewById(R.id.perfil_ver_caja).setOnClickListener(v -> {
            if (SesionStore.requerir(requireActivity()) != null && getActivity() instanceof MainActivity)
                ((MainActivity) requireActivity()).mostrarCierre();
        });
    }

    @Override public void onResume() {
        super.onResume();
        EmpleadosStore.Empleado empleado = SesionStore.requerir(requireActivity());
        if (empleado == null) return;
        ((TextView) requireView().findViewById(R.id.perfil_nombre)).setText(empleado.nombre);
        ((TextView) requireView().findViewById(R.id.perfil_user)).setText(empleado.user);
        String[] partes = empleado.nombre.trim().split("\\s+");
        String iniciales = primeraLetra(partes[0]);
        if (partes.length > 1) iniciales += primeraLetra(partes[partes.length - 1]);
        ((TextView) requireView().findViewById(R.id.perfil_iniciales)).setText(iniciales.toUpperCase(Locale.getDefault()));
    }

    private String primeraLetra(String texto) {
        return texto.isEmpty() ? "" : texto.substring(0, texto.offsetByCodePoints(0, 1));
    }
}

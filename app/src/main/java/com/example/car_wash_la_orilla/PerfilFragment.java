package com.example.car_wash_la_orilla;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

public class PerfilFragment extends Fragment {
    private Bitmap qr;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup parent, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_perfil, parent, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        qr = new QrNegocioStore(requireContext()).cargar();
        ((ImageView) view.findViewById(R.id.perfil_qr)).setImageBitmap(qr);
        view.findViewById(R.id.perfil_qr).setOnClickListener(v -> ampliar());
    }

    @Override public void onResume() {
        super.onResume();
        EmpleadosStore.Empleado empleado = SesionStore.requerir(requireActivity());
        if (empleado == null) return;
        ((TextView) requireView().findViewById(R.id.perfil_nombre)).setText(empleado.nombre);
        ((TextView) requireView().findViewById(R.id.perfil_user)).setText(empleado.user);
    }

    @Override public void onDestroyView() {
        qr = null;
        super.onDestroyView();
    }

    private void ampliar() {
        if (qr == null || SesionStore.requerir(requireActivity()) == null) return;
        ImageView imagen = new ImageView(requireContext());
        imagen.setImageBitmap(qr);
        imagen.setAdjustViewBounds(true);
        imagen.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imagen.setBackgroundColor(android.graphics.Color.WHITE);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        imagen.setPadding(padding, padding, padding, padding);
        imagen.setContentDescription(getString(R.string.perfil_qr_title));
        new AlertDialog.Builder(requireContext()).setTitle(R.string.perfil_qr_title)
                .setView(imagen).setPositiveButton(R.string.perfil_qr_cerrar, null).show();
    }
}

package com.example.car_wash_la_orilla;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Typeface;
import android.text.TextUtils;
import com.google.android.material.card.MaterialCardView;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class InicioFragment extends Fragment {

    @Override public void onResume() {
        super.onResume();
        View vista = requireView();
        LocalDate hoy = LocalDate.now();
        ((TextView) vista.findViewById(R.id.textoFecha)).setText("Hoy · "
                + hoy.format(DateTimeFormatter.ofPattern("dd MMMM", new Locale("es", "SV"))));
        LinearLayout recientes = vista.findViewById(R.id.contenedorListaRecientes);
        recientes.removeAllViews();
        try {
            List<AtencionesStore.Atencion> atenciones = new ArrayList<>();
            long centavos = 0;
            int carros = 0, motos = 0;
            for (AtencionesStore.Atencion atencion : new AtencionesStore(requireContext()).delDia(hoy)) {
                if (atencion.anulada) continue;
                atenciones.add(atencion);
                centavos = Math.addExact(centavos, atencion.precioCentavos);
                if (atencion.vehiculo.equals("carro")) carros++; else motos++;
            }
            ((TextView) vista.findViewById(R.id.inicio_atenciones)).setText(String.valueOf(atenciones.size()));
            ((TextView) vista.findViewById(R.id.inicio_carros)).setText(String.valueOf(carros));
            ((TextView) vista.findViewById(R.id.inicio_motos)).setText(String.valueOf(motos));
            ((TextView) vista.findViewById(R.id.inicio_esperado)).setText("$" + BigDecimal.valueOf(centavos, 2).toPlainString());
            for (int i = 0; i < Math.min(3, atenciones.size()); i++) agregarReciente(recientes, atenciones.get(i));
            if (atenciones.isEmpty()) {
                TextView vacio = new TextView(requireContext());
                vacio.setText(R.string.inicio_sin_atenciones);
                vacio.setTextColor(requireContext().getColor(R.color.app_text_secondary));
                recientes.addView(vacio);
            }
        } catch (org.json.JSONException | ArithmeticException error) {
            for (int id : new int[] {R.id.inicio_atenciones, R.id.inicio_carros, R.id.inicio_motos, R.id.inicio_esperado}) {
                ((TextView) vista.findViewById(id)).setText("—");
            }
            Toast.makeText(requireContext(), R.string.atenciones_error, Toast.LENGTH_LONG).show();
        }
    }

    private void agregarReciente(LinearLayout lista, AtencionesStore.Atencion atencion) {
        float densidad = getResources().getDisplayMetrics().density;
        MaterialCardView tarjeta = new MaterialCardView(requireContext());
        tarjeta.setRadius(10 * densidad);
        tarjeta.setCardElevation(0);
        tarjeta.setCardBackgroundColor(requireContext().getColor(R.color.app_card_bg));
        tarjeta.setStrokeColor(requireContext().getColor(R.color.app_border));
        tarjeta.setStrokeWidth(Math.round(1.5f * densidad));
        LinearLayout.LayoutParams parametros = new LinearLayout.LayoutParams(-1, -2);
        parametros.bottomMargin = Math.round(8 * densidad);
        lista.addView(tarjeta, parametros);
        LinearLayout fila = new LinearLayout(requireContext());
        int padding = Math.round(12 * densidad);
        fila.setPadding(padding, padding, padding, padding);
        TextView nombre = new TextView(requireContext());
        nombre.setText(atencion.hora() + " - " + atencion.servicio);
        nombre.setTextColor(requireContext().getColor(R.color.app_black));
        nombre.setTextSize(14);
        nombre.setMaxLines(1);
        nombre.setEllipsize(TextUtils.TruncateAt.END);
        fila.addView(nombre, new LinearLayout.LayoutParams(0, -2, 1));
        TextView precio = new TextView(requireContext());
        precio.setText("$" + atencion.precio());
        precio.setTextColor(requireContext().getColor(R.color.app_black));
        precio.setTextSize(15);
        precio.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        fila.addView(precio);
        tarjeta.addView(fila);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View vista = inflater.inflate(R.layout.fragment_inicio, container, false);

        vista.findViewById(R.id.botonEmpleados).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).mostrarEmpleados();
            }
        });

        View botonRegistrarServicio = vista.findViewById(R.id.botonRegistrarServicio);
        if (botonRegistrarServicio != null) {
            botonRegistrarServicio.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).mostrarRegistrar();
                }
            });
        }

        return vista;
    }
}

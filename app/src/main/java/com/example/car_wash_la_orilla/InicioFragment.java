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
        actualizar();
    }

    private void actualizar() {
        if (getView() == null) return;
        EmpleadosStore.Empleado empleado = SesionStore.requerir(requireActivity());
        if (empleado == null) return;
        View vista = requireView();
        LocalDate hoy = LocalDate.now();
        ((TextView) vista.findViewById(R.id.textoFecha)).setText("Hoy · "
                + hoy.format(DateTimeFormatter.ofPattern("dd MMMM", new Locale("es", "SV"))));
        LinearLayout recientes = vista.findViewById(R.id.contenedorListaRecientes);
        recientes.removeAllViews();
        try {
            org.json.JSONObject caja = new CierresStore(requireContext()).resumenActual(hoy);
            String estado = caja.getString("estado");
            ((TextView) vista.findViewById(R.id.inicio_caja_estado)).setText("abierta".equals(estado)
                    ? R.string.caja_abierta : "cerrada".equals(estado) ? R.string.caja_cerrada : R.string.caja_sin_abrir);
            TextView detalle = vista.findViewById(R.id.inicio_caja_detalle);
            detalle.setVisibility(caja.has("aperturaCentavos") ? View.VISIBLE : View.GONE);
            if (caja.has("aperturaCentavos")) detalle.setText(getString(R.string.caja_apertura_detalle,
                    BigDecimal.valueOf(caja.getLong("aperturaCentavos"), 2).toPlainString(),
                    java.time.LocalDateTime.parse(caja.getString("fechaApertura")).format(DateTimeFormatter.ofPattern("HH:mm"))));
            ((TextView) vista.findViewById(R.id.inicio_caja_accion)).setText(
                    "sin_abrir".equals(estado) ? R.string.apertura_title : R.string.caja_ver);
            vista.findViewById(R.id.inicio_caja_accion).setEnabled(true);
            vista.findViewById(R.id.botonRegistrarServicio).setEnabled(!"cerrada".equals(estado));
            ((TextView) vista.findViewById(R.id.inicio_esperado)).setText("sin_abrir".equals(estado) ? "—"
                    : "$" + BigDecimal.valueOf(caja.getLong("esperadoCentavos"), 2).toPlainString());
            List<AtencionesStore.Atencion> atenciones = new ArrayList<>();
            int carros = 0, motos = 0;
            for (AtencionesStore.Atencion atencion : new AtencionesStore(requireContext()).delDia(hoy)) {
                if (atencion.anulada || !empleado.id.equals(atencion.empleadoId)) continue;
                atenciones.add(atencion);
                if (atencion.vehiculo.equals("carro")) carros++; else motos++;
            }
            ((TextView) vista.findViewById(R.id.inicio_atenciones)).setText(String.valueOf(atenciones.size()));
            ((TextView) vista.findViewById(R.id.inicio_carros)).setText(String.valueOf(carros));
            ((TextView) vista.findViewById(R.id.inicio_motos)).setText(String.valueOf(motos));
            for (int i = 0; i < Math.min(3, atenciones.size()); i++) agregarReciente(recientes, atenciones.get(i));
            if (atenciones.isEmpty()) {
                TextView vacio = new TextView(requireContext());
                vacio.setText(R.string.inicio_sin_atenciones);
                vacio.setTextColor(requireContext().getColor(R.color.app_text_secondary));
                recientes.addView(vacio);
            }
        } catch (org.json.JSONException | RuntimeException error) {
            for (int id : new int[] {R.id.inicio_atenciones, R.id.inicio_carros, R.id.inicio_motos, R.id.inicio_esperado})
                ((TextView) vista.findViewById(id)).setText("—");
            vista.findViewById(R.id.inicio_caja_accion).setEnabled(false);
            vista.findViewById(R.id.botonRegistrarServicio).setEnabled(false);
            Toast.makeText(requireContext(), R.string.atenciones_error, Toast.LENGTH_LONG).show();
        }
    }

    private void accionCaja(boolean registrar) {
        if (SesionStore.requerir(requireActivity()) == null) return;
        actualizar();
        try {
            String estado = new CierresStore(requireContext()).resumenActual(LocalDate.now()).getString("estado");
            if ("sin_abrir".equals(estado)) {
                if (registrar) Toast.makeText(requireContext(), R.string.caja_falta_apertura, Toast.LENGTH_LONG).show();
                if (getChildFragmentManager().findFragmentByTag("apertura") == null)
                    new AperturaDialogFragment().show(getChildFragmentManager(), "apertura");
            } else if (getActivity() instanceof MainActivity) {
                if (registrar && "abierta".equals(estado)) ((MainActivity) getActivity()).mostrarRegistrar();
                else ((MainActivity) getActivity()).mostrarCierre();
            }
        } catch (org.json.JSONException error) {
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

        vista.findViewById(R.id.botonRegistrarServicio).setOnClickListener(v -> accionCaja(true));
        vista.findViewById(R.id.inicio_caja_accion).setOnClickListener(v -> accionCaja(false));
        getChildFragmentManager().setFragmentResultListener("apertura_guardada", this, (key, data) -> actualizar());

        return vista;
    }
}

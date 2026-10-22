package com.example.car_wash_la_orilla;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

public class RegistrarServicioFragment extends Fragment {

    private LinearLayout contenedorServicios;
    private TextInputEditText campoCliente;
    private TextView textoPrecioTotal;

    private LinearLayout contenedorEfectivo;
    private TextInputEditText campoMontoRecibido;
    private TextView textoCambioCalculado;

    private boolean esCarroSeleccionado = true;
    private int indiceServicioSeleccionado = 0;
    private String metodoPagoSeleccionado = "Efectivo";

    private String[] serviciosCarro = {};
    private String[] preciosCarro = {};
    private String[] serviciosMoto = {};
    private String[] preciosMoto = {};
    private String[] idsCarro = {}, idsMoto = {};
    private MaterialButton botonGuardarAtencion;
    private boolean cajaAbierta;

    private void cargarCatalogos() {
        java.util.List<String> nombresCarro = new java.util.ArrayList<>();
        java.util.List<String> importesCarro = new java.util.ArrayList<>();
        java.util.List<String> nombresMoto = new java.util.ArrayList<>();
        java.util.List<String> importesMoto = new java.util.ArrayList<>();
        java.util.List<String> carroIds = new java.util.ArrayList<>();
        java.util.List<String> motoIds = new java.util.ArrayList<>();
        try {
            for (CatalogoStore.Servicio servicio : new CatalogoStore(requireContext()).getAll()) {
                if (!servicio.activo) continue;
                if (servicio.vehiculo.equals("carro")) {
                    nombresCarro.add(servicio.nombre);
                    carroIds.add(servicio.id);
                    importesCarro.add("$" + servicio.precio());
                } else {
                    nombresMoto.add(servicio.nombre);
                    motoIds.add(servicio.id);
                    importesMoto.add("$" + servicio.precio());
                }
            }
        } catch (org.json.JSONException error) {
            nombresCarro.clear();
            importesCarro.clear();
            nombresMoto.clear();
            importesMoto.clear();
            carroIds.clear();
            motoIds.clear();
            Toast.makeText(requireContext(), R.string.registrar_catalogos_error, Toast.LENGTH_LONG).show();
        }
        serviciosCarro = nombresCarro.toArray(new String[0]);
        preciosCarro = importesCarro.toArray(new String[0]);
        serviciosMoto = nombresMoto.toArray(new String[0]);
        preciosMoto = importesMoto.toArray(new String[0]);
        idsCarro = carroIds.toArray(new String[0]);
        idsMoto = motoIds.toArray(new String[0]);
        indiceServicioSeleccionado = 0;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View vista = inflater.inflate(R.layout.fragment_registrar_servicio, container, false);

        TabLayout vehiculos = vista.findViewById(R.id.registrar_vehiculo_tabs);
        contenedorServicios = vista.findViewById(R.id.contenedorServicios);
        campoCliente = vista.findViewById(R.id.campoCliente);
        textoPrecioTotal = vista.findViewById(R.id.textoPrecioTotal);
        botonGuardarAtencion = vista.findViewById(R.id.botonGuardarAtencion);
        TabLayout tabsMetodoPago = vista.findViewById(R.id.tabsMetodoPago);

        contenedorEfectivo = vista.findViewById(R.id.contenedorEfectivo);
        campoMontoRecibido = vista.findViewById(R.id.campoMontoRecibido);
        textoCambioCalculado = vista.findViewById(R.id.textoCambioCalculado);

        if (campoMontoRecibido != null) {
            campoMontoRecibido.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    calcularYMostrarCambio();
                }
                @Override public void afterTextChanged(android.text.Editable s) { }
            });
        }

        if (tabsMetodoPago != null) {
            TabLayout.Tab tabMetodo = tabsMetodoPago.getTabAt(metodoPagoSeleccionado.equalsIgnoreCase("Transferencia") ? 1 : 0);
            if (tabMetodo != null) tabMetodo.select();
            actualizarVisibilidadEfectivo();
            tabsMetodoPago.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override public void onTabSelected(TabLayout.Tab tab) {
                    metodoPagoSeleccionado = tab.getPosition() == 0 ? "Efectivo" : "Transferencia";
                    actualizarVisibilidadEfectivo();
                }
                @Override public void onTabUnselected(TabLayout.Tab tab) { }
                @Override public void onTabReselected(TabLayout.Tab tab) { }
            });
        }

        if (botonGuardarAtencion != null) {
            botonGuardarAtencion.setOnClickListener(v -> mostrarDialogoConfirmacionGuardado());
        }

        vista.findViewById(R.id.registrar_ir_inicio).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).mostrarInicio();
        });
        cargarCatalogos();
        TabLayout.Tab seleccionada = vehiculos.getTabAt(esCarroSeleccionado ? 0 : 1);
        if (seleccionada != null) seleccionada.select();
        vehiculos.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                esCarroSeleccionado = tab.getPosition() == 0;
                indiceServicioSeleccionado = 0;
                renderizarListaServicios();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { }
        });
        renderizarListaServicios();

        return vista;
    }

    private void renderizarListaServicios() {
        contenedorServicios.removeAllViews();

        String[] nombres = esCarroSeleccionado ? serviciosCarro : serviciosMoto;
        String[] precios = esCarroSeleccionado ? preciosCarro : preciosMoto;

        for (int i = 0; i < nombres.length; i++) {
            final int indice = i;
            boolean estaSeleccionado = (indice == indiceServicioSeleccionado);

            MaterialCardView tarjeta = new MaterialCardView(requireContext());
            LinearLayout.LayoutParams parametrosTarjeta = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            parametrosTarjeta.setMargins(0, 0, 0, dpAPx(10));
            tarjeta.setLayoutParams(parametrosTarjeta);
            tarjeta.setRadius(dpAPx(10));
            tarjeta.setCardElevation(0);

            if (estaSeleccionado) {
                tarjeta.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.app_card_selected));
                tarjeta.setStrokeColor(Color.parseColor("#9CA3AF"));
            } else {
                tarjeta.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.app_card_bg));
                tarjeta.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.app_border));
            }
            tarjeta.setStrokeWidth(dpAPx(1.5f));

            RelativeLayout contenedorRelativo = new RelativeLayout(requireContext());
            contenedorRelativo.setPadding(dpAPx(14), dpAPx(14), dpAPx(14), dpAPx(14));

            TextView textoNombre = new TextView(requireContext());
            RelativeLayout.LayoutParams parametrosNombre = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            parametrosNombre.addRule(RelativeLayout.ALIGN_PARENT_START);
            textoNombre.setLayoutParams(parametrosNombre);
            textoNombre.setText(nombres[i]);
            textoNombre.setTextColor(ContextCompat.getColor(requireContext(), R.color.app_black));
            textoNombre.setTextSize(15);
            textoNombre.setTypeface(Typeface.create("sans-serif-condensed", estaSeleccionado ? Typeface.BOLD : Typeface.NORMAL));

            TextView textoPrecio = new TextView(requireContext());
            RelativeLayout.LayoutParams parametrosPrecio = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            parametrosPrecio.addRule(RelativeLayout.ALIGN_PARENT_END);
            textoPrecio.setLayoutParams(parametrosPrecio);
            textoPrecio.setText(precios[i]);
            textoPrecio.setTextColor(ContextCompat.getColor(requireContext(), R.color.app_black));
            textoPrecio.setTextSize(15);
            textoPrecio.setTypeface(Typeface.create("sans-serif-condensed", estaSeleccionado ? Typeface.BOLD : Typeface.NORMAL));

            contenedorRelativo.addView(textoNombre);
            contenedorRelativo.addView(textoPrecio);
            tarjeta.addView(contenedorRelativo);

            tarjeta.setOnClickListener(v -> {
                indiceServicioSeleccionado = indice;
                renderizarListaServicios();
                actualizarMostrarPrecio();
            });

            contenedorServicios.addView(tarjeta);
        }

        actualizarMostrarPrecio();
    }

    private void actualizarMostrarPrecio() {
        String[] precios = esCarroSeleccionado ? preciosCarro : preciosMoto;
        botonGuardarAtencion.setEnabled(precios.length > 0 && cajaAbierta);
        if (precios.length == 0) {
            textoPrecioTotal.setText(R.string.registrar_sin_servicios);
            return;
        }
        if (indiceServicioSeleccionado >= 0 && indiceServicioSeleccionado < precios.length) {
            textoPrecioTotal.setText("Precio " + precios[indiceServicioSeleccionado]);
        }
        calcularYMostrarCambio();
    }

    private void actualizarVisibilidadEfectivo() {
        boolean esEfectivo = metodoPagoSeleccionado.equalsIgnoreCase("Efectivo");
        if (contenedorEfectivo != null) {
            contenedorEfectivo.setVisibility(esEfectivo ? View.VISIBLE : View.GONE);
        }
        if (esEfectivo) {
            calcularYMostrarCambio();
        }
    }

    private void calcularYMostrarCambio() {
        if (textoCambioCalculado == null) return;
        String[] precios = esCarroSeleccionado ? preciosCarro : preciosMoto;
        if (precios.length == 0 || indiceServicioSeleccionado < 0 || indiceServicioSeleccionado >= precios.length) {
            textoCambioCalculado.setText("$0.00");
            textoCambioCalculado.setTextColor(ContextCompat.getColor(requireContext(), R.color.app_black));
            return;
        }

        String precioStr = precios[indiceServicioSeleccionado];
        long precioCentavos = 0;
        try {
            precioCentavos = new java.math.BigDecimal(precioStr.substring(1)).movePointRight(2).longValueExact();
        } catch (Exception ignored) { }

        String recibidoStr = campoMontoRecibido != null && campoMontoRecibido.getText() != null
                ? campoMontoRecibido.getText().toString().trim().replace(',', '.') : "";

        if (recibidoStr.isEmpty()) {
            textoCambioCalculado.setText("$0.00");
            textoCambioCalculado.setTextColor(ContextCompat.getColor(requireContext(), R.color.app_black));
            return;
        }

        try {
            long recibidoCentavos = new java.math.BigDecimal(recibidoStr).movePointRight(2).longValueExact();
            if (recibidoCentavos >= precioCentavos) {
                long cambioCentavos = recibidoCentavos - precioCentavos;
                textoCambioCalculado.setText("$" + java.math.BigDecimal.valueOf(cambioCentavos, 2).toPlainString());
                textoCambioCalculado.setTextColor(ContextCompat.getColor(requireContext(), R.color.app_black));
            } else {
                long faltaCentavos = precioCentavos - recibidoCentavos;
                textoCambioCalculado.setText("Falta $" + java.math.BigDecimal.valueOf(faltaCentavos, 2).toPlainString());
                textoCambioCalculado.setTextColor(Color.parseColor("#DC2626"));
            }
        } catch (Exception e) {
            textoCambioCalculado.setText("$0.00");
            textoCambioCalculado.setTextColor(ContextCompat.getColor(requireContext(), R.color.app_black));
        }
    }

    private void mostrarDialogoConfirmacionGuardado() {
        if (!actualizarEstadoCaja()) return;
        String[] servicios = esCarroSeleccionado ? serviciosCarro : serviciosMoto;
        if (servicios.length == 0) return;

        String cliente = campoCliente != null && campoCliente.getText() != null
                ? campoCliente.getText().toString().trim() : "";
        if (cliente.isEmpty()) {
            if (campoCliente != null) {
                campoCliente.setError("Ingresa los datos del encargado del vehículo");
                campoCliente.requestFocus();
            } else {
                Toast.makeText(requireContext(), "Ingresa los datos del encargado del vehículo", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        String vehiculo = esCarroSeleccionado ? "Carro" : "Moto";
        String[] precios = esCarroSeleccionado ? preciosCarro : preciosMoto;
        String servicio = servicios[indiceServicioSeleccionado];
        String precio = precios[indiceServicioSeleccionado];
        String servicioId = (esCarroSeleccionado ? idsCarro : idsMoto)[indiceServicioSeleccionado];

        EmpleadosStore.Empleado empleado = SesionStore.requerir(requireActivity());
        if (empleado == null) {
            Toast.makeText(requireContext(), R.string.registrar_sin_empleados, Toast.LENGTH_SHORT).show();
            return;
        }

        long precioConfirmado = new java.math.BigDecimal(precio.substring(1)).movePointRight(2).longValueExact();

        View vistaDialogo = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirmar_atencion, null);

        TextView textoDialogoVehiculo = vistaDialogo.findViewById(R.id.textoDialogoVehiculo);
        TextView textoDialogoServicio = vistaDialogo.findViewById(R.id.textoDialogoServicio);
        TextView textoDialogoCliente = vistaDialogo.findViewById(R.id.textoDialogoCliente);
        TextView textoDialogoMetodoPago = vistaDialogo.findViewById(R.id.textoDialogoMetodoPago);
        TextView textoDialogoPrecio = vistaDialogo.findViewById(R.id.textoDialogoPrecio);
        MaterialButton botonDialogoCancelar = vistaDialogo.findViewById(R.id.botonDialogoCancelar);
        MaterialButton botonDialogoGuardar = vistaDialogo.findViewById(R.id.botonDialogoGuardar);

        textoDialogoVehiculo.setText("• Vehículo: " + vehiculo);
        textoDialogoServicio.setText("• Servicio: " + servicio);
        textoDialogoCliente.setText("• Encargado: " + cliente + "\n• Empleado: " + empleado.nombre);
        if (textoDialogoMetodoPago != null) {
            if (metodoPagoSeleccionado.equalsIgnoreCase("Efectivo") && campoMontoRecibido != null
                    && campoMontoRecibido.getText() != null && !campoMontoRecibido.getText().toString().trim().isEmpty()) {
                String recibido = campoMontoRecibido.getText().toString().trim();
                String cambioStr = textoCambioCalculado != null ? textoCambioCalculado.getText().toString() : "$0.00";
                textoDialogoMetodoPago.setText("• Método de pago: Efectivo (Entregado: $" + recibido + " | Cambio: " + cambioStr + ")");
            } else {
                textoDialogoMetodoPago.setText("• Método de pago: " + metodoPagoSeleccionado);
            }
        }
        textoDialogoPrecio.setText("• Importe: " + precio);

        AlertDialog dialogo = new AlertDialog.Builder(requireContext())
                .setView(vistaDialogo)
                .create();

        if (dialogo.getWindow() != null) {
            dialogo.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        botonDialogoCancelar.setOnClickListener(v -> dialogo.dismiss());

        botonDialogoGuardar.setOnClickListener(v -> {
            if (SesionStore.requerir(requireActivity()) == null) return;
            if (!actualizarEstadoCaja()) { dialogo.dismiss(); return; }
            botonDialogoGuardar.setEnabled(false);
            try {
                if (!new AtencionesStore(requireContext()).registrarActual(servicioId, cliente, metodoPagoSeleccionado, precioConfirmado)) {
                    throw new IllegalStateException();
                }
            } catch (org.json.JSONException | IllegalStateException error) {
                botonDialogoGuardar.setEnabled(true);
                Toast.makeText(requireContext(), R.string.atencion_guardar_error, Toast.LENGTH_LONG).show();
                return;
            }
            dialogo.dismiss();
            Toast.makeText(requireContext(), "Atención registrada con éxito", Toast.LENGTH_SHORT).show();
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).mostrarInicio();
            }
        });

        dialogo.show();
    }

    @Override public void onResume() {
        super.onResume();
        EmpleadosStore.Empleado empleado = SesionStore.requerir(requireActivity());
        actualizarEstadoCaja();
        if (empleado != null && getView() != null)
            ((TextView) requireView().findViewById(R.id.registrar_empleado)).setText(
                    getString(R.string.registrar_empleado_actual, empleado.nombre));
    }

    private boolean actualizarEstadoCaja() {
        if (getView() == null || SesionStore.requerir(requireActivity()) == null) return false;
        String estado = "sin_abrir";
        try { estado = new CierresStore(requireContext()).resumenActual(java.time.LocalDate.now()).getString("estado"); }
        catch (org.json.JSONException | RuntimeException error) {
            Toast.makeText(requireContext(), R.string.atenciones_error, Toast.LENGTH_LONG).show();
        }
        cajaAbierta = "abierta".equals(estado);
        requireView().findViewById(R.id.registrar_aviso_caja).setVisibility(cajaAbierta ? View.GONE : View.VISIBLE);
        ((TextView) requireView().findViewById(R.id.registrar_aviso_texto)).setText(
                "cerrada".equals(estado) ? R.string.caja_cerrada : R.string.caja_falta_apertura);
        String[] precios = esCarroSeleccionado ? preciosCarro : preciosMoto;
        botonGuardarAtencion.setEnabled(cajaAbierta && precios.length > 0);
        return cajaAbierta;
    }

    private int dpAPx(float dp) {
        float densidad = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * densidad);
    }
}

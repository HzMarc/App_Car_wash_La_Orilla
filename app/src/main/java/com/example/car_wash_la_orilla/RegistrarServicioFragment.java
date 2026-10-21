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

public class RegistrarServicioFragment extends Fragment {

    private LinearLayout contenedorServicios;
    private TextView textoEmpleadoSeleccionado;
    private TextView textoPrecioTotal;

    private boolean esCarroSeleccionado = true;
    private int indiceServicioSeleccionado = 0;
    private int indiceEmpleadoSeleccionado = 0;

    private String[] empleados = {};
    private String[] serviciosCarro = {};
    private String[] preciosCarro = {};
    private String[] serviciosMoto = {};
    private String[] preciosMoto = {};
    private String[] idsEmpleados = {}, idsCarro = {}, idsMoto = {};
    private MaterialButton botonGuardarAtencion;

    private void cargarCatalogos() {
        java.util.List<String> nombresCarro = new java.util.ArrayList<>();
        java.util.List<String> importesCarro = new java.util.ArrayList<>();
        java.util.List<String> nombresMoto = new java.util.ArrayList<>();
        java.util.List<String> importesMoto = new java.util.ArrayList<>();
        java.util.List<String> nombresEmpleados = new java.util.ArrayList<>();
        java.util.List<String> empleadosIds = new java.util.ArrayList<>();
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
            for (EmpleadosStore.Empleado empleado : new EmpleadosStore(requireContext()).getAll()) {
                if (empleado.activo) {
                    nombresEmpleados.add(empleado.nombre);
                    empleadosIds.add(empleado.id);
                }
            }
        } catch (org.json.JSONException error) {
            nombresCarro.clear();
            importesCarro.clear();
            nombresMoto.clear();
            importesMoto.clear();
            nombresEmpleados.clear();
            empleadosIds.clear();
            carroIds.clear();
            motoIds.clear();
            Toast.makeText(requireContext(), R.string.registrar_catalogos_error, Toast.LENGTH_LONG).show();
        }
        serviciosCarro = nombresCarro.toArray(new String[0]);
        preciosCarro = importesCarro.toArray(new String[0]);
        serviciosMoto = nombresMoto.toArray(new String[0]);
        preciosMoto = importesMoto.toArray(new String[0]);
        empleados = nombresEmpleados.toArray(new String[0]);
        idsEmpleados = empleadosIds.toArray(new String[0]);
        idsCarro = carroIds.toArray(new String[0]);
        idsMoto = motoIds.toArray(new String[0]);
        indiceEmpleadoSeleccionado = 0;
        indiceServicioSeleccionado = 0;
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View vista = inflater.inflate(R.layout.fragment_registrar_servicio, container, false);

        TabLayout vehiculos = vista.findViewById(R.id.registrar_vehiculo_tabs);
        contenedorServicios = vista.findViewById(R.id.contenedorServicios);
        MaterialCardView tarjetaSeleccionarEmpleado = vista.findViewById(R.id.tarjetaSeleccionarEmpleado);
        textoEmpleadoSeleccionado = vista.findViewById(R.id.textoEmpleadoSeleccionado);
        textoPrecioTotal = vista.findViewById(R.id.textoPrecioTotal);
        botonGuardarAtencion = vista.findViewById(R.id.botonGuardarAtencion);

        if (tarjetaSeleccionarEmpleado != null) {
            tarjetaSeleccionarEmpleado.setOnClickListener(v -> mostrarDialogoSeleccionEmpleado());
        }

        if (botonGuardarAtencion != null) {
            botonGuardarAtencion.setOnClickListener(v -> mostrarDialogoConfirmacionGuardado());
        }

        cargarCatalogos();
        textoEmpleadoSeleccionado.setText(empleados.length == 0 ? getString(R.string.registrar_sin_empleados) : empleados[0]);
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
        botonGuardarAtencion.setEnabled(precios.length > 0 && empleados.length > 0);
        if (precios.length == 0) {
            textoPrecioTotal.setText(R.string.registrar_sin_servicios);
            return;
        }
        if (indiceServicioSeleccionado >= 0 && indiceServicioSeleccionado < precios.length) {
            textoPrecioTotal.setText("Precio " + precios[indiceServicioSeleccionado]);
        }
    }

    private void mostrarDialogoSeleccionEmpleado() {
        if (empleados.length == 0) {
            Toast.makeText(requireContext(), R.string.registrar_sin_empleados, Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Seleccionar empleado")
                .setSingleChoiceItems(empleados, indiceEmpleadoSeleccionado, (dialogo, opcion) -> {
                    indiceEmpleadoSeleccionado = opcion;
                    textoEmpleadoSeleccionado.setText(empleados[opcion]);
                    dialogo.dismiss();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogoConfirmacionGuardado() {
        if (empleados.length == 0 || (esCarroSeleccionado ? serviciosCarro : serviciosMoto).length == 0) return;
        String vehiculo = esCarroSeleccionado ? "Carro" : "Moto";
        String[] servicios = esCarroSeleccionado ? serviciosCarro : serviciosMoto;
        String[] precios = esCarroSeleccionado ? preciosCarro : preciosMoto;
        String servicio = servicios[indiceServicioSeleccionado];
        String precio = precios[indiceServicioSeleccionado];
        String empleado = empleados[indiceEmpleadoSeleccionado];
        String servicioId = (esCarroSeleccionado ? idsCarro : idsMoto)[indiceServicioSeleccionado];
        String empleadoId = idsEmpleados[indiceEmpleadoSeleccionado];
        long precioConfirmado = new java.math.BigDecimal(precio.substring(1)).movePointRight(2).longValueExact();

        View vistaDialogo = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirmar_atencion, null);

        TextView textoDialogoVehiculo = vistaDialogo.findViewById(R.id.textoDialogoVehiculo);
        TextView textoDialogoServicio = vistaDialogo.findViewById(R.id.textoDialogoServicio);
        TextView textoDialogoEmpleado = vistaDialogo.findViewById(R.id.textoDialogoEmpleado);
        TextView textoDialogoPrecio = vistaDialogo.findViewById(R.id.textoDialogoPrecio);
        MaterialButton botonDialogoCancelar = vistaDialogo.findViewById(R.id.botonDialogoCancelar);
        MaterialButton botonDialogoGuardar = vistaDialogo.findViewById(R.id.botonDialogoGuardar);

        textoDialogoVehiculo.setText("• Vehículo: " + vehiculo);
        textoDialogoServicio.setText("• Servicio: " + servicio);
        textoDialogoEmpleado.setText("• Empleado: " + empleado);
        textoDialogoPrecio.setText("• Importe: " + precio);

        AlertDialog dialogo = new AlertDialog.Builder(requireContext())
                .setView(vistaDialogo)
                .create();

        if (dialogo.getWindow() != null) {
            dialogo.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        botonDialogoCancelar.setOnClickListener(v -> dialogo.dismiss());

        botonDialogoGuardar.setOnClickListener(v -> {
            botonDialogoGuardar.setEnabled(false);
            try {
                if (!new AtencionesStore(requireContext()).registrar(servicioId, empleadoId, precioConfirmado)) {
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

    private int dpAPx(float dp) {
        float densidad = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * densidad);
    }
}

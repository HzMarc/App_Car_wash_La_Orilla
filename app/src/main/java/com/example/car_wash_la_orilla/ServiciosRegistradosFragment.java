package com.example.car_wash_la_orilla;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class ServiciosRegistradosFragment extends Fragment {

    private SearchView vistaBusqueda;
    private LinearLayout contenedorListaServicios;
    private TextView textoDetalleAtencion;

    private int indiceSeleccionado = 0;
    private final List<ElementoServicio> listaServiciosCompleta = new ArrayList<>();
    private final List<ElementoServicio> listaServiciosFiltrada = new ArrayList<>();

    public static class ElementoServicio {
        String hora;
        String vehiculo;
        String servicio;
        String empleado;
        String precio;

        final String id;
        final boolean anulada, ejemplo;
        ElementoServicio(AtencionesStore.Atencion atencion, boolean ejemplo) {
            id = atencion.id;
            anulada = atencion.anulada;
            this.ejemplo = ejemplo;
            hora = atencion.hora();
            vehiculo = atencion.vehiculo.equals("carro") ? "Carro" : "Moto";
            servicio = atencion.servicio;
            empleado = atencion.empleado;
            precio = "$" + atencion.precio();
        }
        public String obtenerTextoEncabezado() {
            return hora + " - " + vehiculo + " - " + servicio;
        }

        public String obtenerTextoResumenDetalle() {
            return vehiculo + " · " + servicio + " · " + empleado + " · " + precio;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View vista = inflater.inflate(R.layout.fragment_servicios_registrados, container, false);

        vistaBusqueda = vista.findViewById(R.id.vistaBusqueda);
        contenedorListaServicios = vista.findViewById(R.id.contenedorListaServicios);
        textoDetalleAtencion = vista.findViewById(R.id.textoDetalleAtencion);
        MaterialButton botonCorregir = vista.findViewById(R.id.botonCorregir);
        MaterialButton botonAnular = vista.findViewById(R.id.botonAnular);

        inicializarDatosServicios();

        configurarBusqueda();

        if (botonCorregir != null) {
            botonCorregir.setOnClickListener(v -> mostrarDialogoMotivo(true));
        }

        if (botonAnular != null) {
            botonAnular.setOnClickListener(v -> mostrarDialogoMotivo(false));
        }

        renderizarListaServicios();

        return vista;
    }

    private void inicializarDatosServicios() {
        listaServiciosCompleta.clear();
        try {
            for (AtencionesStore.Atencion atencion : new AtencionesStore(requireContext()).delDia(java.time.LocalDate.now())) {
                listaServiciosCompleta.add(new ElementoServicio(atencion, atencion.ejemplo));
            }
        } catch (org.json.JSONException error) {
            Toast.makeText(requireContext(), R.string.atenciones_error, Toast.LENGTH_LONG).show();
        }
        listaServiciosFiltrada.clear();
        listaServiciosFiltrada.addAll(listaServiciosCompleta);
    }

    @Override public void onResume() {
        super.onResume();
        inicializarDatosServicios();
        filtrarServicios(vistaBusqueda.getQuery().toString());
    }

    private void configurarBusqueda() {
        if (vistaBusqueda == null) return;

        vistaBusqueda.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String consulta) {
                filtrarServicios(consulta);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String textoNuevo) {
                filtrarServicios(textoNuevo);
                return true;
            }
        });
    }

    private void filtrarServicios(String textoConsulta) {
        listaServiciosFiltrada.clear();

        if (textoConsulta == null || textoConsulta.trim().isEmpty()) {
            listaServiciosFiltrada.addAll(listaServiciosCompleta);
        } else {
            String textoMinusculas = textoConsulta.toLowerCase().trim();
            for (ElementoServicio elemento : listaServiciosCompleta) {
                if (elemento.empleado.toLowerCase().contains(textoMinusculas) ||
                        elemento.servicio.toLowerCase().contains(textoMinusculas) ||
                        elemento.vehiculo.toLowerCase().contains(textoMinusculas) ||
                        elemento.hora.toLowerCase().contains(textoMinusculas)) {
                    listaServiciosFiltrada.add(elemento);
                }
            }
        }

        if (indiceSeleccionado >= listaServiciosFiltrada.size()) {
            indiceSeleccionado = 0;
        }

        renderizarListaServicios();
    }

    private void renderizarListaServicios() {
        if (contenedorListaServicios == null) return;
        contenedorListaServicios.removeAllViews();

        if (listaServiciosFiltrada.isEmpty()) {
            TextView textoVacio = new TextView(requireContext());
            textoVacio.setText("No se encontraron resultados para la búsqueda");
            textoVacio.setTextColor(ContextCompat.getColor(requireContext(), R.color.app_text_secondary));
            textoVacio.setTextSize(14);
            textoVacio.setPadding(dpAPx(8), dpAPx(20), dpAPx(8), dpAPx(20));
            contenedorListaServicios.addView(textoVacio);
            textoDetalleAtencion.setText("Sin selección");
            return;
        }

        for (int i = 0; i < listaServiciosFiltrada.size(); i++) {
            final int indice = i;
            final ElementoServicio elemento = listaServiciosFiltrada.get(i);
            boolean estaSeleccionado = (indice == indiceSeleccionado);

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

            LinearLayout contenedorVertical = new LinearLayout(requireContext());
            contenedorVertical.setOrientation(LinearLayout.VERTICAL);
            contenedorVertical.setPadding(dpAPx(14), dpAPx(14), dpAPx(14), dpAPx(14));

            RelativeLayout contenedorRelativo = new RelativeLayout(requireContext());

            TextView textoEncabezado = new TextView(requireContext());
            RelativeLayout.LayoutParams parametrosEncabezado = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            parametrosEncabezado.addRule(RelativeLayout.ALIGN_PARENT_START);
            textoEncabezado.setLayoutParams(parametrosEncabezado);
            textoEncabezado.setText(elemento.obtenerTextoEncabezado());
            textoEncabezado.setTextColor(ContextCompat.getColor(requireContext(), R.color.app_black));
            textoEncabezado.setTextSize(14);
            textoEncabezado.setTypeface(Typeface.create("sans-serif", estaSeleccionado ? Typeface.BOLD : Typeface.NORMAL));

            TextView textoPrecio = new TextView(requireContext());
            RelativeLayout.LayoutParams parametrosPrecio = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            parametrosPrecio.addRule(RelativeLayout.ALIGN_PARENT_END);
            textoPrecio.setLayoutParams(parametrosPrecio);
            textoPrecio.setText(elemento.precio);
            textoPrecio.setTextColor(ContextCompat.getColor(requireContext(), R.color.app_black));
            textoPrecio.setTextSize(15);
            textoPrecio.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));

            contenedorRelativo.addView(textoEncabezado);
            contenedorRelativo.addView(textoPrecio);

            TextView textoEmpleado = new TextView(requireContext());
            LinearLayout.LayoutParams parametrosEmpleado = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            parametrosEmpleado.setMargins(0, dpAPx(4), 0, 0);
            textoEmpleado.setLayoutParams(parametrosEmpleado);
            textoEmpleado.setText(elemento.empleado + (elemento.anulada ? " · " + getString(R.string.atencion_anulada) : "")
                    + (elemento.ejemplo ? " · " + getString(R.string.atencion_ejemplo) : ""));
            textoEmpleado.setTextColor(ContextCompat.getColor(requireContext(), R.color.app_text_secondary));
            textoEmpleado.setTextSize(13);

            contenedorVertical.addView(contenedorRelativo);
            contenedorVertical.addView(textoEmpleado);
            tarjeta.addView(contenedorVertical);

            tarjeta.setOnClickListener(v -> {
                indiceSeleccionado = indice;
                renderizarListaServicios();
                actualizarDetalleInferior();
            });

            contenedorListaServicios.addView(tarjeta);
        }

        actualizarDetalleInferior();
    }

    private void actualizarDetalleInferior() {
        if (!listaServiciosFiltrada.isEmpty() && indiceSeleccionado >= 0 && indiceSeleccionado < listaServiciosFiltrada.size()) {
            ElementoServicio seleccionado = listaServiciosFiltrada.get(indiceSeleccionado);
            textoDetalleAtencion.setText(seleccionado.obtenerTextoResumenDetalle());
        } else {
            textoDetalleAtencion.setText("Sin selección");
        }
    }

    private void mostrarDialogoMotivo(boolean corregir) {
        String tituloAccion = corregir ? "Corregir importe de atención" : "Anular atención";
        if (listaServiciosFiltrada.isEmpty() || indiceSeleccionado < 0 || indiceSeleccionado >= listaServiciosFiltrada.size()) {
            Toast.makeText(requireContext(), "Selecciona una atención primero", Toast.LENGTH_SHORT).show();
            return;
        }

        ElementoServicio seleccionado = listaServiciosFiltrada.get(indiceSeleccionado);
        if (seleccionado.anulada) {
            Toast.makeText(requireContext(), R.string.atencion_cambio_error, Toast.LENGTH_LONG).show();
            return;
        }

        View vistaDialogo = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_motivo, null);

        TextView textoTitulo = vistaDialogo.findViewById(R.id.textoTituloMotivo);
        EditText campoMotivo = vistaDialogo.findViewById(R.id.campoMotivo);
        MaterialButton botonCancelar = vistaDialogo.findViewById(R.id.botonCancelarMotivo);
        MaterialButton botonConfirmar = vistaDialogo.findViewById(R.id.botonConfirmarMotivo);

        EditText precioCorregido = new EditText(requireContext());
        if (corregir) {
            precioCorregido.setHint(R.string.atencion_precio);
            precioCorregido.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            precioCorregido.setText(seleccionado.precio.substring(1));
            LinearLayout formulario = (LinearLayout) ((MaterialCardView) vistaDialogo).getChildAt(0);
            formulario.addView(precioCorregido, 2, new LinearLayout.LayoutParams(-1, -2));
        }

        if (textoTitulo != null) {
            textoTitulo.setText(tituloAccion);
        }

        AlertDialog dialogo = new AlertDialog.Builder(requireContext())
                .setView(vistaDialogo)
                .create();

        if (dialogo.getWindow() != null) {
            dialogo.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        if (botonCancelar != null) {
            botonCancelar.setOnClickListener(v -> dialogo.dismiss());
        }

        if (botonConfirmar != null) {
            botonConfirmar.setOnClickListener(v -> {
                String motivo = campoMotivo.getText() != null ? campoMotivo.getText().toString().trim() : "";
                if (motivo.isEmpty()) {
                    Toast.makeText(requireContext(), "Por favor ingresa un motivo", Toast.LENGTH_SHORT).show();
                    return;
                }

                Long nuevoPrecio = corregir ? CatalogoStore.parsePrice(precioCorregido.getText().toString()) : null;
                if (corregir && nuevoPrecio == null) {
                    precioCorregido.setError(getString(R.string.catalogo_invalid_price));
                    return;
                }
                try {
                    if (!new AtencionesStore(requireContext()).cambiar(seleccionado.id, motivo, nuevoPrecio)) throw new IllegalStateException();
                } catch (org.json.JSONException | IllegalStateException error) {
                    Toast.makeText(requireContext(), R.string.atencion_cambio_error, Toast.LENGTH_LONG).show();
                    return;
                }
                dialogo.dismiss();
                inicializarDatosServicios();
                filtrarServicios(vistaBusqueda.getQuery().toString());
                Toast.makeText(requireContext(), tituloAccion + " realizada", Toast.LENGTH_SHORT).show();
            });
        }

        dialogo.show();
    }

    private int dpAPx(float dp) {
        float densidad = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * densidad);
    }
}

package com.example.car_wash_la_orilla;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import org.json.JSONException;

import java.util.List;

public class CatalogoFragment extends Fragment {
    static final String RESULT_CHANGED = "catalogo_cambiado";
    private static final String DIALOG_TAG = "editar_servicio";
    private CatalogoStore store;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_catalogo, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        store = new CatalogoStore(requireContext());
        ((RecyclerView) view.findViewById(R.id.catalogo_list)).setLayoutManager(new LinearLayoutManager(requireContext()));
        view.findViewById(R.id.catalogo_add).setOnClickListener(v -> openEditor(null));
        getChildFragmentManager().setFragmentResultListener(RESULT_CHANGED, getViewLifecycleOwner(), (key, result) -> load());
        load();
    }

    private void load() {
        View view = getView();
        if (view == null) return;
        RecyclerView list = view.findViewById(R.id.catalogo_list);
        MaterialButton add = view.findViewById(R.id.catalogo_add);
        try {
            List<CatalogoStore.Servicio> services = store.getAll();
            list.setAdapter(new ServiceAdapter(services));
            view.findViewById(R.id.catalogo_empty).setVisibility(services.isEmpty() ? View.VISIBLE : View.GONE);
            ((TextView) view.findViewById(R.id.catalogo_empty_title)).setText(R.string.catalogo_empty);
            view.findViewById(R.id.catalogo_empty_hint).setVisibility(View.VISIBLE);
            add.setEnabled(true);
        } catch (JSONException error) {
            // Conservar los datos originales; impedir que un catálogo ilegible se sobrescriba.
            list.setAdapter(null);
            view.findViewById(R.id.catalogo_empty).setVisibility(View.VISIBLE);
            ((TextView) view.findViewById(R.id.catalogo_empty_title)).setText(R.string.catalogo_read_error);
            view.findViewById(R.id.catalogo_empty_hint).setVisibility(View.GONE);
            add.setEnabled(false);
        }
    }

    private void openEditor(@Nullable String id) {
        if (getChildFragmentManager().findFragmentByTag(DIALOG_TAG) == null) {
            ServicioDialogFragment.newInstance(id).show(getChildFragmentManager(), DIALOG_TAG);
        }
    }

    private class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.Holder> {
        private final List<CatalogoStore.Servicio> services;
        ServiceAdapter(List<CatalogoStore.Servicio> services) { this.services = services; }
        class Holder extends RecyclerView.ViewHolder { Holder(View view) { super(view); } }

        @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_catalogo_servicio, parent, false));
        }

        @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
            CatalogoStore.Servicio service = services.get(position);
            View view = holder.itemView;
            Context context = view.getContext();
            int text = context.getColor(service.activo ? R.color.ink : R.color.ink_secondary);
            MaterialCardView card = (MaterialCardView) view;
            card.setCardBackgroundColor(context.getColor(service.activo ? R.color.app_card_bg : R.color.app_card_selected));
            TextView name = view.findViewById(R.id.service_name), price = view.findViewById(R.id.service_price);
            name.setText(service.nombre);
            name.setTextColor(text);
            price.setText("$" + service.precio());
            price.setTextColor(text);
            TextView vehicle = view.findViewById(R.id.service_vehicle);
            vehicle.setText(service.vehiculo.equals("carro") ? R.string.catalogo_car : R.string.catalogo_motorcycle);
            vehicle.setTextColor(context.getColor(service.activo ? R.color.app_black : R.color.app_text_secondary));
            GradientDrawable badge = new GradientDrawable();
            badge.setColor(context.getColor(R.color.app_card_selected));
            badge.setCornerRadius(8 * getResources().getDisplayMetrics().density);
            badge.setStroke((int) getResources().getDisplayMetrics().density, text);
            vehicle.setBackground(badge);
            TextView edit = view.findViewById(R.id.service_edit);
            edit.setContentDescription(getString(R.string.catalogo_edit_service, service.nombre));
            edit.setOnClickListener(v -> openEditor(service.id));
            MaterialSwitch active = view.findViewById(R.id.service_active);
            active.setOnCheckedChangeListener(null);
            active.setChecked(service.activo);
            active.setText(service.activo ? R.string.catalogo_active : R.string.catalogo_inactive);
            active.setContentDescription(getString(R.string.catalogo_toggle, service.nombre));
            active.setOnCheckedChangeListener((button, checked) -> {
                boolean saved;
                try { saved = store.setActive(service.id, checked); }
                catch (JSONException failure) { saved = false; }
                if (!saved) Toast.makeText(context, R.string.catalogo_write_error, Toast.LENGTH_LONG).show();
                load();
            });
        }

        @Override public int getItemCount() { return services.size(); }
    }
}

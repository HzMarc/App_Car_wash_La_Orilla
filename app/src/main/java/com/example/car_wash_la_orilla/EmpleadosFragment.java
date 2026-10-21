package com.example.car_wash_la_orilla;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;

import org.json.JSONException;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EmpleadosFragment extends Fragment {
    static final String RESULT_CHANGED = "empleados_cambiados";
    private static final String DIALOG_TAG = "editar_empleado";
    private EmpleadosStore store;
    private String query = "";
    private final List<EmpleadosStore.Empleado> employees = new ArrayList<>();
    private boolean readable;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_empleados, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        store = new EmpleadosStore(requireContext());
        if (state != null) query = state.getString("search_query", "");
        ((RecyclerView) view.findViewById(R.id.empleados_list)).setLayoutManager(new LinearLayoutManager(requireContext()));
        view.findViewById(R.id.empleados_add).setOnClickListener(v -> openEditor(null));
        SearchView search = view.findViewById(R.id.empleados_search);
        search.setQuery(query, false);
        search.clearFocus();
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String text) { search.clearFocus(); return true; }
            @Override public boolean onQueryTextChange(String text) { query = text; filter(); return true; }
        });
        getChildFragmentManager().setFragmentResultListener(RESULT_CHANGED, getViewLifecycleOwner(), (key, result) -> load());
        load();
    }

    @Override public void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("search_query", query);
    }

    private void load() {
        employees.clear();
        readable = true;
        try { employees.addAll(store.getAll()); }
        catch (JSONException error) { readable = false; }
        filter();
    }

    private static String searchKey(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).trim();
    }

    private void filter() {
        View view = getView();
        if (view == null) return;
        String key = searchKey(query);
        List<EmpleadosStore.Empleado> matches = new ArrayList<>();
        if (readable) {
            for (EmpleadosStore.Empleado employee : employees) {
                if (searchKey(employee.nombre).contains(key)) matches.add(employee);
            }
        }
        RecyclerView list = view.findViewById(R.id.empleados_list);
        list.setAdapter(new EmployeeAdapter(matches));
        TextView empty = view.findViewById(R.id.empleados_empty);
        empty.setVisibility(matches.isEmpty() ? View.VISIBLE : View.GONE);
        empty.setText(!readable ? R.string.empleados_read_error
                : employees.isEmpty() ? R.string.empleados_empty : R.string.empleados_no_results);
        view.findViewById(R.id.empleados_add).setEnabled(readable);
    }

    private void openEditor(@Nullable String id) {
        if (getChildFragmentManager().findFragmentByTag(DIALOG_TAG) == null) {
            EmpleadoDialogFragment.newInstance(id).show(getChildFragmentManager(), DIALOG_TAG);
        }
    }

    private void openHistory(EmpleadosStore.Empleado employee) {
        String tag = "historial_empleado";
        if (getChildFragmentManager().findFragmentByTag(tag) == null) {
            HistorialEmpleadoBottomSheet.newInstance(employee.id, employee.nombre)
                    .show(getChildFragmentManager(), tag);
        }
    }

    private class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.Holder> {
        private final List<EmpleadosStore.Empleado> matches;
        EmployeeAdapter(List<EmpleadosStore.Empleado> matches) { this.matches = matches; }
        class Holder extends RecyclerView.ViewHolder { Holder(View view) { super(view); } }
        @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_empleado, parent, false));
        }

        @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
            EmpleadosStore.Empleado employee = matches.get(position);
            View view = holder.itemView;
            Context context = view.getContext();
            int text = context.getColor(employee.activo ? R.color.ink : R.color.ink_secondary);
            ((MaterialCardView) view).setCardBackgroundColor(context.getColor(employee.activo ? R.color.app_card_bg : R.color.app_card_selected));
            TextView name = view.findViewById(R.id.empleado_nombre), status = view.findViewById(R.id.empleado_estado);
            name.setText(employee.nombre);
            name.setTextColor(text);
            status.setText(employee.activo ? R.string.empleados_active : R.string.empleados_inactive);
            TextView avatar = view.findViewById(R.id.empleado_avatar);
            int end = employee.nombre.offsetByCodePoints(0, 1);
            avatar.setText(employee.nombre.substring(0, end).toUpperCase(Locale.ROOT));
            avatar.setTextColor(context.getColor(R.color.bg));
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(context.getColor(employee.activo ? R.color.ink : R.color.ink_secondary));
            avatar.setBackground(circle);
            view.setOnClickListener(v -> openEditor(employee.id));
            View history = view.findViewById(R.id.empleado_historial);
            history.setContentDescription(getString(R.string.empleados_history_description, employee.nombre));
            history.setOnClickListener(v -> openHistory(employee));
            MaterialSwitch active = view.findViewById(R.id.empleado_activo);
            active.setOnCheckedChangeListener(null);
            active.setChecked(employee.activo);
            active.setContentDescription(getString(R.string.empleados_toggle, employee.nombre));
            active.setOnCheckedChangeListener((button, checked) -> {
                boolean saved;
                try { saved = store.setActive(employee.id, checked); }
                catch (JSONException error) { saved = false; }
                if (!saved) Toast.makeText(context, R.string.empleados_write_error, Toast.LENGTH_LONG).show();
                load();
            });
        }
        @Override public int getItemCount() { return matches.size(); }
    }
}

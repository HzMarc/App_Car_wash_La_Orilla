package com.example.car_wash_la_orilla;

import android.os.Bundle;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Cierre e historial conectado al mismo almacenamiento que Registrar, Servicios e Inicio. */
public class CierreFragment extends Fragment {
    /** JSON: [{"empleadoId":"1","empleado":"Ana","atenciones":3,"esperadoCentavos":1500}]. */
    public static final String EXTRA_RESUMENES = "cierre.resumenes";
    public static final String EXTRA_FECHA = "cierre.fecha"; // ISO yyyy-MM-dd; omitir para hoy.
    private final List<JSONObject> summaries = new ArrayList<>();
    private LocalDate summaryDate;
    private TabLayoutMediator tabMediator;
    private boolean datosValidos;

    private void cargarResumenes() {
        summaries.clear();
        datosValidos = false;
        summaryDate = LocalDate.now();
        try {
            String fecha = requireActivity().getIntent().getStringExtra(EXTRA_FECHA);
            if (fecha != null) summaryDate = LocalDate.parse(fecha);
            JSONArray datos = new AtencionesStore(requireContext()).resumenes(summaryDate);
            for (int i = 0; i < datos.length(); i++) summaries.add(datos.getJSONObject(i));
            datosValidos = true;
        } catch (JSONException | RuntimeException error) {
            summaries.clear();
            Toast.makeText(requireContext(), R.string.cierre_invalid_summary, Toast.LENGTH_LONG).show();
        }
    }

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_cierre, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        cargarResumenes();
        ViewPager2 pager = view.findViewById(R.id.cierre_pager);
        pager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull @Override public Fragment createFragment(int position) {
                return position == 0 ? new HoyFragment() : new HistorialFragment();
            }
            @Override public int getItemCount() { return 2; }
        });
        TabLayout tabs = view.findViewById(R.id.cierre_tabs);
        tabMediator = new TabLayoutMediator(tabs, pager, (tab, position) ->
                tab.setText(position == 0 ? R.string.cierre_today : R.string.cierre_history));
        tabMediator.attach();
    }

    @Override public void onDestroyView() {
        if (tabMediator != null) tabMediator.detach();
        tabMediator = null;
        super.onDestroyView();
    }

    private SharedPreferences storage() { return requireContext().getSharedPreferences("cierres", Context.MODE_PRIVATE); }
    private JSONArray history() throws JSONException { return new JSONArray(storage().getString("historial", "[]")); }
    private static String money(BigDecimal amount) { return "$" + amount.setScale(2).toPlainString(); }
    private static String signedMoney(BigDecimal amount) {
        return (amount.signum() < 0 ? "−" : amount.signum() > 0 ? "+" : "") + money(amount.abs());
    }
    private static BigDecimal amount(TextInputEditText input) {
        String value = String.valueOf(input.getText()).trim().replace(',', '.');
        return value.matches("[0-9]{1,9}(\\.[0-9]{1,2})?") ? new BigDecimal(value).setScale(2) : null;
    }
    private static int status(BigDecimal difference) {
        return difference.signum() < 0 ? R.string.cierre_short : difference.signum() > 0 ? R.string.cierre_over : R.string.cierre_balanced;
    }

    public static class HoyFragment extends Fragment {
        private int selected = -1;
        private TextInputEditText delivered;
        private MaterialButton save;
        private View content;
        private CierreFragment host() { return (CierreFragment) requireParentFragment(); }

        private String idSeleccionado() {
            return selected >= 0 && selected < host().summaries.size()
                    ? host().summaries.get(selected).optString("empleadoId") : null;
        }

        private void recargar(String empleadoId) {
            host().cargarResumenes();
            List<String> nombres = new ArrayList<>();
            selected = -1;
            for (int i = 0; i < host().summaries.size(); i++) {
                JSONObject fila = host().summaries.get(i);
                nombres.add(fila.optString("empleado"));
                if (fila.optString("empleadoId").equals(empleadoId)) selected = i;
            }
            if (selected < 0 && !nombres.isEmpty()) {
                selected = 0;
                for (int i = 0; i < host().summaries.size(); i++) {
                    if (host().summaries.get(i).optInt("atenciones") > 0) { selected = i; break; }
                }
            }
            MaterialAutoCompleteTextView selector = content.findViewById(R.id.cierre_employee);
            selector.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, nombres));
            selector.setEnabled(!nombres.isEmpty());
            selector.setText(selected < 0 ? "" : nombres.get(selected), false);
            content.findViewById(R.id.cierre_employee_notice).setVisibility(nombres.isEmpty() ? View.VISIBLE : View.GONE);
            refresh();
        }

        @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle state) {
            return inflater.inflate(R.layout.fragment_cierre_hoy, parent, false);
        }

        @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
            content = view;
            delivered = view.findViewById(R.id.cierre_delivered);
            save = view.findViewById(R.id.cierre_save);
            MaterialAutoCompleteTextView employee = view.findViewById(R.id.cierre_employee);
            employee.setOnItemClickListener((parent, item, position, id) -> { selected = position; refresh(); });
            TextWatcher watcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { refresh(); }
                @Override public void afterTextChanged(Editable text) { }
            };
            delivered.addTextChangedListener(watcher);
            save.setOnClickListener(v -> saveClosure());
            recargar(state == null ? null : state.getString("empleado_id"));
        }

        @Override public void onViewStateRestored(@Nullable Bundle state) { super.onViewStateRestored(state); refresh(); }
        @Override public void onResume() { super.onResume(); if (content != null) recargar(idSeleccionado()); }
        @Override public void onSaveInstanceState(@NonNull Bundle state) { super.onSaveInstanceState(state); state.putString("empleado_id", idSeleccionado()); }
        @Override public void onDestroyView() { super.onDestroyView(); content = null; delivered = null; save = null; }

        private BigDecimal expected() {
            return selected < 0 ? BigDecimal.ZERO.setScale(2)
                    : BigDecimal.valueOf(host().summaries.get(selected).optLong("esperadoCentavos"), 2);
        }

        private boolean alreadyClosed() throws JSONException {
            if (selected < 0) return false;
            String employeeId = host().summaries.get(selected).getString("empleadoId");
            JSONArray rows = host().history();
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                if (row.getString("fecha").equals(host().summaryDate.toString()) && row.getString("empleadoId").equals(employeeId)) return true;
            }
            return false;
        }

        private void refresh() {
            BigDecimal expected = expected(), handed = amount(delivered);
            ((TextView) content.findViewById(R.id.cierre_count)).setText(String.valueOf(selected < 0 ? 0 : host().summaries.get(selected).optInt("atenciones")));
            ((TextView) content.findViewById(R.id.cierre_expected)).setText(money(expected));
            TextView aviso = content.findViewById(R.id.cierre_employee_notice);
            boolean ejemplos = selected >= 0 && host().summaries.get(selected).optBoolean("ejemplo", false);
            aviso.setText(host().summaries.isEmpty() ? R.string.cierre_no_employees : R.string.cierre_incluye_ejemplos);
            aviso.setVisibility(host().summaries.isEmpty() || ejemplos ? View.VISIBLE : View.GONE);
            ((TextInputLayout) content.findViewById(R.id.cierre_delivered_layout)).setError(
                    delivered.length() > 0 && handed == null ? getString(R.string.cierre_invalid_amount) : null);
            boolean ready = host().datosValidos && selected >= 0 && handed != null;
            BigDecimal diff = ready ? handed.subtract(expected) : BigDecimal.ZERO;
            int background = ready && diff.signum() < 0 ? R.color.ink : ready && diff.signum() > 0 ? R.color.gold : R.color.bg;
            int foreground = ready && diff.signum() < 0 ? R.color.bg : R.color.ink;
            MaterialCardView card = content.findViewById(R.id.cierre_difference_card);
            card.setCardBackgroundColor(requireContext().getColor(background));
            card.setStrokeColor(requireContext().getColor(ready && diff.signum() == 0 ? R.color.ink : R.color.line));
            card.setStrokeWidth((int) (getResources().getDisplayMetrics().density * (ready && diff.signum() == 0 ? 2 : 1)));
            TextView label = content.findViewById(R.id.cierre_status), figure = content.findViewById(R.id.cierre_difference);
            label.setText(ready ? status(diff) : R.string.cierre_difference);
            label.setTextColor(requireContext().getColor(foreground));
            figure.setText(ready ? signedMoney(diff) : "—");
            figure.setTextColor(requireContext().getColor(foreground));
            content.findViewById(R.id.cierre_difference_hint).setVisibility(ready ? View.GONE : View.VISIBLE);
            boolean closed = false, readable = true;
            try { closed = alreadyClosed(); } catch (JSONException invalidHistory) { readable = false; }
            boolean today = host().summaryDate.equals(LocalDate.now());
            save.setText(closed ? R.string.cierre_already_saved : R.string.cierre_save);
            save.setEnabled(ready && !closed && readable && today);
        }

        private void saveClosure() {
            BigDecimal handed = amount(delivered);
            if (selected < 0 || handed == null || !host().summaryDate.equals(LocalDate.now())) return;
            String anterior = host().summaries.get(selected).toString();
            LocalDate fechaAnterior = host().summaryDate;
            recargar(idSeleccionado());
            if (selected < 0 || !host().datosValidos || !fechaAnterior.equals(host().summaryDate)
                    || !anterior.equals(host().summaries.get(selected).toString())) {
                Toast.makeText(requireContext(), R.string.cierre_totales_actualizados, Toast.LENGTH_LONG).show();
                return;
            }
            try {
                if (alreadyClosed()) { refresh(); return; }
                JSONObject summary = host().summaries.get(selected);
                JSONObject row = new JSONObject();
                row.put("fecha", host().summaryDate.toString());
                row.put("empleadoId", summary.getString("empleadoId"));
                row.put("empleado", summary.getString("empleado"));
                row.put("atenciones", summary.getInt("atenciones"));
                row.put("esperado", expected().toPlainString());
                row.put("entregado", handed.toPlainString());
                JSONArray rows = host().history();
                rows.put(row);
                if (!host().storage().edit().putString("historial", rows.toString()).commit()) throw new IllegalStateException();
                Toast.makeText(requireContext(), R.string.cierre_saved, Toast.LENGTH_SHORT).show();
                refresh();
                host().getChildFragmentManager().setFragmentResult("cierre_guardado", new Bundle());
                ((ViewPager2) host().requireView().findViewById(R.id.cierre_pager)).setCurrentItem(1);
            } catch (JSONException | IllegalStateException failure) {
                Toast.makeText(requireContext(), R.string.cierre_save_error, Toast.LENGTH_LONG).show();
            }
        }
    }

    public static class HistorialFragment extends Fragment {
        @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle state) {
            return inflater.inflate(R.layout.fragment_cierre_historial, parent, false);
        }
        @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
            ((RecyclerView) view.findViewById(R.id.cierre_history_list)).setLayoutManager(new LinearLayoutManager(requireContext()));
            getParentFragmentManager().setFragmentResultListener("cierre_guardado", getViewLifecycleOwner(), (key, data) -> load());
            load();
        }
        @Override public void onResume() { super.onResume(); load(); }
        private void load() {
            View view = getView();
            if (view == null) return;
            List<JSONObject> rows = new ArrayList<>();
            try {
                JSONArray data = ((CierreFragment) requireParentFragment()).history();
                for (int i = data.length() - 1; i >= 0; i--) rows.add(data.getJSONObject(i));
            } catch (JSONException failure) {
                ((TextView) view.findViewById(R.id.cierre_history_empty).findViewById(R.id.cierre_history_empty_title)).setText(R.string.cierre_history_error);
            }
            ((RecyclerView) view.findViewById(R.id.cierre_history_list)).setAdapter(new HistoryAdapter(rows));
            view.findViewById(R.id.cierre_history_empty).setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.Holder> {
        private final List<JSONObject> rows;
        HistoryAdapter(List<JSONObject> rows) { this.rows = rows; }
        static class Holder extends RecyclerView.ViewHolder { Holder(View view) { super(view); } }
        @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cierre, parent, false));
        }
        @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
            JSONObject row = rows.get(position);
            View v = holder.itemView;
            Context ctx = v.getContext();
            BigDecimal expected = new BigDecimal(row.optString("esperado", "0"));
            BigDecimal handed = new BigDecimal(row.optString("entregado", "0"));
            boolean legacy = row.has("contado");
            BigDecimal compared = legacy ? new BigDecimal(row.optString("contado", "0")) : handed;
            BigDecimal diff = compared.subtract(expected);
            String date = LocalDate.parse(row.optString("fecha")).format(DateTimeFormatter.ofPattern("dd MMM yyyy", new Locale("es", "SV")));
            ((TextView) v.findViewById(R.id.history_date)).setText(date);
            ((TextView) v.findViewById(R.id.history_count)).setText(ctx.getString(R.string.cierre_history_count, row.optInt("atenciones"), row.optString("empleado")));
            ((TextView) v.findViewById(R.id.history_difference)).setText(signedMoney(diff));
            TextView label = v.findViewById(R.id.history_status);
            label.setText(status(diff));
            int bg = diff.signum() < 0 ? R.color.ink : diff.signum() > 0 ? R.color.gold : R.color.bg;
            GradientDrawable chip = new GradientDrawable();
            chip.setColor(ctx.getColor(bg));
            chip.setCornerRadius(8 * ctx.getResources().getDisplayMetrics().density);
            chip.setStroke((int) ctx.getResources().getDisplayMetrics().density, ctx.getColor(R.color.ink));
            label.setBackground(chip);
            label.setTextColor(ctx.getColor(diff.signum() < 0 ? R.color.bg : R.color.ink));
            ((TextView) v.findViewById(R.id.history_detail)).setText(legacy
                    ? ctx.getString(R.string.cierre_history_detail_legacy, money(expected), money(compared), money(handed))
                    : ctx.getString(R.string.cierre_history_detail, money(expected), money(handed)));
        }
        @Override public int getItemCount() { return rows.size(); }
    }
}

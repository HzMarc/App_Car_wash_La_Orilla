package com.example.car_wash_la_orilla;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Consulta de los importes entregados en los cierres de un empleado, agrupados por día. */
public class HistorialEmpleadoBottomSheet extends BottomSheetDialogFragment {
    static HistorialEmpleadoBottomSheet newInstance(String id, String name) {
        HistorialEmpleadoBottomSheet sheet = new HistorialEmpleadoBottomSheet();
        Bundle args = new Bundle();
        args.putString("employee_id", id);
        args.putString("employee_name", name);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.bottom_sheet_historial_empleado, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        ViewCompat.setAccessibilityHeading(view.findViewById(R.id.employee_history_title), true);
        ViewCompat.setOnApplyWindowInsetsListener(view, (root, insets) -> {
            root.setPadding(0, 0, 0, insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom);
            return insets;
        });
        ((TextView) view.findViewById(R.id.employee_history_name)).setText(requireArguments().getString("employee_name"));
        view.findViewById(R.id.employee_history_close).setOnClickListener(v -> dismiss());
        RecyclerView list = view.findViewById(R.id.employee_history_list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        TextView empty = view.findViewById(R.id.employee_history_empty);
        TextView total = view.findViewById(R.id.employee_history_total);
        try {
            // Misma fuente que CierreActivity. Filtrar por ID, nunca por nombre editable.
            String saved = requireContext().getSharedPreferences("cierres", Context.MODE_PRIVATE).getString("historial", "[]");
            JSONArray history = new JSONArray(saved);
            Map<LocalDate, BigDecimal> byDay = new TreeMap<>(Collections.reverseOrder());
            for (int i = 0; i < history.length(); i++) {
                JSONObject row = history.getJSONObject(i);
                if (!requireArguments().getString("employee_id").equals(row.optString("empleadoId"))) continue;
                LocalDate date = LocalDate.parse(row.getString("fecha"));
                BigDecimal delivered = new BigDecimal(row.getString("entregado")).setScale(2, RoundingMode.UNNECESSARY);
                if (delivered.signum() < 0) throw new JSONException("Invalid delivered amount");
                byDay.merge(date, delivered, BigDecimal::add);
            }
            List<Map.Entry<LocalDate, BigDecimal>> days = new ArrayList<>(byDay.entrySet());
            BigDecimal sum = BigDecimal.ZERO;
            for (Map.Entry<LocalDate, BigDecimal> day : days) sum = sum.add(day.getValue());
            list.setAdapter(new DayAdapter(days));
            total.setText(money(sum));
            empty.setVisibility(days.isEmpty() ? View.VISIBLE : View.GONE);
        } catch (JSONException | NumberFormatException | ArithmeticException | DateTimeParseException error) {
            empty.setText(R.string.empleados_history_error);
            empty.setVisibility(View.VISIBLE);
            total.setText("—");
        }
    }

    @Override public void onStart() {
        super.onStart();
        BottomSheetDialog dialog = (BottomSheetDialog) requireDialog();
        BottomSheetBehavior<FrameLayout> behavior = dialog.getBehavior();
        int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.85f);
        behavior.setMaxWidth((int) (600 * getResources().getDisplayMetrics().density));
        behavior.setMaxHeight(height);
        FrameLayout sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (sheet != null) {
            ViewGroup.LayoutParams params = sheet.getLayoutParams();
            params.height = height;
            sheet.setLayoutParams(params);
            sheet.setBackgroundTintList(ColorStateList.valueOf(requireContext().getColor(R.color.bg)));
        }
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        if (dialog.getWindow() != null) dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    private static String money(BigDecimal amount) { return "$" + amount.setScale(2).toPlainString(); }

    private static class DayAdapter extends RecyclerView.Adapter<DayAdapter.Holder> {
        private final List<Map.Entry<LocalDate, BigDecimal>> days;
        private final DateTimeFormatter dates = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", new Locale("es", "SV"));
        DayAdapter(List<Map.Entry<LocalDate, BigDecimal>> days) { this.days = days; }
        static class Holder extends RecyclerView.ViewHolder { Holder(View view) { super(view); } }
        @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_entrega_empleado, parent, false));
        }
        @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
            Map.Entry<LocalDate, BigDecimal> day = days.get(position);
            ((TextView) holder.itemView.findViewById(R.id.entrega_fecha)).setText(day.getKey().format(dates));
            ((TextView) holder.itemView.findViewById(R.id.entrega_monto)).setText(money(day.getValue()));
        }
        @Override public int getItemCount() { return days.size(); }
    }
}

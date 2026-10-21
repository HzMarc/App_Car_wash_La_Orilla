package com.example.car_wash_la_orilla;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;

public class EmpleadoDialogFragment extends DialogFragment {
    private View form;
    private boolean available = true;

    static EmpleadoDialogFragment newInstance(@Nullable String id) {
        EmpleadoDialogFragment dialog = new EmpleadoDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putString("employee_id", id);
        dialog.setArguments(arguments);
        return dialog;
    }

    @NonNull @Override public Dialog onCreateDialog(@Nullable Bundle state) {
        form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_empleado, null);
        String id = requireArguments().getString("employee_id");
        TextInputEditText name = form.findViewById(R.id.empleado_nombre_input);
        MaterialSwitch active = form.findViewById(R.id.empleado_estado_input);
        if (id != null) {
            try {
                EmpleadosStore.Empleado employee = new EmpleadosStore(requireContext()).find(id);
                if (employee == null) throw new JSONException("Missing employee");
                name.setText(employee.nombre);
                active.setChecked(employee.activo);
            } catch (JSONException error) {
                available = false;
                Toast.makeText(requireContext(), R.string.empleados_missing, Toast.LENGTH_LONG).show();
            }
        }
        if (state != null) {
            name.setText(state.getString("name"));
            active.setChecked(state.getBoolean("active", true));
        }
        active.setText(active.isChecked() ? R.string.empleados_active : R.string.empleados_inactive);
        active.setOnCheckedChangeListener((button, checked) -> active.setText(checked ? R.string.empleados_active : R.string.empleados_inactive));
        return DialogoOrilla.crear(requireContext(),
                id == null ? R.string.empleados_add : R.string.empleados_edit,
                form, R.string.empleados_cancel, R.string.empleados_save);
    }

    @Override public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) requireDialog();
        dialog.findViewById(android.R.id.button1).setEnabled(available);
        dialog.findViewById(android.R.id.button1).setOnClickListener(v -> save());
        Window window = dialog.getWindow();
        if (window != null) {
            float density = getResources().getDisplayMetrics().density;
            window.setLayout(Math.min(getResources().getDisplayMetrics().widthPixels - (int) (32 * density),
                    (int) (400 * density)), WindowManager.LayoutParams.WRAP_CONTENT);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    @Override public void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state);
        if (form != null) {
            state.putString("name", String.valueOf(((TextInputEditText) form.findViewById(R.id.empleado_nombre_input)).getText()));
            state.putBoolean("active", ((MaterialSwitch) form.findViewById(R.id.empleado_estado_input)).isChecked());
        }
    }

    private void save() {
        TextInputEditText input = form.findViewById(R.id.empleado_nombre_input);
        String name = String.valueOf(input.getText()).trim();
        TextInputLayout field = form.findViewById(R.id.empleado_nombre_layout);
        field.setError(name.isEmpty() ? getString(R.string.empleados_name_required) : null);
        if (name.isEmpty()) { input.requestFocus(); return; }
        boolean active = ((MaterialSwitch) form.findViewById(R.id.empleado_estado_input)).isChecked();
        try {
            if (!new EmpleadosStore(requireContext()).save(requireArguments().getString("employee_id"), name, active)) {
                throw new IllegalStateException("Save failed");
            }
            getParentFragmentManager().setFragmentResult(EmpleadosFragment.RESULT_CHANGED, new Bundle());
            Toast.makeText(requireContext(), R.string.empleados_saved, Toast.LENGTH_SHORT).show();
            dismiss();
        } catch (JSONException | IllegalStateException error) {
            Toast.makeText(requireContext(), R.string.empleados_write_error, Toast.LENGTH_LONG).show();
        }
    }

    @Override public void onDestroyView() { super.onDestroyView(); form = null; }
}

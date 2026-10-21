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

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;

public class ServicioDialogFragment extends DialogFragment {
    private View form;
    private boolean available = true;

    static ServicioDialogFragment newInstance(@Nullable String id) {
        ServicioDialogFragment dialog = new ServicioDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putString("service_id", id);
        dialog.setArguments(arguments);
        return dialog;
    }

    @NonNull @Override public Dialog onCreateDialog(@Nullable Bundle state) {
        form = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_catalogo_servicio, null);
        String id = requireArguments().getString("service_id");
        TextInputEditText name = form.findViewById(R.id.service_name_input);
        TextInputEditText price = form.findViewById(R.id.service_price_input);
        ChipGroup vehicle = form.findViewById(R.id.service_vehicle_group);
        if (state != null) {
            name.setText(state.getString("name"));
            price.setText(state.getString("price"));
            vehicle.check(state.getInt("vehicle", R.id.service_car));
        } else if (id != null) {
            try {
                CatalogoStore.Servicio service = new CatalogoStore(requireContext()).find(id);
                if (service == null) throw new JSONException("Missing service");
                name.setText(service.nombre);
                price.setText(service.precio());
                vehicle.check(service.vehiculo.equals("moto") ? R.id.service_motorcycle : R.id.service_car);
            } catch (JSONException error) {
                available = false;
                Toast.makeText(requireContext(), R.string.catalogo_missing, Toast.LENGTH_LONG).show();
            }
        }
        return DialogoOrilla.crear(requireContext(),
                id == null ? R.string.catalogo_new : R.string.catalogo_edit_title,
                form, R.string.catalogo_cancel, R.string.catalogo_save);
    }

    @Override public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) requireDialog();
        dialog.findViewById(android.R.id.button1).setEnabled(available);
        // Reemplazar el listener para que los errores de validación no cierren el diálogo.
        dialog.findViewById(android.R.id.button1).setOnClickListener(v -> save());
        Window window = dialog.getWindow();
        if (window != null) {
            float density = getResources().getDisplayMetrics().density;
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            window.setLayout(Math.min(screenWidth - (int) (32 * density), (int) (400 * density)), WindowManager.LayoutParams.WRAP_CONTENT);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    @Override public void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state);
        if (form != null) {
            state.putString("name", String.valueOf(((TextInputEditText) form.findViewById(R.id.service_name_input)).getText()));
            state.putString("price", String.valueOf(((TextInputEditText) form.findViewById(R.id.service_price_input)).getText()));
            state.putInt("vehicle", ((ChipGroup) form.findViewById(R.id.service_vehicle_group)).getCheckedChipId());
        }
    }

    private void save() {
        TextInputEditText nameInput = form.findViewById(R.id.service_name_input);
        TextInputEditText priceInput = form.findViewById(R.id.service_price_input);
        TextInputLayout nameLayout = form.findViewById(R.id.service_name_layout);
        TextInputLayout priceLayout = form.findViewById(R.id.service_price_layout);
        String name = String.valueOf(nameInput.getText()).trim();
        Long cents = CatalogoStore.parsePrice(String.valueOf(priceInput.getText()));
        nameLayout.setError(name.isEmpty() ? getString(R.string.catalogo_name_required) : null);
        priceLayout.setError(cents == null ? getString(R.string.catalogo_invalid_price) : null);
        if (name.isEmpty() || cents == null) return;
        String vehicle = ((ChipGroup) form.findViewById(R.id.service_vehicle_group)).getCheckedChipId() == R.id.service_motorcycle ? "moto" : "carro";
        try {
            boolean saved = new CatalogoStore(requireContext()).save(requireArguments().getString("service_id"), name, vehicle, cents);
            if (!saved) throw new IllegalStateException("Save failed");
            getParentFragmentManager().setFragmentResult(CatalogoFragment.RESULT_CHANGED, new Bundle());
            Toast.makeText(requireContext(), R.string.catalogo_saved, Toast.LENGTH_SHORT).show();
            dismiss();
        } catch (JSONException | IllegalStateException error) {
            Toast.makeText(requireContext(), R.string.catalogo_write_error, Toast.LENGTH_LONG).show();
        }
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        form = null;
    }
}

package com.example.car_wash_la_orilla;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Formulario y confirmación explícita; conserva su estado al rotar. */
public class AperturaDialogFragment extends DialogFragment {
    private TextInputEditText input;
    private TextInputLayout inputLayout;
    private TextView confirmacion;
    private boolean confirmando;
    private LocalDate fecha;

    @NonNull @Override public Dialog onCreateDialog(Bundle state) {
        fecha = LocalDate.parse(state == null ? LocalDate.now().toString() : state.getString("fecha"));
        confirmando = state != null && state.getBoolean("confirmando");
        android.view.View form = getLayoutInflater().inflate(R.layout.dialog_apertura, null);
        input = form.findViewById(R.id.apertura_monto);
        inputLayout = form.findViewById(R.id.apertura_monto_layout);
        confirmacion = form.findViewById(R.id.apertura_confirmacion);
        if (state != null) input.setText(state.getString("monto", ""));
        AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.apertura_title).setView(form)
                .setNegativeButton(R.string.apertura_cancelar, null)
                .setPositiveButton(R.string.apertura_continuar, null).create();
        dialog.setOnShowListener(ignored -> {
            actualizar(dialog);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (SesionStore.requerir(requireActivity()) == null) { dismiss(); return; }
                BigDecimal monto = AperturasStore.importe(String.valueOf(input.getText()));
                if (monto == null) { inputLayout.setError(getString(R.string.cierre_invalid_amount)); return; }
                if (!confirmando) {
                    inputLayout.setError(null);
                    confirmando = true;
                    ((android.view.inputmethod.InputMethodManager) requireContext().getSystemService(
                            android.content.Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(input.getWindowToken(), 0);
                    actualizar(dialog);
                    return;
                }
                try {
                    if (!new AperturasStore(requireContext()).abrir(fecha, monto)) throw new IllegalStateException();
                    getParentFragmentManager().setFragmentResult("apertura_guardada", new Bundle());
                    Toast.makeText(requireContext(), R.string.apertura_guardada, Toast.LENGTH_SHORT).show();
                    dismiss();
                } catch (org.json.JSONException | IllegalStateException error) {
                    Toast.makeText(requireContext(), R.string.apertura_error, Toast.LENGTH_LONG).show();
                    getParentFragmentManager().setFragmentResult("apertura_guardada", new Bundle());
                    dismiss();
                }
            });
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
                if (confirmando) { confirmando = false; actualizar(dialog); } else dismiss();
            });
        });
        return dialog;
    }

    private void actualizar(AlertDialog dialog) {
        inputLayout.setVisibility(confirmando ? android.view.View.GONE : android.view.View.VISIBLE);
        confirmacion.setVisibility(confirmando ? android.view.View.VISIBLE : android.view.View.GONE);
        confirmacion.setText(getString(R.string.apertura_confirmar_monto, String.valueOf(input.getText()).trim()));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(confirmando ? R.string.apertura_confirmar : R.string.apertura_continuar);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setText(confirmando ? R.string.apertura_editar : R.string.apertura_cancelar);
    }

    @Override public void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("fecha", fecha.toString());
        state.putString("monto", String.valueOf(input.getText()));
        state.putBoolean("confirmando", confirmando);
    }
}

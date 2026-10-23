package com.example.car_wash_la_orilla;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class QrTransferenciaBottomSheet extends BottomSheetDialogFragment {
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.bottom_sheet_qr_transferencia, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        ViewCompat.setAccessibilityHeading(view.findViewById(R.id.transferencia_qr_title), true);
        ((ImageView) view.findViewById(R.id.transferencia_qr))
                .setImageBitmap(new QrNegocioStore(requireContext()).cargar());
        view.findViewById(R.id.transferencia_qr_cerrar).setOnClickListener(v -> dismiss());
    }

    @Override public void onStart() {
        super.onStart();
        if (SesionStore.requerir(requireActivity()) == null) {
            dismiss();
            return;
        }
        BottomSheetDialog dialog = (BottomSheetDialog) requireDialog();
        dialog.getBehavior().setMaxWidth((int) (600 * getResources().getDisplayMetrics().density));
        dialog.getBehavior().setSkipCollapsed(true);
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
    }
}

package com.example.car_wash_la_orilla;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Mismo título, fondo blanco y fila de botones que los diálogos importados. */
final class DialogoOrilla {
    private DialogoOrilla() { }

    static AlertDialog crear(Context context, int titulo, View formulario, int cancelar, int guardar) {
        float densidad = context.getResources().getDisplayMetrics().density;
        int espacio = Math.round(20 * densidad);
        LinearLayout contenido = new LinearLayout(context);
        contenido.setOrientation(LinearLayout.VERTICAL);
        contenido.setPadding(0, espacio, 0, espacio);
        TextView encabezado = new TextView(context);
        encabezado.setText(titulo);
        encabezado.setTextColor(context.getColor(R.color.app_black));
        encabezado.setTextSize(20);
        encabezado.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        encabezado.setPadding(espacio, 0, espacio, Math.round(8 * densidad));
        contenido.addView(encabezado);
        contenido.addView(formulario, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout acciones = new LinearLayout(context);
        acciones.setOrientation(LinearLayout.HORIZONTAL);
        acciones.setPadding(espacio, Math.round(8 * densidad), espacio, 0);
        MaterialButton botonCancelar = boton(context, cancelar, false);
        MaterialButton botonGuardar = boton(context, guardar, true);
        botonCancelar.setId(android.R.id.button2);
        botonGuardar.setId(android.R.id.button1);
        LinearLayout.LayoutParams izquierda = new LinearLayout.LayoutParams(0, Math.round(52 * densidad), 1);
        izquierda.setMarginEnd(Math.round(6 * densidad));
        LinearLayout.LayoutParams derecha = new LinearLayout.LayoutParams(0, Math.round(52 * densidad), 1);
        derecha.setMarginStart(Math.round(6 * densidad));
        acciones.addView(botonCancelar, izquierda);
        acciones.addView(botonGuardar, derecha);
        contenido.addView(acciones);
        GradientDrawable fondo = new GradientDrawable();
        fondo.setColor(context.getColor(R.color.app_card_bg));
        fondo.setCornerRadius(16 * densidad);
        AlertDialog dialogo = new MaterialAlertDialogBuilder(context)
                .setBackground(fondo).setView(contenido).create();
        botonCancelar.setOnClickListener(v -> dialogo.dismiss());
        return dialogo;
    }

    private static MaterialButton boton(Context context, int texto, boolean principal) {
        float densidad = context.getResources().getDisplayMetrics().density;
        MaterialButton boton = new MaterialButton(context);
        boton.setText(texto);
        boton.setTextSize(14);
        boton.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        boton.setCornerRadius(Math.round(10 * densidad));
        boton.setInsetTop(0);
        boton.setInsetBottom(0);
        boton.setElevation(0);
        boton.setTextColor(context.getColor(principal ? R.color.white : R.color.app_black));
        boton.setBackgroundTintList(ColorStateList.valueOf(principal ? context.getColor(R.color.app_black) : Color.TRANSPARENT));
        if (!principal) {
            boton.setStrokeColor(ColorStateList.valueOf(context.getColor(R.color.app_black)));
            boton.setStrokeWidth(Math.round(1.5f * densidad));
        }
        return boton;
    }
}

package com.example.car_wash_la_orilla;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Conserva identificadores estables y empleados inactivos para referencias históricas. */
final class EmpleadosStore {
    private final SharedPreferences preferences;

    EmpleadosStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences("empleados", Context.MODE_PRIVATE);
    }

    static final class Empleado {
        final String id, nombre;
        final boolean activo;
        Empleado(String id, String nombre, boolean activo) {
            this.id = id;
            this.nombre = nombre;
            this.activo = activo;
        }
    }

    List<Empleado> getAll() throws JSONException {
        JSONArray data = new JSONArray(preferences.getString("empleados", "[]"));
        List<Empleado> employees = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) {
            JSONObject row = data.getJSONObject(i);
            String id = row.getString("id"), name = row.getString("nombre");
            if (id.trim().isEmpty() || name.trim().isEmpty()) throw new JSONException("Invalid employee");
            employees.add(new Empleado(id, name, row.getBoolean("activo")));
        }
        return employees;
    }

    Empleado find(String id) throws JSONException {
        for (Empleado employee : getAll()) if (employee.id.equals(id)) return employee;
        return null;
    }

    boolean save(String id, String name, boolean active) throws JSONException {
        String normalized = name.trim();
        if (normalized.isEmpty() || normalized.length() > 80) return false;
        List<Empleado> employees = getAll();
        if (id == null) {
            employees.add(new Empleado(UUID.randomUUID().toString(), normalized, active));
        } else {
            int index = indexOf(employees, id);
            if (index < 0) return false;
            employees.set(index, new Empleado(id, normalized, active));
        }
        return write(employees);
    }

    boolean setActive(String id, boolean active) throws JSONException {
        List<Empleado> employees = getAll();
        int index = indexOf(employees, id);
        if (index < 0) return false;
        Empleado employee = employees.get(index);
        employees.set(index, new Empleado(employee.id, employee.nombre, active));
        return write(employees);
    }

    private int indexOf(List<Empleado> employees, String id) {
        for (int i = 0; i < employees.size(); i++) if (employees.get(i).id.equals(id)) return i;
        return -1;
    }

    private boolean write(List<Empleado> employees) throws JSONException {
        JSONArray data = new JSONArray();
        for (Empleado employee : employees) {
            JSONObject row = new JSONObject();
            row.put("id", employee.id);
            row.put("nombre", employee.nombre);
            row.put("activo", employee.activo);
            data.put(row);
        }
        return preferences.edit().putString("empleados", data.toString()).commit();
    }
}

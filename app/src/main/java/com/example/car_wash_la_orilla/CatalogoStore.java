package com.example.car_wash_la_orilla;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Almacenamiento local provisional del catálogo; no borra servicios al desactivarlos. */
final class CatalogoStore {
    private final SharedPreferences preferences;

    CatalogoStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences("catalogo_servicios", Context.MODE_PRIVATE);
    }

    static final class Servicio {
        final String id, nombre, vehiculo;
        final long precioCentavos;
        final boolean activo;

        Servicio(String id, String nombre, String vehiculo, long precioCentavos, boolean activo) {
            this.id = id;
            this.nombre = nombre;
            this.vehiculo = vehiculo;
            this.precioCentavos = precioCentavos;
            this.activo = activo;
        }

        String precio() { return BigDecimal.valueOf(precioCentavos, 2).toPlainString(); }
    }

    static Long parsePrice(String text) {
        String value = text.trim().replace(',', '.');
        if (!value.matches("[0-9]{1,6}(\\.[0-9]{1,2})?")) return null;
        long cents = new BigDecimal(value).movePointRight(2).longValueExact();
        return cents > 0 ? cents : null;
    }

    List<Servicio> getAll() throws JSONException {
        JSONArray json = new JSONArray(preferences.getString("servicios", "[]"));
        List<Servicio> result = new ArrayList<>();
        for (int i = 0; i < json.length(); i++) {
            JSONObject row = json.getJSONObject(i);
            String id = row.getString("id"), name = row.getString("nombre"), vehicle = row.getString("vehiculo");
            long price = row.getLong("precioCentavos");
            if (id.isEmpty() || name.trim().isEmpty() || price <= 0 || price > 99999999
                    || !(vehicle.equals("carro") || vehicle.equals("moto"))) {
                throw new JSONException("Invalid service");
            }
            result.add(new Servicio(id, name, vehicle, price, row.getBoolean("activo")));
        }
        return result;
    }

    Servicio find(String id) throws JSONException {
        for (Servicio service : getAll()) if (service.id.equals(id)) return service;
        return null;
    }

    boolean save(String id, String name, String vehicle, long cents) throws JSONException {
        List<Servicio> services = getAll();
        if (id == null) {
            services.add(new Servicio(UUID.randomUUID().toString(), name, vehicle, cents, true));
        } else {
            int index = indexOf(services, id);
            if (index < 0) return false;
            // Mantener el estado actual al editar, incluso tras recrear el diálogo.
            services.set(index, new Servicio(id, name, vehicle, cents, services.get(index).activo));
        }
        return write(services);
    }

    boolean setActive(String id, boolean active) throws JSONException {
        List<Servicio> services = getAll();
        int index = indexOf(services, id);
        if (index < 0) return false;
        Servicio old = services.get(index);
        services.set(index, new Servicio(old.id, old.nombre, old.vehiculo, old.precioCentavos, active));
        return write(services);
    }

    private int indexOf(List<Servicio> services, String id) {
        for (int i = 0; i < services.size(); i++) if (services.get(i).id.equals(id)) return i;
        return -1;
    }

    private boolean write(List<Servicio> services) throws JSONException {
        JSONArray data = new JSONArray();
        for (Servicio service : services) {
            JSONObject row = new JSONObject();
            row.put("id", service.id);
            row.put("nombre", service.nombre);
            row.put("vehiculo", service.vehiculo);
            row.put("precioCentavos", service.precioCentavos);
            row.put("activo", service.activo);
            data.put(row);
        }
        return preferences.edit().putString("servicios", data.toString()).commit();
    }
}

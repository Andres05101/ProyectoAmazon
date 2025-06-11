package com.example.workflow.model;

import java.util.HashMap;
import java.util.Map;

public class Bodega {

    private String nombre;
    private int ubicacion;
    private Map<String, Integer> inventario = new HashMap<>();

    public Bodega(String nombre, int ubicacion) {
        this.nombre = nombre;
        this.ubicacion = ubicacion;
    }

    public String getNombre() {
        return nombre;
    }

    public int getUbicacion() {
        return ubicacion;
    }

    public Map<String, Integer> getInventario() {
        return inventario;
    }

    public void agregarProducto(String producto, int cantidad) {
        inventario.put(producto, inventario.getOrDefault(producto, 0) + cantidad);
    }

    public boolean tieneProducto(String producto) {
        return inventario.getOrDefault(producto, 0) > 0;
    }

    public void descontarProducto(String producto) {
        inventario.put(producto, inventario.get(producto) - 1);
    }
}

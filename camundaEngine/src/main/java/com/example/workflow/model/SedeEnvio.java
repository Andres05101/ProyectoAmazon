package com.example.workflow.model;

public class SedeEnvio {

    private String nombre;
    private int ubicacion; // valor entre 0 y 100

    public SedeEnvio(String nombre, int ubicacion) {
        this.nombre = nombre;
        this.ubicacion = ubicacion;
    }

    public String getNombre() {
        return nombre;
    }

    public int getUbicacion() {
        return ubicacion;
    }
}

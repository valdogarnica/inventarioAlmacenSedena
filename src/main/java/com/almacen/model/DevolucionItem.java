package com.almacen.model;

public class DevolucionItem {
    private int herramientaId;
    private int cantidadDevolver;
    private String observacion;
    private String nombreHerramienta;

    public int getHerramientaId() {
        return herramientaId;
    }

    public void setHerramientaId(int herramientaId) {
        this.herramientaId = herramientaId;
    }

    public int getCantidadDevolver() {
        return cantidadDevolver;
    }

    public void setCantidadDevolver(int cantidadDevolver) {
        this.cantidadDevolver = cantidadDevolver;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getNombreHerramienta() {
        return nombreHerramienta;
    }

    public void setNombreHerramienta(String nombreHerramienta) {
        this.nombreHerramienta = nombreHerramienta;
    }
}

package com.almacen.model;

public class DetallePrestamo {
    private int id;
    private int prestamoId;
    private int herramientaId;
    private String nombreHerramienta;
    private String categoria;
    private int cantidad;
    private int cantidadDevuelta;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPrestamoId() {
        return prestamoId;
    }

    public void setPrestamoId(int prestamoId) {
        this.prestamoId = prestamoId;
    }

    public int getHerramientaId() {
        return herramientaId;
    }

    public void setHerramientaId(int herramientaId) {
        this.herramientaId = herramientaId;
    }

    public String getNombreHerramienta() {
        return nombreHerramienta;
    }

    public void setNombreHerramienta(String nombreHerramienta) {
        this.nombreHerramienta = nombreHerramienta;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public int getCantidadDevuelta() {
        return cantidadDevuelta;
    }

    public void setCantidadDevuelta(int cantidadDevuelta) {
        this.cantidadDevuelta = cantidadDevuelta;
    }

    public int getPendiente() {
        return Math.max(0, cantidad - cantidadDevuelta);
    }
}

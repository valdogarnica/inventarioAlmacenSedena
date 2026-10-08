package com.almacen.model;

/**
 * Existencia acumulada de un material: suma el stock de todos los proveedores que lo
 * surten (mismo nombre y unidad) y guarda el desglose por proveedor.
 */
public class ExistenciaMaterial {
    private String nombre;
    private String unidad;
    private String categoria;
    private String tipo;
    private int numProveedores;
    private String desglose;
    private int disponible;
    private int prestado;
    private String ultimaFecha;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public int getNumProveedores() {
        return numProveedores;
    }

    public void setNumProveedores(int numProveedores) {
        this.numProveedores = numProveedores;
    }

    /** Texto "proveedor: stock" de cada proveedor, separado por " | ". */
    public String getDesglose() {
        return desglose;
    }

    public void setDesglose(String desglose) {
        this.desglose = desglose;
    }

    public int getDisponible() {
        return disponible;
    }

    public void setDisponible(int disponible) {
        this.disponible = disponible;
    }

    public int getPrestado() {
        return prestado;
    }

    public void setPrestado(int prestado) {
        this.prestado = prestado;
    }

    public int getTotal() {
        return disponible + prestado;
    }

    public String getUltimaFecha() {
        return ultimaFecha;
    }

    public void setUltimaFecha(String ultimaFecha) {
        this.ultimaFecha = ultimaFecha;
    }
}

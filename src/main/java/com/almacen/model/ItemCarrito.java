package com.almacen.model;

public class ItemCarrito {
    private Herramienta herramienta;
    private int cantidad;
    
    public ItemCarrito(Herramienta herramienta, int cantidad) {
        this.herramienta = herramienta;
        this.cantidad = cantidad;
    }
    
    public Herramienta getHerramienta() {
        return herramienta;
    }
    
    public void setHerramienta(Herramienta herramienta) {
        this.herramienta = herramienta;
    }
    
    public int getCantidad() {
        return cantidad;
    }
    
    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }
    
    public int getIdHerramienta() {
        return herramienta.getId();
    }
    
    public String getNombre() {
        return herramienta.getNombre();
    }
    
    public String getCategoria() {
        return herramienta.getCategoria();
    }

    public String getUnidad() {
        return herramienta.getUnidad();
    }

    public String getProveedorNombre() {
        return herramienta.getProveedorNombre();
    }
}

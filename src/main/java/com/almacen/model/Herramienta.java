package com.almacen.model;

public class Herramienta {
    private int id;
    private String nombre;
    private String categoria;
    private int stock;
    private String descripcion;
    private int estado; // 1 = alta, 0 = baja
    
    public Herramienta() {
    }
    
    public Herramienta(int id, String nombre, String categoria, int stock, String descripcion, int estado) {
        this.id = id;
        this.nombre = nombre;
        this.categoria = categoria;
        this.stock = stock;
        this.descripcion = descripcion;
        this.estado = estado;
    }
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    
    public String getCategoria() {
        return categoria;
    }
    
    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }
    
    public int getStock() {
        return stock;
    }
    
    public void setStock(int stock) {
        this.stock = stock;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public int getEstado() {
        return estado;
    }

    public void setEstado(int estado) {
        this.estado = estado;
    }
    
    @Override
    public String toString() {
        return nombre + " - " + categoria + " (Stock: " + stock + ")";
    }
}

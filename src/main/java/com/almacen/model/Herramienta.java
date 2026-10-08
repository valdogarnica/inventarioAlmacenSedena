package com.almacen.model;

public class Herramienta {
    private int id;
    private String nombre;
    private String categoria;
    private String tipo;
    private String unidad;
    private Integer proveedorId;
    private String proveedorNombre;
    private String remision;
    private String fechaRegistro;
    private int stock;
    private int cantidadPrestada;
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

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    public Integer getProveedorId() {
        return proveedorId;
    }

    public void setProveedorId(Integer proveedorId) {
        this.proveedorId = proveedorId;
    }

    public String getProveedorNombre() {
        return proveedorNombre;
    }

    public void setProveedorNombre(String proveedorNombre) {
        this.proveedorNombre = proveedorNombre;
    }

    public String getRemision() {
        return remision;
    }

    public void setRemision(String remision) {
        this.remision = remision;
    }

    public String getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(String fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
    
    public int getStock() {
        return stock;
    }
    
    public void setStock(int stock) {
        this.stock = stock;
    }

    public int getCantidadPrestada() {
        return cantidadPrestada;
    }

    public void setCantidadPrestada(int cantidadPrestada) {
        this.cantidadPrestada = cantidadPrestada;
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
        StringBuilder sb = new StringBuilder(nombre);
        if (proveedorNombre != null && !proveedorNombre.trim().isEmpty()) {
            sb.append(" [").append(proveedorNombre).append("]");
        }
        sb.append(" - ").append(categoria).append(" (Stock: ").append(stock);
        if (unidad != null && !unidad.trim().isEmpty()) {
            sb.append(" ").append(unidad);
        }
        sb.append(")");
        return sb.toString();
    }
}

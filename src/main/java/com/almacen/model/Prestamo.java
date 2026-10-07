package com.almacen.model;

import java.time.LocalDateTime;

public class Prestamo {
    private int id;
    private int herramientaId;
    private String nombreHerramienta;
    private String categoria;
    private int cantidad;
    private String nombreCliente;
    private String nombreEmpleado;
    private LocalDateTime fechaPrestamo;
    private LocalDateTime fechaDevolucion;
    private String estado; // "PRESTADO", "DEVUELTO"
    private String residenteSobrestante;
    private boolean autorizacion;
    private String folio;
    private String fotoNombre;
    
    public Prestamo() {
    }
    
    public Prestamo(int id, int herramientaId, String nombreHerramienta, String categoria, 
                   int cantidad, String nombreCliente, String nombreEmpleado, 
                   LocalDateTime fechaPrestamo, LocalDateTime fechaDevolucion, String estado,
                   String residenteSobrestante, boolean autorizacion, String folio, String fotoNombre) {
        this.id = id;
        this.herramientaId = herramientaId;
        this.nombreHerramienta = nombreHerramienta;
        this.categoria = categoria;
        this.cantidad = cantidad;
        this.nombreCliente = nombreCliente;
        this.nombreEmpleado = nombreEmpleado;
        this.fechaPrestamo = fechaPrestamo;
        this.fechaDevolucion = fechaDevolucion;
        this.estado = estado;
        this.residenteSobrestante = residenteSobrestante;
        this.autorizacion = autorizacion;
        this.folio = folio;
        this.fotoNombre = fotoNombre;
    }
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
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
    
    public String getNombreCliente() {
        return nombreCliente;
    }
    
    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }
    
    public String getNombreEmpleado() {
        return nombreEmpleado;
    }
    
    public void setNombreEmpleado(String nombreEmpleado) {
        this.nombreEmpleado = nombreEmpleado;
    }
    
    public LocalDateTime getFechaPrestamo() {
        return fechaPrestamo;
    }
    
    public void setFechaPrestamo(LocalDateTime fechaPrestamo) {
        this.fechaPrestamo = fechaPrestamo;
    }
    
    public LocalDateTime getFechaDevolucion() {
        return fechaDevolucion;
    }
    
    public void setFechaDevolucion(LocalDateTime fechaDevolucion) {
        this.fechaDevolucion = fechaDevolucion;
    }
    
    public String getEstado() {
        return estado;
    }
    
    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getResidenteSobrestante() {
        return residenteSobrestante;
    }

    public void setResidenteSobrestante(String residenteSobrestante) {
        this.residenteSobrestante = residenteSobrestante;
    }

    public boolean isAutorizacion() {
        return autorizacion;
    }

    public void setAutorizacion(boolean autorizacion) {
        this.autorizacion = autorizacion;
    }

    public String getFolio() {
        return folio;
    }

    public void setFolio(String folio) {
        this.folio = folio;
    }

    public String getFotoNombre() {
        return fotoNombre;
    }

    public void setFotoNombre(String fotoNombre) {
        this.fotoNombre = fotoNombre;
    }
}

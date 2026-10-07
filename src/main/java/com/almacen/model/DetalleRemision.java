package com.almacen.model;

import java.time.LocalDate;

public class DetalleRemision {
    private int id;
    private int remisionId;
    private int herramientaId;
    private String nombreMaterial;
    private String descripcion;
    private String categoria;
    private String tipo;
    private String unidad;
    private int cantidad;
    // Datos del encabezado (solo para reportes)
    private String numeroRemision;
    private String proveedorNombre;
    private LocalDate fechaRemision;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRemisionId() {
        return remisionId;
    }

    public void setRemisionId(int remisionId) {
        this.remisionId = remisionId;
    }

    public int getHerramientaId() {
        return herramientaId;
    }

    public void setHerramientaId(int herramientaId) {
        this.herramientaId = herramientaId;
    }

    public String getNombreMaterial() {
        return nombreMaterial;
    }

    public void setNombreMaterial(String nombreMaterial) {
        this.nombreMaterial = nombreMaterial;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
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

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public String getNumeroRemision() {
        return numeroRemision;
    }

    public void setNumeroRemision(String numeroRemision) {
        this.numeroRemision = numeroRemision;
    }

    public String getProveedorNombre() {
        return proveedorNombre;
    }

    public void setProveedorNombre(String proveedorNombre) {
        this.proveedorNombre = proveedorNombre;
    }

    public LocalDate getFechaRemision() {
        return fechaRemision;
    }

    public void setFechaRemision(LocalDate fechaRemision) {
        this.fechaRemision = fechaRemision;
    }
}

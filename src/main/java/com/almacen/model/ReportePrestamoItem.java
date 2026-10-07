package com.almacen.model;

import java.time.LocalDateTime;

public class ReportePrestamoItem {
    private String nombreCliente;
    private String residenteSobrestante;
    private LocalDateTime fechaPrestamo;
    private String nombreHerramienta;
    private String categoria;
    private int cantidad;

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public String getResidenteSobrestante() {
        return residenteSobrestante;
    }

    public void setResidenteSobrestante(String residenteSobrestante) {
        this.residenteSobrestante = residenteSobrestante;
    }

    public LocalDateTime getFechaPrestamo() {
        return fechaPrestamo;
    }

    public void setFechaPrestamo(LocalDateTime fechaPrestamo) {
        this.fechaPrestamo = fechaPrestamo;
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
}

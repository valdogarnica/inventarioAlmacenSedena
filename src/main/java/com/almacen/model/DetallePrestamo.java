package com.almacen.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DetallePrestamo {
    private int id;
    private int prestamoId;
    private int herramientaId;
    private String nombreHerramienta;
    private String categoria;
    private int cantidad;
    private int cantidadDevuelta;
    private boolean noRetorno;
    private String unidad;
    /** Partidas reales cuando esta fila junta el mismo material de varios proveedores. */
    private List<DetallePrestamo> partes;

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
        return noRetorno ? 0 : Math.max(0, cantidad - cantidadDevuelta);
    }

    /** Material de no retorno: se entregó con el préstamo y no se devuelve. */
    public boolean isNoRetorno() {
        return noRetorno;
    }

    public void setNoRetorno(boolean noRetorno) {
        this.noRetorno = noRetorno;
    }

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    /** Las partidas guardadas que forman esta fila (ella misma si no es una fila unida). */
    public List<DetallePrestamo> getPartes() {
        return partes != null ? partes : Collections.singletonList(this);
    }

    /**
     * Junta en una sola fila el mismo material (nombre y unidad) que se prestó de varios
     * proveedores, sumando cantidades. Conserva el orden de la primera aparición.
     */
    public static List<DetallePrestamo> agrupar(List<DetallePrestamo> detalles) {
        Map<String, List<DetallePrestamo>> grupos = new LinkedHashMap<>();
        for (DetallePrestamo d : detalles) {
            String clave = clave(d.nombreHerramienta) + "|" + clave(d.unidad) + "|" + d.noRetorno;
            grupos.computeIfAbsent(clave, k -> new ArrayList<>()).add(d);
        }
        List<DetallePrestamo> resultado = new ArrayList<>();
        for (List<DetallePrestamo> grupo : grupos.values()) {
            if (grupo.size() == 1) {
                resultado.add(grupo.get(0));
                continue;
            }
            DetallePrestamo primero = grupo.get(0);
            DetallePrestamo unido = new DetallePrestamo();
            unido.id = primero.id;
            unido.prestamoId = primero.prestamoId;
            unido.herramientaId = primero.herramientaId;
            unido.nombreHerramienta = primero.nombreHerramienta;
            unido.categoria = primero.categoria;
            unido.unidad = primero.unidad;
            unido.noRetorno = primero.noRetorno;
            for (DetallePrestamo d : grupo) {
                unido.cantidad += d.cantidad;
                unido.cantidadDevuelta += d.cantidadDevuelta;
            }
            unido.partes = new ArrayList<>(grupo);
            resultado.add(unido);
        }
        return resultado;
    }

    private static String clave(String texto) {
        return texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
    }
}

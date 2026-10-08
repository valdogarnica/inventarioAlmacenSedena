package com.almacen.model;

import java.util.ArrayList;
import java.util.List;

/** Datos de la página Inicio: indicadores, gráficas y listas de atención. */
public class ResumenInicio {
    /** Un valor con su etiqueta (una barra de una gráfica). */
    public static class Dato {
        public final String etiqueta;
        public final int valor;

        public Dato(String etiqueta, int valor) {
            this.etiqueta = etiqueta;
            this.valor = valor;
        }
    }

    /** Material sumando todos sus proveedores. */
    public static class Material {
        public final String nombre;
        public final String unidad;
        public final String tipo;
        public final int disponible;
        public final int prestado;

        public Material(String nombre, String unidad, String tipo, int disponible, int prestado) {
            this.nombre = nombre;
            this.unidad = unidad;
            this.tipo = tipo;
            this.disponible = disponible;
            this.prestado = prestado;
        }
    }

    /** Préstamo activo con lo que le falta devolver. */
    public static class PrestamoPendiente {
        public final int id;
        public final String cliente;
        public final String fecha;
        public final long dias;
        public final int pendientes;

        public PrestamoPendiente(int id, String cliente, String fecha, long dias, int pendientes) {
            this.id = id;
            this.cliente = cliente;
            this.fecha = fecha;
            this.dias = dias;
            this.pendientes = pendientes;
        }
    }

    public int umbralBajo;
    public int materiales;
    public int disponible;
    public int prestado;
    public int prestamosActivos;
    public int entregadoNoRetorno;
    public final List<Material> sinStock = new ArrayList<>();
    public final List<Material> stockBajo = new ArrayList<>();
    public final List<Dato> stockPorTipo = new ArrayList<>();
    public final List<Dato> stockPorProveedor = new ArrayList<>();
    public final List<Dato> masPrestados = new ArrayList<>();
    public final List<Dato> masEntregadosNoRetorno = new ArrayList<>();
    public final List<Dato> prestamosPorMes = new ArrayList<>();
    public final List<PrestamoPendiente> prestamosAntiguos = new ArrayList<>();
}

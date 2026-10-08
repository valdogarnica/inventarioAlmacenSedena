package com.almacen.database;

/**
 * Catálogos simples (lista de nombres) que el usuario puede administrar:
 * categorías, tipos y unidades. Cada uno tiene su tabla y la columna
 * correspondiente en la tabla de herramientas/materiales.
 */
public enum Catalogo {
    CATEGORIAS("categorias", "categoria", "Categoría", "Categorías", "Nueva categoría"),
    TIPOS("tipos", "tipo", "Tipo", "Tipos", "Nuevo tipo"),
    UNIDADES("unidades", "unidad", "Unidad", "Unidades", "Nueva unidad");

    private final String tabla;
    private final String columna;
    private final String singular;
    private final String plural;
    private final String etiquetaNuevo;

    Catalogo(String tabla, String columna, String singular, String plural, String etiquetaNuevo) {
        this.tabla = tabla;
        this.columna = columna;
        this.singular = singular;
        this.plural = plural;
        this.etiquetaNuevo = etiquetaNuevo;
    }

    public String getTabla() {
        return tabla;
    }

    public String getColumna() {
        return columna;
    }

    public String getSingular() {
        return singular;
    }

    public String getPlural() {
        return plural;
    }

    /** "Nueva categoría", "Nuevo tipo", "Nueva unidad". */
    public String getEtiquetaNuevo() {
        return etiquetaNuevo;
    }
}

package com.almacen.model;

public class Proveedor {
    private int id;
    private String nombre;
    private String contacto;
    private String telefono;
    private int totalMateriales;
    private int totalRemisiones;

    public Proveedor() {
    }

    public Proveedor(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
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

    public String getContacto() {
        return contacto;
    }

    public void setContacto(String contacto) {
        this.contacto = contacto;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public int getTotalMateriales() {
        return totalMateriales;
    }

    public void setTotalMateriales(int totalMateriales) {
        this.totalMateriales = totalMateriales;
    }

    public int getTotalRemisiones() {
        return totalRemisiones;
    }

    public void setTotalRemisiones(int totalRemisiones) {
        this.totalRemisiones = totalRemisiones;
    }

    @Override
    public String toString() {
        return nombre;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Proveedor)) {
            return false;
        }
        return id == ((Proveedor) o).id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}

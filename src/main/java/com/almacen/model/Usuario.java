package com.almacen.model;

/**
 * Usuario que entra al sistema. El administrador registra a los empleados; el nombre
 * del usuario en sesión es el "empleado en turno" que registra préstamos y remisiones.
 */
public class Usuario {
    public static final String ROL_ADMIN = "ADMIN";
    public static final String ROL_EMPLEADO = "EMPLEADO";

    private int id;
    private String usuario;
    private String nombre;
    private String rol = ROL_EMPLEADO;
    private boolean activo = true;

    public Usuario() {
    }

    public Usuario(int id, String usuario, String nombre, String rol, boolean activo) {
        this.id = id;
        this.usuario = usuario;
        this.nombre = nombre;
        this.rol = rol;
        this.activo = activo;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public boolean isAdmin() {
        return ROL_ADMIN.equals(rol);
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}

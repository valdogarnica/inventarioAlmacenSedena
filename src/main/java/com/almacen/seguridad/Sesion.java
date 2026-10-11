package com.almacen.seguridad;

import com.almacen.model.Usuario;

/**
 * Sesión local. Al iniciar la aplicación las credenciales se capturan antes de elegir la
 * base de datos y se guardan aquí, en memoria, hasta validarlas contra los usuarios de
 * esa base; después solo queda el usuario autenticado.
 */
public final class Sesion {
    private static String usuarioPendiente;
    private static char[] contrasenaPendiente;
    private static Usuario actual;

    private Sesion() {
    }

    /** Guarda en memoria lo que se escribió en el inicio de sesión. */
    public static void capturarCredenciales(String usuario, char[] contrasena) {
        limpiarCredenciales();
        usuarioPendiente = usuario;
        contrasenaPendiente = contrasena;
    }

    public static String getUsuarioPendiente() {
        return usuarioPendiente;
    }

    public static char[] getContrasenaPendiente() {
        return contrasenaPendiente;
    }

    public static boolean hayCredenciales() {
        return usuarioPendiente != null && contrasenaPendiente != null;
    }

    /** Las credenciales ya se validaron: queda el usuario y se borra la contraseña de memoria. */
    public static void iniciar(Usuario usuario) {
        actual = usuario;
        limpiarCredenciales();
    }

    public static Usuario getUsuario() {
        return actual;
    }

    public static boolean esAdmin() {
        return actual != null && actual.isAdmin();
    }

    public static void cerrar() {
        actual = null;
        limpiarCredenciales();
    }

    private static void limpiarCredenciales() {
        if (contrasenaPendiente != null) {
            java.util.Arrays.fill(contrasenaPendiente, '\0');
        }
        contrasenaPendiente = null;
    }
}

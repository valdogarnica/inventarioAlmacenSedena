package com.almacen.ui;

import com.almacen.database.DatabaseManager;
import com.almacen.seguridad.Contrasenas;
import com.almacen.seguridad.Sesion;
import javax.swing.JOptionPane;
import java.awt.Component;
import java.io.File;

/**
 * Acciones reservadas al administrador (crear bases de datos) y validaciones comunes
 * de nombres de base y contraseñas.
 */
public final class AdministracionSistema {
    private AdministracionSistema() {
    }

    /**
     * Valida el nombre que escribió el usuario y lo devuelve con extensión .db,
     * o null (después de avisar) si no sirve.
     */
    static String nombreArchivoBase(Component parent, String nombre) {
        String nombreBD = nombre == null ? "" : nombre.trim();
        if (nombreBD.isEmpty()) {
            Notificaciones.showMessageDialog(parent, "Ingrese un nombre para la base de datos",
                "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        if (nombreBD.matches(".*[\\\\/:*?\"<>|].*")) {
            Notificaciones.showMessageDialog(parent,
                "El nombre no puede llevar estos caracteres: \\ / : * ? \" < > |",
                "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        return nombreBD.toLowerCase().endsWith(".db") ? nombreBD : nombreBD + ".db";
    }

    /** Crea una base nueva en la carpeta de información (solo administrador). */
    static void crearBaseDatos(Component parent) {
        if (!Sesion.esAdmin()) {
            Notificaciones.showMessageDialog(parent, "Solo el administrador puede crear bases de datos",
                "Acceso denegado", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String ruta = AppPreferences.getDbFolderPath();
        File carpeta = ruta.isEmpty() ? null : new File(ruta);
        if (carpeta == null || !carpeta.isDirectory()) {
            Notificaciones.showMessageDialog(parent, "No se encontró la carpeta de información",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String nombre = Alerta.pedirTexto(parent,
            "Nombre de la nueva base de datos. Se guardará en " + carpeta.getAbsolutePath()
                + " y tendrá su propio administrador inicial (usuario " + Contrasenas.ADMIN_INICIAL
                + ", contraseña " + Contrasenas.CONTRASENA_INICIAL + ").",
            "Crear base de datos", null);
        if (nombre == null) {
            return;
        }
        String nombreBD = nombreArchivoBase(parent, nombre);
        if (nombreBD == null) {
            return;
        }
        File archivo = new File(carpeta, nombreBD);
        if (archivo.exists()) {
            Notificaciones.showMessageDialog(parent, "Ya existe una base de datos con ese nombre",
                "Información", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (DatabaseManager.crearBaseDatos(archivo.getAbsolutePath())) {
            AppPreferences.carpetaFotos(carpeta, nombreBD).mkdirs();
            Notificaciones.exito(parent, "Base de datos creada: " + nombreBD
                + ". Para usarla, cierre sesión y elíjala al conectar.");
        } else {
            Notificaciones.showMessageDialog(parent, "Error al crear la base de datos",
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Revisa una contraseña nueva y su confirmación. Devuelve el mensaje de error,
     * o null si es válida.
     */
    static String validarContrasenaNueva(char[] contrasena, char[] confirmacion) {
        if (contrasena.length < Contrasenas.LONGITUD_MINIMA) {
            return "La contraseña debe tener al menos " + Contrasenas.LONGITUD_MINIMA + " caracteres";
        }
        if (!java.util.Arrays.equals(contrasena, confirmacion)) {
            return "La contraseña y su confirmación no coinciden";
        }
        return null;
    }

    /** Usuario válido: letras, números, punto, guion o guion bajo, sin espacios. */
    static String validarNombreUsuario(String usuario) {
        if (usuario.isEmpty()) {
            return "El usuario es obligatorio";
        }
        if (!usuario.matches("[\\p{L}0-9._-]{3,30}")) {
            return "El usuario debe tener de 3 a 30 caracteres: letras, números, punto, guion o guion bajo, sin espacios";
        }
        return null;
    }
}

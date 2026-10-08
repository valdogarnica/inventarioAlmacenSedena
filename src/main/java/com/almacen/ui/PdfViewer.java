package com.almacen.ui;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.concurrent.Callable;

/**
 * Genera un reporte y lo abre con el visor de PDF predeterminado.
 */
public final class PdfViewer {
    private PdfViewer() {
    }

    public static void generarYAbrir(Component parent, Callable<File> generador, String mensajeSinDatos) {
        // Se genera en el hilo de la interfaz porque la conexión SQLite es compartida
        Window ventana = parent instanceof Window ? (Window) parent : SwingUtilities.getWindowAncestor(parent);
        Cursor anterior = ventana != null ? ventana.getCursor() : null;
        if (ventana != null) {
            ventana.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }
        File archivo;
        try {
            archivo = generador.call();
        } catch (Exception e) {
            Notificaciones.showMessageDialog(parent,
                "Error al generar reporte: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        } finally {
            if (ventana != null) {
                ventana.setCursor(anterior);
            }
        }
        if (archivo == null) {
            Notificaciones.showMessageDialog(parent, mensajeSinDatos,
                "Información", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        abrir(parent, archivo);
    }

    public static void abrir(Component parent, File archivo) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(archivo);
            } else if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(archivo.toURI());
            } else {
                Notificaciones.showMessageDialog(parent,
                    "Reporte generado en: " + archivo.getAbsolutePath(),
                    "Información", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            Notificaciones.showMessageDialog(parent,
                "Reporte generado en: " + archivo.getAbsolutePath() + "\n(No se pudo abrir: " + e.getMessage() + ")",
                "Información", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}

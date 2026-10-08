package com.almacen.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.fonts.inter.FlatInterFont;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Cursor;
import java.util.prefs.Preferences;

/**
 * Modo claro y oscuro de la aplicación. Los colores de cada modo viven en
 * resources/com/almacen/temas (claves App.*); los componentes los leen con
 * {@link #color(String)} o con referencias "$App.clave" en sus estilos FlatLaf,
 * así al cambiar de modo todo se vuelve a pintar con la paleta nueva.
 */
public final class Tema {
    private static final Preferences PREFS = Preferences.userNodeForPackage(Tema.class);
    private static final String PREF_OSCURO = "tema_oscuro";
    private static boolean instalado;
    /** Sube cada vez que cambia el modo; los componentes fuera de la ventana lo comparan. */
    private static int version;
    private static final String VERSION = "Tema.version";

    private Tema() {
    }

    /** Aplica el tema guardado (claro por omisión). Se llama una vez al iniciar. */
    public static void instalar() {
        if (!instalado) {
            // Fuente Inter (incluida en el programa) para un aspecto uniforme en cualquier PC
            FlatInterFont.install();
            FlatLaf.setPreferredFontFamily(FlatInterFont.FAMILY);
            FlatLaf.setPreferredLightFontFamily(FlatInterFont.FAMILY_LIGHT);
            FlatLaf.setPreferredSemiboldFontFamily(FlatInterFont.FAMILY_SEMIBOLD);
            FlatLaf.registerCustomDefaultsSource("com.almacen.temas");
            instalado = true;
        }
        aplicarLaf(esOscuro());
    }

    public static boolean esOscuro() {
        return PREFS.getBoolean(PREF_OSCURO, false);
    }

    /** Cambia entre claro y oscuro con una transición suave y recuerda la elección. */
    public static void setOscuro(boolean oscuro) {
        if (oscuro == esOscuro() && instalado) {
            return;
        }
        PREFS.putBoolean(PREF_OSCURO, oscuro);
        terminarEdiciones();
        version++;
        FlatAnimatedLafChange.showSnapshot();
        aplicarLaf(oscuro);
        FlatLaf.updateUI();
        FlatAnimatedLafChange.hideSnapshotWithAnimation();
    }

    /**
     * Pone al día con el modo actual un componente que no siempre está en la ventana,
     * como el editor de una celda (FlatLaf solo actualiza lo que está en pantalla al cambiar
     * de modo). Los editores de celda lo llaman antes de mostrarse.
     */
    public static <T extends java.awt.Component> T alDia(T componente) {
        if (componente instanceof javax.swing.JComponent) {
            javax.swing.JComponent c = (javax.swing.JComponent) componente;
            Object v = c.getClientProperty(VERSION);
            // La primera vez también: pudo crearse antes de un cambio de modo
            if (!Integer.valueOf(version).equals(v)) {
                javax.swing.SwingUtilities.updateComponentTreeUI(c);
                c.putClientProperty(VERSION, version);
            }
        }
        return componente;
    }

    /** Termina (o cancela) la edición de las tablas antes de cambiar de modo. */
    private static void terminarEdiciones() {
        for (java.awt.Window w : java.awt.Window.getWindows()) {
            terminarEdiciones(w);
        }
    }

    private static void terminarEdiciones(java.awt.Component c) {
        if (c instanceof javax.swing.JTable) {
            javax.swing.JTable t = (javax.swing.JTable) c;
            if (t.isEditing()) {
                try {
                    if (!t.getCellEditor().stopCellEditing()) {
                        t.getCellEditor().cancelCellEditing();
                    }
                } catch (RuntimeException e) {
                    if (t.getCellEditor() != null) {
                        t.getCellEditor().cancelCellEditing();
                    }
                }
            }
        }
        if (c instanceof java.awt.Container) {
            for (java.awt.Component hijo : ((java.awt.Container) c).getComponents()) {
                terminarEdiciones(hijo);
            }
        }
    }

    /** Color del modo actual, por ejemplo color("App.muted"). */
    public static Color color(String clave) {
        Color c = UIManager.getColor(clave);
        return c != null ? c : Color.GRAY;
    }

    private static void aplicarLaf(boolean oscuro) {
        try {
            UIManager.setLookAndFeel(oscuro ? new FlatDarkLaf() : new FlatLightLaf());
            UIManager.put("defaultFont", UIManager.getFont("defaultFont").deriveFont(13f));
            UIManager.put("Button.cursor", Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } catch (Exception e) {
            System.err.println("Error al configurar el tema: " + e.getMessage());
        }
    }
}

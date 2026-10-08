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
        FlatAnimatedLafChange.showSnapshot();
        aplicarLaf(oscuro);
        FlatLaf.updateUI();
        FlatAnimatedLafChange.hideSnapshotWithAnimation();
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

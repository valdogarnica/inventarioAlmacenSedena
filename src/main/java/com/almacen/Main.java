package com.almacen;

import com.almacen.ui.MainWindow;
import javax.swing.*;
import java.awt.Image;
import javax.imageio.ImageIO;

public class Main {
    public static void main(String[] args) {
        configurarTema();
        com.almacen.ui.Mayusculas.instalar();
        
        // Configurar estilo de Swing
        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            // Cargar icono desde resources y aplicarlo a la ventana principal
            try {
                java.net.URL iconUrl = Main.class.getResource("/icono.png");
                if (iconUrl != null) {
                    Image icon = ImageIO.read(iconUrl);
                    window.setIconImage(icon);
                }
            } catch (Exception ignored) {
                // Si falla, continuar sin icono
            }
            window.setVisible(true);
        });
    }

    /** Tema visual de la aplicación (modo claro u oscuro guardado por el usuario). */
    public static void configurarTema() {
        com.almacen.ui.Tema.instalar();
    }
}

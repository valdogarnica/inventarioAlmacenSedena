package com.almacen;

import com.almacen.model.Usuario;
import com.almacen.seguridad.Sesion;
import com.almacen.ui.AppPreferences;
import com.almacen.ui.ConfiguracionDBDialog;
import com.almacen.ui.InicioSesionDialog;
import com.almacen.ui.MainWindow;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        configurarTema();
        com.almacen.ui.Mayusculas.instalar();
        SwingUtilities.invokeLater(Main::iniciarSesion);
    }

    /**
     * Inicio de sesión local: primero se piden usuario y contraseña (quedan en memoria),
     * después se elige la base de datos y, al conectar, se validan contra sus usuarios.
     * Si no son válidas se vuelve a pedirlas; "Salir" cierra la aplicación.
     */
    public static void iniciarSesion() {
        while (true) {
            String sugerido = Sesion.getUsuarioPendiente() != null
                ? Sesion.getUsuarioPendiente() : AppPreferences.getUltimoUsuario();
            InicioSesionDialog login = new InicioSesionDialog(null, sugerido);
            login.setVisible(true);
            if (!login.isAceptado()) {
                System.exit(0);
            }
            ConfiguracionDBDialog seleccion = new ConfiguracionDBDialog(null);
            seleccion.setVisible(true);
            Usuario usuario = seleccion.getUsuarioAutenticado();
            if (usuario != null) {
                new MainWindow(usuario).setVisible(true);
                return;
            }
        }
    }

    /** Tema visual de la aplicación (modo claro u oscuro guardado por el usuario). */
    public static void configurarTema() {
        com.almacen.ui.Tema.instalar();
    }
}

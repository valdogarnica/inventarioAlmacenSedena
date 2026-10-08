package com.almacen.ui;

import com.almacen.database.Catalogo;
import com.almacen.database.DatabaseManager;
import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Combo para elegir un valor de un catálogo (categoría, tipo o unidad) con botón
 * para dar de alta un valor nuevo sin salir del formulario.
 */
public class CatalogoSelector extends JPanel {
    private final Catalogo catalogo;
    private final JComboBox<String> combo = new JComboBox<>();

    public CatalogoSelector(Catalogo catalogo) {
        super(new BorderLayout(8, 0));
        this.catalogo = catalogo;
        setOpaque(false);
        ComboBuscable.instalar(combo);
        add(combo, BorderLayout.CENTER);
        JButton btnNuevo = new JButton(catalogo == Catalogo.TIPOS ? "Nuevo" : "Nueva");
        btnNuevo.setToolTipText(catalogo.getEtiquetaNuevo());
        UIStyles.styleSecondaryButton(btnNuevo);
        UIStyles.applySvgIcon(btnNuevo, "/icons/add.svg", 16);
        btnNuevo.addActionListener(e -> agregarNuevo());
        add(btnNuevo, BorderLayout.EAST);
        recargar();
    }

    public JComboBox<String> getCombo() {
        return combo;
    }

    public void recargar() {
        Object actual = combo.getSelectedItem();
        try {
            DatabaseManager db = DatabaseManager.getInstance();
            if (!db.isConnected()) {
                return;
            }
            List<String> valores = db.obtenerCatalogo(catalogo);
            combo.removeAllItems();
            for (String v : valores) {
                combo.addItem(v);
            }
            if (actual != null) {
                setSeleccion(actual.toString());
            }
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this,
                "Error al cargar " + catalogo.getPlural().toLowerCase() + ": " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public String getSeleccion() {
        Object v = combo.getSelectedItem();
        return v != null ? v.toString().trim() : "";
    }

    public void setSeleccion(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return;
        }
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).equalsIgnoreCase(valor.trim())) {
                combo.setSelectedIndex(i);
                return;
            }
        }
        // Valor que no está en el catálogo (datos antiguos): se muestra igual
        combo.addItem(valor.trim());
        combo.setSelectedItem(valor.trim());
    }

    private void agregarNuevo() {
        String nombre = Alerta.pedirTexto(this, "Escriba el nombre.", catalogo.getEtiquetaNuevo(), null);
        if (nombre == null || nombre.trim().isEmpty()) {
            return;
        }
        try {
            boolean creada = DatabaseManager.getInstance().agregarCatalogo(catalogo, nombre.trim());
            if (!creada) {
                Notificaciones.showMessageDialog(this,
                    "Ese valor ya existe en " + catalogo.getPlural().toLowerCase(),
                    "Información", JOptionPane.INFORMATION_MESSAGE);
            }
            recargar();
            setSeleccion(nombre.trim());
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this,
                "Error: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

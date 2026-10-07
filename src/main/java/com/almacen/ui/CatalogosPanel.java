package com.almacen.ui;

import com.almacen.database.Catalogo;
import com.almacen.database.DatabaseManager;
import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Página para administrar los catálogos: categorías, tipos y unidades.
 */
public class CatalogosPanel extends JPanel implements Pagina {
    private final ListaCatalogo[] listas;

    public CatalogosPanel() {
        super(new GridLayout(1, 3, 12, 12));
        setOpaque(false);
        listas = new ListaCatalogo[]{
            new ListaCatalogo(Catalogo.CATEGORIAS),
            new ListaCatalogo(Catalogo.TIPOS),
            new ListaCatalogo(Catalogo.UNIDADES)
        };
        for (ListaCatalogo l : listas) {
            add(l);
        }
    }

    @Override
    public void alMostrar() {
        for (ListaCatalogo l : listas) {
            l.cargar();
        }
    }

    /** Lista de un catálogo con agregar y renombrar. */
    private static class ListaCatalogo extends JPanel {
        private final Catalogo catalogo;
        private final DefaultListModel<String> modelo = new DefaultListModel<>();
        private final JList<String> lista = new JList<>(modelo);
        private final JTextField txtNuevo = new JTextField();
        private final JTextField txtRenombrar = new JTextField();

        ListaCatalogo(Catalogo catalogo) {
            super(new BorderLayout(8, 8));
            this.catalogo = catalogo;
            setOpaque(false);

            lista.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting() && lista.getSelectedValue() != null) {
                    txtRenombrar.setText(lista.getSelectedValue());
                }
            });
            lista.setFixedCellHeight(26);
            add(UIStyles.createCard(catalogo.getPlural(), new JScrollPane(lista)), BorderLayout.CENTER);

            JPanel sur = new JPanel(new GridLayout(2, 1, 6, 6));
            sur.setOpaque(false);

            JPanel agregar = new JPanel(new BorderLayout(6, 0));
            agregar.setOpaque(false);
            agregar.add(txtNuevo, BorderLayout.CENTER);
            JButton btnAgregar = new JButton("Agregar");
            UIStyles.stylePrimaryButton(btnAgregar);
            UIStyles.applySvgIcon(btnAgregar, "/icons/add.svg", 16);
            btnAgregar.addActionListener(e -> agregar());
            txtNuevo.addActionListener(e -> agregar());
            agregar.add(btnAgregar, BorderLayout.EAST);
            sur.add(UIStyles.createCard(catalogo.getEtiquetaNuevo(), agregar));

            JPanel renombrar = new JPanel(new BorderLayout(6, 0));
            renombrar.setOpaque(false);
            renombrar.add(txtRenombrar, BorderLayout.CENTER);
            JButton btnRenombrar = new JButton("Renombrar");
            UIStyles.styleSecondaryButton(btnRenombrar);
            UIStyles.applySvgIcon(btnRenombrar, "/icons/edit.svg", 16);
            btnRenombrar.addActionListener(e -> renombrar());
            renombrar.add(btnRenombrar, BorderLayout.EAST);
            sur.add(UIStyles.createCard("Renombrar selección", renombrar));
            add(sur, BorderLayout.SOUTH);
        }

        void cargar() {
            try {
                DatabaseManager db = DatabaseManager.getInstance();
                if (!db.isConnected()) {
                    return;
                }
                List<String> valores = db.obtenerCatalogo(catalogo);
                modelo.clear();
                for (String v : valores) {
                    modelo.addElement(v);
                }
            } catch (Exception e) {
                Notificaciones.showMessageDialog(this, "Error al cargar " + catalogo.getPlural().toLowerCase() + ": " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void agregar() {
            String nombre = txtNuevo.getText().trim();
            if (nombre.isEmpty()) {
                return;
            }
            try {
                if (!DatabaseManager.getInstance().agregarCatalogo(catalogo, nombre)) {
                    Notificaciones.showMessageDialog(this, "Ese valor ya existe",
                        "Información", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                txtNuevo.setText("");
                cargar();
                lista.setSelectedValue(nombre, true);
            } catch (Exception e) {
                Notificaciones.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void renombrar() {
            String seleccion = lista.getSelectedValue();
            String nuevo = txtRenombrar.getText().trim();
            if (seleccion == null) {
                Notificaciones.showMessageDialog(this, "Seleccione un valor para renombrar",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (nuevo.isEmpty() || nuevo.equals(seleccion)) {
                return;
            }
            try {
                if (!DatabaseManager.getInstance().renombrarCatalogo(catalogo, seleccion, nuevo)) {
                    Notificaciones.showMessageDialog(this, "No se pudo renombrar (puede que ya exista)",
                        "Información", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                txtRenombrar.setText("");
                cargar();
                lista.setSelectedValue(nuevo, true);
            } catch (Exception e) {
                Notificaciones.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

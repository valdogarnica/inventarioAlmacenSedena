package com.almacen.ui;

import com.almacen.database.DatabaseManager;
import javax.swing.*;
import java.awt.*;
import java.util.List;

public class CategoriasDialog extends JDialog {
    private DefaultListModel<String> modelo;
    private JList<String> lista;
    private JTextField txtNueva;
    private JButton btnAgregar;
    private JTextField txtEditar;
    private JButton btnRenombrar;

    public CategoriasDialog(JFrame parent) {
        super(parent, "Categorías", true);
        initComponents();
        cargarCategorias();
    }

    private void initComponents() {
        setSize(500, 400);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(UIStyles.BG);

        modelo = new DefaultListModel<>();
        lista = new JList<>(modelo);
        lista.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String seleccion = lista.getSelectedValue();
                if (seleccion != null) {
                    txtEditar.setText(seleccion);
                }
            }
        });
        JScrollPane scroll = new JScrollPane(lista);
        add(UIStyles.createCard("Lista de categorías", scroll), BorderLayout.CENTER);

        JPanel panelSur = new JPanel(new GridLayout(2, 1, 8, 8));
        panelSur.setOpaque(false);

        JPanel panelFormulario = new JPanel(new BorderLayout(8, 0));
        panelFormulario.setOpaque(false);
        JLabel lblNueva = new JLabel("Nueva categoría:");
        lblNueva.setForeground(UIStyles.TEXT);
        panelFormulario.add(lblNueva, BorderLayout.WEST);
        txtNueva = new JTextField();
        panelFormulario.add(txtNueva, BorderLayout.CENTER);
        btnAgregar = new JButton("Agregar");
        btnAgregar.addActionListener(e -> agregarCategoria());
        UIStyles.stylePrimaryButton(btnAgregar);
        panelFormulario.add(btnAgregar, BorderLayout.EAST);
        panelSur.add(UIStyles.createCard("Agregar categoría", panelFormulario));

        JPanel panelEditar = new JPanel(new BorderLayout(8, 0));
        panelEditar.setOpaque(false);
        JLabel lblEditar = new JLabel("Renombrar selección:");
        lblEditar.setForeground(UIStyles.TEXT);
        panelEditar.add(lblEditar, BorderLayout.WEST);
        txtEditar = new JTextField();
        panelEditar.add(txtEditar, BorderLayout.CENTER);
        btnRenombrar = new JButton("Renombrar");
        btnRenombrar.addActionListener(e -> renombrarCategoria());
        UIStyles.styleSecondaryButton(btnRenombrar);
        panelEditar.add(btnRenombrar, BorderLayout.EAST);
        panelSur.add(UIStyles.createCard("Editar categoría", panelEditar));

        add(panelSur, BorderLayout.SOUTH);
    }

    private void cargarCategorias() {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            if (!dbManager.isConnected()) {
                return;
            }
            List<String> categorias = dbManager.obtenerCategorias();
            modelo.clear();
            for (String c : categorias) {
                modelo.addElement(c);
            }
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this,
                "Error al cargar categorías: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void agregarCategoria() {
        String nombre = txtNueva.getText().trim();
        if (nombre.isEmpty()) {
            return;
        }
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            if (!dbManager.isConnected()) {
                Notificaciones.showMessageDialog(this,
                    "No hay conexión a la base de datos",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            boolean creada = dbManager.agregarCategoria(nombre);
            if (!creada) {
                Notificaciones.showMessageDialog(this,
                    "La categoría ya existe",
                    "Información", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            txtNueva.setText("");
            cargarCategorias();
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this,
                "Error: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void renombrarCategoria() {
        String seleccion = lista.getSelectedValue();
        String nuevoNombre = txtEditar.getText().trim();
        if (seleccion == null || seleccion.trim().isEmpty()) {
            Notificaciones.showMessageDialog(this,
                "Seleccione una categoría para renombrar",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (nuevoNombre.isEmpty()) {
            Notificaciones.showMessageDialog(this,
                "Ingrese el nuevo nombre",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            boolean actualizado = dbManager.actualizarCategoria(seleccion, nuevoNombre);
            if (!actualizado) {
                Notificaciones.showMessageDialog(this,
                    "No se pudo renombrar (puede que ya exista)",
                    "Información", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            txtEditar.setText("");
            cargarCategorias();
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this,
                "Error: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

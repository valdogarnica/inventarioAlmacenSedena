package com.almacen.ui;

import com.almacen.database.DatabaseManager;
import com.almacen.model.Herramienta;
import javax.swing.*;
import java.awt.*;
import java.util.List;

public class EditarHerramientaDialog extends JDialog {
    private final int herramientaId;
    private JTextField txtNombre;
    private JComboBox<String> comboCategoria;
    private JTextField txtStock;
    private JTextArea txtDescripcion;
    private JButton btnGuardar;
    private JButton btnCancelar;

    public EditarHerramientaDialog(Window parent, int herramientaId) {
        super(parent, "Editar Herramienta", ModalityType.APPLICATION_MODAL);
        this.herramientaId = herramientaId;
        initComponents();
        cargarCategorias();
        cargarHerramienta();
    }

    private void initComponents() {
        setSize(520, 420);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(UIStyles.BG);

        JPanel panelPrincipal = new JPanel(new GridBagLayout());
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panelPrincipal.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel lblNombre = new JLabel("Nombre:");
        lblNombre.setForeground(UIStyles.TEXT);
        panelPrincipal.add(lblNombre, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        txtNombre = new JTextField(22);
        panelPrincipal.add(txtNombre, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel lblCategoria = new JLabel("Categoría:");
        lblCategoria.setForeground(UIStyles.TEXT);
        panelPrincipal.add(lblCategoria, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        comboCategoria = new JComboBox<>();
        panelPrincipal.add(comboCategoria, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel lblStock = new JLabel("Stock:");
        lblStock.setForeground(UIStyles.TEXT);
        panelPrincipal.add(lblStock, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        txtStock = new JTextField(22);
        panelPrincipal.add(txtStock, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        JLabel lblDescripcion = new JLabel("Descripción:");
        lblDescripcion.setForeground(UIStyles.TEXT);
        panelPrincipal.add(lblDescripcion, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        txtDescripcion = new JTextArea(5, 22);
        txtDescripcion.setLineWrap(true);
        txtDescripcion.setWrapStyleWord(true);
        JScrollPane scrollDescripcion = new JScrollPane(txtDescripcion);
        panelPrincipal.add(scrollDescripcion, gbc);

        add(UIStyles.createCard("Editar herramienta", panelPrincipal), BorderLayout.CENTER);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelBotones.setOpaque(false);
        btnGuardar = new JButton("Guardar");
        btnGuardar.addActionListener(e -> guardarCambios());
        btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());
        UIStyles.stylePrimaryButton(btnGuardar);
        UIStyles.styleDangerButton(btnCancelar);
        panelBotones.add(btnCancelar);
        panelBotones.add(btnGuardar);
        add(panelBotones, BorderLayout.SOUTH);
    }

    private void cargarCategorias() {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            List<String> categorias = dbManager.obtenerCategorias();
            comboCategoria.removeAllItems();
            for (String c : categorias) {
                comboCategoria.addItem(c);
            }
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this,
                "Error al cargar categorías: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarHerramienta() {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            Herramienta herramienta = dbManager.obtenerHerramientaPorId(herramientaId);
            if (herramienta == null) {
                Notificaciones.showMessageDialog(this,
                    "No se encontró la herramienta",
                    "Error", JOptionPane.ERROR_MESSAGE);
                dispose();
                return;
            }
            txtNombre.setText(herramienta.getNombre());
            comboCategoria.setSelectedItem(herramienta.getCategoria());
            txtStock.setText(String.valueOf(herramienta.getStock()));
            txtDescripcion.setText(herramienta.getDescripcion() != null ? herramienta.getDescripcion() : "");
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this,
                "Error al cargar herramienta: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void guardarCambios() {
        String nombre = txtNombre.getText().trim();
        String categoria = (String) comboCategoria.getSelectedItem();
        String stockStr = txtStock.getText().trim();
        String descripcion = txtDescripcion.getText().trim();

        if (nombre.isEmpty() || categoria == null || categoria.trim().isEmpty() || stockStr.isEmpty()) {
            Notificaciones.showMessageDialog(this,
                "Por favor complete todos los campos obligatorios",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            int stock = Integer.parseInt(stockStr);
            if (stock < 0) {
                Notificaciones.showMessageDialog(this,
                    "El stock no puede ser negativo",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            DatabaseManager dbManager = DatabaseManager.getInstance();
            Herramienta herramienta = new Herramienta();
            herramienta.setId(herramientaId);
            herramienta.setNombre(nombre);
            herramienta.setCategoria(categoria);
            herramienta.setStock(stock);
            herramienta.setDescripcion(descripcion);
            dbManager.actualizarHerramienta(herramienta);
            Notificaciones.showMessageDialog(this,
                "Herramienta actualizada",
                "Éxito", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (NumberFormatException e) {
            Notificaciones.showMessageDialog(this,
                "Por favor ingrese un número válido para el stock",
                "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this,
                "Error: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

package com.almacen.ui;

import com.almacen.database.DatabaseManager;
import com.almacen.model.Herramienta;
import javax.swing.*;
import java.awt.*;

public class AgregarHerramientaDialog extends JDialog {
    private JTextField txtNombre;
    private JComboBox<String> comboCategoria;
    private JButton btnNuevaCategoria;
    private JTextField txtStock;
    private JTextArea txtDescripcion;
    private JButton btnGuardar;
    private JButton btnCancelar;
    
    public AgregarHerramientaDialog(JFrame parent) {
        super(parent, "Agregar Herramienta", true);
        initComponents();
    }
    
    private void initComponents() {
        setSize(500, 400);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(UIStyles.BG);
        
        JPanel panelPrincipal = new JPanel(new GridBagLayout());
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panelPrincipal.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Nombre
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel lblNombre = new JLabel("Nombre:");
        lblNombre.setForeground(UIStyles.TEXT);
        panelPrincipal.add(lblNombre, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        txtNombre = new JTextField(20);
        panelPrincipal.add(txtNombre, gbc);
        
        // Categoría
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
        JPanel panelCategoria = new JPanel(new BorderLayout(8, 0));
        panelCategoria.setOpaque(false);
        comboCategoria = new JComboBox<>();
        panelCategoria.add(comboCategoria, BorderLayout.CENTER);
        btnNuevaCategoria = new JButton("Nueva");
        btnNuevaCategoria.addActionListener(e -> agregarCategoria());
        UIStyles.styleSecondaryButton(btnNuevaCategoria);
        UIStyles.applySvgIcon(btnNuevaCategoria, "/icons/add.svg", 16);
        panelCategoria.add(btnNuevaCategoria, BorderLayout.EAST);
        panelPrincipal.add(panelCategoria, gbc);
        
        // Stock
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel lblStock = new JLabel("Stock inicial:");
        lblStock.setForeground(UIStyles.TEXT);
        panelPrincipal.add(lblStock, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        txtStock = new JTextField(20);
        panelPrincipal.add(txtStock, gbc);
        
        // Descripción
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
        txtDescripcion = new JTextArea(5, 20);
        txtDescripcion.setLineWrap(true);
        txtDescripcion.setWrapStyleWord(true);
        JScrollPane scrollDescripcion = new JScrollPane(txtDescripcion);
        panelPrincipal.add(scrollDescripcion, gbc);
        
        add(UIStyles.createCard("Nueva herramienta", panelPrincipal), BorderLayout.CENTER);
        
        // Botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelBotones.setOpaque(false);
        btnGuardar = new JButton("Guardar");
        btnGuardar.addActionListener(e -> guardarHerramienta());
        btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());
        UIStyles.stylePrimaryButton(btnGuardar);
        UIStyles.styleDangerButton(btnCancelar);
        UIStyles.applySvgIcon(btnGuardar, "/icons/save.svg", 16);
        UIStyles.applySvgIcon(btnCancelar, "/icons/cancel.svg", 16);
        panelBotones.add(btnCancelar);
        panelBotones.add(btnGuardar);
        add(panelBotones, BorderLayout.SOUTH);

        cargarCategorias();
    }
    
    private void guardarHerramienta() {
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
            if (!dbManager.isConnected()) {
                Notificaciones.showMessageDialog(this, 
                    "No hay conexión a la base de datos", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            Herramienta herramienta = new Herramienta();
            herramienta.setNombre(nombre);
            herramienta.setCategoria(categoria);
            herramienta.setStock(stock);
            herramienta.setDescripcion(descripcion);
            herramienta.setEstado(1);
            
            dbManager.agregarHerramienta(herramienta);
            
            Notificaciones.showMessageDialog(this, 
                "Herramienta agregada exitosamente", 
                "Éxito", JOptionPane.INFORMATION_MESSAGE);
            limpiarCampos();
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
    
    private void limpiarCampos() {
        txtNombre.setText("");
        txtStock.setText("");
        txtDescripcion.setText("");
        if (comboCategoria.getItemCount() > 0) {
            comboCategoria.setSelectedIndex(0);
        }
        txtNombre.requestFocusInWindow();
    }

    private void cargarCategorias() {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            if (!dbManager.isConnected()) {
                return;
            }
            java.util.List<String> categorias = dbManager.obtenerCategorias();
            comboCategoria.removeAllItems();
            for (String c : categorias) {
                comboCategoria.addItem(c);
            }
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this,"Error al cargar categorías: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void agregarCategoria() {
        String nombre = JOptionPane.showInputDialog(this,
            "Nombre de la nueva categoría:",
            "Agregar Categoría",
            JOptionPane.QUESTION_MESSAGE);
        if (nombre == null || nombre.trim().isEmpty()) {
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
            boolean creada = dbManager.agregarCategoria(nombre.trim());
            if (!creada) {
                Notificaciones.showMessageDialog(this,
                    "La categoría ya existe",
                    "Información", JOptionPane.INFORMATION_MESSAGE);
            }
            cargarCategorias();
            comboCategoria.setSelectedItem(nombre.trim());
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this,
                "Error: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

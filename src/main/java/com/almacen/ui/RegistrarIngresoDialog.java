package com.almacen.ui;

import com.almacen.database.DatabaseManager;
import com.almacen.model.Herramienta;
import javax.swing.*;
import java.awt.*;
import java.util.List;

public class RegistrarIngresoDialog extends JDialog {
    private JComboBox<Herramienta> comboHerramientas;
    private JTextField txtCantidad;
    private JButton btnRegistrar;
    private JButton btnCancelar;
    
    public RegistrarIngresoDialog(Window parent) {
        super(parent, "Registrar Ingreso de Stock", ModalityType.APPLICATION_MODAL);
        initComponents();
        cargarHerramientas();
    }
    
    private void initComponents() {
        setSize(640, 220);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(UIStyles.BG);
        
        JPanel panelPrincipal = new JPanel(new GridBagLayout());
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panelPrincipal.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Herramienta
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel lblHerramienta = new JLabel("Material [proveedor]:");
        lblHerramienta.setForeground(UIStyles.TEXT);
        panelPrincipal.add(lblHerramienta, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        comboHerramientas = new JComboBox<>();
        comboHerramientas.setPreferredSize(new Dimension(420, 30));
        ComboBuscable.instalar(comboHerramientas);
        panelPrincipal.add(comboHerramientas, gbc);
        
        // Cantidad
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel lblCantidad = new JLabel("Cantidad a agregar:");
        lblCantidad.setForeground(UIStyles.TEXT);
        panelPrincipal.add(lblCantidad, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        txtCantidad = new JTextField(20);
        panelPrincipal.add(txtCantidad, gbc);
        
        add(UIStyles.createCard("Ingreso de stock", panelPrincipal), BorderLayout.CENTER);
        
        // Botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelBotones.setOpaque(false);
        btnRegistrar = new JButton("Registrar Ingreso");
        btnRegistrar.addActionListener(e -> registrarIngreso());
        btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());
        UIStyles.stylePrimaryButton(btnRegistrar);
        UIStyles.styleDangerButton(btnCancelar);
        panelBotones.add(btnCancelar);
        panelBotones.add(btnRegistrar);
        add(panelBotones, BorderLayout.SOUTH);
    }
    
    private void cargarHerramientas() {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            if (!dbManager.isConnected()) {
                Notificaciones.showMessageDialog(this, 
                    "No hay conexión a la base de datos", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Buscar todas las herramientas (búsqueda vacía para obtener todas)
            List<Herramienta> herramientas = dbManager.buscarHerramientas("");
            comboHerramientas.removeAllItems();
            
            for (Herramienta h : herramientas) {
                comboHerramientas.addItem(h);
            }
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this, 
                "Error al cargar herramientas: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void registrarIngreso() {
        Herramienta herramienta = (Herramienta) comboHerramientas.getSelectedItem();
        if (herramienta == null) {
            Notificaciones.showMessageDialog(this, 
                "Por favor seleccione una herramienta", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String cantidadStr = txtCantidad.getText().trim();
        if (cantidadStr.isEmpty()) {
            Notificaciones.showMessageDialog(this, 
                "Por favor ingrese la cantidad", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            int cantidad = Integer.parseInt(cantidadStr);
            if (cantidad <= 0) {
                Notificaciones.showMessageDialog(this, 
                    "La cantidad debe ser mayor a 0", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            DatabaseManager dbManager = DatabaseManager.getInstance();
            dbManager.actualizarStock(herramienta.getId(), cantidad);
            
            Notificaciones.showMessageDialog(this, 
                "Ingreso registrado exitosamente. Nuevo stock: " + 
                (herramienta.getStock() + cantidad), 
                "Éxito", JOptionPane.INFORMATION_MESSAGE);
            
            dispose();
        } catch (NumberFormatException e) {
            Notificaciones.showMessageDialog(this, 
                "Por favor ingrese un número válido", 
                "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this, 
                "Error: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

package com.almacen.ui;

import com.almacen.database.DatabaseManager;
import com.almacen.model.DetallePrestamo;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.List;

public class DetallePrestamoDialog extends JDialog {
    private final int prestamoId;
    private JTable tablaDetalle;
    private DefaultTableModel modelo;

    public DetallePrestamoDialog(Window parent, int prestamoId) {
        super(parent, "Herramientas del Préstamo", ModalityType.APPLICATION_MODAL);
        this.prestamoId = prestamoId;
        initComponents();
        cargarDetalle();
    }

    private void initComponents() {
        setSize(800, 500);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(UIStyles.BG);

        String[] columnas = {"Herramienta", "Categoría", "Prestado", "Devuelto", "Pendiente", "Observaciones"};
        modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaDetalle = new JTable(modelo);
        tablaDetalle.setRowHeight(28);
        tablaDetalle.getTableHeader().setReorderingAllowed(false);
        // Estilizar encabezado y centrar números
        UIStyles.styleTableHeader(tablaDetalle);
        aplicarCentradoNumeros();
        tablaDetalle.getColumnModel().getColumn(5).setPreferredWidth(260);
        JScrollPane scroll = new JScrollPane(tablaDetalle);
        add(UIStyles.createCard("Detalle del préstamo", scroll), BorderLayout.CENTER);

        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.addActionListener(e -> dispose());
        UIStyles.styleDangerButton(btnCerrar);
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelBotones.setOpaque(false);
        panelBotones.add(btnCerrar);
        add(panelBotones, BorderLayout.SOUTH);
    }

    private void cargarDetalle() {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            if (!dbManager.isConnected()) {
                Notificaciones.showMessageDialog(this,
                    "No hay conexión a la base de datos",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            List<DetallePrestamo> detalles = dbManager.obtenerDetallesPrestamo(prestamoId);
            java.util.Map<Integer, String> observaciones = dbManager.obtenerObservacionesPrestamo(prestamoId);
            modelo.setRowCount(0);
            for (DetallePrestamo d : detalles) {
                String obs = observaciones.getOrDefault(d.getHerramientaId(), "");
                modelo.addRow(new Object[]{
                    d.getNombreHerramienta(),
                    d.getCategoria(),
                    d.getCantidad(),
                    d.getCantidadDevuelta(),
                    d.getPendiente(),
                    obs
                });
            }
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this,
                "Error al cargar detalle: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void aplicarCentradoNumeros() {
        DefaultTableCellRenderer center = UIStyles.createCenteredNumberRenderer();
        tablaDetalle.getColumnModel().getColumn(2).setCellRenderer(center); // Prestado
        tablaDetalle.getColumnModel().getColumn(3).setCellRenderer(center); // Devuelto
        tablaDetalle.getColumnModel().getColumn(4).setCellRenderer(center); // Pendiente
    }
}

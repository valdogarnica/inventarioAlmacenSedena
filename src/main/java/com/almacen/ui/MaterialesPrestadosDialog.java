package com.almacen.ui;

import com.almacen.database.DatabaseManager;
import com.almacen.model.ReportePrestamoItem;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

public class MaterialesPrestadosDialog extends JDialog {
    private JTable tabla;
    private DefaultTableModel modelo;
    private JComboBox<String> comboMaterial;
    private DefaultComboBoxModel<String> comboModel;
    private List<String> materiales = new ArrayList<>();
    private JButton btnFiltrar;
    private JButton btnLimpiar;

    public MaterialesPrestadosDialog(Window parent) {
        super(parent, "Materiales prestados", ModalityType.APPLICATION_MODAL);
        initComponents();
        cargarDatos();
    }

    private void initComponents() {
        setSize(1150, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(UIStyles.BG);

        JPanel panelFiltro = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelFiltro.setOpaque(false);
        JLabel lblFiltro = new JLabel("Material:");
        lblFiltro.setForeground(UIStyles.TEXT);
        panelFiltro.add(lblFiltro);
        comboModel = new DefaultComboBoxModel<>();
        comboMaterial = new JComboBox<>(comboModel);
        comboMaterial.setEditable(true);
        comboMaterial.setPreferredSize(new Dimension(220, 28));
        comboMaterial.addActionListener(e -> {
            if (comboMaterial.isPopupVisible()) {
                comboMaterial.setPopupVisible(false);
            }
            cargarDatos();
        });
        panelFiltro.add(comboMaterial);
        btnFiltrar = new JButton("Filtrar");
        btnFiltrar.addActionListener(e -> cargarDatos());
        UIStyles.styleSecondaryButton(btnFiltrar);
        panelFiltro.add(btnFiltrar);
        btnLimpiar = new JButton("Limpiar");
        btnLimpiar.addActionListener(e -> {
            comboMaterial.setSelectedItem("");
            actualizarComboFiltrado("");
            cargarDatos();
        });
        UIStyles.styleSecondaryButton(btnLimpiar);
        panelFiltro.add(btnLimpiar);
        add(panelFiltro, BorderLayout.NORTH);

        String[] columnas = {"Material", "Categoría", "Proveedor", "Unidad", "Cantidad", "Cliente", "Residente/Sobrestante", "Fecha préstamo"};
        modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tabla = new JTable(modelo);
        tabla.setRowHeight(28);
        tabla.getTableHeader().setReorderingAllowed(false);
        UIStyles.styleTableHeader(tabla);
        DefaultTableCellRenderer center = UIStyles.createCenteredNumberRenderer();
        tabla.getColumnModel().getColumn(4).setCellRenderer(center);
        JScrollPane scroll = new JScrollPane(tabla);
        add(UIStyles.createCard("Materiales prestados", scroll), BorderLayout.CENTER);

        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.addActionListener(e -> dispose());
        UIStyles.styleDangerButton(btnCerrar);
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelBotones.setOpaque(false);
        panelBotones.add(btnCerrar);
        add(panelBotones, BorderLayout.SOUTH);

        configurarBusquedaCombo();
        cargarMaterialesCombo();
    }

    private void cargarDatos() {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            if (!dbManager.isConnected()) {
                Notificaciones.showMessageDialog(this,
                    "No hay conexión a la base de datos",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String filtro = getFiltroMaterial();
            List<ReportePrestamoItem> items = dbManager.obtenerMaterialesPrestados(filtro);
            modelo.setRowCount(0);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (ReportePrestamoItem item : items) {
                String fecha = item.getFechaPrestamo() != null ? item.getFechaPrestamo().format(formatter) : "";
                modelo.addRow(new Object[]{
                    item.getNombreHerramienta(),
                    item.getCategoria(),
                    item.getProveedor() != null ? item.getProveedor() : "-",
                    item.getUnidad(),
                    item.getCantidad(),
                    item.getNombreCliente(),
                    item.getResidenteSobrestante(),
                    fecha
                });
            }
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this,
                "Error al cargar materiales: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void configurarBusquedaCombo() {
        Component editorComp = comboMaterial.getEditor().getEditorComponent();
        if (editorComp instanceof JTextField) {
            JTextField editor = (JTextField) editorComp;
            editor.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                @Override
                public void insertUpdate(javax.swing.event.DocumentEvent e) {
                    actualizarComboFiltrado(editor.getText());
                }

                @Override
                public void removeUpdate(javax.swing.event.DocumentEvent e) {
                    actualizarComboFiltrado(editor.getText());
                }

                @Override
                public void changedUpdate(javax.swing.event.DocumentEvent e) {
                    actualizarComboFiltrado(editor.getText());
                }
            });
        }
    }

    private void cargarMaterialesCombo() {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            if (!dbManager.isConnected()) {
                return;
            }
            materiales = dbManager.obtenerHerramientasPrestadasActivas();
            actualizarComboFiltrado(getFiltroMaterial());
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this,
                "Error al cargar materiales: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizarComboFiltrado(String texto) {
        String filtro = texto != null ? texto.trim().toLowerCase() : "";
        comboModel.removeAllElements();
        comboModel.addElement("");
        for (String material : materiales) {
            if (filtro.isEmpty() || material.toLowerCase().contains(filtro)) {
                comboModel.addElement(material);
            }
        }
        if (comboMaterial.isShowing()) {
            comboMaterial.setPopupVisible(true);
        }
    }

    private String getFiltroMaterial() {
        Object editorValue = comboMaterial.getEditor().getItem();
        if (editorValue != null) {
            return editorValue.toString().trim();
        }
        Object selected = comboMaterial.getSelectedItem();
        return selected != null ? selected.toString().trim() : "";
    }
}

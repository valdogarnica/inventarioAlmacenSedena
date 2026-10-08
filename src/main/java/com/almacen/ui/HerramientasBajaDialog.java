package com.almacen.ui;

import com.almacen.database.DatabaseManager;
import com.almacen.model.Herramienta;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.List;

public class HerramientasBajaDialog extends JDialog {
    private JTable tabla;
    private DefaultTableModel modelo;
    private JButton btnAnterior;
    private JButton btnSiguiente;
    private JLabel lblPagina;
    private JComboBox<Integer> comboTamanoPagina;
    private JTextField txtFiltro;
    private JButton btnFiltrar;
    private JButton btnLimpiar;
    private int paginaActual = 1;
    private int totalRegistros = 0;

    public HerramientasBajaDialog(Window parent) {
        super(parent, "Herramientas dadas de baja", ModalityType.APPLICATION_MODAL);
        initComponents();
        cargarPagina(1);
    }

    private void initComponents() {
        setSize(1100, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JPanel panelFiltro = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelFiltro.setOpaque(false);
        JLabel lblFiltro = new JLabel("Filtro:");
        panelFiltro.add(lblFiltro);
        txtFiltro = new JTextField(25);
        txtFiltro.addActionListener(e -> cargarPagina(1));
        panelFiltro.add(txtFiltro);
        btnFiltrar = new JButton("Filtrar");
        btnFiltrar.addActionListener(e -> cargarPagina(1));
        UIStyles.styleSecondaryButton(btnFiltrar);
        panelFiltro.add(btnFiltrar);
        btnLimpiar = new JButton("Limpiar");
        btnLimpiar.addActionListener(e -> {
            txtFiltro.setText("");
            cargarPagina(1);
        });
        UIStyles.styleSecondaryButton(btnLimpiar);
        panelFiltro.add(btnLimpiar);
        add(panelFiltro, BorderLayout.NORTH);

        String[] columnas = {"ID", "Nombre", "Categoría", "Unidad", "Proveedor", "Stock", "Descripción", "Dar de alta"};
        modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 7;
            }
        };
        tabla = new JTable(modelo);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.setRowHeight(32);
        tabla.getColumnModel().getColumn(7).setCellRenderer(new AltaRenderer());
        tabla.getColumnModel().getColumn(7).setCellEditor(new AltaEditor());
        // Estilizar encabezado y centrar números
        UIStyles.styleTableHeader(tabla);
        aplicarCentradoNumeros();
        JScrollPane scroll = new JScrollPane(tabla);
        add(UIStyles.createCard("Herramientas en baja", scroll), BorderLayout.CENTER);

        JPanel panelControles = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelControles.setOpaque(false);
        JLabel lblTamano = new JLabel("Registros por página:");
        panelControles.add(lblTamano);
        comboTamanoPagina = new JComboBox<>(new Integer[]{10, 20, 50});
        comboTamanoPagina.setSelectedItem(10);
        comboTamanoPagina.addActionListener(e -> cargarPagina(1));
        panelControles.add(comboTamanoPagina);
        btnAnterior = new JButton("Anterior");
        btnAnterior.addActionListener(e -> cargarPagina(paginaActual - 1));
        UIStyles.styleSecondaryButton(btnAnterior);
        panelControles.add(btnAnterior);
        btnSiguiente = new JButton("Siguiente");
        btnSiguiente.addActionListener(e -> cargarPagina(paginaActual + 1));
        UIStyles.styleSecondaryButton(btnSiguiente);
        panelControles.add(btnSiguiente);
        lblPagina = new JLabel("Página 1 de 1");
        panelControles.add(lblPagina);

        add(panelControles, BorderLayout.SOUTH);
    }

    private void cargarPagina(int pagina) {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            if (!dbManager.isConnected()) {
                Notificaciones.showMessageDialog(this,
                    "No hay conexión a la base de datos",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int tamano = (Integer) comboTamanoPagina.getSelectedItem();
            String filtro = txtFiltro.getText();
            totalRegistros = dbManager.contarHerramientasFiltradas(filtro, 0);
            int totalPaginas = (int) Math.ceil(totalRegistros / (double) tamano);
            if (totalPaginas == 0) {
                totalPaginas = 1;
            }
            paginaActual = Math.max(1, Math.min(pagina, totalPaginas));
            int offset = (paginaActual - 1) * tamano;
            List<Herramienta> herramientas = dbManager.obtenerHerramientasPaginadasFiltradas(filtro, 0, offset, tamano);
            modelo.setRowCount(0);
            for (Herramienta h : herramientas) {
                modelo.addRow(new Object[]{
                    h.getId(),
                    h.getNombre(),
                    h.getCategoria(),
                    h.getUnidad(),
                    h.getProveedorNombre() != null ? h.getProveedorNombre() : "-",
                    h.getStock(),
                    h.getDescripcion(),
                    "Dar de alta"
                });
            }
            lblPagina.setText("Página " + paginaActual + " de " + totalPaginas + " (Total: " + totalRegistros + ")");
            btnAnterior.setEnabled(paginaActual > 1);
            btnSiguiente.setEnabled(paginaActual < totalPaginas);
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this,
                "Error al cargar herramientas en baja: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void aplicarCentradoNumeros() {
        DefaultTableCellRenderer center = UIStyles.createCenteredNumberRenderer();
        tabla.getColumnModel().getColumn(0).setCellRenderer(center); // ID
        tabla.getColumnModel().getColumn(5).setCellRenderer(center); // Stock
    }

    private class AltaRenderer extends JButton implements TableCellRenderer {
        public AltaRenderer() {
            setText("Dar de alta");
            UIStyles.styleSuccessButton(this);
            setOpaque(true);
            setContentAreaFilled(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText("Dar de alta");
            UIStyles.styleSuccessButton(this);
            return this;
        }
    }

    private class AltaEditor extends AbstractCellEditor implements TableCellEditor {
        private final JButton button = new JButton("Dar de alta");
        private int row;

        public AltaEditor() {
            UIStyles.styleSuccessButton(button);
            button.addActionListener(e -> {
                darDeAlta(row);
                stopCellEditing();
            });
        }

        @Override
        public Object getCellEditorValue() {
            return "Dar de alta";
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.row = row;
            return Tema.alDia(button);
        }
    }

    private void darDeAlta(int row) {
        int id = (Integer) modelo.getValueAt(row, 0);
        if (!Alerta.confirmar(this, "La herramienta volverá a aparecer en el inventario.",
                "¿Dar de alta esta herramienta?", Alerta.Tipo.PREGUNTA, "Dar de alta", "Cancelar")) {
            return;
        }
        new SwingWorker<Void, Void>() {
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    DatabaseManager dbManager = DatabaseManager.getInstance();
                    dbManager.cambiarEstadoHerramienta(id, 1);
                } catch (Exception e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                if (error != null) {
                    Notificaciones.showMessageDialog(HerramientasBajaDialog.this,
                        "Error al dar de alta: " + error.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                cargarPagina(paginaActual);
            }
        }.execute();
    }
}

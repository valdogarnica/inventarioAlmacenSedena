package com.almacen.ui;

import com.almacen.database.DatabaseManager;
import com.almacen.model.Herramienta;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.List;

public class InventarioDialog extends JDialog {
    private JTable tablaInventario;
    private DefaultTableModel modelo;
    private JButton btnAnterior;
    private JButton btnSiguiente;
    private JLabel lblPagina;
    private JComboBox<Integer> comboTamanoPagina;
    private JTextField txtFiltroNombre;
    private JButton btnFiltrar;
    private JButton btnLimpiar;
    private int paginaActual = 1;
    private int totalRegistros = 0;

    public InventarioDialog(JFrame parent) {
        super(parent, "Inventario Actual", true);
        initComponents();
        cargarPagina(1);
    }

    private void initComponents() {
        setSize(1150, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(UIStyles.BG);

        JPanel panelFiltro = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelFiltro.setOpaque(false);
        JLabel lblFiltro = new JLabel("Nombre herramienta:");
        lblFiltro.setForeground(UIStyles.TEXT);
        panelFiltro.add(lblFiltro);
        txtFiltroNombre = new JTextField(25);
        txtFiltroNombre.addActionListener(e -> cargarPagina(1));
        txtFiltroNombre.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                cargarPagina(1);
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                cargarPagina(1);
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                cargarPagina(1);
            }
        });
        panelFiltro.add(txtFiltroNombre);
        btnFiltrar = new JButton("Filtrar");
        btnFiltrar.addActionListener(e -> cargarPagina(1));
        UIStyles.styleSecondaryButton(btnFiltrar);
        panelFiltro.add(btnFiltrar);
        btnLimpiar = new JButton("Limpiar");
        btnLimpiar.addActionListener(e -> {
            txtFiltroNombre.setText("");
            cargarPagina(1);
        });
        UIStyles.styleSecondaryButton(btnLimpiar);
        panelFiltro.add(btnLimpiar);
        add(panelFiltro, BorderLayout.NORTH);

        String[] columnas = {"ID", "Nombre", "Categoría", "Stock", "Descripción", "Estado", "Editar", "Dar de baja"};
        modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6 || column == 7;
            }
        };
        tablaInventario = new JTable(modelo);
        tablaInventario.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaInventario.getTableHeader().setReorderingAllowed(false);
        tablaInventario.setRowHeight(28);
        tablaInventario.getColumnModel().getColumn(6).setCellRenderer(new EditarRenderer());
        tablaInventario.getColumnModel().getColumn(6).setCellEditor(new EditarEditor());
        tablaInventario.getColumnModel().getColumn(7).setCellRenderer(new EstadoRenderer());
        tablaInventario.getColumnModel().getColumn(7).setCellEditor(new EstadoEditor());
        // Estilizar encabezado y centrar números
        UIStyles.styleTableHeader(tablaInventario);
        aplicarCentradoNumeros();
        JScrollPane scroll = new JScrollPane(tablaInventario);
        add(UIStyles.createCard("Existencias", scroll), BorderLayout.CENTER);

        JPanel panelControles = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelControles.setOpaque(false);

        JLabel lblTamano = new JLabel("Registros por página:");
        lblTamano.setForeground(UIStyles.TEXT);
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

        JButton btnVerBajas = new JButton("Ver herramientas dadas de baja");
        btnVerBajas.addActionListener(e -> verHerramientasBaja());
        UIStyles.styleSecondaryButton(btnVerBajas);
        panelControles.add(btnVerBajas);

        lblPagina = new JLabel("Página 1 de 1");
        lblPagina.setForeground(UIStyles.TEXT);
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
            String filtro = txtFiltroNombre.getText();
            totalRegistros = dbManager.contarHerramientasPorNombre(filtro, 1);
            int totalPaginas = (int) Math.ceil(totalRegistros / (double) tamano);
            if (totalPaginas == 0) {
                totalPaginas = 1;
            }

            paginaActual = Math.max(1, Math.min(pagina, totalPaginas));
            int offset = (paginaActual - 1) * tamano;

            List<Herramienta> herramientas = dbManager.obtenerHerramientasPaginadasPorNombre(filtro, 1, offset, tamano);
            modelo.setRowCount(0);
            for (Herramienta h : herramientas) {
                modelo.addRow(new Object[]{
                    h.getId(),
                    h.getNombre(),
                    h.getCategoria(),
                    h.getStock(),
                    h.getDescripcion(),
                    h.getEstado() == 1 ? "Alta" : "Baja",
                    "Editar",
                    h.getEstado() == 1 ? "Dar de baja" : ""
                });
            }

            lblPagina.setText("Página " + paginaActual + " de " + totalPaginas + " (Total: " + totalRegistros + ")");
            btnAnterior.setEnabled(paginaActual > 1);
            btnSiguiente.setEnabled(paginaActual < totalPaginas);
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this,
                "Error al cargar inventario: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void aplicarCentradoNumeros() {
        DefaultTableCellRenderer center = UIStyles.createCenteredNumberRenderer();
        tablaInventario.getColumnModel().getColumn(0).setCellRenderer(center); // ID
        tablaInventario.getColumnModel().getColumn(3).setCellRenderer(center); // Stock
    }

    private class EditarRenderer extends JButton implements TableCellRenderer {
        public EditarRenderer() {
            setText("Editar");
            UIStyles.styleSecondaryButton(this);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return this;
        }
    }

    private class EditarEditor extends AbstractCellEditor implements TableCellEditor {
        private final JButton button = new JButton("Editar");
        private int row;

        public EditarEditor() {
            UIStyles.styleSecondaryButton(button);
            button.addActionListener(e -> {
                editarHerramienta(row);
                stopCellEditing();
            });
        }

        @Override
        public Object getCellEditorValue() {
            return "Editar";
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.row = row;
            return button;
        }
    }

    private class EstadoRenderer extends JButton implements TableCellRenderer {
        public EstadoRenderer() {
            setText("Baja");
            UIStyles.styleDangerButton(this);
            setOpaque(true);
            setContentAreaFilled(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String texto = value != null ? value.toString() : "";
            setText(texto);
            if ("Dar de baja".equalsIgnoreCase(texto)) {
                UIStyles.styleDangerButton(this);
                setEnabled(true);
            } else {
                setText("-");
                setEnabled(false);
                UIStyles.styleSecondaryButton(this);
            }
            setOpaque(true);
            setContentAreaFilled(true);
            return this;
        }
    }

    private class EstadoEditor extends AbstractCellEditor implements TableCellEditor {
        private final JButton button = new JButton();
        private int row;

        public EstadoEditor() {
            button.addActionListener(e -> {
                confirmarCambioEstado(row);
                stopCellEditing();
            });
        }

        @Override
        public Object getCellEditorValue() {
            return button.getText();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.row = row;
            String texto = value != null ? value.toString() : "";
            button.setText(texto);
            if ("Dar de baja".equalsIgnoreCase(texto)) {
                UIStyles.styleDangerButton(button);
                button.setEnabled(true);
            } else {
                button.setText("-");
                button.setEnabled(false);
                UIStyles.styleSecondaryButton(button);
            }
            button.setOpaque(true);
            button.setContentAreaFilled(true);
            return button;
        }
    }

    private void confirmarCambioEstado(int row) {
        String accion = (String) modelo.getValueAt(row, 7);
        if (!"Dar de baja".equalsIgnoreCase(accion)) {
            return;
        }
        int respuesta = JOptionPane.showConfirmDialog(this,
            "¿Desea dar de baja esta herramienta?",
            "Confirmar",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }
        cambiarEstado(row, 0);
    }

    private void editarHerramienta(int row) {
        int id = (Integer) modelo.getValueAt(row, 0);
        EditarHerramientaDialog dialog = new EditarHerramientaDialog(this, id);
        dialog.setVisible(true);
        cargarPagina(paginaActual);
    }

    private void cambiarEstado(int row, int nuevoEstado) {
        int id = (Integer) modelo.getValueAt(row, 0);
        new SwingWorker<Void, Void>() {
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    DatabaseManager dbManager = DatabaseManager.getInstance();
                    dbManager.cambiarEstadoHerramienta(id, nuevoEstado);
                } catch (Exception e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void done() {
                if (error != null) {
                    Notificaciones.showMessageDialog(InventarioDialog.this,
                        "Error al cambiar estado: " + error.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (nuevoEstado == 0) {
                    cargarPagina(paginaActual);
                } else {
                    String nuevoTexto = "Dar de baja";
                    modelo.setValueAt(nuevoTexto, row, 7);
                    modelo.setValueAt("Alta", row, 5);
                    modelo.fireTableCellUpdated(row, 7);
                    modelo.fireTableCellUpdated(row, 5);
                    modelo.fireTableDataChanged();
                    tablaInventario.revalidate();
                    tablaInventario.repaint();
                    SwingUtilities.invokeLater(() -> {
                        tablaInventario.revalidate();
                        tablaInventario.repaint();
                    });
                }
            }
        }.execute();
    }

    private void verHerramientasBaja() {
        HerramientasBajaDialog dialog = new HerramientasBajaDialog(this);
        dialog.setVisible(true);
        cargarPagina(paginaActual);
    }
}

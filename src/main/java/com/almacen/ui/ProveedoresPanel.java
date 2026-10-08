package com.almacen.ui;

import com.almacen.database.DatabaseManager;
import com.almacen.model.Herramienta;
import com.almacen.model.Proveedor;
import com.almacen.model.Remision;
import com.almacen.report.ReportesPdf;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Página de proveedores: lista de proveedores y, del seleccionado, sus materiales
 * (con stock propio) y sus remisiones.
 */
public class ProveedoresPanel extends JPanel implements Pagina {
    private DefaultTableModel modeloProveedores;
    private JTable tablaProveedores;
    private DefaultTableModel modeloMateriales;
    private DefaultTableModel modeloRemisiones;
    private JLabel lblSeleccion;
    private List<Proveedor> proveedores = new ArrayList<>();

    public ProveedoresPanel() {
        super(new BorderLayout(10, 10));
        setOpaque(false);
        initComponents();
    }

    private void initComponents() {
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        acciones.setOpaque(false);
        JButton btnNuevo = new JButton("Nuevo proveedor");
        btnNuevo.addActionListener(e -> nuevo());
        UIStyles.stylePrimaryButton(btnNuevo);
        UIStyles.applySvgIcon(btnNuevo, "/icons/add.svg", 16);
        JButton btnEditar = new JButton("Editar");
        btnEditar.addActionListener(e -> editar());
        UIStyles.styleSecondaryButton(btnEditar);
        UIStyles.applySvgIcon(btnEditar, "/icons/edit.svg", 16);
        JButton btnPdf = new JButton("Inventario del proveedor (PDF)");
        btnPdf.addActionListener(e -> pdfInventario());
        UIStyles.styleSecondaryButton(btnPdf);
        UIStyles.applySvgIcon(btnPdf, "/icons/report.svg", 16);
        acciones.add(btnNuevo);
        acciones.add(btnEditar);
        acciones.add(btnPdf);
        add(acciones, BorderLayout.NORTH);

        modeloProveedores = new DefaultTableModel(new String[]{"ID", "Proveedor", "Contacto", "Teléfono", "Materiales", "Remisiones"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaProveedores = new JTable(modeloProveedores);
        tablaProveedores.setRowHeight(28);
        tablaProveedores.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaProveedores.getTableHeader().setReorderingAllowed(false);
        UIStyles.styleTableHeader(tablaProveedores);
        tablaProveedores.getColumnModel().getColumn(0).setCellRenderer(UIStyles.createCenteredNumberRenderer());
        tablaProveedores.getColumnModel().getColumn(4).setCellRenderer(UIStyles.createCenteredNumberRenderer());
        tablaProveedores.getColumnModel().getColumn(5).setCellRenderer(UIStyles.createCenteredNumberRenderer());
        tablaProveedores.getColumnModel().getColumn(0).setPreferredWidth(40);
        tablaProveedores.getColumnModel().getColumn(1).setPreferredWidth(200);
        tablaProveedores.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarDetalleSeleccion();
            }
        });
        tablaProveedores.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editar();
                }
            }
        });

        modeloMateriales = new DefaultTableModel(new String[]{"ID", "Material", "Categoría", "Tipo", "Unidad", "Stock", "Últ. remisión"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable tablaMateriales = new JTable(modeloMateriales);
        tablaMateriales.setRowHeight(26);
        tablaMateriales.getTableHeader().setReorderingAllowed(false);
        UIStyles.styleTableHeader(tablaMateriales);
        tablaMateriales.getColumnModel().getColumn(0).setCellRenderer(UIStyles.createCenteredNumberRenderer());
        tablaMateriales.getColumnModel().getColumn(5).setCellRenderer(UIStyles.createCenteredNumberRenderer());
        tablaMateriales.getColumnModel().getColumn(1).setPreferredWidth(220);

        modeloRemisiones = new DefaultTableModel(new String[]{"Folio", "No. remisión", "Fecha", "Obra", "Partidas", "Cantidad"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable tablaRemisiones = new JTable(modeloRemisiones);
        tablaRemisiones.setRowHeight(26);
        tablaRemisiones.getTableHeader().setReorderingAllowed(false);
        UIStyles.styleTableHeader(tablaRemisiones);
        tablaRemisiones.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int fila = tablaRemisiones.rowAtPoint(e.getPoint());
                if (e.getClickCount() == 2 && fila >= 0) {
                    int id = (Integer) modeloRemisiones.getValueAt(tablaRemisiones.convertRowIndexToModel(fila), 0);
                    new RegistrarRemisionDialog(SwingUtilities.getWindowAncestor(ProveedoresPanel.this), id).setVisible(true);
                    alMostrar();
                }
            }
        });

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Materiales del proveedor", new JScrollPane(tablaMateriales));
        pestanas.addTab("Remisiones del proveedor", new JScrollPane(tablaRemisiones));
        JPanel panelDetalle = new JPanel(new BorderLayout(6, 6));
        panelDetalle.setOpaque(false);
        lblSeleccion = new JLabel("Seleccione un proveedor");
        lblSeleccion.setForeground(UIStyles.TEXT);
        lblSeleccion.setFont(lblSeleccion.getFont().deriveFont(Font.BOLD, 14f));
        panelDetalle.add(lblSeleccion, BorderLayout.NORTH);
        panelDetalle.add(pestanas, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            UIStyles.createCard("Proveedores", new JScrollPane(tablaProveedores)),
            UIStyles.createCard("Detalle", panelDetalle));
        split.setResizeWeight(0.45);
        split.setBorder(null);
        split.setOpaque(false);
        add(split, BorderLayout.CENTER);
    }

    @Override
    public void alMostrar() {
        Proveedor actual = seleccionado();
        try {
            proveedores = DatabaseManager.getInstance().obtenerProveedores();
            modeloProveedores.setRowCount(0);
            for (Proveedor p : proveedores) {
                modeloProveedores.addRow(new Object[]{p.getId(), p.getNombre(), p.getContacto(), p.getTelefono(),
                    p.getTotalMateriales(), p.getTotalRemisiones()});
            }
            int seleccion = proveedores.isEmpty() ? -1 : 0;
            if (actual != null) {
                for (int i = 0; i < proveedores.size(); i++) {
                    if (proveedores.get(i).getId() == actual.getId()) {
                        seleccion = i;
                        break;
                    }
                }
            }
            if (seleccion >= 0) {
                tablaProveedores.setRowSelectionInterval(seleccion, seleccion);
            }
            cargarDetalleSeleccion();
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this, "Error al cargar proveedores: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Proveedor seleccionado() {
        int fila = tablaProveedores.getSelectedRow();
        if (fila < 0 || fila >= proveedores.size()) {
            return null;
        }
        return proveedores.get(tablaProveedores.convertRowIndexToModel(fila));
    }

    private void cargarDetalleSeleccion() {
        modeloMateriales.setRowCount(0);
        modeloRemisiones.setRowCount(0);
        Proveedor p = seleccionado();
        if (p == null) {
            lblSeleccion.setText("Seleccione un proveedor");
            return;
        }
        lblSeleccion.setText(p.getNombre());
        try {
            DatabaseManager db = DatabaseManager.getInstance();
            for (Herramienta h : db.obtenerMaterialesPorProveedor(p.getId())) {
                modeloMateriales.addRow(new Object[]{h.getId(), h.getNombre(), h.getCategoria(), h.getTipo(),
                    h.getUnidad(), h.getStock(), h.getRemision()});
            }
            for (Remision r : db.obtenerRemisionesPaginadas(null, p.getId(), 0, 500)) {
                modeloRemisiones.addRow(new Object[]{r.getId(),
                    r.getNumeroRemision() != null ? r.getNumeroRemision() : "-",
                    r.getFecha() != null ? r.getFecha().format(ReportesPdf.FECHA) : "",
                    r.getObra(), r.getTotalPartidas(), r.getTotalCantidad()});
            }
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this, "Error al cargar el detalle: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void nuevo() {
        ProveedorFormDialog dialog = new ProveedorFormDialog(SwingUtilities.getWindowAncestor(this), null);
        dialog.setVisible(true);
        if (dialog.getGuardado() != null) {
            alMostrar();
        }
    }

    private void editar() {
        Proveedor p = seleccionado();
        if (p == null) {
            Notificaciones.showMessageDialog(this, "Seleccione un proveedor", "Información", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        ProveedorFormDialog dialog = new ProveedorFormDialog(SwingUtilities.getWindowAncestor(this), p);
        dialog.setVisible(true);
        if (dialog.getGuardado() != null) {
            alMostrar();
        }
    }

    private void pdfInventario() {
        Proveedor p = seleccionado();
        if (p == null) {
            Notificaciones.showMessageDialog(this, "Seleccione un proveedor", "Información", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        PdfViewer.generarYAbrir(this, () -> ReportesPdf.inventarioGeneral(p.getId()),
            "Este proveedor no tiene materiales activos");
    }
}

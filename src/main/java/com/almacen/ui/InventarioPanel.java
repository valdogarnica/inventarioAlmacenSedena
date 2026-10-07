package com.almacen.ui;

import com.almacen.database.DatabaseManager;
import com.almacen.model.Herramienta;
import com.almacen.model.Proveedor;
import com.almacen.report.ReportesPdf;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Página de inventario: existencias por material y proveedor, con alta, edición,
 * baja, ingreso de stock y reporte general.
 */
public class InventarioPanel extends JPanel implements Pagina {
    private static final Proveedor TODOS = new Proveedor(0, "(Todos los proveedores)");
    private static final int COL_ID = 0;
    private static final int COL_EDITAR = 10;
    private static final int COL_BAJA = 11;

    private final JTextField txtFiltro = new JTextField(24);
    private final JComboBox<Proveedor> comboProveedor = new JComboBox<>();
    private DefaultTableModel modelo;
    private JTable tabla;
    private Paginador paginador;
    private boolean cargandoProveedores;

    public InventarioPanel() {
        super(new BorderLayout(10, 10));
        setOpaque(false);
        initComponents();
    }

    private void initComponents() {
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filtros.setOpaque(false);
        JLabel lblFiltro = new JLabel("Buscar:");
        lblFiltro.setForeground(UIStyles.TEXT);
        filtros.add(lblFiltro);
        txtFiltro.setToolTipText("Nombre, categoría, tipo, unidad, proveedor, descripción o ID");
        txtFiltro.getDocument().addDocumentListener(new SimpleDocumentListener(() -> cargarPagina(1)));
        filtros.add(txtFiltro);
        JLabel lblProveedor = new JLabel("Proveedor:");
        lblProveedor.setForeground(UIStyles.TEXT);
        filtros.add(lblProveedor);
        comboProveedor.setPreferredSize(new Dimension(230, 30));
        comboProveedor.addActionListener(e -> {
            if (!cargandoProveedores) {
                cargarPagina(1);
            }
        });
        filtros.add(comboProveedor);
        JButton btnLimpiar = new JButton("Limpiar");
        btnLimpiar.addActionListener(e -> {
            txtFiltro.setText("");
            comboProveedor.setSelectedIndex(0);
        });
        UIStyles.styleSecondaryButton(btnLimpiar);
        filtros.add(btnLimpiar);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        acciones.setOpaque(false);
        JButton btnAgregar = new JButton("Agregar material");
        btnAgregar.addActionListener(e -> agregar());
        UIStyles.stylePrimaryButton(btnAgregar);
        UIStyles.applySvgIcon(btnAgregar, "/icons/add.svg", 16);
        JButton btnIngreso = new JButton("Registrar ingreso");
        btnIngreso.addActionListener(e -> registrarIngreso());
        UIStyles.styleSuccessButton(btnIngreso);
        UIStyles.applySvgIcon(btnIngreso, "/icons/save.svg", 16);
        JButton btnBajas = new JButton("Dados de baja");
        btnBajas.addActionListener(e -> verBajas());
        UIStyles.styleSecondaryButton(btnBajas);
        UIStyles.applySvgIcon(btnBajas, "/icons/search.svg", 16);
        JButton btnReporte = new JButton("Reporte PDF");
        btnReporte.addActionListener(e -> reporte());
        UIStyles.styleSecondaryButton(btnReporte);
        UIStyles.applySvgIcon(btnReporte, "/icons/report.svg", 16);
        acciones.add(btnAgregar);
        acciones.add(btnIngreso);
        acciones.add(btnBajas);
        acciones.add(btnReporte);

        JPanel norte = new JPanel(new GridLayout(2, 1, 0, 2));
        norte.setOpaque(false);
        norte.add(acciones);
        norte.add(filtros);
        add(norte, BorderLayout.NORTH);

        String[] columnas = {"ID", "Material", "Categoría", "Tipo", "Unidad", "Proveedor", "Stock",
            "Últ. remisión", "Fecha alta", "Descripción", "Editar", "Baja"};
        modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == COL_EDITAR || column == COL_BAJA;
            }
        };
        tabla = new JTable(modelo);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.setRowHeight(28);
        UIStyles.styleTableHeader(tabla);
        tabla.getColumnModel().getColumn(COL_ID).setCellRenderer(UIStyles.createCenteredNumberRenderer());
        tabla.getColumnModel().getColumn(6).setCellRenderer(UIStyles.createCenteredNumberRenderer());
        tabla.getColumnModel().getColumn(COL_ID).setPreferredWidth(45);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(240);
        TablaBotones.instalar(tabla, COL_EDITAR, "Editar", TablaBotones.Estilo.SECUNDARIO, this::editar);
        TablaBotones.instalar(tabla, COL_BAJA, "Baja", TablaBotones.Estilo.PELIGRO, this::darDeBaja);
        add(UIStyles.createCard("Existencias por material y proveedor", new JScrollPane(tabla)), BorderLayout.CENTER);

        paginador = new Paginador(new Integer[]{20, 50, 100}, 20, this::cargarPagina);
        add(paginador, BorderLayout.SOUTH);
    }

    @Override
    public void alMostrar() {
        cargarProveedores();
        cargarPagina(paginador.getPaginaActual());
    }

    private void cargarProveedores() {
        Proveedor actual = (Proveedor) comboProveedor.getSelectedItem();
        cargandoProveedores = true;
        try {
            comboProveedor.removeAllItems();
            comboProveedor.addItem(TODOS);
            for (Proveedor p : DatabaseManager.getInstance().obtenerProveedores()) {
                comboProveedor.addItem(p);
            }
            if (actual != null) {
                comboProveedor.setSelectedItem(actual);
            }
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this, "Error al cargar proveedores: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            cargandoProveedores = false;
        }
    }

    private Integer proveedorFiltro() {
        Proveedor p = (Proveedor) comboProveedor.getSelectedItem();
        return p == null || p.getId() == 0 ? null : p.getId();
    }

    private void cargarPagina(int pagina) {
        try {
            DatabaseManager db = DatabaseManager.getInstance();
            if (!db.isConnected()) {
                return;
            }
            String filtro = txtFiltro.getText();
            Integer proveedorId = proveedorFiltro();
            int total = db.contarInventario(filtro, proveedorId, 1);
            int offset = paginador.calcularOffset(pagina, total);
            List<Herramienta> lista = db.obtenerInventarioPaginado(filtro, proveedorId, 1, offset, paginador.getTamano());
            modelo.setRowCount(0);
            for (Herramienta h : lista) {
                modelo.addRow(new Object[]{
                    h.getId(),
                    h.getNombre(),
                    h.getCategoria(),
                    h.getTipo(),
                    h.getUnidad(),
                    h.getProveedorNombre() != null ? h.getProveedorNombre() : "-",
                    h.getStock(),
                    h.getRemision() != null ? h.getRemision() : "",
                    ReportesPdf.fecha(h.getFechaRegistro()),
                    h.getDescripcion(),
                    "Editar",
                    "Baja"
                });
            }
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this, "Error al cargar inventario: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Window ventana() {
        return SwingUtilities.getWindowAncestor(this);
    }

    private void agregar() {
        MaterialFormDialog dialog = new MaterialFormDialog(ventana(), null);
        dialog.setVisible(true);
        alMostrar();
    }

    private void editar(int fila) {
        int id = (Integer) modelo.getValueAt(fila, COL_ID);
        MaterialFormDialog dialog = new MaterialFormDialog(ventana(), id);
        dialog.setVisible(true);
        cargarPagina(paginador.getPaginaActual());
    }

    private void darDeBaja(int fila) {
        int id = (Integer) modelo.getValueAt(fila, COL_ID);
        String nombre = String.valueOf(modelo.getValueAt(fila, 1));
        int respuesta = JOptionPane.showConfirmDialog(this,
            "¿Desea dar de baja \"" + nombre + "\"?", "Confirmar",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            DatabaseManager.getInstance().cambiarEstadoHerramienta(id, 0);
            cargarPagina(paginador.getPaginaActual());
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this, "Error al cambiar estado: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void registrarIngreso() {
        RegistrarIngresoDialog dialog = new RegistrarIngresoDialog(ventana());
        dialog.setVisible(true);
        cargarPagina(paginador.getPaginaActual());
    }

    private void verBajas() {
        HerramientasBajaDialog dialog = new HerramientasBajaDialog(ventana());
        dialog.setVisible(true);
        cargarPagina(paginador.getPaginaActual());
    }

    private void reporte() {
        Integer proveedorId = proveedorFiltro();
        PdfViewer.generarYAbrir(this, () -> ReportesPdf.inventarioGeneral(proveedorId),
            "No hay materiales para reportar");
    }
}

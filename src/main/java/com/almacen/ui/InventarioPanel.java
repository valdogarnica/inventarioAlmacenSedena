package com.almacen.ui;

import com.almacen.database.DatabaseManager;
import com.almacen.model.ExistenciaMaterial;
import com.almacen.model.Herramienta;
import com.almacen.model.Proveedor;
import com.almacen.report.ReportesPdf;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Página de inventario. Tiene dos vistas:
 * <ul>
 *   <li>Total por material: el mismo material de varios proveedores se acumula en una fila,
 *       con el desglose de cuánto tiene cada proveedor.</li>
 *   <li>Detalle por proveedor: un registro por material y proveedor (con alta, edición y baja).</li>
 * </ul>
 */
public class InventarioPanel extends JPanel implements Pagina {
    private static final Proveedor TODOS = new Proveedor(0, "(Todos los proveedores)");
    private static final int COL_ID = 0;
    private static final int COL_EDITAR = 10;
    private static final int COL_BAJA = 11;
    private static final int COL_AG_DETALLE = 8;
    private static final String VISTA_TOTAL = "total";
    private static final String VISTA_DETALLE = "detalle";

    private final JTextField txtFiltro = new JTextField(24);
    private final JComboBox<Proveedor> comboProveedor = new JComboBox<>();
    private DefaultTableModel modelo;
    private DefaultTableModel modeloAgrupado;
    private JTable tabla;
    private final CardLayout tarjetas = new CardLayout();
    private final JPanel panelVistas = new JPanel(tarjetas);
    private final JToggleButton btnVistaTotal = new JToggleButton("Total por material");
    private final JToggleButton btnVistaDetalle = new JToggleButton("Detalle por proveedor");
    private String vista = VISTA_TOTAL;
    private Paginador paginador;
    private boolean cargandoProveedores;

    public InventarioPanel() {
        super(new BorderLayout(10, 10));
        setOpaque(false);
        initComponents();
    }

    private void initComponents() {
        JPanel filtros = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 4));
        filtros.setOpaque(false);
        JLabel lblFiltro = new JLabel("Buscar:");
        filtros.add(lblFiltro);
        txtFiltro.setToolTipText("Nombre, categoría, tipo, unidad, proveedor, descripción o ID");
        txtFiltro.getDocument().addDocumentListener(new SimpleDocumentListener(() -> cargarPagina(1)));
        filtros.add(txtFiltro);
        JLabel lblProveedor = new JLabel("Proveedor:");
        filtros.add(lblProveedor);
        comboProveedor.setPreferredSize(new Dimension(230, 30));
        ComboBuscable.instalar(comboProveedor);
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

        JPanel acciones = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 4));
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

        JLabel lblVista = new JLabel("   Vista:");
        ButtonGroup grupoVista = new ButtonGroup();
        grupoVista.add(btnVistaTotal);
        grupoVista.add(btnVistaDetalle);
        btnVistaTotal.setSelected(true);
        btnVistaTotal.setToolTipText("Suma el stock del mismo material de todos los proveedores");
        btnVistaDetalle.setToolTipText("Un renglón por material y proveedor");
        UIStyles.styleToggleButton(btnVistaTotal);
        UIStyles.styleToggleButton(btnVistaDetalle);
        btnVistaTotal.addActionListener(e -> cambiarVista(VISTA_TOTAL));
        btnVistaDetalle.addActionListener(e -> cambiarVista(VISTA_DETALLE));
        acciones.add(lblVista);
        acciones.add(btnVistaTotal);
        acciones.add(btnVistaDetalle);

        add(UIStyles.createBarra(acciones, filtros), BorderLayout.NORTH);

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
        tabla.setRowHeight(32);
        UIStyles.styleTableHeader(tabla);
        tabla.getColumnModel().getColumn(COL_ID).setCellRenderer(UIStyles.createCenteredNumberRenderer());
        tabla.getColumnModel().getColumn(6).setCellRenderer(UIStyles.createCenteredNumberRenderer());
        tabla.getColumnModel().getColumn(COL_ID).setPreferredWidth(45);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(240);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(150);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(100);
        tabla.getColumnModel().getColumn(5).setPreferredWidth(150);
        TablaBotones.instalar(tabla, COL_EDITAR, "Editar", TablaBotones.Estilo.SECUNDARIO, this::editar);
        TablaBotones.instalar(tabla, COL_BAJA, "Baja", TablaBotones.Estilo.PELIGRO, this::darDeBaja);
        tabla.getColumnModel().getColumn(COL_EDITAR).setMinWidth(78);
        tabla.getColumnModel().getColumn(COL_BAJA).setMinWidth(70);

        String[] columnasAgrupado = {"Material", "Unidad", "Categoría", "Tipo", "Proveedores", "Disponible",
            "Prestado", "Total", "Detalle"};
        modeloAgrupado = new DefaultTableModel(columnasAgrupado, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == COL_AG_DETALLE;
            }
        };
        JTable tablaAgrupada = new JTable(modeloAgrupado);
        tablaAgrupada.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaAgrupada.setRowHeight(32);
        UIStyles.styleTableHeader(tablaAgrupada);
        int[] anchosAgrupado = {220, 70, 160, 110, 340, 80, 80, 70, 70};
        for (int i = 0; i < anchosAgrupado.length; i++) {
            tablaAgrupada.getColumnModel().getColumn(i).setPreferredWidth(anchosAgrupado[i]);
        }
        tablaAgrupada.getColumnModel().getColumn(4).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            {
                setHorizontalAlignment(SwingConstants.CENTER);
            }

            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean foco, int r, int c) {
                super.getTableCellRendererComponent(t, value, sel, foco, r, c);
                setToolTipText(value != null ? "<html>" + value.toString().replace(" | ", "<br>") + "</html>" : null);
                return this;
            }
        });
        TablaBotones.instalar(tablaAgrupada, COL_AG_DETALLE, "Ver", TablaBotones.Estilo.SECUNDARIO, this::verDetalleMaterial);

        panelVistas.setOpaque(false);
        panelVistas.add(UIStyles.createCard("Existencias por material (suma de todos los proveedores)",
            new JScrollPane(tablaAgrupada)), VISTA_TOTAL);
        panelVistas.add(UIStyles.createCard("Existencias por material y proveedor", new JScrollPane(tabla)), VISTA_DETALLE);
        add(panelVistas, BorderLayout.CENTER);

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

    private void cambiarVista(String nueva) {
        vista = nueva;
        btnVistaTotal.setSelected(VISTA_TOTAL.equals(nueva));
        btnVistaDetalle.setSelected(VISTA_DETALLE.equals(nueva));
        tarjetas.show(panelVistas, nueva);
        cargarPagina(1);
    }

    /** Desde la vista total: muestra los registros de ese material con cada proveedor. */
    private void verDetalleMaterial(int fila) {
        String material = String.valueOf(modeloAgrupado.getValueAt(fila, 0));
        txtFiltro.setText(material);
        cambiarVista(VISTA_DETALLE);
    }

    private void cargarPagina(int pagina) {
        try {
            DatabaseManager db = DatabaseManager.getInstance();
            if (!db.isConnected()) {
                return;
            }
            String filtro = txtFiltro.getText();
            Integer proveedorId = proveedorFiltro();
            if (VISTA_TOTAL.equals(vista)) {
                cargarPaginaAgrupada(db, pagina, filtro, proveedorId);
                return;
            }
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

    private void cargarPaginaAgrupada(DatabaseManager db, int pagina, String filtro, Integer proveedorId) throws Exception {
        int total = db.contarInventarioAgrupado(filtro, proveedorId);
        int offset = paginador.calcularOffset(pagina, total);
        List<ExistenciaMaterial> lista = db.obtenerInventarioAgrupado(filtro, proveedorId, offset, paginador.getTamano());
        modeloAgrupado.setRowCount(0);
        for (ExistenciaMaterial e : lista) {
            modeloAgrupado.addRow(new Object[]{
                e.getNombre(),
                e.getUnidad(),
                e.getCategoria(),
                e.getTipo(),
                e.getDesglose(),
                e.getDisponible(),
                e.getPrestado(),
                e.getTotal(),
                "Ver"
            });
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
        if (!Alerta.confirmar(this, "\"" + nombre + "\" dejará de aparecer en el inventario. "
                + "Podrá darlo de alta otra vez desde \"Dados de baja\".",
                "¿Dar de baja este material?", Alerta.Tipo.AVISO, "Dar de baja", "Cancelar")) {
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

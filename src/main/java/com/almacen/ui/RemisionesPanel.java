package com.almacen.ui;

import com.almacen.database.DatabaseManager;
import com.almacen.model.Remision;
import com.almacen.report.ReportesPdf;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.function.Supplier;

/**
 * Página de remisiones: alta de remisiones de proveedor y consulta de las registradas.
 */
public class RemisionesPanel extends JPanel implements Pagina {
    private static final int COL_ID = 0;
    private static final int COL_VER = 9;
    private static final int COL_PDF = 10;

    private final Supplier<String> empleado;
    private final JTextField txtFiltro = new JTextField(26);
    private DefaultTableModel modelo;
    private Paginador paginador;

    public RemisionesPanel(Supplier<String> empleado) {
        super(new BorderLayout(10, 10));
        this.empleado = empleado;
        setOpaque(false);
        initComponents();
    }

    private void initComponents() {
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filtros.setOpaque(false);
        JLabel lbl = new JLabel("Buscar:");
        lbl.setForeground(UIStyles.TEXT);
        filtros.add(lbl);
        txtFiltro.setToolTipText("No. remisión, proveedor, obra, quién envía/recibe, fecha (aaaa-mm-dd) o material");
        txtFiltro.getDocument().addDocumentListener(new SimpleDocumentListener(() -> cargarPagina(1)));
        filtros.add(txtFiltro);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        acciones.setOpaque(false);
        JButton btnNueva = new JButton("Nueva remisión");
        btnNueva.setFont(btnNueva.getFont().deriveFont(Font.BOLD));
        btnNueva.addActionListener(e -> nuevaRemision());
        UIStyles.stylePrimaryButton(btnNueva);
        UIStyles.applySvgIcon(btnNueva, "/icons/add.svg", 16);
        acciones.add(btnNueva);

        JPanel norte = new JPanel(new GridLayout(2, 1, 0, 2));
        norte.setOpaque(false);
        norte.add(acciones);
        norte.add(filtros);
        add(norte, BorderLayout.NORTH);

        modelo = new DefaultTableModel(new String[]{"Folio", "No. remisión", "Fecha", "Proveedor", "Obra", "Envía",
            "Recibió", "Partidas", "Cantidad total", "Detalle", "PDF"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == COL_VER || column == COL_PDF;
            }
        };
        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(28);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getTableHeader().setReorderingAllowed(false);
        UIStyles.styleTableHeader(tabla);
        tabla.getColumnModel().getColumn(COL_ID).setCellRenderer(UIStyles.createCenteredNumberRenderer());
        tabla.getColumnModel().getColumn(7).setCellRenderer(UIStyles.createCenteredNumberRenderer());
        tabla.getColumnModel().getColumn(8).setCellRenderer(UIStyles.createCenteredNumberRenderer());
        TablaBotones.instalar(tabla, COL_VER, "Ver", TablaBotones.Estilo.SECUNDARIO, this::verDetalle);
        TablaBotones.instalar(tabla, COL_PDF, "PDF", TablaBotones.Estilo.SECUNDARIO, this::pdf);
        tabla.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int fila = tabla.rowAtPoint(e.getPoint());
                if (e.getClickCount() == 2 && fila >= 0) {
                    verDetalle(tabla.convertRowIndexToModel(fila));
                }
            }
        });
        add(UIStyles.createCard("Remisiones registradas", new JScrollPane(tabla)), BorderLayout.CENTER);

        paginador = new Paginador(new Integer[]{20, 50, 100}, 20, this::cargarPagina);
        add(paginador, BorderLayout.SOUTH);
    }

    @Override
    public void alMostrar() {
        cargarPagina(paginador.getPaginaActual());
    }

    private void cargarPagina(int pagina) {
        try {
            DatabaseManager db = DatabaseManager.getInstance();
            if (!db.isConnected()) {
                return;
            }
            String filtro = txtFiltro.getText();
            int total = db.contarRemisiones(filtro, null);
            int offset = paginador.calcularOffset(pagina, total);
            List<Remision> lista = db.obtenerRemisionesPaginadas(filtro, null, offset, paginador.getTamano());
            modelo.setRowCount(0);
            for (Remision r : lista) {
                modelo.addRow(new Object[]{
                    r.getId(),
                    r.getNumeroRemision() != null ? r.getNumeroRemision() : "-",
                    r.getFecha() != null ? r.getFecha().format(ReportesPdf.FECHA) : "",
                    r.getProveedorNombre(),
                    r.getObra(),
                    r.getEnvia(),
                    r.getRecibe(),
                    r.getTotalPartidas(),
                    r.getTotalCantidad(),
                    "Ver",
                    "PDF"
                });
            }
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this, "Error al cargar remisiones: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void nuevaRemision() {
        RegistrarRemisionDialog dialog = new RegistrarRemisionDialog(SwingUtilities.getWindowAncestor(this), empleado.get());
        dialog.setVisible(true);
        if (dialog.isGuardado()) {
            cargarPagina(1);
        }
    }

    private void verDetalle(int fila) {
        int id = (Integer) modelo.getValueAt(fila, COL_ID);
        new RemisionDetalleDialog(SwingUtilities.getWindowAncestor(this), id).setVisible(true);
    }

    private void pdf(int fila) {
        int id = (Integer) modelo.getValueAt(fila, COL_ID);
        PdfViewer.generarYAbrir(this, () -> ReportesPdf.remision(id), "No se encontró la remisión");
    }
}

package com.almacen.ui;

import com.almacen.database.DatabaseManager;
import com.almacen.model.DetalleRemision;
import com.almacen.model.Remision;
import com.almacen.report.ReportesPdf;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Consulta de una remisión registrada con todos sus materiales.
 */
public class RemisionDetalleDialog extends JDialog {
    private final int remisionId;

    public RemisionDetalleDialog(Window parent, int remisionId) {
        super(parent, "Detalle de remisión", ModalityType.APPLICATION_MODAL);
        this.remisionId = remisionId;
        initComponents();
    }

    private void initComponents() {
        setSize(980, 560);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(UIStyles.BG);

        Remision r;
        java.util.List<DetalleRemision> detalles;
        try {
            DatabaseManager db = DatabaseManager.getInstance();
            r = db.obtenerRemisionPorId(remisionId);
            detalles = db.obtenerDetallesRemision(remisionId);
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this, "Error al cargar la remisión: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (r == null) {
            return;
        }

        JPanel datos = new JPanel(new GridLayout(0, 4, 12, 6));
        datos.setOpaque(false);
        agregarDato(datos, "No. remisión", r.getNumeroRemision());
        agregarDato(datos, "Folio interno", String.valueOf(r.getId()));
        agregarDato(datos, "Proveedor", r.getProveedorNombre());
        agregarDato(datos, "Fecha", r.getFecha() != null ? r.getFecha().format(ReportesPdf.FECHA) : "");
        agregarDato(datos, "Obra", r.getObra());
        agregarDato(datos, "Envía", r.getEnvia());
        agregarDato(datos, "Recibió", r.getRecibe());
        agregarDato(datos, "Registrada", r.getFechaRegistro() != null ? r.getFechaRegistro().format(ReportesPdf.FECHA_HORA) : "");
        if (r.getObservaciones() != null && !r.getObservaciones().trim().isEmpty()) {
            agregarDato(datos, "Observaciones", r.getObservaciones());
        }
        add(UIStyles.createCard("Remisión", datos), BorderLayout.NORTH);

        DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"Item", "ID material", "Material", "Cantidad", "Unidad", "Categoría", "Tipo", "Descripción"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        int item = 1;
        for (DetalleRemision d : detalles) {
            modelo.addRow(new Object[]{item++, d.getHerramientaId(), d.getNombreMaterial(), d.getCantidad(),
                d.getUnidad(), d.getCategoria(), d.getTipo(), d.getDescripcion()});
        }
        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(26);
        tabla.getTableHeader().setReorderingAllowed(false);
        UIStyles.styleTableHeader(tabla);
        tabla.getColumnModel().getColumn(0).setCellRenderer(UIStyles.createCenteredNumberRenderer());
        tabla.getColumnModel().getColumn(1).setCellRenderer(UIStyles.createCenteredNumberRenderer());
        tabla.getColumnModel().getColumn(3).setCellRenderer(UIStyles.createCenteredNumberRenderer());
        tabla.getColumnModel().getColumn(2).setPreferredWidth(300);
        add(UIStyles.createCard("Materiales (" + r.getTotalPartidas() + " partidas, " + r.getTotalCantidad() + " en total)",
            new JScrollPane(tabla)), BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.setOpaque(false);
        JButton btnPdf = new JButton("Comprobante PDF");
        btnPdf.addActionListener(e -> PdfViewer.generarYAbrir(this, () -> ReportesPdf.remision(remisionId),
            "No se encontró la remisión"));
        UIStyles.stylePrimaryButton(btnPdf);
        UIStyles.applySvgIcon(btnPdf, "/icons/report.svg", 16);
        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.addActionListener(e -> dispose());
        UIStyles.styleSecondaryButton(btnCerrar);
        botones.add(btnCerrar);
        botones.add(btnPdf);
        add(botones, BorderLayout.SOUTH);
    }

    private void agregarDato(JPanel panel, String etiqueta, String valor) {
        JLabel lbl = new JLabel("<html><b>" + etiqueta + ":</b> " + escapar(valor) + "</html>");
        lbl.setForeground(UIStyles.TEXT);
        panel.add(lbl);
    }

    private static String escapar(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return "-";
        }
        return valor.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

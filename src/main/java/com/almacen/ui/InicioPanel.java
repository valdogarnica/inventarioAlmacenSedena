package com.almacen.ui;

import com.almacen.database.DatabaseManager;
import com.almacen.model.ResumenInicio;
import com.almacen.model.ResumenInicio.Dato;
import com.almacen.report.ReportesPdf;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

/**
 * Página "Inicio": indicadores del almacén, gráficas y lo que necesita atención
 * (materiales sin stock o con poco stock y préstamos pendientes más antiguos).
 */
public class InicioPanel extends JPanel implements Pagina {
    private final JSpinner spnUmbral = new JSpinner(new SpinnerNumberModel(AppPreferences.getUmbralStockBajo(), 0, 9999, 1));
    private final Indicador indMateriales = new Indicador("Materiales", "registrados");
    private final Indicador indDisponible = new Indicador("Piezas disponibles", "en el almacén");
    private final Indicador indPrestado = new Indicador("Piezas prestadas", "pendientes de devolver");
    private final Indicador indPrestamos = new Indicador("Préstamos activos", "con algo pendiente");
    private final Indicador indSinStock = new Indicador("Sin stock", "materiales en cero");
    private final Indicador indStockBajo = new Indicador("Stock bajo", "");
    private final JComponent txtAnalisis = UIStyles.textoAjustable("");
    private final GraficaBarras grafTipo = new GraficaBarras(false, "piezas disponibles");
    private final GraficaBarras grafMes = new GraficaBarras(true, "préstamos");
    private final GraficaBarras grafPrestados = new GraficaBarras(false, "piezas prestadas en total");
    private final GraficaBarras grafNoRetorno = new GraficaBarras(false, "piezas entregadas");
    private final GraficaBarras grafProveedor = new GraficaBarras(false, "piezas disponibles");
    private final DefaultTableModel modeloSinStock = modelo("Material", "Unidad", "Tipo", "Prestado");
    private final DefaultTableModel modeloBajo = modelo("Material", "Unidad", "Tipo", "Disponible");
    private final DefaultTableModel modeloAntiguos = modelo("Préstamo", "Cliente", "Fecha", "Días", "Pendientes");
    private ResumenInicio resumen;

    public InicioPanel() {
        super(new BorderLayout());
        setOpaque(false);

        JButton btnActualizar = new JButton("Actualizar");
        UIStyles.styleSecondaryButton(btnActualizar);
        UIStyles.applySvgIcon(btnActualizar, "/icons/refresh.svg", 16);
        btnActualizar.addActionListener(e -> alMostrar());
        JButton btnPdf = new JButton("Reporte PDF del análisis");
        UIStyles.stylePrimaryButton(btnPdf);
        UIStyles.applySvgIcon(btnPdf, "/icons/report.svg", 16);
        btnPdf.addActionListener(e -> PdfViewer.generarYAbrir(this,
            () -> ReportesPdf.analisisAlmacen(umbral()), "No hay materiales para analizar"));
        spnUmbral.setPreferredSize(new Dimension(80, spnUmbral.getPreferredSize().height));
        spnUmbral.addChangeListener(e -> {
            AppPreferences.setUmbralStockBajo(umbral());
            alMostrar();
        });
        JPanel fila = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 4));
        fila.setOpaque(false);
        fila.add(new JLabel("Stock bajo: hasta"));
        fila.add(spnUmbral);
        fila.add(new JLabel("piezas"));
        fila.add(btnActualizar);
        fila.add(btnPdf);

        JPanel indicadores = new JPanel(new GridResponsivo(170, 6, 12, 12));
        indicadores.setOpaque(false);
        for (Indicador i : new Indicador[]{indMateriales, indDisponible, indPrestado, indPrestamos, indSinStock, indStockBajo}) {
            indicadores.add(i);
        }

        UIStyles.textoSecundario(txtAnalisis);
        JPanel graficas = new JPanel(new GridResponsivo(420, 2, 14, 14));
        graficas.setOpaque(false);
        graficas.add(UIStyles.createCard("Análisis", txtAnalisis));
        graficas.add(UIStyles.createCard("Préstamos por mes (últimos 6 meses)", grafMes));
        graficas.add(UIStyles.createCard("Piezas disponibles por tipo", grafTipo));
        graficas.add(UIStyles.createCard("Piezas disponibles por proveedor", grafProveedor));
        graficas.add(UIStyles.createCard("Herramientas más prestadas", grafPrestados));
        graficas.add(UIStyles.createCard("Material de no retorno más entregado", grafNoRetorno));

        JPanel listas = new JPanel(new GridResponsivo(380, 3, 14, 14));
        listas.setOpaque(false);
        listas.add(UIStyles.createCard("Materiales sin stock", tabla(modeloSinStock)));
        listas.add(UIStyles.createCard("Materiales con stock bajo", tabla(modeloBajo)));
        listas.add(UIStyles.createCard("Préstamos pendientes más antiguos", tabla(modeloAntiguos)));

        JPanel contenido = new JPanel();
        contenido.setOpaque(false);
        contenido.setLayout(new javax.swing.BoxLayout(contenido, javax.swing.BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[]{fila, indicadores, graficas, listas}) {
            c.setAlignmentX(LEFT_ALIGNMENT);
            contenido.add(c);
            contenido.add(javax.swing.Box.createVerticalStrut(14));
        }
        JPanel norte = new JPanel(new BorderLayout());
        norte.setOpaque(false);
        norte.add(contenido, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(norte);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    private int umbral() {
        return (Integer) spnUmbral.getValue();
    }

    @Override
    public void alMostrar() {
        try {
            resumen = DatabaseManager.getInstance().obtenerResumenInicio(umbral());
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this, "Error al cargar el análisis: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        ResumenInicio r = resumen;
        indMateriales.setValor(r.materiales, null);
        indDisponible.setValor(r.disponible, null);
        indPrestado.setValor(r.prestado, null);
        indPrestamos.setValor(r.prestamosActivos, null);
        indSinStock.setValor(r.sinStock.size(), r.sinStock.isEmpty() ? null : "App.danger");
        indStockBajo.setValor(r.stockBajo.size(), r.stockBajo.isEmpty() ? null : "App.warning");
        indStockBajo.setDetalle("con " + r.umbralBajo + " piezas o menos");

        grafTipo.setDatos(r.stockPorTipo);
        grafMes.setDatos(r.prestamosPorMes);
        grafPrestados.setDatos(r.masPrestados);
        grafNoRetorno.setDatos(r.masEntregadosNoRetorno);
        grafProveedor.setDatos(r.stockPorProveedor);

        modeloSinStock.setRowCount(0);
        for (ResumenInicio.Material m : r.sinStock) {
            modeloSinStock.addRow(new Object[]{m.nombre, m.unidad, m.tipo, m.prestado});
        }
        modeloBajo.setRowCount(0);
        for (ResumenInicio.Material m : r.stockBajo) {
            modeloBajo.addRow(new Object[]{m.nombre, m.unidad, m.tipo, m.disponible});
        }
        modeloAntiguos.setRowCount(0);
        for (ResumenInicio.PrestamoPendiente p : r.prestamosAntiguos) {
            modeloAntiguos.addRow(new Object[]{"#" + p.id, p.cliente, p.fecha, p.dias, p.pendientes});
        }
        ((javax.swing.JTextArea) txtAnalisis).setText(String.join("\n", analisis(r)));
        revalidate();
        repaint();
    }

    /** Frases del análisis a partir de los datos (las mismas van en el PDF). */
    public static List<String> analisis(ResumenInicio r) {
        List<String> lineas = new ArrayList<>();
        if (r.materiales == 0) {
            lineas.add("• Todavía no hay materiales registrados.");
            return lineas;
        }
        int existencia = r.disponible + r.prestado;
        int porcentaje = existencia == 0 ? 0 : Math.round(r.prestado * 100f / existencia);
        lineas.add("• Hay " + r.materiales + " materiales con " + existencia + " piezas en total; "
            + porcentaje + "% están prestadas ahora.");
        if (r.sinStock.isEmpty()) {
            lineas.add("• Ningún material está en cero.");
        } else {
            lineas.add("• " + r.sinStock.size() + (r.sinStock.size() == 1 ? " material está" : " materiales están")
                + " en cero: " + nombres(r.sinStock) + ".");
        }
        if (!r.stockBajo.isEmpty()) {
            lineas.add("• " + r.stockBajo.size() + " con stock bajo (" + r.umbralBajo + " o menos): "
                + nombres(r.stockBajo) + ".");
        }
        if (!r.masPrestados.isEmpty()) {
            Dato d = r.masPrestados.get(0);
            lineas.add("• La herramienta más prestada es " + d.etiqueta + " (" + d.valor + " piezas en total).");
        }
        if (!r.masEntregadosNoRetorno.isEmpty()) {
            Dato d = r.masEntregadosNoRetorno.get(0);
            lineas.add("• El material de no retorno que más sale es " + d.etiqueta + " (" + d.valor + " piezas).");
        }
        if (!r.prestamosAntiguos.isEmpty()) {
            ResumenInicio.PrestamoPendiente p = r.prestamosAntiguos.get(0);
            lineas.add("• El préstamo pendiente más antiguo es el #" + p.id + " de " + p.cliente + ", con "
                + p.dias + (p.dias == 1 ? " día" : " días") + " y " + p.pendientes + " piezas por devolver.");
        } else {
            lineas.add("• No hay herramientas pendientes de devolver.");
        }
        return lineas;
    }

    private static String nombres(List<ResumenInicio.Material> materiales) {
        List<String> n = new ArrayList<>();
        for (int i = 0; i < materiales.size() && i < 5; i++) {
            n.add(materiales.get(i).nombre);
        }
        String texto = String.join(", ", n);
        return materiales.size() > 5 ? texto + " y " + (materiales.size() - 5) + " más" : texto;
    }

    private static DefaultTableModel modelo(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static JScrollPane tabla(DefaultTableModel modelo) {
        JTable tabla = new JTable(modelo);
        UIStyles.styleTableHeader(tabla);
        tabla.setRowHeight(30);
        boolean prestamos = "Préstamo".equals(modelo.getColumnName(0));
        tabla.getColumnModel().getColumn(0).setPreferredWidth(prestamos ? 70 : 170);
        if (prestamos) {
            tabla.getColumnModel().getColumn(1).setPreferredWidth(150);
            tabla.getColumnModel().getColumn(4).setPreferredWidth(90);
        }
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setPreferredSize(new Dimension(360, 250));
        return scroll;
    }

    /** Tarjeta con un número grande. */
    private static final class Indicador extends JPanel {
        private final JLabel lblValor = new JLabel("0");
        private final JLabel lblDetalle = new JLabel();

        Indicador(String titulo, String detalle) {
            super(new BorderLayout(0, 2));
            putClientProperty("FlatLaf.style", "background: $App.card; arc: 12; border: 12,16,12,16,$App.border,1,12");
            JLabel lblTitulo = new JLabel(titulo);
            UIStyles.textoSecundario(lblTitulo);
            lblTitulo.setFont(lblTitulo.getFont().deriveFont(Font.BOLD));
            lblValor.setFont(lblValor.getFont().deriveFont(Font.BOLD, lblValor.getFont().getSize2D() + 13f));
            lblDetalle.setText(detalle);
            UIStyles.textoSecundario(lblDetalle);
            lblDetalle.setFont(lblDetalle.getFont().deriveFont(lblDetalle.getFont().getSize2D() - 1f));
            add(lblTitulo, BorderLayout.NORTH);
            add(lblValor, BorderLayout.CENTER);
            add(lblDetalle, BorderLayout.SOUTH);
        }

        /** {@code color} es una clave del tema para resaltar el número, o null para el texto normal. */
        void setValor(int valor, String color) {
            lblValor.setText(String.format("%,d", valor));
            lblValor.putClientProperty("FlatLaf.style", "foreground: " + (color != null ? "$" + color : "$App.text"));
        }

        void setDetalle(String detalle) {
            lblDetalle.setText(detalle);
        }
    }
}

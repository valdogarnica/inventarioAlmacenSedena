package com.almacen.ui;

import com.almacen.database.DatabaseManager;
import com.almacen.model.Proveedor;
import com.almacen.report.ReportesPdf;
import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * Página de reportes PDF.
 */
public class ReportesPanel extends JPanel implements Pagina {
    private ProveedorSelector selProveedorInventario;
    private ProveedorSelector selProveedorEntradas;
    private final SelectorFecha dateDesde = new SelectorFecha();
    private final SelectorFecha dateHasta = new SelectorFecha();
    private final JComboBox<String> comboHerramienta = new JComboBox<>();
    private final JComboBox<String> comboTipo = new JComboBox<>();
    private static final String TODOS_LOS_TIPOS = "(Todos los tipos)";

    public ReportesPanel() {
        super(new BorderLayout());
        setOpaque(false);
        initComponents();
    }

    private void initComponents() {
        JPanel grid = new JPanel(new GridResponsivo(400, 2, 14, 14));
        grid.setOpaque(false);

        grid.add(tarjeta("Reporte general de inventario",
            "Todos los materiales activos con categoría, tipo, unidad, proveedor, disponible, prestado, total y fecha de alta. Incluye las existencias acumuladas por material (sumando proveedores) y el resumen por unidad.",
            null,
            () -> PdfViewer.generarYAbrir(this, () -> ReportesPdf.inventarioGeneral(null), "No hay materiales para reportar")));

        selProveedorInventario = new ProveedorSelector(false, false);
        grid.add(tarjeta("Inventario por proveedor",
            "Materiales que pertenecen a un proveedor, con su stock propio.",
            fila("Proveedor:", selProveedorInventario),
            () -> {
                Proveedor p = selProveedorInventario.getSeleccion();
                if (p == null) {
                    Notificaciones.showMessageDialog(this, "Seleccione un proveedor", "Información", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                PdfViewer.generarYAbrir(this, () -> ReportesPdf.inventarioGeneral(p.getId()), "Este proveedor no tiene materiales activos");
            }));

        dateDesde.setDateFormatString("dd/MM/yyyy");
        dateHasta.setDateFormatString("dd/MM/yyyy");
        dateDesde.setDate(Date.from(LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        dateHasta.setDate(new Date());
        dateDesde.setPermiteVacia(false);
        dateHasta.setPermiteVacia(false);
        selProveedorEntradas = new ProveedorSelector("(Todos los proveedores)", false);
        JPanel controlesEntradas = new JPanel(new GridLayout(2, 1, 4, 4));
        controlesEntradas.setOpaque(false);
        JPanel fechas = new JPanel(new WrapLayout(FlowLayout.LEFT, 6, 0));
        fechas.setOpaque(false);
        fechas.add(etiqueta("Desde:"));
        fechas.add(dateDesde);
        fechas.add(etiqueta("Hasta:"));
        fechas.add(dateHasta);
        controlesEntradas.add(fechas);
        controlesEntradas.add(fila("Proveedor:", selProveedorEntradas));
        grid.add(tarjeta("Entradas por remisión",
            "Material recibido por remisión en un rango de fechas: fecha, remisión, proveedor, material, unidad y cantidad.",
            controlesEntradas,
            () -> {
                LocalDate desde = aLocalDate(dateDesde.getDate());
                LocalDate hasta = aLocalDate(dateHasta.getDate());
                if (desde != null && hasta != null && desde.isAfter(hasta)) {
                    Notificaciones.showMessageDialog(this, "La fecha inicial es posterior a la final",
                        "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Integer proveedorId = selProveedorEntradas.getSeleccionId();
                PdfViewer.generarYAbrir(this, () -> ReportesPdf.entradasPorRemision(desde, hasta, proveedorId),
                    "No hay entradas por remisión en ese periodo");
            }));

        grid.add(tarjeta("Préstamos activos (general)",
            "Herramientas prestadas agrupadas por cliente y residente, con categoría, proveedor, unidad, cantidad y fecha.",
            null,
            () -> PdfViewer.generarYAbrir(this, ReportesPdf::prestamosActivos, "No hay préstamos activos para reportar")));

        comboHerramienta.setPreferredSize(new Dimension(260, 28));
        ComboBuscable.instalar(comboHerramienta);
        grid.add(tarjeta("Reporte por herramienta",
            "Existencias de la herramienta sumando todos sus proveedores (con lo que tiene cada uno), sus entradas por remisión y sus préstamos activos con fecha, unidad y categoría.",
            fila("Herramienta:", comboHerramienta),
            () -> {
                String h = (String) comboHerramienta.getSelectedItem();
                if (h == null || h.trim().isEmpty()) {
                    Notificaciones.showMessageDialog(this, "Seleccione una herramienta", "Información", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                PdfViewer.generarYAbrir(this, () -> ReportesPdf.prestamosPorHerramienta(h), "No hay datos de esa herramienta");
            }));

        comboTipo.setPreferredSize(new Dimension(260, 28));
        ComboBuscable.instalar(comboTipo);
        grid.add(tarjeta("Reporte por tipo",
            "Materiales agrupados por tipo (herramienta, material, equipo…): disponible, prestado, entregado como no retorno y existencia, con el resumen y la gráfica de todos los tipos.",
            fila("Tipo:", comboTipo),
            () -> {
                Object t = comboTipo.getSelectedItem();
                String tipo = t == null || TODOS_LOS_TIPOS.equals(t) ? null : t.toString();
                PdfViewer.generarYAbrir(this, () -> ReportesPdf.inventarioPorTipo(tipo), "No hay materiales de ese tipo");
            }));

        grid.add(tarjeta("Análisis del almacén",
            "Lo mismo que la página Inicio: indicadores, gráficas de préstamos y existencias, materiales sin stock o con stock bajo y préstamos pendientes más antiguos.",
            null,
            () -> PdfViewer.generarYAbrir(this, () -> ReportesPdf.analisisAlmacen(AppPreferences.getUmbralStockBajo()),
                "No hay materiales para analizar")));

        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setOpaque(false);
        contenedor.add(grid, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(contenedor);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    @Override
    public void alMostrar() {
        selProveedorInventario.recargar();
        selProveedorEntradas.recargar();
        try {
            Object actual = comboHerramienta.getSelectedItem();
            comboHerramienta.removeAllItems();
            for (String h : DatabaseManager.getInstance().obtenerNombresMateriales()) {
                comboHerramienta.addItem(h);
            }
            if (actual != null) {
                comboHerramienta.setSelectedItem(actual);
            }
            Object tipoActual = comboTipo.getSelectedItem();
            comboTipo.removeAllItems();
            comboTipo.addItem(TODOS_LOS_TIPOS);
            for (String t : DatabaseManager.getInstance().obtenerCatalogo(com.almacen.database.Catalogo.TIPOS)) {
                comboTipo.addItem(t);
            }
            comboTipo.setSelectedItem(tipoActual != null ? tipoActual : TODOS_LOS_TIPOS);
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this, "Error al cargar herramientas: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static LocalDate aLocalDate(Date fecha) {
        return fecha == null ? null : fecha.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static JLabel etiqueta(String texto) {
        JLabel lbl = new JLabel(texto);
        return lbl;
    }

    private static JPanel fila(String etiqueta, JComponent campo) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        p.add(etiqueta(etiqueta), BorderLayout.WEST);
        p.add(campo, BorderLayout.CENTER);
        return p;
    }

    private JPanel tarjeta(String titulo, String descripcion, JComponent controles, Runnable accion) {
        JPanel cuerpo = new JPanel(new BorderLayout(8, 10));
        cuerpo.setOpaque(false);
        JComponent lblDesc = UIStyles.textoAjustable(descripcion);
        cuerpo.add(lblDesc, BorderLayout.NORTH);
        if (controles != null) {
            JPanel envoltura = new JPanel(new BorderLayout());
            envoltura.setOpaque(false);
            envoltura.add(controles, BorderLayout.NORTH);
            cuerpo.add(envoltura, BorderLayout.CENTER);
        }
        JButton btn = new JButton("Generar PDF");
        UIStyles.stylePrimaryButton(btn);
        UIStyles.applySvgIcon(btn, "/icons/report.svg", 16);
        btn.addActionListener(e -> accion.run());
        JPanel pie = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        pie.setOpaque(false);
        pie.add(btn);
        cuerpo.add(pie, BorderLayout.SOUTH);
        return UIStyles.createCard(titulo, cuerpo);
    }
}

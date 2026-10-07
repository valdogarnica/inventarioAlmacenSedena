package com.almacen.ui;

import com.almacen.database.DatabaseManager;
import com.almacen.model.Prestamo;
import com.almacen.model.ReportePrestamoItem;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import javax.swing.*;
import com.toedter.calendar.JDateChooser;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class VerPrestamosDialog extends JDialog {
    private JTable tablaPrestamos;
    private DefaultTableModel modelo;
    private JButton btnDevolver;
    private JButton btnActualizar;
    private JButton btnReporte;
    private JButton btnAnterior;
    private JButton btnSiguiente;
    private JLabel lblPagina;
    private JComboBox<Integer> comboTamanoPagina;
    private JTextField txtFiltroCliente;
    private JTextField txtFiltroEmpleado;
    private JTextField txtFiltroResidente;
    private JDateChooser dateFiltroFecha;
    private JButton btnFiltrar;
    private JButton btnLimpiar;
    private int paginaActual = 1;
    private int totalRegistros = 0;
    
    public VerPrestamosDialog(JFrame parent) {
        super(parent, "Ver Préstamos", true);
        initComponents();
        cargarPagina(1);
    }
    
    private void initComponents() {
        setSize(1350, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(UIStyles.BG);

        JPanel panelFiltro = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelFiltro.setOpaque(false);
        JLabel lblCliente = new JLabel("Cliente:");
        lblCliente.setForeground(UIStyles.TEXT);
        panelFiltro.add(lblCliente);
        txtFiltroCliente = new JTextField(12);
        panelFiltro.add(txtFiltroCliente);

        JLabel lblEmpleado = new JLabel("Empleado:");
        lblEmpleado.setForeground(UIStyles.TEXT);
        panelFiltro.add(lblEmpleado);
        txtFiltroEmpleado = new JTextField(12);
        panelFiltro.add(txtFiltroEmpleado);

        JLabel lblResidente = new JLabel("Residente/Sobrestante:");
        lblResidente.setForeground(UIStyles.TEXT);
        panelFiltro.add(lblResidente);
        txtFiltroResidente = new JTextField(14);
        panelFiltro.add(txtFiltroResidente);

        JLabel lblFecha = new JLabel("Fecha préstamo:");
        lblFecha.setForeground(UIStyles.TEXT);
        panelFiltro.add(lblFecha);
        dateFiltroFecha = new JDateChooser();
        dateFiltroFecha.setDateFormatString("yyyy-MM-dd");
        dateFiltroFecha.setPreferredSize(new Dimension(120, 28));
        aplicarEstiloCalendario(dateFiltroFecha);
        panelFiltro.add(dateFiltroFecha);

        btnFiltrar = new JButton("Filtrar");
        btnFiltrar.addActionListener(e -> cargarPagina(1));
        UIStyles.styleSecondaryButton(btnFiltrar);
        panelFiltro.add(btnFiltrar);
        btnLimpiar = new JButton("Limpiar");
        btnLimpiar.addActionListener(e -> {
            txtFiltroCliente.setText("");
            txtFiltroEmpleado.setText("");
            txtFiltroResidente.setText("");
            dateFiltroFecha.setDate(null);
            cargarPagina(1);
        });
        UIStyles.styleSecondaryButton(btnLimpiar);
        panelFiltro.add(btnLimpiar);
        add(panelFiltro, BorderLayout.NORTH);

        javax.swing.event.DocumentListener filtroListener = new javax.swing.event.DocumentListener() {
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
        };
        txtFiltroCliente.getDocument().addDocumentListener(filtroListener);
        txtFiltroEmpleado.getDocument().addDocumentListener(filtroListener);
        txtFiltroResidente.getDocument().addDocumentListener(filtroListener);
        dateFiltroFecha.getDateEditor().addPropertyChangeListener("date", e -> cargarPagina(1));
        
        String[] columnas = {"ID", "Cliente", "Empleado", "Residente/Sobrestante", "Autorización", "Folio",
                             "Fecha Préstamo", "Fecha Devolución", "Días prestado", "Estado", "Foto", "Herramientas"};
        modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 10 || column == 11;
            }
        };
        tablaPrestamos = new JTable(modelo);
        tablaPrestamos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaPrestamos.getTableHeader().setReorderingAllowed(false);
        tablaPrestamos.setRowHeight(28);
        tablaPrestamos.getColumnModel().getColumn(10).setPreferredWidth(90);
        tablaPrestamos.getColumnModel().getColumn(11).setPreferredWidth(130);
        tablaPrestamos.getColumnModel().getColumn(10).setCellRenderer(new VerFotoRenderer());
        tablaPrestamos.getColumnModel().getColumn(10).setCellEditor(new VerFotoEditor());
        tablaPrestamos.getColumnModel().getColumn(11).setCellRenderer(new VerDetalleRenderer());
        tablaPrestamos.getColumnModel().getColumn(11).setCellEditor(new VerDetalleEditor());
        tablaPrestamos.getColumnModel().getColumn(9).setCellRenderer(new EstadoRowRenderer());
        // Estilizar encabezado y centrar números
        UIStyles.styleTableHeader(tablaPrestamos);
        aplicarCentradoNumeros();
        JScrollPane scroll = new JScrollPane(tablaPrestamos);
        add(UIStyles.createCard("Prestamos", scroll), BorderLayout.CENTER);
        
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelBotones.setOpaque(false);
        btnActualizar = new JButton("Actualizar");
        btnActualizar.addActionListener(e -> cargarPagina(paginaActual));
        btnDevolver = new JButton("Registrar devolución");
        btnDevolver.addActionListener(e -> abrirDevolucion());
        btnReporte = new JButton("Reporte PDF");
        btnReporte.addActionListener(e -> abrirSeleccionReporte());
        JButton btnVerDevueltos = new JButton("Ver préstamos devueltos");
        btnVerDevueltos.addActionListener(e -> abrirPrestamosDevueltos());
        JButton btnMateriales = new JButton("Materiales prestados");
        btnMateriales.addActionListener(e -> abrirMaterialesPrestados());
        UIStyles.styleSecondaryButton(btnActualizar);
        UIStyles.stylePrimaryButton(btnDevolver);
        UIStyles.styleSecondaryButton(btnReporte);
        UIStyles.styleSecondaryButton(btnVerDevueltos);
        UIStyles.styleSuccessButton(btnMateriales);
        UIStyles.applySvgIcon(btnActualizar, "/icons/refresh.svg", 16);
        UIStyles.applySvgIcon(btnDevolver, "/icons/save.svg", 16);
        UIStyles.applySvgIcon(btnReporte, "/icons/report.svg", 16);
        UIStyles.applySvgIcon(btnVerDevueltos, "/icons/search.svg", 16);
        UIStyles.applySvgIcon(btnMateriales, "/icons/search.svg", 16);
        // Aumentar tamaño de botones
        Dimension btnSize = new Dimension(180, 35);
        btnActualizar.setPreferredSize(btnSize);
        btnDevolver.setPreferredSize(btnSize);
        btnReporte.setPreferredSize(btnSize);
        btnVerDevueltos.setPreferredSize(btnSize);
        btnMateriales.setPreferredSize(btnSize);
        panelBotones.add(btnActualizar);
        panelBotones.add(btnDevolver);
        panelBotones.add(btnReporte);
        panelBotones.add(btnVerDevueltos);
        panelBotones.add(btnMateriales);

        JPanel panelPaginacion = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelPaginacion.setOpaque(false);
        JLabel lblTamano = new JLabel("Registros por página:");
        lblTamano.setForeground(UIStyles.TEXT);
        panelPaginacion.add(lblTamano);
        comboTamanoPagina = new JComboBox<>(new Integer[]{10, 20, 50});
        comboTamanoPagina.setSelectedItem(10);
        comboTamanoPagina.addActionListener(e -> cargarPagina(1));
        panelPaginacion.add(comboTamanoPagina);
        btnAnterior = new JButton("Anterior");
        btnAnterior.addActionListener(e -> cargarPagina(paginaActual - 1));
        UIStyles.styleSecondaryButton(btnAnterior);
        panelPaginacion.add(btnAnterior);
        btnSiguiente = new JButton("Siguiente");
        btnSiguiente.addActionListener(e -> cargarPagina(paginaActual + 1));
        UIStyles.styleSecondaryButton(btnSiguiente);
        panelPaginacion.add(btnSiguiente);
        lblPagina = new JLabel("Página 1 de 1");
        lblPagina.setForeground(UIStyles.TEXT);
        panelPaginacion.add(lblPagina);

        JPanel panelInferior = new JPanel(new BorderLayout());
        panelInferior.setOpaque(false);
        panelInferior.add(panelPaginacion, BorderLayout.CENTER);
        panelInferior.add(panelBotones, BorderLayout.EAST);
        add(panelInferior, BorderLayout.SOUTH);
    }

    private void aplicarEstiloCalendario(JDateChooser chooser) {
        try {
            JButton btn = chooser.getCalendarButton();
            btn.setPreferredSize(new Dimension(34, 28));
            UIStyles.styleSecondaryButton(btn);
            btn.setText("");
        } catch (Exception ignored) {
        }
        try {
            java.awt.Component editor = chooser.getDateEditor().getUiComponent();
            if (editor instanceof JComponent) {
                JComponent comp = (JComponent) editor;
                comp.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                comp.setBackground(Color.WHITE);
                comp.setForeground(UIStyles.TEXT);
            }
            com.toedter.calendar.JCalendar cal = chooser.getJCalendar();
            cal.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            cal.setBackground(Color.WHITE);
            cal.setForeground(UIStyles.TEXT);
            cal.getDayChooser().setDayBordersVisible(true);
            cal.getDayChooser().setWeekdayForeground(new Color(45, 108, 223));
            cal.getDayChooser().setSundayForeground(new Color(227, 75, 75));
            cal.getDayChooser().setDecorationBackgroundColor(new Color(245, 247, 251));
        } catch (Exception ignored) {
        }
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
            // Solo contar y obtener préstamos con estado PRESTADO
            String cliente = txtFiltroCliente.getText();
            String empleado = txtFiltroEmpleado.getText();
            String residente = txtFiltroResidente.getText();
            String fecha = dateFiltroFecha.getDate() != null
                ? new java.text.SimpleDateFormat("yyyy-MM-dd").format(dateFiltroFecha.getDate())
                : "";
            totalRegistros = dbManager.contarPrestamosPorEstadoConFiltros("PRESTADO", cliente, empleado, residente, fecha);
            int totalPaginas = (int) Math.ceil(totalRegistros / (double) tamano);
            if (totalPaginas == 0) {
                totalPaginas = 1;
            }
            paginaActual = Math.max(1, Math.min(pagina, totalPaginas));
            int offset = (paginaActual - 1) * tamano;

            List<Prestamo> prestamos = dbManager.obtenerPrestamosPaginadosPorEstadoConFiltros(
                "PRESTADO", cliente, empleado, residente, fecha, offset, tamano);
            modelo.setRowCount(0);
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            
            for (Prestamo p : prestamos) {
                String fechaPrestamo = p.getFechaPrestamo() != null ? 
                    p.getFechaPrestamo().format(formatter) : "";
                String fechaDevolucion = p.getFechaDevolucion() != null ? 
                    p.getFechaDevolucion().format(formatter) : "";
                long diasPrestado = calcularDiasPrestado(p.getFechaPrestamo(), p.getFechaDevolucion());
                
                modelo.addRow(new Object[]{
                    p.getId(),
                    p.getNombreCliente(),
                    p.getNombreEmpleado(),
                    p.getResidenteSobrestante(),
                    p.isAutorizacion() ? "Sí" : "No",
                    p.getFolio(),
                    fechaPrestamo,
                    fechaDevolucion,
                    diasPrestado,
                    p.getEstado(),
                    p.getFotoNombre(),
                    "Ver herramientas"
                });
            }

            lblPagina.setText("Página " + paginaActual + " de " + totalPaginas + " (Total: " + totalRegistros + ")");
            btnAnterior.setEnabled(paginaActual > 1);
            btnSiguiente.setEnabled(paginaActual < totalPaginas);
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this, 
                "Error al cargar préstamos: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void abrirDevolucion() {
        int filaSeleccionada = tablaPrestamos.getSelectedRow();
        if (filaSeleccionada == -1) {
            Notificaciones.showMessageDialog(this, 
                "Por favor seleccione un préstamo", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String estado = (String) modelo.getValueAt(filaSeleccionada, 9);
        if ("DEVUELTO".equals(estado)) {
            Notificaciones.showMessageDialog(this, 
                "Este préstamo ya fue devuelto", 
                "Información", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int idPrestamo = (Integer) modelo.getValueAt(filaSeleccionada, 0);
        DevolucionParcialDialog dialog = new DevolucionParcialDialog(this, idPrestamo);
        dialog.setVisible(true);
        cargarPagina(paginaActual);
    }

    private class VerFotoRenderer extends JButton implements TableCellRenderer {
        public VerFotoRenderer() {
            setText("Ver");
            UIStyles.styleSecondaryButton(this);
            setPreferredSize(new Dimension(80, 28));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value == null || value.toString().trim().isEmpty()) {
                setText("Sin foto");
                setEnabled(false);
            } else {
                setText("Ver");
                setEnabled(true);
            }
            return this;
        }
    }

    private class VerFotoEditor extends AbstractCellEditor implements TableCellEditor {
        private final JButton button = new JButton("Ver");
        private int row;

        public VerFotoEditor() {
            UIStyles.styleSecondaryButton(button);
            button.setPreferredSize(new Dimension(80, 28));
            button.addActionListener(e -> {
                verFotoPrestamo(row);
                stopCellEditing();
            });
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.row = row;
            if (value == null || value.toString().trim().isEmpty()) {
                button.setText("Sin foto");
                button.setEnabled(false);
            } else {
                button.setText("Ver");
                button.setEnabled(true);
            }
            return button;
        }
    }

    private void verFotoPrestamo(int row) {
        try {
            String fotoNombre = (String) modelo.getValueAt(row, 10);
            String rutaFotos = AppPreferences.getPhotoFolderPath();
            if (rutaFotos.isEmpty()) {
                Notificaciones.showMessageDialog(this,
                    "No está configurada la carpeta de fotos",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (fotoNombre == null || fotoNombre.trim().isEmpty()) {
                Notificaciones.showMessageDialog(this,
                    "Este préstamo no tiene foto",
                    "Información", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            File archivo = new File(rutaFotos, fotoNombre);
            if (!archivo.exists()) {
                Notificaciones.showMessageDialog(this,
                    "No se encontró la foto: " + archivo.getAbsolutePath(),
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Desktop.getDesktop().open(archivo);
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this,
                "Error al abrir la foto: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private class VerDetalleRenderer extends JButton implements TableCellRenderer {
        public VerDetalleRenderer() {
            setText("Ver");
            UIStyles.styleSecondaryButton(this);
            setPreferredSize(new Dimension(120, 28));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText("Ver");
            setEnabled(true);
            return this;
        }
    }

    private class VerDetalleEditor extends AbstractCellEditor implements TableCellEditor {
        private final JButton button = new JButton("Ver");
        private int row;

        public VerDetalleEditor() {
            UIStyles.styleSecondaryButton(button);
            button.setPreferredSize(new Dimension(120, 28));
            button.addActionListener(e -> {
                verDetallePrestamo(row);
                stopCellEditing();
            });
        }

        @Override
        public Object getCellEditorValue() {
            return "Ver";
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.row = row;
            return button;
        }
    }

    private void verDetallePrestamo(int row) {
        int idPrestamo = (Integer) modelo.getValueAt(row, 0);
        DetallePrestamoDialog dialog = new DetallePrestamoDialog(this, idPrestamo);
        dialog.setVisible(true);
    }

    private void generarReportePdf() {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            if (!dbManager.isConnected()) {
                Notificaciones.showMessageDialog(this,
                    "No hay conexión a la base de datos",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            List<ReportePrestamoItem> items = dbManager.obtenerReportePrestamosActivos();
            if (items.isEmpty()) {
                Notificaciones.showMessageDialog(this,
                    "No hay préstamos activos para reportar",
                    "Información", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            File archivo = File.createTempFile("reporte_prestamos_activos_", ".pdf");
            archivo.deleteOnExit();

            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage();
                document.addPage(page);
                PDPageContentStream content = new PDPageContentStream(document, page);
                float margin = 40;
                float y = 740;

                content.setFont(PDType1Font.HELVETICA_BOLD, 14);
                y = writeLine(content, "Reporte de préstamos activos", margin, y);
                content.setFont(PDType1Font.HELVETICA, 10);
                y = writeLine(content, "Incluye cliente (normalizado), residente/sobrestante y herramientas", margin, y - 8);
                y -= 16;

                java.util.LinkedHashMap<String, java.util.List<ReportePrestamoItem>> grupos = new java.util.LinkedHashMap<>();
                for (ReportePrestamoItem item : items) {
                    String cliente = normalizarNombre(item.getNombreCliente());
                    String residente = safe(item.getResidenteSobrestante());
                    String key = cliente + "||" + residente;
                    grupos.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(item);
                }

                float colHerr = margin;
                float colCat = margin + 230;
                float colCant = margin + 390;
                float colFecha = margin + 450;
                float tableRight = margin + 560;

                for (java.util.Map.Entry<String, java.util.List<ReportePrestamoItem>> entry : grupos.entrySet()) {
                    String[] partes = entry.getKey().split("\\|\\|", -1);
                    String cliente = partes.length > 0 ? partes[0] : "";
                    String residente = partes.length > 1 ? partes[1] : "";

                    if (y < margin + 80) {
                        content.close();
                        page = new PDPage();
                        document.addPage(page);
                        content = new PDPageContentStream(document, page);
                        y = 740;
                    }

                    content.setFont(PDType1Font.HELVETICA_BOLD, 11);
                    y = writeLine(content, "Cliente: " + cliente, margin, y);
                    content.setFont(PDType1Font.HELVETICA, 10);
                    y = writeLine(content, "Residente/Sobrestante: " + residente, margin, y);
                    y -= 4;

                    y = drawGeneralHeader(content, y, 18, colHerr, colCat, colCant, colFecha, tableRight);
                    content.setFont(PDType1Font.HELVETICA, 10);

                    for (ReportePrestamoItem item : entry.getValue()) {
                        if (y < margin + 60) {
                            content.close();
                            page = new PDPage();
                            document.addPage(page);
                            content = new PDPageContentStream(document, page);
                            y = 740;
                            y = drawGeneralHeader(content, y, 18, colHerr, colCat, colCant, colFecha, tableRight);
                            content.setFont(PDType1Font.HELVETICA, 10);
                        }

                        String herramienta = safe(item.getNombreHerramienta());
                        String categoria = safe(item.getCategoria());
                        String fecha = item.getFechaPrestamo() != null
                            ? item.getFechaPrestamo().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                            : "";
                        drawGeneralRowLine(content, y, 18, colHerr, colCat, colCant, colFecha, tableRight);
                        float textY = y - 5;
                        writeText(content, herramienta, colHerr + 4, textY);
                        writeText(content, categoria, colCat + 4, textY);
                        writeText(content, String.valueOf(item.getCantidad()), colCant + 4, textY);
                        writeText(content, fecha, colFecha + 4, textY);
                        y -= 18;
                    }

                    y -= 8;
                }

                content.close();
                document.save(archivo);
            }
            abrirPdfEnNavegador(archivo);
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this,
                "Error al generar reporte: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void generarReportePdfPorHerramienta(String herramienta) {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            if (!dbManager.isConnected()) {
                Notificaciones.showMessageDialog(this,
                    "No hay conexión a la base de datos",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (herramienta == null || herramienta.trim().isEmpty()) {
                Notificaciones.showMessageDialog(this,
                    "Seleccione una herramienta",
                    "Información", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            List<ReportePrestamoItem> items = dbManager.obtenerReportePrestamosActivosPorHerramienta(herramienta);
            if (items.isEmpty()) {
                Notificaciones.showMessageDialog(this,
                    "No hay préstamos activos para esa herramienta",
                    "Información", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            File archivo = File.createTempFile("reporte_prestamos_herramienta_", ".pdf");
            archivo.deleteOnExit();

            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage();
                document.addPage(page);
                PDPageContentStream content = new PDPageContentStream(document, page);
                float margin = 40;
                float y = 740;

                content.setFont(PDType1Font.HELVETICA_BOLD, 14);
                y = writeLine(content, "Reporte de préstamos activos por herramienta", margin, y);
                content.setFont(PDType1Font.HELVETICA, 10);
                y = writeLine(content, "Herramienta: " + herramienta, margin, y - 4);
                y -= 16;

                float colHerr = margin;
                float colCant = margin + 230;
                float colCliente = margin + 300;
                float colRes = margin + 430;
                float tableRight = margin + 520;

                y = drawHerramientaHeader(content, y, 18, colHerr, colCant, colCliente, colRes, tableRight);
                content.setFont(PDType1Font.HELVETICA, 10);

                for (ReportePrestamoItem item : items) {
                    if (y < margin + 60) {
                        content.close();
                        page = new PDPage();
                        document.addPage(page);
                        content = new PDPageContentStream(document, page);
                        y = 740;
                        y = drawHerramientaHeader(content, y, 18, colHerr, colCant, colCliente, colRes, tableRight);
                        content.setFont(PDType1Font.HELVETICA, 10);
                    }
                    drawHerramientaRowLine(content, y, 18, colHerr, colCant, colCliente, colRes, tableRight);
                    float textY = y - 5;
                    writeText(content, safe(item.getNombreHerramienta()), colHerr + 4, textY);
                    writeText(content, String.valueOf(item.getCantidad()), colCant + 4, textY);
                    writeText(content, safe(item.getNombreCliente()), colCliente + 4, textY);
                    writeText(content, safe(item.getResidenteSobrestante()), colRes + 4, textY);
                    y -= 18;
                }

                content.close();
                document.save(archivo);
            }
            abrirPdfEnNavegador(archivo);
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this,
                "Error al generar reporte: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void abrirSeleccionReporte() {
        SeleccionReporteDialog dialog = new SeleccionReporteDialog(this);
        dialog.setVisible(true);
    }

    private void abrirPdfEnNavegador(File archivo) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(archivo.toURI());
            } else {
                Notificaciones.showMessageDialog(this,
                    "Vista previa generada en: " + archivo.getAbsolutePath(),
                    "Información", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this,
                "Error al abrir PDF: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private class SeleccionReporteDialog extends JDialog {
        private final JRadioButton rbReporteGeneral = new JRadioButton("Reporte general de préstamos activos");
        private final JRadioButton rbReporteHerramienta = new JRadioButton("Reporte por herramienta");
        private final JComboBox<String> comboHerramientas = new JComboBox<>();

        public SeleccionReporteDialog(Window parent) {
            super(parent, "Seleccionar reporte", ModalityType.APPLICATION_MODAL);
            setSize(520, 240);
            setLocationRelativeTo(parent);
            setLayout(new BorderLayout(10, 10));
            getContentPane().setBackground(UIStyles.BG);

            JPanel panelOpciones = new JPanel(new GridBagLayout());
            panelOpciones.setOpaque(false);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(8, 8, 8, 8);

            ButtonGroup group = new ButtonGroup();
            group.add(rbReporteGeneral);
            group.add(rbReporteHerramienta);
            rbReporteGeneral.setSelected(true);
            rbReporteGeneral.setOpaque(false);
            rbReporteHerramienta.setOpaque(false);

            panelOpciones.add(rbReporteGeneral, gbc);
            gbc.gridy++;
            panelOpciones.add(rbReporteHerramienta, gbc);
            gbc.gridy++;
            panelOpciones.add(new JLabel("Herramienta:"), gbc);
            gbc.gridx = 1;
            comboHerramientas.setPreferredSize(new Dimension(240, 28));
            panelOpciones.add(comboHerramientas, gbc);

            comboHerramientas.setEnabled(false);

            rbReporteHerramienta.addActionListener(e -> comboHerramientas.setEnabled(true));
            rbReporteGeneral.addActionListener(e -> comboHerramientas.setEnabled(false));

            cargarHerramientasCombo();

            JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            panelBotones.setOpaque(false);
            JButton btnGenerar = new JButton("Generar");
            JButton btnCancelar = new JButton("Cancelar");
            UIStyles.stylePrimaryButton(btnGenerar);
            UIStyles.styleSecondaryButton(btnCancelar);
            panelBotones.add(btnCancelar);
            panelBotones.add(btnGenerar);

            btnCancelar.addActionListener(e -> dispose());
            btnGenerar.addActionListener(e -> {
                if (rbReporteGeneral.isSelected()) {
                    generarReportePdf();
                } else {
                    String herramienta = (String) comboHerramientas.getSelectedItem();
                    generarReportePdfPorHerramienta(herramienta);
                }
                dispose();
            });

            add(panelOpciones, BorderLayout.CENTER);
            add(panelBotones, BorderLayout.SOUTH);
        }

        private void cargarHerramientasCombo() {
            try {
                DatabaseManager dbManager = DatabaseManager.getInstance();
                if (!dbManager.isConnected()) {
                    return;
                }
                comboHerramientas.removeAllItems();
                for (String h : dbManager.obtenerHerramientasPrestadasActivas()) {
                    comboHerramientas.addItem(h);
                }
            } catch (Exception e) {
                Notificaciones.showMessageDialog(this,
                    "Error al cargar herramientas: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    private float writeLine(PDPageContentStream content, String text, float x, float y) throws Exception {
        content.beginText();
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
        return y - 14;
    }

    private void writeText(PDPageContentStream content, String text, float x, float y) throws Exception {
        content.beginText();
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
    }

    private float drawHerramientaHeader(PDPageContentStream content, float y, float rowHeight,
                                        float colHerr, float colCant, float colCliente, float colRes, float tableRight) throws Exception {
        float headerTop = y + 4;
        float textY = y - 5;
        content.setNonStrokingColor(new Color(230, 230, 230));
        content.addRect(colHerr - 4, y - rowHeight + 4, tableRight - colHerr + 8, rowHeight);
        content.fill();
        content.setNonStrokingColor(Color.BLACK);
        content.setLineWidth(0.5f);
        content.moveTo(colHerr - 4, headerTop);
        content.lineTo(tableRight + 4, headerTop);
        content.stroke();
        drawHerramientaVerticalLines(content, y, rowHeight, colHerr, colCant, colCliente, colRes, tableRight);

        content.setFont(PDType1Font.HELVETICA_BOLD, 10);
        writeText(content, "Herramienta", colHerr + 4, textY);
        writeText(content, "Cantidad", colCant + 4, textY);
        writeText(content, "Cliente", colCliente + 4, textY);
        writeText(content, "Residente", colRes + 4, textY);
        return y - rowHeight;
    }

    private void drawHerramientaRowLine(PDPageContentStream content, float y, float rowHeight,
                                        float colHerr, float colCant, float colCliente, float colRes, float tableRight) throws Exception {
        content.setLineWidth(0.4f);
        content.moveTo(colHerr - 4, y - rowHeight + 4);
        content.lineTo(tableRight + 4, y - rowHeight + 4);
        content.stroke();
        drawHerramientaVerticalLines(content, y, rowHeight, colHerr, colCant, colCliente, colRes, tableRight);
    }

    private void drawHerramientaVerticalLines(PDPageContentStream content, float y, float rowHeight,
                                              float colHerr, float colCant, float colCliente, float colRes, float tableRight) throws Exception {
        float top = y + 4;
        float bottom = y - rowHeight + 4;
        float left = colHerr - 4;
        float right = tableRight + 4;
        content.setLineWidth(0.4f);
        content.moveTo(left, top);
        content.lineTo(left, bottom);
        content.stroke();
        content.moveTo(colCant - 4, top);
        content.lineTo(colCant - 4, bottom);
        content.stroke();
        content.moveTo(colCliente - 4, top);
        content.lineTo(colCliente - 4, bottom);
        content.stroke();
        content.moveTo(colRes - 4, top);
        content.lineTo(colRes - 4, bottom);
        content.stroke();
        content.moveTo(right, top);
        content.lineTo(right, bottom);
        content.stroke();
    }

    private float drawGeneralHeader(PDPageContentStream content, float y, float rowHeight,
                                    float colHerr, float colCat, float colCant, float colFecha, float tableRight) throws Exception {
        float headerTop = y + 4;
        float textY = y - 5;
        content.setNonStrokingColor(new Color(230, 230, 230));
        content.addRect(colHerr - 4, y - rowHeight + 4, tableRight - colHerr + 8, rowHeight);
        content.fill();
        content.setNonStrokingColor(Color.BLACK);
        content.setLineWidth(0.5f);
        content.moveTo(colHerr - 4, headerTop);
        content.lineTo(tableRight + 4, headerTop);
        content.stroke();
        drawGeneralVerticalLines(content, y, rowHeight, colHerr, colCat, colCant, colFecha, tableRight);

        content.setFont(PDType1Font.HELVETICA_BOLD, 10);
        writeText(content, "Herramienta", colHerr + 4, textY);
        writeText(content, "Categoría", colCat + 4, textY);
        writeText(content, "Cantidad", colCant + 4, textY);
        writeText(content, "Fecha", colFecha + 4, textY);
        return y - rowHeight;
    }

    private void drawGeneralRowLine(PDPageContentStream content, float y, float rowHeight,
                                    float colHerr, float colCat, float colCant, float colFecha, float tableRight) throws Exception {
        content.setLineWidth(0.4f);
        content.moveTo(colHerr - 4, y - rowHeight + 4);
        content.lineTo(tableRight + 4, y - rowHeight + 4);
        content.stroke();
        drawGeneralVerticalLines(content, y, rowHeight, colHerr, colCat, colCant, colFecha, tableRight);
    }

    private void drawGeneralVerticalLines(PDPageContentStream content, float y, float rowHeight,
                                          float colHerr, float colCat, float colCant, float colFecha, float tableRight) throws Exception {
        float top = y + 4;
        float bottom = y - rowHeight + 4;
        float left = colHerr - 4;
        float right = tableRight + 4;
        content.setLineWidth(0.4f);
        content.moveTo(left, top);
        content.lineTo(left, bottom);
        content.stroke();
        content.moveTo(colCat - 4, top);
        content.lineTo(colCat - 4, bottom);
        content.stroke();
        content.moveTo(colCant - 4, top);
        content.lineTo(colCant - 4, bottom);
        content.stroke();
        content.moveTo(colFecha - 4, top);
        content.lineTo(colFecha - 4, bottom);
        content.stroke();
        content.moveTo(right, top);
        content.lineTo(right, bottom);
        content.stroke();
    }

    private String normalizarNombre(String nombre) {
        if (nombre == null) {
            return "";
        }
        String limpio = nombre.trim().replaceAll("\\s+", " ").toLowerCase();
        String[] partes = limpio.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String p : partes) {
            if (p.isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) {
                sb.append(p.substring(1));
            }
            sb.append(" ");
        }
        return sb.toString().trim();
    }

    private String safe(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private long calcularDiasPrestado(LocalDateTime inicio, LocalDateTime fin) {
        if (inicio == null) {
            return 0;
        }
        LocalDateTime finCalc = fin != null ? fin : LocalDateTime.now();
        long dias = ChronoUnit.DAYS.between(inicio, finCalc);
        return Math.max(0, dias);
    }

    private void aplicarCentradoNumeros() {
        DefaultTableCellRenderer center = UIStyles.createCenteredNumberRenderer();
        tablaPrestamos.getColumnModel().getColumn(0).setCellRenderer(center); // ID
        tablaPrestamos.getColumnModel().getColumn(8).setCellRenderer(center); // Días prestado
    }

    private void abrirPrestamosDevueltos() {
        VerPrestamosDevueltosDialog dialog = new VerPrestamosDevueltosDialog((JFrame) getParent());
        dialog.setVisible(true);
    }

    private void abrirMaterialesPrestados() {
        MaterialesPrestadosDialog dialog = new MaterialesPrestadosDialog(this);
        dialog.setVisible(true);
    }

    private class EstadoRowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, 
                boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.CENTER);
            
            try {
                String estado = (String) value;
                int idPrestamo = (Integer) modelo.getValueAt(row, 0);
                DatabaseManager dbManager = DatabaseManager.getInstance();
                
                if ("PRESTADO".equals(estado)) {
                    // Verificar si tiene devoluciones parciales (incompleto)
                    boolean incompleto = dbManager.tieneDevolucionesParciales(idPrestamo);
                    if (incompleto) {
                        // Amarillo para préstamos incompletos
                        c.setBackground(new Color(255, 255, 200)); // Amarillo claro
                    } else {
                        // Rojo claro para préstamos prestados completos
                        c.setBackground(new Color(255, 200, 200)); // Rojo claro
                    }
                    c.setForeground(Color.BLACK);
                } else {
                    // Color por defecto para otros estados
                    c.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                    c.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
                }
                
                if (isSelected) {
                    Color currentBg = c.getBackground();
                    c.setBackground(new Color(
                        Math.max(0, currentBg.getRed() - 30),
                        Math.max(0, currentBg.getGreen() - 30),
                        Math.max(0, currentBg.getBlue() - 30)
                    ));
                }
            } catch (Exception e) {
                // En caso de error, mantener color por defecto
                c.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                c.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
            }
            
            return c;
        }
    }
}

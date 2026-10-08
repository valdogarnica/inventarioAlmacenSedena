package com.almacen.ui;

import com.almacen.database.DatabaseManager;
import com.almacen.model.Prestamo;
import com.almacen.report.ReportesPdf;
import javax.swing.*;
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
    private SelectorFecha dateFiltroFecha;
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

        JPanel panelFiltro = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelFiltro.setOpaque(false);
        JLabel lblCliente = new JLabel("Cliente:");
        panelFiltro.add(lblCliente);
        txtFiltroCliente = new JTextField(12);
        panelFiltro.add(txtFiltroCliente);

        JLabel lblEmpleado = new JLabel("Empleado:");
        panelFiltro.add(lblEmpleado);
        txtFiltroEmpleado = new JTextField(12);
        panelFiltro.add(txtFiltroEmpleado);

        JLabel lblResidente = new JLabel("Residente/Sobrestante:");
        panelFiltro.add(lblResidente);
        txtFiltroResidente = new JTextField(14);
        panelFiltro.add(txtFiltroResidente);

        JLabel lblFecha = new JLabel("Fecha préstamo:");
        panelFiltro.add(lblFecha);
        dateFiltroFecha = new SelectorFecha();
        dateFiltroFecha.setDateFormatString("yyyy-MM-dd");
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
        dateFiltroFecha.addPropertyChangeListener("date", e -> cargarPagina(1));
        
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
        tablaPrestamos.setRowHeight(32);
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
        UIStyles.tamanoMinimo(btnActualizar, btnSize.width, btnSize.height);
        UIStyles.tamanoMinimo(btnDevolver, btnSize.width, btnSize.height);
        UIStyles.tamanoMinimo(btnReporte, btnSize.width, btnSize.height);
        UIStyles.tamanoMinimo(btnVerDevueltos, btnSize.width, btnSize.height);
        UIStyles.tamanoMinimo(btnMateriales, btnSize.width, btnSize.height);
        panelBotones.add(btnActualizar);
        panelBotones.add(btnDevolver);
        panelBotones.add(btnReporte);
        panelBotones.add(btnVerDevueltos);
        panelBotones.add(btnMateriales);

        JPanel panelPaginacion = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelPaginacion.setOpaque(false);
        JLabel lblTamano = new JLabel("Registros por página:");
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
        panelPaginacion.add(lblPagina);

        JPanel panelInferior = new JPanel(new BorderLayout());
        panelInferior.setOpaque(false);
        panelInferior.add(panelPaginacion, BorderLayout.CENTER);
        panelInferior.add(panelBotones, BorderLayout.EAST);
        add(panelInferior, BorderLayout.SOUTH);
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
            return Tema.alDia(button);
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
            File archivo = AppPreferences.archivoFoto(fotoNombre);
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
            return Tema.alDia(button);
        }
    }

    private void verDetallePrestamo(int row) {
        int idPrestamo = (Integer) modelo.getValueAt(row, 0);
        DetallePrestamoDialog dialog = new DetallePrestamoDialog(this, idPrestamo);
        dialog.setVisible(true);
    }

    private void generarReportePdf() {
        PdfViewer.generarYAbrir(this, ReportesPdf::prestamosActivos, "No hay préstamos activos para reportar");
    }

    private void generarReportePdfPorHerramienta(String herramienta) {
        if (herramienta == null || herramienta.trim().isEmpty()) {
            Notificaciones.showMessageDialog(this,
                "Seleccione una herramienta",
                "Información", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        PdfViewer.generarYAbrir(this, () -> ReportesPdf.prestamosPorHerramienta(herramienta),
            "No hay préstamos activos para esa herramienta");
    }

    private void abrirSeleccionReporte() {
        SeleccionReporteDialog dialog = new SeleccionReporteDialog(this);
        dialog.setVisible(true);
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
            ComboBuscable.instalar(comboHerramientas);
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

    /** Estado como insignia: amarillo si tiene devoluciones parciales, rojo si sigue prestado completo. */
    private class EstadoRowRenderer extends InsigniaRenderer {
        @Override
        protected Tono tono(JTable table, Object value, int row) {
            if (!"PRESTADO".equals(value)) {
                return Tono.INFO;
            }
            int idPrestamo = (Integer) modelo.getValueAt(row, 0);
            try {
                return DatabaseManager.getInstance().tieneDevolucionesParciales(idPrestamo) ? Tono.AVISO : Tono.PELIGRO;
            } catch (java.sql.SQLException e) {
                return Tono.PELIGRO;
            }
        }
    }
}

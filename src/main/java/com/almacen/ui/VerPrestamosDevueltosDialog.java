package com.almacen.ui;

import com.almacen.database.DatabaseManager;
import com.almacen.model.Prestamo;
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

public class VerPrestamosDevueltosDialog extends JDialog {
    private JTable tablaPrestamos;
    private DefaultTableModel modelo;
    private JButton btnActualizar;
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
    
    public VerPrestamosDevueltosDialog(JFrame parent) {
        super(parent, "Préstamos Devueltos", true);
        initComponents();
        cargarPagina(1);
    }
    
    private void initComponents() {
        setSize(1200, 650);
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
        add(UIStyles.createCard("Préstamos Devueltos", scroll), BorderLayout.CENTER);
        
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelBotones.setOpaque(false);
        btnActualizar = new JButton("Actualizar");
        btnActualizar.addActionListener(e -> cargarPagina(paginaActual));
        UIStyles.styleSecondaryButton(btnActualizar);
        Dimension btnSize = new Dimension(180, 35);
        btnActualizar.setPreferredSize(btnSize);
        UIStyles.applySvgIcon(btnActualizar, "/icons/refresh.svg", 16);
        panelBotones.add(btnActualizar);

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
            String cliente = txtFiltroCliente.getText();
            String empleado = txtFiltroEmpleado.getText();
            String residente = txtFiltroResidente.getText();
            String fecha = dateFiltroFecha.getDate() != null
                ? new java.text.SimpleDateFormat("yyyy-MM-dd").format(dateFiltroFecha.getDate())
                : "";
            totalRegistros = dbManager.contarPrestamosPorEstadoConFiltros("DEVUELTO", cliente, empleado, residente, fecha);
            int totalPaginas = (int) Math.ceil(totalRegistros / (double) tamano);
            if (totalPaginas == 0) {
                totalPaginas = 1;
            }
            paginaActual = Math.max(1, Math.min(pagina, totalPaginas));
            int offset = (paginaActual - 1) * tamano;

            List<Prestamo> prestamos = dbManager.obtenerPrestamosPaginadosPorEstadoConFiltros(
                "DEVUELTO", cliente, empleado, residente, fecha, offset, tamano);
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

    private class EstadoRowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, 
                boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.CENTER);
            
            try {
                String estado = (String) value;
                if ("DEVUELTO".equals(estado)) {
                    // Verde claro para préstamos devueltos
                    c.setBackground(new Color(200, 255, 200)); // Verde claro
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

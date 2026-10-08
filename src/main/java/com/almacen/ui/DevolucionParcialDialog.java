package com.almacen.ui;

import com.almacen.database.DatabaseManager;
import com.almacen.model.DevolucionItem;
import com.almacen.model.DetallePrestamo;
import com.almacen.model.Prestamo;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class DevolucionParcialDialog extends JDialog {
    private final int prestamoId;
    private Prestamo prestamo;
    private JTable tabla;
    private DevolucionTableModel modelo;
    private JCheckBox chkEntregaCompleta;
    private JButton btnDevolver;
    private JButton btnCancelar;
    private JButton btnComprobante;

    public DevolucionParcialDialog(Window parent, int prestamoId) {
        super(parent, "Registrar devolución", ModalityType.APPLICATION_MODAL);
        this.prestamoId = prestamoId;
        initComponents();
        cargarDatos();
    }

    private void initComponents() {
        setSize(1100, 750);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        modelo = new DevolucionTableModel();
        tabla = new JTable(modelo);
        tabla.setRowHeight(32);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getColumnModel().getColumn(4).setCellEditor(new SpinnerEditor());
        tabla.getColumnModel().getColumn(4).setCellRenderer(new SpinnerRenderer());
        tabla.getColumnModel().getColumn(6).setPreferredWidth(220);
        // Estilizar encabezado y centrar números
        UIStyles.styleTableHeader(tabla);
        DefaultTableCellRenderer centerRenderer = UIStyles.createCenteredNumberRenderer();
        tabla.getColumnModel().getColumn(2).setCellRenderer(centerRenderer); // Prestado
        tabla.getColumnModel().getColumn(3).setCellRenderer(centerRenderer); // Pendiente
        tabla.getColumnModel().getColumn(4).setCellRenderer(centerRenderer); // Devolver
        JScrollPane scroll = new JScrollPane(tabla);

        JPanel panelCentro = new JPanel(new BorderLayout(8, 8));
        panelCentro.setOpaque(false);
        chkEntregaCompleta = new JCheckBox("Entrega completa (devolver todo)");
        chkEntregaCompleta.setOpaque(false);
        chkEntregaCompleta.addActionListener(e -> aplicarEntregaCompleta());
        panelCentro.add(chkEntregaCompleta, BorderLayout.NORTH);
        panelCentro.add(scroll, BorderLayout.CENTER);
        add(UIStyles.createCard("Herramientas prestadas", panelCentro), BorderLayout.CENTER);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelBotones.setOpaque(false);
        btnComprobante = new JButton("Comprobante PDF");
        btnComprobante.addActionListener(e -> generarComprobante());
        UIStyles.styleSecondaryButton(btnComprobante);
        btnDevolver = new JButton("Registrar devolución");
        btnDevolver.addActionListener(e -> registrarDevolucion());
        UIStyles.stylePrimaryButton(btnDevolver);
        btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());
        UIStyles.styleSecondaryButton(btnCancelar);
        
       

        panelBotones.add(btnComprobante);
        panelBotones.add(btnCancelar);
        panelBotones.add(btnDevolver);
        add(panelBotones, BorderLayout.SOUTH);
    }

    private JPanel crearPanelInformacionPrestamo() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Panel izquierdo - Información del préstamo
        JPanel panelInfo = new JPanel(new GridBagLayout());
        panelInfo.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 15);
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        panelInfo.add(crearEtiqueta("ID Préstamo:"), gbc);
        gbc.gridx = 1;
        JLabel lblId = new JLabel("-");
        panelInfo.add(lblId, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        panelInfo.add(crearEtiqueta("Cliente:"), gbc);
        gbc.gridx = 1;
        JLabel lblCliente = new JLabel("-");
        panelInfo.add(lblCliente, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        panelInfo.add(crearEtiqueta("Empleado:"), gbc);
        gbc.gridx = 1;
        JLabel lblEmpleado = new JLabel("-");
        panelInfo.add(lblEmpleado, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        panelInfo.add(crearEtiqueta("Residente/Sobrestante:"), gbc);
        gbc.gridx = 1;
        JLabel lblResidente = new JLabel("-");
        panelInfo.add(lblResidente, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 4;
        panelInfo.add(crearEtiqueta("Fecha Préstamo:"), gbc);
        gbc.gridx = 1;
        JLabel lblFechaPrestamo = new JLabel("-");
        panelInfo.add(lblFechaPrestamo, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 5;
        panelInfo.add(crearEtiqueta("Folio:"), gbc);
        gbc.gridx = 1;
        JLabel lblFolio = new JLabel("-");
        panelInfo.add(lblFolio, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 6;
        panelInfo.add(crearEtiqueta("Autorización:"), gbc);
        gbc.gridx = 1;
        JLabel lblAutorizacion = new JLabel("-");
        panelInfo.add(lblAutorizacion, gbc);
        
        // Actualizar información cuando se cargue el préstamo
        if (prestamo != null) {
            lblId.setText(String.valueOf(prestamo.getId()));
            lblCliente.setText(prestamo.getNombreCliente() != null ? prestamo.getNombreCliente() : "-");
            lblEmpleado.setText(prestamo.getNombreEmpleado() != null ? prestamo.getNombreEmpleado() : "-");
            lblResidente.setText(prestamo.getResidenteSobrestante() != null ? prestamo.getResidenteSobrestante() : "-");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            lblFechaPrestamo.setText(prestamo.getFechaPrestamo() != null ? prestamo.getFechaPrestamo().format(formatter) : "-");
            lblFolio.setText(prestamo.getFolio() != null && !prestamo.getFolio().trim().isEmpty() ? prestamo.getFolio() : "-");
            lblAutorizacion.setText(prestamo.isAutorizacion() ? "Sí" : "No");
        }
        
        // Panel derecho - Foto
        JPanel panelFoto = new JPanel(new BorderLayout());
        panelFoto.setOpaque(false);
        JLabel lblFotoTitulo = new JLabel("Foto del préstamo:");
        lblFotoTitulo.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        panelFoto.add(lblFotoTitulo, BorderLayout.NORTH);
        
        JLabel lblFoto = new JLabel();
        lblFoto.setHorizontalAlignment(SwingConstants.CENTER);
        lblFoto.putClientProperty("FlatLaf.style", "border: 1,1,1,1,$App.border,1,8");
        lblFoto.setPreferredSize(new Dimension(200, 200));
        lblFoto.setMinimumSize(new Dimension(200, 200));
        lblFoto.setMaximumSize(new Dimension(200, 200));
        cargarFoto(lblFoto);
        
        JScrollPane scrollFoto = new JScrollPane(lblFoto);
        scrollFoto.setPreferredSize(new Dimension(220, 220));
        panelFoto.add(scrollFoto, BorderLayout.CENTER);
        
        panel.add(UIStyles.createCard("Información del préstamo", panelInfo), BorderLayout.CENTER);
        panel.add(UIStyles.createCard("Foto", panelFoto), BorderLayout.EAST);
        
        return panel;
    }
    
    private JLabel crearEtiqueta(String texto) {
        JLabel label = new JLabel(texto);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        return label;
    }
   
    private void cargarFoto(JLabel lblFoto) {
        if (prestamo == null || prestamo.getFotoNombre() == null || prestamo.getFotoNombre().trim().isEmpty()) {
            lblFoto.setText("Sin foto");
            lblFoto.setIcon(null);
            return;
        }
        
        try {
            String rutaFotos = AppPreferences.getPhotoFolderPath();
            if (rutaFotos.isEmpty()) {
                lblFoto.setText("Carpeta de fotos no configurada");
                lblFoto.setIcon(null);
                return;
            }
            
            File archivoFoto = AppPreferences.archivoFoto(prestamo.getFotoNombre());
            if (!archivoFoto.exists()) {
                lblFoto.setText("Foto no encontrada");
                lblFoto.setIcon(null);
                return;
            }
            
            BufferedImage imagen = ImageIO.read(archivoFoto);
            if (imagen != null) {
                ImageIcon icono = new ImageIcon(escalarImagen(imagen, 200, 200));
                lblFoto.setIcon(icono);
                lblFoto.setText("");
            } else {
                lblFoto.setText("Error al cargar foto");
                lblFoto.setIcon(null);
            }
        } catch (Exception e) {
            lblFoto.setText("Error: " + e.getMessage());
            lblFoto.setIcon(null);
        }
    }
    
    private Image escalarImagen(BufferedImage imagenOriginal, int anchoMax, int altoMax) {
        int ancho = imagenOriginal.getWidth();
        int alto = imagenOriginal.getHeight();
        
        double escalaAncho = (double) anchoMax / ancho;
        double escalaAlto = (double) altoMax / alto;
        double escala = Math.min(escalaAncho, escalaAlto);
        
        int nuevoAncho = (int) (ancho * escala);
        int nuevoAlto = (int) (alto * escala);
        
        return imagenOriginal.getScaledInstance(nuevoAncho, nuevoAlto, Image.SCALE_SMOOTH);
    }

    private void cargarDatos() {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            prestamo = dbManager.obtenerPrestamoPorId(prestamoId);
            List<DetallePrestamo> detalles = dbManager.obtenerDetallesPrestamo(prestamoId);
            modelo.setDetalles(detalles);
            chkEntregaCompleta.setSelected(false);
            tabla.setEnabled(true);
            
            // Crear y agregar panel de información después de cargar el préstamo
            JPanel panelSuperior = crearPanelInformacionPrestamo();
            add(panelSuperior, BorderLayout.NORTH);
            revalidate();
            repaint();
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this,
                "Error al cargar datos: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void registrarDevolucion() {
        try {
            if (modelo.tieneObservacionesIncompletas()) {
                Notificaciones.showMessageDialog(this,
                    "Debe ingresar la observación en las herramientas marcadas",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            List<DevolucionItem> devoluciones = modelo.getDevoluciones();
            if (devoluciones.isEmpty()) {
                Notificaciones.showMessageDialog(this,
                    "No hay cantidades para devolver",
                    "Información", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            DatabaseManager dbManager = DatabaseManager.getInstance();
            dbManager.registrarDevolucionParcial(prestamoId, devoluciones);
            Notificaciones.showMessageDialog(this,
                "Devolución registrada",
                "Éxito", JOptionPane.INFORMATION_MESSAGE);
            cargarDatos();
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this,
                "Error al registrar devolución: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void generarComprobante() {
        try {
            boolean hayDevolucion = false;
            for (int i = 0; i < modelo.detalles.size(); i++) {
                if (modelo.devolver.get(i) > 0) {
                    hayDevolucion = true;
                    break;
                }
            }
            if (!hayDevolucion) {
                Notificaciones.showMessageDialog(this,
                    "No hay cantidades para devolver",
                    "Información", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            java.io.File archivo = java.io.File.createTempFile("comprobante_devolucion_", ".pdf");
            archivo.deleteOnExit();

            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage();
                document.addPage(page);
                PDPageContentStream content = new PDPageContentStream(document, page);
                float margin = 40;
                float y = 720; // Bajamos más el contenido inicial
                float rowHeight = 16; // Aumentamos el alto de fila para mejor legibilidad

                content.setFont(PDType1Font.HELVETICA_BOLD, 16);
                y = writeLine(content, "Comprobante de devolución (vista previa)", margin, y);
                y -= 10; // Espacio adicional después del título
                content.setFont(PDType1Font.HELVETICA, 11);
                String cliente = prestamo != null ? prestamo.getNombreCliente() : "";
                String residente = prestamo != null ? prestamo.getResidenteSobrestante() : "";
                y = writeLine(content, "Cliente: " + cliente, margin, y);
                y = writeLine(content, "Residente/Sobrestante: " + residente, margin, y);
                y = writeLine(content, "Fecha: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), margin, y);
                y -= 20; // Más espacio antes de la tabla

                // Mejorar alineación de columnas
                float colHerr = margin + 5;
                float colPrest = margin + 280; // Ajustado para mejor alineación
                float colDev = margin + 360;   // Ajustado para mejor alineación
                float colPend = margin + 440;  // Ajustado para mejor alineación
                float tableRight = margin + 520;

                y = drawTableHeader(content, y, rowHeight, colHerr, colPrest, colDev, colPend, tableRight);

                for (int i = 0; i < modelo.detalles.size(); i++) {
                    int devolverAhora = modelo.devolver.get(i);
                    DetallePrestamo d = modelo.detalles.get(i);
                    int pendienteBase = d.getPendiente();
                    int pendienteNuevo = Math.max(0, pendienteBase - devolverAhora);

                    if (y < margin + 80) {
                        content.close();
                        page = new PDPage();
                        document.addPage(page);
                        content = new PDPageContentStream(document, page);
                        y = 720;
                        y = drawTableHeader(content, y, rowHeight, colHerr, colPrest, colDev, colPend, tableRight);
                    }
                    drawRowLine(content, y, tableRight, rowHeight, colHerr, colPrest, colDev, colPend);
                    content.setFont(PDType1Font.HELVETICA, 10);
                    // Texto alineado a la izquierda para herramienta
                    writeText(content, d.getNombreHerramienta(), colHerr, y);
                    // Números centrados
                    writeTextCentered(content, String.valueOf(d.getCantidad()), colPrest, colDev - 5, y);
                    writeTextCentered(content, String.valueOf(devolverAhora), colDev, colPend - 5, y);
                    writeTextCentered(content, String.valueOf(pendienteNuevo), colPend, tableRight, y);
                    y -= rowHeight;
                }

                content.close();
                document.save(archivo);
            }

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(archivo);
            } else {
                Notificaciones.showMessageDialog(this,
                    "Vista previa generada en: " + archivo.getAbsolutePath(),
                    "Información", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this,
                "Error al generar comprobante: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
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
    
    private void writeTextCentered(PDPageContentStream content, String text, float leftX, float rightX, float y) throws Exception {
        // Calcular ancho del texto usando el tamaño de fuente actual (10)
        float fontSize = 10f;
        float textWidth = PDType1Font.HELVETICA.getStringWidth(text) / 1000f * fontSize;
        float centerX = (leftX + rightX) / 2f;
        float startX = centerX - (textWidth / 2f);
        content.beginText();
        content.setFont(PDType1Font.HELVETICA, fontSize);
        content.newLineAtOffset(startX, y);
        content.showText(text);
        content.endText();
    }

    private float drawTableHeader(PDPageContentStream content, float y, float rowHeight,
                                  float colHerr, float colPrest, float colDev, float colPend, float tableRight) throws Exception {
        float headerTop = y + 6;
        float headerBottom = y - rowHeight + 2;
        
        // Dibujar fondo del header
        content.setNonStrokingColor(new Color(230, 230, 230));
        content.addRect(colHerr - 5, headerBottom, tableRight - colHerr + 10, rowHeight + 4);
        content.fill();
        
        // Dibujar bordes
        content.setNonStrokingColor(Color.BLACK);
        content.setLineWidth(0.5f);
        // Línea superior
        content.moveTo(colHerr - 5, headerTop);
        content.lineTo(tableRight + 5, headerTop);
        content.stroke();
        // Línea inferior
        content.moveTo(colHerr - 5, headerBottom);
        content.lineTo(tableRight + 5, headerBottom);
        content.stroke();
        
        // Líneas verticales
        drawVerticalLines(content, y, rowHeight, colHerr, colPrest, colDev, colPend, tableRight);

        content.setFont(PDType1Font.HELVETICA_BOLD, 11);
        // Centrar texto en encabezados numéricos
        writeTextCentered(content, "Herramienta", colHerr, colPrest - 5, y);
        writeTextCentered(content, "Prestado", colPrest, colDev - 5, y);
        writeTextCentered(content, "Devuelto", colDev, colPend - 5, y);
        writeTextCentered(content, "Pendiente", colPend, tableRight, y);
        return y - rowHeight - 2;
    }

    private void drawRowLine(PDPageContentStream content, float y, float tableRight, float rowHeight,
                             float colHerr, float colPrest, float colDev, float colPend) throws Exception {
        content.setLineWidth(0.4f);
        float lineY = y - rowHeight + 2;
        content.moveTo(colHerr - 5, lineY);
        content.lineTo(tableRight + 5, lineY);
        content.stroke();
        drawVerticalLines(content, y, rowHeight, colHerr, colPrest, colDev, colPend, tableRight);
    }

    private void drawVerticalLines(PDPageContentStream content, float y, float rowHeight,
                                   float colHerr, float colPrest, float colDev, float colPend, float tableRight) throws Exception {
        float top = y + 6;
        float bottom = y - rowHeight + 2;
        float left = colHerr - 5;
        float right = tableRight + 5;
        content.setLineWidth(0.4f);
        // Línea izquierda
        content.moveTo(left, top);
        content.lineTo(left, bottom);
        content.stroke();
        // Línea entre Herramienta y Prestado
        content.moveTo(colPrest - 5, top);
        content.lineTo(colPrest - 5, bottom);
        content.stroke();
        // Línea entre Prestado y Devuelto
        content.moveTo(colDev - 5, top);
        content.lineTo(colDev - 5, bottom);
        content.stroke();
        // Línea entre Devuelto y Pendiente
        content.moveTo(colPend - 5, top);
        content.lineTo(colPend - 5, bottom);
        content.stroke();
        // Línea derecha
        content.moveTo(right, top);
        content.lineTo(right, bottom);
        content.stroke();
    }
    private void aplicarEntregaCompleta() {
        boolean entregarTodo = chkEntregaCompleta.isSelected();
        modelo.setDevolverTodo(entregarTodo);
        tabla.setEnabled(!entregarTodo);
        tabla.repaint();
    }

    private class DevolucionTableModel extends AbstractTableModel {
        
        private final String[] columnas = {"Herramienta", "Categoría", "Prestado", "Pendiente", "Devolver", "Obs", "Observación", "Foto"};
        private List<DetallePrestamo> detalles = new ArrayList<>();
        private List<Integer> devolver = new ArrayList<>();
        private List<Boolean> conObservacion = new ArrayList<>();
        private List<String> observaciones = new ArrayList<>();

        public void setDetalles(List<DetallePrestamo> detalles) {
            this.detalles = detalles != null ? detalles : new ArrayList<>();
            devolver = new ArrayList<>();
            conObservacion = new ArrayList<>();
            observaciones = new ArrayList<>();
            for (int i = 0; i < this.detalles.size(); i++) {
                devolver.add(0);
                conObservacion.add(false);
                observaciones.add("");
            }
            fireTableDataChanged();
        }

        public void setDevolverTodo(boolean entregarTodo) {
            for (int i = 0; i < detalles.size(); i++) {
                devolver.set(i, entregarTodo ? detalles.get(i).getPendiente() : 0);
            }
            if (!detalles.isEmpty()) {
                fireTableRowsUpdated(0, detalles.size() - 1);
            }
        }

        public List<DevolucionItem> getDevoluciones() {
            List<DevolucionItem> items = new ArrayList<>();
            for (int i = 0; i < detalles.size(); i++) {
                int cant = devolver.get(i);
                if (cant > 0) {
                    DevolucionItem item = new DevolucionItem();
                    item.setHerramientaId(detalles.get(i).getHerramientaId());
                    item.setNombreHerramienta(detalles.get(i).getNombreHerramienta());
                    item.setCantidadDevolver(cant);
                    if (conObservacion.get(i)) {
                        item.setObservacion(observaciones.get(i));
                    } else {
                        item.setObservacion("");
                    }
                    items.add(item);
                }
            }
            return items;
        }

        @Override
        public int getRowCount() {
            return detalles.size();
        }

        @Override
        public int getColumnCount() {
            return columnas.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnas[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            DetallePrestamo d = detalles.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return d.getNombreHerramienta();
                case 1:
                    return d.getCategoria();
                case 2:
                    return d.getCantidad();
                case 3:
                    return Math.max(0, d.getPendiente() - devolver.get(rowIndex));
                case 4:
                    return devolver.get(rowIndex);
                case 5:
                    return conObservacion.get(rowIndex);
                case 6:
                    return observaciones.get(rowIndex);
                default:
                    return "";
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            if (columnIndex == 4) {
                return true;
            }
            if (columnIndex == 5) {
                return devolver.get(rowIndex) > 0;
            }
            if (columnIndex == 6) {
                return conObservacion.get(rowIndex) && devolver.get(rowIndex) > 0;
            }
            return false;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == 4) {
                int valor = 0;
                if (aValue instanceof Number) {
                    valor = ((Number) aValue).intValue();
                } else if (aValue != null) {
                    try {
                        valor = Integer.parseInt(aValue.toString());
                    } catch (NumberFormatException ignored) {
                        valor = 0;
                    }
                }
                int max = detalles.get(rowIndex).getPendiente();
                valor = Math.max(0, Math.min(valor, max));
                devolver.set(rowIndex, valor);
                if (valor == 0) {
                    conObservacion.set(rowIndex, false);
                    observaciones.set(rowIndex, "");
                    fireTableCellUpdated(rowIndex, 5);
                    fireTableCellUpdated(rowIndex, 6);
                }
                fireTableCellUpdated(rowIndex, columnIndex);
                fireTableCellUpdated(rowIndex, 3);
            } else if (columnIndex == 5) {
                boolean checked = aValue instanceof Boolean ? (Boolean) aValue : false;
                conObservacion.set(rowIndex, checked);
                if (!checked) {
                    observaciones.set(rowIndex, "");
                }
                fireTableCellUpdated(rowIndex, 6);
            } else if (columnIndex == 6) {
                observaciones.set(rowIndex, aValue != null ? aValue.toString() : "");
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex >= 2 && columnIndex <= 4) {
                return Integer.class;
            }
            if (columnIndex == 5) {
                return Boolean.class;
            }
            return String.class;
        }

        public boolean tieneObservacionesIncompletas() {
            for (int i = 0; i < detalles.size(); i++) {
                if (devolver.get(i) > 0 && conObservacion.get(i)) {
                    String obs = observaciones.get(i);
                    if (obs == null || obs.trim().isEmpty()) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private class SpinnerRenderer extends JSpinner implements javax.swing.table.TableCellRenderer {
        public SpinnerRenderer() {
            setModel(new SpinnerNumberModel(0, 0, 9999, 1));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            int actual = value instanceof Number ? ((Number) value).intValue() : 0;
            setValue(actual);
            return this;
        }
    }

    private class SpinnerEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor {
        private final JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, 9999, 1));
        private int editingRow = -1;

        public SpinnerEditor() {
            spinner.addChangeListener(e -> {
                if (editingRow >= 0) {
                    modelo.setValueAt(spinner.getValue(), editingRow, 4);
                }
            });
        }

        @Override
        public Object getCellEditorValue() {
            return spinner.getValue();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            int max = modelo.detalles.get(row).getPendiente();
            spinner.setModel(new SpinnerNumberModel(0, 0, max, 1));
            spinner.setValue(value instanceof Number ? value : 0);
            editingRow = row;
            return spinner;
        }
    }
}

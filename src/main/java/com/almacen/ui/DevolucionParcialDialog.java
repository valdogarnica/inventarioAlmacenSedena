package com.almacen.ui;

import com.almacen.database.DatabaseManager;
import com.almacen.model.DevolucionItem;
import com.almacen.model.DetallePrestamo;
import com.almacen.model.Prestamo;
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
    /** Material de no retorno del préstamo: no se devuelve, solo aparece en el comprobante. */
    private List<DetallePrestamo> noRetorno = new ArrayList<>();
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
            List<DetallePrestamo> detalles = new ArrayList<>();
            noRetorno = new ArrayList<>();
            for (DetallePrestamo d : DetallePrestamo.agrupar(dbManager.obtenerDetallesPrestamo(prestamoId))) {
                (d.isNoRetorno() ? noRetorno : detalles).add(d);
            }
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
            // Con los datos de antes de recargar: "devuelto antes" y "devuelto ahora" quedan bien
            List<DetallePrestamo> detalles = new ArrayList<>(modelo.detalles);
            List<Integer> devuelto = new ArrayList<>(modelo.devolver);
            List<String> obs = modelo.observacionesComprobante();
            if (Alerta.confirmar(this, "La devolución quedó registrada. ¿Desea abrir el comprobante en PDF?",
                    "Devolución registrada", Alerta.Tipo.EXITO, "Abrir comprobante", "Ahora no")) {
                abrirComprobante(detalles, devuelto, obs, false);
            }
            cargarDatos();
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this,
                "Error al registrar devolución: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Vista previa del comprobante con las cantidades capturadas, antes de registrar. */
    private void generarComprobante() {
        boolean hayDevolucion = false;
        for (int cantidad : modelo.devolver) {
            hayDevolucion |= cantidad > 0;
        }
        if (!hayDevolucion) {
            Notificaciones.showMessageDialog(this,
                "No hay cantidades para devolver",
                "Información", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        abrirComprobante(modelo.detalles, modelo.devolver, modelo.observacionesComprobante(), true);
    }

    private void abrirComprobante(List<DetallePrestamo> detalles, List<Integer> devuelto,
                                  List<String> observaciones, boolean vistaPrevia) {
        List<DetallePrestamo> todos = new ArrayList<>(detalles);
        todos.addAll(noRetorno);
        PdfViewer.generarYAbrir(this,
            () -> com.almacen.report.ReportesPdf.comprobanteDevolucion(prestamo, todos, devuelto, observaciones, vistaPrevia),
            "No se pudo generar el comprobante");
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
                // Una fila puede juntar el mismo material de varios proveedores: lo devuelto
                // se reparte entre sus partidas según lo que cada una tiene pendiente
                for (DetallePrestamo parte : detalles.get(i).getPartes()) {
                    int tomar = Math.min(cant, parte.getPendiente());
                    if (tomar <= 0) {
                        continue;
                    }
                    cant -= tomar;
                    DevolucionItem item = new DevolucionItem();
                    item.setHerramientaId(parte.getHerramientaId());
                    item.setNombreHerramienta(parte.getNombreHerramienta());
                    item.setCantidadDevolver(tomar);
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

        /** Observación de cada fila (vacía si no se marcó). */
        public List<String> observacionesComprobante() {
            List<String> lista = new ArrayList<>();
            for (int i = 0; i < detalles.size(); i++) {
                lista.add(conObservacion.get(i) && devolver.get(i) > 0 ? observaciones.get(i) : "");
            }
            return lista;
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
            return Tema.alDia(spinner);
        }
    }
}

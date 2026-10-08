package com.almacen.ui;

import com.almacen.database.DatabaseManager;
import com.almacen.model.ItemCarrito;
import com.almacen.model.Prestamo;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ConfirmarPrestamoDialog extends JDialog {
    private JTextField txtNombreCliente;
    private JTextField txtResidenteSobrestante;
    private JToggleButton toggleAutorizacion;
    private JTextField txtFolio;
    private JPanel panelFolio;
    private JComboBox<Webcam> comboCamaras;
    private Webcam webcam;
    private WebcamPanel webcamPanel;
    private JPanel cameraContainer;
    private JLabel lblFotoCapturada;
    private String fotoNombre;
    private BufferedImage fotoCapturada;
    private JTable tablaResumen;
    private DefaultTableModel modeloResumen;
    private JButton btnConfirmar;
    private JButton btnCancelar;
    private final List<ItemCarrito> items;
    private final String nombreEmpleado;
    private boolean prestamoConfirmado = false;
    
    public ConfirmarPrestamoDialog(JFrame parent, List<ItemCarrito> items, String nombreEmpleado) {
        super(parent, "Confirmar Préstamo", true);
        this.items = items;
        this.nombreEmpleado = nombreEmpleado;
        initComponents();
    }
    
    private void initComponents() {
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cerrarCamara();
            }
        });
        
        // Panel superior - Información del cliente
        JPanel panelCliente = new JPanel(new GridBagLayout());
        panelCliente.setOpaque(false);
        GridBagConstraints gbcCliente = new GridBagConstraints();
        gbcCliente.insets = new Insets(6, 6, 6, 6);
        gbcCliente.anchor = GridBagConstraints.WEST;

        gbcCliente.gridx = 0;
        gbcCliente.gridy = 0;
        JLabel lblCliente = new JLabel("Nombre del Cliente:");
        panelCliente.add(lblCliente, gbcCliente);

        gbcCliente.gridx = 1;
        gbcCliente.fill = GridBagConstraints.HORIZONTAL;
        gbcCliente.weightx = 1.0;
        txtNombreCliente = new JTextField(26);
        panelCliente.add(txtNombreCliente, gbcCliente);

        gbcCliente.gridx = 0;
        gbcCliente.gridy = 1;
        gbcCliente.fill = GridBagConstraints.NONE;
        gbcCliente.weightx = 0;
        JLabel lblResidente = new JLabel("Residente o Sobrestante:");
        panelCliente.add(lblResidente, gbcCliente);

        gbcCliente.gridx = 1;
        gbcCliente.fill = GridBagConstraints.HORIZONTAL;
        gbcCliente.weightx = 1.0;
        txtResidenteSobrestante = new JTextField(26);
        panelCliente.add(txtResidenteSobrestante, gbcCliente);

        gbcCliente.gridx = 0;
        gbcCliente.gridy = 2;
        gbcCliente.fill = GridBagConstraints.NONE;
        gbcCliente.weightx = 0;
        JLabel lblAut = new JLabel("Autorización:");
        panelCliente.add(lblAut, gbcCliente);

        gbcCliente.gridx = 1;
        gbcCliente.fill = GridBagConstraints.NONE;
        toggleAutorizacion = new JToggleButton("No");
        toggleAutorizacion.addActionListener(e -> actualizarFolioVisible());
        UIStyles.styleToggleButton(toggleAutorizacion);
        panelCliente.add(toggleAutorizacion, gbcCliente);

        panelFolio = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        panelFolio.setOpaque(false);
        JLabel lblFolio = new JLabel("Folio:");
        panelFolio.add(lblFolio);
        txtFolio = new JTextField(18);
        panelFolio.add(txtFolio);
        panelFolio.setVisible(false);

        gbcCliente.gridx = 1;
        gbcCliente.gridy = 3;
        gbcCliente.fill = GridBagConstraints.HORIZONTAL;
        panelCliente.add(panelFolio, gbcCliente);

        add(UIStyles.createCard("Cliente", panelCliente), BorderLayout.NORTH);
        
        // Panel central - Resumen y cámara
        JPanel panelCentro = new JPanel(new GridLayout(1, 2, 10, 10));
        panelCentro.setOpaque(false);

        JPanel panelResumen = new JPanel(new BorderLayout());
        panelResumen.setOpaque(false);
        
        String[] columnas = {"Nombre", "Categoría", "Cantidad", "Devolución"};
        modeloResumen = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaResumen = new JTable(modeloResumen);
        tablaResumen.getTableHeader().setReorderingAllowed(false);
        tablaResumen.setRowHeight(32);
        // Estilizar encabezado y centrar números
        UIStyles.styleTableHeader(tablaResumen);
        DefaultTableCellRenderer centerRenderer = UIStyles.createCenteredNumberRenderer();
        tablaResumen.getColumnModel().getColumn(2).setCellRenderer(centerRenderer); // Cantidad
        
        java.util.Set<String> noRetorno = new java.util.HashSet<>();
        try {
            noRetorno = DatabaseManager.getInstance().obtenerTiposNoRetorno();
        } catch (Exception ignored) {
            // Sin la lista se muestran todos como de retorno; al guardar se usa la base
        }
        for (ItemCarrito item : items) {
            String tipo = item.getHerramienta().getTipo();
            boolean sinRetorno = tipo != null && noRetorno.contains(tipo.trim().toLowerCase());
            modeloResumen.addRow(new Object[]{
                item.getNombre(),
                item.getCategoria(),
                item.getCantidad(),
                sinRetorno ? "No retorno" : "Se devuelve"
            });
        }
        tablaResumen.getColumnModel().getColumn(3).setCellRenderer(new InsigniaRenderer() {
            @Override
            protected Tono tono(JTable table, Object value, int row) {
                return "No retorno".equals(value) ? Tono.AVISO : Tono.INFO;
            }
        });
        
        JScrollPane scrollResumen = new JScrollPane(tablaResumen);
        panelResumen.add(scrollResumen, BorderLayout.CENTER);
        
        // Información del empleado
        JPanel panelInfo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelInfo.setOpaque(false);
        JLabel lblEmpleado = new JLabel("Empleado: " + nombreEmpleado);
        panelInfo.add(lblEmpleado);
        JLabel lblFecha = new JLabel("Fecha: " + LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        panelInfo.add(lblFecha);
        panelResumen.add(panelInfo, BorderLayout.SOUTH);
        
        panelCentro.add(UIStyles.createCard("Resumen del prestamo", panelResumen));

        JPanel panelCamara = new JPanel(new BorderLayout(8, 8));
        panelCamara.setOpaque(false);

        JPanel panelSelector = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelSelector.setOpaque(false);
        JLabel lblCamara = new JLabel("Cámara:");
        panelSelector.add(lblCamara);
        comboCamaras = new JComboBox<>();
        panelSelector.add(comboCamaras);
        JButton btnIniciar = new JButton("Iniciar cámara");
        btnIniciar.addActionListener(e -> iniciarCamara());
        UIStyles.stylePrimaryButton(btnIniciar);
        panelSelector.add(btnIniciar);
        panelCamara.add(panelSelector, BorderLayout.NORTH);

        cameraContainer = new JPanel(new CardLayout());
        cameraContainer.setOpaque(false);
        JLabel lblSinCamara = new JLabel("Inicie la cámara para ver el video", SwingConstants.CENTER);
        lblFotoCapturada = new JLabel("Sin foto", SwingConstants.CENTER);
        cameraContainer.add(lblSinCamara, "preview");
        cameraContainer.add(lblFotoCapturada, "foto");
        panelCamara.add(cameraContainer, BorderLayout.CENTER);

        JPanel panelFotoBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelFotoBotones.setOpaque(false);
        JButton btnCapturar = new JButton("Capturar foto");
        btnCapturar.addActionListener(e -> capturarFoto());
        UIStyles.stylePrimaryButton(btnCapturar);
        panelFotoBotones.add(btnCapturar);
        panelCamara.add(panelFotoBotones, BorderLayout.SOUTH);

        panelCentro.add(UIStyles.createCard("Foto del préstamo", panelCamara));

        add(panelCentro, BorderLayout.CENTER);
        
        // Botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelBotones.setOpaque(false);
        btnConfirmar = new JButton("Confirmar Préstamo");
        btnConfirmar.addActionListener(e -> confirmarPrestamo());
        btnConfirmar.setFont(btnConfirmar.getFont().deriveFont(Font.BOLD));
        btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());
        UIStyles.stylePrimaryButton(btnConfirmar);
        UIStyles.styleSecondaryButton(btnCancelar);
        panelBotones.add(btnCancelar);
        panelBotones.add(btnConfirmar);
        add(panelBotones, BorderLayout.SOUTH);

        cargarCamaras();
        actualizarFolioVisible();
    }
    
    private void confirmarPrestamo() {
        String nombreCliente = txtNombreCliente.getText().trim();
        if (nombreCliente.isEmpty()) {
            Notificaciones.showMessageDialog(this, 
                "Por favor ingrese el nombre del cliente", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String residenteSobrestante = txtResidenteSobrestante.getText().trim();
        boolean autorizacion = toggleAutorizacion.isSelected();
        String folio = txtFolio.getText().trim();
        if (autorizacion && folio.isEmpty()) {
            Notificaciones.showMessageDialog(this,
                "Por favor ingrese el folio de autorización",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            if (!dbManager.isConnected()) {
                Notificaciones.showMessageDialog(this, 
                    "No hay conexión a la base de datos", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Verificar stocks antes de confirmar
            for (ItemCarrito item : items) {
                // El stock es la suma de todos los proveedores del material
                if (dbManager.obtenerStockMaterial(item.getNombre(), item.getUnidad()) < item.getCantidad()) {
                    Notificaciones.showMessageDialog(this, 
                        "No hay suficiente stock para: " + item.getNombre(), 
                        "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            
            LocalDateTime ahora = LocalDateTime.now();

            // Guardar foto al confirmar (si se capturó)
            if (fotoCapturada != null) {
                String rutaFotos = AppPreferences.getPhotoFolderPath();
                if (rutaFotos.isEmpty()) {
                    Notificaciones.showMessageDialog(this,
                        "Configure la carpeta de fotos en la aplicación",
                        "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String nombreArchivo = generarNombreFoto(nombreCliente, ahora);
                File carpeta = new File(rutaFotos);
                if (!carpeta.exists()) {
                    carpeta.mkdirs();
                }
                File archivo = new File(carpeta, nombreArchivo);
                ImageIO.write(fotoCapturada, "jpg", archivo);
                fotoNombre = nombreArchivo;
            } else {
                fotoNombre = null;
            }

            Prestamo prestamo = new Prestamo();
            prestamo.setNombreCliente(nombreCliente);
            prestamo.setNombreEmpleado(nombreEmpleado);
            prestamo.setFechaPrestamo(ahora);
            prestamo.setEstado("PRESTADO");
            prestamo.setResidenteSobrestante(residenteSobrestante);
            prestamo.setAutorizacion(autorizacion);
            prestamo.setFolio(autorizacion ? folio : null);
            prestamo.setFotoNombre(fotoNombre);

            dbManager.crearPrestamoConDetalles(prestamo, items);
            
            Notificaciones.showMessageDialog(this,
                "ENTREGADO".equals(prestamo.getEstado())
                    ? "Material entregado. Es de no retorno, así que el préstamo queda cerrado."
                    : "Préstamo realizado exitosamente",
                "Éxito", JOptionPane.INFORMATION_MESSAGE);
            
            prestamoConfirmado = true;
            cerrarCamara();
            dispose();
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this, 
                "Error al realizar el préstamo: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizarFolioVisible() {
        boolean activo = toggleAutorizacion.isSelected();
        toggleAutorizacion.setText(activo ? "Sí" : "No");
        panelFolio.setVisible(activo);
        revalidate();
        repaint();
    }

    private void cargarCamaras() {
        try {
            List<Webcam> webcams = Webcam.getWebcams();
            comboCamaras.removeAllItems();
            for (Webcam cam : webcams) {
                comboCamaras.addItem(cam);
            }
            if (comboCamaras.getItemCount() > 0) {
                comboCamaras.setSelectedIndex(0);
            }
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this,
                "No se pudieron cargar las cámaras: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void iniciarCamara() {
        cerrarCamara();
        Webcam seleccionada = (Webcam) comboCamaras.getSelectedItem();
        if (seleccionada == null) {
            Notificaciones.showMessageDialog(this,
                "No hay cámaras disponibles",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        webcam = seleccionada;
        configurarResolucion(webcam);
        webcamPanel = new WebcamPanel(webcam);
        webcamPanel.setMirrored(true);
        cameraContainer.removeAll();
        cameraContainer.add(webcamPanel, "preview");
        cameraContainer.add(lblFotoCapturada, "foto");
        mostrarPreview();
    }

    private void mostrarPreview() {
        CardLayout cl = (CardLayout) cameraContainer.getLayout();
        cl.show(cameraContainer, "preview");
        revalidate();
        repaint();
    }

    private void mostrarFoto(BufferedImage image) {
        ImageIcon icon = new ImageIcon(image.getScaledInstance(520, 340, Image.SCALE_SMOOTH));
        lblFotoCapturada.setIcon(icon);
        lblFotoCapturada.setText("");
        CardLayout cl = (CardLayout) cameraContainer.getLayout();
        cl.show(cameraContainer, "foto");
        revalidate();
        repaint();
    }

    private void capturarFoto() {
        if (webcam == null || !webcam.isOpen()) {
            Notificaciones.showMessageDialog(this,
                "Primero inicie la cámara",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            BufferedImage image = webcam.getImage();
            if (image == null) {
                Notificaciones.showMessageDialog(this,
                    "No se pudo capturar la imagen",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            fotoCapturada = image;
            mostrarFoto(image);
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this,
                "Error al capturar foto: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String generarNombreFoto(String cliente, LocalDateTime fecha) {
        String limpio = cliente.replaceAll("[^A-Za-z0-9_-]", "_");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        return limpio + "_" + fecha.format(fmt) + ".jpg";
    }

    private void cerrarCamara() {
        if (webcamPanel != null) {
            webcamPanel.stop();
        }
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
    }

    private void configurarResolucion(Webcam cam) {
        try {
            Dimension[] sizes = cam.getViewSizes();
            if (sizes != null && sizes.length > 0) {
                Dimension max = sizes[0];
                for (Dimension size : sizes) {
                    if (size.width * size.height > max.width * max.height) {
                        max = size;
                    }
                }
                cam.setViewSize(max);
            }
        } catch (Exception e) {
            // Mantener resolución por defecto si falla
        }
    }
    
    public boolean isPrestamoConfirmado() {
        return prestamoConfirmado;
    }
}

package com.almacen.ui;

import com.almacen.database.DatabaseManager;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfiguracionDBDialog extends JDialog {
    private JTextField txtRutaCarpeta;
    private JTextField txtRutaFotos;
    private JComboBox<String> comboBasesDatos;
    private JButton btnSeleccionarCarpeta;
    private JButton btnSeleccionarFotos;
    private JButton btnConectar;
    private JButton btnCrearBaseDatos;
    private JTextField txtNombreNuevaBD;
    private boolean conectado = false;
    
    public ConfiguracionDBDialog(JFrame parent) {
        super(parent, "Configuración de Base de Datos", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initComponents();
        cargarRutaGuardada();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setSize(600, 400);
        setLocationRelativeTo(null);
        getContentPane().setBackground(UIStyles.BG);
        
        // Panel principal
        JPanel panelPrincipal = new JPanel(new GridBagLayout());
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panelPrincipal.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Ruta de carpeta
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel lblCarpeta = new JLabel("Carpeta de Bases de Datos:");
        lblCarpeta.setForeground(UIStyles.TEXT);
        panelPrincipal.add(lblCarpeta, gbc);
        
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        txtRutaCarpeta = new JTextField(30);
        txtRutaCarpeta.setEditable(false);
        panelPrincipal.add(txtRutaCarpeta, gbc);
        
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        btnSeleccionarCarpeta = new JButton("Seleccionar");
        btnSeleccionarCarpeta.addActionListener(e -> seleccionarCarpeta());
        UIStyles.styleSecondaryButton(btnSeleccionarCarpeta);
        panelPrincipal.add(btnSeleccionarCarpeta, gbc);

        // Carpeta de fotos
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel lblFotos = new JLabel("Carpeta de Fotos:");
        lblFotos.setForeground(UIStyles.TEXT);
        panelPrincipal.add(lblFotos, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        txtRutaFotos = new JTextField(30);
        txtRutaFotos.setEditable(false);
        panelPrincipal.add(txtRutaFotos, gbc);

        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        btnSeleccionarFotos = new JButton("Seleccionar");
        btnSeleccionarFotos.addActionListener(e -> seleccionarCarpetaFotos());
        UIStyles.styleSecondaryButton(btnSeleccionarFotos);
        panelPrincipal.add(btnSeleccionarFotos, gbc);
        
        // ComboBox de bases de datos
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel lblBaseDatos = new JLabel("Base de Datos:");
        lblBaseDatos.setForeground(UIStyles.TEXT);
        panelPrincipal.add(lblBaseDatos, gbc);
        
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        comboBasesDatos = new JComboBox<>();
        comboBasesDatos.setPreferredSize(new Dimension(300, 30));
        ComboBuscable.instalar(comboBasesDatos);
        panelPrincipal.add(comboBasesDatos, gbc);
        
        // Separador
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panelPrincipal.add(new JSeparator(), gbc);
        
        // Crear nueva base de datos
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel lblNueva = new JLabel("Crear Nueva BD:");
        lblNueva.setForeground(UIStyles.TEXT);
        panelPrincipal.add(lblNueva, gbc);
        
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        txtNombreNuevaBD = new JTextField();
        txtNombreNuevaBD.setToolTipText("Ingrese el nombre de la nueva base de datos (sin extensión .db)");
        panelPrincipal.add(txtNombreNuevaBD, gbc);
        
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        btnCrearBaseDatos = new JButton("Crear");
        btnCrearBaseDatos.addActionListener(e -> crearBaseDatos());
        UIStyles.styleSecondaryButton(btnCrearBaseDatos);
        panelPrincipal.add(btnCrearBaseDatos, gbc);
        
        // Botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelBotones.setOpaque(false);
        btnConectar = new JButton("Conectar");
        btnConectar.addActionListener(e -> conectar());
        UIStyles.stylePrimaryButton(btnConectar);
        UIStyles.applySvgIcon(btnConectar, "/icons/enlace.svg", 16);
        panelBotones.add(btnConectar);
        
        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());
        UIStyles.styleDangerButton(btnCancelar);
        UIStyles.applySvgIcon(btnCancelar, "/icons/cancel.svg", 16);
        panelBotones.add(btnCancelar);
        
        JPanel card = UIStyles.createCard("Base de datos", panelPrincipal);
        add(card, BorderLayout.CENTER);
        add(panelBotones, BorderLayout.SOUTH);
    }
    
    private void seleccionarCarpeta() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Seleccionar Carpeta de Bases de Datos");
        
        if (txtRutaCarpeta.getText().isEmpty()) {
            fileChooser.setCurrentDirectory(FileSystemView.getFileSystemView().getHomeDirectory());
        } else {
            File currentDir = new File(txtRutaCarpeta.getText());
            if (currentDir.exists()) {
                fileChooser.setCurrentDirectory(currentDir);
            }
        }
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = fileChooser.getSelectedFile();
            txtRutaCarpeta.setText(selectedFolder.getAbsolutePath());
            cargarBasesDatos(selectedFolder);
            AppPreferences.setDbFolderPath(selectedFolder.getAbsolutePath());
        }
    }

    private void seleccionarCarpetaFotos() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Seleccionar Carpeta de Fotos");

        if (txtRutaFotos.getText().isEmpty()) {
            fileChooser.setCurrentDirectory(FileSystemView.getFileSystemView().getHomeDirectory());
        } else {
            File currentDir = new File(txtRutaFotos.getText());
            if (currentDir.exists()) {
                fileChooser.setCurrentDirectory(currentDir);
            }
        }

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = fileChooser.getSelectedFile();
            txtRutaFotos.setText(selectedFolder.getAbsolutePath());
            AppPreferences.setPhotoFolderPath(selectedFolder.getAbsolutePath());
        }
    }
    
    private void cargarBasesDatos(File carpeta) {
        comboBasesDatos.removeAllItems();
        
        if (carpeta != null && carpeta.exists() && carpeta.isDirectory()) {
            File[] archivos = carpeta.listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".db") || name.toLowerCase().endsWith(".sqlite"));
            
            if (archivos != null) {
                for (File archivo : archivos) {
                    comboBasesDatos.addItem(archivo.getName());
                }
            }
        }
        
        if (comboBasesDatos.getItemCount() == 0) {
            comboBasesDatos.addItem("No hay bases de datos disponibles");
        }
    }
    
    private void crearBaseDatos() {
        String nombreBD = txtNombreNuevaBD.getText().trim();
        String rutaCarpeta = txtRutaCarpeta.getText().trim();
        
        if (nombreBD.isEmpty()) {
            Notificaciones.showMessageDialog(this, 
                "Por favor ingrese un nombre para la base de datos", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (rutaCarpeta.isEmpty()) {
            Notificaciones.showMessageDialog(this, 
                "Por favor seleccione primero la carpeta de bases de datos", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Asegurar que el nombre tenga extensión .db
        if (!nombreBD.toLowerCase().endsWith(".db")) {
            nombreBD += ".db";
        }
        
        try {
            Path rutaCompleta = Paths.get(rutaCarpeta, nombreBD);
            
            // Verificar si ya existe
            if (Files.exists(rutaCompleta)) {
                int respuesta = JOptionPane.showConfirmDialog(this,
                    "La base de datos ya existe. ¿Desea reemplazarla?",
                    "Confirmar", JOptionPane.YES_NO_OPTION);
                if (respuesta != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            
            // Crear la base de datos
            DatabaseManager dbManager = DatabaseManager.getInstance();
            if (dbManager.connect(rutaCompleta.toString())) {
                dbManager.disconnect();
                Notificaciones.showMessageDialog(this, 
                    "Base de datos creada exitosamente", 
                    "Éxito", JOptionPane.INFORMATION_MESSAGE);
                cargarBasesDatos(new File(rutaCarpeta));
                txtNombreNuevaBD.setText("");
            } else {
                Notificaciones.showMessageDialog(this, 
                    "Error al crear la base de datos", 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this, 
                "Error: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void conectar() {
        String rutaCarpeta = txtRutaCarpeta.getText().trim();
        String rutaFotos = txtRutaFotos.getText().trim();
        String nombreBD = (String) comboBasesDatos.getSelectedItem();
        
        if (rutaCarpeta.isEmpty()) {
            Notificaciones.showMessageDialog(this, 
                "Por favor seleccione la carpeta de bases de datos", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (nombreBD == null || nombreBD.equals("No hay bases de datos disponibles")) {
            Notificaciones.showMessageDialog(this, 
                "Por favor seleccione una base de datos válida", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (rutaFotos.isEmpty()) {
            Notificaciones.showMessageDialog(this,
                "Por favor seleccione la carpeta de fotos",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            String rutaCompleta = Paths.get(rutaCarpeta, nombreBD).toString();
            DatabaseManager dbManager = DatabaseManager.getInstance();
            
            if (dbManager.connect(rutaCompleta)) {
                conectado = true;
                AppPreferences.setDbFolderPath(rutaCarpeta);
                AppPreferences.setPhotoFolderPath(rutaFotos);
                Notificaciones.showMessageDialog(this, 
                    "Conexión exitosa a la base de datos", 
                    "Éxito", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } else {
                Notificaciones.showMessageDialog(this, 
                    "Error al conectar con la base de datos", 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this, 
                "Error: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void cargarRutaGuardada() {
        try {
            String rutaGuardada = AppPreferences.getDbFolderPath();
            if (!rutaGuardada.isEmpty()) {
                File carpeta = new File(rutaGuardada);
                if (carpeta.exists() && carpeta.isDirectory()) {
                    txtRutaCarpeta.setText(rutaGuardada);
                    cargarBasesDatos(carpeta);
                }
            }
            String rutaFotos = AppPreferences.getPhotoFolderPath();
            if (!rutaFotos.isEmpty()) {
                File carpetaFotos = new File(rutaFotos);
                if (carpetaFotos.exists() && carpetaFotos.isDirectory()) {
                    txtRutaFotos.setText(rutaFotos);
                }
            }
        } catch (Exception e) {
            // Ignorar errores al cargar preferencias
        }
    }
    
    public boolean isConectado() {
        return conectado;
    }
}

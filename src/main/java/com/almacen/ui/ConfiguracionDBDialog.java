package com.almacen.ui;

import com.almacen.database.DatabaseManager;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Configuración de la base de datos. El usuario elige una sola carpeta donde se guarda
 * toda la información: ahí están las bases de datos y, por cada una, su carpeta de fotos
 * con el mismo nombre (inventario.db → inventarioFotos), que se crea automáticamente.
 */
public class ConfiguracionDBDialog extends JDialog {
    private static final String SIN_BASES = "No hay bases de datos disponibles";

    private JTextField txtRutaCarpeta;
    private JComboBox<String> comboBasesDatos;
    private JLabel lblFotos;
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
        setSize(760, 440);
        setLocationRelativeTo(null);

        JPanel panelPrincipal = new JPanel(new GridBagLayout());
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        panelPrincipal.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel ayuda = new JLabel("<html><div style='width:470px'>Seleccione una carpeta donde se guardará toda la "
            + "información. Ahí se guardan las bases de datos y, por cada una, su carpeta de fotos "
            + "(por ejemplo <b>inventario.db</b> usa <b>inventarioFotos</b>), que se crea automáticamente.</div></html>");
        UIStyles.textoSecundario(ayuda);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        panelPrincipal.add(ayuda, gbc);

        // Carpeta global
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        panelPrincipal.add(etiqueta("Carpeta de información:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        txtRutaCarpeta = new JTextField(30);
        txtRutaCarpeta.setEditable(false);
        panelPrincipal.add(txtRutaCarpeta, gbc);
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JButton btnSeleccionarCarpeta = new JButton("Seleccionar carpeta");
        btnSeleccionarCarpeta.addActionListener(e -> seleccionarCarpeta());
        UIStyles.styleSecondaryButton(btnSeleccionarCarpeta);
        panelPrincipal.add(btnSeleccionarCarpeta, gbc);

        // Base de datos
        gbc.gridx = 0;
        gbc.gridy = 2;
        panelPrincipal.add(etiqueta("Base de datos:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        comboBasesDatos = new JComboBox<>();
        comboBasesDatos.setPreferredSize(new Dimension(300, 30));
        ComboBuscable.instalar(comboBasesDatos);
        comboBasesDatos.addActionListener(e -> actualizarCarpetaFotos());
        panelPrincipal.add(comboBasesDatos, gbc);

        // Carpeta de fotos (calculada)
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panelPrincipal.add(etiqueta("Fotos de esta base:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        lblFotos = new JLabel("-");
        lblFotos.putClientProperty("FlatLaf.style", "foreground: $App.accentSoftText");
        panelPrincipal.add(lblFotos, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 3;
        panelPrincipal.add(new JSeparator(), gbc);

        // Nueva base de datos
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panelPrincipal.add(etiqueta("Crear nueva base:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        txtNombreNuevaBD = new JTextField();
        txtNombreNuevaBD.putClientProperty("JTextField.placeholderText", "Nombre, por ejemplo: inventario");
        txtNombreNuevaBD.setToolTipText("Nombre de la nueva base de datos (sin extensión .db)");
        txtNombreNuevaBD.addActionListener(e -> crearBaseDatos());
        panelPrincipal.add(txtNombreNuevaBD, gbc);
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JButton btnCrearBaseDatos = new JButton("Crear");
        btnCrearBaseDatos.addActionListener(e -> crearBaseDatos());
        UIStyles.styleSecondaryButton(btnCrearBaseDatos);
        UIStyles.applySvgIcon(btnCrearBaseDatos, "/icons/add.svg", 16);
        panelPrincipal.add(btnCrearBaseDatos, gbc);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelBotones.setOpaque(false);
        JButton btnConectar = new JButton("Conectar");
        btnConectar.addActionListener(e -> conectar());
        UIStyles.stylePrimaryButton(btnConectar);
        UIStyles.applySvgIcon(btnConectar, "/icons/enlace.svg", 16);
        panelBotones.add(btnConectar);
        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());
        UIStyles.styleSecondaryButton(btnCancelar);
        UIStyles.applySvgIcon(btnCancelar, "/icons/cancel.svg", 16);
        panelBotones.add(btnCancelar);

        add(UIStyles.createCard("Base de datos", panelPrincipal), BorderLayout.CENTER);
        add(panelBotones, BorderLayout.SOUTH);
    }

    private static JLabel etiqueta(String texto) {
        JLabel lbl = new JLabel(texto);
        return lbl;
    }

    private File carpeta() {
        String ruta = txtRutaCarpeta.getText().trim();
        return ruta.isEmpty() ? null : new File(ruta);
    }

    private String baseSeleccionada() {
        Object v = comboBasesDatos.getSelectedItem();
        return v == null || SIN_BASES.equals(v) ? null : v.toString();
    }

    private void seleccionarCarpeta() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Seleccione la carpeta donde se guardará toda la información");
        File actual = carpeta();
        fileChooser.setCurrentDirectory(actual != null && actual.exists()
            ? actual : FileSystemView.getFileSystemView().getHomeDirectory());
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File seleccionada = fileChooser.getSelectedFile();
            txtRutaCarpeta.setText(seleccionada.getAbsolutePath());
            AppPreferences.setDbFolderPath(seleccionada.getAbsolutePath());
            cargarBasesDatos(seleccionada, null);
        }
    }

    private void cargarBasesDatos(File carpeta, String seleccionar) {
        comboBasesDatos.removeAllItems();
        if (carpeta != null && carpeta.isDirectory()) {
            File[] archivos = carpeta.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".db") || name.toLowerCase().endsWith(".sqlite"));
            if (archivos != null) {
                Arrays.sort(archivos, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                for (File archivo : archivos) {
                    comboBasesDatos.addItem(archivo.getName());
                }
            }
        }
        if (comboBasesDatos.getItemCount() == 0) {
            comboBasesDatos.addItem(SIN_BASES);
        } else if (seleccionar != null) {
            comboBasesDatos.setSelectedItem(seleccionar);
        }
        actualizarCarpetaFotos();
    }

    /** Muestra (y crea) la carpeta de fotos de la base elegida. */
    private void actualizarCarpetaFotos() {
        File carpeta = carpeta();
        String base = baseSeleccionada();
        if (carpeta == null || base == null) {
            lblFotos.setText("-");
            return;
        }
        File fotos = AppPreferences.carpetaFotos(carpeta, base);
        fotos.mkdirs();
        lblFotos.setText(fotos.getAbsolutePath());
        lblFotos.setToolTipText(fotos.getAbsolutePath());
    }

    private void crearBaseDatos() {
        String nombreBD = txtNombreNuevaBD.getText().trim();
        File carpeta = carpeta();
        if (carpeta == null) {
            Notificaciones.showMessageDialog(this,
                "Primero seleccione la carpeta donde se guardará la información",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (nombreBD.isEmpty()) {
            Notificaciones.showMessageDialog(this,
                "Ingrese un nombre para la base de datos",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (nombreBD.matches(".*[\\\\/:*?\"<>|].*")) {
            Notificaciones.showMessageDialog(this,
                "El nombre no puede llevar estos caracteres: \\ / : * ? \" < > |",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!nombreBD.toLowerCase().endsWith(".db")) {
            nombreBD += ".db";
        }
        try {
            Path rutaCompleta = Paths.get(carpeta.getAbsolutePath(), nombreBD);
            if (Files.exists(rutaCompleta)) {
                Notificaciones.showMessageDialog(this,
                    "Ya existe una base de datos con ese nombre; se seleccionó en la lista",
                    "Información", JOptionPane.INFORMATION_MESSAGE);
                cargarBasesDatos(carpeta, nombreBD);
                return;
            }
            DatabaseManager dbManager = DatabaseManager.getInstance();
            if (dbManager.connect(rutaCompleta.toString())) {
                dbManager.disconnect();
                File fotos = AppPreferences.carpetaFotos(carpeta, nombreBD);
                fotos.mkdirs();
                cargarBasesDatos(carpeta, nombreBD);
                txtNombreNuevaBD.setText("");
                Notificaciones.showMessageDialog(this,
                    "Base de datos creada: " + nombreBD + "\nCarpeta de fotos: " + fotos.getName(),
                    "Éxito", JOptionPane.INFORMATION_MESSAGE);
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
        File carpeta = carpeta();
        String nombreBD = baseSeleccionada();
        if (carpeta == null) {
            Notificaciones.showMessageDialog(this,
                "Seleccione la carpeta donde se guardará la información",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (nombreBD == null) {
            Notificaciones.showMessageDialog(this,
                "Seleccione una base de datos o cree una nueva",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            String rutaCompleta = new File(carpeta, nombreBD).getAbsolutePath();
            DatabaseManager dbManager = DatabaseManager.getInstance();
            if (dbManager.connect(rutaCompleta)) {
                conectado = true;
                AppPreferences.usarBaseDatos(carpeta, nombreBD);
                Notificaciones.showMessageDialog(this,
                    "Conexión exitosa a " + nombreBD,
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
                if (carpeta.isDirectory()) {
                    txtRutaCarpeta.setText(rutaGuardada);
                    String ultima = AppPreferences.getUltimaBaseDatos();
                    cargarBasesDatos(carpeta, ultima.isEmpty() ? null : ultima);
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

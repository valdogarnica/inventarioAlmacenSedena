package com.almacen.ui;

import com.almacen.database.DatabaseManager;
import com.almacen.model.Usuario;
import com.almacen.seguridad.Contrasenas;
import com.almacen.seguridad.Sesion;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.File;
import java.util.Arrays;

/**
 * Selección de la base de datos, después del inicio de sesión. El usuario elige una sola
 * carpeta donde se guarda toda la información: ahí están las bases de datos y, por cada
 * una, su carpeta de fotos con el mismo nombre (inventario.db → inventarioFotos).
 * Al conectar se validan las credenciales que quedaron en memoria contra los usuarios de
 * esa base. Aquí no se crean bases (eso lo hace el administrador dentro del sistema),
 * salvo la primera cuando la carpeta no tiene ninguna.
 */
public class ConfiguracionDBDialog extends JDialog {
    private static final String SIN_BASES = "No hay bases de datos disponibles";

    private JTextField txtRutaCarpeta;
    private JComboBox<String> comboBasesDatos;
    private JLabel lblFotos;
    private JTextField txtNombreNuevaBD;
    private JPanel panelPrimeraBase;
    private Usuario usuarioAutenticado;

    public ConfiguracionDBDialog(Window parent) {
        super(parent, "Seleccionar base de datos", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        InicioSesionDialog.cargarIcono(this);
        initComponents();
        cargarRutaGuardada();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setSize(760, 480);
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
        txtRutaCarpeta.putClientProperty(Mayusculas.EXCLUIR, true);
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

        // Primera base: solo cuando la carpeta no tiene ninguna (las demás las crea el admin dentro del sistema)
        panelPrimeraBase = new JPanel(new GridBagLayout());
        panelPrimeraBase.setOpaque(false);
        GridBagConstraints g2 = new GridBagConstraints();
        g2.insets = new Insets(4, 0, 4, 8);
        g2.anchor = GridBagConstraints.WEST;
        g2.gridx = 0;
        g2.gridy = 0;
        g2.gridwidth = 3;
        g2.fill = GridBagConstraints.HORIZONTAL;
        g2.weightx = 1;
        JComponent avisoPrimera = UIStyles.textoAjustable("Esta carpeta no tiene bases de datos. Solo el administrador "
            + "puede crear la primera: inicie sesión con el administrador inicial (usuario " + Contrasenas.ADMIN_INICIAL
            + ", contraseña " + Contrasenas.CONTRASENA_INICIAL + ") y escriba el nombre de la base.");
        panelPrimeraBase.add(avisoPrimera, g2);
        g2.gridy = 1;
        g2.gridwidth = 1;
        g2.fill = GridBagConstraints.NONE;
        g2.weightx = 0;
        panelPrimeraBase.add(etiqueta("Nombre de la base:"), g2);
        g2.gridx = 1;
        g2.fill = GridBagConstraints.HORIZONTAL;
        g2.weightx = 1;
        txtNombreNuevaBD = new JTextField();
        txtNombreNuevaBD.putClientProperty("JTextField.placeholderText", "Nombre, por ejemplo: inventario");
        txtNombreNuevaBD.setToolTipText("Nombre de la nueva base de datos (sin extensión .db)");
        txtNombreNuevaBD.addActionListener(e -> crearPrimeraBase());
        panelPrimeraBase.add(txtNombreNuevaBD, g2);
        g2.gridx = 2;
        g2.fill = GridBagConstraints.NONE;
        g2.weightx = 0;
        JButton btnCrearBaseDatos = new JButton("Crear");
        btnCrearBaseDatos.addActionListener(e -> crearPrimeraBase());
        UIStyles.styleSecondaryButton(btnCrearBaseDatos);
        UIStyles.applySvgIcon(btnCrearBaseDatos, "/icons/add.svg", 16);
        panelPrimeraBase.add(btnCrearBaseDatos, g2);
        panelPrimeraBase.setVisible(false);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panelPrincipal.add(panelPrimeraBase, gbc);
        // Relleno: el formulario queda arriba aunque no se muestre lo de la primera base
        gbc.gridy = 5;
        gbc.weighty = 1.0;
        panelPrincipal.add(Box.createVerticalGlue(), gbc);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelBotones.setOpaque(false);
        JButton btnConectar = new JButton("Conectar");
        btnConectar.addActionListener(e -> conectar());
        UIStyles.stylePrimaryButton(btnConectar);
        UIStyles.applySvgIcon(btnConectar, "/icons/enlace.svg", 16);
        panelBotones.add(btnConectar);
        JButton btnVolver = new JButton("Volver al inicio de sesión");
        btnVolver.addActionListener(e -> dispose());
        UIStyles.styleSecondaryButton(btnVolver);
        UIStyles.applySvgIcon(btnVolver, "/icons/izquierda.svg", 16);
        panelBotones.add(btnVolver, 0);
        getRootPane().setDefaultButton(btnConectar);

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
        boolean sinBases = comboBasesDatos.getItemCount() == 0;
        panelPrimeraBase.setVisible(sinBases && carpeta != null && carpeta.isDirectory());
        if (sinBases) {
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

    /**
     * Crea la primera base de una carpeta vacía. Cada base nueva nace con el administrador
     * inicial, así que solo se permite si se inició sesión con esas credenciales.
     */
    private void crearPrimeraBase() {
        File carpeta = carpeta();
        if (carpeta == null || !carpeta.isDirectory()) {
            Notificaciones.showMessageDialog(this,
                "Primero seleccione la carpeta donde se guardará la información",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (baseSeleccionada() != null) {
            Notificaciones.showMessageDialog(this,
                "Esta carpeta ya tiene bases de datos. Solo el administrador puede crear más desde Configuración.",
                "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!esAdministradorInicial()) {
            Notificaciones.showMessageDialog(this,
                "Solo el administrador puede crear una base de datos. Vuelva al inicio de sesión y entre con el "
                    + "administrador inicial (usuario " + Contrasenas.ADMIN_INICIAL + ").",
                "Acceso denegado", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String nombreBD = AdministracionSistema.nombreArchivoBase(this, txtNombreNuevaBD.getText());
        if (nombreBD == null) {
            return;
        }
        if (DatabaseManager.crearBaseDatos(new File(carpeta, nombreBD).getAbsolutePath())) {
            AppPreferences.carpetaFotos(carpeta, nombreBD).mkdirs();
            txtNombreNuevaBD.setText("");
            cargarBasesDatos(carpeta, nombreBD);
            Notificaciones.exito(this, "Base de datos creada: " + nombreBD + ". Ya puede conectar.");
        } else {
            Notificaciones.showMessageDialog(this, "Error al crear la base de datos",
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static boolean esAdministradorInicial() {
        return Sesion.hayCredenciales()
            && Contrasenas.ADMIN_INICIAL.equalsIgnoreCase(Sesion.getUsuarioPendiente())
            && java.util.Arrays.equals(Contrasenas.CONTRASENA_INICIAL.toCharArray(), Sesion.getContrasenaPendiente());
    }

    /** Conecta con la base elegida y valida contra ella las credenciales que están en memoria. */
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
                "Seleccione una base de datos",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!Sesion.hayCredenciales()) {
            dispose();
            return;
        }
        DatabaseManager dbManager = DatabaseManager.getInstance();
        try {
            String rutaCompleta = new File(carpeta, nombreBD).getAbsolutePath();
            if (!dbManager.connect(rutaCompleta)) {
                Notificaciones.showMessageDialog(this,
                    "Error al conectar con la base de datos",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Usuario usuario = dbManager.autenticar(Sesion.getUsuarioPendiente(), Sesion.getContrasenaPendiente());
            if (usuario == null) {
                dbManager.disconnect();
                Alerta.mostrar(this,
                    "El usuario o la contraseña no son correctos para la base " + nombreBD
                        + ", o el usuario está desactivado. Vuelva a iniciar sesión.",
                    "Credenciales no válidas", Alerta.Tipo.ERROR);
                dispose();
                return;
            }
            AppPreferences.usarBaseDatos(carpeta, nombreBD);
            AppPreferences.setUltimoUsuario(usuario.getUsuario());
            Sesion.iniciar(usuario);
            usuarioAutenticado = usuario;
            dispose();
        } catch (Exception e) {
            dbManager.disconnect();
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

    /** Usuario validado contra la base conectada, o null si se volvió al inicio de sesión. */
    public Usuario getUsuarioAutenticado() {
        return usuarioAutenticado;
    }
}

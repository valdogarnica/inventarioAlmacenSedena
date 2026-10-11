package com.almacen.ui;

import com.almacen.seguridad.Sesion;
import javax.swing.*;
import java.awt.*;

/**
 * Inicio de sesión local. Es lo primero que aparece al abrir la aplicación: las
 * credenciales quedan en memoria y se validan al conectar con la base de datos elegida.
 */
public class InicioSesionDialog extends JDialog {
    private final JTextField txtUsuario = new JTextField(20);
    private final JPasswordField txtContrasena = new JPasswordField(20);
    private boolean aceptado;

    public InicioSesionDialog(Window parent, String usuarioSugerido) {
        super(parent, "Iniciar sesión", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initComponents(usuarioSugerido);
    }

    private void initComponents(String usuarioSugerido) {
        setLayout(new BorderLayout(10, 10));
        cargarIcono(this);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel ayuda = new JLabel("Ingrese su usuario y contraseña. Se validarán al conectar con la base de datos.");
        UIStyles.textoSecundario(ayuda);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        form.add(ayuda, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        form.add(new JLabel("Usuario:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        txtUsuario.putClientProperty(Mayusculas.EXCLUIR, true);
        txtUsuario.putClientProperty("JTextField.placeholderText", "Usuario");
        txtUsuario.setText(usuarioSugerido != null ? usuarioSugerido : "");
        form.add(txtUsuario, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        form.add(new JLabel("Contraseña:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        txtContrasena.putClientProperty(Mayusculas.EXCLUIR, true);
        txtContrasena.putClientProperty("JTextField.placeholderText", "Contraseña");
        txtContrasena.putClientProperty("JTextField.showRevealButton", true);
        form.add(txtContrasena, gbc);

        add(UIStyles.createCard("Iniciar sesión", form), BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.setOpaque(false);
        JButton btnSalir = new JButton("Salir");
        btnSalir.addActionListener(e -> dispose());
        UIStyles.styleSecondaryButton(btnSalir);
        UIStyles.applySvgIcon(btnSalir, "/icons/cancel.svg", 16);
        JButton btnContinuar = new JButton("Continuar");
        btnContinuar.addActionListener(e -> continuar());
        UIStyles.stylePrimaryButton(btnContinuar);
        UIStyles.applySvgIcon(btnContinuar, "/icons/derecha.svg", 16);
        botones.add(btnSalir);
        botones.add(btnContinuar);
        add(botones, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(btnContinuar);
        getRootPane().registerKeyboardAction(e -> dispose(),
            KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        pack();
        setSize(Math.max(getWidth(), 520), getHeight());
        setResizable(false);
        setLocationRelativeTo(getOwner());
        JComponent foco = txtUsuario.getText().isEmpty() ? txtUsuario : txtContrasena;
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                foco.requestFocusInWindow();
            }
        });
    }

    private void continuar() {
        String usuario = txtUsuario.getText().trim();
        char[] contrasena = txtContrasena.getPassword();
        if (usuario.isEmpty() || contrasena.length == 0) {
            Notificaciones.showMessageDialog(this, "Escriba su usuario y su contraseña",
                "Datos incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Sesion.capturarCredenciales(usuario, contrasena);
        txtContrasena.setText("");
        aceptado = true;
        dispose();
    }

    /** true si el usuario escribió sus credenciales; false si eligió salir. */
    public boolean isAceptado() {
        return aceptado;
    }

    static void cargarIcono(Window ventana) {
        try {
            java.net.URL url = InicioSesionDialog.class.getResource("/icono.png");
            if (url != null) {
                ventana.setIconImage(javax.imageio.ImageIO.read(url));
            }
        } catch (Exception ignored) {
            // Sin icono
        }
    }
}

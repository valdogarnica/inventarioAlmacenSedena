package com.almacen.ui;

import com.almacen.database.DatabaseManager;
import com.almacen.model.Usuario;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 * Alta y edición de usuarios: empleados (los registra el administrador) y la cuenta del
 * propio administrador, que puede cambiar su usuario y su contraseña.
 */
public class UsuarioFormDialog extends JDialog {
    private final Usuario usuario;
    private final boolean cuentaAdmin;
    private final JTextField txtNombre = new JTextField(24);
    private final JTextField txtUsuario = new JTextField(24);
    private final JPasswordField txtActual = new JPasswordField(24);
    private final JPasswordField txtContrasena = new JPasswordField(24);
    private final JPasswordField txtConfirmar = new JPasswordField(24);
    private final JCheckBox chkActivo = new JCheckBox("Puede iniciar sesión (activo)", true);
    private Usuario guardado;

    /** Empleado nuevo (usuario == null) o existente. */
    public static UsuarioFormDialog empleado(Window parent, Usuario usuario) {
        return new UsuarioFormDialog(parent, usuario, false);
    }

    /** Cuenta del administrador en sesión. */
    public static UsuarioFormDialog cuentaAdministrador(Window parent, Usuario admin) {
        return new UsuarioFormDialog(parent, admin, true);
    }

    private UsuarioFormDialog(Window parent, Usuario usuario, boolean cuentaAdmin) {
        super(parent, cuentaAdmin ? "Cuenta de administrador"
            : usuario == null ? "Nuevo empleado" : "Editar empleado", ModalityType.APPLICATION_MODAL);
        this.usuario = usuario;
        this.cuentaAdmin = cuentaAdmin;
        initComponents();
    }

    /** Usuario guardado, o null si se canceló. */
    public Usuario getGuardado() {
        return guardado;
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        txtUsuario.putClientProperty(Mayusculas.EXCLUIR, true);
        for (JPasswordField campo : new JPasswordField[]{txtActual, txtContrasena, txtConfirmar}) {
            campo.putClientProperty(Mayusculas.EXCLUIR, true);
            campo.putClientProperty("JTextField.showRevealButton", true);
        }
        boolean nuevo = usuario == null;
        int fila = 0;
        agregarCampo(form, gbc, fila++, "Nombre *:", txtNombre);
        agregarCampo(form, gbc, fila++, "Usuario *:", txtUsuario);
        if (cuentaAdmin) {
            agregarCampo(form, gbc, fila++, "Contraseña actual *:", txtActual);
        }
        agregarCampo(form, gbc, fila++, nuevo ? "Contraseña *:" : "Nueva contraseña:", txtContrasena);
        agregarCampo(form, gbc, fila++, nuevo ? "Confirmar *:" : "Confirmar nueva:", txtConfirmar);
        if (!nuevo) {
            txtContrasena.putClientProperty("JTextField.placeholderText", "Déjela vacía para no cambiarla");
        }
        if (!cuentaAdmin && !nuevo) {
            chkActivo.setOpaque(false);
            gbc.gridx = 1;
            gbc.gridy = fila;
            form.add(chkActivo, gbc);
        }
        String titulo = cuentaAdmin ? "Usuario y contraseña del administrador" : "Datos del empleado";
        add(UIStyles.createCard(titulo, form), BorderLayout.CENTER);

        if (usuario != null) {
            txtNombre.setText(usuario.getNombre());
            txtUsuario.setText(usuario.getUsuario());
            chkActivo.setSelected(usuario.isActivo());
        }

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.setOpaque(false);
        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());
        UIStyles.styleSecondaryButton(btnCancelar);
        JButton btnGuardar = new JButton("Guardar");
        btnGuardar.addActionListener(e -> guardar());
        UIStyles.stylePrimaryButton(btnGuardar);
        UIStyles.applySvgIcon(btnGuardar, "/icons/save.svg", 16);
        botones.add(btnCancelar);
        botones.add(btnGuardar);
        add(botones, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(btnGuardar);

        pack();
        setSize(Math.max(getWidth(), 500), getHeight());
        setLocationRelativeTo(getOwner());
    }

    private void agregarCampo(JPanel form, GridBagConstraints gbc, int fila, String etiqueta, JComponent campo) {
        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        form.add(new JLabel(etiqueta), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        form.add(campo, gbc);
    }

    private void guardar() {
        String nombre = txtNombre.getText().trim().replaceAll("\\s+", " ");
        String nombreUsuario = txtUsuario.getText().trim();
        char[] actual = txtActual.getPassword();
        char[] contrasena = txtContrasena.getPassword();
        char[] confirmar = txtConfirmar.getPassword();
        try {
            if (nombre.isEmpty()) {
                aviso("El nombre es obligatorio");
                return;
            }
            String error = AdministracionSistema.validarNombreUsuario(nombreUsuario);
            if (error != null) {
                aviso(error);
                return;
            }
            boolean nuevo = usuario == null;
            if (nuevo || contrasena.length > 0 || confirmar.length > 0) {
                error = AdministracionSistema.validarContrasenaNueva(contrasena, confirmar);
                if (error != null) {
                    aviso(error);
                    return;
                }
            }
            DatabaseManager db = DatabaseManager.getInstance();
            if (cuentaAdmin && !db.verificarContrasena(usuario.getId(), actual)) {
                aviso("La contraseña actual no es correcta");
                return;
            }
            Usuario u = new Usuario(nuevo ? 0 : usuario.getId(), nombreUsuario, nombre,
                nuevo ? Usuario.ROL_EMPLEADO : usuario.getRol(), cuentaAdmin || chkActivo.isSelected());
            if (nuevo) {
                int id = db.agregarEmpleado(u, contrasena);
                if (id < 0) {
                    aviso("Ya existe un usuario con ese nombre de usuario");
                    return;
                }
                u.setId(id);
            } else if (!db.actualizarUsuario(u, contrasena)) {
                aviso("Ya existe otro usuario con ese nombre de usuario");
                return;
            }
            guardado = u;
            dispose();
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(actual, '\0');
            Arrays.fill(contrasena, '\0');
            Arrays.fill(confirmar, '\0');
        }
    }

    private void aviso(String mensaje) {
        Notificaciones.showMessageDialog(this, mensaje, "Revise los datos", JOptionPane.WARNING_MESSAGE);
    }
}

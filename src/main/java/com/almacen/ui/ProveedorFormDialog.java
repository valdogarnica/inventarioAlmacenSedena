package com.almacen.ui;

import com.almacen.database.DatabaseManager;
import com.almacen.model.Proveedor;
import javax.swing.*;
import java.awt.*;

/**
 * Alta y edición de proveedores.
 */
public class ProveedorFormDialog extends JDialog {
    private final Proveedor proveedor;
    private final JTextField txtNombre = new JTextField(24);
    private final JTextField txtContacto = new JTextField(24);
    private final JTextField txtTelefono = new JTextField(24);
    private Proveedor guardado;

    public ProveedorFormDialog(Window parent, Proveedor proveedor) {
        super(parent, proveedor == null ? "Nuevo proveedor" : "Editar proveedor", ModalityType.APPLICATION_MODAL);
        this.proveedor = proveedor;
        initComponents();
    }

    /** Devuelve el proveedor guardado, o null si se canceló. */
    public Proveedor getGuardado() {
        return guardado;
    }

    private void initComponents() {
        setSize(460, 280);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(UIStyles.BG);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        agregarCampo(form, gbc, 0, "Nombre *:", txtNombre);
        agregarCampo(form, gbc, 1, "Contacto:", txtContacto);
        agregarCampo(form, gbc, 2, "Teléfono:", txtTelefono);
        add(UIStyles.createCard("Datos del proveedor", form), BorderLayout.CENTER);

        if (proveedor != null) {
            txtNombre.setText(proveedor.getNombre());
            txtContacto.setText(proveedor.getContacto() != null ? proveedor.getContacto() : "");
            txtTelefono.setText(proveedor.getTelefono() != null ? proveedor.getTelefono() : "");
        }

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.setOpaque(false);
        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());
        UIStyles.styleDangerButton(btnCancelar);
        JButton btnGuardar = new JButton("Guardar");
        btnGuardar.addActionListener(e -> guardar());
        UIStyles.stylePrimaryButton(btnGuardar);
        UIStyles.applySvgIcon(btnGuardar, "/icons/save.svg", 16);
        botones.add(btnCancelar);
        botones.add(btnGuardar);
        add(botones, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(btnGuardar);
    }

    private void agregarCampo(JPanel form, GridBagConstraints gbc, int fila, String etiqueta, JComponent campo) {
        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel lbl = new JLabel(etiqueta);
        lbl.setForeground(UIStyles.TEXT);
        form.add(lbl, gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        form.add(campo, gbc);
    }

    private void guardar() {
        String nombre = txtNombre.getText().trim().replaceAll("\\s+", " ");
        if (nombre.isEmpty()) {
            Notificaciones.showMessageDialog(this, "El nombre del proveedor es obligatorio",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Proveedor p = new Proveedor(proveedor != null ? proveedor.getId() : 0, nombre);
        p.setContacto(txtContacto.getText().trim());
        p.setTelefono(txtTelefono.getText().trim());
        try {
            DatabaseManager db = DatabaseManager.getInstance();
            if (proveedor == null) {
                if (db.agregarProveedor(p) < 0) {
                    Notificaciones.showMessageDialog(this, "Ya existe un proveedor con ese nombre",
                        "Información", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
            } else if (!db.actualizarProveedor(p)) {
                Notificaciones.showMessageDialog(this, "Ya existe otro proveedor con ese nombre",
                    "Información", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            guardado = p;
            dispose();
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this, "Error: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

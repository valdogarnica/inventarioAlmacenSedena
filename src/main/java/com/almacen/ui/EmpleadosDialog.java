package com.almacen.ui;

import com.almacen.database.DatabaseManager;
import com.almacen.model.Usuario;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Empleados que pueden iniciar sesión en la base de datos actual. Solo el administrador
 * los registra, edita y desactiva (un empleado desactivado ya no puede entrar, pero sus
 * préstamos y remisiones se conservan).
 */
public class EmpleadosDialog extends JDialog {
    private DefaultTableModel modelo;
    private JTable tabla;
    private List<Usuario> usuarios = new ArrayList<>();

    public EmpleadosDialog(Window parent) {
        super(parent, "Empleados", ModalityType.APPLICATION_MODAL);
        initComponents();
        cargar();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setSize(760, 480);
        setLocationRelativeTo(getOwner());

        JPanel acciones = new JPanel(new WrapLayout(FlowLayout.RIGHT, 8, 4));
        acciones.setOpaque(false);
        JButton btnNuevo = new JButton("Nuevo empleado");
        btnNuevo.addActionListener(e -> nuevo());
        UIStyles.stylePrimaryButton(btnNuevo);
        UIStyles.applySvgIcon(btnNuevo, "/icons/add.svg", 16);
        JButton btnEditar = new JButton("Editar");
        btnEditar.addActionListener(e -> editar());
        UIStyles.styleSecondaryButton(btnEditar);
        UIStyles.applySvgIcon(btnEditar, "/icons/edit.svg", 16);
        acciones.add(btnNuevo);
        acciones.add(btnEditar);

        modelo = new DefaultTableModel(new String[]{"Nombre", "Usuario", "Rol", "Estado"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tabla = new JTable(modelo);
        tabla.setRowHeight(32);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getTableHeader().setReorderingAllowed(false);
        UIStyles.styleTableHeader(tabla);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(240);
        tabla.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editar();
                }
            }
        });

        JPanel contenido = new JPanel(new BorderLayout(8, 8));
        contenido.setOpaque(false);
        contenido.add(acciones, BorderLayout.NORTH);
        contenido.add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(UIStyles.createCard("Usuarios de esta base de datos", contenido), BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.setOpaque(false);
        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.addActionListener(e -> dispose());
        UIStyles.styleSecondaryButton(btnCerrar);
        botones.add(btnCerrar);
        add(botones, BorderLayout.SOUTH);
    }

    private void cargar() {
        try {
            usuarios = DatabaseManager.getInstance().obtenerUsuarios();
            modelo.setRowCount(0);
            for (Usuario u : usuarios) {
                modelo.addRow(new Object[]{u.getNombre(), u.getUsuario(),
                    u.isAdmin() ? "Administrador" : "Empleado", u.isActivo() ? "Activo" : "Inactivo"});
            }
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this, "Error al cargar los empleados: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void nuevo() {
        UsuarioFormDialog dialog = UsuarioFormDialog.empleado(this, null);
        dialog.setVisible(true);
        if (dialog.getGuardado() != null) {
            cargar();
            Notificaciones.exito(this, "Empleado registrado: " + dialog.getGuardado().getNombre());
        }
    }

    private void editar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            Notificaciones.showMessageDialog(this, "Seleccione un empleado", "Información", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Usuario u = usuarios.get(tabla.convertRowIndexToModel(fila));
        if (u.isAdmin()) {
            Notificaciones.showMessageDialog(this,
                "La cuenta del administrador se cambia en Configuración > Cuenta de administrador.",
                "Información", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        UsuarioFormDialog dialog = UsuarioFormDialog.empleado(this, u);
        dialog.setVisible(true);
        if (dialog.getGuardado() != null) {
            cargar();
            Notificaciones.exito(this, "Empleado actualizado");
        }
    }
}

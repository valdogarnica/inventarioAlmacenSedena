package com.almacen.ui;

import com.almacen.database.DatabaseManager;
import com.almacen.model.Proveedor;
import javax.swing.*;
import java.awt.*;

/**
 * Combo de proveedores con botón para registrar uno nuevo.
 * Puede ofrecer una primera opción "sin proveedor" (id 0) con la etiqueta indicada.
 */
public class ProveedorSelector extends JPanel {
    private final Proveedor ninguno;
    private final boolean permiteNinguno;
    private final JComboBox<Proveedor> combo = new JComboBox<>();

    public ProveedorSelector(boolean permiteNinguno, boolean conBotonNuevo) {
        this(permiteNinguno ? "(Sin proveedor)" : null, conBotonNuevo);
    }

    /**
     * @param etiquetaNinguno texto de la primera opción "sin proveedor" (por ejemplo "(Todos)"),
     *                        o null para no ofrecerla
     */
    public ProveedorSelector(String etiquetaNinguno, boolean conBotonNuevo) {
        super(new BorderLayout(8, 0));
        this.permiteNinguno = etiquetaNinguno != null;
        this.ninguno = new Proveedor(0, etiquetaNinguno != null ? etiquetaNinguno : "");
        setOpaque(false);
        add(combo, BorderLayout.CENTER);
        if (conBotonNuevo) {
            JButton btnNuevo = new JButton("Nuevo");
            btnNuevo.setToolTipText("Registrar proveedor");
            UIStyles.styleSecondaryButton(btnNuevo);
            UIStyles.applySvgIcon(btnNuevo, "/icons/add.svg", 16);
            btnNuevo.addActionListener(e -> nuevo());
            add(btnNuevo, BorderLayout.EAST);
        }
        recargar();
    }

    public JComboBox<Proveedor> getCombo() {
        return combo;
    }

    public void recargar() {
        Proveedor actual = getSeleccion();
        try {
            DatabaseManager db = DatabaseManager.getInstance();
            if (!db.isConnected()) {
                return;
            }
            combo.removeAllItems();
            if (permiteNinguno) {
                combo.addItem(ninguno);
            }
            for (Proveedor p : db.obtenerProveedores()) {
                combo.addItem(p);
            }
            if (actual != null) {
                setSeleccion(actual.getId());
            }
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this,
                "Error al cargar proveedores: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Proveedor elegido, o null si no hay o es "(Sin proveedor)". */
    public Proveedor getSeleccion() {
        Proveedor p = (Proveedor) combo.getSelectedItem();
        return p == null || p.getId() == 0 ? null : p;
    }

    public Integer getSeleccionId() {
        Proveedor p = getSeleccion();
        return p != null ? p.getId() : null;
    }

    public void setSeleccion(Integer proveedorId) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            int id = combo.getItemAt(i).getId();
            if ((proveedorId == null && id == 0) || (proveedorId != null && id == proveedorId)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void nuevo() {
        ProveedorFormDialog dialog = new ProveedorFormDialog(SwingUtilities.getWindowAncestor(this), null);
        dialog.setVisible(true);
        Proveedor nuevo = dialog.getGuardado();
        if (nuevo != null) {
            recargar();
            setSeleccion(nuevo.getId());
        }
    }
}

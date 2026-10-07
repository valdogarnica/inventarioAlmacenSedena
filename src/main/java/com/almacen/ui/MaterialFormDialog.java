package com.almacen.ui;

import com.almacen.database.Catalogo;
import com.almacen.database.DatabaseManager;
import com.almacen.model.Herramienta;
import com.almacen.model.Proveedor;
import javax.swing.*;
import java.awt.*;

/**
 * Alta o edición de un material/herramienta con su categoría, tipo, unidad y proveedor.
 * Un material pertenece a un proveedor: el mismo nombre con otro proveedor es otro registro.
 */
public class MaterialFormDialog extends JDialog {
    private final Integer herramientaId;
    private Herramienta original;
    private final JTextField txtNombre = new JTextField(24);
    private CatalogoSelector selCategoria;
    private CatalogoSelector selTipo;
    private CatalogoSelector selUnidad;
    private ProveedorSelector selProveedor;
    private final JTextField txtRemision = new JTextField(24);
    private final JTextField txtStock = new JTextField(24);
    private final JTextArea txtDescripcion = new JTextArea(4, 24);
    private boolean guardado;

    /** @param herramientaId null para dar de alta un material nuevo */
    public MaterialFormDialog(Window parent, Integer herramientaId) {
        super(parent, herramientaId == null ? "Agregar material / herramienta" : "Editar material / herramienta",
            ModalityType.APPLICATION_MODAL);
        this.herramientaId = herramientaId;
        initComponents();
        if (herramientaId != null) {
            cargar();
        }
    }

    public boolean isGuardado() {
        return guardado;
    }

    private void initComponents() {
        setSize(600, 560);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(UIStyles.BG);

        selCategoria = new CatalogoSelector(Catalogo.CATEGORIAS);
        selTipo = new CatalogoSelector(Catalogo.TIPOS);
        selUnidad = new CatalogoSelector(Catalogo.UNIDADES);
        selProveedor = new ProveedorSelector(true, true);
        if (herramientaId == null) {
            selUnidad.setSeleccion("Pieza");
        }

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        int fila = 0;
        agregarCampo(form, gbc, fila++, "Nombre *:", txtNombre);
        agregarCampo(form, gbc, fila++, "Proveedor:", selProveedor);
        agregarCampo(form, gbc, fila++, "Categoría *:", selCategoria);
        agregarCampo(form, gbc, fila++, "Tipo:", selTipo);
        agregarCampo(form, gbc, fila++, "Unidad:", selUnidad);
        agregarCampo(form, gbc, fila++, herramientaId == null ? "Stock inicial *:" : "Stock *:", txtStock);
        agregarCampo(form, gbc, fila++, "No. remisión:", txtRemision);
        txtDescripcion.setLineWrap(true);
        txtDescripcion.setWrapStyleWord(true);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weighty = 1;
        agregarCampo(form, gbc, fila, "Descripción:", new JScrollPane(txtDescripcion));
        if (herramientaId != null) {
            txtRemision.setEditable(false);
            txtRemision.setToolTipText("Última remisión con la que entró este material");
        }

        add(UIStyles.createCard(herramientaId == null ? "Nuevo material" : "Editar material", form), BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.setOpaque(false);
        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());
        UIStyles.styleDangerButton(btnCancelar);
        UIStyles.applySvgIcon(btnCancelar, "/icons/cancel.svg", 16);
        JButton btnGuardar = new JButton("Guardar");
        btnGuardar.addActionListener(e -> guardar());
        UIStyles.stylePrimaryButton(btnGuardar);
        UIStyles.applySvgIcon(btnGuardar, "/icons/save.svg", 16);
        botones.add(btnCancelar);
        botones.add(btnGuardar);
        add(botones, BorderLayout.SOUTH);
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
        gbc.fill = campo instanceof JScrollPane ? GridBagConstraints.BOTH : GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        form.add(campo, gbc);
    }

    private void cargar() {
        try {
            original = DatabaseManager.getInstance().obtenerHerramientaPorId(herramientaId);
            if (original == null) {
                Notificaciones.showMessageDialog(this, "No se encontró el material",
                    "Error", JOptionPane.ERROR_MESSAGE);
                dispose();
                return;
            }
            txtNombre.setText(original.getNombre());
            selCategoria.setSeleccion(original.getCategoria());
            selTipo.setSeleccion(original.getTipo());
            selUnidad.setSeleccion(original.getUnidad());
            selProveedor.setSeleccion(original.getProveedorId());
            txtStock.setText(String.valueOf(original.getStock()));
            txtRemision.setText(original.getRemision() != null ? original.getRemision() : "");
            txtDescripcion.setText(original.getDescripcion() != null ? original.getDescripcion() : "");
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this, "Error al cargar material: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void guardar() {
        String nombre = txtNombre.getText().trim().replaceAll("\\s+", " ");
        String categoria = selCategoria.getSeleccion();
        String stockStr = txtStock.getText().trim();
        if (nombre.isEmpty() || categoria.isEmpty() || stockStr.isEmpty()) {
            Notificaciones.showMessageDialog(this, "Por favor complete todos los campos obligatorios (*)",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int stock;
        try {
            stock = Integer.parseInt(stockStr);
        } catch (NumberFormatException e) {
            Notificaciones.showMessageDialog(this, "Por favor ingrese un número válido para el stock",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (stock < 0) {
            Notificaciones.showMessageDialog(this, "El stock no puede ser negativo",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            DatabaseManager db = DatabaseManager.getInstance();
            if (!db.isConnected()) {
                Notificaciones.showMessageDialog(this, "No hay conexión a la base de datos",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Proveedor proveedor = selProveedor.getSeleccion();
            Integer proveedorId = proveedor != null ? proveedor.getId() : null;
            Herramienta duplicado = db.buscarMaterialDeProveedor(nombre, proveedorId);
            if (duplicado != null && (herramientaId == null || duplicado.getId() != herramientaId)) {
                Notificaciones.showMessageDialog(this,
                    "Ya existe \"" + duplicado.getNombre() + "\" para " +
                        (proveedor != null ? "el proveedor " + proveedor.getNombre() : "materiales sin proveedor") +
                        " (ID " + duplicado.getId() + ").\nPara sumar cantidades use Registrar ingreso o una remisión.",
                    "Material duplicado", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Herramienta h = new Herramienta();
            h.setNombre(nombre);
            h.setCategoria(categoria);
            h.setTipo(selTipo.getSeleccion());
            h.setUnidad(selUnidad.getSeleccion());
            h.setProveedorId(proveedorId);
            h.setProveedorNombre(proveedor != null ? proveedor.getNombre() : null);
            h.setStock(stock);
            h.setDescripcion(txtDescripcion.getText().trim());
            if (herramientaId == null) {
                h.setEstado(1);
                String remision = txtRemision.getText().trim();
                h.setRemision(remision.isEmpty() ? null : remision);
                db.agregarHerramienta(h);
                Notificaciones.showMessageDialog(this, "Material agregado exitosamente",
                    "Éxito", JOptionPane.INFORMATION_MESSAGE);
                guardado = true;
                limpiar();
            } else {
                h.setId(herramientaId);
                db.actualizarHerramienta(h);
                Notificaciones.showMessageDialog(this, "Material actualizado",
                    "Éxito", JOptionPane.INFORMATION_MESSAGE);
                guardado = true;
                dispose();
            }
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this, "Error: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiar() {
        txtNombre.setText("");
        txtStock.setText("");
        txtRemision.setText("");
        txtDescripcion.setText("");
        txtNombre.requestFocusInWindow();
    }
}

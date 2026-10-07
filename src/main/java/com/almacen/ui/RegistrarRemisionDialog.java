package com.almacen.ui;

import com.almacen.database.Catalogo;
import com.almacen.database.DatabaseManager;
import com.almacen.model.DetalleRemision;
import com.almacen.model.Herramienta;
import com.almacen.model.Proveedor;
import com.almacen.model.Remision;
import com.almacen.report.ReportesPdf;
import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Captura de una remisión de proveedor: encabezado (número, proveedor, obra, envía,
 * fecha) y la lista de materiales con su cantidad y unidad. Al guardar, cada material
 * se suma al stock de ESE proveedor o se crea como material nuevo de ese proveedor.
 */
public class RegistrarRemisionDialog extends JDialog {
    private static final int COL_NUM = 0;
    private static final int COL_MATERIAL = 1;
    private static final int COL_CANTIDAD = 2;
    private static final int COL_UNIDAD = 3;
    private static final int COL_CATEGORIA = 4;
    private static final int COL_TIPO = 5;
    private static final int COL_DESCRIPCION = 6;
    private static final int COL_ESTADO = 7;

    private final String empleado;
    private final JTextField txtNumero = new JTextField(16);
    private final JDateChooser dateFecha = new JDateChooser();
    private ProveedorSelector selProveedor;
    private final JTextField txtObra = new JTextField(16);
    private final JTextField txtEnvia = new JTextField(16);
    private final JTextField txtRecibe = new JTextField(16);
    private final JTextField txtObservaciones = new JTextField(16);
    private JTable tabla;
    private PartidasModel modelo;
    private JComboBox<String> comboMaterialEditor;
    private JLabel lblResumen;
    private Map<String, Herramienta> materialesProveedor = new HashMap<>();
    private boolean guardado;

    public RegistrarRemisionDialog(Window parent, String empleado) {
        super(parent, "Registrar remisión de proveedor", ModalityType.APPLICATION_MODAL);
        this.empleado = empleado;
        initComponents();
        cargarMaterialesProveedor();
    }

    public boolean isGuardado() {
        return guardado;
    }

    private void initComponents() {
        setSize(1180, 720);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(UIStyles.BG);

        // ---------- Encabezado
        selProveedor = new ProveedorSelector(false, true);
        selProveedor.getCombo().addActionListener(e -> cargarMaterialesProveedor());
        dateFecha.setDateFormatString("dd/MM/yyyy");
        dateFecha.setDate(new Date());
        dateFecha.setPreferredSize(new Dimension(150, 30));
        if (empleado != null) {
            txtRecibe.setText(empleado);
        }
        txtNumero.setToolTipText("Número o folio que trae la hoja de remisión (opcional)");

        JPanel encabezado = new JPanel(new GridBagLayout());
        encabezado.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.anchor = GridBagConstraints.WEST;
        agregarCampo(encabezado, gbc, 0, 0, "Proveedor *:", selProveedor);
        agregarCampo(encabezado, gbc, 2, 0, "No. remisión:", txtNumero);
        agregarCampo(encabezado, gbc, 0, 1, "Obra:", txtObra);
        agregarCampo(encabezado, gbc, 2, 1, "Fecha *:", dateFecha);
        agregarCampo(encabezado, gbc, 0, 2, "Envía:", txtEnvia);
        agregarCampo(encabezado, gbc, 2, 2, "Recibe:", txtRecibe);
        gbc.gridwidth = 3;
        agregarCampo(encabezado, gbc, 0, 3, "Observaciones:", txtObservaciones);
        gbc.gridwidth = 1;
        add(UIStyles.createCard("Datos de la remisión", encabezado), BorderLayout.NORTH);

        // ---------- Partidas
        modelo = new PartidasModel();
        tabla = new JTable(modelo);
        tabla.setRowHeight(28);
        tabla.setSurrendersFocusOnKeystroke(true);
        tabla.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        tabla.getTableHeader().setReorderingAllowed(false);
        UIStyles.styleTableHeader(tabla);

        comboMaterialEditor = new JComboBox<>();
        comboMaterialEditor.setEditable(true);
        tabla.getColumnModel().getColumn(COL_MATERIAL).setCellEditor(new DefaultCellEditor(comboMaterialEditor));
        tabla.getColumnModel().getColumn(COL_UNIDAD).setCellEditor(crearEditorCatalogo(Catalogo.UNIDADES));
        tabla.getColumnModel().getColumn(COL_CATEGORIA).setCellEditor(crearEditorCatalogo(Catalogo.CATEGORIAS));
        tabla.getColumnModel().getColumn(COL_TIPO).setCellEditor(crearEditorCatalogo(Catalogo.TIPOS));

        DefaultTableCellRenderer centro = UIStyles.createCenteredNumberRenderer();
        tabla.getColumnModel().getColumn(COL_NUM).setCellRenderer(centro);
        tabla.getColumnModel().getColumn(COL_CANTIDAD).setCellRenderer(centro);
        tabla.getColumnModel().getColumn(COL_ESTADO).setCellRenderer(new EstadoRenderer());
        int[] anchos = {40, 320, 80, 100, 160, 110, 180, 170};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }

        JPanel panelPartidas = new JPanel(new BorderLayout(8, 8));
        panelPartidas.setOpaque(false);
        panelPartidas.add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel accionesFilas = new JPanel(new FlowLayout(FlowLayout.LEFT));
        accionesFilas.setOpaque(false);
        JButton btnAgregarFila = new JButton("Agregar fila");
        btnAgregarFila.addActionListener(e -> {
            detenerEdicion();
            modelo.agregarFila();
            int fila = modelo.getRowCount() - 1;
            tabla.changeSelection(fila, COL_MATERIAL, false, false);
            tabla.editCellAt(fila, COL_MATERIAL);
        });
        UIStyles.styleSuccessButton(btnAgregarFila);
        UIStyles.applySvgIcon(btnAgregarFila, "/icons/add.svg", 16);
        JButton btnQuitarFila = new JButton("Quitar fila");
        btnQuitarFila.addActionListener(e -> {
            detenerEdicion();
            int fila = tabla.getSelectedRow();
            if (fila >= 0) {
                modelo.quitarFila(fila);
            }
        });
        UIStyles.styleSecondaryButton(btnQuitarFila);
        UIStyles.applySvgIcon(btnQuitarFila, "/icons/delete.svg", 16);
        accionesFilas.add(btnAgregarFila);
        accionesFilas.add(btnQuitarFila);
        JLabel ayuda = new JLabel("  Escriba el material o elija uno que ya tenga este proveedor. Las filas vacías se ignoran.");
        ayuda.setForeground(new Color(110, 120, 140));
        accionesFilas.add(ayuda);
        panelPartidas.add(accionesFilas, BorderLayout.SOUTH);
        add(UIStyles.createCard("Materiales recibidos (elementos)", panelPartidas), BorderLayout.CENTER);

        // ---------- Botones
        JPanel sur = new JPanel(new BorderLayout());
        sur.setOpaque(false);
        lblResumen = new JLabel();
        lblResumen.setForeground(UIStyles.TEXT);
        lblResumen.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        sur.add(lblResumen, BorderLayout.WEST);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.setOpaque(false);
        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());
        UIStyles.styleDangerButton(btnCancelar);
        UIStyles.applySvgIcon(btnCancelar, "/icons/cancel.svg", 16);
        JButton btnGuardar = new JButton("Guardar remisión");
        btnGuardar.addActionListener(e -> guardar());
        UIStyles.stylePrimaryButton(btnGuardar);
        UIStyles.applySvgIcon(btnGuardar, "/icons/save.svg", 16);
        botones.add(btnCancelar);
        botones.add(btnGuardar);
        sur.add(botones, BorderLayout.EAST);
        add(sur, BorderLayout.SOUTH);

        for (int i = 0; i < 5; i++) {
            modelo.agregarFila();
        }
        actualizarResumen();
    }

    private void agregarCampo(JPanel panel, GridBagConstraints gbc, int x, int y, String etiqueta, JComponent campo) {
        int ancho = gbc.gridwidth;
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel lbl = new JLabel(etiqueta);
        lbl.setForeground(UIStyles.TEXT);
        panel.add(lbl, gbc);
        gbc.gridx = x + 1;
        gbc.gridwidth = ancho;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(campo, gbc);
    }

    private DefaultCellEditor crearEditorCatalogo(Catalogo catalogo) {
        JComboBox<String> combo = new JComboBox<>();
        combo.setEditable(true);
        try {
            for (String v : DatabaseManager.getInstance().obtenerCatalogo(catalogo)) {
                combo.addItem(v);
            }
        } catch (Exception ignored) {
            // El combo queda editable aunque no se haya podido cargar
        }
        return new DefaultCellEditor(combo);
    }

    private void detenerEdicion() {
        if (tabla.isEditing()) {
            tabla.getCellEditor().stopCellEditing();
        }
    }

    private static String clave(String nombre) {
        return nombre == null ? "" : nombre.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    /** Carga los materiales que ya tiene el proveedor elegido para sugerirlos y detectar si existen. */
    private void cargarMaterialesProveedor() {
        materialesProveedor = new HashMap<>();
        if (comboMaterialEditor == null) {
            return;
        }
        comboMaterialEditor.removeAllItems();
        Proveedor proveedor = selProveedor.getSeleccion();
        if (proveedor != null) {
            try {
                for (Herramienta h : DatabaseManager.getInstance().obtenerMaterialesPorProveedor(proveedor.getId())) {
                    materialesProveedor.put(clave(h.getNombre()), h);
                    comboMaterialEditor.addItem(h.getNombre());
                }
            } catch (Exception e) {
                Notificaciones.showMessageDialog(this, "Error al cargar materiales del proveedor: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        modelo.fireTableDataChanged();
        actualizarResumen();
    }

    private void actualizarResumen() {
        int partidas = 0;
        int total = 0;
        int nuevos = 0;
        for (Partida p : modelo.filas) {
            if (!p.vacia()) {
                partidas++;
                total += Math.max(0, p.cantidad);
                if (!materialesProveedor.containsKey(clave(p.material))) {
                    nuevos++;
                }
            }
        }
        lblResumen.setText("Partidas: " + partidas + "   |   Cantidad total: " + total +
            "   |   Materiales nuevos para este proveedor: " + nuevos);
    }

    private void guardar() {
        detenerEdicion();
        Proveedor proveedor = selProveedor.getSeleccion();
        if (proveedor == null) {
            Notificaciones.showMessageDialog(this, "Seleccione o registre el proveedor de la remisión",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Date fecha = dateFecha.getDate();
        if (fecha == null) {
            Notificaciones.showMessageDialog(this, "Indique la fecha de la remisión",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        List<DetalleRemision> partidas = new ArrayList<>();
        Map<String, Integer> repetidos = new HashMap<>();
        for (int i = 0; i < modelo.filas.size(); i++) {
            Partida p = modelo.filas.get(i);
            if (p.vacia()) {
                continue;
            }
            if (p.material == null || p.material.trim().isEmpty()) {
                Notificaciones.showMessageDialog(this, "La fila " + (i + 1) + " tiene cantidad pero no tiene material",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (p.cantidad <= 0) {
                Notificaciones.showMessageDialog(this, "La fila " + (i + 1) + " (" + p.material.trim() + ") debe tener una cantidad mayor a 0",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String clave = clave(p.material);
            if (repetidos.containsKey(clave)) {
                Notificaciones.showMessageDialog(this, "El material \"" + p.material.trim() + "\" está repetido en las filas " +
                    repetidos.get(clave) + " y " + (i + 1) + ". Sume las cantidades en una sola fila.",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            repetidos.put(clave, i + 1);
            DetalleRemision d = new DetalleRemision();
            d.setNombreMaterial(p.material.trim().replaceAll("\\s+", " "));
            d.setCantidad(p.cantidad);
            d.setUnidad(texto(p.unidad));
            d.setCategoria(texto(p.categoria));
            d.setTipo(texto(p.tipo));
            d.setDescripcion(texto(p.descripcion));
            partidas.add(d);
        }
        if (partidas.isEmpty()) {
            Notificaciones.showMessageDialog(this, "Agregue al menos un material a la remisión",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            DatabaseManager db = DatabaseManager.getInstance();
            String numero = txtNumero.getText().trim();
            if (db.existeRemision(numero, proveedor.getId())) {
                int r = JOptionPane.showConfirmDialog(this,
                    "Ya se registró la remisión \"" + numero + "\" de " + proveedor.getNombre() +
                        ".\n¿Desea registrarla de nuevo? Las cantidades se sumarán otra vez al stock.",
                    "Remisión repetida", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (r != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            Remision remision = new Remision();
            remision.setNumeroRemision(numero.isEmpty() ? null : numero);
            remision.setProveedorId(proveedor.getId());
            remision.setObra(texto(txtObra.getText()));
            remision.setEnvia(texto(txtEnvia.getText()));
            remision.setRecibe(texto(txtRecibe.getText()));
            remision.setObservaciones(texto(txtObservaciones.getText()));
            remision.setFecha(fecha.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            int id = db.registrarRemision(remision, partidas);
            guardado = true;

            int r = JOptionPane.showConfirmDialog(this,
                "Remisión registrada (folio interno " + id + ") con " + partidas.size() + " materiales.\n" +
                    "¿Desea abrir el comprobante en PDF?",
                "Remisión guardada", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (r == JOptionPane.YES_OPTION) {
                PdfViewer.generarYAbrir(getOwner(), () -> ReportesPdf.remision(id), "No se encontró la remisión");
            }
            dispose();
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this, "Error al guardar la remisión: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String texto(String valor) {
        if (valor == null) {
            return null;
        }
        String t = valor.trim();
        return t.isEmpty() ? null : t;
    }

    // ------------------------------------------------------------------ modelo

    private static class Partida {
        String material = "";
        int cantidad;
        String unidad = "Pieza";
        String categoria = "";
        String tipo = "Material";
        String descripcion = "";

        boolean vacia() {
            return (material == null || material.trim().isEmpty()) && cantidad <= 0;
        }
    }

    private class PartidasModel extends AbstractTableModel {
        private final String[] columnas = {"#", "Material / elemento *", "Cantidad *", "Unidad", "Categoría", "Tipo", "Descripción", "Estado"};
        private final List<Partida> filas = new ArrayList<>();

        void agregarFila() {
            Partida nueva = new Partida();
            if (!filas.isEmpty()) {
                Partida anterior = filas.get(filas.size() - 1);
                nueva.categoria = anterior.categoria;
                nueva.tipo = anterior.tipo;
            }
            filas.add(nueva);
            fireTableRowsInserted(filas.size() - 1, filas.size() - 1);
            actualizarResumen();
        }

        void quitarFila(int fila) {
            filas.remove(fila);
            if (filas.isEmpty()) {
                filas.add(new Partida());
            }
            fireTableDataChanged();
            actualizarResumen();
        }

        @Override
        public int getRowCount() {
            return filas.size();
        }

        @Override
        public int getColumnCount() {
            return columnas.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnas[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == COL_CANTIDAD || columnIndex == COL_NUM ? Integer.class : String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex != COL_NUM && columnIndex != COL_ESTADO;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Partida p = filas.get(rowIndex);
            switch (columnIndex) {
                case COL_NUM:
                    return rowIndex + 1;
                case COL_MATERIAL:
                    return p.material;
                case COL_CANTIDAD:
                    return p.cantidad > 0 ? p.cantidad : null;
                case COL_UNIDAD:
                    return p.unidad;
                case COL_CATEGORIA:
                    return p.categoria;
                case COL_TIPO:
                    return p.tipo;
                case COL_DESCRIPCION:
                    return p.descripcion;
                case COL_ESTADO:
                    if (p.material == null || p.material.trim().isEmpty()) {
                        return "";
                    }
                    Herramienta h = materialesProveedor.get(clave(p.material));
                    return h != null
                        ? "Existente (stock " + h.getStock() + " → " + (h.getStock() + Math.max(0, p.cantidad)) + ")"
                        : "Nuevo para este proveedor";
                default:
                    return "";
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            Partida p = filas.get(rowIndex);
            String valor = aValue != null ? aValue.toString() : "";
            switch (columnIndex) {
                case COL_MATERIAL:
                    p.material = valor;
                    Herramienta h = materialesProveedor.get(clave(valor));
                    if (h != null) {
                        // Material ya existente del proveedor: usar sus datos
                        p.material = h.getNombre();
                        if (h.getUnidad() != null && !h.getUnidad().trim().isEmpty()) {
                            p.unidad = h.getUnidad();
                        }
                        p.categoria = h.getCategoria();
                        if (h.getTipo() != null && !h.getTipo().trim().isEmpty()) {
                            p.tipo = h.getTipo();
                        }
                    }
                    break;
                case COL_CANTIDAD:
                    if (aValue instanceof Number) {
                        p.cantidad = ((Number) aValue).intValue();
                    } else {
                        try {
                            p.cantidad = Integer.parseInt(valor.trim());
                        } catch (NumberFormatException e) {
                            p.cantidad = 0;
                        }
                    }
                    break;
                case COL_UNIDAD:
                    p.unidad = valor;
                    break;
                case COL_CATEGORIA:
                    p.categoria = valor;
                    break;
                case COL_TIPO:
                    p.tipo = valor;
                    break;
                case COL_DESCRIPCION:
                    p.descripcion = valor;
                    break;
                default:
                    return;
            }
            fireTableRowsUpdated(rowIndex, rowIndex);
            actualizarResumen();
        }
    }

    private static class EstadoRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String texto = value != null ? value.toString() : "";
            if (!isSelected) {
                if (texto.startsWith("Existente")) {
                    c.setForeground(new Color(30, 120, 60));
                } else if (texto.startsWith("Nuevo")) {
                    c.setForeground(new Color(45, 108, 223));
                } else {
                    c.setForeground(table.getForeground());
                }
            }
            return c;
        }
    }
}

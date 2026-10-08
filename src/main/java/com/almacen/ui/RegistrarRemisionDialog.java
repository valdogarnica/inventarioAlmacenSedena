package com.almacen.ui;

import com.almacen.database.Catalogo;
import com.almacen.database.DatabaseManager;
import com.almacen.model.DetalleRemision;
import com.almacen.model.Herramienta;
import com.almacen.model.Proveedor;
import com.almacen.model.Remision;
import com.almacen.report.ReportesPdf;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private final SelectorFecha dateFecha = new SelectorFecha();
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
    /** Todos los registros activos de cada material (de cualquier proveedor), por nombre. */
    private Map<String, List<Herramienta>> materialesTodos = new HashMap<>();
    private boolean guardado;

    /** Remisión que se consulta o edita; null cuando es una remisión nueva. */
    private Integer remisionId;
    /** false mientras la remisión registrada solo se consulta (hasta marcar "Habilitar edición"). */
    private boolean editable = true;
    private JCheckBox chkEditar;
    private JButton btnGuardar;
    private JButton btnCancelar;
    private final List<JComponent> controlesEdicion = new ArrayList<>();
    /** Cantidades con que se registró la remisión, por material, para calcular el stock al editar. */
    private Map<String, Integer> cantidadOriginal = new HashMap<>();
    private int proveedorOriginal;

    public RegistrarRemisionDialog(Window parent, String empleado) {
        super(parent, "Registrar remisión de proveedor", ModalityType.APPLICATION_MODAL);
        this.empleado = empleado;
        initComponents();
        cargarMaterialesProveedor();
    }

    /**
     * Consulta de una remisión registrada. Con la casilla "Habilitar edición" se pueden
     * cambiar sus datos y sus materiales; al guardar se ajusta el stock.
     */
    public RegistrarRemisionDialog(Window parent, int remisionId) {
        super(parent, "Remisión", ModalityType.APPLICATION_MODAL);
        this.empleado = null;
        this.remisionId = remisionId;
        initComponents();
        cargarRemision();
        setEditable(false);
    }

    public boolean isGuardado() {
        return guardado;
    }

    private void initComponents() {
        setSize(1180, 720);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));

        // ---------- Encabezado
        selProveedor = new ProveedorSelector(false, true);
        selProveedor.getCombo().addActionListener(e -> cargarMaterialesProveedor());
        dateFecha.setDateFormatString("dd/MM/yyyy");
        dateFecha.setDate(new Date());
        dateFecha.setPermiteVacia(false);
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
        tabla.setRowHeight(32);
        tabla.setSurrendersFocusOnKeystroke(true);
        tabla.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        tabla.getTableHeader().setReorderingAllowed(false);
        UIStyles.styleTableHeader(tabla);

        comboMaterialEditor = new JComboBox<>();
        comboMaterialEditor.setEditable(true);
        ComboBuscable.instalar(comboMaterialEditor);
        tabla.getColumnModel().getColumn(COL_MATERIAL).setCellEditor(new DefaultCellEditor(comboMaterialEditor));
        tabla.getColumnModel().getColumn(COL_UNIDAD).setCellEditor(crearEditorCatalogo(Catalogo.UNIDADES));
        tabla.getColumnModel().getColumn(COL_CATEGORIA).setCellEditor(crearEditorCatalogo(Catalogo.CATEGORIAS));
        tabla.getColumnModel().getColumn(COL_TIPO).setCellEditor(crearEditorCatalogo(Catalogo.TIPOS));

        DefaultTableCellRenderer centro = UIStyles.createCenteredNumberRenderer();
        tabla.getColumnModel().getColumn(COL_NUM).setCellRenderer(centro);
        tabla.getColumnModel().getColumn(COL_CANTIDAD).setCellRenderer(centro);
        tabla.getColumnModel().getColumn(COL_ESTADO).setCellRenderer(new EstadoRenderer());
        int[] anchos = {40, 290, 85, 90, 150, 100, 140, 280};
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
        JLabel ayuda = new JLabel("  Elija un material ya registrado (de cualquier proveedor) para que se sume a su total, o escriba uno nuevo. Las filas vacías se ignoran.");
        UIStyles.textoSecundario(ayuda);
        accionesFilas.add(ayuda);
        panelPartidas.add(accionesFilas, BorderLayout.SOUTH);
        add(UIStyles.createCard("Materiales recibidos (elementos)", panelPartidas), BorderLayout.CENTER);

        // ---------- Botones
        JPanel sur = new JPanel(new BorderLayout());
        sur.setOpaque(false);
        lblResumen = new JLabel();
        lblResumen.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        sur.add(lblResumen, BorderLayout.WEST);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.setOpaque(false);
        btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> dispose());
        UIStyles.styleSecondaryButton(btnCancelar);
        UIStyles.applySvgIcon(btnCancelar, "/icons/cancel.svg", 16);
        btnGuardar = new JButton("Guardar remisión");
        btnGuardar.addActionListener(e -> guardar());
        UIStyles.stylePrimaryButton(btnGuardar);
        UIStyles.applySvgIcon(btnGuardar, "/icons/save.svg", 16);
        if (remisionId != null) {
            chkEditar = new JCheckBox("Habilitar edición");
            chkEditar.setOpaque(false);
            chkEditar.setFont(chkEditar.getFont().deriveFont(Font.BOLD));
            chkEditar.setToolTipText("Permite cambiar los datos y los materiales de esta remisión");
            chkEditar.addActionListener(e -> alternarEdicion());
            botones.add(chkEditar);
            JButton btnPdf = new JButton("Comprobante PDF");
            btnPdf.addActionListener(e -> PdfViewer.generarYAbrir(this, () -> ReportesPdf.remision(remisionId),
                "No se encontró la remisión"));
            UIStyles.styleSecondaryButton(btnPdf);
            UIStyles.applySvgIcon(btnPdf, "/icons/report.svg", 16);
            botones.add(btnPdf);
            btnCancelar.setText("Cerrar");
            btnGuardar.setText("Guardar cambios");
        }
        botones.add(btnCancelar);
        botones.add(btnGuardar);
        sur.add(botones, BorderLayout.EAST);
        add(sur, BorderLayout.SOUTH);

        controlesEdicion.add(txtNumero);
        controlesEdicion.add(txtObra);
        controlesEdicion.add(txtEnvia);
        controlesEdicion.add(txtRecibe);
        controlesEdicion.add(txtObservaciones);
        controlesEdicion.add(btnAgregarFila);
        controlesEdicion.add(btnQuitarFila);
        controlesEdicion.add(btnGuardar);

        if (remisionId == null) {
            for (int i = 0; i < 5; i++) {
                modelo.agregarFila();
            }
        }
        actualizarResumen();
    }

    // ------------------------------------------------------------------ consulta / edición

    private void cargarRemision() {
        try {
            DatabaseManager db = DatabaseManager.getInstance();
            Remision r = db.obtenerRemisionPorId(remisionId);
            if (r == null) {
                Notificaciones.showMessageDialog(this, "No se encontró la remisión", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            setTitle("Remisión " + (r.getNumeroRemision() != null ? r.getNumeroRemision() + " " : "")
                + "(folio interno " + r.getId() + ")");
            proveedorOriginal = r.getProveedorId();
            selProveedor.recargar();
            selProveedor.setSeleccion(r.getProveedorId());
            txtNumero.setText(r.getNumeroRemision() != null ? r.getNumeroRemision() : "");
            txtObra.setText(r.getObra() != null ? r.getObra() : "");
            txtEnvia.setText(r.getEnvia() != null ? r.getEnvia() : "");
            txtRecibe.setText(r.getRecibe() != null ? r.getRecibe() : "");
            txtObservaciones.setText(r.getObservaciones() != null ? r.getObservaciones() : "");
            dateFecha.setDate(r.getFecha() != null
                ? Date.from(r.getFecha().atStartOfDay(ZoneId.systemDefault()).toInstant()) : null);
            cantidadOriginal = new HashMap<>();
            modelo.filas.clear();
            for (DetalleRemision d : db.obtenerDetallesRemision(remisionId)) {
                Partida p = new Partida();
                p.material = d.getNombreMaterial();
                p.cantidad = d.getCantidad();
                p.unidad = d.getUnidad() != null ? d.getUnidad() : "";
                p.categoria = d.getCategoria() != null ? d.getCategoria() : "";
                p.tipo = d.getTipo() != null ? d.getTipo() : "";
                p.descripcion = d.getDescripcion() != null ? d.getDescripcion() : "";
                modelo.filas.add(p);
                cantidadOriginal.merge(clave(d.getNombreMaterial()), d.getCantidad(), Integer::sum);
            }
            cargarMaterialesProveedor();
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this, "Error al cargar la remisión: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void alternarEdicion() {
        if (chkEditar.isSelected()) {
            setEditable(true);
            return;
        }
        detenerEdicion();
        // Al desmarcar se descartan los cambios que no se guardaron
        cargarRemision();
        setEditable(false);
    }

    private void setEditable(boolean valor) {
        editable = valor;
        for (JComponent c : controlesEdicion) {
            if (c instanceof JTextField) {
                ((JTextField) c).setEditable(valor);
            } else {
                c.setEnabled(valor);
            }
        }
        dateFecha.setEnabled(valor);
        for (Component c : selProveedor.getComponents()) {
            c.setEnabled(valor);
        }
        if (chkEditar != null && chkEditar.isSelected() != valor) {
            chkEditar.setSelected(valor);
        }
        modelo.fireTableDataChanged();
        actualizarResumen();
    }

    /** Cuánto de este material sumó la remisión al registrarse (solo cuenta si el proveedor no cambió). */
    private int cantidadRegistrada(String material) {
        Proveedor p = selProveedor.getSeleccion();
        if (remisionId == null || p == null || p.getId() != proveedorOriginal) {
            return 0;
        }
        return cantidadOriginal.getOrDefault(clave(material), 0);
    }

    private void agregarCampo(JPanel panel, GridBagConstraints gbc, int x, int y, String etiqueta, JComponent campo) {
        int ancho = gbc.gridwidth;
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JLabel lbl = new JLabel(etiqueta);
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
        ComboBuscable.instalar(combo);
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

    /**
     * Carga los materiales de todos los proveedores para sugerirlos (así el mismo material
     * se escribe igual y se acumula en el total) y detecta cuáles ya tiene el proveedor elegido.
     */
    private void cargarMaterialesProveedor() {
        materialesProveedor = new HashMap<>();
        materialesTodos = new LinkedHashMap<>();
        if (comboMaterialEditor == null) {
            return;
        }
        comboMaterialEditor.removeAllItems();
        Proveedor proveedor = selProveedor.getSeleccion();
        try {
            for (Herramienta h : DatabaseManager.getInstance().obtenerMaterialesActivos()) {
                String k = clave(h.getNombre());
                if (!materialesTodos.containsKey(k)) {
                    comboMaterialEditor.addItem(h.getNombre().trim());
                }
                materialesTodos.computeIfAbsent(k, x -> new ArrayList<>()).add(h);
                if (proveedor != null && h.getProveedorId() != null && h.getProveedorId() == proveedor.getId()) {
                    materialesProveedor.put(k, h);
                }
            }
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this, "Error al cargar materiales: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
        modelo.fireTableDataChanged();
        actualizarResumen();
    }

    /** Stock disponible del material sumando todos sus proveedores. */
    private int stockTotal(String material) {
        int total = 0;
        for (Herramienta h : materialesTodos.getOrDefault(clave(material), new ArrayList<>())) {
            total += h.getStock();
        }
        return total;
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
            if (remisionId == null && db.existeRemision(numero, proveedor.getId())) {
                if (!Alerta.confirmar(this,
                    "Ya se registró la remisión \"" + numero + "\" de " + proveedor.getNombre() +
                        ". ¿Desea registrarla de nuevo? Las cantidades se sumarán otra vez al stock.",
                    "Remisión repetida", Alerta.Tipo.AVISO, "Registrar de nuevo", "Cancelar")) {
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
            if (remisionId != null) {
                remision.setId(remisionId);
                db.actualizarRemision(remision, partidas);
                guardado = true;
                Notificaciones.showMessageDialog(this,
                    "Remisión actualizada. El stock de sus materiales se ajustó a las nuevas cantidades.",
                    "Remisión guardada", JOptionPane.INFORMATION_MESSAGE);
                cargarRemision();
                setEditable(false);
                return;
            }
            int id = db.registrarRemision(remision, partidas);
            guardado = true;

            if (Alerta.confirmar(this,
                "Remisión registrada (folio interno " + id + ") con " + partidas.size() + " materiales. " +
                    "¿Desea abrir el comprobante en PDF?",
                "Remisión guardada", Alerta.Tipo.EXITO, "Abrir PDF", "Ahora no")) {
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
            return editable && columnIndex != COL_NUM && columnIndex != COL_ESTADO;
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
                    if (!editable) {
                        return "Registrado";
                    }
                    Herramienta h = materialesProveedor.get(clave(p.material));
                    // Al editar, el stock actual ya incluye lo que sumó esta remisión
                    int cantidad = Math.max(0, p.cantidad) - cantidadRegistrada(p.material);
                    int totalGeneral = stockTotal(p.material);
                    String total = " · total " + totalGeneral + " → " + (totalGeneral + cantidad);
                    if (h != null) {
                        int despues = h.getStock() + cantidad;
                        return (despues < 0 ? "Sin existencia suficiente (stock " : "Existente (stock ")
                            + h.getStock() + " → " + despues + ")" + total;
                    }
                    return materialesTodos.containsKey(clave(p.material))
                        ? "Nuevo para este proveedor" + total
                        : "Nuevo material";
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
                    if (h == null && materialesTodos.containsKey(clave(valor))) {
                        // Lo surte otro proveedor: se copian sus datos para que se acumule como el mismo material
                        h = materialesTodos.get(clave(valor)).get(0);
                    }
                    if (h != null) {
                        // Material ya existente: usar sus datos
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
        EstadoRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String texto = value != null ? value.toString() : "";
            if (!isSelected) {
                if (texto.startsWith("Existente")) {
                    c.setForeground(Tema.color("App.successText"));
                } else if (texto.startsWith("Nuevo")) {
                    c.setForeground(Tema.color("App.accentSoftText"));
                } else {
                    c.setForeground(table.getForeground());
                }
            }
            return c;
        }
    }
}

package com.almacen.ui;

import com.almacen.database.DatabaseManager;
import com.almacen.model.Herramienta;
import com.almacen.model.ItemCarrito;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MainWindow extends JFrame {
    private JTextField txtBusqueda;
    private JTable tablaHerramientas;
    private ResultadosTableModel modeloTabla;
    private JTable tablaCarrito;
    private DefaultTableModel modeloCarrito;
    private JButton btnQuitarCarrito;
    private JButton btnLimpiarCarrito;
    private JButton btnRealizarPrestamo;
    private JLabel lblEmpleado;
    private List<ItemCarrito> carrito;
    private JButton btnAnterior;
    private JButton btnSiguiente;
    private JLabel lblPagina;
    private JComboBox<Integer> comboTamanoPagina;
    private int paginaActual = 1;
    private int totalRegistros = 0;
    
    public MainWindow() {
        carrito = new ArrayList<>();
        initComponents();
        verificarConexion();
    }
    
    private void initComponents() {
        setTitle("Sistema de Inventario de Almacén");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 700);
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        cargarIconoApp();
        
        // Menú
        JMenuBar menuBar = new JMenuBar();
        JMenu menuArchivo = new JMenu("Archivo");
        JMenuItem menuItemConfig = new JMenuItem("Configurar Base de Datos");
        menuItemConfig.addActionListener(e -> abrirConfiguracion());
        menuArchivo.add(menuItemConfig);
        menuArchivo.addSeparator();
        JMenuItem menuItemSalir = new JMenuItem("Salir");
        menuItemSalir.addActionListener(e -> System.exit(0));
        menuArchivo.add(menuItemSalir);
        
        JMenu menuGestion = new JMenu("Gestión");
        JMenuItem menuItemAgregar = new JMenuItem("Agregar Herramienta");
        menuItemAgregar.addActionListener(e -> agregarHerramienta());
        menuGestion.add(menuItemAgregar);
        JMenuItem menuItemPrestamos = new JMenuItem("Ver Préstamos");
        menuItemPrestamos.addActionListener(e -> verPrestamos());
        menuGestion.add(menuItemPrestamos);
        JMenuItem menuItemInventario = new JMenuItem("Ver Inventario");
        menuItemInventario.addActionListener(e -> verInventario());
        menuGestion.add(menuItemInventario);
        JMenuItem menuItemCategorias = new JMenuItem("Categorías");
        menuItemCategorias.addActionListener(e -> verCategorias());
        menuGestion.add(menuItemCategorias);
        JMenuItem menuItemIngresos = new JMenuItem("Registrar Ingreso de Stock");
        menuItemIngresos.addActionListener(e -> registrarIngreso());
        menuGestion.add(menuItemIngresos);
        
        menuBar.add(menuArchivo);
        menuBar.add(menuGestion);
        setJMenuBar(menuBar);
        
        // Panel principal
        JPanel panelPrincipal = new JPanel(new BorderLayout(16, 16));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panelPrincipal.setBackground(UIStyles.BG);
        
        // Panel superior - Información del empleado
        JPanel panelEmpleado = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelEmpleado.setOpaque(false);
        JLabel lblEmpleadoTitulo = new JLabel("Empleado:");
        lblEmpleadoTitulo.setForeground(UIStyles.TEXT);
        panelEmpleado.add(lblEmpleadoTitulo);
        lblEmpleado = new JLabel("No configurado");
        lblEmpleado.setFont(lblEmpleado.getFont().deriveFont(Font.BOLD));
        lblEmpleado.setForeground(UIStyles.TEXT);
        panelEmpleado.add(lblEmpleado);
        JButton btnCambiarEmpleado = new JButton("Cambiar");
        btnCambiarEmpleado.addActionListener(e -> configurarEmpleado());
        UIStyles.styleSecondaryButton(btnCambiarEmpleado);
        btnCambiarEmpleado.setPreferredSize(new Dimension(120, 34));
        UIStyles.applySvgIcon(btnCambiarEmpleado, "/icons/edit.svg", 16);
        panelEmpleado.add(btnCambiarEmpleado);
        panelPrincipal.add(panelEmpleado, BorderLayout.NORTH);
        
        // Panel central dividido
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setOpaque(false);
        splitPane.setBorder(null);
        
        // Panel izquierdo - Búsqueda y resultados
        JPanel panelIzquierdo = new JPanel(new BorderLayout(12, 12));
        panelIzquierdo.setOpaque(false);
        
        // Búsqueda
        JPanel panelBusqueda = new JPanel(new BorderLayout(10, 10));
        panelBusqueda.setOpaque(false);
        JLabel lblBuscar = new JLabel("Buscar herramienta:");
        lblBuscar.setForeground(UIStyles.TEXT);
        panelBusqueda.add(lblBuscar, BorderLayout.WEST);
        txtBusqueda = new JTextField();
        txtBusqueda.setPreferredSize(new Dimension(320, 32));
        configurarBusquedaEnVivo();
        JButton btnBuscar = new JButton("Buscar");
        btnBuscar.addActionListener(e -> buscarHerramientas());
        UIStyles.stylePrimaryButton(btnBuscar);
        btnBuscar.setPreferredSize(new Dimension(120, 34));
        UIStyles.applySvgIcon(btnBuscar, "/icons/search.svg", 16);
        JPanel panelBuscarBtn = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelBuscarBtn.setOpaque(false);
        panelBuscarBtn.add(txtBusqueda);
        panelBuscarBtn.add(btnBuscar);
        panelBusqueda.add(panelBuscarBtn, BorderLayout.CENTER);
        JPanel cardBusqueda = UIStyles.createCard("Busqueda", panelBusqueda);
        panelIzquierdo.add(cardBusqueda, BorderLayout.NORTH);
        
        // Tabla de herramientas
        modeloTabla = new ResultadosTableModel();
        tablaHerramientas = new JTable(modeloTabla);
        tablaHerramientas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaHerramientas.getTableHeader().setReorderingAllowed(false);
        tablaHerramientas.setRowHeight(28);
        tablaHerramientas.getColumnModel().getColumn(4).setPreferredWidth(90);
        tablaHerramientas.getColumnModel().getColumn(5).setPreferredWidth(110);
        tablaHerramientas.getColumnModel().getColumn(4).setCellEditor(new CantidadSpinnerEditor());
        tablaHerramientas.getColumnModel().getColumn(4).setCellRenderer(new CantidadSpinnerRenderer());
        tablaHerramientas.getColumnModel().getColumn(5).setCellRenderer(new BotonAgregarRenderer());
        tablaHerramientas.getColumnModel().getColumn(5).setCellEditor(new BotonAgregarEditor());
        // Estilizar encabezado y centrar números
        UIStyles.styleTableHeader(tablaHerramientas);
        DefaultTableCellRenderer centerRenderer = UIStyles.createCenteredNumberRenderer();
        tablaHerramientas.getColumnModel().getColumn(0).setCellRenderer(centerRenderer); // ID
        tablaHerramientas.getColumnModel().getColumn(3).setCellRenderer(centerRenderer); // Stock
        JScrollPane scrollHerramientas = new JScrollPane(tablaHerramientas);
        scrollHerramientas.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        JPanel cardResultados = UIStyles.createCard("Resultados", scrollHerramientas);
        
        // Panel de paginación
        JPanel panelPaginacion = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelPaginacion.setOpaque(false);
        JLabel lblTamano = new JLabel("Registros por página:");
        lblTamano.setForeground(UIStyles.TEXT);
        panelPaginacion.add(lblTamano);
        comboTamanoPagina = new JComboBox<>(new Integer[]{50, 100, 200});
        comboTamanoPagina.setSelectedItem(50);
        comboTamanoPagina.addActionListener(e -> cargarPagina(1));
        panelPaginacion.add(comboTamanoPagina);
        btnAnterior = new JButton("Anterior");
        btnAnterior.addActionListener(e -> cargarPagina(paginaActual - 1));
        UIStyles.styleSecondaryButton(btnAnterior);
        btnAnterior.setPreferredSize(new Dimension(120, 34));
        UIStyles.applySvgIcon(btnAnterior, "/icons/menorque.svg", 16);
        panelPaginacion.add(btnAnterior);
        btnSiguiente = new JButton("Siguiente");
        btnSiguiente.addActionListener(e -> cargarPagina(paginaActual + 1));
        UIStyles.styleSecondaryButton(btnSiguiente);
        btnSiguiente.setPreferredSize(new Dimension(120, 34));
        UIStyles.applySvgIcon(btnSiguiente, "/icons/mayorque.svg", 16);
        panelPaginacion.add(btnSiguiente);
        lblPagina = new JLabel("Página 1 de 1");
        lblPagina.setForeground(UIStyles.TEXT);
        panelPaginacion.add(lblPagina);
        
        JPanel panelResultadosCompleto = new JPanel(new BorderLayout());
        panelResultadosCompleto.setOpaque(false);
        panelResultadosCompleto.add(cardResultados, BorderLayout.CENTER);
        panelResultadosCompleto.add(panelPaginacion, BorderLayout.SOUTH);
        panelIzquierdo.add(panelResultadosCompleto, BorderLayout.CENTER);
        
        // Panel derecho - Carrito
        JPanel panelDerecho = new JPanel(new BorderLayout(12, 12));
        panelDerecho.setOpaque(false);
        
        String[] columnasCarrito = {"Nombre", "Categoría", "Cantidad"};
        modeloCarrito = new DefaultTableModel(columnasCarrito, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaCarrito = new JTable(modeloCarrito);
        tablaCarrito.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaCarrito.getTableHeader().setReorderingAllowed(false);
        tablaCarrito.setRowHeight(28);
        // Estilizar encabezado y centrar números
        UIStyles.styleTableHeader(tablaCarrito);
        DefaultTableCellRenderer centerRendererCarrito = UIStyles.createCenteredNumberRenderer();
        tablaCarrito.getColumnModel().getColumn(2).setCellRenderer(centerRendererCarrito); // Cantidad
        JScrollPane scrollCarrito = new JScrollPane(tablaCarrito);
        JPanel cardCarrito = UIStyles.createCard("Herramientas seleccionadas", scrollCarrito);
        
        // Botones del carrito
        JPanel panelBotonesCarrito = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        panelBotonesCarrito.setOpaque(false);
        btnQuitarCarrito = new JButton("Quitar del Carrito");
        btnQuitarCarrito.addActionListener(e -> quitarDelCarrito());
        btnLimpiarCarrito = new JButton("Limpiar Carrito");
        btnLimpiarCarrito.addActionListener(e -> limpiarCarrito());
        btnRealizarPrestamo = new JButton("Realizar Préstamo");
        btnRealizarPrestamo.addActionListener(e -> realizarPrestamo());
        btnRealizarPrestamo.setFont(btnRealizarPrestamo.getFont().deriveFont(Font.BOLD, 14f));
        UIStyles.styleSecondaryButton(btnQuitarCarrito);
        UIStyles.styleDangerButton(btnLimpiarCarrito);
        UIStyles.stylePrimaryButton(btnRealizarPrestamo);
        Dimension btnSize = new Dimension(145, 34);
        btnQuitarCarrito.setPreferredSize(btnSize);
        btnLimpiarCarrito.setPreferredSize(btnSize);
        btnRealizarPrestamo.setPreferredSize(new Dimension(165, 34));
        UIStyles.applySvgIcon(btnQuitarCarrito, "/icons/cancel.svg", 16);
        UIStyles.applySvgIcon(btnLimpiarCarrito, "/icons/delete.svg", 16);
        UIStyles.applySvgIcon(btnRealizarPrestamo, "/icons/save.svg", 16);
        panelBotonesCarrito.add(btnQuitarCarrito);
        panelBotonesCarrito.add(btnLimpiarCarrito);
        panelBotonesCarrito.add(btnRealizarPrestamo);
        cardCarrito.add(panelBotonesCarrito, BorderLayout.SOUTH);
        panelDerecho.add(cardCarrito, BorderLayout.CENTER);
        
        splitPane.setLeftComponent(panelIzquierdo);
        splitPane.setRightComponent(panelDerecho);
        splitPane.setDividerLocation(600);
        splitPane.setResizeWeight(0.5);
        
        panelPrincipal.add(splitPane, BorderLayout.CENTER);
        add(panelPrincipal);
    }

    private void cargarIconoApp() {
        try {
            java.net.URL iconUrl = getClass().getResource("/icono.png");
            if (iconUrl != null) {
                setIconImage(javax.imageio.ImageIO.read(iconUrl));
            }
        } catch (Exception ignored) {
            // Si falla, continuar sin icono
        }
    }
    
    private void verificarConexion() {
        DatabaseManager dbManager = DatabaseManager.getInstance();
        if (!dbManager.isConnected()) {
            abrirConfiguracion();
        }
        if (AppPreferences.getPhotoFolderPath().isEmpty()) {
            abrirConfiguracion();
        }
        configurarEmpleado();
        // Cargar resultados automáticamente al iniciar
        cargarPagina(1);
    }
    
    private void abrirConfiguracion() {
        ConfiguracionDBDialog dialog = new ConfiguracionDBDialog(this);
        dialog.setVisible(true);
    }
    
    private void configurarEmpleado() {
        while (true) {
            String empleado = JOptionPane.showInputDialog(this, 
                "Ingrese su nombre:", 
                "Configurar Empleado", 
                JOptionPane.QUESTION_MESSAGE);

            if (empleado != null && !empleado.trim().isEmpty()) {
                lblEmpleado.setText(empleado.trim());
                break;
            }
            Notificaciones.showMessageDialog(this,
                "Debe ingresar un nombre para continuar.",
                "Nombre requerido", JOptionPane.WARNING_MESSAGE);
        }
    }
    
    private void buscarHerramientas() {
        cargarPagina(1);
    }
    
    private void cargarPagina(int pagina) {
        String busqueda = txtBusqueda.getText().trim();
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            if (!dbManager.isConnected()) {
                Notificaciones.showMessageDialog(this, 
                    "No hay conexión a la base de datos", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            int tamano = (Integer) comboTamanoPagina.getSelectedItem();
            totalRegistros = dbManager.contarHerramientasBusqueda(busqueda);
            int totalPaginas = (int) Math.ceil(totalRegistros / (double) tamano);
            if (totalPaginas == 0) {
                totalPaginas = 1;
            }
            paginaActual = Math.max(1, Math.min(pagina, totalPaginas));
            int offset = (paginaActual - 1) * tamano;
            
            List<Herramienta> herramientas = dbManager.buscarHerramientasPaginadas(busqueda, offset, tamano);
            modeloTabla.setDatos(herramientas);
            
            lblPagina.setText("Página " + paginaActual + " de " + totalPaginas + " (Total: " + totalRegistros + ")");
            btnAnterior.setEnabled(paginaActual > 1);
            btnSiguiente.setEnabled(paginaActual < totalPaginas);
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this, 
                "Error al buscar: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void agregarAlCarritoDesdeFila(int filaModelo) {
        try {
            Herramienta herramienta = modeloTabla.getHerramientaAt(filaModelo);
            if (herramienta == null) {
                return;
            }
            int stock = herramienta.getStock();
            int cantidad = modeloTabla.getCantidadAt(filaModelo);
            if (cantidad <= 0) {
                Notificaciones.showMessageDialog(this, 
                    "La cantidad debe ser mayor a 0", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (cantidad > stock) {
                Notificaciones.showMessageDialog(this, 
                    "No hay suficiente stock. Stock disponible: " + stock, 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Verificar si ya está en el carrito
            boolean encontrado = false;
            for (ItemCarrito item : carrito) {
                if (item.getIdHerramienta() == herramienta.getId()) {
                    int nuevaCantidad = item.getCantidad() + cantidad;
                    if (nuevaCantidad > stock) {
                        Notificaciones.showMessageDialog(this, 
                            "No hay suficiente stock. Stock disponible: " + stock, 
                            "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    item.setCantidad(nuevaCantidad);
                    encontrado = true;
                    break;
                }
            }
            if (!encontrado) {
                carrito.add(new ItemCarrito(herramienta, cantidad));
            }
            actualizarTablaCarrito();
        } catch (Exception e) {
            Notificaciones.showMessageDialog(this, 
                "Error: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void configurarBusquedaEnVivo() {
        txtBusqueda.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                cargarPagina(1);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                cargarPagina(1);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                cargarPagina(1);
            }
        });
    }
    
    private void quitarDelCarrito() {
        int filaSeleccionada = tablaCarrito.getSelectedRow();
        if (filaSeleccionada == -1) {
            Notificaciones.showMessageDialog(this, 
                "Por favor seleccione un item del carrito", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        carrito.remove(filaSeleccionada);
        actualizarTablaCarrito();
    }

    private void limpiarCarrito() {
        if (carrito.isEmpty()) {
            return;
        }
        int respuesta = JOptionPane.showConfirmDialog(this,
            "¿Desea limpiar todo el carrito?",
            "Confirmar", JOptionPane.YES_NO_OPTION);
        if (respuesta == JOptionPane.YES_OPTION) {
            carrito.clear();
            actualizarTablaCarrito();
        }
    }
    
    private void actualizarTablaCarrito() {
        modeloCarrito.setRowCount(0);
        for (ItemCarrito item : carrito) {
            modeloCarrito.addRow(new Object[]{
                item.getNombre(),
                item.getCategoria(),
                item.getCantidad()
            });
        }
    }
    
    private void realizarPrestamo() {
        if (carrito.isEmpty()) {
            Notificaciones.showMessageDialog(this, 
                "El carrito está vacío", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String empleado = lblEmpleado.getText();
        if (empleado.equals("No configurado")) {
            Notificaciones.showMessageDialog(this, 
                "Por favor configure el nombre del empleado", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        ConfirmarPrestamoDialog dialog = new ConfirmarPrestamoDialog(this, carrito, empleado);
        dialog.setVisible(true);
        
        if (dialog.isPrestamoConfirmado()) {
            carrito.clear();
            actualizarTablaCarrito();
            cargarPagina(paginaActual); // Refrescar búsqueda para actualizar stocks
        }
    }

    private class ResultadosTableModel extends AbstractTableModel {
        private final String[] columnas = {"ID", "Nombre", "Categoría", "Stock", "Cantidad", "Agregar"};
        private List<Herramienta> datos = new ArrayList<>();
        private List<Integer> cantidades = new ArrayList<>();

        public void setDatos(List<Herramienta> herramientas) {
            datos = herramientas != null ? herramientas : new ArrayList<>();
            cantidades = new ArrayList<>();
            for (int i = 0; i < datos.size(); i++) {
                cantidades.add(1);
            }
            fireTableDataChanged();
        }

        public Herramienta getHerramientaAt(int row) {
            if (row < 0 || row >= datos.size()) {
                return null;
            }
            return datos.get(row);
        }

        public int getCantidadAt(int row) {
            if (row < 0 || row >= cantidades.size()) {
                return 1;
            }
            return cantidades.get(row);
        }

        @Override
        public int getRowCount() {
            return datos.size();
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
        public Object getValueAt(int rowIndex, int columnIndex) {
            Herramienta h = datos.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return h.getId();
                case 1:
                    return h.getNombre();
                case 2:
                    return h.getCategoria();
                case 3:
                    return h.getStock();
                case 4:
                    return cantidades.get(rowIndex);
                case 5:
                    return "+";
                default:
                    return "";
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 4 || columnIndex == 5;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == 4) {
                int valor = 1;
                if (aValue instanceof Number) {
                    valor = ((Number) aValue).intValue();
                } else if (aValue != null) {
                    try {
                        valor = Integer.parseInt(aValue.toString());
                    } catch (NumberFormatException ignored) {
                        valor = 1;
                    }
                }
                if (valor < 1) {
                    valor = 1;
                }
                cantidades.set(rowIndex, valor);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0 || columnIndex == 3 || columnIndex == 4) {
                return Integer.class;
            }
            return String.class;
        }
    }

    private class BotonAgregarRenderer extends JButton implements TableCellRenderer {
        public BotonAgregarRenderer() {
            setText("+");
            UIStyles.styleSuccessButton(this);
            setPreferredSize(new Dimension(45, 28));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return this;
        }
    }

    private class BotonAgregarEditor extends AbstractCellEditor implements TableCellEditor {
        private final JButton button = new JButton("+");
        private int row;

        public BotonAgregarEditor() {
            UIStyles.styleSuccessButton(button);
            button.setPreferredSize(new Dimension(45, 28));
            button.addActionListener(e -> {
                agregarAlCarritoDesdeFila(row);
                stopCellEditing();
            });
        }

        @Override
        public Object getCellEditorValue() {
            return "+";
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.row = row;
            return button;
        }
    }

    private class CantidadSpinnerEditor extends AbstractCellEditor implements TableCellEditor {
        private final JSpinner spinner = new JSpinner();

        public CantidadSpinnerEditor() {
            spinner.setModel(new SpinnerNumberModel(1, 1, 9999, 1));
        }

        @Override
        public Object getCellEditorValue() {
            Object value = spinner.getValue();
            return value instanceof Integer ? value : 1;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            int actual = 1;
            if (value instanceof Number) {
                actual = ((Number) value).intValue();
            } else if (value != null) {
                try {
                    actual = Integer.parseInt(value.toString());
                } catch (NumberFormatException ignored) {
                    actual = 1;
                }
            }
            spinner.setValue(Math.max(1, actual));
            return spinner;
        }
    }

    private class CantidadSpinnerRenderer extends JSpinner implements TableCellRenderer {
        public CantidadSpinnerRenderer() {
            setModel(new SpinnerNumberModel(1, 1, 9999, 1));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            int actual = 1;
            if (value instanceof Number) {
                actual = ((Number) value).intValue();
            } else if (value != null) {
                try {
                    actual = Integer.parseInt(value.toString());
                } catch (NumberFormatException ignored) {
                    actual = 1;
                }
            }
            setValue(Math.max(1, actual));
            return this;
        }
    }
    
    private void agregarHerramienta() {
        AgregarHerramientaDialog dialog = new AgregarHerramientaDialog(this);
        dialog.setVisible(true);
        cargarPagina(1);
    }
    
    private void verPrestamos() {
        VerPrestamosDialog dialog = new VerPrestamosDialog(this);
        dialog.setVisible(true);
        // Refrescar cantidades al cerrar la ventana de préstamos
        cargarPagina(paginaActual);
    }

    private void verInventario() {
        InventarioDialog dialog = new InventarioDialog(this);
        dialog.setVisible(true);
    }

    private void verCategorias() {
        CategoriasDialog dialog = new CategoriasDialog(this);
        dialog.setVisible(true);
    }
    
    private void registrarIngreso() {
        RegistrarIngresoDialog dialog = new RegistrarIngresoDialog(this);
        dialog.setVisible(true);
        cargarPagina(paginaActual); // Refrescar búsqueda
    }
}

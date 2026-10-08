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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // Menú paginado
    private static final Color MENU_BG = new Color(27, 42, 74);
    private static final String PAG_PRESTAMO = "prestamo";
    private static final String PAG_PRESTAMOS = "prestamos";
    private static final String PAG_INVENTARIO = "inventario";
    private static final String PAG_REMISIONES = "remisiones";
    private static final String PAG_PROVEEDORES = "proveedores";
    private static final String PAG_CATALOGOS = "catalogos";
    private static final String PAG_REPORTES = "reportes";
    private static final String PAG_CONFIGURACION = "configuracion";
    private static final int COL_ID = 0;
    private static final int COL_NOMBRE = 1;
    private static final int COL_STOCK = 6;
    private static final int COL_CANTIDAD = 7;
    private static final int COL_AGREGAR = 8;
    private CardLayout cardLayout;
    private JPanel panelPaginas;
    private JPanel menuLateral;
    private JLabel lblTituloPagina;
    private final ButtonGroup grupoMenu = new ButtonGroup();
    private final Map<String, JToggleButton> botonesMenu = new HashMap<>();
    private final Map<String, JComponent> paginas = new HashMap<>();
    private final Map<String, String> titulosPaginas = new HashMap<>();
    private final List<String> ordenPaginas = new ArrayList<>();
    private String paginaVisible = PAG_PRESTAMO;
    
    public MainWindow() {
        carrito = new ArrayList<>();
        initComponents();
        verificarConexion();
    }
    
    private void initComponents() {
        setTitle("Sistema de Inventario de Almacén");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 760);
        setMinimumSize(new Dimension(1000, 620));
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        cargarIconoApp();

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(UIStyles.BG);

        // Contenido paginado
        cardLayout = new CardLayout();
        panelPaginas = new JPanel(cardLayout);
        panelPaginas.setOpaque(false);

        // Encabezado de la página actual + empleado
        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setBackground(UIStyles.CARD);
        encabezado.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UIStyles.BORDER),
            BorderFactory.createEmptyBorder(10, 18, 10, 18)));
        lblTituloPagina = new JLabel();
        lblTituloPagina.setFont(lblTituloPagina.getFont().deriveFont(Font.BOLD, 20f));
        lblTituloPagina.setForeground(UIStyles.TEXT);
        encabezado.add(lblTituloPagina, BorderLayout.WEST);
        JPanel panelEmpleado = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
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
        UIStyles.applySvgIcon(btnCambiarEmpleado, "/icons/edit.svg", 16);
        panelEmpleado.add(btnCambiarEmpleado);
        encabezado.add(panelEmpleado, BorderLayout.EAST);

        JPanel centro = new JPanel(new BorderLayout());
        centro.setOpaque(false);
        centro.add(encabezado, BorderLayout.NORTH);
        JPanel envoltura = new JPanel(new BorderLayout());
        envoltura.setOpaque(false);
        envoltura.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        envoltura.add(panelPaginas, BorderLayout.CENTER);
        centro.add(envoltura, BorderLayout.CENTER);

        // Menú lateral paginado
        menuLateral = new JPanel();
        menuLateral.setLayout(new BoxLayout(menuLateral, BoxLayout.Y_AXIS));
        menuLateral.setBackground(MENU_BG);
        menuLateral.setBorder(BorderFactory.createEmptyBorder(16, 10, 16, 10));
        menuLateral.setPreferredSize(new Dimension(230, 100));
        JLabel lblApp = new JLabel("<html><b>Almacén</b><br><span style='font-size:9px'>Inventario y préstamos</span></html>");
        lblApp.setForeground(Color.WHITE);
        lblApp.setFont(lblApp.getFont().deriveFont(17f));
        lblApp.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblApp.setBorder(BorderFactory.createEmptyBorder(0, 8, 18, 0));
        menuLateral.add(lblApp);

        agregarSeccionMenu("PRÉSTAMOS");
        agregarPagina(PAG_PRESTAMO, "Nuevo préstamo", "/icons/add.svg", crearPaginaPrestamo());
        agregarPagina(PAG_PRESTAMOS, "Préstamos registrados", "/icons/search.svg", crearPaginaPrestamos());
        agregarSeccionMenu("ALMACÉN");
        agregarPagina(PAG_INVENTARIO, "Inventario", "/icons/enlace.svg", new InventarioPanel());
        agregarPagina(PAG_REMISIONES, "Remisiones", "/icons/save.svg", new RemisionesPanel(this::getEmpleado));
        agregarPagina(PAG_PROVEEDORES, "Proveedores", "/icons/edit.svg", new ProveedoresPanel());
        agregarPagina(PAG_CATALOGOS, "Categorías, tipos y unidades", "/icons/refresh.svg", new CatalogosPanel());
        agregarSeccionMenu("CONSULTAS");
        agregarPagina(PAG_REPORTES, "Reportes", "/icons/report.svg", new ReportesPanel());
        agregarPagina(PAG_CONFIGURACION, "Configuración", "/icons/edit.svg", crearPaginaConfiguracion());
        menuLateral.add(Box.createVerticalGlue());
        JLabel lblAtajos = new JLabel("<html>Ctrl+1 … Ctrl+8 para cambiar de página</html>");
        lblAtajos.setForeground(new Color(160, 175, 205));
        lblAtajos.setFont(lblAtajos.getFont().deriveFont(11f));
        lblAtajos.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblAtajos.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        menuLateral.add(lblAtajos);

        raiz.add(menuLateral, BorderLayout.WEST);
        raiz.add(centro, BorderLayout.CENTER);
        setContentPane(raiz);
        registrarAtajos();
    }

    private void agregarSeccionMenu(String titulo) {
        JLabel lbl = new JLabel(titulo);
        lbl.setForeground(new Color(140, 158, 194));
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 11f));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setBorder(BorderFactory.createEmptyBorder(12, 8, 6, 0));
        menuLateral.add(lbl);
    }

    private void agregarPagina(String id, String titulo, String icono, JComponent pagina) {
        panelPaginas.add(pagina, id);
        paginas.put(id, pagina);
        titulosPaginas.put(id, titulo);
        ordenPaginas.add(id);

        JToggleButton boton = new JToggleButton(titulo);
        boton.setHorizontalAlignment(SwingConstants.LEFT);
        boton.setAlignmentX(Component.LEFT_ALIGNMENT);
        boton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        boton.setFocusPainted(false);
        boton.putClientProperty("FlatLaf.style",
            "background: #1B2A4A; foreground: #DCE4F5; selectedBackground: #2D6CDF; selectedForeground: #FFFFFF; " +
            "hoverBackground: #26385F; pressedBackground: #2D6CDF; borderWidth: 0; focusWidth: 0; arc: 10; margin: 8,12,8,12");
        try {
            com.formdev.flatlaf.extras.FlatSVGIcon svg = new com.formdev.flatlaf.extras.FlatSVGIcon(icono.substring(1), 16, 16);
            svg.setColorFilter(new com.formdev.flatlaf.extras.FlatSVGIcon.ColorFilter(c -> Color.WHITE));
            boton.setIcon(svg);
            boton.setIconTextGap(10);
        } catch (Exception ignored) {
            // Sin icono
        }
        boton.addActionListener(e -> mostrarPagina(id));
        grupoMenu.add(boton);
        botonesMenu.put(id, boton);
        menuLateral.add(boton);
        menuLateral.add(Box.createVerticalStrut(4));
    }

    private void registrarAtajos() {
        JRootPane rootPane = getRootPane();
        for (int i = 0; i < ordenPaginas.size() && i < 9; i++) {
            String id = ordenPaginas.get(i);
            KeyStroke ks = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_1 + i,
                java.awt.event.InputEvent.CTRL_DOWN_MASK);
            rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks, "pagina_" + id);
            rootPane.getActionMap().put("pagina_" + id, new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    mostrarPagina(id);
                }
            });
        }
    }

    private void mostrarPagina(String id) {
        cardLayout.show(panelPaginas, id);
        lblTituloPagina.setText(titulosPaginas.get(id));
        JToggleButton boton = botonesMenu.get(id);
        if (boton != null && !boton.isSelected()) {
            boton.setSelected(true);
        }
        paginaVisible = id;
        JComponent pagina = paginas.get(id);
        if (pagina instanceof Pagina && DatabaseManager.getInstance().isConnected()) {
            ((Pagina) pagina).alMostrar();
        }
    }

    private String getEmpleado() {
        String empleado = lblEmpleado.getText();
        return "No configurado".equals(empleado) ? "" : empleado;
    }

    /** Página "Nuevo préstamo": búsqueda de material + carrito. */
    private JComponent crearPaginaPrestamo() {
        JPanel panelPrincipal = new JPanel(new BorderLayout(16, 16));
        panelPrincipal.setOpaque(false);

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
        JLabel lblBuscar = new JLabel("Buscar herramienta / material:");
        lblBuscar.setForeground(UIStyles.TEXT);
        panelBusqueda.add(lblBuscar, BorderLayout.WEST);
        txtBusqueda = new JTextField();
        txtBusqueda.setPreferredSize(new Dimension(320, 32));
        txtBusqueda.setToolTipText("Nombre, categoría, tipo, unidad o proveedor");
        configurarBusquedaEnVivo();
        JButton btnBuscar = new JButton("Buscar");
        btnBuscar.addActionListener(e -> buscarHerramientas());
        UIStyles.stylePrimaryButton(btnBuscar);
        UIStyles.tamanoMinimo(btnBuscar, 120, 34);
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
        tablaHerramientas.getColumnModel().getColumn(COL_ID).setPreferredWidth(45);
        tablaHerramientas.getColumnModel().getColumn(COL_NOMBRE).setPreferredWidth(200);
        tablaHerramientas.getColumnModel().getColumn(COL_CANTIDAD).setPreferredWidth(90);
        tablaHerramientas.getColumnModel().getColumn(COL_AGREGAR).setPreferredWidth(70);
        tablaHerramientas.getColumnModel().getColumn(COL_CANTIDAD).setCellEditor(new CantidadSpinnerEditor());
        tablaHerramientas.getColumnModel().getColumn(COL_CANTIDAD).setCellRenderer(new CantidadSpinnerRenderer());
        tablaHerramientas.getColumnModel().getColumn(COL_AGREGAR).setCellRenderer(new BotonAgregarRenderer());
        tablaHerramientas.getColumnModel().getColumn(COL_AGREGAR).setCellEditor(new BotonAgregarEditor());
        // Estilizar encabezado y centrar números
        UIStyles.styleTableHeader(tablaHerramientas);
        DefaultTableCellRenderer centerRenderer = UIStyles.createCenteredNumberRenderer();
        tablaHerramientas.getColumnModel().getColumn(COL_ID).setCellRenderer(centerRenderer);
        tablaHerramientas.getColumnModel().getColumn(COL_STOCK).setCellRenderer(centerRenderer);
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
        UIStyles.tamanoMinimo(btnAnterior, 120, 34);
        UIStyles.applySvgIcon(btnAnterior, "/icons/menorque.svg", 16);
        panelPaginacion.add(btnAnterior);
        btnSiguiente = new JButton("Siguiente");
        btnSiguiente.addActionListener(e -> cargarPagina(paginaActual + 1));
        UIStyles.styleSecondaryButton(btnSiguiente);
        UIStyles.tamanoMinimo(btnSiguiente, 120, 34);
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
        
        String[] columnasCarrito = {"Nombre", "Proveedor", "Unidad", "Cantidad"};
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
        tablaCarrito.getColumnModel().getColumn(3).setCellRenderer(centerRendererCarrito); // Cantidad
        JScrollPane scrollCarrito = new JScrollPane(tablaCarrito);
        JPanel cardCarrito = UIStyles.createCard("Herramientas seleccionadas", scrollCarrito);
        
        // Botones del carrito
        JPanel panelBotonesCarrito = new JPanel(new BorderLayout(8, 8));
        panelBotonesCarrito.setOpaque(false);
        JPanel panelBotonesSecundarios = new JPanel(new GridLayout(1, 2, 8, 0));
        panelBotonesSecundarios.setOpaque(false);
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
        UIStyles.applySvgIcon(btnQuitarCarrito, "/icons/cancel.svg", 16);
        UIStyles.applySvgIcon(btnLimpiarCarrito, "/icons/delete.svg", 16);
        UIStyles.applySvgIcon(btnRealizarPrestamo, "/icons/save.svg", 16);
        UIStyles.tamanoMinimo(btnRealizarPrestamo, 165, 40);
        panelBotonesSecundarios.add(btnQuitarCarrito);
        panelBotonesSecundarios.add(btnLimpiarCarrito);
        panelBotonesCarrito.add(panelBotonesSecundarios, BorderLayout.NORTH);
        panelBotonesCarrito.add(btnRealizarPrestamo, BorderLayout.SOUTH);
        cardCarrito.add(panelBotonesCarrito, BorderLayout.SOUTH);
        panelDerecho.add(cardCarrito, BorderLayout.CENTER);
        
        splitPane.setLeftComponent(panelIzquierdo);
        splitPane.setRightComponent(panelDerecho);
        splitPane.setDividerLocation(780);
        splitPane.setResizeWeight(0.62);
        
        panelPrincipal.add(splitPane, BorderLayout.CENTER);
        return new PaginaPrestamo(panelPrincipal);
    }

    /** Envoltura para refrescar los resultados al volver a la página de préstamo. */
    private class PaginaPrestamo extends JPanel implements Pagina {
        PaginaPrestamo(JComponent contenido) {
            super(new BorderLayout());
            setOpaque(false);
            add(contenido, BorderLayout.CENTER);
        }

        @Override
        public void alMostrar() {
            cargarPagina(paginaActual);
        }
    }

    /** Página "Préstamos registrados": accesos a activos, devueltos y materiales prestados. */
    private JComponent crearPaginaPrestamos() {
        JPanel grid = new JPanel(new GridLayout(1, 3, 14, 14));
        grid.setOpaque(false);
        grid.add(tarjetaAccion("Préstamos activos",
            "Consultar préstamos, registrar devoluciones (totales o parciales) y generar reportes.",
            "Abrir", "/icons/search.svg", this::verPrestamos));
        grid.add(tarjetaAccion("Préstamos devueltos",
            "Historial de préstamos ya devueltos con sus observaciones.",
            "Abrir", "/icons/search.svg", () -> new VerPrestamosDevueltosDialog(this).setVisible(true)));
        grid.add(tarjetaAccion("Materiales prestados",
            "Qué material está fuera, cuánto, con quién y desde cuándo.",
            "Abrir", "/icons/search.svg", () -> new MaterialesPrestadosDialog(this).setVisible(true)));
        JPanel pagina = new JPanel(new BorderLayout());
        pagina.setOpaque(false);
        pagina.add(grid, BorderLayout.NORTH);
        return pagina;
    }

    /** Página "Configuración": base de datos y empleado. */
    private JComponent crearPaginaConfiguracion() {
        JPanel grid = new JPanel(new GridLayout(1, 3, 14, 14));
        grid.setOpaque(false);
        grid.add(tarjetaAccion("Base de datos",
            "Elegir la carpeta y la base de datos SQLite, y la carpeta de fotos.",
            "Configurar", "/icons/edit.svg", () -> {
                abrirConfiguracion();
                mostrarPagina(paginaVisible);
            }));
        grid.add(tarjetaAccion("Empleado en turno",
            "Cambiar el nombre del empleado que registra préstamos y recibe remisiones.",
            "Cambiar", "/icons/edit.svg", this::configurarEmpleado));
        grid.add(tarjetaAccion("Salir",
            "Cerrar el sistema.",
            "Salir", "/icons/cancel.svg", () -> System.exit(0)));
        JPanel pagina = new JPanel(new BorderLayout());
        pagina.setOpaque(false);
        pagina.add(grid, BorderLayout.NORTH);
        return pagina;
    }

    private JPanel tarjetaAccion(String titulo, String descripcion, String textoBoton, String icono, Runnable accion) {
        JPanel cuerpo = new JPanel(new BorderLayout(8, 12));
        cuerpo.setOpaque(false);
        JLabel lbl = new JLabel("<html><div style='width:240px'>" + descripcion + "</div></html>");
        lbl.setForeground(new Color(90, 100, 120));
        cuerpo.add(lbl, BorderLayout.CENTER);
        JButton btn = new JButton(textoBoton);
        UIStyles.stylePrimaryButton(btn);
        UIStyles.applySvgIcon(btn, icono, 16);
        btn.addActionListener(e -> accion.run());
        JPanel pie = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        pie.setOpaque(false);
        pie.add(btn);
        cuerpo.add(pie, BorderLayout.SOUTH);
        return UIStyles.createCard(titulo, cuerpo);
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
        // Mostrar la primera página y cargar resultados al iniciar
        mostrarPagina(PAG_PRESTAMO);
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
                item.getProveedorNombre() != null ? item.getProveedorNombre() : "-",
                item.getUnidad(),
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
        private final String[] columnas = {"ID", "Nombre", "Categoría", "Tipo", "Unidad", "Proveedor", "Stock", "Cantidad", "Agregar"};
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
                    return h.getTipo();
                case 4:
                    return h.getUnidad();
                case 5:
                    return h.getProveedorNombre() != null ? h.getProveedorNombre() : "-";
                case COL_STOCK:
                    return h.getStock();
                case COL_CANTIDAD:
                    return cantidades.get(rowIndex);
                case COL_AGREGAR:
                    return "+";
                default:
                    return "";
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == COL_CANTIDAD || columnIndex == COL_AGREGAR;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == COL_CANTIDAD) {
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
            if (columnIndex == COL_ID || columnIndex == COL_STOCK || columnIndex == COL_CANTIDAD) {
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
    
    private void verPrestamos() {
        VerPrestamosDialog dialog = new VerPrestamosDialog(this);
        dialog.setVisible(true);
        // Refrescar cantidades al cerrar la ventana de préstamos
        cargarPagina(paginaActual);
    }
}

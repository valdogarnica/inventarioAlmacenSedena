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
    private static final int COL_CARRITO_CANTIDAD = 2;
    private static final int COL_CARRITO_DEVOLUCION = 3;
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
    private static final int ANCHO_MENU = 248;
    // Con este ancho los iconos quedan donde mismo que con el menú expandido
    private static final int ANCHO_MENU_CONTRAIDO = 64;
    private static final int DURACION_ANIMACION_MS = 250;
    private static final int ANCHO_CONTRAER_MENU = 1200;
    private static final java.util.prefs.Preferences PREFS =
        java.util.prefs.Preferences.userNodeForPackage(MainWindow.class);
    private static final String PREF_MENU_CONTRAIDO = "menu_contraido";
    private static final String PREF_MENU_MANUAL = "menu_manual";
    private static final String PAG_INICIO = "inicio";
    private static final String PAG_PRESTAMO = "prestamo";
    private static final String PAG_PRESTAMOS = "prestamos";
    private static final String PAG_INVENTARIO = "inventario";
    private static final String PAG_REMISIONES = "remisiones";
    private static final String PAG_PROVEEDORES = "proveedores";
    private static final String PAG_CATALOGOS = "catalogos";
    private static final String PAG_REPORTES = "reportes";
    private static final String PAG_CONFIGURACION = "configuracion";
    private static final int COL_NOMBRE = 0;
    private static final int COL_TIPO = 2;
    private static final int COL_STOCK = 4;
    private static final int COL_CANTIDAD = 5;
    private static final int COL_AGREGAR = 6;
    /** Tipos (en minúsculas) de material de no retorno, según Catálogos. */
    private java.util.Set<String> tiposNoRetorno = new java.util.HashSet<>();
    private CardLayout cardLayout;
    private JPanel panelPaginas;
    private JPanel menuLateral;
    private JLabel lblTituloPagina;
    private JLabel lblSubtituloPagina;
    private JPanel itemsMenu;
    private JPanel lblApp;
    private JLabel lblAtajos;
    private JButton btnTema;
    private javax.swing.Timer animacionMenu;
    private JButton btnMenu;
    private boolean menuContraido;
    private boolean menuManual;
    private final List<JComponent> seccionesMenu = new ArrayList<>();
    private final Map<String, String> subtitulosPaginas = new HashMap<>();
    private final ButtonGroup grupoMenu = new ButtonGroup();
    private final Map<String, JToggleButton> botonesMenu = new HashMap<>();
    private final Map<String, JComponent> paginas = new HashMap<>();
    private final Map<String, String> titulosPaginas = new HashMap<>();
    private final List<String> ordenPaginas = new ArrayList<>();
    private String paginaVisible = PAG_INICIO;
    
    public MainWindow() {
        carrito = new ArrayList<>();
        initComponents();
        verificarConexion();
    }
    
    private void initComponents() {
        setTitle("Sistema de Inventario de Almacén");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 760);
        setMinimumSize(new Dimension(880, 580));
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        cargarIconoApp();

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.putClientProperty("FlatLaf.style", "background: $App.background");

        // Contenido paginado
        cardLayout = new CardLayout();
        panelPaginas = new JPanel(cardLayout);
        panelPaginas.setOpaque(false);

        // Encabezado de la página actual + empleado
        JPanel encabezado = new JPanel(new BorderLayout(12, 0));
        encabezado.setOpaque(true);
        encabezado.putClientProperty("FlatLaf.style", "background: $App.card");
        encabezado.setBorder(new BordeInferior());
        lblTituloPagina = new JLabel();
        lblTituloPagina.setFont(lblTituloPagina.getFont().deriveFont(Font.BOLD, 21f));
        lblSubtituloPagina = new JLabel();
        UIStyles.textoSecundario(lblSubtituloPagina);
        JPanel titulos = new JPanel(new GridLayout(2, 1, 0, 0));
        titulos.setOpaque(false);
        titulos.add(lblTituloPagina);
        titulos.add(lblSubtituloPagina);
        encabezado.add(titulos, BorderLayout.CENTER);

        JPanel panelEmpleado = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        panelEmpleado.setOpaque(false);
        btnTema = new JButton();
        UIStyles.styleSecondaryButton(btnTema);
        UIStyles.estilo(btnTema, "arc: 999; margin: 7,7,7,7");
        btnTema.addActionListener(e -> {
            Tema.setOscuro(!Tema.esOscuro());
            actualizarBotonTema();
        });
        actualizarBotonTema();
        panelEmpleado.add(btnTema);
        JLabel lblIconoEmpleado = new JLabel(icono("/icons/usuario.svg", 18, "App.muted"));
        panelEmpleado.add(lblIconoEmpleado);
        JPanel textoEmpleado = new JPanel(new GridLayout(2, 1, 0, 0));
        textoEmpleado.setOpaque(false);
        JLabel lblEmpleadoTitulo = new JLabel("Empleado en turno");
        UIStyles.textoSecundario(lblEmpleadoTitulo);
        lblEmpleadoTitulo.setFont(lblEmpleadoTitulo.getFont().deriveFont(11f));
        textoEmpleado.add(lblEmpleadoTitulo);
        lblEmpleado = new JLabel("No configurado");
        lblEmpleado.setFont(lblEmpleado.getFont().deriveFont(Font.BOLD));
        textoEmpleado.add(lblEmpleado);
        panelEmpleado.add(textoEmpleado);
        JButton btnCambiarEmpleado = new JButton("Cambiar");
        btnCambiarEmpleado.addActionListener(e -> configurarEmpleado());
        UIStyles.styleSecondaryButton(btnCambiarEmpleado);
        UIStyles.applySvgIcon(btnCambiarEmpleado, "/icons/edit.svg", 14);
        panelEmpleado.add(btnCambiarEmpleado);
        JPanel envolturaEmpleado = new JPanel(new GridBagLayout());
        envolturaEmpleado.setOpaque(false);
        envolturaEmpleado.add(panelEmpleado);
        encabezado.add(envolturaEmpleado, BorderLayout.EAST);

        JPanel centro = new JPanel(new BorderLayout());
        centro.setOpaque(false);
        centro.add(encabezado, BorderLayout.NORTH);
        JPanel envoltura = new JPanel(new BorderLayout());
        envoltura.setOpaque(false);
        envoltura.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        envoltura.add(panelPaginas, BorderLayout.CENTER);
        centro.add(envoltura, BorderLayout.CENTER);

        // Menú lateral contraíble
        menuLateral = new JPanel(new BorderLayout());
        menuLateral.setOpaque(true);
        menuLateral.putClientProperty("FlatLaf.style", "background: $App.menu; border: 14,10,14,10");

        JPanel marca = new JPanel(new BorderLayout(8, 0));
        marca.setOpaque(false);
        marca.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        btnMenu = new JButton(icono("/icons/menu.svg", 20, "App.menuText"));
        btnMenu.setToolTipText("Contraer o expandir el menú");
        btnMenu.putClientProperty("FlatLaf.style", "background: $App.menu; hoverBackground: $App.menuHover; "
            + "pressedBackground: $App.menuHover; focusedBackground: $App.menu; borderWidth: 0; focusWidth: 0; "
            + "innerFocusWidth: 0; arc: 10; margin: 8,10,8,10");
        btnMenu.addActionListener(e -> {
            menuManual = true;
            setMenuContraido(!menuContraido);
        });
        marca.add(btnMenu, BorderLayout.WEST);
        lblApp = new JPanel(new GridLayout(2, 1, 0, 0));
        lblApp.setOpaque(false);
        JLabel lblNombreApp = UIStyles.titulo("Almacén", 3f);
        lblNombreApp.putClientProperty("FlatLaf.style", "foreground: $App.menuText");
        JLabel lblDescripcionApp = new JLabel("Inventario y préstamos");
        lblDescripcionApp.putClientProperty("FlatLaf.style", "foreground: $App.menuMuted");
        lblApp.add(lblNombreApp);
        lblApp.add(lblDescripcionApp);
        marca.add(lblApp, BorderLayout.CENTER);
        menuLateral.add(marca, BorderLayout.NORTH);

        itemsMenu = new JPanel();
        itemsMenu.setLayout(new BoxLayout(itemsMenu, BoxLayout.Y_AXIS));
        itemsMenu.setOpaque(false);
        agregarPagina(PAG_INICIO, "Inicio", "/icons/inicio.svg", new InicioPanel(),
            "Resumen del almacén con gráficas y lo que necesita atención");
        agregarSeccionMenu("PRÉSTAMOS");
        agregarPagina(PAG_PRESTAMO, "Nuevo préstamo", "/icons/carrito.svg", crearPaginaPrestamo(),
            "Busque material y agréguelo al carrito para prestarlo");
        agregarPagina(PAG_PRESTAMOS, "Préstamos registrados", "/icons/lista.svg", crearPaginaPrestamos(),
            "Consulte, devuelva y reporte los préstamos");
        agregarSeccionMenu("ALMACÉN");
        agregarPagina(PAG_INVENTARIO, "Inventario", "/icons/caja.svg", new InventarioPanel(),
            "Existencias por material y por proveedor");
        agregarPagina(PAG_REMISIONES, "Remisiones", "/icons/camion.svg", new RemisionesPanel(this::getEmpleado),
            "Entradas de material por remisión de proveedor");
        agregarPagina(PAG_PROVEEDORES, "Proveedores", "/icons/usuarios.svg", new ProveedoresPanel(),
            "Proveedores con sus materiales y remisiones");
        agregarPagina(PAG_CATALOGOS, "Categorías, tipos y unidades", "/icons/etiqueta.svg", new CatalogosPanel(),
            "Catálogos que se eligen al capturar materiales");
        agregarSeccionMenu("CONSULTAS");
        agregarPagina(PAG_REPORTES, "Reportes", "/icons/grafica.svg", new ReportesPanel(),
            "Reportes en PDF listos para imprimir");
        agregarPagina(PAG_CONFIGURACION, "Configuración", "/icons/ajustes.svg", crearPaginaConfiguracion(),
            "Base de datos, empleado y salida");
        itemsMenu.add(Box.createVerticalGlue());
        JScrollPane scrollMenu = new JScrollPane(itemsMenu,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollMenu.setBorder(BorderFactory.createEmptyBorder());
        scrollMenu.setOpaque(false);
        scrollMenu.getViewport().setOpaque(false);
        scrollMenu.getVerticalScrollBar().setUnitIncrement(16);
        menuLateral.add(scrollMenu, BorderLayout.CENTER);

        lblAtajos = new JLabel("<html>Ctrl+1 a Ctrl+9: cambiar de página</html>");
        lblAtajos.putClientProperty("FlatLaf.style", "foreground: $App.menuMuted");
        lblAtajos.setFont(lblAtajos.getFont().deriveFont(11f));
        lblAtajos.setBorder(BorderFactory.createEmptyBorder(10, 6, 0, 0));
        menuLateral.add(lblAtajos, BorderLayout.SOUTH);

        raiz.add(menuLateral, BorderLayout.WEST);
        raiz.add(centro, BorderLayout.CENTER);
        setContentPane(raiz);
        registrarAtajos();

        // En pantallas angostas el menú se contrae solo (salvo que el usuario lo haya elegido)
        menuContraido = PREFS.getBoolean(PREF_MENU_CONTRAIDO, false);
        menuManual = PREFS.getBoolean(PREF_MENU_MANUAL, false);
        aplicarMenuContraido(menuContraido);
        menuLateral.setPreferredSize(new Dimension(menuContraido ? ANCHO_MENU_CONTRAIDO : ANCHO_MENU, 100));
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (!menuManual) {
                    boolean angosta = getWidth() < ANCHO_CONTRAER_MENU;
                    if (angosta != menuContraido) {
                        setMenuContraido(angosta);
                    }
                }
            }
        });
    }

    /** Icono SVG pintado con un color del tema (se actualiza solo al cambiar de modo). */
    private static Icon icono(String ruta, int tamano, String claveColor) {
        try {
            com.formdev.flatlaf.extras.FlatSVGIcon svg = new com.formdev.flatlaf.extras.FlatSVGIcon(ruta.substring(1), tamano, tamano);
            svg.setColorFilter(new com.formdev.flatlaf.extras.FlatSVGIcon.ColorFilter(c -> Tema.color(claveColor)));
            return svg;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Contrae el menú a solo iconos (con el nombre como ayuda emergente) o lo expande,
     * con una animación del ancho.
     */
    private void setMenuContraido(boolean contraido) {
        menuContraido = contraido;
        PREFS.putBoolean(PREF_MENU_CONTRAIDO, contraido);
        PREFS.putBoolean(PREF_MENU_MANUAL, menuManual);
        if (animacionMenu != null) {
            animacionMenu.stop();
        }
        if (!contraido) {
            // Al abrir, los textos aparecen mientras el menú crece
            aplicarMenuContraido(false);
        }
        int inicio = menuLateral.getWidth() > 0 ? menuLateral.getWidth() : menuLateral.getPreferredSize().width;
        int fin = contraido ? ANCHO_MENU_CONTRAIDO : ANCHO_MENU;
        long arranque = System.currentTimeMillis();
        animacionMenu = new javax.swing.Timer(15, null);
        animacionMenu.addActionListener(e -> {
            double t = Math.min(1.0, (System.currentTimeMillis() - arranque) / (double) DURACION_ANIMACION_MS);
            double suave = 1 - Math.pow(1 - t, 3); // desacelera al final
            int ancho = (int) Math.round(inicio + (fin - inicio) * suave);
            menuLateral.setPreferredSize(new Dimension(ancho, 100));
            menuLateral.revalidate();
            if (t >= 1.0) {
                animacionMenu.stop();
                if (menuContraido) {
                    // Al cerrar, los textos se quitan cuando el menú ya es angosto
                    aplicarMenuContraido(true);
                }
            }
        });
        animacionMenu.start();
    }

    /** Muestra u oculta los textos del menú. */
    private void aplicarMenuContraido(boolean contraido) {
        lblApp.setVisible(!contraido);
        lblAtajos.setVisible(!contraido);
        for (JComponent seccion : seccionesMenu) {
            if (seccion instanceof JLabel) {
                seccion.setVisible(!contraido);
            } else {
                seccion.setVisible(contraido);
            }
        }
        for (Map.Entry<String, JToggleButton> e : botonesMenu.entrySet()) {
            JToggleButton b = e.getValue();
            String titulo = titulosPaginas.get(e.getKey());
            b.setText(contraido ? "" : titulo);
            b.setToolTipText(contraido ? titulo : null);
            b.setHorizontalAlignment(contraido ? SwingConstants.CENTER : SwingConstants.LEFT);
        }
        menuLateral.revalidate();
        menuLateral.repaint();
    }

    private void actualizarBotonTema() {
        boolean oscuro = Tema.esOscuro();
        btnTema.setIcon(icono(oscuro ? "/icons/sol.svg" : "/icons/luna.svg", 18, "App.text"));
        btnTema.setToolTipText(oscuro ? "Cambiar a modo claro" : "Cambiar a modo oscuro");
    }

    /** Línea inferior del encabezado con el color de borde del tema. */
    private static final class BordeInferior extends javax.swing.border.EmptyBorder {
        BordeInferior() {
            super(12, 22, 13, 18);
        }

        @Override
        public void paintBorder(Component c, java.awt.Graphics g, int x, int y, int width, int height) {
            g.setColor(Tema.color("App.border"));
            g.fillRect(x, y + height - 1, width, 1);
        }
    }

    private void agregarSeccionMenu(String titulo) {
        JLabel lbl = new JLabel(titulo);
        lbl.putClientProperty("FlatLaf.style", "foreground: $App.menuSection");
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 10.5f));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setBorder(BorderFactory.createEmptyBorder(14, 10, 6, 0));
        itemsMenu.add(lbl);
        seccionesMenu.add(lbl);
        // Con el menú contraído, las secciones se ven como una línea
        JSeparator sep = new JSeparator();
        sep.putClientProperty("FlatLaf.style", "foreground: $App.menuSeparator; background: $App.menuSeparator");
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 12));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        sep.setVisible(false);
        itemsMenu.add(sep);
        seccionesMenu.add(sep);
    }

    private void agregarPagina(String id, String titulo, String icono, JComponent pagina, String subtitulo) {
        panelPaginas.add(pagina, id);
        paginas.put(id, pagina);
        titulosPaginas.put(id, titulo);
        subtitulosPaginas.put(id, subtitulo);
        ordenPaginas.add(id);

        JToggleButton boton = new JToggleButton(titulo);
        boton.setHorizontalAlignment(SwingConstants.LEFT);
        boton.setAlignmentX(Component.LEFT_ALIGNMENT);
        boton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        boton.setFocusPainted(false);
        boton.putClientProperty("FlatLaf.style",
            "background: $App.menu; foreground: $App.menuText; selectedBackground: $App.accent; selectedForeground: #FFFFFF; " +
            "hoverBackground: $App.menuHover; pressedBackground: $App.accentPressed; focusedBackground: $App.menu; " +
            "borderWidth: 0; focusWidth: 0; innerFocusWidth: 0; arc: 10; margin: 9,12,9,12");
        Icon ic = icono(icono, 18, "App.menuText");
        if (ic != null) {
            boton.setIcon(ic);
            boton.setIconTextGap(12);
        }
        boton.addActionListener(e -> mostrarPagina(id));
        grupoMenu.add(boton);
        botonesMenu.put(id, boton);
        itemsMenu.add(boton);
        itemsMenu.add(Box.createVerticalStrut(4));
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
        lblSubtituloPagina.setText(subtitulosPaginas.getOrDefault(id, ""));
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
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        
        // Panel izquierdo - Búsqueda y resultados
        JPanel panelIzquierdo = new JPanel(new BorderLayout(12, 12));
        panelIzquierdo.setOpaque(false);
        
        // Búsqueda
        JPanel panelBusqueda = new JPanel(new BorderLayout(10, 10));
        panelBusqueda.setOpaque(false);
        JLabel lblBuscar = new JLabel("Buscar herramienta / material:");
        panelBusqueda.add(lblBuscar, BorderLayout.WEST);
        txtBusqueda = new JTextField();
        txtBusqueda.putClientProperty("JTextField.placeholderText", "Escriba nombre, categoría, tipo o unidad…");
        txtBusqueda.putClientProperty("JTextField.showClearButton", true);
        txtBusqueda.setToolTipText("Nombre, categoría, tipo, unidad o proveedor");
        configurarBusquedaEnVivo();
        JButton btnBuscar = new JButton("Buscar");
        btnBuscar.addActionListener(e -> buscarHerramientas());
        UIStyles.stylePrimaryButton(btnBuscar);
        UIStyles.tamanoMinimo(btnBuscar, 120, 34);
        UIStyles.applySvgIcon(btnBuscar, "/icons/search.svg", 16);
        panelBusqueda.add(txtBusqueda, BorderLayout.CENTER);
        panelBusqueda.add(btnBuscar, BorderLayout.EAST);
        JPanel cardBusqueda = UIStyles.createCard("Búsqueda", panelBusqueda);
        panelIzquierdo.add(cardBusqueda, BorderLayout.NORTH);
        
        // Tabla de herramientas
        modeloTabla = new ResultadosTableModel();
        tablaHerramientas = new JTable(modeloTabla);
        tablaHerramientas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaHerramientas.getTableHeader().setReorderingAllowed(false);
        tablaHerramientas.setRowHeight(32);
        tablaHerramientas.getColumnModel().getColumn(COL_NOMBRE).setPreferredWidth(240);
        tablaHerramientas.getColumnModel().getColumn(COL_TIPO).setPreferredWidth(150);
        tablaHerramientas.getColumnModel().getColumn(COL_CANTIDAD).setPreferredWidth(90);
        tablaHerramientas.getColumnModel().getColumn(COL_CANTIDAD).setMinWidth(80);
        tablaHerramientas.getColumnModel().getColumn(COL_AGREGAR).setPreferredWidth(80);
        tablaHerramientas.getColumnModel().getColumn(COL_AGREGAR).setMinWidth(76);
        tablaHerramientas.getColumnModel().getColumn(COL_CANTIDAD).setCellEditor(new CantidadSpinnerEditor());
        tablaHerramientas.getColumnModel().getColumn(COL_CANTIDAD).setCellRenderer(new CantidadSpinnerRenderer());
        tablaHerramientas.getColumnModel().getColumn(COL_AGREGAR).setCellRenderer(new BotonAgregarRenderer());
        tablaHerramientas.getColumnModel().getColumn(COL_AGREGAR).setCellEditor(new BotonAgregarEditor());
        // Estilizar encabezado y centrar números
        UIStyles.styleTableHeader(tablaHerramientas);
        DefaultTableCellRenderer centerRenderer = UIStyles.createCenteredNumberRenderer();
        tablaHerramientas.getColumnModel().getColumn(COL_STOCK).setCellRenderer(centerRenderer);
        tablaHerramientas.getColumnModel().getColumn(COL_TIPO).setCellRenderer(new InsigniaRenderer() {
            @Override
            protected Tono tono(JTable table, Object value, int row) {
                Herramienta h = modeloTabla.getHerramientaAt(row);
                return h != null && esNoRetorno(h) ? Tono.AVISO : null;
            }
        });
        JScrollPane scrollHerramientas = new JScrollPane(tablaHerramientas);

        JPanel cardResultados = UIStyles.createCard("Resultados", scrollHerramientas);
        
        // Panel de paginación
        JPanel panelPaginacion = new JPanel(new WrapLayout(FlowLayout.LEFT, 6, 2));
        panelPaginacion.setOpaque(false);
        JLabel lblTamano = new JLabel("Registros por página:");
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
        panelPaginacion.add(lblPagina);
        
        JPanel panelResultadosCompleto = new JPanel(new BorderLayout());
        panelResultadosCompleto.setOpaque(false);
        panelResultadosCompleto.add(cardResultados, BorderLayout.CENTER);
        panelResultadosCompleto.add(panelPaginacion, BorderLayout.SOUTH);
        panelIzquierdo.add(panelResultadosCompleto, BorderLayout.CENTER);
        
        // Panel derecho - Carrito
        JPanel panelDerecho = new JPanel(new BorderLayout(12, 12));
        panelDerecho.setOpaque(false);
        
        String[] columnasCarrito = {"Nombre", "Unidad", "Cantidad", "Devolución"};
        modeloCarrito = new DefaultTableModel(columnasCarrito, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == COL_CARRITO_CANTIDAD;
            }

            @Override
            public void setValueAt(Object aValue, int row, int column) {
                if (column == COL_CARRITO_CANTIDAD && row < carrito.size()) {
                    // La cantidad se puede bajar o subir sin pasar del stock del material
                    ItemCarrito item = carrito.get(row);
                    int cantidad = aValue instanceof Number ? ((Number) aValue).intValue() : item.getCantidad();
                    int stock = item.getHerramienta().getStock();
                    item.setCantidad(Math.max(1, Math.min(stock, cantidad)));
                    aValue = item.getCantidad();
                }
                super.setValueAt(aValue, row, column);
            }
        };
        tablaCarrito = new JTable(modeloCarrito);
        tablaCarrito.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaCarrito.getTableHeader().setReorderingAllowed(false);
        tablaCarrito.setRowHeight(32);
        // Estilizar encabezado y centrar números
        UIStyles.styleTableHeader(tablaCarrito);
        tablaCarrito.getColumnModel().getColumn(COL_CARRITO_CANTIDAD).setCellRenderer(new CantidadSpinnerRenderer());
        tablaCarrito.getColumnModel().getColumn(COL_CARRITO_CANTIDAD).setCellEditor(new CantidadCarritoEditor());
        tablaCarrito.getColumnModel().getColumn(COL_CARRITO_CANTIDAD).setPreferredWidth(85);
        tablaCarrito.getColumnModel().getColumn(COL_CARRITO_CANTIDAD).setMinWidth(80);
        tablaCarrito.getColumnModel().getColumn(0).setPreferredWidth(150);
        tablaCarrito.getColumnModel().getColumn(1).setPreferredWidth(70);
        tablaCarrito.getColumnModel().getColumn(1).setMinWidth(62);
        tablaCarrito.getColumnModel().getColumn(COL_CARRITO_DEVOLUCION).setPreferredWidth(110);
        tablaCarrito.getColumnModel().getColumn(COL_CARRITO_DEVOLUCION).setMinWidth(105);
        tablaCarrito.getColumnModel().getColumn(COL_CARRITO_DEVOLUCION).setCellRenderer(new InsigniaRenderer() {
            @Override
            protected Tono tono(JTable table, Object value, int row) {
                return row < carrito.size() && esNoRetorno(carrito.get(row).getHerramienta()) ? Tono.AVISO : Tono.INFO;
            }
        });
        tablaCarrito.setToolTipText("Cambie la cantidad con las flechas; no puede pasar del stock del material");
        JScrollPane scrollCarrito = new JScrollPane(tablaCarrito);
        JPanel cardCarrito = UIStyles.createCard("Herramientas seleccionadas", scrollCarrito);
        
        // Botones del carrito
        JPanel panelBotonesCarrito = new JPanel(new BorderLayout(8, 8));
        panelBotonesCarrito.setOpaque(false);
        JPanel panelBotonesSecundarios = new JPanel(new GridLayout(1, 2, 8, 0));
        panelBotonesSecundarios.setOpaque(false);
        btnQuitarCarrito = new JButton("Quitar");
        btnQuitarCarrito.setToolTipText("Quita una cantidad o todo el material seleccionado");
        btnQuitarCarrito.addActionListener(e -> quitarDelCarrito());
        btnLimpiarCarrito = new JButton("Vaciar");
        btnLimpiarCarrito.setToolTipText("Quita todo del carrito");
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
        // El reparto entre resultados y carrito se ajusta al ancho de la pantalla
        panelIzquierdo.setMinimumSize(new Dimension(420, 200));
        panelDerecho.setMinimumSize(new Dimension(300, 200));
        splitPane.addHierarchyListener(new java.awt.event.HierarchyListener() {
            @Override
            public void hierarchyChanged(java.awt.event.HierarchyEvent e) {
                if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0 && splitPane.isShowing()) {
                    splitPane.removeHierarchyListener(this);
                    SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(0.66));
                }
            }
        });
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
        JPanel grid = new JPanel(new GridResponsivo(260, 3, 14, 14));
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
        JPanel grid = new JPanel(new GridResponsivo(260, 3, 14, 14));
        grid.setOpaque(false);
        grid.add(tarjetaAccion("Base de datos",
            "Elegir la carpeta donde se guarda toda la información y la base de datos. Cada base tiene su carpeta de fotos.",
            "Configurar", "/icons/edit.svg", () -> {
                abrirConfiguracion();
                mostrarPagina(paginaVisible);
            }));
        grid.add(tarjetaAccion("Empleado en turno",
            "Cambiar el nombre del empleado que registra préstamos y recibe remisiones.",
            "Cambiar", "/icons/edit.svg", this::configurarEmpleado));
        grid.add(tarjetaMayusculas());
        grid.add(tarjetaAccion("Salir",
            "Cerrar el sistema.",
            "Salir", "/icons/cancel.svg", () -> System.exit(0)));
        JPanel pagina = new JPanel(new BorderLayout());
        pagina.setOpaque(false);
        pagina.add(grid, BorderLayout.NORTH);
        return pagina;
    }

    /** Tarjeta con la casilla para escribir todo en mayúsculas. */
    private JPanel tarjetaMayusculas() {
        JPanel cuerpo = new JPanel(new BorderLayout(8, 12));
        cuerpo.setOpaque(false);
        JComponent lbl = UIStyles.textoAjustable(
            "Cuando está activa, todo lo que se escribe en los campos queda en MAYÚSCULAS.");
        UIStyles.textoSecundario(lbl);
        cuerpo.add(lbl, BorderLayout.CENTER);
        javax.swing.JCheckBox casilla = new javax.swing.JCheckBox("Escribir todo en MAYÚSCULAS",
            AppPreferences.isMayusculas());
        casilla.setOpaque(false);
        casilla.addActionListener(e -> {
            AppPreferences.setMayusculas(casilla.isSelected());
            Notificaciones.exito(this, casilla.isSelected()
                ? "Desde ahora los campos se escriben en mayúsculas."
                : "Los campos aceptan mayúsculas y minúsculas.");
        });
        cuerpo.add(casilla, BorderLayout.SOUTH);
        return UIStyles.createCard("Escritura en mayúsculas", cuerpo);
    }

    private JPanel tarjetaAccion(String titulo, String descripcion, String textoBoton, String icono, Runnable accion) {
        JPanel cuerpo = new JPanel(new BorderLayout(8, 12));
        cuerpo.setOpaque(false);
        JComponent lbl = UIStyles.textoAjustable(descripcion);
        UIStyles.textoSecundario(lbl);
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
        // Al iniciar se muestra el resumen del almacén
        mostrarPagina(PAG_INICIO);
    }
    
    private void abrirConfiguracion() {
        ConfiguracionDBDialog dialog = new ConfiguracionDBDialog(this);
        dialog.setVisible(true);
    }
    
    private void configurarEmpleado() {
        while (true) {
            String empleado = Alerta.pedirTexto(this,
                "Ingrese su nombre para registrar préstamos y remisiones.",
                "Empleado en turno", null);

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
            tiposNoRetorno = dbManager.obtenerTiposNoRetorno();
            totalRegistros = dbManager.contarMaterialesPrestables(busqueda);
            int totalPaginas = (int) Math.ceil(totalRegistros / (double) tamano);
            if (totalPaginas == 0) {
                totalPaginas = 1;
            }
            paginaActual = Math.max(1, Math.min(pagina, totalPaginas));
            int offset = (paginaActual - 1) * tamano;
            
            // El mismo material de varios proveedores sale una vez, con el stock sumado
            List<Herramienta> herramientas = dbManager.buscarMaterialesPrestables(busqueda, offset, tamano);
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
        
        if (tablaCarrito.isEditing()) {
            tablaCarrito.getCellEditor().stopCellEditing();
        }
        ItemCarrito item = carrito.get(filaSeleccionada);
        if (item.getCantidad() > 1) {
            // Quitar solo una parte de la cantidad
            JSpinner spinner = new JSpinner(new SpinnerNumberModel(1, 1, item.getCantidad(), 1));
            JPanel panel = new JPanel(new BorderLayout(6, 6));
            panel.add(new JLabel("<html>¿Cuántas piezas de <b>" + item.getNombre() + "</b> quiere quitar?<br>"
                + "En el carrito hay " + item.getCantidad() + " " + (item.getUnidad() != null ? item.getUnidad() : "")
                + ".</html>"), BorderLayout.NORTH);
            panel.add(spinner, BorderLayout.CENTER);
            panel.setOpaque(false);
            if (!Alerta.confirmar(this, panel, "Quitar del carrito", Alerta.Tipo.PREGUNTA, "Quitar", "Cancelar")) {
                return;
            }
            try {
                spinner.commitEdit();
            } catch (java.text.ParseException ignored) {
                // Se usa el último valor válido
            }
            int quitar = (Integer) spinner.getValue();
            if (quitar < item.getCantidad()) {
                item.setCantidad(item.getCantidad() - quitar);
                actualizarTablaCarrito();
                tablaCarrito.setRowSelectionInterval(filaSeleccionada, filaSeleccionada);
                return;
            }
        }
        carrito.remove(filaSeleccionada);
        actualizarTablaCarrito();
    }

    private void limpiarCarrito() {
        if (carrito.isEmpty()) {
            return;
        }
        if (Alerta.confirmar(this, "Se quitarán todos los materiales del carrito.",
                "¿Vaciar el carrito?", Alerta.Tipo.AVISO, "Vaciar", "Cancelar")) {
            carrito.clear();
            actualizarTablaCarrito();
        }
    }
    
    private void actualizarTablaCarrito() {
        modeloCarrito.setRowCount(0);
        for (ItemCarrito item : carrito) {
            modeloCarrito.addRow(new Object[]{
                item.getNombre(),
                item.getUnidad(),
                item.getCantidad(),
                esNoRetorno(item.getHerramienta()) ? "No retorno" : "Se devuelve"
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

    /** Material de no retorno: su tipo está marcado así en Catálogos (por omisión, "Material"). */
    private boolean esNoRetorno(Herramienta h) {
        return h.getTipo() != null && tiposNoRetorno.contains(h.getTipo().trim().toLowerCase());
    }

    private class ResultadosTableModel extends AbstractTableModel {
        private final String[] columnas = {"Nombre", "Categoría", "Tipo", "Unidad", "Stock", "Cantidad", "Agregar"};
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
                case COL_NOMBRE:
                    return h.getNombre();
                case 1:
                    return h.getCategoria();
                case COL_TIPO:
                    return esNoRetorno(h) ? h.getTipo() + " · no retorno" : h.getTipo();
                case 3:
                    return h.getUnidad();
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
            if (columnIndex == COL_STOCK || columnIndex == COL_CANTIDAD) {
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

    /** Editor de la cantidad en el carrito: de 1 al stock del material. */
    private class CantidadCarritoEditor extends AbstractCellEditor implements TableCellEditor {
        private final JSpinner spinner = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));

        @Override
        public Object getCellEditorValue() {
            try {
                spinner.commitEdit();
            } catch (java.text.ParseException ignored) {
                // Se usa el último valor válido
            }
            return spinner.getValue();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            int max = row < carrito.size() ? Math.max(1, carrito.get(row).getHerramienta().getStock()) : 9999;
            int actual = value instanceof Number ? ((Number) value).intValue() : 1;
            spinner.setModel(new SpinnerNumberModel(Math.max(1, Math.min(actual, max)), 1, max, 1));
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

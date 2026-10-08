package com.almacen.database;

import com.almacen.model.DetallePrestamo;
import com.almacen.model.ExistenciaMaterial;
import com.almacen.model.Herramienta;
import com.almacen.model.ItemCarrito;
import com.almacen.model.Prestamo;
import com.almacen.model.DevolucionItem;
import com.almacen.model.ReportePrestamoItem;
import com.almacen.model.DetalleRemision;
import com.almacen.model.Proveedor;
import com.almacen.model.Remision;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static DatabaseManager instance;
    private Connection connection;
    private String dbPath;
    
    private DatabaseManager() {
        
    }
    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }
    
    public boolean connect(String dbPath) {
        try {
            this.dbPath = dbPath;
            String url = "jdbc:sqlite:" + dbPath;
            connection = DriverManager.getConnection(url);
            createTables();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
    
    public String getDbPath() {
        return dbPath;
    }
    
    private void createTables() throws SQLException {
        // Tabla de categorías
        String createCategorias = "CREATE TABLE IF NOT EXISTS categorias (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre TEXT NOT NULL UNIQUE)";

        // Tabla de tipos (catálogo administrable igual que categorías)
        String createTipos = "CREATE TABLE IF NOT EXISTS tipos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre TEXT NOT NULL UNIQUE)";

        // Tabla de unidades de medida (pieza, caja, metro...)
        String createUnidades = "CREATE TABLE IF NOT EXISTS unidades (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre TEXT NOT NULL UNIQUE)";

        // Tabla de proveedores
        String createProveedores = "CREATE TABLE IF NOT EXISTS proveedores (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre TEXT NOT NULL UNIQUE COLLATE NOCASE, " +
                "contacto TEXT, " +
                "telefono TEXT, " +
                "fecha_registro TEXT)";

        // Tabla de herramientas / materiales. Cada registro pertenece a un proveedor:
        // el mismo material de dos proveedores distintos son dos registros con su propio stock.
        String createHerramientas = "CREATE TABLE IF NOT EXISTS herramientas (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre TEXT NOT NULL, " +
                "categoria TEXT NOT NULL, " +
                "stock INTEGER NOT NULL DEFAULT 0, " +
                "descripcion TEXT, " +
                "remision TEXT, " +
                "provedor TEXT, " +
                "estado INTEGER NOT NULL DEFAULT 1, " +
                "tipo TEXT, " +
                "unidad TEXT, " +
                "proveedor_id INTEGER REFERENCES proveedores(id), " +
                "fecha_registro TEXT)";

        // Remisiones (encabezado): documento con el que llega material de un proveedor
        String createRemisiones = "CREATE TABLE IF NOT EXISTS remisiones (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "numero_remision TEXT, " +
                "proveedor_id INTEGER NOT NULL, " +
                "obra TEXT, " +
                "envia TEXT, " +
                "recibe TEXT, " +
                "fecha TEXT NOT NULL, " +
                "fecha_registro TEXT NOT NULL, " +
                "observaciones TEXT, " +
                "FOREIGN KEY (proveedor_id) REFERENCES proveedores(id))";

        // Detalle de remisiones: cada material recibido con su cantidad y unidad
        String createDetalleRemisiones = "CREATE TABLE IF NOT EXISTS detalle_remisiones (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "remision_id INTEGER NOT NULL, " +
                "herramienta_id INTEGER NOT NULL, " +
                "nombre_material TEXT NOT NULL, " +
                "descripcion TEXT, " +
                "categoria TEXT, " +
                "tipo TEXT, " +
                "unidad TEXT, " +
                "cantidad INTEGER NOT NULL, " +
                "FOREIGN KEY (remision_id) REFERENCES remisiones(id), " +
                "FOREIGN KEY (herramienta_id) REFERENCES herramientas(id))";

        // Tabla de préstamos (encabezado)
        String createPrestamos = "CREATE TABLE IF NOT EXISTS prestamos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre_cliente TEXT NOT NULL, " +
                "nombre_empleado TEXT NOT NULL, " +
                "fecha_prestamo TEXT NOT NULL, " +
                "fecha_devolucion TEXT, " +
                "estado TEXT NOT NULL DEFAULT 'PRESTADO', " +
                "residente_sobrestante TEXT, " +
                "autorizacion INTEGER DEFAULT 0, " +
                "folio TEXT, " +
                "foto_nombre TEXT)";

        // Tabla detalle de préstamos
        String createDetallePrestamos = "CREATE TABLE IF NOT EXISTS detalle_prestamos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "prestamo_id INTEGER NOT NULL, " +
                "herramienta_id INTEGER NOT NULL, " +
                "nombre_herramienta TEXT NOT NULL, " +
                "categoria TEXT NOT NULL, " +
                "cantidad INTEGER NOT NULL, " +
                "cantidad_devuelta INTEGER NOT NULL DEFAULT 0, " +
                "FOREIGN KEY (prestamo_id) REFERENCES prestamos(id), " +
                "FOREIGN KEY (herramienta_id) REFERENCES herramientas(id))";

        // Historial de devoluciones con observación
        String createHistorialDevoluciones = "CREATE TABLE IF NOT EXISTS historial_devoluciones (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "prestamo_id INTEGER NOT NULL, " +
                "herramienta_id INTEGER NOT NULL, " +
                "nombre_herramienta TEXT NOT NULL, " +
                "cantidad_devuelta INTEGER NOT NULL, " +
                "observacion TEXT NOT NULL, " +
                "fecha TEXT NOT NULL, " +
                "foto_nombre_devolucion TEXT," +
                "FOREIGN KEY (prestamo_id) REFERENCES prestamos(id), " +
                "FOREIGN KEY (herramienta_id) REFERENCES herramientas(id))";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createCategorias);
            stmt.execute(createTipos);
            stmt.execute(createUnidades);
            stmt.execute(createProveedores);
            stmt.execute(createHerramientas);
            stmt.execute(createRemisiones);
            stmt.execute(createDetalleRemisiones);
            stmt.execute(createPrestamos);
            stmt.execute(createDetallePrestamos);
            stmt.execute(createHistorialDevoluciones);
        }

        ensureHerramientaColumns();
        migratePrestamosSiNecesario();
        ensurePrestamoColumns();
        ensureDetalleColumns();
        migrarProveedoresYUnidades();

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_herramientas_proveedor ON herramientas(proveedor_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_detalle_remisiones_remision ON detalle_remisiones(remision_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_remisiones_proveedor ON remisiones(proveedor_id)");
        }

        // Insertar datos de ejemplo si la tabla está vacía
        insertarDatosEjemplo();
        migrarNoRetorno();
    }

    /**
     * Versión 3: el tipo "Material" pasa a ser de no retorno (la mayoría del material
     * no se devuelve). Solo se hace una vez; después el usuario lo cambia en Catálogos.
     */
    private void migrarNoRetorno() throws SQLException {
        if (obtenerVersionEsquema() >= VERSION_NO_RETORNO) {
            return;
        }
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("UPDATE tipos SET no_retorno = 1 WHERE LOWER(TRIM(nombre)) = 'material'");
            stmt.execute("PRAGMA user_version = " + VERSION_NO_RETORNO);
        }
    }

    private void insertarDatosEjemplo() throws SQLException {
        String check = "SELECT COUNT(*) FROM herramientas";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(check)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String fecha = LocalDateTime.now().toString();
                String insert = "INSERT INTO herramientas (nombre, categoria, tipo, unidad, stock, descripcion, estado, fecha_registro) VALUES " +
                        "('Pala', 'Herramientas de mano', 'Herramienta', 'Pieza', 10, 'Pala de acero para excavación', 1, '" + fecha + "'), " +
                        "('Pico', 'Herramientas de mano', 'Herramienta', 'Pieza', 8, 'Pico para romper tierra y rocas', 1, '" + fecha + "'), " +
                        "('Martillo', 'Herramientas de mano', 'Herramienta', 'Pieza', 15, 'Martillo de construcción', 1, '" + fecha + "'), " +
                        "('Destornillador', 'Herramientas de mano', 'Herramienta', 'Pieza', 20, 'Destornillador Phillips', 1, '" + fecha + "'), " +
                        "('Llave inglesa', 'Herramientas de mano', 'Herramienta', 'Pieza', 12, 'Llave ajustable', 1, '" + fecha + "'), " +
                        "('Taladro', 'Herramientas eléctricas', 'Equipo', 'Pieza', 5, 'Taladro eléctrico profesional', 1, '" + fecha + "'), " +
                        "('Sierra', 'Herramientas de mano', 'Herramienta', 'Pieza', 7, 'Sierra de mano para madera', 1, '" + fecha + "'), " +
                        "('Nivel', 'Herramientas de medición', 'Herramienta', 'Pieza', 9, 'Nivel de burbuja', 1, '" + fecha + "')";
                stmt.execute(insert);
            }
        }

        insertarCatalogoEjemplo(Catalogo.CATEGORIAS,
                "Herramientas de mano", "Herramientas eléctricas", "Herramientas de medición");
        insertarCatalogoEjemplo(Catalogo.TIPOS,
                "Herramienta", "Material", "Consumible", "Equipo");
        insertarCatalogoEjemplo(Catalogo.UNIDADES,
                "Pieza", "Caja", "Paquete", "Juego", "Metro", "Litro", "Kilogramo", "Rollo", "Lámina", "Bulto", "Galón");
    }

    /**
     * Inserta valores iniciales en un catálogo vacío y sincroniza el catálogo
     * con los valores ya usados en la tabla de herramientas.
     */
    private void insertarCatalogoEjemplo(Catalogo catalogo, String... valores) throws SQLException {
        String check = "SELECT COUNT(*) FROM " + catalogo.getTabla();
        boolean vacio;
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(check)) {
            vacio = rs.next() && rs.getInt(1) == 0;
        }
        if (vacio) {
            String insert = "INSERT OR IGNORE INTO " + catalogo.getTabla() + " (nombre) VALUES (?)";
            try (PreparedStatement pstmt = connection.prepareStatement(insert)) {
                for (String v : valores) {
                    pstmt.setString(1, v);
                    pstmt.executeUpdate();
                }
            }
        }
        try (Statement stmt = connection.createStatement()) {
            String insertFromTools = "INSERT OR IGNORE INTO " + catalogo.getTabla() + " (nombre) " +
                    "SELECT DISTINCT " + catalogo.getColumna() + " FROM herramientas " +
                    "WHERE " + catalogo.getColumna() + " IS NOT NULL AND TRIM(" + catalogo.getColumna() + ") <> ''";
            stmt.execute(insertFromTools);
        }
    }

    private void ensurePrestamoColumns() throws SQLException {
        ensureColumn("prestamos", "residente_sobrestante", "TEXT");
        ensureColumn("prestamos", "autorizacion", "INTEGER DEFAULT 0");
        ensureColumn("prestamos", "folio", "TEXT");
        ensureColumn("prestamos", "foto_nombre", "TEXT");
    }

    private void ensureDetalleColumns() throws SQLException {
        ensureColumn("detalle_prestamos", "cantidad_devuelta", "INTEGER NOT NULL DEFAULT 0");
        // Material de no retorno: sale del almacén con el préstamo y no se espera de regreso
        ensureColumn("detalle_prestamos", "no_retorno", "INTEGER NOT NULL DEFAULT 0");
        ensureColumn("tipos", "no_retorno", "INTEGER NOT NULL DEFAULT 0");
    }

    private void ensureHerramientaColumns() throws SQLException {
        ensureColumn("herramientas", "estado", "INTEGER NOT NULL DEFAULT 1");
        ensureColumn("herramientas", "remision", "TEXT");
        ensureColumn("herramientas", "provedor", "TEXT");
        ensureColumn("herramientas", "tipo", "TEXT");
        ensureColumn("herramientas", "unidad", "TEXT");
        ensureColumn("herramientas", "proveedor_id", "INTEGER REFERENCES proveedores(id)");
        ensureColumn("herramientas", "fecha_registro", "TEXT");
    }

    /**
     * Migración para bases de datos existentes (versión 1.x):
     * - Los proveedores escritos como texto en la columna antigua "provedor" se pasan
     *   a la tabla proveedores y se enlazan por proveedor_id.
     * - Los registros sin unidad quedan como "Pieza" y sin tipo como "Herramienta",
     *   ya que antes el sistema solo manejaba herramientas por pieza.
     * No se borra ningún dato.
     */
    private void migrarProveedoresYUnidades() throws SQLException {
        if (obtenerVersionEsquema() >= VERSION_ESQUEMA) {
            return;
        }
        connection.setAutoCommit(false);
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("INSERT OR IGNORE INTO proveedores (nombre, fecha_registro) " +
                    "SELECT DISTINCT TRIM(provedor), '" + LocalDateTime.now() + "' FROM herramientas " +
                    "WHERE provedor IS NOT NULL AND TRIM(provedor) <> ''");
            stmt.execute("UPDATE herramientas SET proveedor_id = " +
                    "(SELECT p.id FROM proveedores p WHERE p.nombre = TRIM(herramientas.provedor)) " +
                    "WHERE proveedor_id IS NULL AND provedor IS NOT NULL AND TRIM(provedor) <> ''");
            stmt.execute("UPDATE herramientas SET unidad = 'Pieza' WHERE unidad IS NULL OR TRIM(unidad) = ''");
            stmt.execute("UPDATE herramientas SET tipo = 'Herramienta' WHERE tipo IS NULL OR TRIM(tipo) = ''");
            stmt.execute("PRAGMA user_version = " + VERSION_ESQUEMA);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    /** Versión del esquema guardada en la propia base (PRAGMA user_version). */
    private static final int VERSION_ESQUEMA = 2;
    private static final int VERSION_NO_RETORNO = 3;

    private int obtenerVersionEsquema() throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA user_version")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private void ensureColumn(String table, String column, String type) throws SQLException {
        String pragma = "PRAGMA table_info(" + table + ")";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(pragma)) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    return;
                }
            }
        }
        String alter = "ALTER TABLE " + table + " ADD COLUMN " + column + " " + type;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(alter);
        }
    }

    private void migratePrestamosSiNecesario() throws SQLException {
        if (!tableExists("prestamos")) {
            return;
        }
        if (!hasColumn("prestamos", "herramienta_id")) {
            return;
        }
        connection.setAutoCommit(false);
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE prestamos RENAME TO prestamos_old");

            String createPrestamos = "CREATE TABLE IF NOT EXISTS prestamos (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "nombre_cliente TEXT NOT NULL, " +
                    "nombre_empleado TEXT NOT NULL, " +
                    "fecha_prestamo TEXT NOT NULL, " +
                    "fecha_devolucion TEXT, " +
                    "estado TEXT NOT NULL DEFAULT 'PRESTADO', " +
                    "residente_sobrestante TEXT, " +
                    "autorizacion INTEGER DEFAULT 0, " +
                    "folio TEXT, " +
                    "foto_nombre TEXT)";
            String createDetallePrestamos = "CREATE TABLE IF NOT EXISTS detalle_prestamos (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "prestamo_id INTEGER NOT NULL, " +
                    "herramienta_id INTEGER NOT NULL, " +
                    "nombre_herramienta TEXT NOT NULL, " +
                    "categoria TEXT NOT NULL, " +
                    "cantidad INTEGER NOT NULL, " +
                    "cantidad_devuelta INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY (prestamo_id) REFERENCES prestamos(id), " +
                    "FOREIGN KEY (herramienta_id) REFERENCES herramientas(id))";
            stmt.execute(createPrestamos);
            stmt.execute(createDetallePrestamos);

            String insertPrestamos = "INSERT INTO prestamos (id, nombre_cliente, nombre_empleado, fecha_prestamo, " +
                    "fecha_devolucion, estado, residente_sobrestante, autorizacion, folio, foto_nombre) " +
                    "SELECT id, nombre_cliente, nombre_empleado, fecha_prestamo, fecha_devolucion, estado, " +
                    "residente_sobrestante, autorizacion, folio, foto_nombre FROM prestamos_old";
            stmt.execute(insertPrestamos);

            String insertDetalle = "INSERT INTO detalle_prestamos (prestamo_id, herramienta_id, nombre_herramienta, categoria, cantidad, cantidad_devuelta) " +
                    "SELECT id, herramienta_id, nombre_herramienta, categoria, cantidad, 0 FROM prestamos_old";
            stmt.execute(insertDetalle);

            stmt.execute("DROP TABLE prestamos_old");
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private boolean tableExists(String table) throws SQLException {
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name=?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, table);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean hasColumn(String table, String column) throws SQLException {
        String pragma = "PRAGMA table_info(" + table + ")";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(pragma)) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    // ===================== Herramientas / materiales =====================

    private static final String SELECT_HERRAMIENTA =
            "SELECT h.*, p.nombre AS proveedor_nombre FROM herramientas h " +
            "LEFT JOIN proveedores p ON p.id = h.proveedor_id ";

    /** Filtro de búsqueda general: nombre, categoría, tipo, unidad, proveedor, descripción o ID. */
    private static final String FILTRO_HERRAMIENTA =
            "(h.nombre LIKE ? OR h.categoria LIKE ? OR h.tipo LIKE ? OR h.unidad LIKE ? " +
            "OR p.nombre LIKE ? OR h.descripcion LIKE ? OR CAST(h.id AS TEXT) LIKE ?)";
    private static final int PARAMS_FILTRO_HERRAMIENTA = 7;

    private Herramienta mapHerramienta(ResultSet rs) throws SQLException {
        Herramienta h = new Herramienta(
            rs.getInt("id"),
            rs.getString("nombre"),
            rs.getString("categoria"),
            rs.getInt("stock"),
            rs.getString("descripcion"),
            rs.getInt("estado")
        );
        h.setTipo(rs.getString("tipo"));
        h.setUnidad(rs.getString("unidad"));
        int proveedorId = rs.getInt("proveedor_id");
        h.setProveedorId(rs.wasNull() ? null : proveedorId);
        h.setProveedorNombre(rs.getString("proveedor_nombre"));
        h.setRemision(rs.getString("remision"));
        h.setFechaRegistro(rs.getString("fecha_registro"));
        return h;
    }

    private List<Herramienta> listarHerramientas(PreparedStatement pstmt) throws SQLException {
        List<Herramienta> herramientas = new ArrayList<>();
        try (ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                herramientas.add(mapHerramienta(rs));
            }
        }
        return herramientas;
    }

    private int setFiltroHerramienta(PreparedStatement pstmt, int indice, String filtro) throws SQLException {
        String pattern = "%" + filtro.trim() + "%";
        for (int i = 0; i < PARAMS_FILTRO_HERRAMIENTA; i++) {
            pstmt.setString(indice++, pattern);
        }
        return indice;
    }

    private static void setInteger(PreparedStatement pstmt, int indice, Integer valor) throws SQLException {
        if (valor == null) {
            pstmt.setNull(indice, Types.INTEGER);
        } else {
            pstmt.setInt(indice, valor);
        }
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    public List<Herramienta> buscarHerramientas(String busqueda) throws SQLException {
        if (vacio(busqueda)) {
            String sql = SELECT_HERRAMIENTA + "WHERE h.estado = 1 ORDER BY h.nombre ASC, p.nombre ASC";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                return listarHerramientas(pstmt);
            }
        }
        String sql = SELECT_HERRAMIENTA + "WHERE h.estado = 1 AND " + FILTRO_HERRAMIENTA +
                " ORDER BY h.nombre ASC, p.nombre ASC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            setFiltroHerramienta(pstmt, 1, busqueda);
            return listarHerramientas(pstmt);
        }
    }

    public int contarHerramientas() throws SQLException {
        String sql = "SELECT COUNT(*) FROM herramientas WHERE estado = 1";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public int contarHerramientasFiltradas(String filtro, int estado) throws SQLException {
        String sql = "SELECT COUNT(*) FROM herramientas h LEFT JOIN proveedores p ON p.id = h.proveedor_id " +
                "WHERE h.estado = ?" + (vacio(filtro) ? "" : " AND " + FILTRO_HERRAMIENTA);
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, estado);
            if (!vacio(filtro)) {
                setFiltroHerramienta(pstmt, 2, filtro);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public List<Herramienta> obtenerHerramientasPaginadasFiltradas(String filtro, int estado, int offset, int limit) throws SQLException {
        String sql = SELECT_HERRAMIENTA + "WHERE h.estado = ?" + (vacio(filtro) ? "" : " AND " + FILTRO_HERRAMIENTA) +
                " ORDER BY h.id DESC LIMIT ? OFFSET ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            int i = 1;
            pstmt.setInt(i++, estado);
            if (!vacio(filtro)) {
                i = setFiltroHerramienta(pstmt, i, filtro);
            }
            pstmt.setInt(i++, limit);
            pstmt.setInt(i, offset);
            return listarHerramientas(pstmt);
        }
    }

    /**
     * Conteo para la pantalla de inventario: filtra por texto general y opcionalmente por proveedor.
     */
    public int contarInventario(String filtro, Integer proveedorId, int estado) throws SQLException {
        String sql = "SELECT COUNT(*) FROM herramientas h LEFT JOIN proveedores p ON p.id = h.proveedor_id " +
                "WHERE h.estado = ?" +
                (proveedorId != null ? " AND h.proveedor_id = ?" : "") +
                (vacio(filtro) ? "" : " AND " + FILTRO_HERRAMIENTA);
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            int i = 1;
            pstmt.setInt(i++, estado);
            if (proveedorId != null) {
                pstmt.setInt(i++, proveedorId);
            }
            if (!vacio(filtro)) {
                setFiltroHerramienta(pstmt, i, filtro);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public List<Herramienta> obtenerInventarioPaginado(String filtro, Integer proveedorId, int estado, int offset, int limit) throws SQLException {
        String sql = SELECT_HERRAMIENTA + "WHERE h.estado = ?" +
                (proveedorId != null ? " AND h.proveedor_id = ?" : "") +
                (vacio(filtro) ? "" : " AND " + FILTRO_HERRAMIENTA) +
                " ORDER BY h.nombre ASC, p.nombre ASC LIMIT ? OFFSET ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            int i = 1;
            pstmt.setInt(i++, estado);
            if (proveedorId != null) {
                pstmt.setInt(i++, proveedorId);
            }
            if (!vacio(filtro)) {
                i = setFiltroHerramienta(pstmt, i, filtro);
            }
            pstmt.setInt(i++, limit);
            pstmt.setInt(i, offset);
            return listarHerramientas(pstmt);
        }
    }

    public int contarHerramientasBusqueda(String busqueda) throws SQLException {
        return contarHerramientasFiltradas(busqueda, 1);
    }

    public List<Herramienta> buscarHerramientasPaginadas(String busqueda, int offset, int limit) throws SQLException {
        String sql = SELECT_HERRAMIENTA + "WHERE h.estado = 1" + (vacio(busqueda) ? "" : " AND " + FILTRO_HERRAMIENTA) +
                " ORDER BY h.nombre ASC, p.nombre ASC LIMIT ? OFFSET ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            int i = 1;
            if (!vacio(busqueda)) {
                i = setFiltroHerramienta(pstmt, i, busqueda);
            }
            pstmt.setInt(i++, limit);
            pstmt.setInt(i, offset);
            return listarHerramientas(pstmt);
        }
    }

    public int contarHerramientasBaja() throws SQLException {
        String sql = "SELECT COUNT(*) FROM herramientas WHERE estado = 0";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public Herramienta obtenerHerramientaPorId(int id) throws SQLException {
        String sql = SELECT_HERRAMIENTA + "WHERE h.id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            List<Herramienta> lista = listarHerramientas(pstmt);
            return lista.isEmpty() ? null : lista.get(0);
        }
    }

    /**
     * Busca un material por nombre (sin distinguir mayúsculas ni espacios extra) que
     * pertenezca al proveedor indicado. Primero devuelve el registro activo.
     */
    public Herramienta buscarMaterialDeProveedor(String nombre, Integer proveedorId) throws SQLException {
        String sql = SELECT_HERRAMIENTA +
                "WHERE LOWER(TRIM(h.nombre)) = LOWER(TRIM(?)) AND h.proveedor_id IS ? " +
                "ORDER BY h.estado DESC, h.id ASC LIMIT 1";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            setInteger(pstmt, 2, proveedorId);
            List<Herramienta> lista = listarHerramientas(pstmt);
            return lista.isEmpty() ? null : lista.get(0);
        }
    }

    /** Materiales (activos) que pertenecen a un proveedor. */
    public List<Herramienta> obtenerMaterialesPorProveedor(int proveedorId) throws SQLException {
        String sql = SELECT_HERRAMIENTA + "WHERE h.estado = 1 AND h.proveedor_id = ? ORDER BY h.nombre ASC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, proveedorId);
            return listarHerramientas(pstmt);
        }
    }

    /**
     * Inventario completo para el reporte general: incluye la cantidad que está
     * actualmente prestada de cada material.
     */
    public List<Herramienta> obtenerInventarioGeneral(Integer proveedorId) throws SQLException {
        return obtenerInventarioConPrestado(proveedorId, null);
    }

    /** Registros activos de un material (todos sus proveedores) con lo prestado de cada uno. */
    public List<Herramienta> obtenerExistenciasDeMaterial(String nombre) throws SQLException {
        return obtenerInventarioConPrestado(null, nombre);
    }

    private List<Herramienta> obtenerInventarioConPrestado(Integer proveedorId, String nombre) throws SQLException {
        String sql = "SELECT h.*, p.nombre AS proveedor_nombre, " + PRESTADO_HERRAMIENTA + " AS prestado " +
                "FROM herramientas h LEFT JOIN proveedores p ON p.id = h.proveedor_id " +
                "WHERE h.estado = 1" + (proveedorId != null ? " AND h.proveedor_id = ?" : "") +
                (vacio(nombre) ? "" : " AND LOWER(TRIM(h.nombre)) = LOWER(TRIM(?))") +
                " ORDER BY h.categoria ASC, h.nombre ASC, p.nombre ASC";
        List<Herramienta> lista = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            int i = 1;
            if (proveedorId != null) {
                pstmt.setInt(i++, proveedorId);
            }
            if (!vacio(nombre)) {
                pstmt.setString(i, nombre);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Herramienta h = mapHerramienta(rs);
                    h.setCantidadPrestada(rs.getInt("prestado"));
                    lista.add(h);
                }
            }
        }
        return lista;
    }

    // ----- Existencias acumuladas por material (todos los proveedores)

    /** Cantidad prestada actualmente de la herramienta h (préstamos sin devolver). */
    private static final String PRESTADO_HERRAMIENTA =
            "COALESCE((SELECT SUM(d.cantidad - d.cantidad_devuelta) FROM detalle_prestamos d " +
            "JOIN prestamos pr ON pr.id = d.prestamo_id " +
            "WHERE d.herramienta_id = h.id AND pr.estado = 'PRESTADO' AND d.no_retorno = 0), 0)";

    /** Mismo material = mismo nombre (sin mayúsculas ni espacios extra) y misma unidad. */
    private static final String CLAVE_MATERIAL =
            "LOWER(TRIM(h.nombre)), LOWER(TRIM(COALESCE(h.unidad, '')))";

    private static String havingAgrupado(String filtro, Integer proveedorId) {
        List<String> condiciones = new ArrayList<>();
        if (proveedorId != null) {
            condiciones.add("SUM(CASE WHEN h.proveedor_id = ? THEN 1 ELSE 0 END) > 0");
        }
        if (!vacio(filtro)) {
            condiciones.add("SUM(CASE WHEN " + FILTRO_HERRAMIENTA + " THEN 1 ELSE 0 END) > 0");
        }
        return condiciones.isEmpty() ? "" : " HAVING " + String.join(" AND ", condiciones);
    }

    private int setHavingAgrupado(PreparedStatement pstmt, int i, String filtro, Integer proveedorId) throws SQLException {
        if (proveedorId != null) {
            pstmt.setInt(i++, proveedorId);
        }
        if (!vacio(filtro)) {
            i = setFiltroHerramienta(pstmt, i, filtro);
        }
        return i;
    }

    public int contarInventarioAgrupado(String filtro, Integer proveedorId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM (SELECT 1 FROM herramientas h " +
                "LEFT JOIN proveedores p ON p.id = h.proveedor_id WHERE h.estado = 1 " +
                "GROUP BY " + CLAVE_MATERIAL + havingAgrupado(filtro, proveedorId) + ")";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            setHavingAgrupado(pstmt, 1, filtro, proveedorId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Existencias sumadas por material: el mismo material de varios proveedores se acumula
     * en una fila, con el desglose de cuánto tiene cada proveedor. Los filtros eligen qué
     * materiales mostrar, pero el total siempre incluye a todos sus proveedores.
     */
    public List<ExistenciaMaterial> obtenerInventarioAgrupado(String filtro, Integer proveedorId,
                                                              int offset, int limit) throws SQLException {
        String sql = "SELECT MIN(h.nombre) AS nombre, MIN(h.unidad) AS unidad, MIN(h.categoria) AS categoria, " +
                "MIN(h.tipo) AS tipo, COUNT(*) AS num_proveedores, " +
                "GROUP_CONCAT(COALESCE(p.nombre, 'Sin proveedor') || ': ' || h.stock, ' | ') AS desglose, " +
                "SUM(h.stock) AS disponible, SUM(" + PRESTADO_HERRAMIENTA + ") AS prestado, " +
                "MAX(h.fecha_registro) AS ultima_fecha " +
                "FROM herramientas h LEFT JOIN proveedores p ON p.id = h.proveedor_id WHERE h.estado = 1 " +
                "GROUP BY " + CLAVE_MATERIAL + havingAgrupado(filtro, proveedorId) +
                " ORDER BY LOWER(MIN(h.nombre)) ASC LIMIT ? OFFSET ?";
        List<ExistenciaMaterial> lista = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            int i = setHavingAgrupado(pstmt, 1, filtro, proveedorId);
            pstmt.setInt(i++, limit);
            pstmt.setInt(i, offset);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ExistenciaMaterial e = new ExistenciaMaterial();
                    e.setNombre(rs.getString("nombre"));
                    e.setUnidad(rs.getString("unidad"));
                    e.setCategoria(rs.getString("categoria"));
                    e.setTipo(rs.getString("tipo"));
                    e.setNumProveedores(rs.getInt("num_proveedores"));
                    e.setDesglose(rs.getString("desglose"));
                    e.setDisponible(rs.getInt("disponible"));
                    e.setPrestado(rs.getInt("prestado"));
                    e.setUltimaFecha(rs.getString("ultima_fecha"));
                    lista.add(e);
                }
            }
        }
        return lista;
    }

    /** Todas las existencias acumuladas por material (para el reporte general). */
    public List<ExistenciaMaterial> obtenerInventarioAgrupado() throws SQLException {
        return obtenerInventarioAgrupado(null, null, 0, Integer.MAX_VALUE);
    }

    /** Nombres de los materiales activos, sin repetir aunque los surtan varios proveedores. */
    public List<String> obtenerNombresMateriales() throws SQLException {
        String sql = "SELECT MIN(TRIM(nombre)) AS nombre FROM herramientas WHERE estado = 1 " +
                "GROUP BY LOWER(TRIM(nombre)) ORDER BY LOWER(MIN(TRIM(nombre))) ASC";
        List<String> nombres = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                nombres.add(rs.getString("nombre"));
            }
        }
        return nombres;
    }

    /** Materiales activos de todos los proveedores. */
    public List<Herramienta> obtenerMaterialesActivos() throws SQLException {
        String sql = SELECT_HERRAMIENTA + "WHERE h.estado = 1 ORDER BY h.nombre ASC, p.nombre ASC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            return listarHerramientas(pstmt);
        }
    }

    public void actualizarStock(int herramientaId, int cantidad) throws SQLException {
        String sql = "UPDATE herramientas SET stock = stock + ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, cantidad);
            pstmt.setInt(2, herramientaId);
            pstmt.executeUpdate();
        }
    }

    public int agregarHerramienta(Herramienta herramienta) throws SQLException {
        String sql = "INSERT INTO herramientas (nombre, categoria, tipo, unidad, proveedor_id, provedor, remision, " +
                "stock, descripcion, estado, fecha_registro) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, herramienta.getNombre());
            pstmt.setString(2, herramienta.getCategoria());
            pstmt.setString(3, herramienta.getTipo());
            pstmt.setString(4, herramienta.getUnidad());
            setInteger(pstmt, 5, herramienta.getProveedorId());
            pstmt.setString(6, herramienta.getProveedorNombre());
            pstmt.setString(7, herramienta.getRemision());
            pstmt.setInt(8, herramienta.getStock());
            pstmt.setString(9, herramienta.getDescripcion());
            pstmt.setInt(10, herramienta.getEstado());
            pstmt.setString(11, herramienta.getFechaRegistro() != null
                    ? herramienta.getFechaRegistro() : LocalDateTime.now().toString());
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    herramienta.setId(id);
                    return id;
                }
            }
        }
        throw new SQLException("No se pudo generar el ID del material");
    }

    public void actualizarHerramienta(Herramienta herramienta) throws SQLException {
        String sql = "UPDATE herramientas SET nombre = ?, categoria = ?, tipo = ?, unidad = ?, proveedor_id = ?, " +
                "provedor = ?, stock = ?, descripcion = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, herramienta.getNombre());
            pstmt.setString(2, herramienta.getCategoria());
            pstmt.setString(3, herramienta.getTipo());
            pstmt.setString(4, herramienta.getUnidad());
            setInteger(pstmt, 5, herramienta.getProveedorId());
            pstmt.setString(6, herramienta.getProveedorNombre());
            pstmt.setInt(7, herramienta.getStock());
            pstmt.setString(8, herramienta.getDescripcion());
            pstmt.setInt(9, herramienta.getId());
            pstmt.executeUpdate();
        }
    }

    public void cambiarEstadoHerramienta(int herramientaId, int estado) throws SQLException {
        String sql = "UPDATE herramientas SET estado = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, estado);
            pstmt.setInt(2, herramientaId);
            pstmt.executeUpdate();
        }
    }

    // ===================== Catálogos: categorías, tipos y unidades =====================

    public List<String> obtenerCatalogo(Catalogo catalogo) throws SQLException {
        List<String> valores = new ArrayList<>();
        String sql = "SELECT nombre FROM " + catalogo.getTabla() + " ORDER BY nombre COLLATE NOCASE ASC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                valores.add(rs.getString("nombre"));
            }
        }
        return valores;
    }

    /** Devuelve false si el valor ya existía. */
    public boolean agregarCatalogo(Catalogo catalogo, String nombre) throws SQLException {
        String sql = "INSERT OR IGNORE INTO " + catalogo.getTabla() + " (nombre) VALUES (?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nombre.trim());
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Renombra un valor del catálogo y lo actualiza en todos los materiales,
     * remisiones y préstamos que lo usan.
     */
    public boolean renombrarCatalogo(Catalogo catalogo, String nombreAnterior, String nombreNuevo) throws SQLException {
        connection.setAutoCommit(false);
        try {
            String updateCat = "UPDATE " + catalogo.getTabla() + " SET nombre = ? WHERE nombre = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(updateCat)) {
                pstmt.setString(1, nombreNuevo);
                pstmt.setString(2, nombreAnterior);
                int updated = pstmt.executeUpdate();
                if (updated == 0) {
                    connection.rollback();
                    return false;
                }
            }
            String col = catalogo.getColumna();
            String[] tablas = catalogo == Catalogo.CATEGORIAS
                    ? new String[]{"herramientas", "detalle_remisiones", "detalle_prestamos"}
                    : new String[]{"herramientas", "detalle_remisiones"};
            for (String tabla : tablas) {
                String update = "UPDATE " + tabla + " SET " + col + " = ? WHERE " + col + " = ?";
                try (PreparedStatement pstmt = connection.prepareStatement(update)) {
                    pstmt.setString(1, nombreNuevo);
                    pstmt.setString(2, nombreAnterior);
                    pstmt.executeUpdate();
                }
            }
            connection.commit();
            return true;
        } catch (SQLException e) {
            connection.rollback();
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("unique")) {
                return false;
            }
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    /** Nombres (en minúsculas) de los tipos marcados como material de no retorno. */
    public java.util.Set<String> obtenerTiposNoRetorno() throws SQLException {
        java.util.Set<String> tipos = new java.util.HashSet<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT nombre FROM tipos WHERE no_retorno = 1")) {
            while (rs.next()) {
                tipos.add(rs.getString("nombre").trim().toLowerCase());
            }
        }
        return tipos;
    }

    public void setTipoNoRetorno(String tipo, boolean noRetorno) throws SQLException {
        try (PreparedStatement pstmt = connection.prepareStatement(
                "UPDATE tipos SET no_retorno = ? WHERE nombre = ?")) {
            pstmt.setInt(1, noRetorno ? 1 : 0);
            pstmt.setString(2, tipo);
            pstmt.executeUpdate();
        }
    }

    public List<String> obtenerCategorias() throws SQLException {
        return obtenerCatalogo(Catalogo.CATEGORIAS);
    }

    public boolean agregarCategoria(String nombre) throws SQLException {
        return agregarCatalogo(Catalogo.CATEGORIAS, nombre);
    }

    public boolean actualizarCategoria(String nombreAnterior, String nombreNuevo) throws SQLException {
        return renombrarCatalogo(Catalogo.CATEGORIAS, nombreAnterior, nombreNuevo);
    }

    
        // Métodos para préstamos
    private int insertarPrestamo(Prestamo prestamo) throws SQLException {
        String sql = "INSERT INTO prestamos (nombre_cliente, nombre_empleado, fecha_prestamo, estado, " +
                "residente_sobrestante, autorizacion, folio, foto_nombre) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, prestamo.getNombreCliente());
            pstmt.setString(2, prestamo.getNombreEmpleado());
            pstmt.setString(3, prestamo.getFechaPrestamo().toString());
            pstmt.setString(4, prestamo.getEstado());
            pstmt.setString(5, prestamo.getResidenteSobrestante());
            pstmt.setInt(6, prestamo.isAutorizacion() ? 1 : 0);
            pstmt.setString(7, prestamo.getFolio());
            pstmt.setString(8, prestamo.getFotoNombre());
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("No se pudo generar el ID del préstamo");
    }

    /**
     * Registra un material del carrito. El carrito junta el mismo material de todos los
     * proveedores, así que la cantidad se toma de sus registros empezando por el que tiene
     * más stock. Devuelve true si es material de retorno (hay que devolverlo).
     */
    private boolean insertarDetallePrestamo(int prestamoId, ItemCarrito item) throws SQLException {
        String select = "SELECT h.id, h.nombre, h.categoria, h.stock, COALESCE(t.no_retorno, 0) AS no_retorno " +
                "FROM herramientas h LEFT JOIN tipos t ON LOWER(TRIM(t.nombre)) = LOWER(TRIM(h.tipo)) " +
                "WHERE h.estado = 1 AND h.stock > 0 AND " + MISMO_MATERIAL +
                " ORDER BY h.stock DESC, h.id ASC";
        String insert = "INSERT INTO detalle_prestamos (prestamo_id, herramienta_id, nombre_herramienta, categoria, " +
                "cantidad, cantidad_devuelta, no_retorno) VALUES (?, ?, ?, ?, ?, 0, ?)";
        int restante = item.getCantidad();
        boolean retorno = false;
        try (PreparedStatement pstmt = connection.prepareStatement(select)) {
            pstmt.setString(1, item.getNombre());
            pstmt.setString(2, item.getUnidad());
            try (ResultSet rs = pstmt.executeQuery();
                 PreparedStatement ins = connection.prepareStatement(insert)) {
                while (restante > 0 && rs.next()) {
                    int tomar = Math.min(restante, rs.getInt("stock"));
                    int id = rs.getInt("id");
                    boolean noRetorno = rs.getInt("no_retorno") == 1;
                    ins.setInt(1, prestamoId);
                    ins.setInt(2, id);
                    ins.setString(3, rs.getString("nombre"));
                    ins.setString(4, rs.getString("categoria") != null ? rs.getString("categoria") : "");
                    ins.setInt(5, tomar);
                    ins.setInt(6, noRetorno ? 1 : 0);
                    ins.executeUpdate();
                    actualizarStock(id, -tomar);
                    retorno |= !noRetorno;
                    restante -= tomar;
                }
            }
        }
        if (restante > 0) {
            throw new SQLException("No hay suficiente stock de " + item.getNombre() + ": faltan " + restante);
        }
        return retorno;
    }

    /** Mismo material (nombre y unidad) sin importar el proveedor; parámetros: nombre, unidad. */
    private static final String MISMO_MATERIAL =
            "LOWER(TRIM(h.nombre)) = LOWER(TRIM(?)) AND LOWER(TRIM(COALESCE(h.unidad, ''))) = LOWER(TRIM(COALESCE(?, '')))";

    /** Stock total de un material sumando todos sus proveedores. */
    public int obtenerStockMaterial(String nombre, String unidad) throws SQLException {
        String sql = "SELECT COALESCE(SUM(h.stock), 0) FROM herramientas h WHERE h.estado = 1 AND " + MISMO_MATERIAL;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            pstmt.setString(2, unidad);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static final String SELECT_PRESTABLE =
            "SELECT MIN(h.id) AS id, MIN(TRIM(h.nombre)) AS nombre, MIN(h.categoria) AS categoria, " +
            "MIN(h.tipo) AS tipo, MIN(h.unidad) AS unidad, SUM(h.stock) AS stock " +
            "FROM herramientas h LEFT JOIN proveedores p ON p.id = h.proveedor_id WHERE h.estado = 1";

    /** Cuántos materiales distintos (sumando proveedores) coinciden con la búsqueda de préstamo. */
    public int contarMaterialesPrestables(String busqueda) throws SQLException {
        // La búsqueda va en HAVING para que el stock sume todos los proveedores aunque
        // el texto solo coincida con uno de ellos
        String sql = "SELECT COUNT(*) FROM (" + SELECT_PRESTABLE +
                " GROUP BY " + CLAVE_MATERIAL + havingAgrupado(busqueda, null) + ")";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            setHavingAgrupado(pstmt, 1, busqueda, null);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Materiales para préstamo: el mismo material de varios proveedores aparece una sola vez
     * con el stock sumado.
     */
    public List<Herramienta> buscarMaterialesPrestables(String busqueda, int offset, int limit) throws SQLException {
        String sql = SELECT_PRESTABLE + " GROUP BY " + CLAVE_MATERIAL + havingAgrupado(busqueda, null) + " ORDER BY LOWER(MIN(TRIM(h.nombre))) ASC LIMIT ? OFFSET ?";
        List<Herramienta> lista = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            int i = setHavingAgrupado(pstmt, 1, busqueda, null);
            pstmt.setInt(i++, limit);
            pstmt.setInt(i, offset);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Herramienta h = new Herramienta(rs.getInt("id"), rs.getString("nombre"),
                        rs.getString("categoria"), rs.getInt("stock"), null, 1);
                    h.setTipo(rs.getString("tipo"));
                    h.setUnidad(rs.getString("unidad"));
                    lista.add(h);
                }
            }
        }
        return lista;
    }
    
    public void crearPrestamo(Prestamo prestamo) throws SQLException {
        connection.setAutoCommit(false);
        try {
            insertarPrestamo(prestamo);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }
    
    public void crearPrestamos(List<Prestamo> prestamos) throws SQLException {
        connection.setAutoCommit(false);
        try {
            for (Prestamo prestamo : prestamos) {
                insertarPrestamo(prestamo);
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public void crearPrestamoConDetalles(Prestamo prestamo, List<ItemCarrito> items) throws SQLException {
        connection.setAutoCommit(false);
        try {
            int prestamoId = insertarPrestamo(prestamo);
            boolean hayRetorno = false;
            for (ItemCarrito item : items) {
                hayRetorno |= insertarDetallePrestamo(prestamoId, item);
            }
            if (!hayRetorno) {
                // Solo material de no retorno: se entrega y no queda nada pendiente
                try (PreparedStatement pstmt = connection.prepareStatement(
                        "UPDATE prestamos SET estado = 'ENTREGADO', fecha_devolucion = fecha_prestamo WHERE id = ?")) {
                    pstmt.setInt(1, prestamoId);
                    pstmt.executeUpdate();
                }
                prestamo.setEstado("ENTREGADO");
            }
            prestamo.setId(prestamoId);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }
    
    public List<Prestamo> obtenerPrestamos() throws SQLException {
        List<Prestamo> prestamos = new ArrayList<>();
        String sql = "SELECT * FROM prestamos ORDER BY fecha_prestamo DESC";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Prestamo prestamo = new Prestamo();
                prestamo.setId(rs.getInt("id"));
                prestamo.setNombreCliente(rs.getString("nombre_cliente"));
                prestamo.setNombreEmpleado(rs.getString("nombre_empleado"));
                
                String fechaPrestamo = rs.getString("fecha_prestamo");
                if (fechaPrestamo != null) {
                    prestamo.setFechaPrestamo(LocalDateTime.parse(fechaPrestamo));
                }
                
                String fechaDevolucion = rs.getString("fecha_devolucion");
                if (fechaDevolucion != null) {
                    prestamo.setFechaDevolucion(LocalDateTime.parse(fechaDevolucion));
                }
                
                prestamo.setEstado(rs.getString("estado"));
                prestamo.setResidenteSobrestante(rs.getString("residente_sobrestante"));
                prestamo.setAutorizacion(rs.getInt("autorizacion") == 1);
                prestamo.setFolio(rs.getString("folio"));
                prestamo.setFotoNombre(rs.getString("foto_nombre"));
                prestamos.add(prestamo);
            }
        }
        return prestamos;
    }

    public int contarPrestamos() throws SQLException {
        String sql = "SELECT COUNT(*) FROM prestamos";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public int contarPrestamosPorEstado(String estado) throws SQLException {
        String sql = "SELECT COUNT(*) FROM prestamos WHERE estado = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, estado);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public int contarPrestamosPorEstadoFiltrados(String estado, String filtro) throws SQLException {
        if (filtro == null || filtro.trim().isEmpty()) {
            return contarPrestamosPorEstado(estado);
        }
        String sql = "SELECT COUNT(*) FROM prestamos WHERE estado = ? AND (" +
                "nombre_cliente LIKE ? OR nombre_empleado LIKE ? OR residente_sobrestante LIKE ? OR folio LIKE ? OR CAST(id AS TEXT) LIKE ?" +
                ")";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            String pattern = "%" + filtro.trim() + "%";
            pstmt.setString(1, estado);
            pstmt.setString(2, pattern);
            pstmt.setString(3, pattern);
            pstmt.setString(4, pattern);
            pstmt.setString(5, pattern);
            pstmt.setString(6, pattern);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public List<Prestamo> obtenerPrestamosPaginadosPorEstado(String estado, int offset, int limit) throws SQLException {
        List<Prestamo> prestamos = new ArrayList<>();
        String sql = "SELECT * FROM prestamos WHERE estado = ? ORDER BY fecha_prestamo DESC LIMIT ? OFFSET ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, estado);
            pstmt.setInt(2, limit);
            pstmt.setInt(3, offset);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Prestamo prestamo = new Prestamo();
                    prestamo.setId(rs.getInt("id"));
                    prestamo.setNombreCliente(rs.getString("nombre_cliente"));
                    prestamo.setNombreEmpleado(rs.getString("nombre_empleado"));
                    String fechaPrestamo = rs.getString("fecha_prestamo");
                    if (fechaPrestamo != null) {
                        prestamo.setFechaPrestamo(LocalDateTime.parse(fechaPrestamo));
                    }
                    String fechaDevolucion = rs.getString("fecha_devolucion");
                    if (fechaDevolucion != null) {
                        prestamo.setFechaDevolucion(LocalDateTime.parse(fechaDevolucion));
                    }
                    prestamo.setEstado(rs.getString("estado"));
                    prestamo.setResidenteSobrestante(rs.getString("residente_sobrestante"));
                    prestamo.setAutorizacion(rs.getInt("autorizacion") == 1);
                    prestamo.setFolio(rs.getString("folio"));
                    prestamo.setFotoNombre(rs.getString("foto_nombre"));
                    prestamos.add(prestamo);
                }
            }
        }
        return prestamos;
    }

    public List<Prestamo> obtenerPrestamosPaginadosPorEstadoFiltrados(String estado, String filtro, int offset, int limit) throws SQLException {
        List<Prestamo> prestamos = new ArrayList<>();
        if (filtro == null || filtro.trim().isEmpty()) {
            return obtenerPrestamosPaginadosPorEstado(estado, offset, limit);
        }
        String sql = "SELECT * FROM prestamos WHERE estado = ? AND (" +
                "nombre_cliente LIKE ? OR nombre_empleado LIKE ? OR residente_sobrestante LIKE ? OR folio LIKE ? OR CAST(id AS TEXT) LIKE ?" +
                ") ORDER BY fecha_prestamo DESC LIMIT ? OFFSET ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            String pattern = "%" + filtro.trim() + "%";
            pstmt.setString(1, estado);
            pstmt.setString(2, pattern);
            pstmt.setString(3, pattern);
            pstmt.setString(4, pattern);
            pstmt.setString(5, pattern);
            pstmt.setString(6, pattern);
            pstmt.setInt(7, limit);
            pstmt.setInt(8, offset);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Prestamo prestamo = new Prestamo();
                    prestamo.setId(rs.getInt("id"));
                    prestamo.setNombreCliente(rs.getString("nombre_cliente"));
                    prestamo.setNombreEmpleado(rs.getString("nombre_empleado"));
                    String fechaPrestamo = rs.getString("fecha_prestamo");
                    if (fechaPrestamo != null) {
                        prestamo.setFechaPrestamo(LocalDateTime.parse(fechaPrestamo));
                    }
                    String fechaDevolucion = rs.getString("fecha_devolucion");
                    if (fechaDevolucion != null) {
                        prestamo.setFechaDevolucion(LocalDateTime.parse(fechaDevolucion));
                    }
                    prestamo.setEstado(rs.getString("estado"));
                    prestamo.setResidenteSobrestante(rs.getString("residente_sobrestante"));
                    prestamo.setAutorizacion(rs.getInt("autorizacion") == 1);
                    prestamo.setFolio(rs.getString("folio"));
                    prestamo.setFotoNombre(rs.getString("foto_nombre"));
                    prestamos.add(prestamo);
                }
            }
        }
        return prestamos;
    }

    public int contarPrestamosPorEstadoConFiltros(String estado, String cliente, String empleado, String residente, String fechaPrestamo) throws SQLException {
        String sql = "SELECT COUNT(*) FROM prestamos WHERE " + ESTADO_LISTA + " = ? " +
                "AND (nombre_cliente LIKE ?) AND (nombre_empleado LIKE ?) AND (residente_sobrestante LIKE ?) AND (fecha_prestamo LIKE ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, estado);
            pstmt.setString(2, "%" + safeLike(cliente) + "%");
            pstmt.setString(3, "%" + safeLike(empleado) + "%");
            pstmt.setString(4, "%" + safeLike(residente) + "%");
            pstmt.setString(5, "%" + safeLike(fechaPrestamo) + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public List<Prestamo> obtenerPrestamosPaginadosPorEstadoConFiltros(String estado, String cliente, String empleado,
                                                                       String residente, String fechaPrestamo,
                                                                       int offset, int limit) throws SQLException {
        List<Prestamo> prestamos = new ArrayList<>();
        String sql = "SELECT * FROM prestamos WHERE " + ESTADO_LISTA + " = ? " +
                "AND (nombre_cliente LIKE ?) AND (nombre_empleado LIKE ?) AND (residente_sobrestante LIKE ?) AND (fecha_prestamo LIKE ?) " +
                "ORDER BY fecha_prestamo DESC LIMIT ? OFFSET ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, estado);
            pstmt.setString(2, "%" + safeLike(cliente) + "%");
            pstmt.setString(3, "%" + safeLike(empleado) + "%");
            pstmt.setString(4, "%" + safeLike(residente) + "%");
            pstmt.setString(5, "%" + safeLike(fechaPrestamo) + "%");
            pstmt.setInt(6, limit);
            pstmt.setInt(7, offset);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Prestamo prestamo = new Prestamo();
                    prestamo.setId(rs.getInt("id"));
                    prestamo.setNombreCliente(rs.getString("nombre_cliente"));
                    prestamo.setNombreEmpleado(rs.getString("nombre_empleado"));
                    String fechaP = rs.getString("fecha_prestamo");
                    if (fechaP != null) {
                        prestamo.setFechaPrestamo(LocalDateTime.parse(fechaP));
                    }
                    String fechaD = rs.getString("fecha_devolucion");
                    if (fechaD != null) {
                        prestamo.setFechaDevolucion(LocalDateTime.parse(fechaD));
                    }
                    prestamo.setEstado(rs.getString("estado"));
                    prestamo.setResidenteSobrestante(rs.getString("residente_sobrestante"));
                    prestamo.setAutorizacion(rs.getInt("autorizacion") == 1);
                    prestamo.setFolio(rs.getString("folio"));
                    prestamo.setFotoNombre(rs.getString("foto_nombre"));
                    prestamos.add(prestamo);
                }
            }
        }
        return prestamos;
    }

    /** Los préstamos de puro material de no retorno (ENTREGADO) se listan con los devueltos. */
    private static final String ESTADO_LISTA = "(CASE WHEN estado = 'ENTREGADO' THEN 'DEVUELTO' ELSE estado END)";

    private String safeLike(String value) {
        return value == null ? "" : value.trim();
    }

    public boolean tieneDevolucionesParciales(int prestamoId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM detalle_prestamos " +
                "WHERE prestamo_id = ? AND no_retorno = 0 AND cantidad_devuelta > 0 AND cantidad_devuelta < cantidad";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, prestamoId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public List<Prestamo> obtenerPrestamosPaginados(int offset, int limit) throws SQLException {
        List<Prestamo> prestamos = new ArrayList<>();
        String sql = "SELECT * FROM prestamos ORDER BY fecha_prestamo DESC LIMIT ? OFFSET ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Prestamo prestamo = new Prestamo();
                    prestamo.setId(rs.getInt("id"));
                    prestamo.setNombreCliente(rs.getString("nombre_cliente"));
                    prestamo.setNombreEmpleado(rs.getString("nombre_empleado"));
                    String fechaPrestamo = rs.getString("fecha_prestamo");
                    if (fechaPrestamo != null) {
                        prestamo.setFechaPrestamo(LocalDateTime.parse(fechaPrestamo));
                    }
                    String fechaDevolucion = rs.getString("fecha_devolucion");
                    if (fechaDevolucion != null) {
                        prestamo.setFechaDevolucion(LocalDateTime.parse(fechaDevolucion));
                    }
                    prestamo.setEstado(rs.getString("estado"));
                    prestamo.setResidenteSobrestante(rs.getString("residente_sobrestante"));
                    prestamo.setAutorizacion(rs.getInt("autorizacion") == 1);
                    prestamo.setFolio(rs.getString("folio"));
                    prestamo.setFotoNombre(rs.getString("foto_nombre"));
                    prestamos.add(prestamo);
                }
            }
        }
        return prestamos;
    }

    public List<DetallePrestamo> obtenerDetallesPrestamo(int prestamoId) throws SQLException {
        List<DetallePrestamo> detalles = new ArrayList<>();
        String sql = "SELECT d.*, h.unidad FROM detalle_prestamos d LEFT JOIN herramientas h ON h.id = d.herramienta_id " +
                "WHERE d.prestamo_id = ? ORDER BY d.nombre_herramienta ASC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, prestamoId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    DetallePrestamo detalle = new DetallePrestamo();
                    detalle.setPrestamoId(prestamoId);
                    detalle.setHerramientaId(rs.getInt("herramienta_id"));
                    detalle.setNombreHerramienta(rs.getString("nombre_herramienta"));
                    detalle.setCategoria(rs.getString("categoria"));
                    detalle.setCantidad(rs.getInt("cantidad"));
                    detalle.setCantidadDevuelta(rs.getInt("cantidad_devuelta"));
                    detalle.setNoRetorno(rs.getInt("no_retorno") == 1);
                    detalle.setUnidad(rs.getString("unidad"));
                    detalles.add(detalle);
                }
            }
        }
        return detalles;
    }

    public java.util.Map<Integer, String> obtenerObservacionesPrestamo(int prestamoId) throws SQLException {
        java.util.Map<Integer, String> observaciones = new java.util.HashMap<>();
        String sql = "SELECT herramienta_id, GROUP_CONCAT(observacion, '; ') AS obs " +
                "FROM historial_devoluciones WHERE prestamo_id = ? " +
                "GROUP BY herramienta_id";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, prestamoId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int herramientaId = rs.getInt("herramienta_id");
                    String obs = rs.getString("obs");
                    observaciones.put(herramientaId, obs != null ? obs : "");
                }
            }
        }
        return observaciones;
    }

    public List<ReportePrestamoItem> obtenerReportePrestamosActivos() throws SQLException {
        List<ReportePrestamoItem> items = new ArrayList<>();
        String sql = "SELECT p.nombre_cliente, p.residente_sobrestante, p.fecha_prestamo, " +
                "d.nombre_herramienta, d.categoria, d.cantidad, h.unidad, h.tipo, pv.nombre AS proveedor_nombre " +
                "FROM prestamos p " +
                "JOIN detalle_prestamos d ON d.prestamo_id = p.id " +
                "LEFT JOIN herramientas h ON h.id = d.herramienta_id " +
                "LEFT JOIN proveedores pv ON pv.id = h.proveedor_id " +
                "WHERE p.estado = 'PRESTADO' AND d.no_retorno = 0 " +
                "ORDER BY p.nombre_cliente, d.nombre_herramienta";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ReportePrestamoItem item = new ReportePrestamoItem();
                item.setNombreCliente(rs.getString("nombre_cliente"));
                item.setResidenteSobrestante(rs.getString("residente_sobrestante"));
                String fecha = rs.getString("fecha_prestamo");
                if (fecha != null) {
                    item.setFechaPrestamo(LocalDateTime.parse(fecha));
                }
                item.setNombreHerramienta(rs.getString("nombre_herramienta"));
                item.setCategoria(rs.getString("categoria"));
                item.setUnidad(rs.getString("unidad"));
                item.setTipo(rs.getString("tipo"));
                item.setProveedor(rs.getString("proveedor_nombre"));
                item.setCantidad(rs.getInt("cantidad"));
                items.add(item);
            }
        }
        return items;
    }

    public List<String> obtenerHerramientasPrestadasActivas() throws SQLException {
        List<String> herramientas = new ArrayList<>();
        String sql = "SELECT DISTINCT d.nombre_herramienta " +
                "FROM prestamos p " +
                "JOIN detalle_prestamos d ON d.prestamo_id = p.id " +
                "WHERE p.estado = 'PRESTADO' AND d.no_retorno = 0 " +
                "ORDER BY d.nombre_herramienta ASC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                herramientas.add(rs.getString("nombre_herramienta"));
            }
        }
        return herramientas;
    }

    public List<ReportePrestamoItem> obtenerReportePrestamosActivosPorHerramienta(String herramienta) throws SQLException {
        List<ReportePrestamoItem> items = new ArrayList<>();
        String sql = "SELECT p.nombre_cliente, p.residente_sobrestante, p.fecha_prestamo, " +
                "d.nombre_herramienta, d.categoria, d.cantidad, h.unidad, h.tipo, pv.nombre AS proveedor_nombre " +
                "FROM prestamos p " +
                "JOIN detalle_prestamos d ON d.prestamo_id = p.id " +
                "LEFT JOIN herramientas h ON h.id = d.herramienta_id " +
                "LEFT JOIN proveedores pv ON pv.id = h.proveedor_id " +
                "WHERE p.estado = 'PRESTADO' AND d.no_retorno = 0 AND LOWER(TRIM(d.nombre_herramienta)) = LOWER(TRIM(?)) " +
                "ORDER BY p.nombre_cliente, p.fecha_prestamo DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, herramienta);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ReportePrestamoItem item = new ReportePrestamoItem();
                    item.setNombreCliente(rs.getString("nombre_cliente"));
                    item.setResidenteSobrestante(rs.getString("residente_sobrestante"));
                    String fecha = rs.getString("fecha_prestamo");
                    if (fecha != null) {
                        item.setFechaPrestamo(LocalDateTime.parse(fecha));
                    }
                    item.setNombreHerramienta(rs.getString("nombre_herramienta"));
                    item.setCategoria(rs.getString("categoria"));
                    item.setUnidad(rs.getString("unidad"));
                    item.setTipo(rs.getString("tipo"));
                    item.setProveedor(rs.getString("proveedor_nombre"));
                    item.setCantidad(rs.getInt("cantidad"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    public List<ReportePrestamoItem> obtenerMaterialesPrestados(String filtroMaterial) throws SQLException {
        List<ReportePrestamoItem> items = new ArrayList<>();
        String sql = "SELECT p.nombre_cliente, p.residente_sobrestante, p.fecha_prestamo, " +
                "d.nombre_herramienta, d.categoria, (d.cantidad - d.cantidad_devuelta) AS pendiente, " +
                "h.unidad, h.tipo, pv.nombre AS proveedor_nombre " +
                "FROM prestamos p " +
                "JOIN detalle_prestamos d ON d.prestamo_id = p.id " +
                "LEFT JOIN herramientas h ON h.id = d.herramienta_id " +
                "LEFT JOIN proveedores pv ON pv.id = h.proveedor_id " +
                "WHERE p.estado = 'PRESTADO' AND d.no_retorno = 0 AND (d.cantidad - d.cantidad_devuelta) > 0 " +
                "AND d.nombre_herramienta LIKE ? " +
                "ORDER BY d.nombre_herramienta ASC, p.nombre_cliente ASC";
        String pattern = "%" + (filtroMaterial == null ? "" : filtroMaterial.trim()) + "%";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, pattern);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ReportePrestamoItem item = new ReportePrestamoItem();
                    item.setNombreCliente(rs.getString("nombre_cliente"));
                    item.setResidenteSobrestante(rs.getString("residente_sobrestante"));
                    String fecha = rs.getString("fecha_prestamo");
                    if (fecha != null) {
                        item.setFechaPrestamo(LocalDateTime.parse(fecha));
                    }
                    item.setNombreHerramienta(rs.getString("nombre_herramienta"));
                    item.setCategoria(rs.getString("categoria"));
                    item.setUnidad(rs.getString("unidad"));
                    item.setTipo(rs.getString("tipo"));
                    item.setProveedor(rs.getString("proveedor_nombre"));
                    item.setCantidad(rs.getInt("pendiente"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    public Prestamo obtenerPrestamoPorId(int prestamoId) throws SQLException {
        String sql = "SELECT * FROM prestamos WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, prestamoId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Prestamo prestamo = new Prestamo();
                    prestamo.setId(rs.getInt("id"));
                    prestamo.setNombreCliente(rs.getString("nombre_cliente"));
                    prestamo.setNombreEmpleado(rs.getString("nombre_empleado"));
                    String fechaPrestamo = rs.getString("fecha_prestamo");
                    if (fechaPrestamo != null) {
                        prestamo.setFechaPrestamo(LocalDateTime.parse(fechaPrestamo));
                    }
                    String fechaDevolucion = rs.getString("fecha_devolucion");
                    if (fechaDevolucion != null) {
                        prestamo.setFechaDevolucion(LocalDateTime.parse(fechaDevolucion));
                    }
                    prestamo.setEstado(rs.getString("estado"));
                    prestamo.setResidenteSobrestante(rs.getString("residente_sobrestante"));
                    prestamo.setAutorizacion(rs.getInt("autorizacion") == 1);
                    prestamo.setFolio(rs.getString("folio"));
                    prestamo.setFotoNombre(rs.getString("foto_nombre"));
                    return prestamo;
                }
            }
        }
        return null;
    }

    public void registrarDevolucionParcial(int prestamoId, List<DevolucionItem> devoluciones) throws SQLException {
        if (devoluciones == null || devoluciones.isEmpty()) {
            return;
        }
        connection.setAutoCommit(false);
        try {
            for (DevolucionItem item : devoluciones) {
                String sql = "UPDATE detalle_prestamos " +
                        "SET cantidad_devuelta = cantidad_devuelta + ? " +
                        "WHERE prestamo_id = ? AND herramienta_id = ? AND no_retorno = 0 AND (cantidad_devuelta + ?) <= cantidad";
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setInt(1, item.getCantidadDevolver());
                    pstmt.setInt(2, prestamoId);
                    pstmt.setInt(3, item.getHerramientaId());
                    pstmt.setInt(4, item.getCantidadDevolver());
                    int updated = pstmt.executeUpdate();
                    if (updated == 0) {
                        throw new SQLException("Cantidad inválida para devolución");
                    }
                }
                actualizarStock(item.getHerramientaId(), item.getCantidadDevolver());

                // Guardar observación si existe
                if (item.getObservacion() != null && !item.getObservacion().trim().isEmpty()) {
                    String insertObs = "INSERT INTO historial_devoluciones " +
                            "(prestamo_id, herramienta_id, nombre_herramienta, cantidad_devuelta, observacion, fecha) " +
                            "VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement pstmt = connection.prepareStatement(insertObs)) {
                        pstmt.setInt(1, prestamoId);
                        pstmt.setInt(2, item.getHerramientaId());
                        pstmt.setString(3, item.getNombreHerramienta() != null ? item.getNombreHerramienta() : "");
                        pstmt.setInt(4, item.getCantidadDevolver());
                        pstmt.setString(5, item.getObservacion().trim());
                        pstmt.setString(6, LocalDateTime.now().toString());
                        pstmt.executeUpdate();
                    }
                }
            }

            int pendientes = obtenerPendientesPrestamo(prestamoId);
            if (pendientes == 0) {
                String updateSql = "UPDATE prestamos SET estado = 'DEVUELTO', fecha_devolucion = ? WHERE id = ?";
                try (PreparedStatement pstmt = connection.prepareStatement(updateSql)) {
                    pstmt.setString(1, LocalDateTime.now().toString());
                    pstmt.setInt(2, prestamoId);
                    pstmt.executeUpdate();
                }
            } else {
                String updateSql = "UPDATE prestamos SET estado = 'PRESTADO', fecha_devolucion = NULL WHERE id = ?";
                try (PreparedStatement pstmt = connection.prepareStatement(updateSql)) {
                    pstmt.setInt(1, prestamoId);
                    pstmt.executeUpdate();
                }
            }

            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public int obtenerPendientesPrestamo(int prestamoId) throws SQLException {
        String sql = "SELECT SUM(cantidad - cantidad_devuelta) AS pendientes FROM detalle_prestamos " +
                "WHERE prestamo_id = ? AND no_retorno = 0";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, prestamoId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("pendientes");
                }
            }
        }
        return 0;
    }
    
    public void devolverPrestamo(int prestamoId) throws SQLException {
        List<DevolucionItem> devoluciones = new ArrayList<>();
        String selectSql = "SELECT herramienta_id, (cantidad - cantidad_devuelta) AS pendiente " +
                "FROM detalle_prestamos WHERE prestamo_id = ? AND no_retorno = 0 AND (cantidad - cantidad_devuelta) > 0";
        try (PreparedStatement pstmt = connection.prepareStatement(selectSql)) {
            pstmt.setInt(1, prestamoId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    DevolucionItem item = new DevolucionItem();
                    item.setHerramientaId(rs.getInt("herramienta_id"));
                    item.setCantidadDevolver(rs.getInt("pendiente"));
                    devoluciones.add(item);
                }
            }
        }
        registrarDevolucionParcial(prestamoId, devoluciones);
    }

    // ===================== Proveedores =====================

    private Proveedor mapProveedor(ResultSet rs) throws SQLException {
        Proveedor p = new Proveedor(rs.getInt("id"), rs.getString("nombre"));
        p.setContacto(rs.getString("contacto"));
        p.setTelefono(rs.getString("telefono"));
        return p;
    }

    public List<Proveedor> obtenerProveedores() throws SQLException {
        List<Proveedor> proveedores = new ArrayList<>();
        String sql = "SELECT pv.*, " +
                "(SELECT COUNT(*) FROM herramientas h WHERE h.proveedor_id = pv.id AND h.estado = 1) AS total_materiales, " +
                "(SELECT COUNT(*) FROM remisiones r WHERE r.proveedor_id = pv.id) AS total_remisiones " +
                "FROM proveedores pv ORDER BY pv.nombre COLLATE NOCASE ASC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Proveedor p = mapProveedor(rs);
                p.setTotalMateriales(rs.getInt("total_materiales"));
                p.setTotalRemisiones(rs.getInt("total_remisiones"));
                proveedores.add(p);
            }
        }
        return proveedores;
    }

    public Proveedor obtenerProveedorPorId(int id) throws SQLException {
        String sql = "SELECT * FROM proveedores WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapProveedor(rs);
                }
            }
        }
        return null;
    }

    public Proveedor obtenerProveedorPorNombre(String nombre) throws SQLException {
        String sql = "SELECT * FROM proveedores WHERE nombre = TRIM(?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapProveedor(rs);
                }
            }
        }
        return null;
    }

    /** Registra un proveedor. Devuelve su ID, o -1 si ya existe uno con el mismo nombre. */
    public int agregarProveedor(Proveedor proveedor) throws SQLException {
        if (obtenerProveedorPorNombre(proveedor.getNombre()) != null) {
            return -1;
        }
        String sql = "INSERT INTO proveedores (nombre, contacto, telefono, fecha_registro) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, proveedor.getNombre().trim());
            pstmt.setString(2, proveedor.getContacto());
            pstmt.setString(3, proveedor.getTelefono());
            pstmt.setString(4, LocalDateTime.now().toString());
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    proveedor.setId(id);
                    return id;
                }
            }
        }
        throw new SQLException("No se pudo generar el ID del proveedor");
    }

    /** Actualiza un proveedor. Devuelve false si el nuevo nombre ya pertenece a otro proveedor. */
    public boolean actualizarProveedor(Proveedor proveedor) throws SQLException {
        Proveedor existente = obtenerProveedorPorNombre(proveedor.getNombre());
        if (existente != null && existente.getId() != proveedor.getId()) {
            return false;
        }
        connection.setAutoCommit(false);
        try {
            String sql = "UPDATE proveedores SET nombre = ?, contacto = ?, telefono = ? WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, proveedor.getNombre().trim());
                pstmt.setString(2, proveedor.getContacto());
                pstmt.setString(3, proveedor.getTelefono());
                pstmt.setInt(4, proveedor.getId());
                pstmt.executeUpdate();
            }
            String sqlHerr = "UPDATE herramientas SET provedor = ? WHERE proveedor_id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sqlHerr)) {
                pstmt.setString(1, proveedor.getNombre().trim());
                pstmt.setInt(2, proveedor.getId());
                pstmt.executeUpdate();
            }
            connection.commit();
            return true;
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    // ===================== Remisiones =====================

    private static final String SELECT_REMISION =
            "SELECT r.*, pv.nombre AS proveedor_nombre, " +
            "(SELECT COUNT(*) FROM detalle_remisiones d WHERE d.remision_id = r.id) AS total_partidas, " +
            "(SELECT COALESCE(SUM(d.cantidad), 0) FROM detalle_remisiones d WHERE d.remision_id = r.id) AS total_cantidad " +
            "FROM remisiones r JOIN proveedores pv ON pv.id = r.proveedor_id ";

    private static final String FILTRO_REMISION =
            "(r.numero_remision LIKE ? OR pv.nombre LIKE ? OR r.obra LIKE ? OR r.envia LIKE ? OR r.recibe LIKE ? " +
            "OR r.fecha LIKE ? OR CAST(r.id AS TEXT) LIKE ? " +
            "OR EXISTS (SELECT 1 FROM detalle_remisiones d WHERE d.remision_id = r.id AND d.nombre_material LIKE ?))";
    private static final int PARAMS_FILTRO_REMISION = 8;

    private Remision mapRemision(ResultSet rs) throws SQLException {
        Remision r = new Remision();
        r.setId(rs.getInt("id"));
        r.setNumeroRemision(rs.getString("numero_remision"));
        r.setProveedorId(rs.getInt("proveedor_id"));
        r.setProveedorNombre(rs.getString("proveedor_nombre"));
        r.setObra(rs.getString("obra"));
        r.setEnvia(rs.getString("envia"));
        r.setRecibe(rs.getString("recibe"));
        String fecha = rs.getString("fecha");
        if (fecha != null && !fecha.isEmpty()) {
            r.setFecha(LocalDate.parse(fecha));
        }
        String fechaRegistro = rs.getString("fecha_registro");
        if (fechaRegistro != null && !fechaRegistro.isEmpty()) {
            r.setFechaRegistro(LocalDateTime.parse(fechaRegistro));
        }
        r.setObservaciones(rs.getString("observaciones"));
        r.setTotalPartidas(rs.getInt("total_partidas"));
        r.setTotalCantidad(rs.getInt("total_cantidad"));
        return r;
    }

    /** Indica si ya existe una remisión con ese número para ese proveedor. */
    public boolean existeRemision(String numeroRemision, int proveedorId) throws SQLException {
        if (vacio(numeroRemision)) {
            return false;
        }
        String sql = "SELECT COUNT(*) FROM remisiones WHERE LOWER(TRIM(numero_remision)) = LOWER(TRIM(?)) AND proveedor_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, numeroRemision);
            pstmt.setInt(2, proveedorId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Registra una remisión completa en una sola transacción:
     * guarda el encabezado, y por cada partida busca el material de ESE proveedor
     * (mismo nombre). Si existe le suma la cantidad al stock; si no existe crea un
     * material nuevo ligado al proveedor. Así el mismo material de dos proveedores
     * queda en registros separados, cada uno con su stock.
     *
     * @return el ID de la remisión creada
     */
    public int registrarRemision(Remision remision, List<DetalleRemision> partidas) throws SQLException {
        if (partidas == null || partidas.isEmpty()) {
            throw new SQLException("La remisión no tiene materiales");
        }
        Proveedor proveedor = obtenerProveedorPorId(remision.getProveedorId());
        if (proveedor == null) {
            throw new SQLException("El proveedor seleccionado no existe");
        }
        connection.setAutoCommit(false);
        try {
            int remisionId;
            String sqlRem = "INSERT INTO remisiones (numero_remision, proveedor_id, obra, envia, recibe, fecha, " +
                    "fecha_registro, observaciones) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sqlRem, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, remision.getNumeroRemision());
                pstmt.setInt(2, remision.getProveedorId());
                pstmt.setString(3, remision.getObra());
                pstmt.setString(4, remision.getEnvia());
                pstmt.setString(5, remision.getRecibe());
                pstmt.setString(6, (remision.getFecha() != null ? remision.getFecha() : LocalDate.now()).toString());
                pstmt.setString(7, LocalDateTime.now().toString());
                pstmt.setString(8, remision.getObservaciones());
                pstmt.executeUpdate();
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (!rs.next()) {
                        throw new SQLException("No se pudo generar el ID de la remisión");
                    }
                    remisionId = rs.getInt(1);
                }
            }

            aplicarPartidas(remisionId, remision, proveedor, partidas);
            connection.commit();
            remision.setId(remisionId);
            return remisionId;
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    /** Suma cada partida al material del proveedor (o lo crea) y guarda el detalle de la remisión. */
    private void aplicarPartidas(int remisionId, Remision remision, Proveedor proveedor,
                                 List<DetalleRemision> partidas) throws SQLException {
        String sqlDet = "INSERT INTO detalle_remisiones (remision_id, herramienta_id, nombre_material, descripcion, " +
                "categoria, tipo, unidad, cantidad) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlSumar = "UPDATE herramientas SET stock = stock + ?, estado = 1, remision = ?, " +
                "unidad = COALESCE(NULLIF(TRIM(unidad), ''), ?), tipo = COALESCE(NULLIF(TRIM(tipo), ''), ?) WHERE id = ?";
        for (DetalleRemision partida : partidas) {
            if (vacio(partida.getNombreMaterial()) || partida.getCantidad() <= 0) {
                throw new SQLException("Cada material debe tener nombre y una cantidad mayor a 0");
            }
            String categoria = vacio(partida.getCategoria()) ? "Sin categoría" : partida.getCategoria().trim();
            partida.setCategoria(categoria);
            Herramienta existente = buscarMaterialDeProveedor(partida.getNombreMaterial(), proveedor.getId());
            int herramientaId;
            if (existente != null) {
                herramientaId = existente.getId();
                try (PreparedStatement pstmt = connection.prepareStatement(sqlSumar)) {
                    pstmt.setInt(1, partida.getCantidad());
                    pstmt.setString(2, remision.getNumeroRemision());
                    pstmt.setString(3, partida.getUnidad());
                    pstmt.setString(4, partida.getTipo());
                    pstmt.setInt(5, herramientaId);
                    pstmt.executeUpdate();
                }
            } else {
                Herramienta nueva = new Herramienta();
                nueva.setNombre(partida.getNombreMaterial().trim());
                nueva.setCategoria(categoria);
                nueva.setTipo(partida.getTipo());
                nueva.setUnidad(partida.getUnidad());
                nueva.setProveedorId(proveedor.getId());
                nueva.setProveedorNombre(proveedor.getNombre());
                nueva.setRemision(remision.getNumeroRemision());
                nueva.setStock(partida.getCantidad());
                nueva.setDescripcion(partida.getDescripcion());
                nueva.setEstado(1);
                herramientaId = agregarHerramienta(nueva);
            }
            partida.setHerramientaId(herramientaId);
            partida.setRemisionId(remisionId);
            try (PreparedStatement pstmt = connection.prepareStatement(sqlDet)) {
                pstmt.setInt(1, remisionId);
                pstmt.setInt(2, herramientaId);
                pstmt.setString(3, partida.getNombreMaterial().trim());
                pstmt.setString(4, partida.getDescripcion());
                pstmt.setString(5, categoria);
                pstmt.setString(6, partida.getTipo());
                pstmt.setString(7, partida.getUnidad());
                pstmt.setInt(8, partida.getCantidad());
                pstmt.executeUpdate();
            }
            agregarCatalogo(Catalogo.CATEGORIAS, categoria);
            if (!vacio(partida.getTipo())) {
                agregarCatalogo(Catalogo.TIPOS, partida.getTipo());
            }
            if (!vacio(partida.getUnidad())) {
                agregarCatalogo(Catalogo.UNIDADES, partida.getUnidad());
            }
        }
    }

    /**
     * Modifica una remisión ya registrada (encabezado y partidas) y ajusta el stock:
     * se quita lo que sumó la versión anterior y se suma lo nuevo, todo en una transacción.
     * Si algún material quedara con stock negativo (porque esa cantidad ya está prestada
     * o se usó), no se guarda nada y se explica el motivo.
     */
    public void actualizarRemision(Remision remision, List<DetalleRemision> partidas) throws SQLException {
        if (partidas == null || partidas.isEmpty()) {
            throw new SQLException("La remisión no tiene materiales");
        }
        Proveedor proveedor = obtenerProveedorPorId(remision.getProveedorId());
        if (proveedor == null) {
            throw new SQLException("El proveedor seleccionado no existe");
        }
        List<DetalleRemision> anteriores = obtenerDetallesRemision(remision.getId());
        connection.setAutoCommit(false);
        try {
            java.util.Set<Integer> afectados = new java.util.LinkedHashSet<>();
            try (PreparedStatement pstmt = connection.prepareStatement(
                    "UPDATE herramientas SET stock = stock - ? WHERE id = ?")) {
                for (DetalleRemision d : anteriores) {
                    pstmt.setInt(1, d.getCantidad());
                    pstmt.setInt(2, d.getHerramientaId());
                    pstmt.executeUpdate();
                    afectados.add(d.getHerramientaId());
                }
            }
            try (PreparedStatement pstmt = connection.prepareStatement(
                    "DELETE FROM detalle_remisiones WHERE remision_id = ?")) {
                pstmt.setInt(1, remision.getId());
                pstmt.executeUpdate();
            }
            String sqlRem = "UPDATE remisiones SET numero_remision = ?, proveedor_id = ?, obra = ?, envia = ?, " +
                    "recibe = ?, fecha = ?, observaciones = ? WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sqlRem)) {
                pstmt.setString(1, remision.getNumeroRemision());
                pstmt.setInt(2, remision.getProveedorId());
                pstmt.setString(3, remision.getObra());
                pstmt.setString(4, remision.getEnvia());
                pstmt.setString(5, remision.getRecibe());
                pstmt.setString(6, (remision.getFecha() != null ? remision.getFecha() : LocalDate.now()).toString());
                pstmt.setString(7, remision.getObservaciones());
                pstmt.setInt(8, remision.getId());
                if (pstmt.executeUpdate() == 0) {
                    throw new SQLException("La remisión ya no existe");
                }
            }
            aplicarPartidas(remision.getId(), remision, proveedor, partidas);
            for (DetalleRemision d : partidas) {
                afectados.add(d.getHerramientaId());
            }
            List<String> problemas = new ArrayList<>();
            try (PreparedStatement pstmt = connection.prepareStatement(SELECT_HERRAMIENTA + "WHERE h.id = ?")) {
                for (int id : afectados) {
                    pstmt.setInt(1, id);
                    for (Herramienta h : listarHerramientas(pstmt)) {
                        if (h.getStock() < 0) {
                            problemas.add("«" + h.getNombre() + "» (" +
                                    (h.getProveedorNombre() != null ? h.getProveedorNombre() : "sin proveedor") +
                                    "): faltan " + (-h.getStock()) + " en existencia");
                        }
                    }
                }
            }
            if (!problemas.isEmpty()) {
                throw new SQLException("No se puede quitar esa cantidad porque ya salió del almacén " +
                        "(está prestada o se usó):\n" + String.join("\n", problemas));
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public int contarRemisiones(String filtro, Integer proveedorId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM remisiones r JOIN proveedores pv ON pv.id = r.proveedor_id WHERE 1 = 1" +
                (proveedorId != null ? " AND r.proveedor_id = ?" : "") +
                (vacio(filtro) ? "" : " AND " + FILTRO_REMISION);
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            int i = 1;
            if (proveedorId != null) {
                pstmt.setInt(i++, proveedorId);
            }
            if (!vacio(filtro)) {
                String pattern = "%" + filtro.trim() + "%";
                for (int k = 0; k < PARAMS_FILTRO_REMISION; k++) {
                    pstmt.setString(i++, pattern);
                }
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public List<Remision> obtenerRemisionesPaginadas(String filtro, Integer proveedorId, int offset, int limit) throws SQLException {
        String sql = SELECT_REMISION + "WHERE 1 = 1" +
                (proveedorId != null ? " AND r.proveedor_id = ?" : "") +
                (vacio(filtro) ? "" : " AND " + FILTRO_REMISION) +
                " ORDER BY r.fecha DESC, r.id DESC LIMIT ? OFFSET ?";
        List<Remision> remisiones = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            int i = 1;
            if (proveedorId != null) {
                pstmt.setInt(i++, proveedorId);
            }
            if (!vacio(filtro)) {
                String pattern = "%" + filtro.trim() + "%";
                for (int k = 0; k < PARAMS_FILTRO_REMISION; k++) {
                    pstmt.setString(i++, pattern);
                }
            }
            pstmt.setInt(i++, limit);
            pstmt.setInt(i, offset);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    remisiones.add(mapRemision(rs));
                }
            }
        }
        return remisiones;
    }

    public Remision obtenerRemisionPorId(int id) throws SQLException {
        String sql = SELECT_REMISION + "WHERE r.id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRemision(rs);
                }
            }
        }
        return null;
    }

    private DetalleRemision mapDetalleRemision(ResultSet rs) throws SQLException {
        DetalleRemision d = new DetalleRemision();
        d.setId(rs.getInt("id"));
        d.setRemisionId(rs.getInt("remision_id"));
        d.setHerramientaId(rs.getInt("herramienta_id"));
        d.setNombreMaterial(rs.getString("nombre_material"));
        d.setDescripcion(rs.getString("descripcion"));
        d.setCategoria(rs.getString("categoria"));
        d.setTipo(rs.getString("tipo"));
        d.setUnidad(rs.getString("unidad"));
        d.setCantidad(rs.getInt("cantidad"));
        return d;
    }

    public List<DetalleRemision> obtenerDetallesRemision(int remisionId) throws SQLException {
        List<DetalleRemision> detalles = new ArrayList<>();
        String sql = "SELECT * FROM detalle_remisiones WHERE remision_id = ? ORDER BY id ASC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, remisionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    detalles.add(mapDetalleRemision(rs));
                }
            }
        }
        return detalles;
    }

    /**
     * Entradas de material por remisión entre dos fechas (inclusive). Cualquiera de
     * las fechas puede ser null para no limitar ese extremo.
     */
    public List<DetalleRemision> obtenerEntradasPorRemision(LocalDate desde, LocalDate hasta, Integer proveedorId) throws SQLException {
        return obtenerEntradasPorRemision(desde, hasta, proveedorId, null);
    }

    /** Entradas por remisión; si se indica material, solo las de ese nombre (de cualquier proveedor). */
    public List<DetalleRemision> obtenerEntradasPorRemision(LocalDate desde, LocalDate hasta, Integer proveedorId,
                                                            String material) throws SQLException {
        List<DetalleRemision> entradas = new ArrayList<>();
        String sql = "SELECT d.*, r.numero_remision, r.fecha AS fecha_remision, pv.nombre AS proveedor_nombre " +
                "FROM detalle_remisiones d " +
                "JOIN remisiones r ON r.id = d.remision_id " +
                "JOIN proveedores pv ON pv.id = r.proveedor_id WHERE 1 = 1" +
                (desde != null ? " AND r.fecha >= ?" : "") +
                (hasta != null ? " AND r.fecha <= ?" : "") +
                (proveedorId != null ? " AND r.proveedor_id = ?" : "") +
                (vacio(material) ? "" : " AND LOWER(TRIM(d.nombre_material)) = LOWER(TRIM(?))") +
                " ORDER BY r.fecha ASC, r.id ASC, d.id ASC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            int i = 1;
            if (desde != null) {
                pstmt.setString(i++, desde.toString());
            }
            if (hasta != null) {
                pstmt.setString(i++, hasta.toString());
            }
            if (proveedorId != null) {
                pstmt.setInt(i++, proveedorId);
            }
            if (!vacio(material)) {
                pstmt.setString(i, material);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    DetalleRemision d = mapDetalleRemision(rs);
                    d.setNumeroRemision(rs.getString("numero_remision"));
                    d.setProveedorNombre(rs.getString("proveedor_nombre"));
                    String fecha = rs.getString("fecha_remision");
                    if (fecha != null && !fecha.isEmpty()) {
                        d.setFechaRemision(LocalDate.parse(fecha));
                    }
                    entradas.add(d);
                }
            }
        }
        return entradas;
    }
}

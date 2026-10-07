package com.almacen.database;

import com.almacen.model.DetallePrestamo;
import com.almacen.model.Herramienta;
import com.almacen.model.ItemCarrito;
import com.almacen.model.Prestamo;
import com.almacen.model.DevolucionItem;
import com.almacen.model.ReportePrestamoItem;
import java.sql.*;
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

        // Tabla de herramientas
        String createHerramientas = "CREATE TABLE IF NOT EXISTS herramientas (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre TEXT NOT NULL, " +
                "categoria TEXT NOT NULL, " +
                "stock INTEGER NOT NULL DEFAULT 0, " +
                "descripcion TEXT, " +
                "remision TEXT, " +
                "provedor TEXT, " +
                "estado INTEGER NOT NULL DEFAULT 1)";
        
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
            stmt.execute(createHerramientas);
            stmt.execute(createPrestamos);
            stmt.execute(createDetallePrestamos);
            stmt.execute(createHistorialDevoluciones);
        }

        ensureHerramientaColumns();
        migratePrestamosSiNecesario();
        ensurePrestamoColumns();
        ensureDetalleColumns();
        
        // Insertar datos de ejemplo si la tabla está vacía
        insertarDatosEjemplo();
    }
    
    private void insertarDatosEjemplo() throws SQLException {
        insertarCategoriasEjemplo();

        String check = "SELECT COUNT(*) FROM herramientas";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(check)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String insert = "INSERT INTO herramientas (nombre, categoria, stock, descripcion, estado) VALUES " +
                        "('Pala', 'Herramientas de mano', 10, 'Pala de acero para excavación', 1), " +
                        "('Pico', 'Herramientas de mano', 8, 'Pico para romper tierra y rocas', 1), " +
                        "('Martillo', 'Herramientas de mano', 15, 'Martillo de construcción', 1), " +
                        "('Destornillador', 'Herramientas de mano', 20, 'Destornillador Phillips', 1), " +
                        "('Llave inglesa', 'Herramientas de mano', 12, 'Llave ajustable', 1), " +
                        "('Taladro', 'Herramientas eléctricas', 5, 'Taladro eléctrico profesional', 1), " +
                        "('Sierra', 'Herramientas de mano', 7, 'Sierra de mano para madera', 1), " +
                        "('Nivel', 'Herramientas de medición', 9, 'Nivel de burbuja', 1)";
                stmt.execute(insert);
            }
        }
    }

    private void insertarCategoriasEjemplo() throws SQLException {
        String check = "SELECT COUNT(*) FROM categorias";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(check)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String insert = "INSERT INTO categorias (nombre) VALUES " +
                        "('Herramientas de mano'), " +
                        "('Herramientas eléctricas'), " +
                        "('Herramientas de medición')";
                stmt.execute(insert);
            }
        }
        try (Statement stmt = connection.createStatement()) {
            String insertFromTools = "INSERT OR IGNORE INTO categorias (nombre) " +
                    "SELECT DISTINCT categoria FROM herramientas";
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
    }

    private void ensureHerramientaColumns() throws SQLException {
        ensureColumn("herramientas", "estado", "INTEGER NOT NULL DEFAULT 1");
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
    
    // Métodos para herramientas
    public List<Herramienta> buscarHerramientas(String busqueda) throws SQLException {
        List<Herramienta> herramientas = new ArrayList<>();
        String sql;
        
        if (busqueda == null || busqueda.trim().isEmpty()) {
            sql = "SELECT * FROM herramientas WHERE estado = 1 ORDER BY id DESC";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    herramientas.add(new Herramienta(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("categoria"),
                        rs.getInt("stock"),
                        rs.getString("descripcion"),
                        rs.getInt("estado")
                    ));
                }
            }
        } else {
            sql = "SELECT * FROM herramientas WHERE estado = 1 AND (nombre LIKE ? OR categoria LIKE ?) ORDER BY id DESC";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                String pattern = "%" + busqueda + "%";
                pstmt.setString(1, pattern);
                pstmt.setString(2, pattern);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        herramientas.add(new Herramienta(
                            rs.getInt("id"),
                            rs.getString("nombre"),
                            rs.getString("categoria"),
                            rs.getInt("stock"),
                            rs.getString("descripcion"),
                            rs.getInt("estado")
                        ));
                    }
                }
            }
        }
        return herramientas;
    }

    public List<String> obtenerCategorias() throws SQLException {
        List<String> categorias = new ArrayList<>();
        String sql = "SELECT nombre FROM categorias ORDER BY nombre ASC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                categorias.add(rs.getString("nombre"));
            }
        }
        return categorias;
    }

    public boolean agregarCategoria(String nombre) throws SQLException {
        String sql = "INSERT INTO categorias (nombre) VALUES (?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            // Si ya existe, no insertamos de nuevo
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("unique")) {
                return false;
            }
            throw e;
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
        if (filtro == null || filtro.trim().isEmpty()) {
            String sql = "SELECT COUNT(*) FROM herramientas WHERE estado = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, estado);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
            return 0;
        }
        String sql = "SELECT COUNT(*) FROM herramientas " +
                "WHERE estado = ? AND (nombre LIKE ? OR categoria LIKE ? OR descripcion LIKE ? OR CAST(id AS TEXT) LIKE ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            String pattern = "%" + filtro.trim() + "%";
            pstmt.setInt(1, estado);
            pstmt.setString(2, pattern);
            pstmt.setString(3, pattern);
            pstmt.setString(4, pattern);
            pstmt.setString(5, pattern);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public List<Herramienta> obtenerHerramientasPaginadasFiltradas(String filtro, int estado, int offset, int limit) throws SQLException {
        List<Herramienta> herramientas = new ArrayList<>();
        String sql;
        if (filtro == null || filtro.trim().isEmpty()) {
            sql = "SELECT * FROM herramientas WHERE estado = ? ORDER BY id DESC LIMIT ? OFFSET ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, estado);
                pstmt.setInt(2, limit);
                pstmt.setInt(3, offset);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        herramientas.add(new Herramienta(
                            rs.getInt("id"),
                            rs.getString("nombre"),
                            rs.getString("categoria"),
                            rs.getInt("stock"),
                            rs.getString("descripcion"),
                            rs.getInt("estado")
                        ));
                    }
                }
            }
            return herramientas;
        }
        sql = "SELECT * FROM herramientas " +
                "WHERE estado = ? AND (nombre LIKE ? OR categoria LIKE ? OR descripcion LIKE ? OR CAST(id AS TEXT) LIKE ?) " +
                "ORDER BY id DESC LIMIT ? OFFSET ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            String pattern = "%" + filtro.trim() + "%";
            pstmt.setInt(1, estado);
            pstmt.setString(2, pattern);
            pstmt.setString(3, pattern);
            pstmt.setString(4, pattern);
            pstmt.setString(5, pattern);
            pstmt.setInt(6, limit);
            pstmt.setInt(7, offset);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    herramientas.add(new Herramienta(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("categoria"),
                        rs.getInt("stock"),
                        rs.getString("descripcion"),
                        rs.getInt("estado")
                    ));
                }
            }
        }
        return herramientas;
    }

    public int contarHerramientasPorNombre(String nombre, int estado) throws SQLException {
        if (nombre == null || nombre.trim().isEmpty()) {
            String sql = "SELECT COUNT(*) FROM herramientas WHERE estado = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, estado);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
            return 0;
        }
        String sql = "SELECT COUNT(*) FROM herramientas WHERE estado = ? AND nombre LIKE ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            String pattern = "%" + nombre.trim() + "%";
            pstmt.setInt(1, estado);
            pstmt.setString(2, pattern);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public List<Herramienta> obtenerHerramientasPaginadasPorNombre(String nombre, int estado, int offset, int limit) throws SQLException {
        List<Herramienta> herramientas = new ArrayList<>();
        String sql;
        if (nombre == null || nombre.trim().isEmpty()) {
            sql = "SELECT * FROM herramientas WHERE estado = ? ORDER BY id DESC LIMIT ? OFFSET ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, estado);
                pstmt.setInt(2, limit);
                pstmt.setInt(3, offset);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        herramientas.add(new Herramienta(
                            rs.getInt("id"),
                            rs.getString("nombre"),
                            rs.getString("categoria"),
                            rs.getInt("stock"),
                            rs.getString("descripcion"),
                            rs.getInt("estado")
                        ));
                    }
                }
            }
            return herramientas;
        }
        sql = "SELECT * FROM herramientas WHERE estado = ? AND nombre LIKE ? ORDER BY id DESC LIMIT ? OFFSET ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            String pattern = "%" + nombre.trim() + "%";
            pstmt.setInt(1, estado);
            pstmt.setString(2, pattern);
            pstmt.setInt(3, limit);
            pstmt.setInt(4, offset);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    herramientas.add(new Herramienta(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("categoria"),
                        rs.getInt("stock"),
                        rs.getString("descripcion"),
                        rs.getInt("estado")
                    ));
                }
            }
        }
        return herramientas;
    }

    public int contarHerramientasBusqueda(String busqueda) throws SQLException {
        String sql;
        if (busqueda == null || busqueda.trim().isEmpty()) {
            sql = "SELECT COUNT(*) FROM herramientas WHERE estado = 1";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } else {
            sql = "SELECT COUNT(*) FROM herramientas WHERE estado = 1 AND (nombre LIKE ? OR categoria LIKE ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                String pattern = "%" + busqueda + "%";
                pstmt.setString(1, pattern);
                pstmt.setString(2, pattern);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        }
        return 0;
    }

    public List<Herramienta> buscarHerramientasPaginadas(String busqueda, int offset, int limit) throws SQLException {
        List<Herramienta> herramientas = new ArrayList<>();
        String sql;
        
        if (busqueda == null || busqueda.trim().isEmpty()) {
            sql = "SELECT * FROM herramientas WHERE estado = 1 ORDER BY id DESC LIMIT ? OFFSET ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, limit);
                pstmt.setInt(2, offset);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        herramientas.add(new Herramienta(
                            rs.getInt("id"),
                            rs.getString("nombre"),
                            rs.getString("categoria"),
                            rs.getInt("stock"),
                            rs.getString("descripcion"),
                            rs.getInt("estado")
                        ));
                    }
                }
            }
        } else {
            sql = "SELECT * FROM herramientas WHERE estado = 1 AND (nombre LIKE ? OR categoria LIKE ?) ORDER BY id DESC LIMIT ? OFFSET ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                String pattern = "%" + busqueda + "%";
                pstmt.setString(1, pattern);
                pstmt.setString(2, pattern);
                pstmt.setInt(3, limit);
                pstmt.setInt(4, offset);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        herramientas.add(new Herramienta(
                            rs.getInt("id"),
                            rs.getString("nombre"),
                            rs.getString("categoria"),
                            rs.getInt("stock"),
                            rs.getString("descripcion"),
                            rs.getInt("estado")
                        ));
                    }
                }
            }
        }
        return herramientas;
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

    public List<Herramienta> obtenerHerramientasPaginadas(int offset, int limit) throws SQLException {
        List<Herramienta> herramientas = new ArrayList<>();
        String sql = "SELECT * FROM herramientas WHERE estado = 1 ORDER BY nombre ASC LIMIT ? OFFSET ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    herramientas.add(new Herramienta(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("categoria"),
                        rs.getInt("stock"),
                        rs.getString("descripcion"),
                        rs.getInt("estado")
                    ));
                }
            }
        }
        return herramientas;
    }

    public List<Herramienta> obtenerHerramientasBajaPaginadas(int offset, int limit) throws SQLException {
        List<Herramienta> herramientas = new ArrayList<>();
        String sql = "SELECT * FROM herramientas WHERE estado = 0 ORDER BY nombre ASC LIMIT ? OFFSET ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    herramientas.add(new Herramienta(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("categoria"),
                        rs.getInt("stock"),
                        rs.getString("descripcion"),
                        rs.getInt("estado")
                    ));
                }
            }
        }
        return herramientas;
    }
    
    public Herramienta obtenerHerramientaPorId(int id) throws SQLException {
        String sql = "SELECT * FROM herramientas WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Herramienta(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("categoria"),
                        rs.getInt("stock"),
                        rs.getString("descripcion"),
                        rs.getInt("estado")
                    );
                }
            }
        }
        return null;
    }
    
    public void actualizarStock(int herramientaId, int cantidad) throws SQLException {
        String sql = "UPDATE herramientas SET stock = stock + ? WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, cantidad);
            pstmt.setInt(2, herramientaId);
            pstmt.executeUpdate();
        }
    }
    
    public void agregarHerramienta(Herramienta herramienta) throws SQLException {
        String sql = "INSERT INTO herramientas (nombre, categoria, stock, descripcion, estado) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, herramienta.getNombre());
            pstmt.setString(2, herramienta.getCategoria());
            pstmt.setInt(3, herramienta.getStock());
            pstmt.setString(4, herramienta.getDescripcion());
            pstmt.setInt(5, herramienta.getEstado());
            pstmt.executeUpdate();
        }
    }

    public void actualizarHerramienta(Herramienta herramienta) throws SQLException {
        String sql = "UPDATE herramientas SET nombre = ?, categoria = ?, stock = ?, descripcion = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, herramienta.getNombre());
            pstmt.setString(2, herramienta.getCategoria());
            pstmt.setInt(3, herramienta.getStock());
            pstmt.setString(4, herramienta.getDescripcion());
            pstmt.setInt(5, herramienta.getId());
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

    public boolean actualizarCategoria(String nombreAnterior, String nombreNuevo) throws SQLException {
        connection.setAutoCommit(false);
        try {
            String updateCat = "UPDATE categorias SET nombre = ? WHERE nombre = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(updateCat)) {
                pstmt.setString(1, nombreNuevo);
                pstmt.setString(2, nombreAnterior);
                int updated = pstmt.executeUpdate();
                if (updated == 0) {
                    connection.rollback();
                    return false;
                }
            }
            String updateHerr = "UPDATE herramientas SET categoria = ? WHERE categoria = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(updateHerr)) {
                pstmt.setString(1, nombreNuevo);
                pstmt.setString(2, nombreAnterior);
                pstmt.executeUpdate();
            }
            String updateDetalle = "UPDATE detalle_prestamos SET categoria = ? WHERE categoria = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(updateDetalle)) {
                pstmt.setString(1, nombreNuevo);
                pstmt.setString(2, nombreAnterior);
                pstmt.executeUpdate();
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

    private void insertarDetallePrestamo(int prestamoId, ItemCarrito item) throws SQLException {
        String sql = "INSERT INTO detalle_prestamos (prestamo_id, herramienta_id, nombre_herramienta, categoria, cantidad, cantidad_devuelta) " +
                "VALUES (?, ?, ?, ?, ?, 0)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, prestamoId);
            pstmt.setInt(2, item.getIdHerramienta());
            pstmt.setString(3, item.getNombre());
            pstmt.setString(4, item.getCategoria());
            pstmt.setInt(5, item.getCantidad());
            pstmt.executeUpdate();
        }
        actualizarStock(item.getIdHerramienta(), -item.getCantidad());
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
            for (ItemCarrito item : items) {
                insertarDetallePrestamo(prestamoId, item);
            }
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
        String sql = "SELECT COUNT(*) FROM prestamos WHERE estado = ? " +
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
        String sql = "SELECT * FROM prestamos WHERE estado = ? " +
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

    private String safeLike(String value) {
        return value == null ? "" : value.trim();
    }

    public boolean tieneDevolucionesParciales(int prestamoId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM detalle_prestamos " +
                "WHERE prestamo_id = ? AND cantidad_devuelta > 0 AND cantidad_devuelta < cantidad";
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
        String sql = "SELECT * FROM detalle_prestamos WHERE prestamo_id = ? ORDER BY nombre_herramienta ASC";
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
                "d.nombre_herramienta, d.categoria, d.cantidad " +
                "FROM prestamos p " +
                "JOIN detalle_prestamos d ON d.prestamo_id = p.id " +
                "WHERE p.estado = 'PRESTADO' " +
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
                "WHERE p.estado = 'PRESTADO' " +
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
                "d.nombre_herramienta, d.categoria, d.cantidad " +
                "FROM prestamos p " +
                "JOIN detalle_prestamos d ON d.prestamo_id = p.id " +
                "WHERE p.estado = 'PRESTADO' AND d.nombre_herramienta = ? " +
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
                "d.nombre_herramienta, d.categoria, (d.cantidad - d.cantidad_devuelta) AS pendiente " +
                "FROM prestamos p " +
                "JOIN detalle_prestamos d ON d.prestamo_id = p.id " +
                "WHERE p.estado = 'PRESTADO' AND (d.cantidad - d.cantidad_devuelta) > 0 " +
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
                        "WHERE prestamo_id = ? AND herramienta_id = ? AND (cantidad_devuelta + ?) <= cantidad";
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
        String sql = "SELECT SUM(cantidad - cantidad_devuelta) AS pendientes FROM detalle_prestamos WHERE prestamo_id = ?";
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
                "FROM detalle_prestamos WHERE prestamo_id = ? AND (cantidad - cantidad_devuelta) > 0";
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
}

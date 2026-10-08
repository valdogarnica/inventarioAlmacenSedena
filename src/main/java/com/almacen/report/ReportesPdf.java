package com.almacen.report;

import com.almacen.database.DatabaseManager;
import com.almacen.model.DetalleRemision;
import com.almacen.model.ExistenciaMaterial;
import com.almacen.model.Herramienta;
import com.almacen.model.Proveedor;
import com.almacen.model.Remision;
import com.almacen.model.ReportePrestamoItem;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Todos los reportes PDF del sistema. Cada método genera el archivo y lo devuelve;
 * devuelve null cuando no hay datos que reportar.
 */
public final class ReportesPdf {
    public static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private ReportesPdf() {
    }

    // ---------------------------------------------------------------- Inventario

    /** REPORTE GENERAL DE INVENTARIO: todos los materiales activos, o solo los de un proveedor. */
    public static File inventarioGeneral(Integer proveedorId) throws Exception {
        DatabaseManager db = DatabaseManager.getInstance();
        List<Herramienta> materiales = db.obtenerInventarioGeneral(proveedorId);
        if (materiales.isEmpty()) {
            return null;
        }
        String titulo = proveedorId == null ? "REPORTE GENERAL DE INVENTARIO" : "REPORTE DE INVENTARIO POR PROVEEDOR";
        try (PdfReportBuilder pdf = new PdfReportBuilder(titulo, true)) {
            if (proveedorId != null) {
                Proveedor p = db.obtenerProveedorPorId(proveedorId);
                pdf.datos(new String[][]{
                    {"Proveedor", p != null ? p.getNombre() : ""},
                    {"Contacto", p != null ? safe(p.getContacto()) : ""},
                    {"Teléfono", p != null ? safe(p.getTelefono()) : ""}
                }, 3);
            }
            int totalStock = 0;
            int totalPrestado = 0;
            Map<String, Integer> porUnidad = new LinkedHashMap<>();
            List<String[]> filas = new ArrayList<>();
            for (Herramienta h : materiales) {
                int existencia = h.getStock() + h.getCantidadPrestada();
                filas.add(new String[]{
                    String.valueOf(h.getId()),
                    safe(h.getNombre()),
                    safe(h.getCategoria()),
                    safe(h.getTipo()),
                    safe(h.getUnidad()),
                    guion(h.getProveedorNombre()),
                    String.valueOf(h.getStock()),
                    String.valueOf(h.getCantidadPrestada()),
                    String.valueOf(existencia),
                    fecha(h.getFechaRegistro())
                });
                totalStock += h.getStock();
                totalPrestado += h.getCantidadPrestada();
                String unidad = guion(h.getUnidad());
                porUnidad.merge(unidad, existencia, Integer::sum);
            }
            pdf.datos(new String[][]{
                {"Materiales registrados", String.valueOf(materiales.size())},
                {"Disponible", String.valueOf(totalStock)},
                {"Prestado", String.valueOf(totalPrestado)}
            }, 3);
            pdf.espacio(6);
            pdf.tabla(
                new String[]{"ID", "Material", "Categoría", "Tipo", "Unidad", "Proveedor", "Disponible", "Prestado", "Total", "Fecha alta"},
                new float[]{4, 21, 13, 9, 7, 14, 8, 7, 6, 8},
                new boolean[]{true, false, false, false, false, false, true, true, true, false},
                filas,
                new String[]{"", "TOTAL", "", "", "", "", String.valueOf(totalStock), String.valueOf(totalPrestado),
                    String.valueOf(totalStock + totalPrestado), ""});

            if (proveedorId == null) {
                pdf.subtitulo("Existencias acumuladas por material (todos los proveedores)");
                List<String[]> acumuladas = new ArrayList<>();
                for (ExistenciaMaterial e : db.obtenerInventarioAgrupado()) {
                    acumuladas.add(new String[]{
                        safe(e.getNombre()),
                        safe(e.getUnidad()),
                        safe(e.getDesglose()),
                        String.valueOf(e.getDisponible()),
                        String.valueOf(e.getPrestado()),
                        String.valueOf(e.getTotal())
                    });
                }
                pdf.tabla(new String[]{"Material", "Unidad", "Desglose por proveedor", "Disponible", "Prestado", "Total"},
                    new float[]{22, 7, 41, 8, 7, 6},
                    new boolean[]{false, false, false, true, true, true},
                    acumuladas,
                    new String[]{"TOTAL", "", "", String.valueOf(totalStock), String.valueOf(totalPrestado),
                        String.valueOf(totalStock + totalPrestado)});
            }

            pdf.subtitulo("Resumen por unidad");
            List<String[]> resumen = new ArrayList<>();
            for (Map.Entry<String, Integer> e : porUnidad.entrySet()) {
                resumen.add(new String[]{e.getKey(), String.valueOf(e.getValue())});
            }
            pdf.tabla(new String[]{"Unidad", "Cantidad total"}, new float[]{3, 2}, new boolean[]{false, true}, resumen, null);
            return pdf.guardarTemporal("reporte_inventario_");
        }
    }

    // ---------------------------------------------------------------- Remisiones

    /** Comprobante de una remisión con todos sus materiales. */
    public static File remision(int remisionId) throws Exception {
        DatabaseManager db = DatabaseManager.getInstance();
        Remision r = db.obtenerRemisionPorId(remisionId);
        if (r == null) {
            return null;
        }
        List<DetalleRemision> detalles = db.obtenerDetallesRemision(remisionId);
        try (PdfReportBuilder pdf = new PdfReportBuilder("REMISIÓN DE ENTRADA DE MATERIAL", false)) {
            pdf.datos(new String[][]{
                {"No. remisión", guion(r.getNumeroRemision())},
                {"Folio interno", String.valueOf(r.getId())},
                {"Proveedor", safe(r.getProveedorNombre())},
                {"Fecha", r.getFecha() != null ? r.getFecha().format(FECHA) : ""},
                {"Obra", guion(r.getObra())},
                {"Envía", guion(r.getEnvia())},
                {"Recibió", guion(r.getRecibe())},
                {"Registrada", r.getFechaRegistro() != null ? r.getFechaRegistro().format(FECHA_HORA) : ""}
            }, 2);
            if (r.getObservaciones() != null && !r.getObservaciones().trim().isEmpty()) {
                pdf.datos(new String[][]{{"Observaciones", r.getObservaciones().trim()}}, 1);
            }
            pdf.espacio(6);
            List<String[]> filas = new ArrayList<>();
            int item = 1;
            int total = 0;
            for (DetalleRemision d : detalles) {
                filas.add(new String[]{
                    String.valueOf(item++),
                    safe(d.getNombreMaterial()),
                    safe(d.getCategoria()),
                    safe(d.getTipo()),
                    safe(d.getUnidad()),
                    String.valueOf(d.getCantidad())
                });
                total += d.getCantidad();
            }
            pdf.tabla(new String[]{"Item", "Elementos", "Categoría", "Tipo", "Unidad", "Cantidad"},
                new float[]{4, 30, 13, 10, 8, 7},
                new boolean[]{true, false, false, false, false, true},
                filas,
                new String[]{"", "TOTAL", "", "", "", String.valueOf(total)});
            pdf.firmas("Entrega: " + guion(r.getEnvia()), "Recibe: " + guion(r.getRecibe()));
            return pdf.guardarTemporal("remision_" + remisionId + "_");
        }
    }

    /** Entradas de material por remisión en un rango de fechas. */
    public static File entradasPorRemision(LocalDate desde, LocalDate hasta, Integer proveedorId) throws Exception {
        DatabaseManager db = DatabaseManager.getInstance();
        List<DetalleRemision> entradas = db.obtenerEntradasPorRemision(desde, hasta, proveedorId);
        if (entradas.isEmpty()) {
            return null;
        }
        try (PdfReportBuilder pdf = new PdfReportBuilder("REPORTE DE ENTRADAS POR REMISIÓN", true)) {
            String proveedor = "Todos";
            if (proveedorId != null) {
                Proveedor p = db.obtenerProveedorPorId(proveedorId);
                proveedor = p != null ? p.getNombre() : "";
            }
            pdf.datos(new String[][]{
                {"Desde", desde != null ? desde.format(FECHA) : "Inicio"},
                {"Hasta", hasta != null ? hasta.format(FECHA) : "Hoy"},
                {"Proveedor", proveedor}
            }, 3);
            pdf.espacio(6);
            List<String[]> filas = new ArrayList<>();
            int total = 0;
            for (DetalleRemision d : entradas) {
                filas.add(new String[]{
                    d.getFechaRemision() != null ? d.getFechaRemision().format(FECHA) : "",
                    guion(d.getNumeroRemision()),
                    safe(d.getProveedorNombre()),
                    safe(d.getNombreMaterial()),
                    safe(d.getCategoria()),
                    safe(d.getTipo()),
                    safe(d.getUnidad()),
                    String.valueOf(d.getCantidad())
                });
                total += d.getCantidad();
            }
            pdf.tabla(new String[]{"Fecha", "Remisión", "Proveedor", "Material", "Categoría", "Tipo", "Unidad", "Cantidad"},
                new float[]{8, 9, 14, 28, 13, 9, 8, 7},
                new boolean[]{false, false, false, false, false, false, false, true},
                filas,
                new String[]{"", "", "", "TOTAL (" + entradas.size() + " partidas)", "", "", "", String.valueOf(total)});
            return pdf.guardarTemporal("reporte_entradas_");
        }
    }

    // ---------------------------------------------------------------- Préstamos

    /** Préstamos activos agrupados por cliente y residente/sobrestante. */
    public static File prestamosActivos() throws Exception {
        List<ReportePrestamoItem> items = DatabaseManager.getInstance().obtenerReportePrestamosActivos();
        if (items.isEmpty()) {
            return null;
        }
        try (PdfReportBuilder pdf = new PdfReportBuilder("REPORTE DE PRÉSTAMOS ACTIVOS", false)) {
            LinkedHashMap<String, List<ReportePrestamoItem>> grupos = new LinkedHashMap<>();
            for (ReportePrestamoItem item : items) {
                String key = normalizarNombre(item.getNombreCliente()) + "||" + safe(item.getResidenteSobrestante());
                grupos.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
            }
            for (Map.Entry<String, List<ReportePrestamoItem>> entry : grupos.entrySet()) {
                String[] partes = entry.getKey().split("\\|\\|", -1);
                pdf.subtitulo("Cliente: " + partes[0]);
                pdf.linea("Residente/Sobrestante: " + (partes.length > 1 ? partes[1] : ""));
                List<String[]> filas = new ArrayList<>();
                int total = 0;
                for (ReportePrestamoItem item : entry.getValue()) {
                    filas.add(new String[]{
                        safe(item.getNombreHerramienta()),
                        safe(item.getCategoria()),
                        guion(item.getProveedor()),
                        safe(item.getUnidad()),
                        String.valueOf(item.getCantidad()),
                        fechaHora(item.getFechaPrestamo())
                    });
                    total += item.getCantidad();
                }
                pdf.tabla(new String[]{"Herramienta", "Categoría", "Proveedor", "Unidad", "Cantidad", "Fecha préstamo"},
                    new float[]{26, 15, 14, 8, 8, 13},
                    new boolean[]{false, false, false, false, true, false},
                    filas,
                    new String[]{"TOTAL", "", "", "", String.valueOf(total), ""});
            }
            return pdf.guardarTemporal("reporte_prestamos_activos_");
        }
    }

    /**
     * REPORTE POR HERRAMIENTA: existencias de la herramienta sumando todos sus proveedores
     * (con el desglose de cada uno), sus entradas por remisión y sus préstamos activos.
     */
    public static File prestamosPorHerramienta(String herramienta) throws Exception {
        DatabaseManager db = DatabaseManager.getInstance();
        List<Herramienta> existencias = db.obtenerExistenciasDeMaterial(herramienta);
        List<ReportePrestamoItem> prestamos = db.obtenerReportePrestamosActivosPorHerramienta(herramienta);
        if (existencias.isEmpty() && prestamos.isEmpty()) {
            return null;
        }
        List<DetalleRemision> entradas = db.obtenerEntradasPorRemision(null, null, null, herramienta);
        try (PdfReportBuilder pdf = new PdfReportBuilder("REPORTE POR HERRAMIENTA", true)) {
            String categoria = "";
            String tipo = "";
            String unidad = "";
            if (!existencias.isEmpty()) {
                Herramienta h = existencias.get(0);
                categoria = safe(h.getCategoria());
                tipo = safe(h.getTipo());
                unidad = safe(h.getUnidad());
            } else {
                categoria = safe(prestamos.get(0).getCategoria());
                unidad = safe(prestamos.get(0).getUnidad());
            }
            int disponible = 0;
            int prestado = 0;
            List<String[]> filas = new ArrayList<>();
            for (Herramienta h : existencias) {
                filas.add(new String[]{
                    guion(h.getProveedorNombre()),
                    safe(h.getUnidad()),
                    safe(h.getCategoria()),
                    safe(h.getTipo()),
                    String.valueOf(h.getStock()),
                    String.valueOf(h.getCantidadPrestada()),
                    String.valueOf(h.getStock() + h.getCantidadPrestada()),
                    guion(h.getRemision()),
                    fecha(h.getFechaRegistro())
                });
                disponible += h.getStock();
                prestado += h.getCantidadPrestada();
            }
            pdf.datos(new String[][]{
                {"Herramienta", safe(existencias.isEmpty() ? herramienta : existencias.get(0).getNombre())},
                {"Categoría", categoria},
                {"Tipo", tipo},
                {"Unidad", unidad},
                {"Proveedores", String.valueOf(existencias.size())},
                {"Disponible total", String.valueOf(disponible)},
                {"Prestado", String.valueOf(prestado)},
                {"Total", String.valueOf(disponible + prestado)}
            }, 4);

            pdf.subtitulo("Existencias por proveedor (se suman en el total)");
            if (filas.isEmpty()) {
                pdf.linea("La herramienta ya no tiene registros activos en el inventario.");
            } else {
                pdf.tabla(new String[]{"Proveedor", "Unidad", "Categoría", "Tipo", "Disponible", "Prestado", "Total",
                        "Últ. remisión", "Fecha alta"},
                    new float[]{20, 8, 14, 10, 8, 8, 7, 10, 9},
                    new boolean[]{false, false, false, false, true, true, true, false, false},
                    filas,
                    new String[]{"TOTAL", "", "", "", String.valueOf(disponible), String.valueOf(prestado),
                        String.valueOf(disponible + prestado), "", ""});
            }

            pdf.subtitulo("Entradas por remisión");
            if (entradas.isEmpty()) {
                pdf.linea("Sin entradas por remisión registradas.");
            } else {
                List<String[]> filasEntradas = new ArrayList<>();
                int totalEntradas = 0;
                for (DetalleRemision d : entradas) {
                    filasEntradas.add(new String[]{
                        d.getFechaRemision() != null ? d.getFechaRemision().format(FECHA) : "",
                        guion(d.getNumeroRemision()),
                        safe(d.getProveedorNombre()),
                        safe(d.getUnidad()),
                        String.valueOf(d.getCantidad())
                    });
                    totalEntradas += d.getCantidad();
                }
                pdf.tabla(new String[]{"Fecha", "Remisión", "Proveedor", "Unidad", "Cantidad"},
                    new float[]{10, 12, 24, 10, 8},
                    new boolean[]{false, false, false, false, true},
                    filasEntradas,
                    new String[]{"TOTAL", "", "", "", String.valueOf(totalEntradas)});
            }

            pdf.subtitulo("Préstamos activos");
            if (prestamos.isEmpty()) {
                pdf.linea("No hay préstamos activos de esta herramienta.");
            } else {
                List<String[]> filasPrestamos = new ArrayList<>();
                int total = 0;
                for (ReportePrestamoItem item : prestamos) {
                    filasPrestamos.add(new String[]{
                        fechaHora(item.getFechaPrestamo()),
                        guion(item.getProveedor()),
                        safe(item.getUnidad()),
                        safe(item.getCategoria()),
                        String.valueOf(item.getCantidad()),
                        safe(item.getNombreCliente()),
                        safe(item.getResidenteSobrestante())
                    });
                    total += item.getCantidad();
                }
                pdf.tabla(new String[]{"Fecha préstamo", "Proveedor", "Unidad", "Categoría", "Cantidad", "Cliente", "Residente"},
                    new float[]{11, 14, 7, 13, 7, 17, 17},
                    new boolean[]{false, false, false, false, true, false, false},
                    filasPrestamos,
                    new String[]{"TOTAL", "", "", "", String.valueOf(total), "", ""});
            }
            return pdf.guardarTemporal("reporte_herramienta_");
        }
    }

    // ---------------------------------------------------------------- Utilidades

    public static String safe(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private static String guion(String valor) {
        return valor == null || valor.trim().isEmpty() ? "-" : valor.trim();
    }

    private static String fechaHora(LocalDateTime fecha) {
        return fecha != null ? fecha.format(FECHA_HORA) : "";
    }

    /** Convierte una fecha guardada en ISO (fecha o fecha-hora) a dd/MM/yyyy. */
    public static String fecha(String iso) {
        if (iso == null || iso.trim().isEmpty()) {
            return "-";
        }
        try {
            if (iso.length() > 10) {
                return LocalDateTime.parse(iso).format(FECHA);
            }
            return LocalDate.parse(iso).format(FECHA);
        } catch (Exception e) {
            return iso;
        }
    }

    public static String normalizarNombre(String nombre) {
        if (nombre == null) {
            return "";
        }
        String limpio = nombre.trim().replaceAll("\\s+", " ").toLowerCase();
        StringBuilder sb = new StringBuilder();
        for (String p : limpio.split(" ")) {
            if (p.isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) {
                sb.append(p.substring(1));
            }
            sb.append(" ");
        }
        return sb.toString().trim();
    }
}

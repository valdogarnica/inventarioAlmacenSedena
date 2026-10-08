package com.almacen.report;

import com.almacen.database.DatabaseManager;
import com.almacen.model.DetallePrestamo;
import com.almacen.model.DetalleRemision;
import com.almacen.model.ExistenciaMaterial;
import com.almacen.model.Herramienta;
import com.almacen.model.Prestamo;
import com.almacen.model.Proveedor;
import com.almacen.model.Remision;
import com.almacen.model.ReportePrestamoItem;
import com.almacen.model.ResumenInicio;

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

    /**
     * Inventario por tipo de material. Con {@code tipo} null incluye el resumen de todos los
     * tipos y el detalle de cada uno; con un tipo, solo sus materiales.
     */
    public static File inventarioPorTipo(String tipo) throws Exception {
        DatabaseManager db = DatabaseManager.getInstance();
        java.util.Set<String> noRetorno = db.obtenerTiposNoRetorno();
        Map<Integer, Integer> entregado = db.obtenerEntregadoNoRetorno();
        Map<String, List<Herramienta>> porTipo = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Herramienta h : db.obtenerInventarioGeneral(null)) {
            String t = h.getTipo() == null || h.getTipo().trim().isEmpty() ? "Sin tipo" : h.getTipo().trim();
            if (tipo == null || t.equalsIgnoreCase(tipo.trim())) {
                porTipo.computeIfAbsent(t, k -> new ArrayList<>()).add(h);
            }
        }
        if (porTipo.isEmpty()) {
            return null;
        }
        try (PdfReportBuilder pdf = new PdfReportBuilder("REPORTE DE INVENTARIO POR TIPO", true)) {
            if (tipo == null) {
                pdf.subtitulo("Resumen por tipo");
                List<String[]> resumen = new ArrayList<>();
                int[] tot = new int[5];
                for (Map.Entry<String, List<Herramienta>> e : porTipo.entrySet()) {
                    int[] t = totales(e.getValue(), entregado);
                    resumen.add(new String[]{e.getKey(), devolucion(noRetorno, e.getKey()),
                        String.valueOf(materialesDistintos(e.getValue())), String.valueOf(t[0]),
                        String.valueOf(t[1]), String.valueOf(t[2]), String.valueOf(t[0] + t[1])});
                    tot[0] += t[0];
                    tot[1] += t[1];
                    tot[2] += t[2];
                }
                pdf.tabla(new String[]{"Tipo", "Devolución", "Materiales", "Disponible", "Prestado",
                        "Entregado (no retorno)", "Existencia"},
                    new float[]{18, 12, 10, 10, 10, 14, 10},
                    new boolean[]{false, false, true, true, true, true, true},
                    resumen,
                    new String[]{"TOTAL", "", "", String.valueOf(tot[0]), String.valueOf(tot[1]),
                        String.valueOf(tot[2]), String.valueOf(tot[0] + tot[1])});
                pdf.subtitulo("Piezas disponibles por tipo");
                List<String> etiquetas = new ArrayList<>();
                List<Integer> valores = new ArrayList<>();
                for (Map.Entry<String, List<Herramienta>> e : porTipo.entrySet()) {
                    etiquetas.add(e.getKey());
                    valores.add(totales(e.getValue(), entregado)[0]);
                }
                pdf.barras(etiquetas, valores);
            }
            for (Map.Entry<String, List<Herramienta>> e : porTipo.entrySet()) {
                e.getValue().sort((x, y) -> safe(x.getNombre()).compareToIgnoreCase(safe(y.getNombre())));
                pdf.subtitulo("Tipo: " + e.getKey() + "  (" + devolucion(noRetorno, e.getKey()).toLowerCase() + ")");
                List<String[]> filas = new ArrayList<>();
                for (Herramienta h : e.getValue()) {
                    filas.add(new String[]{
                        safe(h.getNombre()),
                        safe(h.getCategoria()),
                        safe(h.getUnidad()),
                        guion(h.getProveedorNombre()),
                        String.valueOf(h.getStock()),
                        String.valueOf(h.getCantidadPrestada()),
                        String.valueOf(entregado.getOrDefault(h.getId(), 0)),
                        String.valueOf(h.getStock() + h.getCantidadPrestada())
                    });
                }
                int[] t = totales(e.getValue(), entregado);
                pdf.tabla(new String[]{"Material", "Categoría", "Unidad", "Proveedor", "Disponible", "Prestado",
                        "Entregado", "Existencia"},
                    new float[]{24, 15, 8, 16, 9, 8, 8, 9},
                    new boolean[]{false, false, false, false, true, true, true, true},
                    filas,
                    new String[]{"TOTAL", "", "", "", String.valueOf(t[0]), String.valueOf(t[1]),
                        String.valueOf(t[2]), String.valueOf(t[0] + t[1])});
            }
            pdf.linea("Entregado = piezas de material de no retorno que salieron en préstamos y no se devuelven.");
            return pdf.guardarTemporal("reporte_tipo_");
        }
    }

    private static int[] totales(List<Herramienta> materiales, Map<Integer, Integer> entregado) {
        int[] t = new int[3];
        for (Herramienta h : materiales) {
            t[0] += h.getStock();
            t[1] += h.getCantidadPrestada();
            t[2] += entregado.getOrDefault(h.getId(), 0);
        }
        return t;
    }

    private static int materialesDistintos(List<Herramienta> materiales) {
        java.util.Set<String> claves = new java.util.HashSet<>();
        for (Herramienta h : materiales) {
            claves.add(safe(h.getNombre()).trim().toLowerCase() + "|" + safe(h.getUnidad()).trim().toLowerCase());
        }
        return claves.size();
    }

    private static String devolucion(java.util.Set<String> noRetorno, String tipo) {
        return noRetorno.contains(tipo.trim().toLowerCase()) ? "No retorno" : "Se devuelve";
    }

    /** Análisis del almacén (lo mismo que la página Inicio): indicadores, gráficas y listas. */
    public static File analisisAlmacen(int umbralBajo) throws Exception {
        ResumenInicio r = DatabaseManager.getInstance().obtenerResumenInicio(umbralBajo);
        if (r.materiales == 0) {
            return null;
        }
        try (PdfReportBuilder pdf = new PdfReportBuilder("ANÁLISIS DEL ALMACÉN", false)) {
            pdf.datos(new String[][]{
                {"Materiales", String.valueOf(r.materiales)},
                {"Piezas disponibles", String.valueOf(r.disponible)},
                {"Piezas prestadas", String.valueOf(r.prestado)},
                {"Préstamos activos", String.valueOf(r.prestamosActivos)},
                {"Sin stock", String.valueOf(r.sinStock.size())},
                {"Stock bajo (hasta " + umbralBajo + ")", String.valueOf(r.stockBajo.size())}
            }, 3);
            pdf.subtitulo("Análisis");
            for (String linea : com.almacen.ui.InicioPanel.analisis(r)) {
                // La fuente del PDF no tiene viñeta "•"
                pdf.parrafo(linea.replace("• ", "- "));
            }
            graficaPdf(pdf, "Préstamos por mes (últimos 6 meses)", r.prestamosPorMes);
            graficaPdf(pdf, "Piezas disponibles por tipo", r.stockPorTipo);
            graficaPdf(pdf, "Herramientas más prestadas", r.masPrestados);
            graficaPdf(pdf, "Material de no retorno más entregado", r.masEntregadosNoRetorno);
            graficaPdf(pdf, "Piezas disponibles por proveedor", r.stockPorProveedor);
            if (!r.sinStock.isEmpty()) {
                pdf.subtitulo("Materiales sin stock");
                pdf.tabla(new String[]{"Material", "Unidad", "Tipo", "Prestado"}, new float[]{40, 15, 25, 12},
                    new boolean[]{false, false, false, true}, filasMateriales(r.sinStock, false), null);
            }
            if (!r.stockBajo.isEmpty()) {
                pdf.subtitulo("Materiales con stock bajo (" + umbralBajo + " o menos)");
                pdf.tabla(new String[]{"Material", "Unidad", "Tipo", "Disponible"}, new float[]{40, 15, 25, 12},
                    new boolean[]{false, false, false, true}, filasMateriales(r.stockBajo, true), null);
            }
            if (!r.prestamosAntiguos.isEmpty()) {
                pdf.subtitulo("Préstamos pendientes más antiguos");
                List<String[]> filas = new ArrayList<>();
                for (ResumenInicio.PrestamoPendiente p : r.prestamosAntiguos) {
                    filas.add(new String[]{"#" + p.id, safe(p.cliente), p.fecha, String.valueOf(p.dias),
                        String.valueOf(p.pendientes)});
                }
                pdf.tabla(new String[]{"Préstamo", "Cliente", "Fecha", "Días", "Pendientes"},
                    new float[]{10, 38, 16, 10, 12}, new boolean[]{false, false, false, true, true}, filas, null);
            }
            return pdf.guardarTemporal("analisis_almacen_");
        }
    }

    private static void graficaPdf(PdfReportBuilder pdf, String titulo, List<ResumenInicio.Dato> datos) throws Exception {
        if (datos.isEmpty()) {
            return;
        }
        pdf.subtitulo(titulo);
        List<String> etiquetas = new ArrayList<>();
        List<Integer> valores = new ArrayList<>();
        for (ResumenInicio.Dato d : datos) {
            etiquetas.add(d.etiqueta);
            valores.add(d.valor);
        }
        pdf.barras(etiquetas, valores);
    }

    private static List<String[]> filasMateriales(List<ResumenInicio.Material> materiales, boolean disponible) {
        List<String[]> filas = new ArrayList<>();
        for (ResumenInicio.Material m : materiales) {
            filas.add(new String[]{safe(m.nombre), guion(m.unidad), guion(m.tipo),
                String.valueOf(disponible ? m.disponible : m.prestado)});
        }
        return filas;
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

    /**
     * Comprobante de devolución de un préstamo. {@code devueltoAhora} y {@code observaciones}
     * van en el mismo orden que {@code detalles}; con {@code vistaPrevia} se marca que la
     * devolución todavía no se registra.
     */
    public static File comprobanteDevolucion(Prestamo p, List<DetallePrestamo> detalles, List<Integer> devueltoAhora,
                                             List<String> observaciones, boolean vistaPrevia) throws Exception {
        String titulo = vistaPrevia ? "COMPROBANTE DE DEVOLUCIÓN (VISTA PREVIA)" : "COMPROBANTE DE DEVOLUCIÓN";
        try (PdfReportBuilder pdf = new PdfReportBuilder(titulo, false)) {
            List<String[]> filas = new ArrayList<>();
            List<String[]> entregado = new ArrayList<>();
            int totPrestado = 0;
            int totAntes = 0;
            int totAhora = 0;
            int totPendiente = 0;
            int item = 1;
            for (int i = 0; i < detalles.size(); i++) {
                DetallePrestamo d = detalles.get(i);
                if (d.isNoRetorno()) {
                    entregado.add(new String[]{safe(d.getNombreHerramienta()), guion(d.getUnidad()),
                        String.valueOf(d.getCantidad())});
                    continue;
                }
                int ahora = devueltoAhora != null && i < devueltoAhora.size() ? devueltoAhora.get(i) : 0;
                int pendiente = Math.max(0, d.getPendiente() - ahora);
                String obs = observaciones != null && i < observaciones.size() ? safe(observaciones.get(i)) : "";
                filas.add(new String[]{
                    String.valueOf(item++),
                    safe(d.getNombreHerramienta()),
                    guion(d.getUnidad()),
                    String.valueOf(d.getCantidad()),
                    String.valueOf(d.getCantidadDevuelta()),
                    String.valueOf(ahora),
                    String.valueOf(pendiente),
                    obs.isEmpty() ? "-" : obs
                });
                totPrestado += d.getCantidad();
                totAntes += d.getCantidadDevuelta();
                totAhora += ahora;
                totPendiente += pendiente;
            }
            String resultado = totPendiente == 0 ? "Devolución completa"
                : (totPendiente == 1 ? "Devolución parcial: queda 1 pendiente" : "Devolución parcial: quedan " + totPendiente + " pendientes");
            pdf.datos(new String[][]{
                {"Préstamo", String.valueOf(p.getId())},
                {"Folio de autorización", p.isAutorizacion() ? guion(p.getFolio()) : "Sin autorización"},
                {"Cliente", safe(p.getNombreCliente())},
                {"Residente/Sobrestante", guion(p.getResidenteSobrestante())},
                {"Prestó", guion(p.getNombreEmpleado())},
                {"Fecha del préstamo", p.getFechaPrestamo() != null ? p.getFechaPrestamo().format(FECHA_HORA) : ""},
                {"Fecha de devolución", LocalDateTime.now().format(FECHA_HORA)},
                {"Resultado", resultado}
            }, 2);
            pdf.espacio(6);
            pdf.subtitulo("Herramientas devueltas");
            pdf.tabla(new String[]{"#", "Herramienta", "Unidad", "Prestado", "Ya devuelto", "Devuelve hoy",
                    "Pendiente", "Observación"},
                new float[]{3, 17, 7, 8, 10, 12, 9, 18},
                new boolean[]{true, false, false, true, true, true, true, false},
                filas,
                new String[]{"", "TOTAL", "", String.valueOf(totPrestado), String.valueOf(totAntes),
                    String.valueOf(totAhora), String.valueOf(totPendiente), ""});
            if (!entregado.isEmpty()) {
                pdf.espacio(8);
                pdf.subtitulo("Material de no retorno entregado con el préstamo (no se devuelve)");
                pdf.tabla(new String[]{"Material", "Unidad", "Cantidad"},
                    new float[]{30, 10, 8},
                    new boolean[]{false, false, true},
                    entregado, null);
            }
            pdf.firmas("Entrega: " + safe(p.getNombreCliente()), "Recibe (almacén)");
            return pdf.guardarTemporal("comprobante_devolucion_" + p.getId() + "_");
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

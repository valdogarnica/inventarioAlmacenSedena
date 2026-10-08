package com.almacen.report;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.awt.Color;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generador sencillo de reportes PDF con encabezado (título y fecha de generación),
 * bloques de datos y tablas con salto de página automático y numeración de páginas.
 */
public class PdfReportBuilder implements Closeable {
    private static final PDFont FONT = PDType1Font.HELVETICA;
    private static final PDFont FONT_BOLD = PDType1Font.HELVETICA_BOLD;
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Color HEADER_BG = new Color(45, 108, 223);
    private static final Color ZEBRA_BG = new Color(242, 245, 251);
    private static final Color TOTAL_BG = new Color(225, 230, 240);

    private final PDDocument document;
    private final PDRectangle pageSize;
    private final String titulo;
    private final String fechaGeneracion;
    private final float margin = 36;
    private PDPageContentStream content;
    private float y;

    // Tabla en curso (para repetir el encabezado al cambiar de página)
    private String[] tablaHeaders;
    private float[] tablaWidths;
    private boolean[] tablaDerecha;

    public PdfReportBuilder(String titulo, boolean horizontal) throws IOException {
        this.document = new PDDocument();
        this.pageSize = horizontal
                ? new PDRectangle(PDRectangle.LETTER.getHeight(), PDRectangle.LETTER.getWidth())
                : PDRectangle.LETTER;
        this.titulo = titulo;
        this.fechaGeneracion = LocalDateTime.now().format(FECHA_HORA);
        nuevaPagina();
    }

    public float getAnchoUtil() {
        return pageSize.getWidth() - margin * 2;
    }

    private void nuevaPagina() throws IOException {
        if (content != null) {
            content.close();
        }
        PDPage page = new PDPage(pageSize);
        document.addPage(page);
        content = new PDPageContentStream(document, page);
        float top = pageSize.getHeight() - margin;

        content.setNonStrokingColor(HEADER_BG);
        content.addRect(margin, top - 4, getAnchoUtil(), 3);
        content.fill();
        content.setNonStrokingColor(Color.BLACK);

        texto(FONT_BOLD, 14, titulo, margin, top - 22);
        String generado = "Generado: " + fechaGeneracion;
        texto(FONT, 9, generado, pageSize.getWidth() - margin - ancho(FONT, 9, generado), top - 20);

        content.setStrokingColor(new Color(200, 205, 215));
        content.setLineWidth(0.6f);
        content.moveTo(margin, top - 30);
        content.lineTo(pageSize.getWidth() - margin, top - 30);
        content.stroke();
        content.setStrokingColor(Color.BLACK);
        y = top - 46;
    }

    private void asegurarEspacio(float alto) throws IOException {
        if (y - alto < margin + 20) {
            nuevaPagina();
            if (tablaHeaders != null) {
                dibujarEncabezadoTabla();
            }
        }
    }

    /** Línea de texto normal. */
    public void linea(String texto) throws IOException {
        asegurarEspacio(14);
        texto(FONT, 10, texto, margin, y);
        y -= 14;
    }

    /** Subtítulo en negritas. */
    public void subtitulo(String texto) throws IOException {
        asegurarEspacio(30);
        y -= 4;
        texto(FONT_BOLD, 11, texto, margin, y);
        y -= 16;
    }

    /**
     * Bloque de datos tipo "Etiqueta: valor" acomodado en columnas.
     */
    public void datos(String[][] pares, int columnas) throws IOException {
        float anchoCol = getAnchoUtil() / columnas;
        for (int i = 0; i < pares.length; i += columnas) {
            asegurarEspacio(14);
            for (int c = 0; c < columnas && i + c < pares.length; c++) {
                float x = margin + c * anchoCol;
                String etiqueta = pares[i + c][0] + ": ";
                texto(FONT_BOLD, 10, etiqueta, x, y);
                float wEtiqueta = ancho(FONT_BOLD, 10, etiqueta);
                texto(FONT, 10, recortar(FONT, 10, pares[i + c][1], anchoCol - wEtiqueta - 8), x + wEtiqueta, y);
            }
            y -= 14;
        }
    }

    /** Dos líneas de firma al pie del contenido. */
    public void firmas(String izquierda, String derecha) throws IOException {
        asegurarEspacio(70);
        y -= 45;
        float anchoFirma = 200;
        float xIzq = margin + 20;
        float xDer = pageSize.getWidth() - margin - 20 - anchoFirma;
        content.setLineWidth(0.7f);
        content.moveTo(xIzq, y);
        content.lineTo(xIzq + anchoFirma, y);
        content.moveTo(xDer, y);
        content.lineTo(xDer + anchoFirma, y);
        content.stroke();
        String izq = recortar(FONT, 9, izquierda, anchoFirma);
        String der = recortar(FONT, 9, derecha, anchoFirma);
        texto(FONT, 9, izq, xIzq + (anchoFirma - ancho(FONT, 9, izq)) / 2, y - 12);
        texto(FONT, 9, der, xDer + (anchoFirma - ancho(FONT, 9, der)) / 2, y - 12);
        y -= 24;
    }

    /** Texto que se acomoda en varias líneas según el ancho de la página. */
    public void parrafo(String texto) throws IOException {
        float maximo = getAnchoUtil();
        StringBuilder linea = new StringBuilder();
        for (String palabra : texto.split(" ")) {
            String prueba = linea.length() == 0 ? palabra : linea + " " + palabra;
            if (linea.length() > 0 && ancho(FONT, 10, prueba) > maximo) {
                linea(linea.toString());
                linea = new StringBuilder("   " + palabra);
            } else {
                linea = new StringBuilder(prueba);
            }
        }
        if (linea.length() > 0) {
            linea(linea.toString());
        }
    }

    /** Gráfica de barras horizontales sencilla: etiqueta, barra y valor. */
    public void barras(List<String> etiquetas, List<Integer> valores) throws IOException {
        int max = 1;
        for (int v : valores) {
            max = Math.max(max, v);
        }
        float anchoEtiqueta = getAnchoUtil() * 0.28f;
        float anchoBarras = getAnchoUtil() - anchoEtiqueta - 50;
        float alto = 11;
        for (int i = 0; i < etiquetas.size(); i++) {
            asegurarEspacio(alto + 6);
            int v = valores.get(i);
            texto(FONT, 9, recortar(FONT, 9, etiquetas.get(i), anchoEtiqueta - 6), margin, y - 8);
            float x = margin + anchoEtiqueta;
            float largo = v <= 0 ? 0 : Math.max(2, anchoBarras * v / max);
            if (largo > 0) {
                content.setNonStrokingColor(HEADER_BG);
                content.addRect(x, y - alto, largo, alto);
                content.fill();
                content.setNonStrokingColor(Color.BLACK);
            }
            texto(FONT_BOLD, 9, String.valueOf(v), x + largo + 5, y - 8);
            y -= alto + 6;
        }
        y -= 10;
    }

    public void espacio(float alto) {
        y -= alto;
    }

    /**
     * Dibuja una tabla. Los anchos son relativos (se escalan al ancho útil de la página).
     * {@code derecha} indica qué columnas se alinean a la derecha (números); puede ser null.
     * {@code totales} es una fila final opcional resaltada.
     */
    public void tabla(String[] headers, float[] anchosRelativos, boolean[] derecha,
                      List<String[]> filas, String[] totales) throws IOException {
        float suma = 0;
        for (float w : anchosRelativos) {
            suma += w;
        }
        tablaWidths = new float[anchosRelativos.length];
        for (int i = 0; i < anchosRelativos.length; i++) {
            tablaWidths[i] = anchosRelativos[i] / suma * getAnchoUtil();
        }
        tablaHeaders = headers;
        tablaDerecha = derecha != null ? derecha : new boolean[headers.length];

        asegurarEspacio(40);
        dibujarEncabezadoTabla();
        int n = 0;
        for (String[] fila : filas) {
            asegurarEspacio(16);
            dibujarFila(fila, n % 2 == 1 ? ZEBRA_BG : null, FONT);
            n++;
        }
        if (totales != null) {
            asegurarEspacio(16);
            dibujarFila(totales, TOTAL_BG, FONT_BOLD);
        }
        tablaHeaders = null;
        y -= 8;
    }

    private void dibujarEncabezadoTabla() throws IOException {
        float alto = 18;
        content.setNonStrokingColor(HEADER_BG);
        content.addRect(margin, y - alto + 12, getAnchoUtil(), alto);
        content.fill();
        content.setNonStrokingColor(Color.WHITE);
        float x = margin;
        for (int i = 0; i < tablaHeaders.length; i++) {
            celda(FONT_BOLD, 9, tablaHeaders[i], x, tablaWidths[i], tablaDerecha[i]);
            x += tablaWidths[i];
        }
        content.setNonStrokingColor(Color.BLACK);
        y -= alto;
    }

    private void dibujarFila(String[] fila, Color fondo, PDFont font) throws IOException {
        float alto = 16;
        if (fondo != null) {
            content.setNonStrokingColor(fondo);
            content.addRect(margin, y - alto + 12, getAnchoUtil(), alto);
            content.fill();
            content.setNonStrokingColor(Color.BLACK);
        }
        float x = margin;
        for (int i = 0; i < tablaWidths.length; i++) {
            String valor = i < fila.length ? fila[i] : "";
            celda(font, 9, valor, x, tablaWidths[i], tablaDerecha[i]);
            x += tablaWidths[i];
        }
        content.setStrokingColor(new Color(215, 220, 230));
        content.setLineWidth(0.4f);
        content.moveTo(margin, y - alto + 12);
        content.lineTo(margin + getAnchoUtil(), y - alto + 12);
        content.stroke();
        content.setStrokingColor(Color.BLACK);
        y -= alto;
    }

    private void celda(PDFont font, float size, String valor, float x, float anchoCol, boolean derecha) throws IOException {
        String t = recortar(font, size, valor, anchoCol - 8);
        float tx = derecha ? x + anchoCol - 4 - ancho(font, size, t) : x + 4;
        texto(font, size, t, tx, y);
    }

    private void texto(PDFont font, float size, String texto, float x, float yPos) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, yPos);
        content.showText(limpiar(texto));
        content.endText();
    }

    private static float ancho(PDFont font, float size, String texto) throws IOException {
        return font.getStringWidth(limpiar(texto)) / 1000f * size;
    }

    private static String recortar(PDFont font, float size, String texto, float maxAncho) throws IOException {
        String t = limpiar(texto);
        if (ancho(font, size, t) <= maxAncho) {
            return t;
        }
        while (t.length() > 1 && ancho(font, size, t + "...") > maxAncho) {
            t = t.substring(0, t.length() - 1);
        }
        return t + "...";
    }

    /** Las fuentes estándar de PDF solo soportan Latin-1: se sustituye lo demás. */
    private static String limpiar(String texto) {
        if (texto == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(texto.length());
        for (char c : texto.toCharArray()) {
            if (c == '\n' || c == '\r' || c == '\t') {
                sb.append(' ');
            } else if ((c >= 32 && c < 127) || (c >= 160 && c <= 255)) {
                sb.append(c);
            } else if (c == '–' || c == '—') {
                sb.append('-');
            } else if (c == '“' || c == '”') {
                sb.append('"');
            } else if (c == '‘' || c == '’') {
                sb.append('\'');
            } else {
                sb.append('?');
            }
        }
        return sb.toString();
    }

    /** Guarda el reporte en un archivo temporal y devuelve el archivo. */
    public File guardarTemporal(String prefijo) throws IOException {
        content.close();
        content = null;
        int total = document.getNumberOfPages();
        for (int i = 0; i < total; i++) {
            PDPage page = document.getPage(i);
            try (PDPageContentStream pie = new PDPageContentStream(document, page,
                    PDPageContentStream.AppendMode.APPEND, true, true)) {
                String texto = "Página " + (i + 1) + " de " + total;
                pie.beginText();
                pie.setFont(FONT, 8);
                pie.newLineAtOffset(pageSize.getWidth() - margin - ancho(FONT, 8, texto), margin - 12);
                pie.showText(limpiar(texto));
                pie.endText();
                pie.beginText();
                pie.setFont(FONT, 8);
                pie.newLineAtOffset(margin, margin - 12);
                pie.showText(limpiar("Sistema de Inventario de Almacén"));
                pie.endText();
            }
        }
        File archivo = File.createTempFile(prefijo, ".pdf");
        archivo.deleteOnExit();
        document.save(archivo);
        return archivo;
    }

    @Override
    public void close() throws IOException {
        if (content != null) {
            content.close();
        }
        document.close();
    }
}

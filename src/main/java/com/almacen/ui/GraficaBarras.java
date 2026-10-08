package com.almacen.ui;

import com.almacen.model.ResumenInicio.Dato;

import javax.swing.JComponent;
import javax.swing.ToolTipManager;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Gráfica de barras de una sola serie, con los colores del tema. Horizontal (etiqueta a la
 * izquierda, valor al final de la barra) o en columnas (por ejemplo, préstamos por mes).
 * Al pasar el mouse se resalta la barra y se muestra su valor.
 */
public class GraficaBarras extends JComponent {
    private static final int ALTO_BARRA = 18;
    private static final int SEPARACION = 10;

    private final boolean columnas;
    private final String unidad;
    private List<Dato> datos = new ArrayList<>();
    private int resaltada = -1;

    /** @param unidad palabra para el valor en la ayuda, por ejemplo "piezas" o "préstamos". */
    public GraficaBarras(boolean columnas, String unidad) {
        this.columnas = columnas;
        this.unidad = unidad;
        setOpaque(false);
        ToolTipManager.sharedInstance().registerComponent(this);
        MouseAdapter raton = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                cambiarResaltada(indiceEn(e.getX(), e.getY()));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                cambiarResaltada(-1);
            }
        };
        addMouseListener(raton);
        addMouseMotionListener(raton);
    }

    public void setDatos(List<Dato> datos) {
        this.datos = datos != null ? new ArrayList<>(datos) : new ArrayList<>();
        resaltada = -1;
        revalidate();
        repaint();
    }

    private void cambiarResaltada(int indice) {
        if (indice != resaltada) {
            resaltada = indice;
            repaint();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (columnas) {
            return new Dimension(360, 210);
        }
        int filas = Math.max(1, datos.size());
        return new Dimension(360, filas * (ALTO_BARRA + SEPARACION) + 6);
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        int i = indiceEn(e.getX(), e.getY());
        if (i < 0) {
            return null;
        }
        Dato d = datos.get(i);
        return d.etiqueta + ": " + d.valor + " " + unidad;
    }

    private int maximo() {
        int max = 0;
        for (Dato d : datos) {
            max = Math.max(max, d.valor);
        }
        return Math.max(1, max);
    }

    // ---------------------------------------------------------------- geometría

    private int anchoEtiquetas(FontMetrics fm) {
        int max = 0;
        for (Dato d : datos) {
            max = Math.max(max, fm.stringWidth(d.etiqueta));
        }
        return Math.min(Math.max(60, max), Math.max(60, getWidth() * 2 / 5));
    }

    private int indiceEn(int x, int y) {
        if (datos.isEmpty()) {
            return -1;
        }
        if (columnas) {
            float paso = getWidth() / (float) datos.size();
            int i = (int) (x / paso);
            return i >= 0 && i < datos.size() ? i : -1;
        }
        int i = y / (ALTO_BARRA + SEPARACION);
        return i >= 0 && i < datos.size() ? i : -1;
    }

    // ---------------------------------------------------------------- dibujo

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(getFont());
        if (datos.isEmpty()) {
            g2.setColor(Tema.color("App.muted"));
            String texto = "Todavía no hay datos";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(texto, (getWidth() - fm.stringWidth(texto)) / 2, getHeight() / 2);
        } else if (columnas) {
            pintarColumnas(g2);
        } else {
            pintarHorizontal(g2);
        }
        g2.dispose();
    }

    private void pintarHorizontal(Graphics2D g2) {
        FontMetrics fm = g2.getFontMetrics();
        int etiquetas = anchoEtiquetas(fm);
        int inicio = etiquetas + 12;
        Font negrita = getFont().deriveFont(Font.BOLD);
        int anchoValor = g2.getFontMetrics(negrita).stringWidth(String.valueOf(maximo())) + 10;
        int disponible = Math.max(10, getWidth() - inicio - anchoValor);
        int max = maximo();
        for (int i = 0; i < datos.size(); i++) {
            Dato d = datos.get(i);
            int y = i * (ALTO_BARRA + SEPARACION) + 3;
            int base = y + ALTO_BARRA / 2 + fm.getAscent() / 2 - 2;
            g2.setColor(Tema.color("App.text"));
            g2.drawString(recortar(d.etiqueta, fm, etiquetas), 0, base);
            // Riel de fondo y barra (extremo redondeado de 4 px)
            g2.setColor(Tema.color("App.borderMuted"));
            g2.fillRect(inicio, y + ALTO_BARRA / 2, disponible, 1);
            int largo = d.valor <= 0 ? 0 : Math.max(4, Math.round(disponible * d.valor / (float) max));
            if (largo > 0) {
                g2.setColor(Tema.color(i == resaltada ? "App.accentHover" : "App.accent"));
                g2.fill(new RoundRectangle2D.Float(inicio, y, largo, ALTO_BARRA, 8, 8));
                g2.fillRect(inicio, y, Math.min(4, largo), ALTO_BARRA);
            }
            g2.setFont(negrita);
            g2.setColor(Tema.color(i == resaltada ? "App.text" : "App.muted"));
            g2.drawString(String.valueOf(d.valor), inicio + largo + 6, base);
            g2.setFont(getFont());
        }
    }

    private void pintarColumnas(Graphics2D g2) {
        FontMetrics fm = g2.getFontMetrics();
        int abajo = getHeight() - fm.getHeight() - 6;
        int arriba = fm.getHeight() + 6;
        int alto = Math.max(10, abajo - arriba);
        int max = maximo();
        float paso = getWidth() / (float) datos.size();
        int ancho = Math.max(8, Math.min(44, Math.round(paso * 0.55f)));
        // Línea base
        g2.setColor(Tema.color("App.border"));
        g2.fillRect(0, abajo, getWidth(), 1);
        for (int i = 0; i < datos.size(); i++) {
            Dato d = datos.get(i);
            int centro = Math.round(paso * i + paso / 2);
            int largo = d.valor <= 0 ? 0 : Math.max(4, Math.round(alto * d.valor / (float) max));
            if (largo > 0) {
                g2.setColor(Tema.color(i == resaltada ? "App.accentHover" : "App.accent"));
                g2.fill(new RoundRectangle2D.Float(centro - ancho / 2f, abajo - largo, ancho, largo, 8, 8));
                g2.fillRect(centro - ancho / 2, abajo - Math.min(4, largo), ancho, Math.min(4, largo));
            }
            String valor = String.valueOf(d.valor);
            g2.setColor(Tema.color(i == resaltada ? "App.text" : "App.muted"));
            g2.drawString(valor, centro - fm.stringWidth(valor) / 2, abajo - largo - 5);
            g2.setColor(Tema.color("App.muted"));
            String etiqueta = recortar(d.etiqueta, fm, Math.round(paso) - 4);
            g2.drawString(etiqueta, centro - fm.stringWidth(etiqueta) / 2, abajo + fm.getAscent() + 4);
        }
    }

    private static String recortar(String texto, FontMetrics fm, int ancho) {
        if (texto == null) {
            return "";
        }
        if (fm.stringWidth(texto) <= ancho) {
            return texto;
        }
        String puntos = "…";
        int fin = texto.length();
        while (fin > 1 && fm.stringWidth(texto.substring(0, fin) + puntos) > ancho) {
            fin--;
        }
        return texto.substring(0, fin) + puntos;
    }
}

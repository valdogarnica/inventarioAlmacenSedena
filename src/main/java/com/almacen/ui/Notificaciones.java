package com.almacen.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Avisos de la aplicación. Los de éxito y error aparecen como notificación flotante en la
 * esquina superior derecha y se cierran solos; los demás se muestran con {@link Alerta}.
 */
public final class Notificaciones {
    private static NotificacionBase actual;

    private Notificaciones() {
    }

    public static void showMessageDialog(Component parent, Object message) {
        Alerta.mostrar(parent, message, "Aviso", Alerta.Tipo.INFO);
    }

    public static void showMessageDialog(Component parent, Object message, String title, int messageType) {
        if (messageType == JOptionPane.ERROR_MESSAGE && !(message instanceof Component)) {
            error(parent, String.valueOf(message));
            return;
        }
        if (messageType == JOptionPane.INFORMATION_MESSAGE && esExito(title) && !(message instanceof Component)) {
            exito(parent, String.valueOf(message));
            return;
        }
        Alerta.mostrar(parent, message, title, tipo(messageType));
    }

    public static void showMessageDialog(Component parent, Object message, String title, int messageType, Icon icon) {
        showMessageDialog(parent, message, title, messageType);
    }

    public static void exito(Component parent, String mensaje) {
        mostrarFlotante(parent, "Listo", mensaje, Alerta.Tipo.EXITO);
    }

    public static void error(Component parent, String mensaje) {
        mostrarFlotante(parent, "Error", mensaje, Alerta.Tipo.ERROR);
    }

    private static void mostrarFlotante(Component parent, String titulo, String mensaje, Alerta.Tipo tipo) {
        Window owner = parent instanceof Window ? (Window) parent : SwingUtilities.getWindowAncestor(parent);
        if (owner == null || !owner.isVisible()) {
            Alerta.mostrar(parent, mensaje, titulo, tipo);
            return;
        }
        if (actual != null) {
            actual.dispose();
        }
        actual = new NotificacionBase(owner, titulo, mensaje, tipo);
        actual.mostrar();
    }

    private static Alerta.Tipo tipo(int messageType) {
        switch (messageType) {
            case JOptionPane.ERROR_MESSAGE: return Alerta.Tipo.ERROR;
            case JOptionPane.WARNING_MESSAGE: return Alerta.Tipo.AVISO;
            case JOptionPane.QUESTION_MESSAGE: return Alerta.Tipo.PREGUNTA;
            default: return Alerta.Tipo.INFO;
        }
    }

    private static boolean esExito(String title) {
        if (title == null) {
            return false;
        }
        String t = title.trim().toLowerCase();
        return t.contains("éxito") || t.contains("exito");
    }

    /** Tarjeta flotante con franja de color, icono, mensaje y barra del tiempo que falta. */
    private static final class NotificacionBase extends JWindow {
        private static final int ANCHO = 380;
        private static final int DURACION_MS = 5000;
        private final Timer animacion;
        private final Alerta.Tipo tipo;
        private final int xFinal;
        private long inicio;
        private long inicioCierre = -1;
        private float restante = 1f;

        private NotificacionBase(Window owner, String titulo, String mensaje, Alerta.Tipo tipo) {
            super(owner);
            this.tipo = tipo;
            setBackground(new Color(0, 0, 0, 0));
            setFocusableWindowState(false);

            JPanel tarjeta = new JPanel(new BorderLayout(12, 0)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    Shape forma = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                    g2.setColor(Tema.color("App.card"));
                    g2.fill(forma);
                    g2.clip(forma);
                    g2.setColor(color());
                    g2.fillRect(0, 0, 4, getHeight());
                    // Barra del tiempo que falta para cerrarse
                    g2.fillRect(4, getHeight() - 3, Math.round((getWidth() - 4) * restante), 3);
                    g2.setClip(null);
                    g2.setColor(Tema.color("App.border"));
                    g2.draw(forma);
                    g2.dispose();
                }
            };
            tarjeta.setOpaque(false);
            tarjeta.setBorder(BorderFactory.createEmptyBorder(14, 18, 16, 10));

            JLabel icono = new JLabel(new FlatSVGIcon(tipo.icono, 22, 22)
                .setColorFilter(new FlatSVGIcon.ColorFilter(c -> color())));
            icono.setVerticalAlignment(SwingConstants.TOP);
            tarjeta.add(icono, BorderLayout.WEST);

            JPanel textos = new JPanel(new BorderLayout(0, 2));
            textos.setOpaque(false);
            textos.add(UIStyles.titulo(titulo, 1f), BorderLayout.NORTH);
            JComponent texto = UIStyles.textoAjustable(mensaje);
            textos.add(texto, BorderLayout.CENTER);
            tarjeta.add(textos, BorderLayout.CENTER);

            JButton btnCerrar = new JButton(new FlatSVGIcon("icons/cerrar.svg", 14, 14)
                .setColorFilter(new FlatSVGIcon.ColorFilter(c -> Tema.color("App.muted"))));
            btnCerrar.putClientProperty("JButton.buttonType", "toolBarButton");
            btnCerrar.putClientProperty("FlatLaf.style", "arc: 999; margin: 4,4,4,4");
            btnCerrar.setFocusable(false);
            btnCerrar.setToolTipText("Cerrar");
            btnCerrar.addActionListener(e -> cerrar());
            JPanel esquina = new JPanel(new BorderLayout());
            esquina.setOpaque(false);
            esquina.add(btnCerrar, BorderLayout.NORTH);
            tarjeta.add(esquina, BorderLayout.EAST);

            setContentPane(tarjeta);
            // Alto según el texto ya acomodado al ancho fijo
            texto.setSize(ANCHO - 100, Short.MAX_VALUE);
            setSize(ANCHO, Math.max(68, tarjeta.getPreferredSize().height));

            xFinal = owner.getX() + owner.getWidth() - ANCHO - 24;
            setLocation(xFinal + 40, owner.getY() + 70);
            animacion = new Timer(15, e -> paso());
        }

        private Color color() {
            return Tema.color(tipo == Alerta.Tipo.ERROR ? "App.danger" : "App.success");
        }

        private void mostrar() {
            translucir(0f);
            setVisible(true);
            inicio = System.currentTimeMillis();
            animacion.start();
        }

        private void paso() {
            long ahora = System.currentTimeMillis();
            if (inicioCierre >= 0) {
                float p = Math.min(1f, (ahora - inicioCierre) / 180f);
                translucir(1f - p);
                setLocation(xFinal + Math.round(40 * p), getY());
                if (p >= 1f) {
                    animacion.stop();
                    dispose();
                }
                return;
            }
            float entrada = Math.min(1f, (ahora - inicio) / 220f);
            double suave = 1 - Math.pow(1 - entrada, 3);
            translucir((float) suave);
            setLocation(xFinal + (int) Math.round(40 * (1 - suave)), getY());
            restante = Math.max(0f, 1f - (ahora - inicio) / (float) DURACION_MS);
            getContentPane().repaint();
            if (restante <= 0f) {
                cerrar();
            }
        }

        private void cerrar() {
            if (inicioCierre < 0) {
                inicioCierre = System.currentTimeMillis();
                if (!animacion.isRunning()) {
                    animacion.start();
                }
            }
        }

        private void translucir(float opacidad) {
            try {
                setOpacity(Math.max(0f, Math.min(1f, opacidad)));
            } catch (UnsupportedOperationException | IllegalComponentStateException ignored) {
                // Sin transparencia en este equipo: se muestra sin desvanecido
            }
        }
    }
}

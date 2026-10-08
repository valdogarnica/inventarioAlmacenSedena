package com.almacen.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Cuadros de diálogo modernos (aviso, confirmación y captura de texto) que reemplazan
 * a JOptionPane: tarjeta redondeada con icono de color, título, mensaje y botones.
 */
public final class Alerta {
    public enum Tipo {
        INFO("icons/info.svg", "App.accent", "App.accentSoft"),
        EXITO("icons/exito.svg", "App.success", "App.successSoft"),
        AVISO("icons/aviso.svg", "App.warning", "App.warningSoft"),
        ERROR("icons/error.svg", "App.danger", "App.dangerSoft"),
        PREGUNTA("icons/pregunta.svg", "App.accent", "App.accentSoft");

        final String icono;
        final String color;
        final String fondo;

        Tipo(String icono, String color, String fondo) {
            this.icono = icono;
            this.color = color;
            this.fondo = fondo;
        }
    }

    private Alerta() {
    }

    public static void mostrar(Component parent, Object mensaje, String titulo, Tipo tipo) {
        new Dialogo(parent, titulo, mensaje, tipo, null, "Aceptar", null, false).abrir();
    }

    /** Pregunta Sí/No. Devuelve true si el usuario confirma. */
    public static boolean confirmar(Component parent, Object mensaje, String titulo) {
        return confirmar(parent, mensaje, titulo, Tipo.PREGUNTA, "Sí", "No");
    }

    public static boolean confirmar(Component parent, Object mensaje, String titulo, Tipo tipo,
                                    String textoSi, String textoNo) {
        Dialogo d = new Dialogo(parent, titulo, mensaje, tipo, null, textoSi, textoNo, tipo == Tipo.AVISO || tipo == Tipo.ERROR);
        d.abrir();
        return d.aceptado;
    }

    /** Pide un texto. Devuelve null si el usuario cancela. */
    public static String pedirTexto(Component parent, String mensaje, String titulo, String inicial) {
        JTextField campo = new JTextField(inicial != null ? inicial : "", 24);
        Dialogo d = new Dialogo(parent, titulo, mensaje, Tipo.PREGUNTA, campo, "Aceptar", "Cancelar", false);
        campo.addActionListener(e -> d.cerrar(true));
        d.abrir();
        return d.aceptado ? campo.getText() : null;
    }

    private static final class Dialogo extends JDialog {
        private boolean aceptado;

        Dialogo(Component parent, String titulo, Object mensaje, Tipo tipo, JComponent extra,
                String textoAceptar, String textoCancelar, boolean peligro) {
            super(ventana(parent), titulo != null ? titulo : "", ModalityType.APPLICATION_MODAL);
            setUndecorated(true);
            setBackground(new Color(0, 0, 0, 0));

            JPanel tarjeta = new JPanel(new BorderLayout(16, 0)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Tema.color("App.card"));
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 16, 16));
                    g2.setColor(Tema.color("App.border"));
                    g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 16, 16));
                    g2.dispose();
                }
            };
            tarjeta.setOpaque(false);
            tarjeta.setBorder(BorderFactory.createEmptyBorder(22, 22, 18, 22));

            tarjeta.add(new IconoCircular(tipo), BorderLayout.WEST);

            JPanel cuerpo = new JPanel(new BorderLayout(0, 6));
            cuerpo.setOpaque(false);
            if (titulo != null && !titulo.isEmpty()) {
                cuerpo.add(UIStyles.titulo(titulo, 3f), BorderLayout.NORTH);
            }
            JPanel centro = new JPanel(new BorderLayout(0, 10));
            centro.setOpaque(false);
            if (mensaje instanceof Component) {
                centro.add((Component) mensaje, BorderLayout.CENTER);
            } else if (mensaje != null) {
                JComponent texto = UIStyles.textoAjustable(String.valueOf(mensaje));
                ((javax.swing.JTextArea) texto).setColumns(30);
                centro.add(texto, BorderLayout.CENTER);
            }
            if (extra != null) {
                centro.add(extra, BorderLayout.SOUTH);
            }
            cuerpo.add(centro, BorderLayout.CENTER);

            JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            botones.setOpaque(false);
            botones.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
            JButton btnAceptar = new JButton(textoAceptar);
            if (peligro) {
                UIStyles.styleDangerButton(btnAceptar);
            } else {
                UIStyles.stylePrimaryButton(btnAceptar);
            }
            UIStyles.tamanoMinimo(btnAceptar, 90, 34);
            btnAceptar.addActionListener(e -> cerrar(true));
            if (textoCancelar != null) {
                JButton btnCancelar = new JButton(textoCancelar);
                UIStyles.styleSecondaryButton(btnCancelar);
                UIStyles.tamanoMinimo(btnCancelar, 90, 34);
                btnCancelar.addActionListener(e -> cerrar(false));
                botones.add(btnCancelar);
            }
            botones.add(btnAceptar);
            cuerpo.add(botones, BorderLayout.SOUTH);
            tarjeta.add(cuerpo, BorderLayout.CENTER);

            setContentPane(tarjeta);
            // Enter acepta; no se usa setDefaultButton porque FlatLaf pintaría el botón con
            // el color "default" (azul) aunque sea rojo de peligro
            getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "aceptar");
            getRootPane().getActionMap().put("aceptar", new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    cerrar(true);
                }
            });
            getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancelar");
            getRootPane().getActionMap().put("cancelar", new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    cerrar(false);
                }
            });
            pack();
            Dimension tam = getSize();
            setSize(Math.max(tam.width, 380), tam.height);
            setLocationRelativeTo(getOwner());
            JComponent foco = extra != null ? extra : btnAceptar;
            addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowOpened(java.awt.event.WindowEvent e) {
                    foco.requestFocusInWindow();
                }
            });
        }

        void abrir() {
            // Aparece con un desvanecido corto cuando el sistema lo permite
            boolean translucido = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                .isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT);
            if (translucido && opacidad(0f)) {
                long inicio = System.currentTimeMillis();
                Timer t = new Timer(15, null);
                t.addActionListener(e -> {
                    float p = Math.min(1f, (System.currentTimeMillis() - inicio) / 160f);
                    opacidad(p);
                    if (p >= 1f) {
                        t.stop();
                    }
                });
                t.start();
            }
            setVisible(true);
        }

        private boolean opacidad(float valor) {
            try {
                setOpacity(valor);
                return true;
            } catch (UnsupportedOperationException | java.awt.IllegalComponentStateException e) {
                return false;
            }
        }

        void cerrar(boolean aceptar) {
            aceptado = aceptar;
            dispose();
        }
    }

    /** Icono del tipo de alerta dentro de un círculo de color suave. */
    private static final class IconoCircular extends JLabel {
        private final Tipo tipo;

        IconoCircular(Tipo tipo) {
            this.tipo = tipo;
            setIcon(new FlatSVGIcon(tipo.icono, 24, 24)
                .setColorFilter(new FlatSVGIcon.ColorFilter(c -> Tema.color(tipo.color))));
            setHorizontalAlignment(CENTER);
            setVerticalAlignment(CENTER);
            setPreferredSize(new Dimension(46, 46));
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Tema.color(tipo.fondo));
            g2.fillOval(0, 0, 46, 46);
            g2.dispose();
            Graphics g3 = g.create(0, 0, 46, 46);
            getIcon().paintIcon(this, g3, 11, 11);
            g3.dispose();
        }
    }

    private static Window ventana(Component parent) {
        if (parent instanceof Window) {
            return (Window) parent;
        }
        return parent != null ? SwingUtilities.getWindowAncestor(parent) : null;
    }
}

package com.almacen.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public final class Notificaciones {
    private Notificaciones() {
    }

    public static void showMessageDialog(Component parent, Object message) {
        JOptionPane.showMessageDialog(parent, message);
    }

    public static void showMessageDialog(Component parent, Object message, String title, int messageType) {
        if (messageType == JOptionPane.ERROR_MESSAGE) {
            error(parent, String.valueOf(message));
            return;
        }
        if (messageType == JOptionPane.INFORMATION_MESSAGE && esExito(title)) {
            exito(parent, String.valueOf(message));
            return;
        }
        JOptionPane.showMessageDialog(parent, message, title, messageType);
    }

    public static void showMessageDialog(Component parent, Object message, String title, int messageType, Icon icon) {
        if (messageType == JOptionPane.ERROR_MESSAGE) {
            error(parent, String.valueOf(message));
            return;
        }
        if (messageType == JOptionPane.INFORMATION_MESSAGE && esExito(title)) {
            exito(parent, String.valueOf(message));
            return;
        }
        JOptionPane.showMessageDialog(parent, message, title, messageType, icon);
    }

    public static void exito(Component parent, String mensaje) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        if (owner != null && owner.isVisible()) {
            new NotificacionBase(owner, mensaje, new Color(46, 204, 113)).showNotificacion();
        } else {
            JOptionPane.showMessageDialog(parent, mensaje, "Éxito", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public static void error(Component parent, String mensaje) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        if (owner != null && owner.isVisible()) {
            new NotificacionBase(owner, mensaje, new Color(231, 76, 60)).showNotificacion();
        } else {
            JOptionPane.showMessageDialog(parent, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static boolean esExito(String title) {
        if (title == null) {
            return false;
        }
        String t = title.trim().toLowerCase();
        return t.contains("éxito") || t.contains("exito");
    }

    private static final class NotificacionBase extends JWindow {
        private final Timer fadeTimer;
        private final Timer slideTimer;
        private final Timer autoCloseTimer;
        private boolean fadingIn = true;
        private int targetY;

        private NotificacionBase(Window owner, String mensaje, Color backColor) {
            super(owner);
            setBackground(new Color(0, 0, 0, 0));
            setAlwaysOnTop(true);
            setFocusableWindowState(true);
            setOpacity(0f);
            setSize(900, 60);

            int x = owner.getX() + (owner.getWidth() - getWidth()) / 2;
            int yStart = owner.getY() - getHeight();
            targetY = owner.getY() + 20;
            setLocation(x, yStart);

            JPanel content = new JPanel(new BorderLayout());
            content.setBackground(backColor);
            content.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

            JLabel lbl = new JLabel(mensaje);
            lbl.setForeground(Color.WHITE);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lbl.setHorizontalAlignment(SwingConstants.LEFT);
            content.add(lbl, BorderLayout.CENTER);

            JButton btnCerrar = new JButton("X");
            btnCerrar.setForeground(Color.WHITE);
            btnCerrar.setOpaque(false);
            btnCerrar.setContentAreaFilled(false);
            btnCerrar.setBorderPainted(false);
            btnCerrar.setFocusPainted(false);
            btnCerrar.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btnCerrar.setPreferredSize(new Dimension(40, 40));
            btnCerrar.addActionListener(e -> closeNotification());
            content.add(btnCerrar, BorderLayout.EAST);

            setContentPane(content);
            setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));

            fadeTimer = new Timer(20, e -> fadeTick());
            slideTimer = new Timer(10, e -> slideTick());
            autoCloseTimer = new Timer(6000, e -> closeNotification());
        }

        private void showNotificacion() {
            setVisible(true);
            slideTimer.start();
            fadeTimer.start();
            autoCloseTimer.start();
        }

        private void fadeTick() {
            if (fadingIn) {
                if (getOpacity() < 1f) {
                    setOpacity(Math.min(1f, getOpacity() + 0.05f));
                } else {
                    fadeTimer.stop();
                }
            } else {
                if (getOpacity() > 0f) {
                    setOpacity(Math.max(0f, getOpacity() - 0.05f));
                } else {
                    fadeTimer.stop();
                    dispose();
                }
            }
        }

        private void slideTick() {
            if (getY() < targetY) {
                setLocation(getX(), Math.min(targetY, getY() + 5));
            } else {
                slideTimer.stop();
            }
        }

        private void closeNotification() {
            fadingIn = false;
            fadeTimer.start();
        }
    }
}

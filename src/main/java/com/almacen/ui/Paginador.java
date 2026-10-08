package com.almacen.ui;

import javax.swing.*;
import java.awt.*;
import java.util.function.IntConsumer;

/**
 * Barra de paginación reutilizable: registros por página, anterior/siguiente y contador.
 * El dueño implementa la carga con {@code cargador.accept(pagina)} y luego llama a {@link #actualizar(int)}.
 */
public class Paginador extends JPanel {
    private final JComboBox<Integer> comboTamano;
    private final JButton btnAnterior = new JButton("Anterior");
    private final JButton btnSiguiente = new JButton("Siguiente");
    private final JLabel lblPagina = new JLabel("Página 1 de 1");
    private int paginaActual = 1;

    public Paginador(Integer[] tamanos, int tamanoInicial, IntConsumer cargador) {
        super(new WrapLayout(FlowLayout.RIGHT, 8, 4));
        setOpaque(false);
        JLabel lblTamano = new JLabel("Registros por página:");
        lblTamano.setForeground(UIStyles.TEXT);
        add(lblTamano);
        comboTamano = new JComboBox<>(tamanos);
        comboTamano.setSelectedItem(tamanoInicial);
        comboTamano.addActionListener(e -> cargador.accept(1));
        add(comboTamano);
        btnAnterior.addActionListener(e -> cargador.accept(paginaActual - 1));
        UIStyles.styleSecondaryButton(btnAnterior);
        UIStyles.applySvgIcon(btnAnterior, "/icons/menorque.svg", 16);
        add(btnAnterior);
        btnSiguiente.addActionListener(e -> cargador.accept(paginaActual + 1));
        UIStyles.styleSecondaryButton(btnSiguiente);
        UIStyles.applySvgIcon(btnSiguiente, "/icons/mayorque.svg", 16);
        add(btnSiguiente);
        lblPagina.setForeground(UIStyles.TEXT);
        add(lblPagina);
    }

    public int getTamano() {
        return (Integer) comboTamano.getSelectedItem();
    }

    public int getPaginaActual() {
        return paginaActual;
    }

    /**
     * Ajusta la página pedida al rango válido según el total y actualiza los controles.
     * Devuelve el offset que se debe usar en la consulta.
     */
    public int calcularOffset(int paginaPedida, int totalRegistros) {
        int totalPaginas = Math.max(1, (int) Math.ceil(totalRegistros / (double) getTamano()));
        paginaActual = Math.max(1, Math.min(paginaPedida, totalPaginas));
        lblPagina.setText("Página " + paginaActual + " de " + totalPaginas + " (Total: " + totalRegistros + ")");
        btnAnterior.setEnabled(paginaActual > 1);
        btnSiguiente.setEnabled(paginaActual < totalPaginas);
        return (paginaActual - 1) * getTamano();
    }
}

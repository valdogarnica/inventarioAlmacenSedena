package com.almacen.ui;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** DocumentListener que ejecuta la misma acción ante cualquier cambio del texto. */
public class SimpleDocumentListener implements DocumentListener {
    private final Runnable accion;

    public SimpleDocumentListener(Runnable accion) {
        this.accion = accion;
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        accion.run();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        accion.run();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        accion.run();
    }
}

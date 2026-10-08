package com.almacen.ui;

import javax.swing.JFormattedTextField;
import javax.swing.JPasswordField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.event.ContainerEvent;
import java.util.Locale;

/**
 * Escritura en mayúsculas en todos los campos de texto, si el usuario lo activó en
 * Configuración. Se instala una vez al iniciar y alcanza a cada campo que se agrega
 * a una ventana (también los editores de tablas y combos).
 */
public final class Mayusculas {
    /** Client property para que un campo conserve lo que se escribe (por ejemplo, una ruta). */
    public static final String EXCLUIR = "Mayusculas.excluir";
    private static final String INSTALADO = "Mayusculas.instalado";
    private static final String OYENTE = "Mayusculas.oyente";
    private static final Locale ES = new Locale("es", "MX");

    private Mayusculas() {
    }

    public static void instalar() {
        Toolkit.getDefaultToolkit().addAWTEventListener(e -> {
            if (e.getID() == ContainerEvent.COMPONENT_ADDED) {
                aplicarArbol(((ContainerEvent) e).getChild());
            }
        }, AWTEvent.CONTAINER_EVENT_MASK);
    }

    private static void aplicarArbol(Component c) {
        if (c instanceof JTextComponent) {
            aplicar((JTextComponent) c);
        }
        if (c instanceof Container) {
            for (Component hijo : ((Container) c).getComponents()) {
                aplicarArbol(hijo);
            }
        }
    }

    private static void aplicar(JTextComponent campo) {
        // Contraseñas y campos con formato (números, fechas) tienen su propio filtro
        if (campo instanceof JPasswordField || campo instanceof JFormattedTextField
                || campo.getClientProperty(INSTALADO) == campo.getDocument()) {
            return;
        }
        Document doc = campo.getDocument();
        if (!(doc instanceof AbstractDocument)) {
            return;
        }
        AbstractDocument ad = (AbstractDocument) doc;
        ad.setDocumentFilter(new Filtro(campo, ad.getDocumentFilter()));
        campo.putClientProperty(INSTALADO, doc);
        if (campo.getClientProperty(OYENTE) == null) {
            campo.putClientProperty(OYENTE, Boolean.TRUE);
            campo.addPropertyChangeListener("document", e -> aplicar(campo));
        }
    }

    private static final class Filtro extends DocumentFilter {
        private final JTextComponent campo;
        private final DocumentFilter anterior;

        Filtro(JTextComponent campo, DocumentFilter anterior) {
            this.campo = campo;
            this.anterior = anterior;
        }

        private String convertir(String texto) {
            if (texto == null || !AppPreferences.isMayusculas() || !campo.isEditable()
                    || campo.getClientProperty(EXCLUIR) != null) {
                return texto;
            }
            return texto.toUpperCase(ES);
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String texto, AttributeSet attr)
                throws BadLocationException {
            if (anterior != null) {
                anterior.insertString(fb, offset, convertir(texto), attr);
            } else {
                super.insertString(fb, offset, convertir(texto), attr);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int largo, String texto, AttributeSet attrs)
                throws BadLocationException {
            if (anterior != null) {
                anterior.replace(fb, offset, largo, convertir(texto), attrs);
            } else {
                super.replace(fb, offset, largo, convertir(texto), attrs);
            }
        }

        @Override
        public void remove(FilterBypass fb, int offset, int largo) throws BadLocationException {
            if (anterior != null) {
                anterior.remove(fb, offset, largo);
            } else {
                super.remove(fb, offset, largo);
            }
        }
    }
}

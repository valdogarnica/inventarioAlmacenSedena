package com.almacen.ui;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * Agrega búsqueda a un {@link JComboBox}: al abrir la lista aparece un campo "Buscar…"
 * y lo que se escribe filtra las opciones (sin importar mayúsculas ni acentos; varias
 * palabras deben aparecer todas). Las flechas y Enter eligen entre los resultados.
 * <p>
 * En un combo editable el propio texto del combo sirve de búsqueda.
 * <p>
 * El combo sigue usándose igual ({@code addItem}, {@code removeAllItems},
 * {@code getSelectedItem}...): el filtro solo existe mientras la lista está abierta.
 */
public final class ComboBuscable {
    private static final String CLAVE = "ComboBuscable.instalado";

    private ComboBuscable() {
    }

    public static <E> void instalar(JComboBox<E> combo) {
        if (combo.getClientProperty(CLAVE) != null) {
            return;
        }
        combo.putClientProperty(CLAVE, Boolean.TRUE);

        ModeloFiltrable<E> modelo = new ModeloFiltrable<>();
        ComboBoxModel<E> anterior = combo.getModel();
        for (int i = 0; i < anterior.getSize(); i++) {
            modelo.addElement(anterior.getElementAt(i));
        }
        modelo.setSelectedItem(anterior.getSelectedItem());
        combo.setModel(modelo);
        combo.setMaximumRowCount(12);

        if (combo.isEditable()) {
            instalarEditable(combo, modelo);
        } else {
            instalarConCampo(combo, modelo);
        }
    }

    // ---------------------------------------------------------------- combo no editable

    private static <E> void instalarConCampo(JComboBox<E> combo, ModeloFiltrable<E> modelo) {
        JTextField campo = new JTextField(1);
        campo.setFocusable(false);
        campo.putClientProperty("JTextField.placeholderText", "Escriba para buscar…");
        try {
            campo.putClientProperty("JTextField.leadingIcon",
                new com.formdev.flatlaf.extras.FlatSVGIcon("icons/search.svg", 14, 14));
        } catch (Exception ignored) {
            // Sin icono si no se encuentra
        }
        JLabel conteo = new JLabel();
        conteo.setForeground(Tema.color("App.muted"));
        conteo.setFont(conteo.getFont().deriveFont(11f));

        JPanel barra = new JPanel(new BorderLayout(6, 0));
        barra.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 6));
        barra.add(campo, BorderLayout.CENTER);
        barra.add(conteo, BorderLayout.EAST);

        Runnable aplicar = () -> {
            modelo.filtrar(campo.getText());
            conteo.setText(modelo.getSize() == 0 && !campo.getText().isEmpty()
                ? "Sin resultados" : modelo.getSize() + "/" + modelo.getTotal());
            conteo.setForeground(Tema.color(modelo.getSize() == 0 ? "App.dangerText" : "App.muted"));
            JList<?> lista = lista(combo);
            if (lista != null && modelo.getSize() > 0) {
                int i = modelo.indiceDe(combo.getSelectedItem());
                lista.setSelectedIndex(campo.getText().isEmpty() && i >= 0 ? i : 0);
                lista.ensureIndexIsVisible(lista.getSelectedIndex());
            }
        };

        insertarBarra(combo, barra);
        combo.addPropertyChangeListener("UI", e -> SwingUtilities.invokeLater(() -> {
            // La barra quedó fuera del árbol al cambiar de tema: se actualiza con el tema nuevo
            SwingUtilities.updateComponentTreeUI(barra);
            insertarBarra(combo, barra);
        }));

        // Escribir no salta a la opción que empieza con la letra: alimenta la búsqueda
        combo.setKeySelectionManager((key, model) -> -1);
        // La barra espaciadora escribe un espacio en lugar de abrir/cerrar la lista
        combo.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(KeyStroke.getKeyStroke("SPACE"), "none");
        combo.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(KeyStroke.getKeyStroke("released SPACE"), "none");
        combo.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("SPACE"), "none");
        combo.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("released SPACE"), "none");

        combo.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (c < 32 || c == 127 || e.isControlDown() || e.isAltDown() || e.isMetaDown()) {
                    return;
                }
                if (!combo.isPopupVisible() && combo.isShowing()) {
                    combo.showPopup();
                }
                campo.setText(campo.getText() + c);
                aplicar.run();
                e.consume();
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE && !campo.getText().isEmpty()) {
                    String t = campo.getText();
                    campo.setText(e.isControlDown() ? "" : t.substring(0, t.length() - 1));
                    aplicar.run();
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE && !campo.getText().isEmpty()) {
                    campo.setText("");
                    aplicar.run();
                    e.consume();
                }
            }
        });

        combo.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                campo.setText("");
                modelo.filtrar("");
                conteo.setText(modelo.getTotal() + " opciones");
                conteo.setForeground(Tema.color("App.muted"));
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                limpiar();
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                limpiar();
            }

            private void limpiar() {
                if (!campo.getText().isEmpty()) {
                    campo.setText("");
                    modelo.filtrar("");
                }
            }
        });
    }

    private static void insertarBarra(JComboBox<?> combo, JComponent barra) {
        Object hijo = combo.getUI().getAccessibleChild(combo, 0);
        if (!(hijo instanceof JPopupMenu)) {
            return;
        }
        JPopupMenu popup = (JPopupMenu) hijo;
        if (barra.getParent() == popup) {
            return;
        }
        // BasicComboPopup usa BoxLayout vertical: la barra no debe crecer en alto
        barra.setMaximumSize(new Dimension(Integer.MAX_VALUE, barra.getPreferredSize().height));
        barra.setAlignmentX(Component.CENTER_ALIGNMENT);
        popup.insert(barra, 0);
    }

    private static JList<?> lista(JComboBox<?> combo) {
        Object hijo = combo.getUI().getAccessibleChild(combo, 0);
        return hijo instanceof ComboPopup ? ((ComboPopup) hijo).getList() : null;
    }

    // ---------------------------------------------------------------- combo editable

    private static <E> void instalarEditable(JComboBox<E> combo, ModeloFiltrable<E> modelo) {
        conectarEditor(combo, modelo);
        // Al cambiar de tema (claro/oscuro) Swing crea un editor nuevo: se vuelve a conectar
        combo.addPropertyChangeListener("editor", e -> conectarEditor(combo, modelo));
        combo.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                Component editor = combo.getEditor() != null ? combo.getEditor().getEditorComponent() : null;
                if (editor instanceof JTextField) {
                    SwingUtilities.invokeLater(() -> filtrarConservandoTexto(combo, modelo, (JTextField) editor, ""));
                }
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
    }

    private static <E> void conectarEditor(JComboBox<E> combo, ModeloFiltrable<E> modelo) {
        Component editor = combo.getEditor() != null ? combo.getEditor().getEditorComponent() : null;
        if (!(editor instanceof JTextField) || ((JTextField) editor).getClientProperty(CLAVE) != null) {
            return;
        }
        JTextField texto = (JTextField) editor;
        texto.putClientProperty(CLAVE, Boolean.TRUE);
        texto.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP: case KeyEvent.VK_DOWN: case KeyEvent.VK_ENTER:
                    case KeyEvent.VK_ESCAPE: case KeyEvent.VK_TAB: case KeyEvent.VK_LEFT:
                    case KeyEvent.VK_RIGHT: case KeyEvent.VK_HOME: case KeyEvent.VK_END:
                    case KeyEvent.VK_SHIFT: case KeyEvent.VK_CONTROL: case KeyEvent.VK_ALT:
                    case KeyEvent.VK_PAGE_UP: case KeyEvent.VK_PAGE_DOWN:
                        return;
                    default:
                }
                if (!texto.isShowing()) {
                    return;
                }
                filtrarConservandoTexto(combo, modelo, texto, texto.getText());
                if (modelo.getSize() > 0) {
                    if (!combo.isPopupVisible()) {
                        combo.showPopup();
                    }
                    JList<?> lista = lista(combo);
                    if (lista != null) {
                        lista.clearSelection();
                        lista.ensureIndexIsVisible(0);
                    }
                } else {
                    combo.hidePopup();
                }
            }
        });
    }

    /** Al cambiar el contenido, Swing reescribe el editor con la selección: se restaura lo tecleado. */
    private static void filtrarConservandoTexto(JComboBox<?> combo, ModeloFiltrable<?> modelo,
                                                JTextField texto, String filtro) {
        if (!modelo.estaFiltrado() && filtro.trim().isEmpty()) {
            return;
        }
        String actual = texto.getText();
        int caret = texto.getCaretPosition();
        modelo.filtrar(filtro);
        if (!actual.equals(texto.getText())) {
            texto.setText(actual);
            texto.setCaretPosition(Math.min(caret, actual.length()));
        }
    }

    // ---------------------------------------------------------------- utilidades

    static String normalizar(String s) {
        if (s == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return sinAcentos.toLowerCase().trim();
    }

    /**
     * Modelo con todas las opciones y una vista filtrada. La selección es independiente
     * del filtro, así que filtrar nunca cambia el elemento elegido. Extiende
     * DefaultComboBoxModel para que {@link JComboBox#removeAllItems()} vacíe todo de una vez.
     */
    static final class ModeloFiltrable<E> extends DefaultComboBoxModel<E> {
        private final List<E> todos = new ArrayList<>();
        private final List<String> claves = new ArrayList<>();
        private List<E> visibles = todos;
        private String[] palabras = new String[0];
        private Object seleccion;

        int getTotal() {
            return todos.size();
        }

        boolean estaFiltrado() {
            return visibles != todos;
        }

        int indiceDe(Object o) {
            return visibles.indexOf(o);
        }

        void filtrar(String texto) {
            String n = normalizar(texto);
            palabras = n.isEmpty() ? new String[0] : n.split("\\s+");
            if (palabras.length == 0 && !estaFiltrado()) {
                return;
            }
            recalcular();
            // -1/-1: cambia la lista sin que el combo recalcule su tamaño ni su selección
            fireContentsChanged(this, -1, -1);
        }

        private void recalcular() {
            if (palabras.length == 0) {
                visibles = todos;
                return;
            }
            List<E> nuevos = new ArrayList<>();
            for (int i = 0; i < todos.size(); i++) {
                if (coincide(claves.get(i))) {
                    nuevos.add(todos.get(i));
                }
            }
            visibles = nuevos;
        }

        private boolean coincide(String clave) {
            for (String p : palabras) {
                if (!clave.contains(p)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int getSize() {
            return visibles.size();
        }

        @Override
        public E getElementAt(int index) {
            return index >= 0 && index < visibles.size() ? visibles.get(index) : null;
        }

        @Override
        public int getIndexOf(Object item) {
            return visibles.indexOf(item);
        }

        @Override
        public void setSelectedItem(Object item) {
            if ((seleccion != null && !seleccion.equals(item)) || (seleccion == null && item != null)) {
                seleccion = item;
                fireContentsChanged(this, -1, -1);
            }
        }

        @Override
        public Object getSelectedItem() {
            return seleccion;
        }

        @Override
        public void addElement(E item) {
            insertar(todos.size(), item);
            if (todos.size() == 1 && seleccion == null && item != null) {
                setSelectedItem(item);
            }
        }

        @Override
        public void insertElementAt(E item, int index) {
            insertar(index, item);
        }

        private void insertar(int index, E item) {
            todos.add(index, item);
            claves.add(index, normalizar(String.valueOf(item)));
            if (estaFiltrado()) {
                recalcular();
                fireContentsChanged(this, -1, -1);
            } else {
                fireIntervalAdded(this, index, index);
            }
        }

        @Override
        public void addAll(java.util.Collection<? extends E> c) {
            for (E e : c) {
                addElement(e);
            }
        }

        @Override
        public void addAll(int index, java.util.Collection<? extends E> c) {
            int i = index;
            for (E e : c) {
                insertar(i++, e);
            }
        }

        @Override
        public void removeElement(Object item) {
            int real = todos.indexOf(item);
            if (real >= 0) {
                quitar(real);
            }
        }

        @Override
        public void removeElementAt(int index) {
            // El índice se refiere a lo visible, como en cualquier ListModel
            int real = todos.indexOf(getElementAt(index));
            if (real >= 0) {
                quitar(real);
            }
        }

        private void quitar(int real) {
            E item = todos.get(real);
            if (item != null && item.equals(seleccion)) {
                E otro = real > 0 ? todos.get(real - 1) : (todos.size() > 1 ? todos.get(1) : null);
                setSelectedItem(otro);
            }
            todos.remove(real);
            claves.remove(real);
            if (estaFiltrado()) {
                recalcular();
                fireContentsChanged(this, -1, -1);
            } else {
                fireIntervalRemoved(this, real, real);
            }
        }

        @Override
        public void removeAllElements() {
            int n = visibles.size();
            todos.clear();
            claves.clear();
            visibles = todos;
            palabras = new String[0];
            seleccion = null;
            if (n > 0) {
                fireIntervalRemoved(this, 0, n - 1);
            }
        }
    }
}

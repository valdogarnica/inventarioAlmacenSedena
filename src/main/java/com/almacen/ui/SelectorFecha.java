package com.almacen.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.Date;
import java.util.Locale;

/**
 * Campo de fecha con calendario desplegable, con los colores del tema.
 * Se puede escribir la fecha o elegirla en el calendario; avisa cada cambio con
 * la propiedad "date" (igual que el selector anterior).
 */
public class SelectorFecha extends JPanel {
    private static final Locale ES = new Locale("es", "MX");

    private final JTextField campo = new JTextField(9);
    private final JButton btnCalendario = new JButton();
    private final JPopupMenu popup = new JPopupMenu();
    private final JPanel dias = new JPanel(new GridLayout(6, 7, 2, 2));
    private final JLabel lblMes = new JLabel("", SwingConstants.CENTER);
    private SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy");
    private Date fecha;
    private YearMonth mesVisible = YearMonth.now();
    private boolean permiteVacia = true;

    public SelectorFecha() {
        super(new BorderLayout());
        setOpaque(false);
        formato.setLenient(false);

        campo.putClientProperty("JTextField.placeholderText", "dd/mm/aaaa");
        btnCalendario.setIcon(new FlatSVGIcon("icons/calendario.svg", 16, 16)
            .setColorFilter(new FlatSVGIcon.ColorFilter(c -> Tema.color(isEnabled() ? "App.muted" : "App.borderMuted"))));
        btnCalendario.setToolTipText("Elegir en el calendario");
        btnCalendario.setFocusable(false);
        btnCalendario.putClientProperty("JButton.buttonType", "toolBarButton");
        btnCalendario.putClientProperty("FlatLaf.style", "margin: 2,4,2,4");
        btnCalendario.addActionListener(e -> abrirCalendario());
        campo.putClientProperty("JTextField.trailingComponent", btnCalendario);
        campo.addActionListener(e -> leerTexto());
        campo.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                leerTexto();
            }
        });
        add(campo, BorderLayout.CENTER);

        construirCalendario();
    }

    /** Patrón de la fecha escrita, por ejemplo "dd/MM/yyyy" o "yyyy-MM-dd". */
    public void setDateFormatString(String patron) {
        formato = new SimpleDateFormat(patron);
        formato.setLenient(false);
        campo.putClientProperty("JTextField.placeholderText",
            patron.replace("dd", "dd").replace("MM", "mm").replace("yyyy", "aaaa"));
        mostrarTexto();
    }

    /** Si es false, el calendario no muestra "Limpiar" y el campo no puede quedar vacío. */
    public void setPermiteVacia(boolean permiteVacia) {
        this.permiteVacia = permiteVacia;
        construirCalendario();
    }

    public Date getDate() {
        return fecha;
    }

    public void setDate(Date nueva) {
        Date anterior = fecha;
        fecha = nueva;
        mostrarTexto();
        if (anterior == null ? nueva != null : !anterior.equals(nueva)) {
            firePropertyChange("date", anterior, nueva);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        campo.setEnabled(enabled);
        btnCalendario.setEnabled(enabled);
        if (!enabled) {
            popup.setVisible(false);
        }
    }

    private void mostrarTexto() {
        campo.setText(fecha != null ? formato.format(fecha) : "");
    }

    private void leerTexto() {
        String texto = campo.getText().trim();
        if (texto.isEmpty()) {
            if (permiteVacia) {
                setDate(null);
            } else {
                mostrarTexto();
            }
            return;
        }
        try {
            setDate(formato.parse(texto));
        } catch (ParseException e) {
            // Fecha mal escrita: se regresa a la anterior
            mostrarTexto();
        }
    }

    private void abrirCalendario() {
        leerTexto();
        LocalDate base = fecha != null ? aLocal(fecha) : LocalDate.now();
        mesVisible = YearMonth.from(base);
        // El calendario no está en la ventana mientras está cerrado: toma el tema actual al abrir
        SwingUtilities.updateComponentTreeUI(popup);
        pintarDias();
        popup.show(campo, 0, campo.getHeight() + 2);
    }

    private void construirCalendario() {
        popup.removeAll();
        JPanel contenido = new JPanel(new BorderLayout(0, 8));
        contenido.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contenido.putClientProperty("FlatLaf.style", "background: $App.card");

        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setOpaque(false);
        JButton anterior = botonFlecha("icons/izquierda.svg", "Mes anterior");
        anterior.addActionListener(e -> {
            mesVisible = mesVisible.minusMonths(1);
            pintarDias();
        });
        JButton siguiente = botonFlecha("icons/derecha.svg", "Mes siguiente");
        siguiente.addActionListener(e -> {
            mesVisible = mesVisible.plusMonths(1);
            pintarDias();
        });
        lblMes.setFont(lblMes.getFont().deriveFont(java.awt.Font.BOLD, lblMes.getFont().getSize2D() + 1f));
        cabecera.add(anterior, BorderLayout.WEST);
        cabecera.add(lblMes, BorderLayout.CENTER);
        cabecera.add(siguiente, BorderLayout.EAST);

        JPanel semana = new JPanel(new GridLayout(1, 7, 2, 2));
        semana.setOpaque(false);
        for (int i = 0; i < 7; i++) {
            String nombre = DayOfWeek.MONDAY.plus(i).getDisplayName(TextStyle.SHORT, ES);
            JLabel lbl = new JLabel(capitalizar(nombre.replace(".", "")).substring(0, 2), SwingConstants.CENTER);
            UIStyles.textoSecundario(lbl);
            lbl.setFont(lbl.getFont().deriveFont(java.awt.Font.BOLD, lbl.getFont().getSize2D() - 1f));
            semana.add(lbl);
        }
        dias.setOpaque(false);
        JPanel cuerpo = new JPanel(new BorderLayout(0, 4));
        cuerpo.setOpaque(false);
        cuerpo.add(semana, BorderLayout.NORTH);
        cuerpo.add(dias, BorderLayout.CENTER);

        JPanel pie = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        pie.setOpaque(false);
        if (permiteVacia) {
            JButton limpiar = new JButton("Limpiar");
            UIStyles.styleSecondaryButton(limpiar);
            limpiar.addActionListener(e -> {
                popup.setVisible(false);
                setDate(null);
            });
            pie.add(limpiar);
        }
        JButton hoy = new JButton("Hoy");
        UIStyles.stylePrimaryButton(hoy);
        hoy.addActionListener(e -> elegir(LocalDate.now()));
        pie.add(hoy);

        contenido.add(cabecera, BorderLayout.NORTH);
        contenido.add(cuerpo, BorderLayout.CENTER);
        contenido.add(pie, BorderLayout.SOUTH);
        popup.add(contenido);
    }

    private JButton botonFlecha(String icono, String ayuda) {
        JButton b = new JButton(new FlatSVGIcon(icono, 16, 16)
            .setColorFilter(new FlatSVGIcon.ColorFilter(c -> Tema.color("App.text"))));
        b.setToolTipText(ayuda);
        b.setFocusable(false);
        b.putClientProperty("JButton.buttonType", "toolBarButton");
        b.putClientProperty("FlatLaf.style", "arc: 999; margin: 5,5,5,5");
        return b;
    }

    private void pintarDias() {
        String mes = mesVisible.getMonth().getDisplayName(TextStyle.FULL, ES);
        lblMes.setText(capitalizar(mes) + " " + mesVisible.getYear());
        dias.removeAll();
        LocalDate primero = mesVisible.atDay(1);
        LocalDate inicio = primero.minusDays(primero.getDayOfWeek().getValue() - 1);
        LocalDate hoy = LocalDate.now();
        LocalDate elegida = fecha != null ? aLocal(fecha) : null;
        for (int i = 0; i < 42; i++) {
            LocalDate dia = inicio.plusDays(i);
            JButton b = new JButton(String.valueOf(dia.getDayOfMonth()));
            b.setFocusable(false);
            b.setPreferredSize(new Dimension(36, 32));
            boolean delMes = YearMonth.from(dia).equals(mesVisible);
            String estilo;
            if (dia.equals(elegida)) {
                estilo = "background: $App.accent; foreground: #FFFFFF; hoverBackground: $App.accentHover; "
                    + "focusedBackground: $App.accent; borderWidth: 0; font: bold";
            } else if (dia.equals(hoy)) {
                estilo = "background: $App.card; foreground: $App.accentSoftText; hoverBackground: $App.accentSoft; "
                    + "focusedBackground: $App.card; borderColor: $App.accent; borderWidth: 1; font: bold";
            } else {
                estilo = "background: $App.card; foreground: " + (delMes ? "$App.text" : "$App.muted") + "; "
                    + "hoverBackground: $App.secondaryHover; focusedBackground: $App.card; borderWidth: 0";
            }
            b.putClientProperty("FlatLaf.style", estilo + "; arc: 8; margin: 2,2,2,2; focusWidth: 0");
            b.addActionListener(e -> elegir(dia));
            dias.add(b);
        }
        dias.revalidate();
        dias.repaint();
        popup.pack();
    }

    private void elegir(LocalDate dia) {
        popup.setVisible(false);
        setDate(Date.from(dia.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        SwingUtilities.invokeLater(campo::requestFocusInWindow);
    }

    private static LocalDate aLocal(Date d) {
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static String capitalizar(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}

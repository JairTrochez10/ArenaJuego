package Menu;

import Player.GameSettings;
import Player.GameFrameNiveles;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Menu de seleccion de personaje con preview + nombre abajo.
 */
public class MenuPersonajesPanel extends JPanel {

    private static final String FONDO1 = "/Imagenes/menuperso.gif";
    private static final String FONDO2 = "/Menu/imagen/Jefe.gif";
    private static final String FONDO3 = "/Menu/imagen/Enemigos/Calavoso2.png";

    private static final String HEROE1_IMG = "/Menu/imagen/Heroes/Kevin walk.gif"; // Zorritas
    private static final String HEROE2_IMG = "/Menu/imagen/Heroes/Squeletron.gif"; // Larry

    private final MenuPrincipal frame;
    private final GameSettings settings;

    private String elegido = null;
    private String elegidoRuta = null;

    private final JButton btnConfirmar = MenuPrincipal.boton("Confirmar");
    private final JButton btnVolver    = MenuPrincipal.boton("Volver");

    private final JLabel previewImg = new JLabel();
    private final JLabel previewNombre = new JLabel(" ", SwingConstants.CENTER);
    private final JTextField txtNombre = new JTextField();
    private final JLabel lblError = new JLabel(" ", SwingConstants.LEFT);

    public MenuPersonajesPanel(MenuPrincipal frame, GameSettings settings) {
        this.frame = frame;
        this.settings = settings;

        setLayout(new BorderLayout());

        JPanel fondo = new MenuPrincipal.PanelFondo(MenuPrincipal.firstExisting(FONDO1, FONDO2, FONDO3));
        fondo.setLayout(new BorderLayout());
        add(fondo, BorderLayout.CENTER);

        JLabel titulo = new JLabel("Elige tu Personaje (" + settings.dificultadName + ")", SwingConstants.CENTER);
        titulo.setFont(new Font("Consolas", Font.BOLD, 32));
        titulo.setForeground(Color.WHITE);
        JPanel header = new JPanel(new BorderLayout()) { @Override public boolean isOpaque(){ return false; } };
        header.setBorder(BorderFactory.createEmptyBorder(16,16,8,16));
        header.add(titulo, BorderLayout.CENTER);

        JPanel cards = new JPanel(new GridLayout(1,2,24,0)) { @Override public boolean isOpaque(){ return false; } };
        cards.setBorder(BorderFactory.createEmptyBorder(20, 60, 12, 60));

        HeroCard card1 = new HeroCard("Zorritas", HEROE1_IMG);
        HeroCard card2 = new HeroCard("Larry",    HEROE2_IMG);

        cards.add(card1);
        cards.add(card2);

        JPanel bottom = new JPanel(new BorderLayout(0,10)) { @Override public boolean isOpaque(){ return false; } };
        bottom.setBorder(BorderFactory.createEmptyBorder(8, 60, 24, 60));

        JPanel preview = new JPanel(new BorderLayout()) { @Override public boolean isOpaque(){ return false; } };
        previewImg.setHorizontalAlignment(SwingConstants.CENTER);
        previewImg.setBorder(new EmptyBorder(8,8,8,8));

        previewNombre.setForeground(Color.WHITE);
        previewNombre.setFont(new Font("Consolas", Font.BOLD, 20));
        previewNombre.setBorder(new EmptyBorder(4,0,8,0));
        previewNombre.setVisible(false);

        JPanel nombrePanel = new JPanel(new BorderLayout(8,0)) { @Override public boolean isOpaque(){ return false; } };
        JLabel lblNombre = new JLabel("Nombre del jugador:");
        lblNombre.setForeground(Color.WHITE);
        lblNombre.setFont(new Font("Consolas", Font.PLAIN, 18));
        txtNombre.setFont(new Font("Consolas", Font.PLAIN, 18));
        txtNombre.setColumns(18);
        txtNombre.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70,70,70)),
                new EmptyBorder(8,10,8,10)
        ));
        txtNombre.setBackground(new Color(35, 35, 35));
        txtNombre.setForeground(new Color(230, 230, 230));
        limitarLongitud(txtNombre, 16);
        txtNombre.getDocument().addDocumentListener(new SimpleDocListener(this::validarForm));

        nombrePanel.add(lblNombre, BorderLayout.WEST);
        nombrePanel.add(txtNombre, BorderLayout.CENTER);

        lblError.setForeground(new Color(255,120,120));
        lblError.setFont(new Font("Consolas", Font.PLAIN, 14));
        lblError.setBorder(new EmptyBorder(4,2,0,2));

        JPanel centerPreview = new JPanel();
        centerPreview.setOpaque(false);
        centerPreview.setLayout(new BoxLayout(centerPreview, BoxLayout.Y_AXIS));
        centerPreview.add(previewImg);
        centerPreview.add(previewNombre);
        centerPreview.add(nombrePanel);
        centerPreview.add(lblError);

        preview.add(centerPreview, BorderLayout.CENTER);

        JPanel footer = new JPanel() { @Override public boolean isOpaque(){ return false; } };
        footer.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        Dimension BTN = new Dimension(280, 56);
        btnConfirmar.setPreferredSize(BTN);
        btnVolver.setPreferredSize(BTN);
        btnConfirmar.setEnabled(false);

        footer.add(btnVolver);
        footer.add(Box.createHorizontalStrut(20));
        footer.add(btnConfirmar);

        bottom.add(preview, BorderLayout.CENTER);
        bottom.add(footer,  BorderLayout.SOUTH);

        fondo.add(header, BorderLayout.NORTH);
        fondo.add(cards,  BorderLayout.CENTER);
        fondo.add(bottom, BorderLayout.SOUTH);

        card1.addSelectionListener(() -> onElegir("Zorritas", HEROE1_IMG, card1, card2));
        card2.addSelectionListener(() -> onElegir("Larry",    HEROE2_IMG, card2, card1));

        btnVolver.addActionListener(e -> {
            frame.setContentPane(new MenuDificultadPanel(frame));
            frame.revalidate(); frame.repaint();
        });

        btnConfirmar.addActionListener(e -> onConfirmar());
    }

    private void onElegir(String nombreHeroe, String rutaImg, HeroCard selected, HeroCard other) {
        this.elegido = nombreHeroe;
        this.elegidoRuta = rutaImg;
        selected.setSelected(true);
        other.setSelected(false);

        previewImg.setIcon(cargarIcono(rutaImg, 260, 260));
        previewNombre.setText(nombreHeroe);
        previewNombre.setVisible(true);

        validarForm();
        revalidate();
        repaint();
    }

    private void onConfirmar() {
        if (!validarForm()) return;

        String nombre = txtNombre.getText().trim();
        if (nombre.isEmpty()) nombre = "Invitado";

        GameFrameNiveles ventanaJuego = new GameFrameNiveles(settings, elegido, nombre);
        ventanaJuego.setVisible(true);

        frame.dispose();
    }

    private boolean validarForm() {
        String err = null;
        boolean okSel = (elegido != null);
        String nom = txtNombre.getText() == null ? "" : txtNombre.getText().trim();

        if (!okSel) {
            err = "Selecciona un personaje.";
        } else if (nom.isEmpty()) {
            err = "Ingresa un nombre.";
        } else if (!nom.matches("[A-Za-z0-9 _\\-]{1,16}")) {
            err = "Usa letras, numeros, espacio, _ o - (max 16).";
        }

        lblError.setText(err == null ? " " : err);
        btnConfirmar.setEnabled(err == null);
        return err == null;
    }

    // ---------- Card de seleccion ----------
    static class HeroCard extends JPanel {
        private boolean selected = false;
        private final String nombre;
        private final Image img;
        private Runnable onSelect;

        HeroCard(String nombre, String rutaImg) {
            this.nombre = nombre;
            Image tmp = null;
            try {
                java.net.URL u = getClass().getResource(rutaImg);
                if (u != null) tmp = new ImageIcon(u).getImage();
            } catch (Exception ignore) {}
            this.img = tmp;

            setOpaque(false);
            setPreferredSize(new Dimension(400, 420));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(nombre);

            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (onSelect != null) onSelect.run();
                }
            });
        }

        void addSelectionListener(Runnable r) { this.onSelect = r; }
        void setSelected(boolean sel) { this.selected = sel; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int arc = 22;
            g2.setColor(new Color(0, 0, 0, selected ? 220 : 160));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);

            g2.setStroke(new BasicStroke(3f));
            g2.setColor(new Color(255, 215, 0, selected ? 230 : 160));
            g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, arc, arc);

            if (img != null) {
                int maxW = getWidth() - 40;
                int maxH = getHeight() - 120;
                int iw = img.getWidth(null);
                int ih = img.getHeight(null);
                double s = Math.min(maxW / (double)iw, maxH / (double)ih);
                int dw = (int)Math.round(iw * s);
                int dh = (int)Math.round(ih * s);
                int x = (getWidth() - dw) / 2;
                int y = (getHeight() - dh) / 2 - 10;
                g2.drawImage(img, x, y, dw, dh, null);
            }

            g2.setFont(new Font("Consolas", Font.BOLD, 24));
            String txt = nombre + (selected ? " ✓" : "");
            int tw = g2.getFontMetrics().stringWidth(txt);
            g2.setColor(Color.WHITE);
            g2.drawString(txt, (getWidth() - tw) / 2, getHeight() - 28);

            g2.dispose();
        }
    }

    // ===== Helpers =====
    private static ImageIcon cargarIcono(String ruta, int w, int h) {
        try {
            java.net.URL u = MenuPersonajesPanel.class.getResource(ruta);
            if (u != null) {
                ImageIcon raw = new ImageIcon(u);
                Image esc = raw.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
                return new ImageIcon(esc);
            }
        } catch (Exception ignore) {}
        java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        g2.setColor(new Color(60,60,60)); g2.fillRoundRect(0,0,w,h,24,24);
        g2.setColor(new Color(90,90,90)); g2.drawRoundRect(0,0,w-1,h-1,24,24);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 16f));
        String txt = "Sin imagen";
        FontMetrics fm = g2.getFontMetrics();
        int tx = (w - fm.stringWidth(txt))/2;
        int ty = (h - fm.getHeight())/2 + fm.getAscent();
        g2.setColor(new Color(180,180,180));
        g2.drawString(txt, tx, ty);
        g2.dispose();
        return new ImageIcon(bi);
    }

    private static void limitarLongitud(JTextField field, int max) {
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                String cur = fb.getDocument().getText(0, fb.getDocument().getLength());
                String next = cur.substring(0, offset) + (text == null ? "" : text) + cur.substring(offset + length);
                if (next.length() <= max) super.replace(fb, offset, length, text, attrs);
            }
            @Override public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                String cur = fb.getDocument().getText(0, fb.getDocument().getLength());
                String next = cur.substring(0, offset) + (string == null ? "" : string);
                if (next.length() <= max) super.insertString(fb, offset, string, attr);
            }
        });
    }

    private static class SimpleDocListener implements javax.swing.event.DocumentListener {
        private final Runnable r;
        SimpleDocListener(Runnable r){ this.r = r; }
        @Override public void insertUpdate(javax.swing.event.DocumentEvent e){ r.run(); }
        @Override public void removeUpdate(javax.swing.event.DocumentEvent e){ r.run(); }
        @Override public void changedUpdate(javax.swing.event.DocumentEvent e){ r.run(); }
    }
}

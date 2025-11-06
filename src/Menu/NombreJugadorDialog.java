package Menu;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class NombreJugadorDialog extends JDialog {

    private final JTextField txtNombre = new JTextField();
    private String nombreFinal = null;
    private final String heroe;

    // Mapa: id del heroe -> ruta de imagen para preview (ajusta estas rutas a las tuyas)
    private static final Map<String, String> HERO_IMG = new HashMap<>();
    static {
        HERO_IMG.put("Paola", "/Imagenes/Heroes/Paola.png");
        HERO_IMG.put("Oscar", "/Imagenes/Heroes/Oscar.png");
        HERO_IMG.put("Larry", "/Imagenes/Heroes/Larry.png");
        HERO_IMG.put("_default", "/Imagenes/Heroes/Default.png");
    }

    public NombreJugadorDialog(Window owner, String heroe) {
        super(owner, "Jugador", ModalityType.APPLICATION_MODAL);
        this.heroe = (heroe == null ? "" : heroe);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(new EmptyBorder(16, 16, 16, 16));
        root.setBackground(new Color(25, 25, 25));

        JLabel titulo = new JLabel("Confirma tu personaje e ingresa tu nombre");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 20f));
        titulo.setForeground(Color.WHITE);
        root.add(titulo, BorderLayout.NORTH);

        JPanel centro = new JPanel();
        centro.setOpaque(false);
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));

        // Preview del heroe
        JLabel imgHeroe = new JLabel();
        imgHeroe.setAlignmentX(Component.CENTER_ALIGNMENT);
        ImageIcon icon = cargarIcono(resolveHeroImg(this.heroe), 240, 240);
        imgHeroe.setIcon(icon);

        JLabel lblHeroe = new JLabel(this.heroe.isBlank() ? "Personaje" : this.heroe);
        lblHeroe.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblHeroe.setForeground(new Color(210, 210, 210));
        lblHeroe.setFont(lblHeroe.getFont().deriveFont(Font.PLAIN, 16f));
        lblHeroe.setBorder(new EmptyBorder(6, 0, 10, 0));

        JPanel nombrePanel = new JPanel(new BorderLayout(8, 0));
        nombrePanel.setOpaque(false);

        JLabel lblNombre = new JLabel("Nombre del jugador:");
        lblNombre.setForeground(new Color(230, 230, 230));
        lblNombre.setFont(lblNombre.getFont().deriveFont(Font.PLAIN, 16f));

        txtNombre.setFont(txtNombre.getFont().deriveFont(18f));
        txtNombre.setColumns(18);
        txtNombre.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 70, 70)),
                new EmptyBorder(8, 10, 8, 10)
        ));
        txtNombre.setBackground(new Color(35, 35, 35));
        txtNombre.setForeground(new Color(230, 230, 230));
        limitarLongitud(txtNombre, 16); // max 16

        nombrePanel.add(lblNombre, BorderLayout.WEST);
        nombrePanel.add(txtNombre, BorderLayout.CENTER);

        centro.add(Box.createVerticalStrut(8));
        centro.add(imgHeroe);
        centro.add(Box.createVerticalStrut(6));
        centro.add(lblHeroe);
        centro.add(Box.createVerticalStrut(6));
        centro.add(nombrePanel);

        root.add(centro, BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        botones.setOpaque(false);
        JButton btnCancelar = new JButton("Cancelar");
        JButton btnAceptar  = new JButton("Jugar");

        btnCancelar.addActionListener(e -> { nombreFinal = null; dispose(); });
        btnAceptar.addActionListener(e -> onAceptar());

        botones.add(btnCancelar);
        botones.add(btnAceptar);
        root.add(botones, BorderLayout.SOUTH);

        setContentPane(root);
        pack();

        if (getWidth() < 520) setSize(520, getHeight());
        setLocationRelativeTo(owner);
    }

    /** Muestra el dialogo y devuelve el nombre (o null si cancela). */
    public String showDialog() {
        setVisible(true);
        return nombreFinal;
    }

    // ===== Helpers =====
    private void onAceptar() {
        String nom = txtNombre.getText();
        if (nom == null) nom = "";
        nom = nom.trim();

        if (nom.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Por favor ingresa un nombre.",
                    "Falta el nombre",
                    JOptionPane.WARNING_MESSAGE);
            txtNombre.requestFocusInWindow();
            return;
        }

        // letras, numeros, espacio, _ y -, hasta 16
        if (!nom.matches("[A-Za-z0-9 _\\-]{1,16}")) {
            JOptionPane.showMessageDialog(this,
                    "Nombre invalido. Usa letras, numeros, espacio, _ o - (max 16).",
                    "Nombre invalido",
                    JOptionPane.ERROR_MESSAGE);
            txtNombre.requestFocusInWindow();
            return;
        }
        nombreFinal = nom;
        dispose();
    }

    private static String resolveHeroImg(String heroe) {
        if (heroe == null || heroe.isBlank()) return HERO_IMG.get("_default");
        return HERO_IMG.getOrDefault(heroe, HERO_IMG.get("_default"));
    }

    private static ImageIcon cargarIcono(String ruta, int w, int h) {
        try {
            java.net.URL u = NombreJugadorDialog.class.getResource(ruta);
            if (u != null) {
                ImageIcon raw = new ImageIcon(u);
                Image esc = raw.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
                return new ImageIcon(esc);
            }
        } catch (Exception ignore) {}
        // fallback si no hay imagen
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
}

package Menu;

import javax.swing.*;
import java.awt.*;

public class MenuPrincipal extends JFrame {

    private static final String FONDO1 = "/Imagenes/menuprin.gif";
    private static final String FONDO2 = "/Menu/imagen/Jefe.gif";
    private static final Dimension BTN_SIZE = new Dimension(280, 56);

    public MenuPrincipal() {
        super("🎮 Menú Principal");
        fullscreen(this);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new PanelFondo(firstExisting(FONDO1, FONDO2));
        root.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.insets = new Insets(12, 0, 12, 0);
        gbc.fill = GridBagConstraints.NONE;

        // ======= TÍTULO DEL JUEGO =======
        JLabel titulo = new JLabel("Dungeons and Dragons", SwingConstants.CENTER);
        titulo.setFont(new Font("Serif", Font.BOLD, 64));
        titulo.setForeground(new Color(255, 215, 0));
        titulo.setOpaque(false);
        titulo.setBorder(BorderFactory.createEmptyBorder(20, 0, 40, 0));

        // Efecto de sombra / brillo
        titulo.setUI(new javax.swing.plaf.basic.BasicLabelUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                String text = ((JLabel) c).getText();
                FontMetrics fm = g2.getFontMetrics(c.getFont());
                int x = (c.getWidth() - fm.stringWidth(text)) / 2;
                int y = (c.getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.setColor(new Color(0, 0, 0, 150));
                g2.drawString(text, x + 3, y + 3);
                g2.setColor(new Color(255, 215, 0));
                g2.drawString(text, x, y);
                g2.dispose();
            }
        });

        gbc.gridy = 0;
        root.add(titulo, gbc);

        // ======= BOTONES =======
        JButton jugar         = boton("Iniciar Juego");
        JButton instrucciones = boton("Instrucciones");
        JButton puntuaciones  = boton("Puntuaciones");
        JButton creditos      = boton("Créditos");
        JButton salir         = boton("Salir");

        jugar.setPreferredSize(BTN_SIZE);
        instrucciones.setPreferredSize(BTN_SIZE);
        puntuaciones.setPreferredSize(BTN_SIZE);
        creditos.setPreferredSize(BTN_SIZE);
        salir.setPreferredSize(BTN_SIZE);

        gbc.gridy = 1; root.add(jugar, gbc);
        gbc.gridy = 2; root.add(instrucciones, gbc);
        gbc.gridy = 3; root.add(puntuaciones, gbc);
        gbc.gridy = 4; root.add(creditos, gbc);
        gbc.gridy = 5; root.add(salir, gbc);

        setContentPane(root);

        // ======= NAVEGACIÓN =======
        jugar.addActionListener(e -> {
            setContentPane(new MenuDificultadPanel(this));
            revalidate(); repaint();
        });

        instrucciones.addActionListener(e -> {
            setContentPane(new MenuInstruccionesPanel(this));
            revalidate(); repaint();
        });

        puntuaciones.addActionListener(e -> {
            setContentPane(new MenuPuntuacionesPanel(this));
            revalidate(); repaint();
        });

        creditos.addActionListener(e -> {
            setContentPane(new MenuCreditosPanel(this));
            revalidate(); repaint();
        });

        salir.addActionListener(e -> System.exit(0));
    }

    void volverAlPrincipal() {
        new MenuPrincipal().setVisible(true);
        dispose();
    }

    // ---------- utilidades ----------
    public static void fullscreen(JFrame f) {
        f.setUndecorated(true);
        f.setExtendedState(JFrame.MAXIMIZED_BOTH);
        f.setResizable(false);
        f.setLocationRelativeTo(null);
    }

    public static JButton boton(String txt) {
        JButton b = new JButton(txt) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 180));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.setStroke(new BasicStroke(2f));
                g2.setColor(new Color(255, 215, 0, 200));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 18, 18);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setRolloverEnabled(false);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setFocusable(false);

        b.setForeground(Color.WHITE);
        b.setFont(new Font("Consolas", Font.BOLD, 22));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
        return b;
    }

    public static String firstExisting(String... rutas) {
        for (String r : rutas)
            if (MenuPrincipal.class.getResource(r) != null)
                return r;
        return null;
    }

    static class PanelFondo extends JPanel {
        final Image img;
        PanelFondo(String ruta) {
            Image tmp = null;
            if (ruta != null) {
                try { tmp = new ImageIcon(getClass().getResource(ruta)).getImage(); } catch (Exception ignore) {}
            }
            img = tmp;
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (img != null) g.drawImage(img, 0, 0, getWidth(), getHeight(), this);
            else {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0,0,new Color(20,20,20),0,getHeight(),new Color(60,60,60)));
                g2.fillRect(0,0,getWidth(),getHeight());
            }
        }
    }
}

package Menu;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

/**
 * Créditos con "transmisión" (glitch + scroll desde abajo) y estilo glass.
 *
 * Fondos (se usa el primero que exista):
 *  - /Imagenes/menuprin.gif
 *  - /Menu/imagen/Jefe.gif
 *  - /Menu/imagen/Enemigos/Calavoso2.png
 *
 * Equipo: Jair, Oscar, Elmer, Luis, Paola
 */
public class MenuCreditosPanel extends JPanel {

    private static final String F1 = "/Imagenes/menuprin.gif";
    private static final String F2 = "/Menu/imagen/Jefe.gif";
    private static final String F3 = "/Menu/imagen/Enemigos/Calavoso2.png";

    public MenuCreditosPanel(MenuPrincipal frame) {
        setLayout(new BorderLayout());

        // Fondo pantalla completa
        String fondoRuta = MenuPrincipal.firstExisting(F1, F2, F3);
        JPanel fondo = new MenuPrincipal.PanelFondo(fondoRuta);
        fondo.setLayout(new GridBagLayout());
        add(fondo, BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.insets = new Insets(22, 22, 22, 22);
        gbc.anchor = GridBagConstraints.CENTER;

        // Contenedor “glass”
        JPanel glass = new RoundedGlassPanel();
        glass.setLayout(new BorderLayout());
        glass.setPreferredSize(new Dimension(900, 560));
        glass.setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));

        // Título
        JLabel titulo = new JLabel("✨ Créditos", SwingConstants.CENTER);
        titulo.setFont(new Font("Consolas", Font.BOLD, 36));
        titulo.setForeground(new Color(255, 215, 0));
        JPanel header = new JPanel(new BorderLayout()) { @Override public boolean isOpaque(){ return false; } };
        header.add(titulo, BorderLayout.CENTER);

        // Canvas de créditos con transmisión
        CreditsCanvas canvas = new CreditsCanvas();
        JScrollPane sp = new JScrollPane(canvas);
        sp.setOpaque(false); sp.getViewport().setOpaque(false);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // Botonera
        JButton btnPlayPause = MenuPrincipal.boton("Pausar");
        JButton btnReset     = MenuPrincipal.boton("Reiniciar");
        JButton btnVolver    = MenuPrincipal.boton("Volver");
        Dimension BTN = new Dimension(220, 52);
        btnPlayPause.setPreferredSize(BTN);
        btnReset.setPreferredSize(BTN);
        btnVolver.setPreferredSize(BTN);

        JPanel south = new JPanel() { @Override public boolean isOpaque(){ return false; } };
        south.add(btnPlayPause);
        south.add(btnReset);
        south.add(btnVolver);

        glass.add(header, BorderLayout.NORTH);
        glass.add(sp,     BorderLayout.CENTER);
        glass.add(south,  BorderLayout.SOUTH);

        // Columna centrada
        JPanel columna = new JPanel();
        columna.setOpaque(false);
        columna.setLayout(new BoxLayout(columna, BoxLayout.Y_AXIS));
        columna.add(Box.createVerticalGlue());
        columna.add(glass);
        columna.add(Box.createVerticalGlue());

        fondo.add(columna, gbc);

        // Acciones
        btnPlayPause.addActionListener(e -> {
            canvas.togglePause();
            btnPlayPause.setText(canvas.isPaused() ? "Reanudar" : "Pausar");
            canvas.requestFocusInWindow();
        });
        btnReset.addActionListener(e -> { canvas.resetScroll(true); canvas.requestFocusInWindow(); });
        btnVolver.addActionListener(e -> {
            frame.setContentPane(new MenuPrincipal.PanelFondo(MenuPrincipal.firstExisting(F1, F2, F3)));
            new MenuPrincipal().setVisible(true);
            frame.dispose();
        });
    }

    /** Panel de vidrio translúcido con borde dorado. */
    static class RoundedGlassPanel extends JPanel {
        RoundedGlassPanel() { setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = 24;
            g2.setColor(new Color(0, 0, 0, 170));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(new Color(255, 215, 0, 200));
            g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Lienzo de créditos con efecto transmisión (glitch) y scroll vertical. */
    static class CreditsCanvas extends JComponent implements ActionListener, KeyListener {

        // Estilo de colores
        private static final Color TITLE_COLOR = new Color(255, 255, 170);
        private static final Color NAME_COLOR  = new Color(140, 255, 140);
        private static final Color ROLE_COLOR  = new Color(230, 230, 230);
        private static final Color SHADOW      = new Color(0, 0, 0, 170);

        // Scroll / transmisión
        private static final int    LINE_SPACING       = 40;    // separación vertical
        private static final double BASE_SPEED         = 0.9;   // px/tick
        private static final double SPEED_MAX          = 1.8;
        private static final double ACCEL              = 0.004; // aceleración suave
        private static final int    REVEAL_MS_PER_CHAR = 18;    // ms por carácter
        private static final int    REVEAL_PRE_OFFSET  = 220;   // activa glitch antes de pasar banda
        private static final float  TOP_MARGIN_FACTOR  = 0.68f; // dónde inicia (parte baja)

        private static final char[] GLITCH =
                ("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789#@%&$+=?/*[]{}<>").toCharArray();

        // Estrellas “pixel” de fondo del canvas
        private static class Star { float x,y,dx,dy,size,alpha; }
        private Star[] stars;
        private final Random rng = new Random();

        // Estructura de línea
        private static class Line {
            final String text;
            final Font font;
            final Color color;
            long startRevealMs = -1;
            Line(String t, Font f, Color c) { text=t; font=f; color=c; }
        }
        private final java.util.List<Line> lines = new java.util.ArrayList<>();

        // Estado
        private final Timer timer = new Timer(16, this);
        private double offsetY;
        private double speed = BASE_SPEED;
        private boolean paused = false;
        private long nowMs;

        CreditsCanvas() {
            setOpaque(false);
            setPreferredSize(new Dimension(880, 420)); // visible dentro del glass
            setFocusable(true);
            addKeyListener(this);
            buildLines();
            resetScroll(false);
            timer.start();
        }

        private void buildLines() {
            Font big   = new Font("Consolas", Font.BOLD, 42);
            Font mid   = new Font("Consolas", Font.BOLD, 26);
            Font small = new Font("Consolas", Font.PLAIN, 20);

            // Espacio inicial
            for (int i=0;i<6;i++) lines.add(new Line("", mid, NAME_COLOR));

            lines.add(new Line("~ ~ ~  CRÉDITOS  ~ ~ ~", big,  TITLE_COLOR));
            lines.add(new Line("", mid, NAME_COLOR));
            lines.add(new Line("Un juego de arena infinito", small, ROLE_COLOR));
            lines.add(new Line("Jefes cada 3 rondas",        small, ROLE_COLOR));
            lines.add(new Line("", mid, NAME_COLOR));

            lines.add(new Line("Equipo", big.deriveFont(36f), TITLE_COLOR));
            lines.add(new Line("", mid, NAME_COLOR));

            block("Jair",  "Diseño y Gameplay", mid, small);
            block("Oscar", "Programación UI",   mid, small);
            block("Elmer", "IA de Enemigos",    mid, small);
            block("Luis",  "Balance y Rondas",  mid, small);
            block("Paola", "Arte y Audio",      mid, small);

            lines.add(new Line("", mid, NAME_COLOR));
            lines.add(new Line("Gracias por jugar 💛", mid, NAME_COLOR));

            // Espacio final
            for (int i=0;i<16;i++) lines.add(new Line("", mid, NAME_COLOR));
            lines.add(new Line("— Fin —", small, NAME_COLOR));
            for (int i=0;i<12;i++) lines.add(new Line("", mid, NAME_COLOR));
        }

        private void block(String nombre, String rol, Font fn, Font fr) {
            lines.add(new Line(nombre, fn, NAME_COLOR));
            lines.add(new Line(rol,    fr, ROLE_COLOR));
            lines.add(new Line("",     fn, NAME_COLOR));
        }

        @Override public void addNotify() {
            super.addNotify();
            requestFocusInWindow();
            // Estrellas según tamaño
            int count = Math.max(60, (getWidth()*getHeight())/18000);
            stars = new Star[count];
            for (int i=0;i<count;i++) stars[i] = randomStar(true);
        }

        private Star randomStar(boolean anywhere){
            Star s = new Star();
            s.size  = 1f + rng.nextFloat()*2.0f;
            s.alpha = 0.35f + rng.nextFloat()*0.65f;
            s.dx    = (rng.nextFloat()-0.5f)*0.06f;
            s.dy    = -0.18f - rng.nextFloat()*0.22f;
            if (anywhere) { s.x = rng.nextFloat()*Math.max(1,getWidth()); s.y = rng.nextFloat()*Math.max(1,getHeight()); }
            else          { s.x = rng.nextFloat()*Math.max(1,getWidth()); s.y = getHeight()+10; }
            return s;
        }

        // Control
        public void togglePause(){ paused = !paused; }
        public boolean isPaused(){ return paused; }

        public void resetScroll(boolean smooth){
            offsetY = getHeight() * TOP_MARGIN_FACTOR;
            speed   = smooth ? 0.2 : BASE_SPEED;
            paused  = false;
            for (Line ln : lines) ln.startRevealMs = -1;
        }

        // Loop
        @Override public void actionPerformed(ActionEvent e) {
            nowMs = System.currentTimeMillis();

            // estrellas
            if (stars!=null) {
                for (Star s : stars) {
                    s.x += s.dx; s.y += s.dy;
                    if (s.y < -8 || s.x < -8 || s.x > getWidth()+8) {
                        Star r = randomStar(false);
                        s.x=r.x; s.y=r.y; s.dx=r.dx; s.dy=r.dy; s.size=r.size; s.alpha=r.alpha;
                    }
                }
            }

            if (!paused) {
                speed = Math.min(SPEED_MAX, speed + ACCEL);
                offsetY -= speed;
            }
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // estrellas pixel
            if (stars!=null){
                for (Star s : stars) {
                    g2.setColor(new Color(255,255,255,(int)(s.alpha*255)));
                    int sz = Math.max(1, Math.round(s.size));
                    g2.fillRect(Math.round(s.x), Math.round(s.y), sz, sz);
                }
            }

            double y = offsetY;
            int cx = getWidth()/2;
            int bandY = (int)(getHeight()*0.72); // banda de activación
            for (int i=0;i<lines.size();i++) {
                Line ln = lines.get(i);
                if (ln.text.isEmpty()) { y += LINE_SPACING; continue; }
                g2.setFont(ln.font);
                FontMetrics fm = g2.getFontMetrics();
                int w = fm.stringWidth(ln.text);
                int ascent = fm.getAscent();

                int tx = cx - w/2;
                int ty = (int)Math.round(y + ascent);

                // activar transmisión cerca de la banda inferior
                if (ln.startRevealMs < 0 && ty < (getHeight() + REVEAL_PRE_OFFSET) && ty > bandY - 100) {
                    ln.startRevealMs = nowMs;
                }

                String toDraw = ln.text;
                if (ln.startRevealMs > 0) {
                    long elapsed = Math.max(0, nowMs - ln.startRevealMs);
                    int revealChars = (int)Math.min(ln.text.length(), Math.floor(elapsed / (double)REVEAL_MS_PER_CHAR));
                    toDraw = mixGlitch(ln.text, revealChars, i);
                }

                if (ty > -50 && ty < getHeight()+60) {
                    g2.setColor(SHADOW); g2.drawString(toDraw, tx+2, ty+2);
                    g2.setColor(ln.color); g2.drawString(toDraw, tx, ty);
                }

                y += LINE_SPACING;
            }

            // hint inferior
            String hint = paused ? "ESPACIO: Reanudar  •  R: Reiniciar"
                    : "ESPACIO: Pausar    •  R: Reiniciar";
            g2.setFont(new Font("Consolas", Font.PLAIN, 14));
            FontMetrics fmh = g2.getFontMetrics();
            int ww = fmh.stringWidth(hint);
            int xh = cx - ww/2;
            int yh = getHeight() - 10;
            g2.setColor(new Color(0,0,0,140));
            g2.fillRoundRect(xh - 10, yh - 22, ww + 20, 26, 10, 10);
            g2.setColor(new Color(255, 215, 0));
            g2.drawString(hint, xh, yh - 4);

            g2.dispose();
        }

        /** Texto real con glitch: los primeros 'reveal' se muestran reales. */
        private String mixGlitch(String real, int reveal, int seed) {
            if (reveal >= real.length()) return real;
            StringBuilder sb = new StringBuilder(real.length());
            sb.append(real, 0, Math.max(0, reveal));
            int remain = real.length() - reveal;
            int r = seed * 73856093 + (int)(nowMs/60);
            for (int i=0;i<remain;i++) {
                char c = real.charAt(reveal + i);
                if (Character.isWhitespace(c)) { sb.append(' '); continue; }
                char g = GLITCH[Math.floorMod(r + i*31, GLITCH.length)];
                sb.append(g);
            }
            return sb.toString();
        }

        // Teclas (atajos dentro del canvas)
        @Override public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()){
                case KeyEvent.VK_SPACE: togglePause(); break;
                case KeyEvent.VK_R:     resetScroll(true); break;
            }
        }
        @Override public void keyReleased(KeyEvent e) {}
        @Override public void keyTyped(KeyEvent e) {}
    }
}

package Player;

import javax.swing.*;
import java.awt.*;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * GameFrameNiveles
 *
 * Controla el progreso a través de las arenas (Arena1 -> Arena5).
 * Muestra transiciones animadas entre niveles y una pantalla de victoria épica.
 */
public class GameFrameNiveles extends JFrame implements LevelListener {

    private int arenaActual = 1;
    private int puntuacionGlobal = 0;
    private int vidaGlobal = -1;

    private final GameSettings settings;
    private final String heroe;
    private final String nombreJugador;
    private final long startNanosGlobal;

    public GameFrameNiveles(GameSettings settings, String heroe, String nombreJugador) {
        this.settings = (settings != null) ? settings : GameSettings.fromDificultad("Media");
        this.heroe = (heroe != null && !heroe.isBlank()) ? heroe : "Default";
        this.nombreJugador = (nombreJugador == null || nombreJugador.isBlank()) ? "Invitado" : nombreJugador;

        setTitle("Arena por niveles");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);

        this.startNanosGlobal = System.nanoTime();
        cargarArena(1);
    }

    // ===================================================================
    //                  CARGAR ARENAS Y PROGRESO
    // ===================================================================
    public void cargarArena(int numeroArena) {
        this.arenaActual = numeroArena;
        getContentPane().removeAll();

        ArenaBase arena;
        switch (numeroArena) {
            case 1 -> arena = new Arena1(settings, heroe, nombreJugador, this);
            case 2 -> arena = new Arena2(settings, heroe, nombreJugador, this);
            case 3 -> arena = new Arena3(settings, heroe, nombreJugador, this);
            case 4 -> arena = new Arena4(settings, heroe, nombreJugador, this);
            case 5 -> arena = new Arena5(settings, heroe, nombreJugador, this);
            default -> {
                mostrarPantallaVictoriaCool();
                return;
            }
        }

        arena.setPuntuacion(puntuacionGlobal);
        int vidaInicial = (vidaGlobal > 0) ? vidaGlobal : -1;
        arena.iniciar(vidaInicial);

        setContentPane(arena);
        revalidate();
        repaint();
    }

    @Override
    public void onLevelComplete(int numeroArena, int puntuacion, int vidaJugador) {
        this.puntuacionGlobal = puntuacion;
        this.vidaGlobal = vidaJugador + 100; // 🔧 Solo +100 HP entre arenas

        if (numeroArena >= 5) {
            mostrarPantallaVictoriaCool();
        } else {
            mostrarTransicionCool(numeroArena);
        }
    }

    // ===================================================================
    //                  TRANSICIÓN ENTRE ARENAS (FADE + CUENTA REGRESIVA)
    // ===================================================================
    private void mostrarTransicionCool(int numeroArenaCompletada) {
        int siguienteArena = numeroArenaCompletada + 1;

        TransitionPanel panel = new TransitionPanel(
                numeroArenaCompletada,
                siguienteArena,
                nombreJugador,
                puntuacionGlobal
        );

        setContentPane(panel);
        revalidate();
        repaint();
    }

    // ===================================================================
    //                  GUARDAR Y VICTORIA
    // ===================================================================
    private void mostrarPantallaVictoriaCool() {
        guardarScoreVictoria();

        VictoryPanel panel = new VictoryPanel(
                nombreJugador,
                puntuacionGlobal,
                this::volverAlMenuPrincipal
        );

        setContentPane(panel);
        revalidate();
        repaint();
    }

    private void guardarScoreVictoria() {
        try {
            long elapsedSec = Math.max(0, (System.nanoTime() - startNanosGlobal) / 1_000_000_000L);
            String when = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String row = String.join(",",
                    when,
                    nombreJugador.replace(",", " "),
                    settings.dificultadName,
                    String.valueOf(puntuacionGlobal),
                    String.valueOf(elapsedSec),
                    "VICTORY"
            );
            try (FileWriter fw = new FileWriter(System.getProperty("user.home") + "/ArenaScores.csv", true)) {
                fw.write(row + System.lineSeparator());
            }
        } catch (Exception ignore) {}
    }

    private void volverAlMenuPrincipal() {
        try {
            Class<?> cls = Class.forName("Menu.MenuPrincipal");
            Object obj = cls.getConstructor().newInstance();
            if (obj instanceof JFrame jf) jf.setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    null,
                    "No se encontró el Menú Principal.\nSe cerrará la ventana.",
                    "Aviso",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
        dispose();
    }

    // ===================================================================
    //      PANEL DE TRANSICIÓN CON FADE CORREGIDO Y CUENTA REGRESIVA
    // ===================================================================
    private static class TransitionPanel extends JPanel {
        private final int arenaSiguiente;
        private final String nombreJugador;
        private final int puntuacion;
        private float alpha = 0f;
        private boolean fadingOut = false;
        private int countdown = 3;

        public TransitionPanel(int arenaCompletada, int arenaSiguiente, String nombreJugador, int puntuacion) {
            this.arenaSiguiente = arenaSiguiente;
            this.nombreJugador = nombreJugador;
            this.puntuacion = puntuacion;

            setLayout(new BorderLayout());
            setFocusable(true);

            // Fade control (seguro contra valores fuera de rango)
            new javax.swing.Timer(50, e -> {
                if (!fadingOut && alpha < 1f)
                    alpha = Math.min(1f, alpha + 0.05f);
                else if (fadingOut && alpha > 0f)
                    alpha = Math.max(0f, alpha - 0.05f);
                repaint();
            }).start();

            // Cuenta regresiva: 3, 2, 1, luego fade-out y cambio de arena
            new javax.swing.Timer(1000, e -> {
                countdown--;
                if (countdown <= 0) {
                    ((javax.swing.Timer) e.getSource()).stop();
                    fadingOut = true;
                    new javax.swing.Timer(1500, ev -> {
                        ((javax.swing.Timer) ev.getSource()).stop();
                        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
                        if (topFrame instanceof GameFrameNiveles gfn)
                            gfn.cargarArena(arenaSiguiente);
                    }).start();
                }
            }).start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Asegurar alpha válido
            float a = Math.max(0f, Math.min(1f, alpha));
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));

            // Fondo degradado oscuro con brillo rojo
            GradientPaint gp = new GradientPaint(
                    0, 0, new Color(20, 20, 20),
                    0, getHeight(), new Color(90, 10, 10)
            );
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Halo dorado suave
            g2.setColor(new Color(255, 215, 0, 70));
            int r = Math.max(getWidth(), getHeight());
            g2.fillOval(getWidth() / 2 - r / 2, getHeight() / 2 - r / 2, r, r);

            // Card central
            int cardW = Math.min(600, getWidth() - 80);
            int cardH = 240;
            int cardX = (getWidth() - cardW) / 2;
            int cardY = (getHeight() - cardH) / 2;
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRoundRect(cardX, cardY, cardW, cardH, 30, 30);
            g2.setStroke(new BasicStroke(3f));
            g2.setColor(new Color(255, 215, 0, 200));
            g2.drawRoundRect(cardX + 1, cardY + 1, cardW - 2, cardH - 2, 30, 30);

            // Texto principal: NIVEL X
            g2.setFont(new Font("Consolas", Font.BOLD, 58));
            String titulo = "NIVEL " + arenaSiguiente;
            int tw = g2.getFontMetrics().stringWidth(titulo);
            g2.setColor(new Color(255, 240, 180));
            g2.drawString(titulo, getWidth() / 2 - tw / 2, cardY + 90);

            // Jugador y puntuación
            g2.setFont(new Font("Consolas", Font.PLAIN, 24));
            String jTxt = "Jugador: " + nombreJugador;
            String pTxt = "Puntuación: " + puntuacion;
            int baseY = cardY + 140;
            g2.setColor(Color.WHITE);
            g2.drawString(jTxt, getWidth() / 2 - g2.getFontMetrics().stringWidth(jTxt) / 2, baseY);
            g2.drawString(pTxt, getWidth() / 2 - g2.getFontMetrics().stringWidth(pTxt) / 2, baseY + 30);

            // Cuenta regresiva
            g2.setFont(new Font("Consolas", Font.BOLD, 40));
            String cTxt = (countdown > 0) ? "Entrando en... " + countdown : "Cargando arena...";
            int twC = g2.getFontMetrics().stringWidth(cTxt);
            g2.setColor(new Color(255, 215, 0, 210));
            g2.drawString(cTxt, getWidth() / 2 - twC / 2, cardY + cardH - 30);

            g2.dispose();
        }
    }

    // ===================================================================
    //              PANTALLA DE VICTORIA ÉPICA CON RELÁMPAGOS
    // ===================================================================
    private static class VictoryPanel extends JPanel {
        private final String nombreJugador;
        private final int puntuacionFinal;
        private final Runnable onVolverMenu;
        private final java.util.List<Lightning> rayos = new ArrayList<>();
        private long lastRayo = 0;
        private double shakePhase = 0;
        private float glowPhase = 0;

        public VictoryPanel(String nombreJugador, int puntuacionFinal, Runnable onVolverMenu) {
            this.nombreJugador = nombreJugador;
            this.puntuacionFinal = puntuacionFinal;
            this.onVolverMenu = onVolverMenu;

            setLayout(new BorderLayout());
            setFocusable(true);

            new javax.swing.Timer(16, e -> {
                glowPhase += 0.05;
                shakePhase += 0.12;
                if (Math.random() < 0.02 && System.currentTimeMillis() - lastRayo > 600) {
                    rayos.add(new Lightning(getWidth(), getHeight()));
                    lastRayo = System.currentTimeMillis();
                }
                rayos.removeIf(l -> !l.activo);
                repaint();
            }).start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            GradientPaint gp = new GradientPaint(0, 0, new Color(20, 0, 0), 0, getHeight(), new Color(100, 0, 0));
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(new Color(255, 80, 0, 70));
            int r = Math.max(getWidth(), getHeight());
            g2.fillOval(getWidth()/2 - r/2, getHeight()/2 - r/2, r, r);

            for (Lightning l : rayos) l.draw(g2);

            int offsetX = (int)(Math.sin(shakePhase) * 2);
            int offsetY = (int)(Math.cos(shakePhase * 0.8) * 2);

            int cardW = Math.min(700, getWidth() - 80);
            int cardH = 270;
            int cardX = (getWidth() - cardW) / 2 + offsetX;
            int cardY = (getHeight() - cardH) / 2 + offsetY;

            g2.setColor(new Color(30, 0, 0, 240));
            g2.fillRoundRect(cardX, cardY, cardW, cardH, 28, 28);
            g2.setStroke(new BasicStroke(3f));
            g2.setColor(new Color(255, 215, 0, 230));
            g2.drawRoundRect(cardX + 1, cardY + 1, cardW - 2, cardH - 2, 28, 28);

            float glow = (float)(0.5 + 0.5 * Math.sin(glowPhase));
            Color glowColor = new Color(255, 220, 80, (int)(150 + 80 * glow));

            g2.setFont(new Font("Consolas", Font.BOLD, 64));
            String titulo = "¡VICTORIA!";
            int tw = g2.getFontMetrics().stringWidth(titulo);
            g2.setColor(glowColor);
            g2.drawString(titulo, getWidth()/2 - tw/2, cardY + 80);

            g2.setFont(new Font("Consolas", Font.PLAIN, 24));
            String jTxt = "Jugador: " + nombreJugador;
            String pTxt = "Puntuación final: " + puntuacionFinal;
            int baseY = cardY + 130;
            g2.setColor(Color.WHITE);
            g2.drawString(jTxt, getWidth()/2 - g2.getFontMetrics().stringWidth(jTxt)/2, baseY);
            g2.drawString(pTxt, getWidth()/2 - g2.getFontMetrics().stringWidth(pTxt)/2, baseY + 30);

            int btnW = 300, btnH = 50;
            int btnX = getWidth()/2 - btnW/2;
            int btnY = cardY + cardH - 70;
            g2.setColor(new Color(90, 0, 0, 230));
            g2.fillRoundRect(btnX, btnY, btnW, btnH, 20, 20);
            g2.setColor(new Color(255, 215, 0, 200));
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawRoundRect(btnX, btnY, btnW, btnH, 20, 20);
            g2.setFont(new Font("Consolas", Font.BOLD, 18));
            String btnTxt = "Volver al Menú Principal (ENTER)";
            g2.setColor(Color.WHITE);
            g2.drawString(btnTxt, getWidth()/2 - g2.getFontMetrics().stringWidth(btnTxt)/2, btnY + 32);

            g2.dispose();
        }

        @Override
        public void addNotify() {
            super.addNotify();
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"), "menu");
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("SPACE"), "menu");
            getActionMap().put("menu", new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (onVolverMenu != null) onVolverMenu.run();
                }
            });
        }

        private static class Lightning {
            int x1, y1, x2, y2, life = 10;
            boolean activo = true;
            Stroke stroke = new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            Lightning(int w, int h) {
                Random r = new Random();
                x1 = r.nextInt(w);
                y1 = 0;
                x2 = x1 + r.nextInt(80) - 40;
                y2 = r.nextInt(h / 2) + h / 3;
            }
            void draw(Graphics2D g) {
                if (!activo) return;
                g.setStroke(stroke);
                g.setColor(new Color(255, 255, 180, (int)(180 * (life / 10.0))));
                g.drawLine(x1, y1, x2, y2);
                g.drawLine(x2, y2, x2 + (int)(Math.random() * 30 - 15), y2 + (int)(Math.random() * 40));
                life--;
                if (life <= 0) activo = false;
            }
        }
    }
}

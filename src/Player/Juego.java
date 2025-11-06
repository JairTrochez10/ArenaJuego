package Player;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Juego extends JPanel implements ActionListener, KeyListener {
    private Timer timer;
    private Personajes paola;
    private Monstruo enemigos;
    private JefeFinal jefeFinal;

    private boolean gameOver = false;
    private boolean jefeActivo = false;

    private final int margen = 40;
    private Image fondo;

    private final GameSettings settings;
    private final String heroeElegido;
    private final String nombreJugador;

    // Rondas infinitas
    private int ronda = 1;
    private int multiplicadorRonda = 1;
    private double roundMulEnemy  = 1.0;  // +15% por ronda
    private double roundMulBoss   = 1.0;  // +14% por ronda

    // Puntuación/tiempo
    private long startNanos;
    private int puntuacion = 0;

    private int touchCooldown = 0;
    private boolean posInicialAjustada = false;

    private String toastMsg = null;
    private long toastUntilMs = 0;
    private void showToast(String msg, int ms){ toastMsg = msg; toastUntilMs = System.currentTimeMillis() + ms; }

    private boolean endDialogShown = false;

    // === NUEVO: animación de curación por ronda ===
    private long healAnimUntilMs = 0;          // hasta cuándo mostrar la animación
    private static final int HEAL_AMOUNT = 100;

    // === NUEVO: habilidad tipo jefe con recarga visible ===
    private long abilityCooldownMs = 10_000;   // 10 segundos
    private long abilityLastUsedMs = -100_000; // para que al inicio falte poco
    private static final String ABILITY_KEY_HINT = "Q";

    // --- PAUSA ---
    private boolean paused = false;
    private final JButton btnPause = uiSmallButton("Pausa");

    public Juego(GameSettings settings, String heroe, String nombreJugador) {
        this.settings = settings != null ? settings : GameSettings.fromDificultad("Media");
        this.heroeElegido = heroe != null ? heroe : "Default";
        this.nombreJugador = (nombreJugador == null || nombreJugador.isBlank()) ? "Invitado" : nombreJugador;
        init();
    }
    public Juego(GameSettings settings, String heroe) { this(settings, heroe, "Invitado"); }
    public Juego() { this(GameSettings.fromDificultad("Media"), "Default", "Invitado"); }

    private void init() {
        setFocusable(true);
        addKeyListener(this);
        try { fondo = new ImageIcon(getClass().getResource("/Imagenes/Arena.jpg")).getImage(); } catch (Exception ignore) {}

        paola = new Personajes(300, 300, settings);
        paola.setHeroeSprite(heroeElegido);

        enemigos = new Monstruo(settings);
        enemigos.setExtraSpeedMul(roundMulEnemy);
        enemigos.spawnRonda(multiplicadorRonda);

        // jugador más rápido que enemigos (2x y min +5)
        syncPlayerSpeed();

        // --- PAUSA: botón
        setLayout(null);
        btnPause.setFocusable(false);
        btnPause.addActionListener(e -> togglePause());
        add(btnPause);

        timer = new Timer(16, this);
        timer.start();

        startNanos = System.nanoTime();
        showToast("¡Ronda " + ronda + "!", 1500);
    }

    @Override public void addNotify(){ super.addNotify(); requestFocusInWindow(); }

    /** Jugador SIEMPRE más rápido: 2x la velocidad efectiva de los enemigos y, como mínimo, +5 por encima. */
    private void syncPlayerSpeed() {
        double enemySpeedNow = Monstruo.SPEED_BASE * settings.enemySpeedMul * roundMulEnemy;
        double desiredByFactor = enemySpeedNow * settings.playerSpeedAdvantage; // 2.0
        int minPlusFive = (int)Math.ceil(enemySpeedNow) + 5;
        int playerTarget = Math.max((int)Math.round(desiredByFactor), minPlusFive);
        paola.setVelocidad(playerTarget);
    }

    // --- PAUSA: mantener botón en esquina
    @Override public void doLayout() {
        super.doLayout();
        int w = getWidth();
        btnPause.setBounds(Math.max(12, w - 110), 12, 96, 36);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Proyectil.setViewportGlobal(getWidth(), getHeight(), margen);

        if (!posInicialAjustada && getWidth() > 0 && getHeight() > 0) {
            paola.x = getWidth()/2 - Personajes.SIZE/2;
            paola.y = getHeight()/2 - Personajes.SIZE/2;
            posInicialAjustada = true;
        }

        paola.setLimitesPantalla(getWidth(), getHeight(), margen);
        enemigos.setLimitesPantalla(getWidth(), getHeight(), margen);
        if (jefeActivo && jefeFinal != null) jefeFinal.setLimitesPantalla(getWidth(), getHeight(), margen);

        // --- PAUSA: cortar la lógica
        if (paused) {
            repaint();
            return;
        }

        // mantener ventaja en tiempo real
        syncPlayerSpeed();

        if (!gameOver) {
            paola.update();
            enemigos.update(paola);

            // Player → Enemigos
            for (Monstruo.Enemigo en : new java.util.ArrayList<>(enemigos.enemigos)) {
                for (Proyectil p : new java.util.ArrayList<>(paola.proyectiles)) {
                    if (p.getBounds().intersects(en.getBounds())) {
                        en.recibirDaño(p.daño);
                        p.activo = false;
                        if (en.vida <= 0) puntuacion += 100;
                    }
                }
                if (en.vida <= 0) enemigos.enemigos.remove(en);
            }

            // Enemigos → Player (disparos)
            for (Monstruo.Enemigo en : enemigos.enemigos) {
                for (Proyectil d : new java.util.ArrayList<>(en.disparos)) {
                    if (d.getBounds().intersects(paola.getBounds())) {
                        paola.recibirDaño(d.daño);
                        d.activo = false;
                    }
                }
            }

            // Contacto
            if (touchCooldown > 0) touchCooldown--;
            for (Monstruo.Enemigo en : enemigos.enemigos) {
                if (!en.puedeDisparar && en.getBounds().intersects(paola.getBounds()) && touchCooldown == 0) {
                    paola.recibirDaño(Math.max(6, (int)Math.round(en.daño * 0.8)));
                    touchCooldown = 15;
                }
            }

            // Rondas / Jefe cada 3
            if (enemigos.enemigos.isEmpty() && !jefeActivo) {
                if (ronda % 3 == 0) {
                    jefeFinal = new JefeFinal(getWidth()/2 - JefeFinal.SIZE/2, margen + 20, settings);
                    jefeFinal.setExtraSpeedMul(roundMulBoss);
                    jefeFinal.setLimitesPantalla(getWidth(), getHeight(), margen);
                    jefeActivo = true;
                    showToast("¡JEFE FINAL!", 1500);
                } else {
                    siguienteRondaEnemigos();
                }
            }

            if (jefeActivo && jefeFinal != null) {
                jefeFinal.update(paola);

                for (Proyectil p : new java.util.ArrayList<>(paola.proyectiles)) {
                    if (p.getBounds().intersects(jefeFinal.getBounds())) {
                        jefeFinal.recibirDaño(p.daño);
                        p.activo = false;
                        if (jefeFinal.vida <= 0) puntuacion += 1000;
                    }
                }
                for (Proyectil d : new java.util.ArrayList<>(jefeFinal.disparos)) {
                    if (d.getBounds().intersects(paola.getBounds())) {
                        paola.recibirDaño(d.daño);
                        d.activo = false;
                    }
                }

                if (jefeFinal.vida <= 0) {
                    jefeActivo = false;
                    jefeFinal = null;
                    siguienteRondaEnemigos();
                }
            }

            if (paola.vida <= 0) gameOver = true;
        }

        repaint();

        if (gameOver && timer != null) {
            timer.stop();
            saveScore();
            if (!endDialogShown) {
                endDialogShown = true;
                SwingUtilities.invokeLater(this::showGameOverPanel);
            }
        }
    }

    private void siguienteRondaEnemigos() {
        ronda++;
        roundMulEnemy  *= 1.15;
        roundMulBoss   *= 1.14;

        multiplicadorRonda *= 2;

        enemigos.setExtraSpeedMul(roundMulEnemy);
        enemigos.spawnRonda(multiplicadorRonda);

        // === NUEVO: curación +100 con animación ===
        int before = paola.vida;
        paola.vida = Math.min(paola.vidaMax, paola.vida + HEAL_AMOUNT);
        if (paola.vida > before) {
            healAnimUntilMs = System.currentTimeMillis() + 1200; // 1.2s de efecto
            showToast("+100 HP", 900);
        }

        // re-aplicar ventaja
        syncPlayerSpeed();

        showToast("¡Ronda " + ronda + "!", 1200);
    }

    private void saveScore() {
        try {
            long elapsedSec = Math.max(0, (System.nanoTime() - startNanos) / 1_000_000_000L);
            String when = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String row = String.join(",",
                    when,
                    nombreJugador.replace(",", " "),
                    settings.dificultadName,
                    String.valueOf(puntuacion),
                    String.valueOf(elapsedSec),
                    "GAME_OVER"
            );
            try (FileWriter fw = new FileWriter(System.getProperty("user.home") + "/ArenaScores.csv", true)) {
                fw.write(row + System.lineSeparator());
            }
        } catch (Exception ignore) {}
    }

    private static String fmtTime(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) return String.format("%dh %02dm %02ds", h, m, s);
        return String.format("%02dm %02ds", m, s);
    }

    private void showGameOverPanel() {
        long elapsedSec = Math.max(0, (System.nanoTime() - startNanos) / 1_000_000_000L);

        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Fin de partida", true);
        d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0,0,new Color(10,10,10),0,getHeight(),new Color(35,35,35)));
                g2.fillRect(0,0,getWidth(),getHeight());
                g2.dispose();
            }
        };
        root.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));

        JLabel title = new JLabel("GAME OVER", SwingConstants.CENTER);
        title.setFont(new Font("Consolas", Font.BOLD, 36));
        title.setForeground(Color.RED);
        root.add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(3,1,8,8));
        center.setOpaque(false);
        Font f = new Font("Consolas", Font.PLAIN, 22);
        JLabel l1 = new JLabel("Jugador: " + nombreJugador, SwingConstants.CENTER);
        JLabel l2 = new JLabel("Puntuación: " + puntuacion, SwingConstants.CENTER);
        JLabel l3 = new JLabel("Tiempo: " + fmtTime(elapsedSec), SwingConstants.CENTER);
        l1.setFont(f); l2.setFont(f); l3.setFont(f);
        l1.setForeground(Color.WHITE); l2.setForeground(Color.WHITE); l3.setForeground(Color.WHITE);
        center.add(l1); center.add(l2); center.add(l3);
        root.add(center, BorderLayout.CENTER);

        JPanel south = new JPanel();
        JButton retry = uiButton("Reintentar");
        JButton back  = uiButton("Volver al Menú");
        retry.setPreferredSize(new Dimension(220, 46));
        back.setPreferredSize(new Dimension(220, 46));
        south.add(retry); south.add(back);
        root.add(south, BorderLayout.SOUTH);

        retry.addActionListener(e -> { d.dispose(); reiniciarPartida(); });
        back.addActionListener(e -> {
            d.dispose();
            try {
                Class<?> cls = Class.forName("Menu.MenuPrincipal");
                Object obj = cls.getConstructor().newInstance();
                if (obj instanceof JFrame jf) jf.setVisible(true);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "No se encontró el Menú Principal.\nSe cerrará la ventana.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            }
            JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(Juego.this);
            if (topFrame != null) topFrame.dispose();
        });

        d.setContentPane(root);
        d.setSize(560, 320);
        d.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        d.setVisible(true);
    }

    private static JButton uiButton(String txt) {
        JButton b = new JButton(txt) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 200));
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
        b.setFont(new Font("Consolas", Font.BOLD, 20));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        return b;
    }

    // --- PAUSA: botón pequeño
    private static JButton uiSmallButton(String txt) {
        JButton b = new JButton(txt) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 200));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setStroke(new BasicStroke(2f));
                g2.setColor(new Color(255, 215, 0, 200));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 14, 14);
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
        b.setFont(new Font("Consolas", Font.BOLD, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        return b;
    }

    private void reiniciarPartida() {
        ronda = 1; multiplicadorRonda = 1;
        roundMulEnemy = 1.0; roundMulBoss = 1.0;
        puntuacion = 0; jefeActivo = false; gameOver = false; endDialogShown = false;

        paola = new Personajes(getWidth()/2 - Personajes.SIZE/2, getHeight()/2 - Personajes.SIZE/2, settings);
        paola.setHeroeSprite(heroeElegido);

        enemigos = new Monstruo(settings);
        enemigos.setExtraSpeedMul(roundMulEnemy);
        enemigos.setLimitesPantalla(getWidth(), getHeight(), margen);
        enemigos.spawnRonda(multiplicadorRonda);

        // reset visual de curación/habilidad
        healAnimUntilMs = 0;
        abilityLastUsedMs = System.currentTimeMillis() - (long)(abilityCooldownMs*0.8); // que casi esté lista

        // asegurar ventaja 2x y +5
        syncPlayerSpeed();

        jefeFinal = null;

        startNanos = System.nanoTime();
        showToast("¡Ronda " + ronda + "!", 1500);
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (fondo != null) g.drawImage(fondo, 0, 0, getWidth(), getHeight(), this);
        else { g.setColor(Color.DARK_GRAY); g.fillRect(0,0,getWidth(),getHeight()); }

        int panelW = 300, panelH = 134; // ↑ para meter la barra de habilidad
        g.setColor(new Color(0,0,0,140));
        g.fillRoundRect(12, 12, panelW, panelH, 12, 12);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Consolas", Font.BOLD, 16));
        g.drawString("Jugador: " + nombreJugador, 22, 38);
        g.drawString("Puntuación: " + puntuacion, 22, 62);

        g.drawString("Vida: " + paola.vida + " / " + paola.vidaMax, 22, 86);
        int barX = 22, barY = 92; int barW = panelW - 44, barH = 12;
        g.setColor(Color.RED);   g.fillRect(barX, barY, barW, barH);
        g.setColor(Color.GREEN); g.fillRect(barX, barY, (int)Math.round(barW * (paola.vida / (double)paola.vidaMax)), barH);

        // === NUEVO: barra de recarga habilidad ===
        long now = System.currentTimeMillis();
        double frac = Math.min(1.0, Math.max(0.0, (now - abilityLastUsedMs) / (double) abilityCooldownMs));
        int abY = 114;
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(barX, abY, barW, 10);
        g.setColor(new Color(80,160,255));
        g.fillRect(barX, abY, (int)Math.round(barW * frac), 10);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Consolas", Font.PLAIN, 14));
        String abTxt = "Habilidad ["+ABILITY_KEY_HINT+"] " + (frac >= 1.0 ? "LISTA" : String.format("%.0f%%", frac*100));
        g.drawString(abTxt, barX, abY - 4);

        String rondaTxt = jefeActivo ? ("JEFE FINAL - Ronda " + ronda) : ("Ronda " + ronda);
        g.setFont(new Font("Consolas", Font.BOLD, 24));
        int tw = g.getFontMetrics().stringWidth(rondaTxt);
        g.setColor(new Color(0,0,0,140));
        g.fillRoundRect(getWidth()/2 - (tw/2) - 16, 12, tw + 32, 36, 12, 12);
        g.setColor(Color.WHITE);
        g.drawString(rondaTxt, getWidth()/2 - tw/2, 38);

        // Dibujo entidades
        paola.draw(g);
        enemigos.draw(g);
        if (jefeActivo && jefeFinal != null) jefeFinal.draw(g);

        // === NUEVO: animación de curación sobre el jugador ===
        if (now < healAnimUntilMs) {
            double t = 1.0 - (healAnimUntilMs - now) / 1200.0; // 0..1
            int cx = paola.x + Personajes.SIZE/2;
            int cy = paola.y + Personajes.SIZE/2;
            int r  = (int)(Personajes.SIZE * (0.8 + t*0.8));
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(80, 220, 120, 100));
            g2.fillOval(cx - r/2, cy - r/2, r, r);
            g2.setColor(new Color(120, 255, 160, 180));
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(cx - r/2, cy - r/2, r, r);
            String healTxt = "+" + HEAL_AMOUNT;
            g2.setFont(new Font("Consolas", Font.BOLD, 18));
            int w = g2.getFontMetrics().stringWidth(healTxt);
            g2.setColor(Color.WHITE);
            int floatY = paola.y - 10 - (int)(t*20);
            g2.drawString(healTxt, cx - w/2, floatY);
            g2.dispose();
        }

        if (toastMsg != null && System.currentTimeMillis() < toastUntilMs) {
            String t = toastMsg;
            g.setFont(new Font("Consolas", Font.BOLD, 28));
            int w = g.getFontMetrics().stringWidth(t);
            int x = getWidth()/2 - w/2, y = getHeight()/2 - 140;
            g.setColor(new Color(0,0,0,160));
            g.fillRoundRect(x-16, y-28, w+32, 48, 16, 16);
            g.setColor(Color.YELLOW);
            g.drawString(t, x, y);
        }

        // --- PAUSA: overlay
        if (paused && !gameOver) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(new Color(0,0,0,140));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setFont(new Font("Consolas", Font.BOLD, 48));
            String txt = "PAUSA";
            int w = g2.getFontMetrics().stringWidth(txt);
            g2.setColor(Color.WHITE);
            g2.drawString(txt, getWidth()/2 - w/2, getHeight()/2);
            g2.dispose();
        }

        if (gameOver) {
            g.setFont(new Font("Arial", Font.BOLD, 50));
            int w = g.getFontMetrics().stringWidth("GAME OVER");
            int h = g.getFontMetrics().getAscent();
            int x = getWidth()/2 - w/2, y = getHeight()/2 + h/2;
            g.setColor(new Color(0,0,0,160));
            g.fillRoundRect(x-20, y-h-20, w+40, h+40, 20, 20);
            g.setColor(Color.RED);
            g.drawString("GAME OVER", x, y);
        }
    }

    // Controles
    @Override public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W -> paola.up = true;
            case KeyEvent.VK_S -> paola.down = true;
            case KeyEvent.VK_A -> paola.left = true;
            case KeyEvent.VK_D -> paola.right = true;
            case KeyEvent.VK_SPACE -> paola.disparando = true;
            case KeyEvent.VK_Q -> { // === NUEVO: activar habilidad si está lista
                long now = System.currentTimeMillis();
                if (now - abilityLastUsedMs >= abilityCooldownMs && !gameOver && !paused) {
                    paola.activarHabilidad();
                    abilityLastUsedMs = now;
                    showToast("¡Habilidad!", 800);
                }
            }
            // --- PAUSA: tecla P
            case KeyEvent.VK_P -> togglePause();
            case KeyEvent.VK_R -> { if (gameOver) reiniciarPartida(); }
            case KeyEvent.VK_ESCAPE -> {
                JFrame top = (JFrame) SwingUtilities.getWindowAncestor(this);
                if (top != null) top.dispose();
            }
        }
    }
    @Override public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W -> paola.up = false;
            case KeyEvent.VK_S -> paola.down = false;
            case KeyEvent.VK_A -> paola.left = false;
            case KeyEvent.VK_D -> paola.right = false;
            case KeyEvent.VK_SPACE -> paola.disparando = false;
        }
    }
    @Override public void keyTyped(KeyEvent e) {}

    // --- PAUSA: helpers
    private void togglePause() {
        if (gameOver) return;
        paused = !paused;
        btnPause.setText(paused ? "Reanudar" : "Pausa");
        repaint();
    }
}

package Player;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

/**
 * ArenaBase
 *
 * Lógica base de una arena:
 *  - 3 rondas de enemigos con cantidad fija:
 *      Arena 1: 10, 15, 20
 *      Arena 2: 11, 16, 21
 *      Arena 3: 12, 17, 22
 *      ...
 *  - Después de la ronda 3 aparece el jefe.
 *  - Cada ronda que empieza: +100 HP al jugador (sin pasar de vidaMax).
 *  - Entre niveles: GameFrameNiveles le suma +200 HP y llama a iniciar(vidaInicial).
 *
 *  - Mantiene puntuación interna (no se borra entre rondas ni al cambiar de arena
 *    porque GameFrameNiveles pasa y recupera esa puntuación).
 *
 *  - Cuando se mata al jefe:
 *      llama a levelListener.onLevelComplete(numeroArena, puntuacion, jugador.vida).
 *
 *  - Cuando el jugador muere:
 *      guarda el score con estado GAME_OVER en ArenaScores.csv
 *      y muestra un dialogo con Reintentar / Volver al menú.
 *
 *  - Incluye:
 *      * Botón de Pausa (esquina superior derecha).
 *      * Overlay de PAUSA "pro":
 *            P  - Reanudar
 *            R  - Reiniciar nivel
 *            ESC - Volver al Menú
 */
public abstract class ArenaBase extends JPanel implements ActionListener, KeyListener {

    // ================== Núcleo ==================
    protected final GameSettings settings;
    protected final String heroeElegido;
    protected final String nombreJugador;
    protected final LevelListener levelListener;
    protected final int numeroArena;      // 1..5

    protected Timer timer;
    protected Personajes jugador;
    protected Monstruo enemigos;
    protected JefeFinal jefeFinal;

    protected boolean gameOver = false;
    protected boolean bossActivo = false;

    protected int margen = 40;
    protected Image fondo;

    // Rondas: 3 por nivel
    protected int rondaActual = 1;
    protected final int MAX_RONDAS = 3;

    // Multiplicadores suaves por ronda (vel, etc.)
    protected double roundMulEnemy = 1.0;
    protected double roundMulBoss  = 1.0;

    // Puntuación acumulada en esta partida (no se borra al cambiar de arena)
    protected int puntuacion = 0;

    protected long startNanos;
    protected int touchCooldown = 0;
    protected boolean posInicialAjustada = false;

    // Mensaje tipo "toast" al centro
    protected String toastMsg = null;
    protected long toastUntilMs = 0L;

    // Curación de ronda
    protected long healAnimUntilMs = 0;
    protected static final int HEAL_ROUND_AMOUNT = 100;

    // Habilidad especial tipo jefe
    protected long abilityCooldownMs = 10_000;   // 10s
    protected long abilityLastUsedMs = -100_000; // para que al inicio casi esté lista
    protected static final String ABILITY_KEY_HINT = "Q";

    // Pausa
    protected boolean paused = false;
    protected final JButton btnPause = uiSmallButton("Pausa");

    public ArenaBase(GameSettings settings,
                     String heroeElegido,
                     String nombreJugador,
                     LevelListener levelListener,
                     int numeroArena,
                     String rutaFondo) {

        this.settings = (settings != null) ? settings : GameSettings.fromDificultad("Media");
        this.heroeElegido = (heroeElegido != null && !heroeElegido.isBlank()) ? heroeElegido : "Default";
        this.nombreJugador = (nombreJugador == null || nombreJugador.isBlank()) ? "Invitado" : nombreJugador;
        this.levelListener = levelListener;
        this.numeroArena = numeroArena;

        setFocusable(true);
        addKeyListener(this);

        // Fondo
        try {
            this.fondo = new ImageIcon(getClass().getResource(rutaFondo)).getImage();
        } catch (Exception ignore) {
            this.fondo = null;
        }

        // Jugador y enemigos
        jugador = new Personajes(300, 300, this.settings);
        jugador.setHeroeSprite(this.heroeElegido);

        enemigos = new Monstruo(this.settings);

        // Botón de pausa
        setLayout(null);
        btnPause.setFocusable(false);
        btnPause.addActionListener(e -> togglePause());
        add(btnPause);

        // Timer (se arranca en iniciar())
        timer = new Timer(16, this);
    }

    // El frame llama a esto ANTES de jugar la arena.
    // vidaInicial:
    //   -1  => vida completa
    //   >0  => se clamp a vidaMax (llega con +200 de GameFrameNiveles)
    public void iniciar(int vidaInicial) {
        gameOver = false;
        bossActivo = false;
        paused = false;
        posInicialAjustada = false;
        rondaActual = 1;
        roundMulEnemy = 1.0;
        roundMulBoss = 1.0;
        touchCooldown = 0;
        toastMsg = null;
        toastUntilMs = 0L;

        // Vida del jugador
        jugador.vida = jugador.vidaMax;
        if (vidaInicial > 0) {
            jugador.vida = Math.min(jugador.vidaMax, vidaInicial);
        }

        // Limites iniciales (se actualizan bien al primer repaint cuando ya tenga tamaño)
        enemigos.setLimitesPantalla(getWidth(), getHeight(), margen);
        enemigos.enemigos.clear();

        // Habilidad y curación
        healAnimUntilMs = 0;
        abilityLastUsedMs = System.currentTimeMillis() - (long)(abilityCooldownMs * 0.8);

        // Spawn ronda 1
        spawnEnemigosRonda(rondaActual);

        // Ventaja de velocidad
        syncPlayerSpeed();

        // Tiempo
        startNanos = System.nanoTime();

        showToast("Arena " + numeroArena + " - Ronda 1", 1500);

        timer.start();
    }

    // El frame (GameFrameNiveles) puede meterle puntuación acumulada.
    public void setPuntuacion(int p) { this.puntuacion = p; }
    public int getPuntuacion() { return puntuacion; }

    @Override
    public void addNotify() {
        super.addNotify();
        requestFocusInWindow();
    }

    // Jugador siempre más rápido que enemigos (2x y al menos +5)
    protected void syncPlayerSpeed() {
        double enemySpeedNow = Monstruo.SPEED_BASE * settings.enemySpeedMul * roundMulEnemy;
        double desiredByFactor = enemySpeedNow * settings.playerSpeedAdvantage; // típicamente 2.0
        int minPlusFive = (int) Math.ceil(enemySpeedNow) + 5;
        int playerTarget = Math.max((int) Math.round(desiredByFactor), minPlusFive);
        jugador.setVelocidad(playerTarget);
    }

    // Botón de pausa en la esquina
    @Override
    public void doLayout() {
        super.doLayout();
        int w = getWidth();
        btnPause.setBounds(Math.max(12, w - 110), 12, 96, 36);
    }

    // ================== Lógica principal ==================
    @Override
    public void actionPerformed(ActionEvent e) {
        Proyectil.setViewportGlobal(getWidth(), getHeight(), margen);

        if (!posInicialAjustada && getWidth() > 0 && getHeight() > 0) {
            jugador.x = getWidth() / 2 - Personajes.SIZE / 2;
            jugador.y = getHeight() / 2 - Personajes.SIZE / 2;
            posInicialAjustada = true;
        }

        jugador.setLimitesPantalla(getWidth(), getHeight(), margen);
        enemigos.setLimitesPantalla(getWidth(), getHeight(), margen);
        if (bossActivo && jefeFinal != null) {
            jefeFinal.setLimitesPantalla(getWidth(), getHeight(), margen);
        }

        if (paused) {
            repaint();
            return;
        }

        syncPlayerSpeed();

        if (!gameOver) {
            // Actualización jugador y enemigos
            jugador.update();
            enemigos.update(jugador);

            // Player -> Enemigos (balas)
            for (Monstruo.Enemigo en : new ArrayList<>(enemigos.enemigos)) {
                for (Proyectil p : new ArrayList<>(jugador.proyectiles)) {
                    if (p.getBounds().intersects(en.getBounds())) {
                        en.recibirDaño(p.daño);
                        p.activo = false;
                        if (en.vida <= 0) puntuacion += 100;
                    }
                }
                if (en.vida <= 0) enemigos.enemigos.remove(en);
            }

            // Enemigos -> Player (disparos)
            for (Monstruo.Enemigo en : enemigos.enemigos) {
                for (Proyectil d : new ArrayList<>(en.disparos)) {
                    if (d.getBounds().intersects(jugador.getBounds())) {
                        jugador.recibirDaño(d.daño);
                        d.activo = false;
                    }
                }
            }

            // Contacto cuerpo-a-cuerpo (enemigos sin disparo)
            if (touchCooldown > 0) touchCooldown--;
            for (Monstruo.Enemigo en : enemigos.enemigos) {
                if (!en.puedeDisparar && en.getBounds().intersects(jugador.getBounds()) && touchCooldown == 0) {
                    jugador.recibirDaño(Math.max(6, (int) Math.round(en.daño * 0.8)));
                    touchCooldown = 15;
                }
            }

            // Gestión de rondas / jefe
            if (enemigos.enemigos.isEmpty() && !bossActivo) {
                if (rondaActual < MAX_RONDAS) {
                    // Siguiente ronda de enemigos
                    rondaActual++;
                    roundMulEnemy *= 1.10;
                    spawnEnemigosRonda(rondaActual);
                } else if (jefeFinal == null) {
                    // Jefe de la arena
                    spawnBoss();
                }
            }

            // Jefe activo
            if (bossActivo && jefeFinal != null) {
                jefeFinal.update(jugador);

                // Player -> jefe
                for (Proyectil p : new ArrayList<>(jugador.proyectiles)) {
                    if (p.getBounds().intersects(jefeFinal.getBounds())) {
                        jefeFinal.recibirDaño(p.daño);
                        p.activo = false;
                        if (jefeFinal.vida <= 0) puntuacion += 1000;
                    }
                }

                // Jefe -> player
                for (Proyectil d : new ArrayList<>(jefeFinal.disparos)) {
                    if (d.getBounds().intersects(jugador.getBounds())) {
                        jugador.recibirDaño(d.daño);
                        d.activo = false;
                    }
                }

                if (jefeFinal.vida <= 0) {
                    bossActivo = false;
                    jefeFinal = null;

                    // Notificamos al controlador de niveles (GameFrameNiveles)
                    if (levelListener != null) {
                        timer.stop();
                        levelListener.onLevelComplete(numeroArena, puntuacion, jugador.vida);
                        return; // ya no seguimos pintando esta arena
                    }
                }
            }

            // ¿Muerte?
            if (jugador.vida <= 0) {
                gameOver = true;
            }
        }

        repaint();

        if (gameOver && timer != null && timer.isRunning()) {
            timer.stop();
            saveScoreGameOver();
            showGameOverDialog();
        }
    }

    // ================== Spawns y jefe ==================

    // Cantidad de enemigos por ronda según arena (10,15,20) + offset por arena.
    protected int getEnemigosForRonda(int ronda) {
        int base = 10 + (numeroArena - 1); // Arena1:10, Arena2:11, etc.
        return switch (ronda) {
            case 1 -> base;
            case 2 -> base + 5;
            case 3 -> base + 10;
            default -> base;
        };
    }

    protected void spawnEnemigosRonda(int ronda) {
        int cantidad = getEnemigosForRonda(ronda);
        enemigos.setExtraSpeedMul(roundMulEnemy);
        enemigos.spawnRonda(cantidad);

        // Curación de ronda (+100 HP, respeta vidaMax)
        int before = jugador.vida;
        jugador.vida = Math.min(jugador.vidaMax, jugador.vida + HEAL_ROUND_AMOUNT);
        if (jugador.vida > before) {
            healAnimUntilMs = System.currentTimeMillis() + 1200;
            showToast("+100 HP", 900);
        }

        syncPlayerSpeed();
        showToast("Ronda " + ronda + " / " + MAX_RONDAS, 1000);
    }

    // Puede ser sobreescrito por cada arena si quieres jefes distintos.
    protected JefeFinal crearJefe() {
        int x = getWidth() / 2 - JefeFinal.SIZE / 2;
        int y = margen + 20;
        return new JefeFinal(x, y, settings);
    }

    protected void spawnBoss() {
        jefeFinal = crearJefe();
        if (jefeFinal != null) {
            jefeFinal.setExtraSpeedMul(roundMulBoss);
            jefeFinal.setLimitesPantalla(getWidth(), getHeight(), margen);
            bossActivo = true;
            showToast("¡JEFE DE LA ARENA " + numeroArena + "!", 1500);
        }
    }

    // ================== Guardado de score al morir ==================

    protected void saveScoreGameOver() {
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

    protected static String fmtTime(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) return String.format("%dh %02dm %02ds", h, m, s);
        return String.format("%02dm %02ds", m, s);
    }

    // ================== Game Over (dialogo) ==================

    protected void showGameOverDialog() {
        long elapsedSec = Math.max(0, (System.nanoTime() - startNanos) / 1_000_000_000L);

        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Fin de partida", true);
        d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, new Color(10, 10, 10),
                        0, getHeight(), new Color(35, 35, 35)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("GAME OVER", SwingConstants.CENTER);
        title.setFont(new Font("Consolas", Font.BOLD, 36));
        title.setForeground(Color.RED);
        root.add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(3, 1, 8, 8));
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

        retry.addActionListener(e -> { d.dispose(); reiniciarArena(); });
        back.addActionListener(e -> {
            d.dispose();
            volverAlMenuPrincipal();
        });

        d.setContentPane(root);
        d.setSize(560, 320);
        d.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        d.setVisible(true);
    }

    protected void volverAlMenuPrincipal() {
        try {
            Class<?> cls = Class.forName("Menu.MenuPrincipal");
            Object obj = cls.getConstructor().newInstance();
            if (obj instanceof JFrame jf) jf.setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                    "No se encontró el Menú Principal.\nSe cerrará la ventana.",
                    "Aviso",
                    JOptionPane.INFORMATION_MESSAGE);
        }
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        if (topFrame != null) topFrame.dispose();
    }

    protected void reiniciarArena() {
        gameOver = false;
        bossActivo = false;
        rondaActual = 1;
        roundMulEnemy = 1.0;
        roundMulBoss = 1.0;
        toastMsg = null;
        toastUntilMs = 0L;
        healAnimUntilMs = 0;
        abilityLastUsedMs = System.currentTimeMillis() - (long)(abilityCooldownMs * 0.8);

        jugador = new Personajes(getWidth() / 2 - Personajes.SIZE / 2,
                getHeight() / 2 - Personajes.SIZE / 2,
                settings);
        jugador.setHeroeSprite(heroeElegido);

        enemigos = new Monstruo(settings);
        enemigos.setExtraSpeedMul(roundMulEnemy);
        enemigos.setLimitesPantalla(getWidth(), getHeight(), margen);
        enemigos.enemigos.clear();
        spawnEnemigosRonda(rondaActual);

        syncPlayerSpeed();
        jefeFinal = null;

        startNanos = System.nanoTime();
        showToast("Arena " + numeroArena + " - Ronda 1", 1500);
        timer.start();
    }

    // ================== Pintado ==================
    protected void showToast(String msg, int ms) {
        toastMsg = msg;
        toastUntilMs = System.currentTimeMillis() + ms;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Fondo
        if (fondo != null) {
            g.drawImage(fondo, 0, 0, getWidth(), getHeight(), this);
        } else {
            g.setColor(Color.DARK_GRAY);
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        // Panel info arriba izquierda
        int panelW = 300, panelH = 134;
        g.setColor(new Color(0, 0, 0, 140));
        g.fillRoundRect(12, 12, panelW, panelH, 12, 12);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Consolas", Font.BOLD, 16));
        g.drawString("Jugador: " + nombreJugador, 22, 38);
        g.drawString("Puntuación: " + puntuacion, 22, 62);

        g.drawString("Vida: " + jugador.vida + " / " + jugador.vidaMax, 22, 86);
        int barX = 22, barY = 92; int barW = panelW - 44, barH = 12;
        g.setColor(Color.RED);   g.fillRect(barX, barY, barW, barH);
        g.setColor(Color.GREEN);
        g.fillRect(barX, barY,
                (int) Math.round(barW * (jugador.vida / (double) jugador.vidaMax)), barH);

        // Barra de habilidad
        long now = System.currentTimeMillis();
        double frac = Math.min(1.0, Math.max(0.0, (now - abilityLastUsedMs) / (double) abilityCooldownMs));
        int abY = 114;
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(barX, abY, barW, 10);
        g.setColor(new Color(80, 160, 255));
        g.fillRect(barX, abY, (int) Math.round(barW * frac), 10);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Consolas", Font.PLAIN, 14));
        String abTxt = "Habilidad [" + ABILITY_KEY_HINT + "] " + (frac >= 1.0 ? "LISTA" : String.format("%.0f%%", frac * 100));
        g.drawString(abTxt, barX, abY - 4);

        // Texto de ronda/arena arriba centro
        String rondaTxt = bossActivo
                ? ("Arena " + numeroArena + " - JEFE")
                : ("Arena " + numeroArena + " - Ronda " + rondaActual + "/" + MAX_RONDAS);
        g.setFont(new Font("Consolas", Font.BOLD, 24));
        int tw = g.getFontMetrics().stringWidth(rondaTxt);
        g.setColor(new Color(0, 0, 0, 140));
        g.fillRoundRect(getWidth() / 2 - (tw / 2) - 16, 12, tw + 32, 36, 12, 12);
        g.setColor(Color.WHITE);
        g.drawString(rondaTxt, getWidth() / 2 - tw / 2, 38);

        // Entidades
        jugador.draw(g);
        enemigos.draw(g);
        if (bossActivo && jefeFinal != null) jefeFinal.draw(g);

        // Animación curación
        if (now < healAnimUntilMs) {
            double t = 1.0 - (healAnimUntilMs - now) / 1200.0;
            int cx = jugador.x + Personajes.SIZE / 2;
            int cy = jugador.y + Personajes.SIZE / 2;
            int r = (int) (Personajes.SIZE * (0.8 + t * 0.8));
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(80, 220, 120, 100));
            g2.fillOval(cx - r / 2, cy - r / 2, r, r);
            g2.setColor(new Color(120, 255, 160, 180));
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(cx - r / 2, cy - r / 2, r, r);
            String healTxt = "+" + HEAL_ROUND_AMOUNT;
            g2.setFont(new Font("Consolas", Font.BOLD, 18));
            int w = g2.getFontMetrics().stringWidth(healTxt);
            g2.setColor(Color.WHITE);
            int floatY = jugador.y - 10 - (int) (t * 20);
            g2.drawString(healTxt, cx - w / 2, floatY);
            g2.dispose();
        }

        // Toast
        if (toastMsg != null && System.currentTimeMillis() < toastUntilMs) {
            String t = toastMsg;
            g.setFont(new Font("Consolas", Font.BOLD, 28));
            int w = g.getFontMetrics().stringWidth(t);
            int x = getWidth() / 2 - w / 2, y = getHeight() / 2 - 140;
            g.setColor(new Color(0, 0, 0, 160));
            g.fillRoundRect(x - 16, y - 28, w + 32, 48, 16, 16);
            g.setColor(Color.YELLOW);
            g.drawString(t, x, y);
        }

        // PAUSA: overlay pro
        if (paused && !gameOver) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Fondo oscurecido
            g2.setColor(new Color(0, 0, 0, 190));
            g2.fillRect(0, 0, getWidth(), getHeight());

            int cardW = Math.min(520, getWidth() - 80);
            int cardH = 220;
            int cardX = (getWidth() - cardW) / 2;
            int cardY = (getHeight() - cardH) / 2;

            g2.setColor(new Color(15, 15, 25, 230));
            g2.fillRoundRect(cardX, cardY, cardW, cardH, 26, 26);
            g2.setStroke(new BasicStroke(3f));
            g2.setColor(new Color(255, 215, 0, 220));
            g2.drawRoundRect(cardX + 1, cardY + 1, cardW - 2, cardH - 2, 26, 26);

            String txt = "PAUSA";
            g2.setFont(new Font("Consolas", Font.BOLD, 40));
            int w = g2.getFontMetrics().stringWidth(txt);
            int tx = getWidth() / 2 - w / 2;
            int ty = cardY + 70;
            g2.setColor(new Color(255, 240, 200));
            g2.drawString(txt, tx, ty);

            g2.setFont(new Font("Consolas", Font.PLAIN, 20));
            int lineY = ty + 40;
            g2.setColor(Color.WHITE);
            g2.drawString("P   - Reanudar", cardX + 40, lineY);
            g2.drawString("R   - Reiniciar nivel", cardX + 40, lineY + 28);
            g2.drawString("ESC - Volver al Menú", cardX + 40, lineY + 56);

            g2.dispose();
        }

        // Overlay GAME OVER simple (el dialogo aparece aparte)
        if (gameOver) {
            g.setFont(new Font("Arial", Font.BOLD, 50));
            int w = g.getFontMetrics().stringWidth("GAME OVER");
            int h = g.getFontMetrics().getAscent();
            int x = getWidth() / 2 - w / 2, y = getHeight() / 2 + h / 2;
            g.setColor(new Color(0, 0, 0, 160));
            g.fillRoundRect(x - 20, y - h - 20, w + 40, h + 40, 20, 20);
            g.setColor(Color.RED);
            g.drawString("GAME OVER", x, y);
        }
    }

    // ================== Controles ==================
    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W -> jugador.up = true;
            case KeyEvent.VK_S -> jugador.down = true;
            case KeyEvent.VK_A -> jugador.left = true;
            case KeyEvent.VK_D -> jugador.right = true;
            case KeyEvent.VK_SPACE -> jugador.disparando = true;
            case KeyEvent.VK_Q -> { // habilidad
                long now = System.currentTimeMillis();
                if (now - abilityLastUsedMs >= abilityCooldownMs && !gameOver && !paused) {
                    jugador.activarHabilidad();
                    abilityLastUsedMs = now;
                    showToast("¡Habilidad!", 800);
                }
            }
            case KeyEvent.VK_P -> togglePause();
            case KeyEvent.VK_R -> {
                if (gameOver || paused) {
                    reiniciarArena();
                }
            }
            case KeyEvent.VK_ESCAPE -> {
                // ESC: si está en pausa o incluso en medio del juego, salir al menú
                volverAlMenuPrincipal();
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W -> jugador.up = false;
            case KeyEvent.VK_S -> jugador.down = false;
            case KeyEvent.VK_A -> jugador.left = false;
            case KeyEvent.VK_D -> jugador.right = false;
            case KeyEvent.VK_SPACE -> jugador.disparando = false;
        }
    }

    @Override public void keyTyped(KeyEvent e) {}

    // ================== Pausa ==================
    protected void togglePause() {
        if (gameOver) return;
        paused = !paused;
        btnPause.setText(paused ? "Reanudar" : "Pausa");
        repaint();
    }

    // ================== Helpers UI ==================
    protected static JButton uiButton(String txt) {
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

    protected static JButton uiSmallButton(String txt) {
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
}

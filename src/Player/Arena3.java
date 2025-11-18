package Player;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Arena3 - Zona de los Ojos
 *
 * - 3 rondas + jefe Gran Ojote Robot.
 * - Fondo de ojos animado.
 * - Enemigos especiales:
 *      * Ojo caminante (rush cerca).
 *      * Rojos shooters (bolas de fuego).
 *      * Azules con rayo láser.
 *
 * - Integra con ArenaBase / GameFrameNiveles:
 *      * Usa jugador, puntuacion, timer, etc. de ArenaBase.
 *      * +100 HP por ronda (HEAL_ROUND_AMOUNT).
 *      * Al matar al jefe llama levelListener.onLevelComplete(3, puntuacion, jugador.vida).
 */
public class Arena3 extends ArenaBase {

    // ====== Rutas de assets de la Zona 3 ======
    private static final String R_FONDO    = "/Imagenes/Zona3/ARENA3S.png";
    private static final String R_OJOTE    = "/Imagenes/Zona3/ojote_robot.gif";
    private static final String R_LASER    = "/Imagenes/Zona3/laser_purpura.gif";
    private static final String R_AZUL     = "/Imagenes/Zona3/enemigo_azul.gif";
    private static final String R_OJO_WALK = "/Imagenes/Zona3/ojo_caminante.gif";
    private static final String R_OJO_HIT  = "/Imagenes/Zona3/ojo_ataque.gif";
    private static final String R_ROJO_G   = "/Imagenes/Zona3/rojo_grande.gif";
    private static final String R_ROJO_P   = "/Imagenes/Zona3/rojo_pequeno.gif";
    private static final String R_FIREBALL = "/Imagenes/Zona3/proyectil_fuego.gif";

    // Fondo de ojos (tile con alpha)
    private Image fondoEyes;
    private float fondoAlpha = 0.9f;

    // Enemigos y láseres especiales de esta arena
    private final List<Enemigo3> enemigosZ3 = new ArrayList<>();
    private final List<Laser> lasers = new ArrayList<>();

    // Jefe de la arena 3
    private BossOjote bossOjote;

    // Cooldown global para daño cuerpo a cuerpo (para que no pegue cada frame)
    private int meleeCooldown = 0; // en ticks de timer (~16ms c/u)

    // ----------------- Constructor -----------------
    public Arena3(GameSettings settings,
                  String heroe,
                  String nombreJugador,
                  LevelListener listener) {
        // fondo base = fondo de ojos
        super(settings, heroe, nombreJugador, listener, 3, R_FONDO);

        try {
            fondoEyes = new ImageIcon(getClass().getResource(R_FONDO)).getImage();
        } catch (Exception ignore) {
            fondoEyes = null;
        }
    }

    // ----------------- Inicio / Reinicio de arena -----------------

    @Override
    public void iniciar(int vidaInicial) {
        gameOver = false;
        bossActivo = false;
        paused = false;
        posInicialAjustada = false;
        rondaActual = 1;
        roundMulEnemy = 1.0;
        roundMulBoss  = 1.0;
        touchCooldown = 0;
        toastMsg = null;
        toastUntilMs = 0L;
        meleeCooldown = 0;

        // Vida del jugador
        jugador.vida = jugador.vidaMax;
        if (vidaInicial > 0) {
            jugador.vida = Math.min(jugador.vidaMax, vidaInicial);
        }

        enemigos.setLimitesPantalla(getWidth(), getHeight(), margen);
        enemigos.enemigos.clear();

        // Limpiamos estructuras de la zona 3
        enemigosZ3.clear();
        lasers.clear();
        bossOjote = null;

        // Habilidad / curación visual
        healAnimUntilMs = 0;
        abilityLastUsedMs = System.currentTimeMillis() - (long) (abilityCooldownMs * 0.8);

        // Primera ronda de la zona 3
        spawnRondaZona3(rondaActual);

        // Ventaja de velocidad
        syncPlayerSpeed();

        // Tiempo de esta arena (para game over local)
        startNanos = System.nanoTime();

        showToast("Arena 3 - Ronda 1", 1500);

        timer.start();
    }

    @Override
    protected void reiniciarArena() {
        // 🔁 En vez de reiniciar la misma arena, volvemos a Arena1
        volverAArena1();
    }

    // ----------------- Spawns de la Zona 3 -----------------

    private void spawnRondaZona3(int mult) {
        enemigosZ3.clear();
        lasers.clear();

        int w = Math.max(getWidth(), 1200);
        int h = Math.max(getHeight(), 700);

        // Ojo caminante (varios)
        for (int i = 0; i < 2 * mult + 2; i++) {
            Point p = pickSpawn(w, h);
            enemigosZ3.add(new OjoCaminante(p.x, p.y, R_OJO_WALK, R_OJO_HIT));
        }
        // Rojos pequeños
        for (int i = 0; i < 2 * mult + 2; i++) {
            Point p = pickSpawn(w, h);
            enemigosZ3.add(new RojoShooter(p.x, p.y, R_ROJO_P, 30, 4, 2, 55));
        }
        // Rojos grandes
        for (int i = 0; i < 1 * mult + 1; i++) {
            Point p = pickSpawn(w, h);
            enemigosZ3.add(new RojoShooter(p.x, p.y, R_ROJO_G, 55, 3, 3, 75));
        }
        // Azules
        for (int i = 0; i < 1 * mult + 1; i++) {
            Point p = pickSpawn(w, h);
            enemigosZ3.add(new AzulLaser(p.x, p.y, R_AZUL, 40, 3, 1600, 2300)); // cd min/max
        }

        // Curación +100 HP por ronda
        int before = jugador.vida;
        jugador.vida = Math.min(jugador.vidaMax, jugador.vida + HEAL_ROUND_AMOUNT);
        if (jugador.vida > before) {
            healAnimUntilMs = System.currentTimeMillis() + 1200;
            showToast("+100 HP", 900);
        }

        syncPlayerSpeed();
        showToast("Ronda " + rondaActual + " / " + MAX_RONDAS, 1000);
    }

    private Point pickSpawn(int w, int h) {
        int m = Math.max(40, margen);
        int[] xs = { m + 60, w / 2 - 60, w - m - 140, m + 60, w - m - 140, w / 2 - 60, m + 60, w - m - 140 };
        int[] ys = { m + 60, m + 60, m + 60, h / 2 - 60, h / 2 - 60, h - m - 140, h - m - 140, h / 2 - 60 };
        int i = (int) (Math.random() * xs.length);
        return new Point(xs[i], ys[i]);
    }

    private void activarJefe() {
        bossActivo = true;
        bossOjote = new BossOjote(R_OJOTE, R_LASER, getWidth(), getHeight(), margen);
        showToast("¡Jefe: Gran Ojote Robot!", 2200);
    }

    // ----------------- Lógica principal (override) -----------------

    @Override
    public void actionPerformed(ActionEvent e) {
        Proyectil.setViewportGlobal(getWidth(), getHeight(), margen);

        if (!posInicialAjustada && getWidth() > 0 && getHeight() > 0) {
            jugador.x = getWidth() / 2 - Personajes.SIZE / 2;
            jugador.y = getHeight() / 2 - Personajes.SIZE / 2;
            posInicialAjustada = true;
        }

        jugador.setLimitesPantalla(getWidth(), getHeight(), margen);

        if (paused) {
            repaint();
            return;
        }

        syncPlayerSpeed();

        if (!gameOver) {
            // Jugador
            jugador.update();

            // Cooldown de cuerpo a cuerpo global
            if (meleeCooldown > 0) meleeCooldown--;

            // Enemigos de la zona 3
            for (int i = 0; i < enemigosZ3.size(); i++) {
                Enemigo3 en = enemigosZ3.get(i);
                en.update(jugador, lasers);

                // choque cuerpo a cuerpo con cooldown
                if (en.getBounds().intersects(jugador.getBounds())) {
                    if (meleeCooldown == 0) {
                        jugador.recibirDaño(en.contactDamage());
                        meleeCooldown = 18; // ~18 ticks * 16ms ≈ 0.3s entre golpes
                    }
                }

                if (en.muerto()) {
                    enemigosZ3.remove(i--);
                    puntuacion += 120; // más puntos que los normales
                }
            }

            // Láseres activos (solo pegan una vez)
            for (int i = 0; i < lasers.size(); i++) {
                Laser l = lasers.get(i);
                l.update();

                if (l.activo && !l.haGolpeado && l.getBounds().intersects(jugador.getBounds())) {
                    jugador.recibirDaño(l.daño);
                    l.haGolpeado = true; // ya pegó, no sigue quitando vida cada frame
                }

                if (!l.activo) {
                    lasers.remove(i--);
                }
            }

            // Proyectiles del jugador → enemigos + jefe
            for (int i = 0; i < jugador.proyectiles.size(); i++) {
                Proyectil p = jugador.proyectiles.get(i);
                if (!p.activo) continue;
                Rectangle pb = p.getBounds();

                // Primero, contra enemigos
                boolean hit = false;
                for (int j = 0; j < enemigosZ3.size(); j++) {
                    Enemigo3 en = enemigosZ3.get(j);
                    if (pb.intersects(en.getBounds())) {
                        en.daño(p.daño);
                        p.activo = false;
                        hit = true;
                        if (en.muerto()) {
                            enemigosZ3.remove(j--);
                            puntuacion += 120;
                        }
                        break;
                    }
                }

                // Si no golpeó enemigo, revisar boss
                if (!hit && bossActivo && bossOjote != null && pb.intersects(bossOjote.getBounds())) {
                    bossOjote.daño(p.daño);
                    p.activo = false;
                    if (bossOjote.muerto()) {
                        bossActivo = false;
                        bossOjote = null;
                        puntuacion += 1500;

                        // Notificar al controlador de niveles
                        if (levelListener != null) {
                            timer.stop();
                            levelListener.onLevelComplete(numeroArena, puntuacion, jugador.vida);
                            return;
                        }
                    }
                }
            }

            // Rondas y jefe
            if (!bossActivo && enemigosZ3.isEmpty()) {
                if (rondaActual < MAX_RONDAS) {
                    rondaActual++;
                    roundMulEnemy *= 1.10;
                    spawnRondaZona3(rondaActual);
                } else {
                    activarJefe();
                }
            }

            // Actualizar jefe
            if (bossActivo && bossOjote != null) {
                bossOjote.update(jugador, lasers, enemigosZ3);
            }

            // ¿Muerte del jugador?
            if (jugador.vida <= 0) {
                gameOver = true;
                // 🔁 En vez de solo GAME OVER, regresamos a Arena1
                volverAArena1();
                return;
            }
        }

        repaint();
    }

    /**
     * 🔁 Vuelve automáticamente al sistema de niveles empezando en Arena1.
     *    - Resetea puntuación.
     *    - Cierra el frame actual.
     *    - Crea un nuevo GameFrameNiveles.
     */
    private void volverAArena1() {
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }

        // Reiniciar puntuación SOLO para esta arena
        this.puntuacion = 0;

        JOptionPane.showMessageDialog(
                this,
                "Has sido derrotado en la Arena 3...\nVolviendo al inicio (Arena 1).",
                "Derrota",
                JOptionPane.INFORMATION_MESSAGE
        );

        // Cerrar la ventana actual y abrir un nuevo flujo de niveles
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w instanceof JFrame jf) {
            jf.dispose();
        }

        GameFrameNiveles nuevo = new GameFrameNiveles(settings, heroeElegido, nombreJugador);
        nuevo.setVisible(true);
    }

    // ----------------- Dibujo (override) -----------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // ===== Fondo: ojos tileados con alpha =====
        if (fondoEyes != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fondoAlpha));
            int iw = fondoEyes.getWidth(null);
            int ih = fondoEyes.getHeight(null);
            if (iw > 0 && ih > 0) {
                for (int y = 0; y < getHeight(); y += ih) {
                    for (int x = 0; x < getWidth(); x += iw) {
                        g2.drawImage(fondoEyes, x, y, null);
                    }
                }
            } else {
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            g2.dispose();
        } else {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        // ===== HUD arriba izquierda =====
        int panelW = 300, panelH = 134;
        g.setColor(new Color(0, 0, 0, 140));
        g.fillRoundRect(12, 12, panelW, panelH, 12, 12);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Consolas", Font.BOLD, 16));
        g.drawString("Jugador: " + nombreJugador, 22, 38);
        g.drawString("Puntuación: " + puntuacion, 22, 62);

        g.drawString("Vida: " + jugador.vida + " / " + jugador.vidaMax, 22, 86);
        int barX = 22, barY = 92;
        int barW = panelW - 44, barH = 12;
        g.setColor(Color.RED);
        g.fillRect(barX, barY, barW, barH);
        g.setColor(Color.GREEN);
        g.fillRect(barX, barY,
                (int) Math.round(barW * (jugador.vida / (double) jugador.vidaMax)), barH);

        // Barra de habilidad
        long now = System.currentTimeMillis();
        double frac = Math.min(1.0, Math.max(0.0,
                (now - abilityLastUsedMs) / (double) abilityCooldownMs));
        int abY = 114;
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(barX, abY, barW, 10);
        g.setColor(new Color(80, 160, 255));
        g.fillRect(barX, abY, (int) Math.round(barW * frac), 10);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Consolas", Font.PLAIN, 14));
        String abTxt = "Habilidad [" + ABILITY_KEY_HINT + "] " +
                (frac >= 1.0 ? "LISTA" : String.format("%.0f%%", frac * 100));
        g.drawString(abTxt, barX, abY - 4);

        // Texto Arena/Ronda arriba centro
        String rondaTxt = bossActivo
                ? "Arena 3 - JEFE: Gran Ojote"
                : "Arena 3 - Ronda " + rondaActual + "/" + MAX_RONDAS;
        g.setFont(new Font("Consolas", Font.BOLD, 24));
        int tw = g.getFontMetrics().stringWidth(rondaTxt);
        g.setColor(new Color(0, 0, 0, 140));
        g.fillRoundRect(getWidth() / 2 - (tw / 2) - 16, 12, tw + 32, 36, 12, 12);
        g.setColor(Color.WHITE);
        g.drawString(rondaTxt, getWidth() / 2 - tw / 2, 38);

        // ===== Entidades =====
        jugador.draw(g);

        for (Enemigo3 en : enemigosZ3) en.draw(g);
        for (Laser l : lasers) l.draw(g);

        if (bossActivo && bossOjote != null) bossOjote.draw(g);

        // ===== Animación de curación =====
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

        // Overlay de PAUSA bonito
        if (paused && !gameOver) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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

        // Overlay GAME OVER simple (el diálogo real lo manejamos nosotros)
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

    // =====================================================================
    //  Enemigos y proyectiles especiales de la Zona 3
    // =====================================================================

    /** Base de enemigo de la Zona 3 (daño muy suave, poca vida). */
    private abstract static class Enemigo3 {
        int x, y;
        int w = 56, h = 56;
        int vida = 40;   // ANTES 50 -> ahora menos vida
        int velocidad = 3;
        int daño = 5;    // contacto MUY bajo
        Image sprite;

        Enemigo3(int x, int y, String ruta) {
            this.x = x;
            this.y = y;
            try {
                sprite = new ImageIcon(Enemigo3.class.getResource(ruta)).getImage();
            } catch (Exception ignore) {}
        }

        Rectangle getBounds() { return new Rectangle(x, y, w, h); }
        void daño(int d) { vida = Math.max(0, vida - Math.max(0, d)); }
        boolean muerto() { return vida <= 0; }
        int contactDamage() { return daño; }

        abstract void update(Personajes p, List<Laser> lasers);

        void draw(Graphics g) {
            if (sprite != null) {
                g.drawImage(sprite, x, y, w, h, null);
            }
            // barra de vida (ajustada al nuevo máximo aproximado 35)
            g.setColor(Color.red);
            g.fillRect(x + (w - 40) / 2, y - 8, 40, 6);
            g.setColor(Color.green);
            double maxRef = 35.0; // referencia para barra
            g.fillRect(x + (w - 40) / 2, y - 8,
                    (int) Math.round(40 * (vida / maxRef)), 6);
        }
    }

    /** Enemigo azul que dispara un rayo láser. */
    private class AzulLaser extends Enemigo3 {
        int cdMin, cdMax;
        long nextShot = System.currentTimeMillis() + 1000;

        AzulLaser(int x, int y, String r, int vida, int vel, int cdMinMs, int cdMaxMs) {
            super(x, y, r);
            this.vida = vida;   // 40
            this.velocidad = vel;
            this.cdMin = cdMinMs;
            this.cdMax = cdMaxMs;
        }

        @Override
        void update(Personajes p, List<Laser> lasers) {
            double dx = p.x - x, dy = p.y - y;
            double dist = Math.max(1, Math.hypot(dx, dy));
            x += (int) Math.round(velocidad * dx / dist);
            y += (int) Math.round(velocidad * dy / dist);

            if (System.currentTimeMillis() >= nextShot) {
                int vx = (int) Math.signum(dx);
                int vy = (int) Math.signum(dy);
                if (vx == 0 && vy == 0) vx = -1;

                lasers.add(Laser.fromSprite(
                        x + w / 2,
                        y + h / 2,
                        vx, vy,
                        R_LASER,
                        900,
                        3   // daño muy moderado
                ));
                nextShot = System.currentTimeMillis()
                        + (cdMin + (int) (Math.random() * (cdMax - cdMin)));
            }
        }
    }

    /** Rojo que dispara bolas de fuego. */
    private class RojoShooter extends Enemigo3 {
        int disparoCd = 0;
        int disparoEvery = 50;
        int projSize = 28;
        Image fireball;
        final List<Proyectil> proyectilesEnemigos = new ArrayList<>();

        RojoShooter(int x, int y, String r, int vida, int vel, int dmgContacto, int cd) {
            super(x, y, r);
            this.vida = vida;      // 30 o 55 según el caso
            this.velocidad = vel;
            // contacto aún más bajo:
            this.daño = 2;
            this.disparoEvery = cd;
            try {
                fireball = new ImageIcon(getClass().getResource(R_FIREBALL)).getImage();
            } catch (Exception ignore) {}
        }

        @Override
        void update(Personajes p, List<Laser> lasers) {
            double dx = p.x - x, dy = p.y - y;
            double dist = Math.max(1, Math.hypot(dx, dy));
            x += (int) Math.round(velocidad * dx / dist);
            y += (int) Math.round(velocidad * dy / dist);

            if (disparoCd > 0) disparoCd--;
            else {
                int vx = (int) Math.signum(dx);
                int vy = (int) Math.signum(dy);
                if (vx == 0 && vy == 0) vy = 1;
                Proyectil pr = new Proyectil(x + w / 2, y + h / 2, vx, vy);
                pr.daño = 3;  // bola de fuego suave
                pr.conTamaño(projSize)
                        .setSprite(fireball)
                        .setRotateWithDirection(true);
                proyectilesEnemigos.add(pr);
                disparoCd = disparoEvery;
            }

            // Actualizar proyectiles propios y colisión con el jugador
            for (int i = 0; i < proyectilesEnemigos.size(); i++) {
                Proyectil pr = proyectilesEnemigos.get(i);
                pr.update();
                if (!pr.activo) {
                    proyectilesEnemigos.remove(i--);
                    continue;
                }
                if (pr.getBounds().intersects(jugador.getBounds())) {
                    jugador.recibirDaño(pr.daño);
                    pr.activo = false;
                }
            }
        }

        @Override
        void draw(Graphics g) {
            super.draw(g);
            for (Proyectil p : proyectilesEnemigos) {
                p.draw(g);
            }
        }
    }

    /** Ojo caminante que acelera y “ataca” cerca del jugador. */
    private class OjoCaminante extends Enemigo3 {
        Image walk, hit;
        boolean atacando = false;
        int tick = 0;

        OjoCaminante(int x, int y, String rWalk, String rHit) {
            super(x, y, rWalk);
            try {
                walk = new ImageIcon(getClass().getResource(rWalk)).getImage();
            } catch (Exception ignore) {}
            try {
                hit = new ImageIcon(getClass().getResource(rHit)).getImage();
            } catch (Exception ignore) {}
        }

        @Override
        void update(Personajes p, List<Laser> lasers) {
            double dx = p.x - x, dy = p.y - y;
            double dist = Math.max(1, Math.hypot(dx, dy));
            atacando = dist < 70;
            if (!atacando) {
                x += (int) Math.round(velocidad * dx / dist);
                y += (int) Math.round(velocidad * dy / dist);
            } else {
                if ((tick++ % 20) == 0) {
                    x += (int) Math.round((velocidad + 3) * dx / dist);
                    y += (int) Math.round((velocidad + 3) * dy / dist);
                    // el daño cuerpo a cuerpo ahora se maneja arriba,
                    // en actionPerformed, con el cooldown global meleeCooldown
                }
            }
        }

        @Override
        void draw(Graphics g) {
            Image img = atacando && (hit != null) ? hit : (walk != null ? walk : sprite);
            g.drawImage(img, x, y, w, h, null);
            super.draw(g);
        }
    }

    // ======= Láser simple (sprite estirado, solo pega 1 vez) =======
    private static class Laser {
        int x, y, vx, vy;
        int len = 800;
        int grosor = 36;
        int daño = 3;               // daño base MUY moderado
        long until;
        boolean activo = true;
        boolean haGolpeado = false; // para que solo pegue 1 vez
        Image sprite;

        static Laser fromSprite(int x, int y, int vx, int vy,
                                String ruta, int ms, int daño) {
            Laser l = new Laser();
            l.x = x;
            l.y = y;
            l.vx = vx;
            l.vy = vy;
            l.until = System.currentTimeMillis() + ms;
            l.daño = daño;
            try {
                l.sprite = new ImageIcon(Laser.class.getResource(ruta)).getImage();
            } catch (Exception ignore) {}
            return l;
        }

        void update() {
            if (System.currentTimeMillis() >= until) activo = false;
        }

        Rectangle getBounds() {
            if (vx != 0) {
                return new Rectangle(
                        Math.min(x, x + vx * len),
                        y - grosor / 2,
                        Math.abs(vx * len),
                        grosor
                );
            } else {
                return new Rectangle(
                        x - grosor / 2,
                        Math.min(y, y + vy * len),
                        grosor,
                        Math.abs(vy * len)
                );
            }
        }

        void draw(Graphics g) {
            Rectangle r = getBounds();
            if (sprite == null) {
                g.setColor(new Color(200, 150, 255, 180));
                g.fillRoundRect(r.x, r.y, r.width, r.height, 16, 16);
            } else {
                g.drawImage(sprite, r.x, r.y, r.width, r.height, null);
            }
        }
    }

    // ======= Jefe pegado a la pared derecha =======
    private class BossOjote {
        int w = 180, h = 180;
        int x, y;
        int vida = 350;   // ANTES 500 -> ahora menos tanque

        int ancho, alto, margenLocal;
        Image sprite;
        Image laserSprite;

        long nextLaser = System.currentTimeMillis() + 1500;
        long nextSummon = System.currentTimeMillis() + 2500;

        BossOjote(String rutaBoss, String rutaLaser, int ancho, int alto, int margen) {
            this.ancho = ancho;
            this.alto = alto;
            this.margenLocal = margen;
            x = ancho - margen - w;
            y = alto / 2 - h / 2;
            try {
                sprite = new ImageIcon(getClass().getResource(rutaBoss)).getImage();
            } catch (Exception ignore) {}
            try {
                laserSprite = new ImageIcon(getClass().getResource(rutaLaser)).getImage();
            } catch (Exception ignore) {}
        }

        void update(Personajes p, List<Laser> lasers, List<Enemigo3> pool) {
            // movimiento vertical suave (sigue al jugador)
            int targetY = Math.max(margenLocal + 40,
                    Math.min(alto - margenLocal - h - 40, p.y - h / 2));
            y += (int) Math.signum(targetY - y) * 4;

            // disparo láser grande a la izquierda
            if (System.currentTimeMillis() >= nextLaser) {
                Laser l = Laser.fromSprite(
                        x - 10,
                        y + h / 2,
                        -1, 0,
                        R_LASER,
                        1200,
                        4   // un poco más fuerte, pero ya súper nerfeado
                );
                l.grosor = 56;
                lasers.add(l);
                nextLaser = System.currentTimeMillis() + 2300;
            }

            // invocación de minions
            if (System.currentTimeMillis() >= nextSummon) {
                Point pSpawn = new Point(x - 140, y + h / 2);
                double r = Math.random();
                if (r < 0.33) {
                    pool.add(new OjoCaminante(pSpawn.x, pSpawn.y, R_OJO_WALK, R_OJO_HIT));
                } else if (r < 0.66) {
                    pool.add(new RojoShooter(pSpawn.x, pSpawn.y, R_ROJO_P, 30, 4, 2, 65));
                } else {
                    pool.add(new AzulLaser(pSpawn.x, pSpawn.y, R_AZUL, 40, 3, 1500, 2300));
                }
                nextSummon = System.currentTimeMillis() + 2600;
            }
        }

        Rectangle getBounds() {
            return new Rectangle(x, y, w, h);
        }

        void daño(int d) {
            vida = Math.max(0, vida - Math.max(0, d));
        }

        boolean muerto() {
            return vida <= 0;
        }

        void draw(Graphics g) {
            if (sprite != null) {
                g.drawImage(sprite, x, y, w, h, null);
            }
            // barra de vida
            g.setColor(Color.red);
            g.fillRect(x, y - 12, w, 8);
            g.setColor(Color.green);
            g.fillRect(x, y - 12, (int) Math.round(w * (vida / 350.0)), 8);
        }
    }
}

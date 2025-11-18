package Player;

import javax.swing.*;
import java.awt.*;
import java.util.Random;

public class JefeFinalDragon extends JefeFinal {

    private static final int DRAGON_SIZE = 200;

    private static final long TELEPORT_MIN_MS = 6000;
    private static final long TELEPORT_MAX_MS = 8000;

    private final GameSettings cfg;
    private ImageIcon dragonSprite;

    private long nextSpawnMs = 0L;
    private long nextTeleportMs = 0L;
    private long blackoutUntilMs = 0L;

    private boolean onRightSide = true;
    private boolean facingLeftSprite = true;
    private double facingBlend = 1.0;

    private int viewW = 0, viewH = 0, viewMargin = 0;

    public JefeFinalDragon(int x, int y, GameSettings settings) {
        super(x, y, (settings != null ? settings : GameSettings.fromDificultad("Media")));
        this.cfg = (settings != null ? settings : GameSettings.fromDificultad("Media"));

        try {
            java.net.URL u = getClass().getResource("/Imagenes/Zona5/Dragon.png");
            dragonSprite = (u != null) ? new ImageIcon(u) : new ImageIcon();
        } catch (Exception e) {
            dragonSprite = new ImageIcon();
        }

        this.vida = 500;

        long now = System.currentTimeMillis();
        nextTeleportMs = now + TELEPORT_MIN_MS
                + (long) (Math.random() * (TELEPORT_MAX_MS - TELEPORT_MIN_MS));
    }

    @Override
    public void setLimitesPantalla(int w, int h, int margen) {
        super.setLimitesPantalla(w, h, margen);
        this.viewW = w;
        this.viewH = h;
        this.viewMargin = Math.max(0, margen);
    }

    /**
     * 🔥 Invoca enemigos propios de Arena5 (EnemigosArena5),
     * sin usar esqueletos.
     */
    public void intentarSpawnearEnemigos(Monstruo monstruo, int rondaArena) {
        if (monstruo == null) return;

        long now = System.currentTimeMillis();
        if (now < nextSpawnMs) return;

        nextSpawnMs = now + 10000; // cada 10s
        Random rnd = new Random();

        int w = (viewW > 0) ? viewW : 900;
        int h = (viewH > 0) ? viewH : 600;
        int margenLocal = (viewMargin > 0) ? viewMargin : 60;

        double enemySpeedNow =
                Monstruo.SPEED_BASE * cfg.enemySpeedMul * (1.0 + 0.1 * (rondaArena - 1));

        int cantidad = 3 + rnd.nextInt(2);
        for (int i = 0; i < cantidad; i++) {
            int tipo = (rnd.nextBoolean()) ? 1 : 2;
            int vida = 35 + 10 * rondaArena;
            int vel = Math.max(2, (int) Math.round(enemySpeedNow * (0.9 + rnd.nextDouble() * 0.4)));
            int dmg = 8 + 4 * rondaArena;

            int x = margenLocal + rnd.nextInt(Math.max(1, w - 2 * margenLocal));
            int y = margenLocal + rnd.nextInt(Math.max(1, h - 2 * margenLocal));

            monstruo.enemigos.add(new EnemigosArena5(x, y, tipo, vida, vel, dmg));
        }
    }

    // =================== BLOQUEA LA INVOCACIÓN BASE ===================

    protected void invocarEsqueletos() {
        // El dragón no invoca esqueletos, solo enemigos propios
    }

    // =================== MOVIMIENTO / UPDATE PERSONALIZADO ===================
    @Override
    public void update(Personajes jugador) {
        long now = System.currentTimeMillis();

        // Teletransporte cada cierto tiempo
        if (now >= nextTeleportMs && viewW > 0 && viewH > 0) {
            teleport();
            nextTeleportMs = now + TELEPORT_MIN_MS
                    + (long) (Math.random() * (TELEPORT_MAX_MS - TELEPORT_MIN_MS));
        }

        // Movimiento leve o animación tipo flotante
        double dx = jugador.x - x;
        double dy = jugador.y - y;
        double dist = Math.hypot(dx, dy);

        if (dist > 1) {
            int step = 2;
            x += (int) Math.round(dx / dist * step);
            y += (int) Math.round(dy / dist * step);
        }

        facingLeftSprite = jugador.x < (x + SIZE / 2);

        // Disparos del jefe
        super.disparos.forEach(Proyectil::update);

        // Sin esqueletos ni invocación del método base
    }

    private void teleport() {
        if (viewW <= 0 || viewH <= 0) return;

        long now = System.currentTimeMillis();
        blackoutUntilMs = now + 10;

        onRightSide = !onRightSide;
        int newX;

        if (onRightSide) {
            newX = Math.max(viewMargin, viewW - viewMargin - DRAGON_SIZE);
            facingBlend = -1.0;
        } else {
            newX = viewMargin;
            facingBlend = 1.0;
        }

        int minY = viewMargin;
        int maxY = Math.max(minY, viewH - viewMargin - DRAGON_SIZE);
        int newY = minY + (int) (Math.random() * (maxY - minY));

        this.x = newX;
        this.y = newY;
    }

    @Override
    public void draw(Graphics g) {
        long now = System.currentTimeMillis();

        if (now < blackoutUntilMs) {
            Rectangle clip = g.getClipBounds();
            g.setColor(Color.BLACK);
            if (clip != null)
                g.fillRect(clip.x, clip.y, clip.width, clip.height);
            else
                g.fillRect(0, 0, 2000, 2000);
            return;
        }

        if (dragonSprite == null) {
            super.draw(g);
            return;
        }

        Image img = dragonSprite.getImage();
        int w = DRAGON_SIZE, h = DRAGON_SIZE;

        if (facingLeftSprite)
            g.drawImage(img, x, y, w, h, null);
        else
            g.drawImage(img, x + w, y, -w, h, null);

        int barW = 120, barH = 8;
        int barX = x, barY = y - 10;
        g.setColor(Color.RED);
        g.fillRect(barX, barY, barW, barH);
        g.setColor(Color.GREEN);
        g.fillRect(barX, barY, (int) Math.round(barW * (vida / 500.0)), barH);

        for (Proyectil d : disparos)
            d.draw(g);
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(x, y, DRAGON_SIZE, DRAGON_SIZE);
    }
}

package Player;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class JefeFinal {

    public static final int SIZE = 128;

    public int x, y;
    public int vida = 300;

    private final GameSettings settings;
    private double extraSpeedMul = 1.0;

    private int ancho = 0, alto = 0, margen = 0;

    private ImageIcon sprite;

    public final List<Proyectil> disparos = new ArrayList<>();
    private int shootCooldown = 0;
    private int burst = 0;

    private boolean facingLeft = false;

    // ----------- ANIMACIÓN DE ENTRADA SIN IMÁGENES -----------
    private boolean introActiva = true;
    private long introStart = System.currentTimeMillis();
    private static final long INTRO_DURACION = 2000;
    // ---------------------------------------------------------

    // Invocación de esqueletos
    private static final long SUMMON_INTERVAL_MS = 5000;
    private long lastSummon = System.currentTimeMillis();

    // Ruta del esqueleto
    private static final String RUTA_ESQUELETO = "/Imagenes/Esqueletito.png";

    public JefeFinal(int x, int y, GameSettings settings) {
        this.x = x;
        this.y = y;
        this.settings = settings != null ? settings : GameSettings.fromDificultad("Media");

        java.net.URL u = getClass().getResource("/Imagenes/JefeDif.gif");
        sprite = (u != null) ? new ImageIcon(u) : new ImageIcon();
    }

    public void setExtraSpeedMul(double mul) {
        this.extraSpeedMul = Math.max(0.1, mul);
    }

    public void setLimitesPantalla(int w, int h, int margen) {
        this.ancho = w;
        this.alto = h;
        this.margen = Math.max(0, margen);
    }

    private int speedNow() {
        double v = 2.0 * settings.bossSpeedMul * extraSpeedMul;
        return Math.max(1, (int) Math.round(v));
    }

    // --------------------------------------------------------------
    // UPDATE
    // --------------------------------------------------------------
    public void update(Personajes jugador) {

        long now = System.currentTimeMillis();

        // ANIMACIÓN DE ENTRADA (NO SE MUEVE)
        if (introActiva) {
            if (now - introStart >= INTRO_DURACION) {
                introActiva = false;
            }
            return;
        }

        // movimiento
        double dx = jugador.x - x;
        double dy = jugador.y - y;
        double dist = Math.hypot(dx, dy);

        if (dist > 1) {
            int step = speedNow();
            x += (int) Math.round(dx / dist * step);
            y += (int) Math.round(dy / dist * step);
        }

        facingLeft = jugador.x < (x + SIZE / 2);

        // invocación cada 5s
        if (now - lastSummon >= SUMMON_INTERVAL_MS) {
            invocarEsqueletos();
            lastSummon = now;
        }

        // disparos
        if (shootCooldown > 0) shootCooldown--;
        else disparar(dx, dy);

        // actualizar balas
        for (int i = 0; i < disparos.size(); i++) {
            Proyectil p = disparos.get(i);
            p.update();
            if (!p.activo) {
                disparos.remove(i);
                i--;
            }
        }
    }

    // --------------------------------------------------------------
    // INVOCAR ESQUELETOS
    // --------------------------------------------------------------
    private void invocarEsqueletos() {
        if (Monstruo.CURRENT == null) return;

        Monstruo m = Monstruo.CURRENT;

        for (int i = 0; i < 2; i++) {

            int offsetX = (i == 0 ? -Monstruo.ENEMY_SIZE : Monstruo.ENEMY_SIZE);
            int spawnX = x + SIZE / 2 + offsetX;
            int spawnY = y + SIZE;

            int vida = (int) (60 * settings.enemyHealthMul);
            int vel = Math.max(2, (int) (Monstruo.SPEED_BASE * settings.enemySpeedMul));
            int dmg = (int) (12 * settings.enemyDamageMul);

            m.enemigos.add(new Monstruo.Enemigo(
                    spawnX,
                    spawnY,
                    RUTA_ESQUELETO,
                    vida,
                    vel,
                    dmg,
                    false
            ));
        }
    }

    // --------------------------------------------------------------
    // DISPARAR
    // --------------------------------------------------------------
    private void disparar(double dx, double dy) {

        int cx = x + SIZE / 2;
        int cy = y + SIZE / 2;

        if (burst % 3 == 2) {

            int n = 10;
            for (int i = 0; i < n; i++) {

                double ang = (Math.PI * 2) * i / n;
                int vx = (int) Math.cos(ang);
                int vy = (int) Math.sin(ang);

                Proyectil p = new Proyectil(cx, cy, vx, vy);
                p.velocidad = 10;
                p.daño = settings.bossBulletDamage;

                p.setSprite(Proyectil.RUTA_PROY_ENEMY)
                        .setRotateWithDirection(true)
                        .conTamaño(9);

                disparos.add(p);
            }

        } else {

            int vx = (int) Math.round(dx / Math.max(1, Math.hypot(dx, dy)));
            int vy = (int) Math.round(dy / Math.max(1, Math.hypot(dx, dy)));

            Proyectil p = new Proyectil(cx, cy, vx, vy);
            p.velocidad = 12;
            p.daño = settings.bossBulletDamage;

            p.setSprite(Proyectil.RUTA_PROY_ENEMY)
                    .setRotateWithDirection(true)
                    .conTamaño(9);

            disparos.add(p);
        }

        burst++;
        shootCooldown = 30;
    }

    // --------------------------------------------------------------
    // DRAW
    // --------------------------------------------------------------
    public void draw(Graphics g) {

        long now = System.currentTimeMillis();

        // ------------------ ANIMACIÓN SIN IMÁGENES ------------------
        if (introActiva) {

            long t = now - introStart;
            double p = t / (double) INTRO_DURACION; // 0.0 a 1.0

            int cx = x + SIZE/2;
            int cy = y + SIZE/2;

            Graphics2D g2 = (Graphics2D) g.create();

            // Fondo oscuro radial
            float alpha = (float) (1.0 - p);
            g2.setColor(new Color(0, 0, 0, (int)(alpha * 150)));
            g2.fillOval(cx - 120, cy - 120, 240, 240);

            // círculo brillante que crece
            int r = (int)(20 + p * 90);
            g2.setColor(new Color(255, 255, 255, 180));
            g2.setStroke(new BasicStroke(4));
            g2.drawOval(cx - r, cy - r, r*2, r*2);

            // chispas
            g2.setColor(Color.WHITE);
            for (int i = 0; i < 20; i++) {
                double ang = Math.random() * Math.PI * 2;
                int rx = cx + (int)(Math.cos(ang) * r);
                int ry = cy + (int)(Math.sin(ang) * r);
                g2.fillOval(rx, ry, 4, 4);
            }

            g2.dispose();
            return;
        }
        // --------------------------------------------------------------

        Image img = sprite.getImage();

        if (!facingLeft)
            g.drawImage(img, x + SIZE, y, -SIZE, SIZE, null);
        else
            g.drawImage(img, x, y, SIZE, SIZE, null);

        // barra de vida
        g.setColor(Color.RED);
        g.fillRect(x, y - 10, 100, 8);
        g.setColor(Color.GREEN);
        g.fillRect(x, y - 10, (int) (100 * (vida / 300.0)), 8);

        // proyectiles
        for (Proyectil d : disparos) d.draw(g);
    }

    public void recibirDaño(int dmg) {
        vida = Math.max(0, vida - Math.max(0, dmg));
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, SIZE, SIZE);
    }
}

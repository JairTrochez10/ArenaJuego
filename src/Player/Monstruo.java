package Player;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Monstruo
 *
 * Maneja:
 * - lista de enemigos
 * - spawn de ronda (cantidad fija total)
 * - movimiento, disparos y colisiones entre enemigos
 */
public class Monstruo {

    public ArrayList<Enemigo> enemigos = new ArrayList<>();

    private int anchoPantalla = 0, altoPantalla = 0, margen = 0;

    public static int ENEMY_SIZE = 56;
    public static final int SPEED_BASE = 3;

    private final GameSettings settings;
    private double extraSpeedMul = 1.0;

    private final java.util.Random random = new java.util.Random();

    public Monstruo(GameSettings settings) {
        this.settings = settings;
    }

    public void setExtraSpeedMul(double mul) {
        this.extraSpeedMul = Math.max(0.5, mul);
    }

    public void setLimitesPantalla(int ancho, int alto, int margen) {
        this.anchoPantalla = ancho;
        this.altoPantalla = alto;
        this.margen = Math.max(0, margen);
    }

    // Puntos donde pueden aparecer enemigos
    private List<Point> spawnPoints() {
        int w = Math.max(400, anchoPantalla);
        int h = Math.max(300, altoPantalla);
        int m = Math.max(40, margen);
        int cx = w / 2;
        int cy = h / 2;
        return List.of(
                new Point(m + 40, m + 40),
                new Point(w - m - 120, m + 40),
                new Point(m + 40, h - m - 120),
                new Point(w - m - 120, h - m - 120),
                new Point(cx - 60, m + 40),
                new Point(cx - 60, h - m - 120),
                new Point(m + 40, cy - 60),
                new Point(w - m - 120, cy - 60)
        );
    }

    /**
     * Genera EXACTAMENTE cantidadTotal enemigos mezclando 5 tipos.
     */
    public void spawnRonda(int cantidadTotal) {
        enemigos.clear();

        List<Point> spawns = spawnPoints();
        if (spawns.isEmpty()) {
            spawns = List.of(new Point(100, 100));
        }
        int s = spawns.size();

        for (int i = 0; i < cantidadTotal; i++) {
            Point base = spawns.get(i % s);
            int jitterX = (i % 3) * 14;
            int jitterY = (i % 2) * 12;
            int spawnX = base.x + jitterX;
            int spawnY = base.y + jitterY;

            int tipo = random.nextInt(5);

            String rutaSprite;
            int vidaBase;
            int dmgBase;
            int velBase = SPEED_BASE;
            boolean puedeDisparar;

            switch (tipo) {
                case 0 -> {
                    rutaSprite = "/Imagenes/Mush.gif";
                    vidaBase = 50;
                    dmgBase = 10;
                    puedeDisparar = false;
                }
                case 1 -> {
                    rutaSprite = "/Imagenes/Slime.gif";
                    vidaBase = 60;
                    dmgBase = 10;
                    puedeDisparar = false;
                }
                case 2 -> {
                    rutaSprite = "/Imagenes/Bat.gif";
                    vidaBase = 40;
                    dmgBase = 8;
                    puedeDisparar = true;
                }
                case 3 -> {
                    rutaSprite = "/Imagenes/Icebat.gif";
                    vidaBase = 50;
                    dmgBase = 8;
                    puedeDisparar = true;
                }
                default -> {
                    rutaSprite = "/Imagenes/Evil Cube.gif";
                    vidaBase = 80;
                    dmgBase = 12;
                    puedeDisparar = false;
                }
            }

            int vidaS = (int) Math.round(vidaBase * settings.enemyHealthMul);
            int velS  = Math.max(1, (int) Math.round(velBase * settings.enemySpeedMul * extraSpeedMul));
            int dmgS  = Math.max(1, (int) Math.round(dmgBase * settings.enemyDamageMul));

            enemigos.add(new Enemigo(spawnX, spawnY, rutaSprite, vidaS, velS, dmgS, puedeDisparar));
        }
    }

    public void update(Personajes p) {
        for (Enemigo e : enemigos) e.update(p);

        for (int i = 0; i < enemigos.size(); i++) {
            Enemigo e1 = enemigos.get(i);

            for (int j = i + 1; j < enemigos.size(); j++) {
                Enemigo e2 = enemigos.get(j);
                if (e1.getBounds().intersects(e2.getBounds())) {
                    int dx = e2.x - e1.x, dy = e2.y - e1.y;
                    if (dx == 0 && dy == 0) dx = 1;
                    double dist = Math.max(1, Math.hypot(dx, dy));
                    e1.x -= (int) (dx / dist * 2);
                    e1.y -= (int) (dy / dist * 2);
                    e2.x += (int) (dx / dist * 2);
                    e2.y += (int) (dy / dist * 2);
                }
            }

            if (anchoPantalla > 0 && altoPantalla > 0) {
                int minX = margen, minY = margen;
                int maxX = Math.max(minX, anchoPantalla - margen - ENEMY_SIZE);
                int maxY = Math.max(minY, altoPantalla - margen - ENEMY_SIZE);
                e1.x = Math.max(minX, Math.min(e1.x, maxX));
                e1.y = Math.max(minY, Math.min(e1.y, maxY));
            }

            if (!e1.disparos.isEmpty() && anchoPantalla > 0 && altoPantalla > 0) {
                for (int k = 0; k < e1.disparos.size(); k++) {
                    Proyectil d = e1.disparos.get(k);
                    Rectangle r = d.getBounds();
                    if (r.x + r.width < margen ||
                            r.x > anchoPantalla - margen ||
                            r.y + r.height < margen ||
                            r.y > altoPantalla - margen ||
                            !d.activo) {
                        e1.disparos.remove(k--);
                    }
                }
            }
        }
    }

    public void draw(Graphics g) {
        for (Enemigo e : enemigos) e.draw(g);
    }

    public static class Enemigo {
        public int x, y, velocidad, vida, vidaMax, daño;
        public boolean puedeDisparar;
        public ImageIcon sprite;
        public ArrayList<Proyectil> disparos = new ArrayList<>();
        private int disparoCooldown = 0;

        public Enemigo(int x, int y, String ruta, int vida, int vel, int dmg, boolean puedeDisparar) {
            this.x = x;
            this.y = y;
            this.vida = vida;
            this.vidaMax = Math.max(1, vida);
            this.velocidad = vel;
            this.daño = dmg;
            this.puedeDisparar = puedeDisparar;
            this.sprite = new ImageIcon(getClass().getResource(ruta));
        }

        public void update(Personajes p) {
            double dx = p.x - x, dy = p.y - y;
            double dist = Math.max(1, Math.hypot(dx, dy));
            x += (int) Math.round(velocidad * dx / dist);
            y += (int) Math.round(velocidad * dy / dist);

            if (disparoCooldown > 0) disparoCooldown--;
            else if (puedeDisparar) {
                int vx = (int) Math.signum(dx);
                int vy = (int) Math.signum(dy);
                if (vx == 0 && vy == 0) vy = 1;

                Proyectil pj = new Proyectil(x + ENEMY_SIZE / 2, y + ENEMY_SIZE / 2, vx, vy);
                pj.daño = daño;

                pj.setSprite(Proyectil.RUTA_PROY_ENEMY)
                        .setRotateWithDirection(true)
                        .setFacingOffsetDegrees(0)
                        .conTamaño(7);

                disparos.add(pj);
                disparoCooldown = 60;
            }

            for (int i = 0; i < disparos.size(); i++) {
                Proyectil d = disparos.get(i);
                d.update();
                if (!d.activo) {
                    disparos.remove(i);
                    i--;
                }
            }
        }

        public void draw(Graphics g) {
            g.drawImage(sprite.getImage(), x, y, ENEMY_SIZE, ENEMY_SIZE, null);
            int barW = 40, barH = 6, ox = (ENEMY_SIZE - barW) / 2;
            g.setColor(Color.RED);
            g.fillRect(x + ox, y - 8, barW, barH);
            g.setColor(Color.GREEN);
            g.fillRect(x + ox, y - 8, (int) Math.round(barW * (vida / (double) vidaMax)), barH);

            for (Proyectil d : disparos) d.draw(g);
        }

        public void recibirDaño(int dmg) {
            vida = Math.max(0, vida - dmg);
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, ENEMY_SIZE, ENEMY_SIZE);
        }
    }
}

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

    /** Tu Juego llama este ctor (x,y,settings). */
    public JefeFinal(int x, int y, GameSettings settings) {
        this.x = x; this.y = y;
        this.settings = (settings != null ? settings : GameSettings.fromDificultad("Media"));
        java.net.URL u = getClass().getResource("/Imagenes/jefesinfondo.gif");
        sprite = (u != null) ? new ImageIcon(u) : new ImageIcon();
    }

    public void setExtraSpeedMul(double mul) { this.extraSpeedMul = Math.max(0.1, mul); }
    public void setLimitesPantalla(int w, int h, int margen) { this.ancho = w; this.alto = h; this.margen = Math.max(0, margen); }

    private int speedNow() {
        double v = 2.0 * settings.bossSpeedMul * extraSpeedMul;
        return (int)Math.max(1, Math.round(v));
    }

    public void update(Personajes jugador) {
        // moverse hacia el jugador
        double dx = jugador.x - x, dy = jugador.y - y;
        double dist = Math.hypot(dx, dy);
        if (dist > 1) {
            x += (int) Math.round((dx / dist) * speedNow());
            y += (int) Math.round((dy / dist) * speedNow());
        }

        // disparo: dirigido + ráfaga cada 3 disparos
        if (shootCooldown > 0) shootCooldown--;
        else {
            int cx = x + SIZE/2, cy = y + SIZE/2;
            if (burst % 3 == 2) {
                int n = 10;
                for (int i = 0; i < n; i++) {
                    double ang = (Math.PI * 2.0) * i / n;
                    int vx = (int) Math.round(Math.cos(ang));
                    int vy = (int) Math.round(Math.sin(ang));
                    if (vx == 0 && vy == 0) vy = 1;
                    Proyectil p = new Proyectil(cx, cy, vx, vy);
                    p.velocidad = 10; p.daño = settings.bossBulletDamage;
                    disparos.add(p);
                }
            } else {
                int vx = (int) Math.round(dx / Math.max(1.0, dist));
                int vy = (int) Math.round(dy / Math.max(1.0, dist));
                if (vx == 0 && vy == 0) vy = 1;
                Proyectil p = new Proyectil(cx, cy, vx, vy);
                p.velocidad = 12; p.daño = settings.bossBulletDamage;
                disparos.add(p);
            }
            burst++;
            shootCooldown = 30; // ~0.5 s
        }

        for (int i = 0; i < disparos.size(); i++) {
            Proyectil d = disparos.get(i);
            d.update();
            if (!d.activo) { disparos.remove(i); i--; }
        }

        // clamp (si está configurado)
        if (ancho > 0 && alto > 0) {
            x = Math.max(margen, Math.min(x, ancho - margen - SIZE));
            y = Math.max(margen, Math.min(y, alto - margen - SIZE));
        }
    }

    public void draw(Graphics g) {
        g.drawImage(sprite.getImage(), x, y, SIZE, SIZE, null);
        int barW = 100, barH = 8;
        int barX = x, barY = y - 10;
        g.setColor(Color.RED);   g.fillRect(barX, barY, barW, barH);
        g.setColor(Color.GREEN); g.fillRect(barX, barY, (int)Math.round(barW * (vida / 300.0)), barH);
        for (Proyectil d : disparos) d.draw(g);
    }

    public void recibirDaño(int dmg) { vida = Math.max(0, vida - Math.max(0, dmg)); }
    public Rectangle getBounds() { return new Rectangle(x, y, SIZE, SIZE); }
}

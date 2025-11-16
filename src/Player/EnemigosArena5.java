package Player;

import javax.swing.*;
import java.awt.*;

public class EnemigosArena5 extends Monstruo.Enemigo {

    private final ImageIcon correr;
    private final ImageIcon descansar;
    private final ImageIcon atacar;

    private enum Estado { CORRER, DESCANSAR, ATACAR }
    private Estado estado = Estado.DESCANSAR;

    private static final long COOLDOWN_MS = 1500;
    private long lastAttackMs = -2000;
    private boolean facingLeft = true;

    public EnemigosArena5(int x, int y, int tipo, int vida, int vel, int dmg) {
        // ruta dummy, no se usa realmente porque dibujamos con nuestros iconos
        super(x, y, "/Imagenes/Mush.gif", vida, vel, dmg, false);

        if (tipo == 1) {
            correr    = load("/Imagenes/Zona5/Video-Project-1.gif");
            descansar = load("/Imagenes/Zona5/Video-Project-2.gif");
            atacar    = load("/Imagenes/Zona5/Video-Project-3.gif");
        } else {
            correr    = load("/Imagenes/Zona5/Enemie-2-unscreen-Run.gif");
            descansar = load("/Imagenes/Zona5/Enemie-2-unscreen.gif");
            atacar    = load("/Imagenes/Zona5/Enemie-2-unscreen-Attack.gif");
        }
    }

    private ImageIcon load(String ruta) {
        try {
            java.net.URL u = getClass().getResource(ruta);
            if (u != null) return new ImageIcon(u);
        } catch (Exception ignore) {}
        return null;
    }

    public boolean puedeAtacarAhora() {
        return System.currentTimeMillis() - lastAttackMs >= COOLDOWN_MS;
    }

    public void registrarAtaque() {
        lastAttackMs = System.currentTimeMillis();
    }

    @Override
    public void update(Personajes p) {
        long now = System.currentTimeMillis();
        boolean enCooldown = (now - lastAttackMs) < COOLDOWN_MS;

        double dx = p.x - x;
        double dy = p.y - y;
        double dist = Math.max(1, Math.hypot(dx, dy));

        if (enCooldown) {
            estado = Estado.DESCANSAR;
            return;
        }

        if (dist > 70) {
            estado = Estado.CORRER;

            if (dx < -1)      facingLeft = true;
            else if (dx > 1)  facingLeft = false;

            x += (int)Math.round(velocidad * dx / dist);
            y += (int)Math.round(velocidad * dy / dist);
        } else {
            estado = Estado.ATACAR;
        }
    }

    @Override
    public void draw(Graphics g) {
        ImageIcon icon;
        switch (estado) {
            case ATACAR -> icon = atacar != null ? atacar : correr;
            case CORRER -> icon = correr != null ? correr : descansar;
            default     -> icon = descansar != null ? descansar : correr;
        }

        int base = Monstruo.ENEMY_SIZE;
        int size = (int)Math.round(base * 2.5); // más grandes que los normales

        if (icon != null) {
            Image img = icon.getImage();
            if (facingLeft) {
                g.drawImage(img, x + size, y, -size, size, null);
            } else {
                g.drawImage(img, x, y, size, size, null);
            }
        }

        int barW = 40, barH = 6, ox = (size - barW) / 2;
        g.setColor(Color.RED);
        g.fillRect(x + ox, y - 8, barW, barH);
        g.setColor(Color.GREEN);
        g.fillRect(x + ox, y - 8,
                (int)Math.round(barW * (vida / (double)vidaMax)), barH);
    }

    @Override
    public Rectangle getBounds() {
        int base = Monstruo.ENEMY_SIZE;
        int size = (int)Math.round(base * 2.5);
        return new Rectangle(x, y, size, size);
    }
}

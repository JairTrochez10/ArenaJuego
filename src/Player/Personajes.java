package Player;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Personajes {

    public static int SIZE = 56;

    public int x, y;
    public int velocidad;
    public int vida, vidaMax;

    public boolean up, down, left, right, disparando;

    public final List<Proyectil> proyectiles = new ArrayList<>();
    private int disparoCooldown = 0;

    private ImageIcon sprite = new ImageIcon(getClass().getResource("/Imagenes/Kevin walk.gif"));
    private String heroName = "Zorritas";

    private int minX = 0, minY = 0, maxX = 5000, maxY = 5000;

    private int direccion = 1;

    // ============================
    // CONSTRUCTOR
    // ============================
    public Personajes(int x, int y, GameSettings settings) {
        this.x = x;
        this.y = y;

        this.vidaMax = 500;
        this.vida = vidaMax;
        this.velocidad = 6;
    }

    // ===========================================================
    // Necesitado por ArenaBase
    // ===========================================================
    public void setVelocidad(int v) {
        this.velocidad = Math.max(1, v);
    }

    public void setLimitesPantalla(int ancho, int alto, int margen) {
        minX = margen;
        minY = margen;
        maxX = ancho - margen - SIZE;
        maxY = alto - margen - SIZE;
    }

    // ===========================================================
    // CAMBIO DE SPRITE DEL HÉROE
    // ===========================================================
    public void setHeroeSprite(String heroeNombre) {

        if (heroeNombre == null) return;
        String base = heroeNombre.trim();
        heroName = base;

        String[] preferidas = null;

        if (base.equalsIgnoreCase("Zorritas")) {
            preferidas = new String[]{
                    "/Menu/imagen/Heroes/Kevin walk.gif",
                    "/Imagenes/Kevin walk.gif"
            };
        } else if (base.equalsIgnoreCase("Larry")) {
            preferidas = new String[]{
                    "/Menu/imagen/Heroes/Squeletron.gif",
                    "/Imagenes/Squeletron.gif"
            };
        }

        if (preferidas != null) {
            for (String r : preferidas) {
                java.net.URL u = getClass().getResource(r);
                if (u != null) {
                    sprite = new ImageIcon(u);
                    return;
                }
            }
        }

        sprite = new ImageIcon(getClass().getResource("/Imagenes/Kevin walk.gif"));
    }

    // ===========================================================
    // UPDATE
    // ===========================================================
    public void update() {

        int vx = 0, vy = 0;
        if (up) vy -= 1;
        if (down) vy += 1;
        if (left) vx -= 1;
        if (right) vx += 1;

        if (vx != 0 || vy != 0) {
            if (Math.abs(vx) > Math.abs(vy)) direccion = (vx < 0) ? 3 : 4;
            else direccion = (vy < 0) ? 2 : 1;
        }

        x += vx * velocidad;
        y += vy * velocidad;

        x = Math.max(minX, Math.min(x, maxX));
        y = Math.max(minY, Math.min(y, maxY));

        if (disparando && disparoCooldown == 0) {
            disparar();
            disparoCooldown = 6;
        }
        if (disparoCooldown > 0) disparoCooldown--;

        for (int i = 0; i < proyectiles.size(); i++) {
            Proyectil p = proyectiles.get(i);
            p.update();
            if (!p.activo) {
                proyectiles.remove(i);
                i--;
            }
        }
    }

    // ===========================================================
    // DRAW
    // ===========================================================
    public void draw(Graphics g) {

        g.drawImage(sprite.getImage(), x, y, SIZE, SIZE, null);

        int barW = 44, barH = 6;
        int ox = (SIZE - barW) / 2;
        int barY = y + SIZE + 6;

        g.setColor(Color.RED);
        g.fillRect(x + ox, barY, barW, barH);

        g.setColor(Color.GREEN);
        g.fillRect(
                x + ox,
                barY,
                (int) (barW * (vida / (double) vidaMax)),
                barH
        );

        for (Proyectil p : proyectiles) {
            p.draw(g);
        }
    }

    // ===========================================================
    // DISPARO NORMAL – COMPLETAMENTE ARREGLADO
    // ===========================================================
    public void disparar() {

        int dx = 0, dy = 0;

        switch (direccion) {
            case 1 -> dy = 1;
            case 2 -> dy = -1;
            case 3 -> dx = -1;
            case 4 -> dx = 1;
        }
        if (dx == 0 && dy == 0) dy = 1;

        int cx = x + SIZE / 2;
        int cy = y + SIZE / 2;

        Proyectil p = new Proyectil(cx + dx * 12, cy + dy * 12, dx, dy);
        p.daño = 10;

        String rutaProj = heroName.equalsIgnoreCase("Larry")
                ? Proyectil.RUTA_PROY_LARRY
                : Proyectil.RUTA_PROY_ZORRITAS;

        // 🔧 AQUI ESTA LO MAS IMPORTANTE:
        // ZORRITAS = rotación direccional
        // LARRY = spin
        boolean rotarDireccion = heroName.equalsIgnoreCase("Zorritas");

        p.setSprite(rutaProj)
                .setRotateWithDirection(rotarDireccion)
                .conTamaño(48);

        proyectiles.add(p);
    }

    // ===========================================================
    // HABILIDAD – COMPLETAMENTE ARREGLADA
    // ===========================================================
    public void activarHabilidad() {

        int cx = x + SIZE / 2;
        int cy = y + SIZE / 2;

        int n = 12;

        for (int i = 0; i < n; i++) {

            double ang = (Math.PI * 2.0) * i / n;

            int vx = (int) Math.round(Math.cos(ang));
            int vy = (int) Math.round(Math.sin(ang));
            if (vx == 0 && vy == 0) vy = 1;

            Proyectil p = new Proyectil(cx, cy, vx, vy);
            p.velocidad = 12;
            p.daño = 12;

            String spr = heroName.equalsIgnoreCase("Larry")
                    ? Proyectil.RUTA_PROY_LARRY
                    : Proyectil.RUTA_PROY_ZORRITAS;

            boolean rotarDireccion = heroName.equalsIgnoreCase("Zorritas");

            p.setSprite(spr)
                    .setRotateWithDirection(rotarDireccion)
                    .conTamaño(52);

            proyectiles.add(p);
        }

        int dx = 0, dy = 1;

        switch (direccion) {
            case 1 -> dy = 1;
            case 2 -> dy = -1;
            case 3 -> { dx = -1; dy = 0; }
            case 4 -> { dx = 1; dy = 0; }
        }

        Proyectil fuerte = new Proyectil(cx, cy, dx, dy);
        fuerte.velocidad = 17;
        fuerte.daño = 18;

        String spr2 = heroName.equalsIgnoreCase("Larry")
                ? Proyectil.RUTA_PROY_LARRY
                : Proyectil.RUTA_PROY_ZORRITAS;

        boolean rotarDireccion2 = heroName.equalsIgnoreCase("Zorritas");

        fuerte.setSprite(spr2)
                .setRotateWithDirection(rotarDireccion2)
                .conTamaño(70);

        proyectiles.add(fuerte);
    }

    // ===========================================================
    public void recibirDaño(int dmg) {
        vida = Math.max(0, vida - Math.max(0, dmg));
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, SIZE, SIZE);
    }
}

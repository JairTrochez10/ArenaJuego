package Player;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Personajes {
    public static int SIZE = 56;        // lo usa Juego para centrar

    public int x, y;
    public int velocidad;
    public int vida, vidaMax;
    public boolean up, down, left, right, disparando;

    public final List<Proyectil> proyectiles = new ArrayList<>();
    private int disparoCooldown = 0;

    private ImageIcon sprite = new ImageIcon(getClass().getResource("/Imagenes/Kevin walk.gif"));

    // === NUEVO: recordar héroe elegido para asignar sprite de proyectil ===
    private String heroName = "Zorritas";

    // límites dinámicos (los manda Juego cada frame)
    private int ancho = 0, alto = 0, margen = 0;

    // 1=abajo, 2=arriba, 3=izq, 4=der (para dirección de disparo)
    private int direccion = 1;

    public Personajes(int x, int y, GameSettings settings) {
        this.x = x; this.y = y;
        this.vidaMax = 500;
        this.vida = vidaMax;
        this.velocidad = 6;  // Juego la ajusta con syncPlayerSpeed()
    }

    /** El menú te pasa el nombre exacto ("Zorritas" o "Larry"). */
    public void setHeroeSprite(String heroeNombre) {
        if (heroeNombre == null) return;
        String base = heroeNombre.trim();
        this.heroName = base; // <<< guardar para decidir el proyectil

        // 1) Mapa explícito
        String[] preferidas = null;
        if (base.equalsIgnoreCase("Zorritas")) {
            preferidas = new String[] {
                    "/Menu/imagen/Heroes/Kevin walk.gif",
                    "/Imagenes/Kevin walk.gif"
            };
        } else if (base.equalsIgnoreCase("Larry")) {
            preferidas = new String[] {
                    "/Menu/imagen/Heroes/Squeletron.gif",
                    "/Imagenes/Squeletron.gif"
            };
        }
        if (preferidas != null) {
            for (String r : preferidas) {
                java.net.URL u = getClass().getResource(r);
                if (u != null) { sprite = new ImageIcon(u); return; }
            }
        }

        // 2) Fallbacks genéricos
        String[] rutas = {
                "/Imagenes/" + base + ".gif",
                "/Imagenes/" + base + " walk.gif",
                "/Imagenes/" + base + ".png",
                "/Menu/imagen/Heroes/" + base + ".gif",
                "/Menu/imagen/Heroes/" + base + ".png",
                "/Imagenes/Kevin walk.gif" // último recurso
        };
        for (String r : rutas) {
            java.net.URL u = getClass().getResource(r);
            if (u != null) { sprite = new ImageIcon(u); return; }
        }
    }

    public void setVelocidad(int v) { this.velocidad = Math.max(1, v); }

    public void setLimitesPantalla(int ancho, int alto, int margen) {
        this.ancho = ancho; this.alto = alto; this.margen = Math.max(0, margen);
    }

    public void update() {
        int vx = 0, vy = 0;
        if (up) vy -= 1;
        if (down) vy += 1;
        if (left) vx -= 1;
        if (right) vx += 1;

        if (vx != 0 || vy != 0) {
            if (Math.abs(vx) > Math.abs(vy)) direccion = (vx < 0) ? 3 : 4;
            else direccion = (vy < 0) ? 2 : 1;

            double len = Math.hypot(vx, vy);
            double nx = vx / Math.max(1.0, len), ny = vy / Math.max(1.0, len);
            x += (int)Math.round(nx * velocidad);
            y += (int)Math.round(ny * velocidad);
        }

        if (ancho > 0 && alto > 0) {
            int minX = margen, minY = margen;
            int maxX = Math.max(minX, ancho - margen - SIZE);
            int maxY = Math.max(minY, alto - margen - SIZE);
            x = Math.max(minX, Math.min(x, maxX));
            y = Math.max(minY, Math.min(y, maxY));
        }

        if (disparando && disparoCooldown == 0) {
            disparar();
            disparoCooldown = 6; // menor = más rápido
        }
        if (disparoCooldown > 0) disparoCooldown--;

        for (int i = 0; i < proyectiles.size(); i++) {
            Proyectil p = proyectiles.get(i);
            p.update();
            if (!p.activo) { proyectiles.remove(i); i--; }
        }
    }

    public void draw(Graphics g) {
        g.drawImage(sprite.getImage(), x, y, SIZE, SIZE, null);

        // barra de vida debajo del personaje
        int barW = 44, barH = 6, ox = (SIZE - barW)/2;
        int barY = y + SIZE + 6;
        g.setColor(Color.RED);   g.fillRect(x + ox, barY, barW, barH);
        g.setColor(Color.GREEN); g.fillRect(x + ox, barY, (int)Math.round(barW * (vida / (double)vidaMax)), barH);

        for (Proyectil p : proyectiles) p.draw(g);
    }

    public void disparar() {
        int dx = 0, dy = 0;
        switch (direccion) { case 1 -> dy = 1; case 2 -> dy = -1; case 3 -> dx = -1; case 4 -> dx = 1; }
        if (dx == 0 && dy == 0) dy = 1;
        int cx = x + SIZE/2, cy = y + SIZE/2, muzzle = 12;
        Proyectil p = new Proyectil(cx + dx*muzzle, cy + dy*muzzle, dx, dy);
        p.daño = 10;

        // === SPRITE POR HÉROE + ROTACIÓN ===
        String rutaProj = ("Larry".equalsIgnoreCase(heroName))
                ? Proyectil.RUTA_PROY_LARRY
                : Proyectil.RUTA_PROY_ZORRITAS;

        p.setSprite(rutaProj)
                .setRotateWithDirection(true)
                .setFacingOffsetDegrees(0) // ajusta si tu PNG “mira” ↑ (−90), ↓ (+90), ← (180)
                .conTamaño(8);

        proyectiles.add(p);
    }

    /** Habilidad (si la usas): ráfaga circular + tiro dirigido */
    public void activarHabilidad() {
        int cx = x + SIZE/2;
        int cy = y + SIZE/2;

        int n = 10;
        for (int i = 0; i < n; i++) {
            double ang = (Math.PI * 2.0) * i / n;
            int vx = (int) Math.round(Math.cos(ang));
            int vy = (int) Math.round(Math.sin(ang));
            if (vx == 0 && vy == 0) vy = 1;
            Proyectil p = new Proyectil(cx, cy, vx, vy);
            p.velocidad = 11;
            p.daño = 12;
            p.conTamaño(7); // puedes asignar sprite también, si quieres
            proyectiles.add(p);
        }

        int dx = 0, dy = 1;
        switch (direccion) { case 1 -> dy = 1; case 2 -> dy = -1; case 3 -> {dx = -1; dy = 0;} case 4 -> {dx = 1; dy = 0;} }
        Proyectil fuerte = new Proyectil(cx, cy, dx, dy);
        fuerte.velocidad = 16;
        fuerte.daño = 18;
        fuerte.conTamaño(9);
        proyectiles.add(fuerte);
    }

    public void recibirDaño(int dmg) { vida = Math.max(0, vida - Math.max(0, dmg)); }

    public Rectangle getBounds() { return new Rectangle(x, y, SIZE, SIZE); }
}

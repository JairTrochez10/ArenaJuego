package Player;

import java.awt.*;

public class Proyectil {
    public int x, y;
    public int dirX, dirY;
    public int velocidad = 10;
    public boolean activo = true;
    public int daño = 10;

    private int size = 8;

    // límites globales para auto-destruir (los fija Juego cada frame)
    private static int VP_W = 800, VP_H = 600, VP_MARGIN = 0;

    public static void setViewportGlobal(int w, int h, int margen) {
        VP_W = Math.max(1, w);
        VP_H = Math.max(1, h);
        VP_MARGIN = Math.max(0, margen);
    }

    public Proyectil(int x, int y, int dirX, int dirY) {
        this.x = x; this.y = y;
        this.dirX = (dirX == 0 && dirY == 0) ? 0 : dirX;
        this.dirY = (dirX == 0 && dirY == 0) ? 1 : dirY;
    }

    /** requerido por tu Monstruo: cambiar tamaño del proyectil */
    public Proyectil conTamaño(int px) { this.size = Math.max(2, px); return this; }

    public void update() {
        x += dirX * velocidad;
        y += dirY * velocidad;

        // fuera de pantalla -> desactivar
        if (x < -VP_MARGIN || x > VP_W + VP_MARGIN || y < -VP_MARGIN || y > VP_H + VP_MARGIN) {
            activo = false;
        }
    }

    public void draw(Graphics g) {
        g.setColor(Color.YELLOW);
        g.fillOval(x - size/2, y - size/2, size, size);
    }

    public Rectangle getBounds() { return new Rectangle(x - size/2, y - size/2, size, size); }
}

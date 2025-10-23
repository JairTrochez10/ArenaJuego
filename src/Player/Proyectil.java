package Player;

import java.awt.*;
import javax.swing.*;

public class Proyectil {
    public int x, y;
    public int dirX, dirY;
    public int velocidad = 10;
    public boolean activo = true;
    public int daño = 10;

    private int size = 20;

    // === Sprite opcional ===
    private Image spriteImg = null;                // si es null, se dibuja el círculo
    private boolean keepAspect = true;             // mantener proporción al escalar
    private boolean rotateWithDirection = false;   // rotar según (dirX, dirY)
    private double facingOffsetRad = 0.0;          // corrección de orientación del sprite

    // Rutas públicas sugeridas (útiles desde otras clases)
    public static final String RUTA_PROY_ZORRITAS = "/Imagenes/Proyectil.png";
    public static final String RUTA_PROY_LARRY    = "/Imagenes/proyectilhues.png";
    public static final String RUTA_PROY_ENEMY    = "/Imagenes/proyectil_enemy.png";

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

    // === Configuración fluida ===
    /** cambiar tamaño del proyectil (afecta sprite o círculo) */
    public Proyectil conTamaño(int px) { this.size = Math.max(10, px); return this; }

    /** asignar sprite desde ruta de recursos */
    public Proyectil setSprite(String ruta) {
        try {
            java.net.URL u = getClass().getResource(ruta);
            if (u != null) this.spriteImg = new ImageIcon(u).getImage();
        } catch (Exception ignore) {}
        return this;
    }

    /** asignar sprite desde Image ya cargado */
    public Proyectil setSprite(Image img) { this.spriteImg = img; return this; }

    /** mantener proporción en el escalado */
    public Proyectil setKeepAspect(boolean keep) { this.keepAspect = keep; return this; }

    /** activar rotación del sprite según la dirección */
    public Proyectil setRotateWithDirection(boolean rotate) { this.rotateWithDirection = rotate; return this; }

    /** offset en grados si el PNG “mira” a otra dirección por defecto */
    public Proyectil setFacingOffsetDegrees(double deg) {
        this.facingOffsetRad = Math.toRadians(deg);
        return this;
    }

    // === Lógica ===
    public void update() {
        x += dirX * velocidad;
        y += dirY * velocidad;

        // fuera de pantalla -> desactivar
        if (x < -VP_MARGIN || x > VP_W + VP_MARGIN || y < -VP_MARGIN || y > VP_H + VP_MARGIN) {
            activo = false;
        }
    }

    public void draw(Graphics g) {
        if (spriteImg != null) {
            int dw = size, dh = size;
            int iw = Math.max(1, spriteImg.getWidth(null));
            int ih = Math.max(1, spriteImg.getHeight(null));
            if (keepAspect) {
                double s = Math.min(dw / (double) iw, dh / (double) ih);
                dw = (int) Math.round(iw * s);
                dh = (int) Math.round(ih * s);
            }

            int cx = x, cy = y;
            int dx = cx - dw / 2;
            int dy = cy - dh / 2;

            if (rotateWithDirection) {
                double ang = Math.atan2(dirY, dirX) + facingOffsetRad; // 0 rad = mirando a la derecha
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.rotate(ang, cx, cy);
                g2.drawImage(spriteImg, dx, dy, dw, dh, null);
                g2.dispose();
            } else {
                g.drawImage(spriteImg, dx, dy, dw, dh, null);
            }
        } else {
            // Fallback: círculo amarillo (tu comportamiento anterior)
            g.setColor(Color.YELLOW);
            g.fillOval(x - size/2, y - size/2, size, size);
        }
    }

    public Rectangle getBounds() { return new Rectangle(x - size/2, y - size/2, size, size); }
}

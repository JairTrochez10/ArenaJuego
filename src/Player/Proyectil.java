package Player;

import java.awt.*;
import javax.swing.*;

public class Proyectil {
    public int x, y;
    public int dirX, dirY;
    public int velocidad = 10;
    public boolean activo = true;
    public int daño = 10;

    // tamaño base (subido)
    private int size = 48;

    // === Sprite opcional ===
    private Image spriteImg = null;                // si es null, se dibuja el circulo
    private boolean keepAspect = true;             // mantener proporcion al escalar
    private boolean rotateWithDirection = false;   // rotar segun (dirX, dirY)
    private double facingOffsetRad = 0.0;          // correccion de orientacion del sprite

    // Rutas publicas sugeridas (utiles desde otras clases)
    public static final String RUTA_PROY_ZORRITAS = "/Imagenes/Proyectil.png";
    public static final String RUTA_PROY_LARRY    = "/Imagenes/proyectilhues.png";
    public static final String RUTA_PROY_ENEMY    = "/Imagenes/proyectil_enemy.png";

    // limites globales para auto-destruir (los fija Juego cada frame)
    private static int VP_W = 800, VP_H = 600, VP_MARGIN = 0;

    // escala global (afecta a todos los proyectiles)
    private static double ESCALA_GLOBAL = 1.0;

    public static void setViewportGlobal(int w, int h, int margen) {
        VP_W = Math.max(1, w);
        VP_H = Math.max(1, h);
        VP_MARGIN = Math.max(0, margen);
    }

    public static void setEscalaGlobal(double s) {
        ESCALA_GLOBAL = Math.max(0.1, s);
    }

    public Proyectil(int x, int y, int dirX, int dirY) {
        this.x = x; this.y = y;
        this.dirX = (dirX == 0 && dirY == 0) ? 0 : dirX;
        this.dirY = (dirX == 0 && dirY == 0) ? 1 : dirY;
    }

    // === Configuracion fluida ===
    /** cambiar tamano del proyectil (afecta sprite o circulo) */
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

    /** mantener proporcion en el escalado */
    public Proyectil setKeepAspect(boolean keep) { this.keepAspect = keep; return this; }

    /** activar rotacion del sprite segun la direccion */
    public Proyectil setRotateWithDirection(boolean rotate) { this.rotateWithDirection = rotate; return this; }

    /** offset en grados si el PNG “mira” a otra direccion por defecto */
    public Proyectil setFacingOffsetDegrees(double deg) {
        this.facingOffsetRad = Math.toRadians(deg);
        return this;
    }

    // === Logica ===
    public void update() {
        x += dirX * velocidad;
        y += dirY * velocidad;

        // fuera de pantalla -> desactivar
        if (x < -VP_MARGIN || x > VP_W + VP_MARGIN || y < -VP_MARGIN || y > VP_H + VP_MARGIN) {
            activo = false;
        }
    }

    public void draw(Graphics g) {
        // aplicar escala global al tamano actual
        int sizeEsc = (int) Math.round(size * ESCALA_GLOBAL);

        if (spriteImg != null) {
            // tamaño fijo para sprites
            int dw = 48, dh = 48;
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
            // fallback: circulo amarillo
            g.setColor(Color.YELLOW);
            g.fillOval(x - sizeEsc / 2, y - sizeEsc / 2, sizeEsc, sizeEsc);
        }
    }

    public Rectangle getBounds() {
        int sizeEsc = (int) Math.round(size * ESCALA_GLOBAL);
        return new Rectangle(x - sizeEsc / 2, y - sizeEsc / 2, sizeEsc, sizeEsc);
    }
}

// --- Ejemplo de uso ---
// Proyectil p = new Proyectil(x, y, dirX, dirY)
//     .setRotateWithDirection(true);

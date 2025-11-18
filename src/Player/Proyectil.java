package Player;

import java.awt.*;
import javax.swing.*;

public class Proyectil {
    public int x, y;
    public int dirX, dirY;
    public int velocidad = 10;
    public boolean activo = true;
    public int daño = 10;

    private int size = 48;

    private Image spriteImg = null;
    private boolean keepAspect = true;

    private boolean rotateByDirection = false; // Zorritas
    private boolean spinMode = false;          // Larry

    private double facingOffsetRad = 0.0;

    private double spinAngle = 0;
    private double spinSpeed = 0.35;

    public static final String RUTA_PROY_ZORRITAS = "/Imagenes/Proyectil.png";
    public static final String RUTA_PROY_LARRY    = "/Imagenes/Hueso.png";
    public static final String RUTA_PROY_ENEMY    = "/Imagenes/Zona5/PROYECTIL.png";

    private static int VP_W = 800, VP_H = 600, VP_MARGIN = 0;
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
        this.x = x;
        this.y = y;

        this.dirX = (dirX == 0 && dirY == 0) ? 0 : dirX;
        this.dirY = (dirX == 0 && dirY == 0) ? 1 : dirY;
    }

    public Proyectil conTamaño(int px) { this.size = Math.max(10, px); return this; }

    // ===========================
    // SPRITES COMPATIBLES
    // ===========================

    /** Sprite cargado desde ruta */
    public Proyectil setSprite(String ruta) {
        try {
            java.net.URL u = getClass().getResource(ruta);
            if (u != null)
                this.spriteImg = new ImageIcon(u).getImage();
        } catch (Exception ignore) {}
        return this;
    }

    /** Sprite cargado desde img → PARA Arena3 que usa Image */
    public Proyectil setSprite(Image img) {
        this.spriteImg = img;
        return this;
    }

    public Proyectil setKeepAspect(boolean keep) { this.keepAspect = keep; return this; }

    public Proyectil setRotateWithDirection(boolean rotate) {
        rotateByDirection = rotate;
        spinMode = !rotate;  // si NO rota por dirección → es spin (Larry)
        return this;
    }

    public Proyectil setFacingOffsetDegrees(double deg) {
        this.facingOffsetRad = Math.toRadians(deg);
        return this;
    }

    public void update() {
        x += dirX * velocidad;
        y += dirY * velocidad;

        if (spinMode) {
            spinAngle += spinSpeed;
        }

        if (x < -VP_MARGIN || x > VP_W + VP_MARGIN || y < -VP_MARGIN || y > VP_H + VP_MARGIN)
            activo = false;
    }

    public void draw(Graphics g) {
        int sizeEsc = (int) Math.round(size * ESCALA_GLOBAL);

        if (spriteImg != null) {

            int dw = 48, dh = 48;
            int iw = Math.max(1, spriteImg.getWidth(null));
            int ih = Math.max(1, spriteImg.getHeight(null));

            if (keepAspect) {
                double s = Math.min(dw / (double) iw, dh / (double) ih);
                dw = (int) Math.round(iw * s);
                dh = (int) Math.round(ih * s);
            }

            int cx = x;
            int cy = y;
            int dx = cx - dw / 2;
            int dy = cy - dh / 2;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            double ang;

            if (rotateByDirection) {
                ang = Math.atan2(dirY, dirX) + facingOffsetRad; // Zorritas
            } else {
                ang = spinAngle; // Larry
            }

            g2.rotate(ang, cx, cy);
            g2.drawImage(spriteImg, dx, dy, dw, dh, null);
            g2.dispose();

        } else {
            g.setColor(Color.YELLOW);
            g.fillOval(x - sizeEsc / 2, y - sizeEsc / 2, sizeEsc, sizeEsc);
        }
    }

    public Rectangle getBounds() {
        int sizeEsc = (int) Math.round(size * ESCALA_GLOBAL);
        return new Rectangle(x - sizeEsc / 2, y - sizeEsc / 2, sizeEsc, sizeEsc);
    }
}

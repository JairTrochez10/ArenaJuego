package Player;

import javax.swing.*;
import java.awt.*;
import java.util.Random;

public class JefeFinalDragon extends JefeFinal {

    private static final int DRAGON_SIZE = 200;

    // Teletransporte cada 6–8 s
    private static final long TELEPORT_MIN_MS = 6000;
    private static final long TELEPORT_MAX_MS = 8000;

    // Guardamos la config (no usamos el settings privado de JefeFinal)
    private final GameSettings cfg;

    private ImageIcon dragonSprite;
    private long nextSpawnMs = 0L;      // para invocar enemigos
    private long nextTeleportMs = 0L;   // para teletransporte
    private long blackoutUntilMs = 0L;  // pantalla negra corta al teletransportar

    // true = pegado a la derecha, false = pegado a la izquierda
    private boolean onRightSide = true;

    // sprite mirando: true = izquierda, false = derecha
    private boolean facingLeftSprite = true;

    // valor suave: -1 = izquierda, +1 = derecha
    private double facingBlend = 1.0;

    // Tamaño de pantalla
    private int viewW = 0, viewH = 0, viewMargin = 0;

    // ========= CONSTRUCTOR ÚNICO =========
    public JefeFinalDragon(int x, int y, GameSettings settings) {
        // super DEBE ser la primera línea
        super(x, y, (settings != null ? settings : GameSettings.fromDificultad("Media")));

        // normalizamos y guardamos en cfg
        this.cfg = (settings != null ? settings : GameSettings.fromDificultad("Media"));

        // cargar sprite del dragón
        try {
            java.net.URL u = getClass().getResource("/Imagenes/Zona5/Dragon.png");
            if (u != null) {
                dragonSprite = new ImageIcon(u);
            } else {
                dragonSprite = new ImageIcon();
            }
        } catch (Exception e) {
            dragonSprite = new ImageIcon();
        }

        // más vida que el jefe normal
        this.vida = 500;

        long now = System.currentTimeMillis();
        nextTeleportMs = now + TELEPORT_MIN_MS
                + (long) (Math.random() * (TELEPORT_MAX_MS - TELEPORT_MIN_MS));
    }

    // =================== Límites de pantalla ===================

    @Override
    public void setLimitesPantalla(int w, int h, int margen) {
        super.setLimitesPantalla(w, h, margen);
        this.viewW = w;
        this.viewH = h;
        this.viewMargin = Math.max(0, margen);
    }

    // =================== Invocar enemigos ===================

    /**
     * El dragón invoca enemigos EnemigosArena5 dentro de los límites de la arena.
     *
     * @param monstruo    instancia de Monstruo que contiene la lista enemigos.enemigos
     * @param rondaArena  ronda actual (1,2,3) para escalar dificultad
     */
    public void intentarSpawnearEnemigos(Monstruo monstruo, int rondaArena) {
        if (monstruo == null) return;

        long now = System.currentTimeMillis();
        if (now < nextSpawnMs) return;           // todavía no toca invocar

        nextSpawnMs = now + 10_000;              // cada 10 segundos

        int w = (viewW > 0) ? viewW : 900;
        int h = (viewH > 0) ? viewH : 600;
        int margenLocal = (viewMargin > 0) ? viewMargin : 60;

        Random rnd = new Random();

        // Velocidad base según tu sistema (igual que en ArenaBase pero usando cfg)
        double enemySpeedNow =
                Monstruo.SPEED_BASE * cfg.enemySpeedMul * (1.0 + 0.1 * (rondaArena - 1));

        // ===== creamos 2 minions tipo 1 y 1 tipo 2 =====

        // Tipo 1
        for (int i = 0; i < 1; i++) {
            int x = margenLocal + rnd.nextInt(Math.max(1, w - 2 * margenLocal));
            //int y = margenLocal + rnd.nextInt(Math.max(1, h - 2 * margenLocal));

            int vida = 20 + 10 * rondaArena;
            int vel  = Math.max(1, (int) Math.round(enemySpeedNow));
            int dmg  = 5 + 5 * rondaArena;

            monstruo.enemigos.add(new EnemigosArena5(x, y, 1, vida, vel, dmg));
        }

        // Tipo 2
        int x = margenLocal + rnd.nextInt(Math.max(1, w - 2 * margenLocal));
        //int y = margenLocal + rnd.nextInt(Math.max(1, h - 2 * margenLocal));
        int vida2 = 30 + 10 * rondaArena;
        int vel2  = Math.max(1, (int) Math.round(enemySpeedNow * 1.1));
        int dmg2  = 6 + 5 * rondaArena;

        monstruo.enemigos.add(new EnemigosArena5(x, y, 2, vida2, vel2, dmg2));
    }

    // =================== Movimiento / update ===================

    // NO usamos @Override aquí porque JefeFinal no tiene este método declarado
    protected void mover(Personajes jugador) {
        // Dragón estático; solo se teletransporta en teleport()
    }

    @Override
    public void update(Personajes jugador) {
        long now = System.currentTimeMillis();

        // Teletransporte cada cierto tiempo
        if (now >= nextTeleportMs && viewW > 0 && viewH > 0) {
            teleport();
            nextTeleportMs = now + TELEPORT_MIN_MS
                    + (long) (Math.random() * (TELEPORT_MAX_MS - TELEPORT_MIN_MS));
        }

        // ======== GIRO SUAVE SEGÚN POSICIÓN DEL JUGADOR ========
        // Centro del dragón y del jugador para comparar mejor
        int dragonCenterX = this.x + DRAGON_SIZE / 2;
        int playerCenterX = jugador.x + Personajes.SIZE / 2;

        // -1 = jugador a la izquierda, +1 = jugador a la derecha
        double target = (playerCenterX < dragonCenterX) ? -1.0 : 1.0;

        // interpolamos suavemente hacia el objetivo
        // 0.15 = rapidez del giro (puedes subir o bajar)
        facingBlend += (target - facingBlend) * 0.15;

        // decidimos si el sprite ya debe mirar a la izquierda o derecha
        // si facingBlend cruza 0, se voltea
        facingLeftSprite = (facingBlend <= 0.0);
        // =======================================================

        // Lógica base de JefeFinal (disparos, movimiento base, etc.)
        super.update(jugador);
    }

    private void teleport() {
        if (viewW <= 0 || viewH <= 0) return;

        long now = System.currentTimeMillis();
        blackoutUntilMs = now + 10; // pequeña pantalla negra

        // Cambiar de lado
        onRightSide = !onRightSide;

        int newX;
        if (onRightSide) {
            newX = Math.max(viewMargin, viewW - viewMargin - DRAGON_SIZE);
            // cuando se teletransporta a la derecha, que tienda a mirar a la izquierda
            facingBlend = -1.0;
        } else {
            newX = viewMargin;
            // cuando se teletransporta a la izquierda, que tienda a mirar a la derecha
            facingBlend = 1.0;
        }

        int minY = viewMargin;
        int maxY = Math.max(minY, viewH - viewMargin - DRAGON_SIZE);
        int newY = minY + (int) Math.round(Math.random() * (maxY - minY));

        this.x = newX;
        this.y = newY;
    }

    // =================== Dibujo ===================

    @Override
    public void draw(Graphics g) {
        long now = System.currentTimeMillis();

        // Pantallazo negro breve al teletransportar
        if (now < blackoutUntilMs) {
            Rectangle clip = g.getClipBounds();
            g.setColor(Color.BLACK);
            if (clip != null) {
                g.fillRect(clip.x, clip.y, clip.width, clip.height);
            } else {
                g.fillRect(0, 0, 2000, 2000);
            }
            return;
        }

        if (dragonSprite == null) {
            super.draw(g);
            return;
        }

        Image img = dragonSprite.getImage();
        int w = DRAGON_SIZE, h = DRAGON_SIZE;

        if (facingLeftSprite) {
            g.drawImage(img, x, y, w, h, null);
        } else {
            g.drawImage(img, x + w, y, -w, h, null);
        }

        // Barra de vida
        int barW = 120, barH = 8;
        int barX = x, barY = y - 10;
        g.setColor(Color.RED);
        g.fillRect(barX, barY, barW, barH);
        g.setColor(Color.GREEN);
        g.fillRect(barX, barY,
                (int) Math.round(barW * (vida / 500.0)), barH);

        // Disparos del jefe
        for (Proyectil d : disparos) d.draw(g);
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(x, y, DRAGON_SIZE, DRAGON_SIZE);
    }
}

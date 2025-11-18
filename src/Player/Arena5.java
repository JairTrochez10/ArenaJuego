package Player;

import javax.swing.*;
import java.awt.*;

/**
 * Arena 5 – Dragón Final
 * Solo genera enemigos propios (EnemigosArena5)
 * Sin enemigos base.
 * Animación épica de entrada:
 *  - Pantalla oscura
 *  - Bordes rojos
 *  - Temblor largo
 *  - Texto DRAGÓN FINAL gigante
 *  - Lógica congelada durante la intro
 *
 * Ahora: si el jugador muere o reinicia, regresa automáticamente a Arena1.
 */
public class Arena5 extends ArenaBase {

    // ============================
    //  DURACIONES ENTRADA ÉPICA
    // ============================
    private static final long BOSS_INTRO_DURATION_MS = 6000; // 6s total
    private static final long BOSS_INTRO_SHAKE_MS = 4500;    // 4.5s de temblor

    private long bossIntroStartMs = -1;
    private boolean bossIntroActive = false;

    public Arena5(GameSettings settings,
                  String heroe,
                  String nombreJugador,
                  LevelListener listener) {
        super(settings, heroe, nombreJugador, listener,
                5, "/Imagenes/Zona5/fondo.png");
    }

    // ============================
    //         RONDAS
    // ============================
    @Override
    protected int getEnemigosForRonda(int ronda) {
        return switch (ronda) {
            case 1 -> 6;
            case 2 -> 8;
            case 3 -> 12;
            default -> 6;
        };
    }

    @Override
    protected void spawnEnemigosRonda(int ronda) {
        enemigos.enemigos.clear();
        int cantidad = getEnemigosForRonda(ronda);
        enemigos.setExtraSpeedMul(roundMulEnemy);

        for (int i = 0; i < cantidad; i++) {
            int x = margen + (int) (Math.random() * (getWidth() - margen * 2));
            int y = margen + (int) (Math.random() * (getHeight() - margen * 2));

            int tipo = (i % 2 == 0) ? 1 : 2;

            int vida = 40 + ronda * 15;
            int vel = (int) Math.round(Monstruo.SPEED_BASE * settings.enemySpeedMul * roundMulEnemy);
            int dmg = 8 + ronda * 6;

            enemigos.enemigos.add(new EnemigosArena5(x, y, tipo, vida, vel, dmg));
        }

        int before = jugador.vida;
        jugador.vida = Math.min(jugador.vidaMax, jugador.vida + HEAL_ROUND_AMOUNT);
        if (jugador.vida > before) {
            healAnimUntilMs = System.currentTimeMillis() + 1200;
            showToast("+100 HP", 900);
        }

        syncPlayerSpeed();
        showToast("Ronda " + ronda + " / " + MAX_RONDAS, 1000);
    }

    // ============================
    //        JEFE FINAL
    // ============================
    @Override
    protected JefeFinal crearJefe() {
        bossIntroActive = true;
        bossIntroStartMs = System.currentTimeMillis();

        int x = getWidth() / 2 - JefeFinal.SIZE / 2;
        int y = margen + 20;
        return new JefeFinalDragon(x, y, settings);
    }

    // ============================
    //    UPDATE CON CONGELADO
    // ============================
    @Override
    public void actionPerformed(java.awt.event.ActionEvent e) {
        long now = System.currentTimeMillis();

        // 🔒 Durante la intro: lógica congelada
        if (bossIntroActive && now - bossIntroStartMs < BOSS_INTRO_DURATION_MS) {
            repaint();
            return;
        }

        // Luego del intro: continúa normal
        super.actionPerformed(e);

        // Si el jugador muere, volvemos automáticamente a Arena1
        if (jugador.vida <= 0) {
            volverAArena1();
            return;
        }

        // Dragón invoca enemigos durante batalla
        if (bossActivo && jefeFinal instanceof JefeFinalDragon dragon) {
            dragon.intentarSpawnearEnemigos(enemigos, rondaActual);
        }
    }

    // ============================
    //   EFECTO VISUAL ÉPICO
    // ============================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (!bossIntroActive) return;

        long now = System.currentTimeMillis();
        long elapsed = now - bossIntroStartMs;

        if (elapsed > BOSS_INTRO_DURATION_MS) {
            bossIntroActive = false;
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();

        // OSCURECIDO
        double t = Math.min(1.0, elapsed / (double) BOSS_INTRO_DURATION_MS);
        int alpha = (int) (220 * Math.pow(t, 0.6));
        g2.setColor(new Color(0, 0, 0, alpha));
        g2.fillRect(0, 0, getWidth(), getHeight());

        // BORDES ROJOS PULSANTES
        int borderAlpha = (int) (200 * (1 - Math.abs(0.5 - t) * 2));
        g2.setColor(new Color(255, 40, 0, Math.max(70, borderAlpha)));
        int thickness = 40;
        g2.fillRect(0, 0, getWidth(), thickness);
        g2.fillRect(0, getHeight() - thickness, getWidth(), thickness);
        g2.fillRect(0, 0, thickness, getHeight());
        g2.fillRect(getWidth() - thickness, 0, thickness, getHeight());

        // TEMBLOR
        if (elapsed < BOSS_INTRO_SHAKE_MS) {
            int shake = (int) (10 * (1 - elapsed / (double) BOSS_INTRO_SHAKE_MS));
            g2.translate(
                    (int) (Math.random() * shake - shake / 2),
                    (int) (Math.random() * shake - shake / 2)
            );
        }

        // TEXTO "DRAGÓN FINAL"
        String txt = "DRAGÓN FINAL";
        g2.setFont(new Font("Consolas", Font.BOLD, 75));
        int w = g2.getFontMetrics().stringWidth(txt);
        int x = getWidth() / 2 - w / 2;
        int y = getHeight() / 2;

        g2.setColor(new Color(255, 0, 0, 230));
        g2.drawString(txt, x, y);

        g2.dispose();
    }

    // ============================
    //   REINICIO AUTOMÁTICO
    // ============================
    @Override
    protected void reiniciarArena() {
        volverAArena1();
    }

    /**
     * 🔁 Vuelve automáticamente al inicio (Arena1) con mensaje de derrota.
     */
    private void volverAArena1() {
        if (timer != null) timer.stop();
        puntuacion = 0;

        JOptionPane.showMessageDialog(
                this,
                "Has sido derrotado...\nRegresando al inicio.",
                "Derrota",
                JOptionPane.INFORMATION_MESSAGE
        );

        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        if (frame != null) {
            frame.dispose();
            GameFrameNiveles nuevo = new GameFrameNiveles(settings, heroeElegido, nombreJugador);
            nuevo.setVisible(true);
        }
    }
}

package Player;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class Arena2 extends ArenaBase {

    private static final String R_ARQ = "/Imagenes/Zona2/arquero.gif";
    private static final String R_LAN = "/Imagenes/Zona2/lancero.gif";
    private static final String R_NEC = "/Imagenes/Zona2/necromante.gif";
    private static final String R_TEN = "/Imagenes/Zona2/tentaculo.gif";
    private static final String R_JEF = "/Imagenes/Zona2/jefefinal.gif";

    private Image imgArq, imgLan, imgNec, imgTen, imgBoss;
    private final Random rnd = new Random();

    private enum Tipo { ARQUERO, LANCERO, NECRO, TENTACULO }

    private class EnZ2 {
        int x, y, w = 70, h = 70;
        int vida = 80, vidaMax = 80;
        double speed = 2.4;
        Tipo tipo;
        Image sprite;

        EnZ2(Tipo t, Image s, int x, int y) {
            tipo = t; sprite = s; this.x = x; this.y = y;
        }

        Rectangle hit() { return new Rectangle(x, y, w, h); }

        void update() {
            double dx = jugador.x - x, dy = jugador.y - y;
            double d = Math.sqrt(dx * dx + dy * dy);
            if (d < 1) d = 1;
            x += (dx / d) * speed;
            y += (dy / d) * speed;
            evitarSuperposicion(this);
        }

        void draw(Graphics g) {
            g.drawImage(sprite, x, y, w, h, null);
            int bw = w, bh = 6, bx = x, by = y - 8;
            g.setColor(Color.RED); g.fillRect(bx, by, bw, bh);
            g.setColor(Color.GREEN);
            g.fillRect(bx, by, (int) (bw * (vida / (double) vidaMax)), bh);
            g.setColor(Color.BLACK); g.drawRect(bx, by, bw, bh);
        }
    }

    private final ArrayList<EnZ2> enemigosZ2 = new ArrayList<>();

    private class BossZ2 {
        int x, y, w = 200, h = 200;
        int vida = 900, vidaMax = 900;
        double speedY = 2.2;
        Image sprite;
        BossZ2(Image s, int x, int y) { sprite = s; this.x = x; this.y = y; }
        Rectangle hit() { return new Rectangle(x, y, w, h); }
        void update() { double dy = jugador.y - y; if (Math.abs(dy) > 2) y += dy > 0 ? speedY : -speedY; }
        void draw(Graphics g) { g.drawImage(sprite, x, y, w, h, null); }
    }

    private BossZ2 jefe = null;
    private boolean jefeActivo = false, jefeMuerto = false;

    private boolean rayoActivo = false;
    private Rectangle rayoRect;
    private int rayoTimer = 0;
    private static final int RAYO_DUR = 25;
    private static final int RAY_DMG = 10;
    private static final int RAY_COOLDOWN = 6;
    private int rayCD = 0;

    private final int[] rondas = {4, 7, 12};
    private int indexRonda = 0, summonCounter = 0, rayCounter = 0;
    private static final int SUMMON_MAX = 240, RAY_MAX = 180;
    private int touchCD = 0;

    public Arena2(GameSettings settings, String heroe, String nombreJugador, LevelListener lis) {
        super(settings, heroe, nombreJugador, lis, 2, "/Imagenes/Zona2/Arena2.jpg");
        cargarSprites();
        this.jefeFinal = null; this.bossActivo = false;
    }

    private void cargarSprites() {
        imgArq = cargar(R_ARQ); imgLan = cargar(R_LAN);
        imgNec = cargar(R_NEC); imgTen = cargar(R_TEN); imgBoss = cargar(R_JEF);
    }

    private Image cargar(String r) {
        try { return new ImageIcon(getClass().getResource(r)).getImage(); }
        catch (Exception e) { System.out.println("NO EXISTE: " + r); return null; }
    }

    @Override
    public void iniciar(int vidaInicial) {
        super.iniciar(vidaInicial);
        enemigosZ2.clear(); jefe = null; jefeActivo = false; jefeMuerto = false;
        indexRonda = 0; summonCounter = 0; rayCounter = 0;
        rayoActivo = false; rayCD = 0; touchCD = 0;
        spawnRondaZ2(rondas[indexRonda]);
    }

    private boolean demasiadoCerca(int nx, int ny) {
        return jugador.getBounds().intersects(new Rectangle(nx, ny, 80, 80));
    }

    private boolean muyCercaEntreEllos(int x, int y) {
        for (EnZ2 e : enemigosZ2)
            if (new Rectangle(x, y, 80, 80).intersects(e.hit())) return true;
        return false;
    }

    private EnZ2 crearEnemigo(Tipo t) {
        int W = Math.max(getWidth(), 800), H = Math.max(getHeight(), 600);
        int x, y, tries = 0;
        do {
            x = rnd.nextInt(Math.max(1, W - 200)) + 100;
            y = rnd.nextInt(Math.max(1, H - 200)) + 100;
            tries++;
            if (tries > 40) break;
        } while (demasiadoCerca(x, y) || muyCercaEntreEllos(x, y));

        Image sp = switch (t) {
            case ARQUERO -> imgArq; case LANCERO -> imgLan;
            case NECRO -> imgNec; case TENTACULO -> imgTen;
        };
        return new EnZ2(t, sp, x, y);
    }

    private void evitarSuperposicion(EnZ2 ref) {
        for (EnZ2 e : enemigosZ2) {
            if (e == ref) continue;
            Rectangle r1 = ref.hit(), r2 = e.hit();
            if (r1.intersects(r2)) {
                if (ref.x < e.x) ref.x -= 2; else ref.x += 2;
                if (ref.y < e.y) ref.y -= 2; else ref.y += 2;
            }
        }
    }

    private void spawnRondaZ2(int cant) {
        if (getWidth() < 200 || getHeight() < 200) {
            SwingUtilities.invokeLater(() -> spawnRondaZ2(cant));
            return;
        }
        enemigosZ2.clear();
        Tipo[] tipos = Tipo.values();
        int usados = 0;
        for (Tipo t : tipos) {
            if (usados >= cant) break;
            enemigosZ2.add(crearEnemigo(t));
            usados++;
        }
        while (usados < cant) {
            enemigosZ2.add(crearEnemigo(tipos[rnd.nextInt(tipos.length)]));
            usados++;
        }
    }

    private void spawnJefe() {
        jefe = new BossZ2(imgBoss, 60, getHeight() / 2 - 100);
        jefeActivo = true; summonCounter = 0; rayCounter = 0;
        rayCD = 0; rayoActivo = false; rayoRect = null;
        showToast("JEFE - Arena 2", 1600);
        rondaActual = MAX_RONDAS;
    }

    @Override
    public void actionPerformed(java.awt.event.ActionEvent e) {
        if (gameOver || paused) { repaint(); return; }
        jugador.update(); jugador.setLimitesPantalla(getWidth(), getHeight(), margen);
        enemigos.enemigos.clear(); updateZ2();

        if (jugador.vida <= 0) {
            gameOver = true;
            volverAArena1();
            return;
        }
        repaint();
    }

    private void updateZ2() {
        Rectangle pj = jugador.getBounds();
        if (touchCD > 0) touchCD--; if (rayCD > 0) rayCD--;
        for (EnZ2 e : enemigosZ2) e.update();

        for (EnZ2 e : enemigosZ2)
            if (pj.intersects(e.hit()) && touchCD == 0) { jugador.recibirDaño(4); touchCD = 16; }

        for (Proyectil p : new ArrayList<>(jugador.proyectiles)) {
            if (!p.activo) continue;
            Rectangle pb = p.getBounds();
            Iterator<EnZ2> it = enemigosZ2.iterator();
            while (it.hasNext()) {
                EnZ2 en = it.next();
                if (pb.intersects(en.hit())) {
                    en.vida -= p.daño; p.activo = false;
                    if (en.vida <= 0) { it.remove(); puntuacion += 100; }
                    break;
                }
            }
        }

        if (!jefeActivo && enemigosZ2.isEmpty()) {
            jugador.vida = Math.min(jugador.vidaMax, jugador.vida + 100);
            showToast("+100 HP", 900);

            rondaActual++;
            if (indexRonda < rondas.length - 1) {
                indexRonda++; spawnRondaZ2(rondas[indexRonda]);
                showToast("Ronda " + (indexRonda + 1), 900);
            } else spawnJefe();
        }

        if (jefeActivo && jefe != null) {
            jefe.update();
            for (Proyectil p : new ArrayList<>(jugador.proyectiles))
                if (p.activo && p.getBounds().intersects(jefe.hit())) { jefe.vida -= p.daño; p.activo = false; }

            if (jefe.vida <= 0) {
                jefeActivo = false; jefeMuerto = true; puntuacion += 1000;
                timer.stop();
                if (levelListener != null)
                    levelListener.onLevelComplete(numeroArena, puntuacion, jugador.vida);
                return;
            }

            summonCounter++;
            if (summonCounter >= SUMMON_MAX) { summonCounter = 0; invocarMinions(); }

            rayCounter++;
            if (rayCounter >= RAY_MAX && !rayoActivo) { rayCounter = 0; activarRayo(); }

            if (rayoActivo && rayoRect != null && pj.intersects(rayoRect) && rayCD == 0) {
                jugador.recibirDaño(RAY_DMG); rayCD = RAY_COOLDOWN;
            }

            if (rayoActivo) { rayoTimer--; if (rayoTimer <= 0) { rayoActivo = false; rayoRect = null; } }
        }
    }

    private void invocarMinions() {
        Tipo[] tipos = Tipo.values();
        for (int i = 0; i < 3; i++)
            enemigosZ2.add(crearEnemigo(tipos[rnd.nextInt(tipos.length)]));
    }

    private void activarRayo() {
        int cx = jugador.x + Personajes.SIZE / 2, w = 80;
        rayoRect = new Rectangle(cx - w / 2, 0, w, getHeight());
        rayoActivo = true; rayoTimer = RAYO_DUR; rayCD = 0;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        for (EnZ2 e : enemigosZ2) e.draw(g);

        if (jefeActivo && jefe != null) {
            jefe.draw(g);
            int bw = 380, bx = getWidth() / 2 - bw / 2, by = 58;
            g.setColor(Color.DARK_GRAY); g.fillRect(bx, by, bw, 20);
            g.setColor(Color.RED); g.fillRect(bx, by, (int)(bw * (jefe.vida / (double)jefe.vidaMax)), 20);
            g.setColor(Color.WHITE); g.drawRect(bx, by, bw, 20);
            g.drawString("JEFE", bx + bw / 2 - 20, by - 4);
        }

        if (rayoActivo && rayoRect != null) {
            g.setColor(new Color(255,255,0,120));
            g.fillRect(rayoRect.x, rayoRect.y, rayoRect.width, rayoRect.height);
            g.setColor(Color.WHITE);
            g.drawRect(rayoRect.x, rayoRect.y, rayoRect.width, rayoRect.height);
        }

        // PAUSA cinematográfica
        if (paused && !gameOver) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(new Color(0,0,0,160));
            g2.fillRect(0,0,getWidth(),getHeight());
            int cardW = Math.min(520, getWidth() - 80);
            int cardH = 220;
            int cardX = (getWidth() - cardW) / 2;
            int cardY = (getHeight() - cardH) / 2;
            g2.setColor(new Color(15,15,25,230));
            g2.fillRoundRect(cardX, cardY, cardW, cardH, 26, 26);
            g2.setStroke(new BasicStroke(3f));
            g2.setColor(new Color(255,215,0,220));
            g2.drawRoundRect(cardX + 1, cardY + 1, cardW - 2, cardH - 2, 26, 26);
            g2.setFont(new Font("Consolas", Font.BOLD, 40));
            String txt = "PAUSA";
            int w = g2.getFontMetrics().stringWidth(txt);
            g2.setColor(new Color(255,240,200));
            g2.drawString(txt, getWidth()/2 - w/2, cardY + 70);
            g2.setFont(new Font("Consolas", Font.PLAIN, 20));
            g2.setColor(Color.WHITE);
            g2.drawString("P   - Reanudar", cardX + 40, cardY + 110);
            g2.drawString("R   - Reiniciar nivel", cardX + 40, cardY + 138);
            g2.drawString("ESC - Volver al Menú", cardX + 40, cardY + 166);
            g2.dispose();
        }
    }

    @Override
    protected void reiniciarArena() {
        volverAArena1();
    }

    /**
     * Vuelve automáticamente a Arena1 con mensaje de derrota.
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

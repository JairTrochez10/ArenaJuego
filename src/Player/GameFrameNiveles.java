package Player;

import javax.swing.*;
import java.awt.*;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * GameFrameNiveles
 *
 * - Controla: Arena1 -> Arena2 -> Arena3 -> Arena4 -> Arena5
 * - Mantiene puntuacionGlobal y vidaGlobal entre arenas.
 * - Pantalla completa.
 * - TRANSICIÓN SIMPLE:
 *      * Al pasar de arena: pantalla tipo “pausa” con "NIVEL X" 1.5s y sigue solo.
 *      * Al ganar (después de Arena5): pantalla final épica y vuelve al menú.
 * - Al ganar se guarda la puntuacion en ArenaScores.csv con estado VICTORY.
 */
public class GameFrameNiveles extends JFrame implements LevelListener {

    private int arenaActual = 1;
    private int puntuacionGlobal = 0;

    // Vida global entre niveles. -1 => vida completa al entrar.
    private int vidaGlobal = -1;

    private final GameSettings settings;
    private final String heroe;
    private final String nombreJugador;

    // Tiempo global desde que se empezó la primera arena (para el score de victoria).
    private final long startNanosGlobal;

    public GameFrameNiveles(GameSettings settings, String heroe, String nombreJugador) {
        this.settings = (settings != null) ? settings : GameSettings.fromDificultad("Media");
        this.heroe = (heroe != null && !heroe.isBlank()) ? heroe : "Default";
        this.nombreJugador = (nombreJugador == null || nombreJugador.isBlank()) ? "Invitado" : nombreJugador;

        setTitle("Arena por niveles");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Pantalla completa sin bordes
        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);

        // Marca de tiempo global (desde que arranca el modo por niveles)
        this.startNanosGlobal = System.nanoTime();

        cargarArena(1);
    }

    /**
     * Carga una arena (1..5).
     * Si el número es >5, muestra la pantalla final de victoria.
     */
    private void cargarArena(int numeroArena) {
        this.arenaActual = numeroArena;

        getContentPane().removeAll();

        ArenaBase arena;

        switch (numeroArena) {
            case 1 -> arena = new Arena1(settings, heroe, nombreJugador, this);
            case 2 -> arena = new Arena2(settings, heroe, nombreJugador, this);
            case 3 -> arena = new Arena3(settings, heroe, nombreJugador, this);
            case 4 -> arena = new Arena4(settings, heroe, nombreJugador, this);
            case 5 -> arena = new Arena5(settings, heroe, nombreJugador, this);
            default -> {
                mostrarPantallaVictoriaCool();
                return;
            }
        }

        // Mantener puntuación acumulada entre arenas
        arena.setPuntuacion(puntuacionGlobal);

        // Vida inicial de esta arena:
        //   vidaGlobal > 0 => viene de la arena anterior (+200 ya sumados)
        int vidaInicial = (vidaGlobal > 0) ? vidaGlobal : -1;
        arena.iniciar(vidaInicial);

        setContentPane(arena);
        revalidate();
        repaint();
    }

    /**
     * Llamado por cada Arena cuando se mata al jefe de ese nivel.
     *
     * @param numeroArena  Número de arena que se completó.
     * @param puntuacion   Puntuación acumulada.
     * @param vidaJugador  Vida actual del jugador al terminar.
     */
    @Override
    public void onLevelComplete(int numeroArena, int puntuacion, int vidaJugador) {
        this.puntuacionGlobal = puntuacion;

        // Curación por NIVEL: +200 HP al pasar a la siguiente arena.
        this.vidaGlobal = vidaJugador + 200;

        if (numeroArena >= 5) {
            // Ya se completaron las 5 arenas => pantalla de victoria final.
            mostrarPantallaVictoriaCool();
        } else {
            // Pantalla de transición tipo pausa (sin botón, auto-sigue).
            mostrarTransicionCool(numeroArena);
        }
    }

    /**
     * Muestra una pantalla de transición a pantalla completa
     * cuando se termina una arena (1..4).
     * Solo texto "NIVEL X" + pequeña descripción, y a los 1.5s entra sola.
     */
    private void mostrarTransicionCool(int numeroArenaCompletada) {
        int siguienteArena = numeroArenaCompletada + 1;

        TransitionPanel panel = new TransitionPanel(
                numeroArenaCompletada,
                siguienteArena,
                nombreJugador,
                puntuacionGlobal
        );

        setContentPane(panel);
        revalidate();
        repaint();

        // Timer: 1500 ms y carga la siguiente arena automáticamente.
        new Timer(1500, e -> {
            ((Timer) e.getSource()).stop();
            cargarArena(siguienteArena);
        }).start();
    }

    /**
     * Muestra una pantalla de victoria épica a pantalla completa
     * al terminar la Arena5. Desde aquí se vuelve al Menú Principal.
     * También guarda la puntuacion en CSV con estado VICTORY.
     */
    private void mostrarPantallaVictoriaCool() {
        guardarScoreVictoria(); // <<--- aquí se guarda la puntuación al ganar

        VictoryPanel panel = new VictoryPanel(
                nombreJugador,
                puntuacionGlobal,
                this::volverAlMenuPrincipal
        );

        setContentPane(panel);
        revalidate();
        repaint();
    }

    /**
     * Registra en ArenaScores.csv la victoria final de este modo por niveles.
     * Formato: fecha, nombreJugador, dificultad, puntuacion, tiempoSegundos, estado
     * estado = "VICTORY".
     */
    private void guardarScoreVictoria() {
        try {
            long elapsedSec = Math.max(0, (System.nanoTime() - startNanosGlobal) / 1_000_000_000L);
            String when = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String row = String.join(",",
                    when,
                    nombreJugador.replace(",", " "),
                    settings.dificultadName,
                    String.valueOf(puntuacionGlobal),
                    String.valueOf(elapsedSec),
                    "VICTORY"
            );
            try (FileWriter fw = new FileWriter(System.getProperty("user.home") + "/ArenaScores.csv", true)) {
                fw.write(row + System.lineSeparator());
            }
        } catch (Exception ignore) { }
    }

    /**
     * Regresa al menú principal (MenuPrincipal) y cierra este frame.
     */
    private void volverAlMenuPrincipal() {
        try {
            Class<?> cls = Class.forName("Menu.MenuPrincipal");
            Object obj = cls.getConstructor().newInstance();
            if (obj instanceof JFrame jf) jf.setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    null,
                    "No se encontró el Menú Principal.\nSe cerrará la ventana.",
                    "Aviso",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
        dispose();
    }

    // =====================================================================
    //  PANELES DE TRANSICIÓN "COOL"
    // =====================================================================

    /**
     * Pantalla de transición entre arenas.
     * Solo muestra un gran "NIVEL X" + jugador/puntuación
     * como si fuera una pausa corta.
     */
    private static class TransitionPanel extends JPanel {

        private final int arenaSiguiente;
        private final String nombreJugador;
        private final int puntuacion;

        public TransitionPanel(int arenaCompletada,
                               int arenaSiguiente,
                               String nombreJugador,
                               int puntuacion) {
            this.arenaSiguiente = arenaSiguiente;
            this.nombreJugador = nombreJugador;
            this.puntuacion = puntuacion;

            setLayout(new BorderLayout());
            setFocusable(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Fondo oscuro tipo pausa
            g2.setColor(new Color(0, 0, 0, 210));
            g2.fillRect(0, 0, getWidth(), getHeight());

            // "Card" central
            int cardW = Math.min(600, getWidth() - 80);
            int cardH = 220;
            int cardX = (getWidth() - cardW) / 2;
            int cardY = (getHeight() - cardH) / 2;

            g2.setColor(new Color(20, 20, 40, 230));
            g2.fillRoundRect(cardX, cardY, cardW, cardH, 28, 28);
            g2.setStroke(new BasicStroke(3f));
            g2.setColor(new Color(255, 215, 0, 220));
            g2.drawRoundRect(cardX + 1, cardY + 1, cardW - 2, cardH - 2, 28, 28);

            // Texto: NIVEL X
            String nivelTxt = "NIVEL " + arenaSiguiente;
            g2.setFont(new Font("Consolas", Font.BOLD, 48));
            int tw = g2.getFontMetrics().stringWidth(nivelTxt);
            int tx = getWidth() / 2 - tw / 2;
            int ty = cardY + 80;
            g2.setColor(new Color(255, 240, 200));
            g2.drawString(nivelTxt, tx, ty);

            // Texto secundario
            g2.setFont(new Font("Consolas", Font.PLAIN, 22));
            String jTxt = "Jugador: " + nombreJugador;
            String pTxt = "Puntuación: " + puntuacion;
            int twJ = g2.getFontMetrics().stringWidth(jTxt);
            int twP = g2.getFontMetrics().stringWidth(pTxt);
            int baseY = cardY + 130;
            g2.setColor(Color.WHITE);
            g2.drawString(jTxt, getWidth() / 2 - twJ / 2, baseY);
            g2.drawString(pTxt, getWidth() / 2 - twP / 2, baseY + 30);

            // Nota pequeña
            g2.setFont(new Font("Consolas", Font.PLAIN, 16));
            String nota = "Preparando la siguiente arena...";
            int twN = g2.getFontMetrics().stringWidth(nota);
            g2.setColor(new Color(200, 200, 200));
            g2.drawString(nota, getWidth() / 2 - twN / 2, cardY + cardH - 20);

            g2.dispose();
        }
    }

    /**
     * Pantalla final de victoria (después de Arena5).
     * Texto grande "¡VICTORIA TOTAL!"
     * y botón "Volver al Menú Principal".
     */
    private static class VictoryPanel extends JPanel {

        private final String nombreJugador;
        private final int puntuacionFinal;
        private final Runnable onVolverMenu;

        public VictoryPanel(String nombreJugador,
                            int puntuacionFinal,
                            Runnable onVolverMenu) {
            this.nombreJugador = nombreJugador;
            this.puntuacionFinal = puntuacionFinal;
            this.onVolverMenu = onVolverMenu;

            setLayout(new BorderLayout());
            setFocusable(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Fondo tipo épico rojo
            GradientPaint gp = new GradientPaint(
                    0, 0, new Color(30, 10, 10),
                    0, getHeight(), new Color(90, 30, 30)
            );
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Halo dorado
            g2.setColor(new Color(255, 215, 0, 55));
            int r = Math.max(getWidth(), getHeight());
            g2.fillOval(getWidth() / 2 - r / 2, getHeight() / 2 - r / 2, r, r);

            // Card central
            int cardW = Math.min(700, getWidth() - 80);
            int cardH = 260;
            int cardX = (getWidth() - cardW) / 2;
            int cardY = (getHeight() - cardH) / 2;

            g2.setColor(new Color(20, 10, 10, 230));
            g2.fillRoundRect(cardX, cardY, cardW, cardH, 30, 30);
            g2.setStroke(new BasicStroke(3f));
            g2.setColor(new Color(255, 215, 0, 230));
            g2.drawRoundRect(cardX + 1, cardY + 1, cardW - 2, cardH - 2, 30, 30);

            // Título
            String titulo = "¡VICTORIA TOTAL!";
            g2.setFont(new Font("Consolas", Font.BOLD, 50));
            int tw = g2.getFontMetrics().stringWidth(titulo);
            g2.setColor(new Color(255, 240, 200));
            g2.drawString(titulo, getWidth() / 2 - tw / 2, cardY + 80);

            // Info
            g2.setFont(new Font("Consolas", Font.PLAIN, 24));
            String jTxt = "Jugador: " + nombreJugador;
            String pTxt = "Puntuación final: " + puntuacionFinal;
            int twJ = g2.getFontMetrics().stringWidth(jTxt);
            int twP = g2.getFontMetrics().stringWidth(pTxt);
            int baseY = cardY + 125;
            g2.setColor(Color.WHITE);
            g2.drawString(jTxt, getWidth() / 2 - twJ / 2, baseY);
            g2.drawString(pTxt, getWidth() / 2 - twP / 2, baseY + 30);

            // Botón “Volver al menú” dibujado bonito (y usamos ENTER/ESPACIO para activarlo)
            int btnW = 320, btnH = 50;
            int btnX = getWidth() / 2 - btnW / 2;
            int btnY = cardY + cardH - 70;
            g2.setColor(new Color(60, 20, 20, 230));
            g2.fillRoundRect(btnX, btnY, btnW, btnH, 20, 20);
            g2.setColor(new Color(255, 215, 0, 220));
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawRoundRect(btnX, btnY, btnW, btnH, 20, 20);
            String btnTxt = "Volver al Menú Principal (ENTER)";
            g2.setFont(new Font("Consolas", Font.BOLD, 18));
            int twB = g2.getFontMetrics().stringWidth(btnTxt);
            g2.setColor(Color.WHITE);
            g2.drawString(btnTxt, getWidth() / 2 - twB / 2, btnY + 32);

            g2.dispose();
        }

        @Override
        public void addNotify() {
            super.addNotify();
            // ENTER / ESPACIO -> volver al menú
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"), "menu");
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("SPACE"), "menu");
            getActionMap().put("menu", new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (onVolverMenu != null) onVolverMenu.run();
                }
            });
        }
    }
}

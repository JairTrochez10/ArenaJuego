package Menu;

import javax.swing.*;
import java.awt.*;

/**
 * Menú de Instrucciones (centrado y con texto que resalta)
 *
 * Fondos (usa el primero que exista):
 *  - /Imagenes/FondoIns.jpg     (fondo de menú)
 *  - /Menu/imagen/Jefe.gif      (fondo de menú)
 *  - /Menu/imagen/Enemigos/Calavoso2.png (fondo de menú)
 */
public class MenuInstruccionesPanel extends JPanel {

    private static final String F1 = "/Imagenes/FondoIns.jpg";
    private static final String F2 = "/Menu/imagen/Jefe.gif";
    private static final String F3 = "/Menu/imagen/Enemigos/Calavoso2.png";

    public MenuInstruccionesPanel(MenuPrincipal frame) {
        setLayout(new BorderLayout());
        String fondoRuta = MenuPrincipal.firstExisting(F1, F2, F3);

        JPanel fondo = new MenuPrincipal.PanelFondo(fondoRuta);
        // Usamos GridBagLayout para centrar TODO
        fondo.setLayout(new GridBagLayout());
        add(fondo, BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.insets = new Insets(24, 24, 24, 24);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;

        // Título con sombra
        ShadowLabel titulo = new ShadowLabel("📜 Instrucciones");
        titulo.setFont(new Font("Consolas", Font.BOLD, 42));
        titulo.setForeground(new Color(255, 215, 0)); // dorado
        titulo.setHorizontalAlignment(SwingConstants.CENTER);

        // Panel “glass” centrado con el contenido
        JPanel glass = new RoundedGlassPanel();
        glass.setLayout(new BorderLayout());
        glass.setPreferredSize(new Dimension(900, 520)); // tamaño cómodo en centro
        glass.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        // Contenido HTML centrado y con estilos para resaltar
        JEditorPane ep = new JEditorPane("text/html", buildHtml());
        ep.setEditable(false);
        ep.setOpaque(false);
        ep.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE); // respeta fuentes

        JScrollPane sp = new JScrollPane(ep);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // Cabecera arriba del “glass”
        JPanel header = new JPanel(new BorderLayout()) { @Override public boolean isOpaque(){ return false; } };
        header.add(titulo, BorderLayout.CENTER);

        glass.add(header, BorderLayout.NORTH);
        glass.add(sp, BorderLayout.CENTER);

        // Contenedor vertical para centrar (título+glass+botón)
        JPanel columna = new JPanel();
        columna.setOpaque(false);
        columna.setLayout(new BoxLayout(columna, BoxLayout.Y_AXIS));
        columna.add(Box.createVerticalGlue());
        columna.add(glass);
        columna.add(Box.createVerticalStrut(18));

        // Botón Volver (mismo estilo que el resto, centrado)
        JButton volver = MenuPrincipal.boton("Volver");
        volver.setAlignmentX(Component.CENTER_ALIGNMENT);
        volver.setPreferredSize(new Dimension(280, 56));
        volver.addActionListener(e -> {
            frame.setContentPane(new MenuPrincipal.PanelFondo(MenuPrincipal.firstExisting(F1, F2, F3)));
            new MenuPrincipal().setVisible(true);
            frame.dispose();
        });
        JPanel botonera = new JPanel() { @Override public boolean isOpaque(){ return false; } };
        botonera.add(volver);

        columna.add(botonera);
        columna.add(Box.createVerticalGlue());

        fondo.add(columna, gbc);
    }

    /** HTML con texto centrado y resaltado */
    private static String buildHtml() {
        // Colores y estilos: dorado para títulos, blanco para texto, sombra CSS leve
        // Centramos todo el contenido.
        return """
        <html>
        <head>
        <style>
          body {
            font-family: Consolas, monospace;
            color: #FFFFFF;
            text-align: center;
            margin: 0;
          }
          .bloque {
            font-size: 18px;
            line-height: 1.45;
            margin: 0 0 14px 0;
            text-shadow: 0 2px 3px rgba(0,0,0,0.6);
          }
          h2 {
            color: #FFD700; /* dorado */
            font-size: 24px;
            margin: 12px 0 10px 0;
            text-shadow: 0 3px 6px rgba(0,0,0,0.7);
          }
          ul {
            list-style: none;
            padding: 0;
            margin: 8px 0 16px 0;
          }
          li {
            margin: 6px 0;
          }
          .pill {
            display: inline-block;
            background: rgba(0,0,0,0.35);
            border: 1px solid rgba(255,215,0,0.6);
            border-radius: 10px;
            padding: 2px 8px;
            margin: 0 2px;
          }
          .gold { color: #FFD700; font-weight: 700; }
          .dim  { color: #D0D0D0; }
        </style>
        </head>
        <body>
          <div class="bloque">
            <h2>CONTROLES</h2>
            <ul>
              <li>• Mover: <span class="pill">W</span> <span class="pill">A</span> <span class="pill">S</span> <span class="pill">D</span></li>
              <li>• Disparar: <span class="pill">SPACE</span></li>
              <li>• Pausa / Game Over: <span class="dim">usa los botones en pantalla</span></li>
              <li>• Salir: <span class="pill">ESC</span></li>
            </ul>
          </div>

          <div class="bloque">
            <h2>OBJETIVO</h2>
            <ul>
              <li>• Sobrevive y consigue la <span class="gold">mejor puntuación</span> posible.</li>
              <li>• Cada ronda <span class="gold">aumentan</span> la cantidad y velocidad de los enemigos.</li>
              <li>• Cada <span class="gold">3 rondas</span> aparece el <span class="gold">JEFE FINAL</span>.</li>
            </ul>
          </div>

          <div class="bloque">
            <h2>SISTEMA</h2>
            <ul>
              <li>• El jugador corre <span class="gold">más rápido (+100% y mínimo +5</span> sobre enemigos).</li>
              <li>• Vida jugador = <span class="gold">500</span>. Barra de vida debajo del personaje y en HUD.</li>
              <li>• Puntuación y tiempo se guardan en: <span class="dim">~/ArenaScores.csv</span></li>
            </ul>
          </div>

          <div class="bloque">
            <h2>ENEMIGOS (base)</h2>
            <ul>
              <li>• <span class="gold">Mush / Slime</span> — cuerpo a cuerpo (daño por contacto).</li>
              <li>• <span class="gold">Bat / Icebat</span> — a distancia (disparan proyectiles).</li>
              <li>• <span class="gold">Evil Cube</span> — pesado (más vida).</li>
            </ul>
          </div>

          <div class="bloque">
            <h2>JEFE FINAL</h2>
            <ul>
              <li>• Mucha vida, se acerca a ti, <span class="gold">dispara dirigido</span> y en <span class="gold">ráfagas</span>.</li>
            </ul>
          </div>
        </body>
        </html>
        """;
    }

    /** Label con sombra suave para resaltar el título */
    static class ShadowLabel extends JLabel {
        public ShadowLabel(String text) { super(text); setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            // sombra
            g2.setColor(new Color(0,0,0,160));
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(getText()))/2;
            int y = (getHeight() + fm.getAscent() - fm.getDescent())/2;
            g2.drawString(getText(), x+2, y+2);
            // texto
            g2.setColor(getForeground());
            g2.drawString(getText(), x, y);
            g2.dispose();
        }
        @Override public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(Math.max(d.width, 300), d.height + 12);
        }
    }

    /** Panel de vidrio translúcido con borde dorado (para resaltar el contenido) */
    static class RoundedGlassPanel extends JPanel {
        RoundedGlassPanel() { setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = 24;
            // fondo translúcido
            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            // borde dorado
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(new Color(255, 215, 0, 200));
            g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}

package Menu;

import Player.GameSettings;

import javax.swing.*;
import java.awt.*;

public class MenuDificultadPanel extends JPanel {

    private static final String F1 = "/Menu/imagen/Jefe.gif";
    private static final String F2 = "/Menu/imagen/fondo.png";
    private static final String F3 = "/Menu/imagen/Enemigos/Calavoso2.png";

    public MenuDificultadPanel(MenuPrincipal frame) {
        setLayout(new BorderLayout());

        String fondoRuta = MenuPrincipal.firstExisting(F1, F2, F3);
        JPanel fondo = new MenuPrincipal.PanelFondo(fondoRuta);
        fondo.setLayout(new GridBagLayout());
        add(fondo, BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.insets = new Insets(18, 18, 18, 18);
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel glass = new JPanel() {
            { setOpaque(false); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int arc = 22;
                g2.setColor(new Color(0,0,0,170));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                g2.setColor(new Color(255,215,0,200));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, arc, arc);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        glass.setLayout(new GridBagLayout());
        glass.setPreferredSize(new Dimension(720, 360));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = 0; gc.insets = new Insets(12, 12, 12, 12);

        JLabel titulo = new JLabel("Selecciona la Dificultad", SwingConstants.CENTER);
        titulo.setFont(new Font("Consolas", Font.BOLD, 32));
        titulo.setForeground(new Color(255,215,0));

        JButton btnNormal = MenuPrincipal.boton("Normal");
        JButton btnDificil = MenuPrincipal.boton("Difícil");
        JButton btnVolver = MenuPrincipal.boton("Volver");

        // --- TODOS IGUALES ---
        Dimension BTN = new Dimension(280, 56);
        JButton[] all = { btnNormal, btnDificil, btnVolver };
        for (JButton b : all) {
            b.setPreferredSize(BTN);
            b.setMinimumSize(BTN);
            b.setMaximumSize(BTN);
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            b.setHorizontalAlignment(SwingConstants.CENTER);
        }

        JPanel col = new JPanel(); col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        col.add(titulo); col.add(Box.createVerticalStrut(18));
        col.add(btnNormal); col.add(Box.createVerticalStrut(12));
        col.add(btnDificil); col.add(Box.createVerticalStrut(24));
        col.add(btnVolver);

        glass.add(col, gc);
        fondo.add(glass, gbc);

        // Acciones
        btnNormal.addActionListener(e -> {
            GameSettings settings = GameSettings.fromDificultad("Media"); // "Normal" → "Media"
            frame.setContentPane(new MenuPersonajesPanel(frame, settings));
            frame.revalidate(); frame.repaint();
        });
        btnDificil.addActionListener(e -> {
            GameSettings settings = GameSettings.fromDificultad("Difícil");
            frame.setContentPane(new MenuPersonajesPanel(frame, settings));
            frame.revalidate(); frame.repaint();
        });

        btnVolver.addActionListener(e -> {
            frame.setContentPane(new MenuPrincipal.PanelFondo(MenuPrincipal.firstExisting(F1, F2, F3)));
            new MenuPrincipal().setVisible(true);
            frame.dispose();
        });
    }
}

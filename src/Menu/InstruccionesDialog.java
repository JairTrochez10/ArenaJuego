package Menu;

import javax.swing.*;
import java.awt.*;

public class InstruccionesDialog {

    private static JButton uiButton(String txt) {
        JButton b = new JButton(txt);
        b.setFont(new Font("Consolas", Font.BOLD, 20));
        b.setForeground(Color.WHITE);
        b.setBackground(new Color(0, 0, 0, 200));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 215, 0, 200), 2, true),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static void mostrar(JFrame owner) {
        JDialog d = new JDialog(owner, "Instrucciones", true);
        d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JTextArea ta = new JTextArea();
        ta.setEditable(false);
        ta.setOpaque(false);
        ta.setFont(new Font("Consolas", Font.PLAIN, 16));
        ta.setText("""
                CONTROLES
                - Mover: W / A / S / D
                - Disparar: SPACE
                - Reiniciar tras morir: R (atajo para testers)
                - Salir: ESC

                OBJETIVO
                - Sobrevive y consigue la mejor puntuación.
                - Cada ronda aumentan velocidad/cantidad.
                - Cada 3 rondas aparece el JEFE FINAL.

                ESTADÍSTICAS ENEMIGOS (base)
                - Mush / Slime (Melee): contacto, vel = 3
                - Bat / Icebat (Rango): disparan, vel = 3
                - Evil Cube (Tanque): más vida, vel = 3

                JEFE FINAL (base)
                - Mucha vida, persigue, tiro dirigido y ráfagas, vel = 3

                NOTA
                - Jugador y enemigos comparten velocidad base (3),
                  y se incrementa por ronda (+10% jugador, +15% enemigos, +14% jefe).
                """);

        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        content.add(new JScrollPane(ta), BorderLayout.CENTER);

        JButton cerrar = uiButton("Cerrar");
        cerrar.addActionListener(e -> d.dispose());
        JPanel south = new JPanel();
        south.add(cerrar);
        content.add(south, BorderLayout.SOUTH);

        d.setContentPane(content);
        d.setSize(640, 520);
        d.setLocationRelativeTo(owner);
        d.setVisible(true);
    }
}

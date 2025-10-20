package Menu;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ScoreboardDialog {

    static class Score {
        String fecha;
        String jugador;
        String dificultad;
        int puntos;
        long segundos;
        String estado;
    }

    private static String fmtTime(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) return String.format("%dh %02dm %02ds", h, m, s);
        return String.format("%02dm %02ds", m, s);
    }

    private static List<Score> loadScores() {
        List<Score> list = new ArrayList<>();
        try {
            String home = System.getProperty("user.home");
            File f = new File(home + "/ArenaScores.csv");
            if (!f.exists()) return list;
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String ln;
                while ((ln = br.readLine()) != null) {
                    String[] c = ln.split(",", -1);
                    if (c.length < 6) continue;
                    Score s = new Score();
                    s.fecha = c[0].trim();
                    s.jugador = c[1].trim();
                    s.dificultad = c[2].trim();
                    try { s.puntos = Integer.parseInt(c[3].trim()); } catch (Exception ignore) { s.puntos = 0; }
                    try { s.segundos = Long.parseLong(c[4].trim()); } catch (Exception ignore) { s.segundos = 0; }
                    s.estado = c[5].trim();
                    list.add(s);
                }
            }
        } catch (Exception ignore) {}
        return list;
    }

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
        List<Score> scores = loadScores();
        scores.sort(Comparator.<Score>comparingInt(s -> -s.puntos)
                .thenComparingLong(s -> s.segundos));

        String[] cols = {"#", "Jugador", "Dificultad", "Puntuación", "Tiempo", "Fecha"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };
        int rank = 1;
        for (Score s : scores) {
            model.addRow(new Object[] { rank++, s.jugador, s.dificultad, s.puntos, fmtTime(s.segundos), s.fecha });
            if (rank > 50) break;
        }

        JTable table = new JTable(model);
        table.setRowHeight(24);
        table.getTableHeader().setReorderingAllowed(false);

        JDialog d = new JDialog(owner, "🏆 Puntuaciones", true);
        d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        root.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton cerrar = uiButton("Cerrar");
        cerrar.addActionListener(e -> d.dispose());
        JPanel south = new JPanel();
        south.add(cerrar);
        root.add(south, BorderLayout.SOUTH);

        d.setContentPane(root);
        d.setSize(720, 520);
        d.setLocationRelativeTo(owner);
        d.setVisible(true);
    }
}

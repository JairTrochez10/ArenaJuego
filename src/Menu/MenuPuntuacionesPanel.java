package Menu;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * Menú de Puntuaciones (mejorado) con:
 *  - Fondo a pantalla completa (elige el primero que exista).
 *  - Panel "glass" con borde dorado.
 *  - Tabla estilizada con zebra striping y header custom.
 *  - Botones: Volver | Eliminar seleccionado | Eliminar todos | Recargar.
 *
 * CSV: ~/ArenaScores.csv
 * Formato por línea: fecha, jugador, dificultad, puntos, segundos, estado
 *
 * Mostramos MEJOR marca por jugador (máx. puntos; en empate, menor tiempo).
 * "Eliminar seleccionado" borra TODAS las partidas de ese jugador en el CSV.
 * "Eliminar todos" vacía el CSV.
 *
 * Ahora la tabla también muestra la columna "Resultado"
 * (GAME_OVER o VICTORY) y resalta las victorias en dorado suave.
 */
public class MenuPuntuacionesPanel extends JPanel {

    // Fondos (ajusta si cambian de ruta)
    private static final String F1 = "/Imagenes/FondoC.jpg";
    private static final String F2 = "/Menu/imagen/Jefe.gif";
    private static final String F3 = "/Menu/imagen/Enemigos/Calavoso2.png";

    private final JFrame frame;

    private JTable table;
    private DefaultTableModel model;
    private JScrollPane scrollPane; // guardamos el scroll para estilar sin NPE

    public MenuPuntuacionesPanel(JFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());

        // Fondo + layout centrado
        String fondoRuta = MenuPrincipal.firstExisting(F1, F2, F3);
        JPanel fondo = new MenuPrincipal.PanelFondo(fondoRuta);
        fondo.setLayout(new GridBagLayout());
        add(fondo, BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.insets = new Insets(22, 22, 22, 22);
        gbc.anchor = GridBagConstraints.CENTER;

        // Contenedor "glass"
        JPanel glass = new RoundedGlassPanel();
        glass.setLayout(new BorderLayout());
        glass.setPreferredSize(new Dimension(980, 580));
        glass.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        // Cabecera
        JLabel titulo = new JLabel("🏆 Puntuaciones", SwingConstants.CENTER);
        titulo.setFont(new Font("Consolas", Font.BOLD, 32));
        titulo.setForeground(new Color(255, 215, 0)); // dorado
        JPanel header = new JPanel(new BorderLayout()) { @Override public boolean isOpaque(){ return false; } };
        header.add(titulo, BorderLayout.CENTER);

        // Tabla + ScrollPane
        model = buildModel();
        table = new JTable(model);
        scrollPane = new JScrollPane(table);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        styleTable(table, scrollPane);

        // Botones
        JButton btnVolver = MenuPrincipal.boton("Volver");
        JButton btnEliminarSel = MenuPrincipal.boton("Eliminar seleccionado");
        JButton btnEliminarTodos = MenuPrincipal.boton("Eliminar todos");
        JButton btnRecargar = MenuPrincipal.boton("Recargar");

        Dimension BTN = new Dimension(240, 52);
        btnVolver.setPreferredSize(BTN);
        btnEliminarSel.setPreferredSize(BTN);
        btnEliminarTodos.setPreferredSize(BTN);
        btnRecargar.setPreferredSize(BTN);

        JPanel actions = new JPanel() { @Override public boolean isOpaque(){ return false; } };
        actions.add(btnVolver);
        actions.add(btnEliminarSel);
        actions.add(btnEliminarTodos);
        actions.add(btnRecargar);

        // Añadir al "glass"
        glass.add(header, BorderLayout.NORTH);
        glass.add(scrollPane, BorderLayout.CENTER);
        glass.add(actions, BorderLayout.SOUTH);

        // Columna centrada
        JPanel columna = new JPanel();
        columna.setOpaque(false);
        columna.setLayout(new BoxLayout(columna, BoxLayout.Y_AXIS));
        columna.add(Box.createVerticalGlue());
        columna.add(glass);
        columna.add(Box.createVerticalGlue());

        fondo.add(columna, gbc);

        // Listeners
        btnVolver.addActionListener(e -> {
            frame.setContentPane(new MenuPrincipal.PanelFondo(MenuPrincipal.firstExisting(F1, F2, F3)));
            new MenuPrincipal().setVisible(true);
            frame.dispose();
        });

        btnRecargar.addActionListener(e -> refreshModel());

        btnEliminarSel.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Selecciona un jugador en la tabla.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String jugador = String.valueOf(table.getValueAt(row, 1));
            int op = JOptionPane.showConfirmDialog(this,
                    "¿Eliminar TODOS los registros del jugador \"" + jugador + "\"?",
                    "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (op == JOptionPane.YES_OPTION) {
                boolean ok = deletePlayerFromCSV(jugador);
                if (!ok) {
                    JOptionPane.showMessageDialog(this, "No se pudo eliminar los registros.", "Error", JOptionPane.ERROR_MESSAGE);
                }
                refreshModel();
            }
        });

        btnEliminarTodos.addActionListener(e -> {
            int op = JOptionPane.showConfirmDialog(this,
                    "Esto eliminará TODOS los registros del archivo de puntuaciones.\n¿Deseas continuar?",
                    "Eliminar todos", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (op == JOptionPane.YES_OPTION) {
                boolean ok = clearCSV();
                if (!ok) {
                    JOptionPane.showMessageDialog(this, "No se pudo limpiar el archivo.", "Error", JOptionPane.ERROR_MESSAGE);
                }
                refreshModel();
            }
        });
    }

    // --------- Construcción del modelo (mejor marca por jugador) ---------
    private DefaultTableModel buildModel() {
        // Se agrega columna "Resultado"
        String[] cols = {"#", "Jugador", "Dificultad", "Puntuación", "Tiempo", "Resultado"};
        DefaultTableModel m = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        Map<String, Score> bestByPlayer = new HashMap<>();
        for (Score s : loadScores()) {
            Score cur = bestByPlayer.get(s.jugador);
            if (cur == null) bestByPlayer.put(s.jugador, s);
            else if (s.puntos > cur.puntos || (s.puntos == cur.puntos && s.segundos < cur.segundos)) {
                // misma lógica de "mejor marca", el estado (GAME_OVER/VICTORY) no cambia el ranking,
                // pero sí se mostrará en la tabla.
                bestByPlayer.put(s.jugador, s);
            }
        }
        List<Score> list = new ArrayList<>(bestByPlayer.values());
        list.sort(Comparator.<Score>comparingInt(sc -> -sc.puntos).thenComparingLong(sc -> sc.segundos));

        int rank = 1;
        for (Score s : list) {
            m.addRow(new Object[]{
                    rank++,
                    s.jugador,
                    s.dificultad,
                    s.puntos,
                    fmtTime(s.segundos),
                    s.estado // GAME_OVER o VICTORY
            });
            if (rank > 100) break; // por si hay muchísimos
        }
        return m;
    }

    private void refreshModel() {
        DefaultTableModel m = buildModel();
        table.setModel(m);
        model = m;
        styleTable(table, scrollPane); // re-aplica estilo
        revalidate();
        repaint();
    }

    // --------- Estilizar tabla ---------
    private void styleTable(JTable t, JScrollPane sp) {
        t.setRowHeight(26);
        t.setFont(new Font("Consolas", Font.PLAIN, 16));
        t.setForeground(Color.WHITE);
        t.setOpaque(false);
        t.setFillsViewportHeight(true);

        if (sp != null) {
            sp.setOpaque(false);
            if (sp.getViewport() != null) sp.getViewport().setOpaque(false);
        }

        // Header
        JTableHeader th = t.getTableHeader();
        th.setDefaultRenderer(new HeaderRenderer(th.getDefaultRenderer()));
        th.setReorderingAllowed(false);
        th.setPreferredSize(new Dimension(th.getPreferredSize().width, 32));

        // Zebra striping + resaltar VICTORY
        DefaultTableCellRenderer zebra = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (c instanceof JComponent jc) jc.setOpaque(false);
                setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

                // Centrar columnas numéricas y de resultado
                if (column == 0 || column == 3 || column == 4 || column == 5) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }

                setForeground(Color.WHITE);

                String resultado = "";
                int colResultado = 5;
                if (table.getColumnCount() > colResultado) {
                    Object resVal = table.getValueAt(row, colResultado);
                    resultado = resVal == null ? "" : resVal.toString().trim();
                }

                if (isSelected) {
                    setBackground(new Color(255, 215, 0, 120));
                    setOpaque(true);
                } else {
                    // Si es victoria, fondo dorado suave
                    if ("VICTORY".equalsIgnoreCase(resultado)) {
                        setBackground(new Color(255, 215, 0, 55));
                        setOpaque(true);
                    } else {
                        // Zebra normal
                        if (row % 2 == 0) {
                            setBackground(new Color(255, 255, 255, 28));
                            setOpaque(true);
                        } else {
                            setOpaque(false);
                        }
                    }
                }
                return this;
            }
        };
        for (int i = 0; i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setCellRenderer(zebra);
        }

        // Anchuras sugeridas
        t.getColumnModel().getColumn(0).setPreferredWidth(40);   // #
        t.getColumnModel().getColumn(1).setPreferredWidth(240);  // Jugador
        t.getColumnModel().getColumn(2).setPreferredWidth(120);  // Dificultad
        t.getColumnModel().getColumn(3).setPreferredWidth(120);  // Puntuación
        t.getColumnModel().getColumn(4).setPreferredWidth(120);  // Tiempo
        t.getColumnModel().getColumn(5).setPreferredWidth(120);  // Resultado
    }

    static class HeaderRenderer implements TableCellRenderer {
        private final TableCellRenderer delegate;
        HeaderRenderer(TableCellRenderer d){ this.delegate = d; }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component c = delegate.getTableCellRendererComponent(table, value, false, false, row, column);
            JLabel lbl = (c instanceof JLabel) ? (JLabel)c : new JLabel(String.valueOf(value), SwingConstants.CENTER);
            lbl.setOpaque(true);
            lbl.setBackground(new Color(0,0,0,180));
            lbl.setForeground(new Color(255, 215, 0));
            lbl.setFont(new Font("Consolas", Font.BOLD, 16));
            lbl.setBorder(BorderFactory.createMatteBorder(0,0,1,0,new Color(255,215,0,140)));
            lbl.setHorizontalAlignment(column == 1 ? SwingConstants.LEFT : SwingConstants.CENTER);
            return lbl;
        }
    }

    // --------- Utilidades de CSV ---------
    private static File csvFile() { return new File(System.getProperty("user.home") + "/ArenaScores.csv"); }

    private static boolean clearCSV() {
        try (FileWriter fw = new FileWriter(csvFile(), false)) {
            // deja el archivo vacío
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /** Elimina TODAS las partidas de un jugador (coincidencia exacta, case-sensitive). */
    private static boolean deletePlayerFromCSV(String jugador) {
        try {
            File f = csvFile();
            if (!f.exists()) return true;

            List<String> keep = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String ln;
                while ((ln = br.readLine()) != null) {
                    String[] c = ln.split(",", -1);
                    if (c.length < 2) continue;
                    String name = c[1].trim();
                    if (!name.equals(jugador)) { // cambiar a equalsIgnoreCase si quieres insensible a mayúsculas
                        keep.add(ln);
                    }
                }
            }
            try (FileWriter fw = new FileWriter(f, false)) {
                for (String k : keep) fw.write(k + System.lineSeparator());
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // ==== Lectura CSV -> mejor marca por jugador ====
    static class Score {
        String jugador, dificultad, estado;
        int puntos;
        long segundos;
    }

    private static List<Score> loadScores() {
        List<Score> list = new ArrayList<>();
        try {
            File f = csvFile();
            if (!f.exists()) return list;
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String ln;
                while ((ln = br.readLine()) != null) {
                    String[] c = ln.split(",", -1);
                    if (c.length < 6) continue;
                    Score s = new Score();
                    s.jugador    = c[1].trim();
                    s.dificultad = c[2].trim();
                    try { s.puntos   = Integer.parseInt(c[3].trim()); } catch (Exception ignore) { s.puntos = 0; }
                    try { s.segundos = Long.parseLong(c[4].trim()); }  catch (Exception ignore) { s.segundos = 0; }
                    s.estado = c[5].trim(); // GAME_OVER o VICTORY
                    list.add(s);
                }
            }
        } catch (Exception ignore) {}
        return list;
    }

    private static String fmtTime(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) return String.format("%dh %02dm %02ds", h, m, s);
        return String.format("%02dm %02ds", m, s);
    }

    // --------- Panel “glass” bonito ---------
    static class RoundedGlassPanel extends JPanel {
        RoundedGlassPanel(){ setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = 22;
            g2.setColor(new Color(0, 0, 0, 170));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(new Color(255, 215, 0, 200));
            g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}

package Menu;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * 🏆 MenuPuntuacionesPanel
 *
 * - Tabla de puntuaciones sin columna de tiempo.
 * - Ordena automáticamente por puntuación (mayor primero).
 * - Protección por contraseña "2007" antes de eliminar.
 * - Fondo y estilos visuales dorados.
 */
public class MenuPuntuacionesPanel extends JPanel {

    private static final String F1 = "/Imagenes/FondoC.jpg";
    private static final String F2 = "/Menu/imagen/Jefe.gif";
    private static final String F3 = "/Menu/imagen/Enemigos/Calavoso2.png";

    private final JFrame frame;
    private JTable table;
    private DefaultTableModel model;
    private JScrollPane scrollPane;

    public MenuPuntuacionesPanel(JFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());

        String fondoRuta = MenuPrincipal.firstExisting(F1, F2, F3);
        JPanel fondo = new MenuPrincipal.PanelFondo(fondoRuta);
        fondo.setLayout(new GridBagLayout());
        add(fondo, BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.insets = new Insets(22, 22, 22, 22);
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel glass = new RoundedGlassPanel();
        glass.setLayout(new BorderLayout());
        glass.setPreferredSize(new Dimension(900, 560));
        glass.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JLabel titulo = new JLabel("🏆 Puntuaciones", SwingConstants.CENTER);
        titulo.setFont(new Font("Consolas", Font.BOLD, 32));
        titulo.setForeground(new Color(255, 215, 0));
        JPanel header = new JPanel(new BorderLayout()) {
            @Override public boolean isOpaque() { return false; }
        };
        header.add(titulo, BorderLayout.CENTER);

        model = buildModel();
        table = new JTable(model);
        scrollPane = new JScrollPane(table);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        styleTable(table, scrollPane);

        // 🔽 Orden automático por puntuación descendente
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        sorter.setSortKeys(Collections.singletonList(new RowSorter.SortKey(3, SortOrder.DESCENDING)));
        sorter.sort();

        JButton btnVolver = MenuPrincipal.boton("Volver");
        JButton btnEliminarSel = MenuPrincipal.boton("Eliminar seleccionado");
        JButton btnEliminarTodos = MenuPrincipal.boton("Eliminar todos");
        JButton btnRecargar = MenuPrincipal.boton("Recargar");

        Dimension BTN = new Dimension(230, 50);
        btnVolver.setPreferredSize(BTN);
        btnEliminarSel.setPreferredSize(BTN);
        btnEliminarTodos.setPreferredSize(BTN);
        btnRecargar.setPreferredSize(BTN);

        JPanel actions = new JPanel() { @Override public boolean isOpaque(){ return false; } };
        actions.add(btnVolver);
        actions.add(btnEliminarSel);
        actions.add(btnEliminarTodos);
        actions.add(btnRecargar);

        glass.add(header, BorderLayout.NORTH);
        glass.add(scrollPane, BorderLayout.CENTER);
        glass.add(actions, BorderLayout.SOUTH);

        JPanel columna = new JPanel();
        columna.setOpaque(false);
        columna.setLayout(new BoxLayout(columna, BoxLayout.Y_AXIS));
        columna.add(Box.createVerticalGlue());
        columna.add(glass);
        columna.add(Box.createVerticalGlue());

        fondo.add(columna, gbc);

        // Botones principales
        btnVolver.addActionListener(e -> {
            frame.setContentPane(new MenuPrincipal.PanelFondo(MenuPrincipal.firstExisting(F1, F2, F3)));
            new MenuPrincipal().setVisible(true);
            frame.dispose();
        });

        btnRecargar.addActionListener(e -> refreshModel());

        // 🔐 Eliminar seleccionado (con contraseña)
        btnEliminarSel.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Selecciona un jugador en la tabla.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String clave = solicitarClave("Ingrese la contraseña para eliminar puntuaciones:");
            if (clave == null) return;
            if (!"2007".equals(clave)) {
                JOptionPane.showMessageDialog(this, "Contraseña incorrecta.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String jugador = String.valueOf(table.getValueAt(row, 1));
            int op = JOptionPane.showConfirmDialog(this,
                    "¿Eliminar TODOS los registros del jugador \"" + jugador + "\"?",
                    "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (op == JOptionPane.YES_OPTION) {
                boolean ok = deletePlayerFromCSV(jugador);
                if (!ok)
                    JOptionPane.showMessageDialog(this, "No se pudo eliminar los registros.", "Error", JOptionPane.ERROR_MESSAGE);
                else
                    JOptionPane.showMessageDialog(this, "Registros de " + jugador + " eliminados correctamente.");
                refreshModel();
            }
        });

        // 🔐 Eliminar todos (con contraseña)
        btnEliminarTodos.addActionListener(e -> {
            String clave = solicitarClave("Ingrese la contraseña para eliminar todas las puntuaciones:");
            if (clave == null) return;
            if (!"2007".equals(clave)) {
                JOptionPane.showMessageDialog(this, "Contraseña incorrecta.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int op = JOptionPane.showConfirmDialog(this,
                    "Esto eliminará TODOS los registros del archivo de puntuaciones.\n¿Deseas continuar?",
                    "Eliminar todos", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (op == JOptionPane.YES_OPTION) {
                boolean ok = clearCSV();
                if (!ok)
                    JOptionPane.showMessageDialog(this, "No se pudo limpiar el archivo.", "Error", JOptionPane.ERROR_MESSAGE);
                else
                    JOptionPane.showMessageDialog(this, "¡Todas las puntuaciones han sido eliminadas!");
                refreshModel();
            }
        });
    }

    private String solicitarClave(String mensaje) {
        JPasswordField pass = new JPasswordField();
        int option = JOptionPane.showConfirmDialog(
                this,
                new Object[]{mensaje, pass},
                "Verificación",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (option == JOptionPane.OK_OPTION) {
            return new String(pass.getPassword());
        }
        return null;
    }

    // ---- Tabla sin columna de tiempo ----
    private DefaultTableModel buildModel() {
        String[] cols = {"#", "Jugador", "Dificultad", "Puntuación", "Resultado"};
        DefaultTableModel m = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        Map<String, Score> bestByPlayer = new HashMap<>();
        for (Score s : loadScores()) {
            Score cur = bestByPlayer.get(s.jugador);
            if (cur == null) bestByPlayer.put(s.jugador, s);
            else if (s.puntos > cur.puntos) bestByPlayer.put(s.jugador, s);
        }

        List<Score> list = new ArrayList<>(bestByPlayer.values());
        list.sort(Comparator.<Score>comparingInt(sc -> -sc.puntos));

        int rank = 1;
        for (Score s : list) {
            m.addRow(new Object[]{rank++, s.jugador, s.dificultad, s.puntos, s.estado});
            if (rank > 100) break;
        }
        return m;
    }

    private void refreshModel() {
        DefaultTableModel m = buildModel();
        table.setModel(m);
        model = m;
        styleTable(table, scrollPane);

        // mantener el orden descendente por puntuación
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(m);
        table.setRowSorter(sorter);
        sorter.setSortKeys(Collections.singletonList(new RowSorter.SortKey(3, SortOrder.DESCENDING)));
        sorter.sort();

        revalidate();
        repaint();
    }

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

        JTableHeader th = t.getTableHeader();
        th.setDefaultRenderer(new HeaderRenderer(th.getDefaultRenderer()));
        th.setReorderingAllowed(false);
        th.setPreferredSize(new Dimension(th.getPreferredSize().width, 32));

        DefaultTableCellRenderer zebra = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (c instanceof JComponent jc) jc.setOpaque(false);
                setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                if (column == 0 || column == 3 || column == 4)
                    setHorizontalAlignment(SwingConstants.CENTER);
                else setHorizontalAlignment(SwingConstants.LEFT);
                setForeground(Color.WHITE);
                String resultado = "";
                if (table.getColumnCount() > 4) {
                    Object resVal = table.getValueAt(row, 4);
                    resultado = resVal == null ? "" : resVal.toString().trim();
                }
                if (isSelected) {
                    setBackground(new Color(255, 215, 0, 120));
                    setOpaque(true);
                } else if ("VICTORY".equalsIgnoreCase(resultado)) {
                    setBackground(new Color(255, 215, 0, 55));
                    setOpaque(true);
                } else if (row % 2 == 0) {
                    setBackground(new Color(255, 255, 255, 28));
                    setOpaque(true);
                } else setOpaque(false);
                return this;
            }
        };
        for (int i = 0; i < t.getColumnCount(); i++)
            t.getColumnModel().getColumn(i).setCellRenderer(zebra);
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
            lbl.setForeground(new Color(255,215,0));
            lbl.setFont(new Font("Consolas", Font.BOLD, 16));
            lbl.setBorder(BorderFactory.createMatteBorder(0,0,1,0,new Color(255,215,0,140)));
            lbl.setHorizontalAlignment(column == 1 ? SwingConstants.LEFT : SwingConstants.CENTER);
            return lbl;
        }
    }

    private static File csvFile() { return new File(System.getProperty("user.home") + "/ArenaScores.csv"); }

    private static boolean clearCSV() {
        try (FileWriter fw = new FileWriter(csvFile(), false)) { return true; }
        catch (IOException e) { return false; }
    }

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
                    if (!c[1].trim().equals(jugador)) keep.add(ln);
                }
            }
            try (FileWriter fw = new FileWriter(f, false)) {
                for (String k : keep) fw.write(k + System.lineSeparator());
            }
            return true;
        } catch (IOException e) { return false; }
    }

    static class Score {
        String jugador, dificultad, estado;
        int puntos;
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
                    s.jugador = c[1].trim();
                    s.dificultad = c[2].trim();
                    try { s.puntos = Integer.parseInt(c[3].trim()); } catch (Exception ignore) {}
                    s.estado = c[5].trim();
                    list.add(s);
                }
            }
        } catch (Exception ignore) {}
        return list;
    }

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

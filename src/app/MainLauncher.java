package app;

import Menu.MenuBridge;
import Menu.MenuPrincipal;
import Player.GameSettings;
import Player.Juego;

import javax.swing.*;

public class MainLauncher {
    public static void main(String[] args) {
        // Conecta Menú → Juego
        MenuBridge.setListener((dificultad, heroe) -> {
            // Pide nombre ANTES de crear la arena (para guardar puntuación)
            String nombre = JOptionPane.showInputDialog(
                    null,
                    "Ingresa tu nombre para guardar la puntuación:",
                    "Nombre del jugador",
                    JOptionPane.QUESTION_MESSAGE
            );
            if (nombre == null || nombre.isBlank()) nombre = "Invitado";

            GameSettings settings = GameSettings.fromDificultad(dificultad);
            final String finalNombre = nombre;

            SwingUtilities.invokeLater(() -> {
                JFrame ventana = new JFrame("Arena - Dungeon Enemigos");
                Juego game = new Juego(settings, heroe, finalNombre);
                ventana.add(game);
                ventana.setUndecorated(true);
                ventana.setExtendedState(JFrame.MAXIMIZED_BOTH);
                ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                ventana.setVisible(true);
                game.requestFocusInWindow();
            });
        });

        // Inicia Menú Principal
        SwingUtilities.invokeLater(() -> {
            MenuPrincipal menu = new MenuPrincipal();
            menu.setVisible(true);
        });
    }
}

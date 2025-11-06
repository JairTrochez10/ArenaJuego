package app;

import Menu.MenuBridge;
import Menu.MenuPrincipal;
import Menu.NombreJugadorDialog;
import Player.GameSettings;
import Player.Juego;

import javax.swing.*;

public class MainLauncher {
    public static void main(String[] args) {
        // Conecta Menu -> Juego segun tu MenuBridge.Listener
        MenuBridge.setListener((dificultad, heroe) -> {
            GameSettings settings = GameSettings.fromDificultad(dificultad);

            // Dialogo con preview del heroe + nombre abajo
            String nombre = new NombreJugadorDialog(null, heroe).showDialog();
            if (nombre == null || nombre.isBlank()) nombre = "Invitado";
            final String finalNombre = nombre;
            final String finalHeroe  = (heroe == null || heroe.isBlank()) ? "Default" : heroe;

            SwingUtilities.invokeLater(() -> {
                JFrame ventana = new JFrame("Arena - Dungeon Enemigos");
                Juego game = new Juego(settings, finalHeroe, finalNombre);
                ventana.add(game);
                ventana.setUndecorated(true);
                ventana.setExtendedState(JFrame.MAXIMIZED_BOTH);
                ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                ventana.setVisible(true);
                game.requestFocusInWindow();
            });
        });

        // Inicia el menu principal
        SwingUtilities.invokeLater(() -> {
            MenuPrincipal menu = new MenuPrincipal();
            menu.setVisible(true);
        });
    }
}

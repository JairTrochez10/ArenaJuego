package app;

import Menu.MenuBridge;
import Menu.MenuPrincipal;
import Menu.NombreJugadorDialog;
import Player.GameFrameNiveles;
import Player.GameSettings;

import javax.swing.*;

public class MainLauncher {
    public static void main(String[] args) {

        // Conecta Menu -> Juego
        MenuBridge.setListener((dificultad, heroe) -> {
            GameSettings settings = GameSettings.fromDificultad(dificultad);

            String nombre = new NombreJugadorDialog(null, heroe).showDialog();
            if (nombre == null || nombre.isBlank()) nombre = "Invitado";

            final String finalNombre = nombre;
            final String finalHeroe  = (heroe == null || heroe.isBlank()) ? "Default" : heroe;

            SwingUtilities.invokeLater(() -> {
                GameFrameNiveles ventanaJuego = new GameFrameNiveles(settings, finalHeroe, finalNombre);
                ventanaJuego.setVisible(true);
            });
        });

        // Inicia el menú principal
        SwingUtilities.invokeLater(() -> {
            MenuPrincipal menu = new MenuPrincipal();
            menu.setVisible(true);
        });
    }
}

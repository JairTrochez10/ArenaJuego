package app;

import Player.*;
import Player.ArenaBase;
import Player.GameSettings;

import javax.swing.*;

/**
 * Arena4Test
 *
 * Programa simple para probar Arena4 directamente,
 * sin menú ni GameFrameNiveles.
 */
public class Test {

    public static void main(String[] args) {

        // Ajustes opcionales
        GameSettings settings = GameSettings.fromDificultad("Media");
        String heroe = "Larry";           // o tu héroe por defecto
        String nombreJugador = "Tester";  // nombre de prueba

        // Crear frame vacío
        JFrame frame = new JFrame("Test: Arena 4");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // ====== IMPORTANTE ======
        // Puedes cambiar Arena4 por otra para probar otras arenas.
        ArenaBase arena = new Arena3(settings, heroe, nombreJugador, null);

        // Inicializar con vida completa
        arena.iniciar(-1);

        frame.setContentPane(arena);

        // Pantalla grande (sin fullscreen)
        frame.setSize(1280, 720);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Dar foco al panel para teclas
        arena.requestFocusInWindow();
    }
}

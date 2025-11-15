package Player;

/**
 * Arena5
 *
 * Enemigos por ronda:
 *   R1: 14   R2: 19   R3: 24  (offset +4)
 * Fondo: /Imagenes/Arena5.jpg
 *
 * Al matar al jefe de esta arena,
 * GameFrameNiveles muestra la pantalla de VICTORIA.
 */
public class Arena5 extends ArenaBase {

    public Arena5(GameSettings settings,
                  String heroe,
                  String nombreJugador,
                  LevelListener listener) {
        super(
                settings,
                heroe,
                nombreJugador,
                listener,
                5,                        // número de arena
                "/Imagenes/Arena5.jpg"    // fondo de esta arena
        );
    }
}

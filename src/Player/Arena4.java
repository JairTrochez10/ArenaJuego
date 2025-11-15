package Player;

/**
 * Arena4
 *
 * Enemigos por ronda:
 *   R1: 13   R2: 18   R3: 23  (offset +3)
 * Fondo: /Imagenes/Arena4.jpg
 */
public class Arena4 extends ArenaBase {

    public Arena4(GameSettings settings,
                  String heroe,
                  String nombreJugador,
                  LevelListener listener) {
        super(
                settings,
                heroe,
                nombreJugador,
                listener,
                4,                        // número de arena
                "/Imagenes/Arena4.jpg"    // fondo de esta arena
        );
    }
}

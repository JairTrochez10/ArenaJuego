package Player;

/**
 * Arena3
 *
 * Enemigos por ronda:
 *   R1: 12   R2: 17   R3: 22  (offset +2)
 * Fondo: /Imagenes/Arena3.jpg
 */
public class Arena3 extends ArenaBase {

    public Arena3(GameSettings settings,
                  String heroe,
                  String nombreJugador,
                  LevelListener listener) {
        super(
                settings,
                heroe,
                nombreJugador,
                listener,
                3,                        // número de arena
                "/Imagenes/Arena3.jpg"    // fondo de esta arena
        );
    }
}

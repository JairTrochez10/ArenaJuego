package Player;

/**
 * Arena2
 *
 * Enemigos por ronda:
 *   R1: 11   R2: 16   R3: 21  (offset +1)
 * Fondo: /Imagenes/Arena2.jpg
 */
public class Arena2 extends ArenaBase {

    public Arena2(GameSettings settings,
                  String heroe,
                  String nombreJugador,
                  LevelListener listener) {
        super(
                settings,
                heroe,
                nombreJugador,
                listener,
                2,                        // número de arena
                "/Imagenes/Arena2.jpg"    // fondo de esta arena
        );
    }
}

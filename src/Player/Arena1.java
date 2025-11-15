package Player;

/**
 * Arena1
 *
 * Enemigos por ronda:
 *   R1: 10   R2: 15   R3: 20
 * Fondo: /Imagenes/Arena1.jpg
 */
public class Arena1 extends ArenaBase {

    public Arena1(GameSettings settings,
                  String heroe,
                  String nombreJugador,
                  LevelListener listener) {
        super(
                settings,          // configuración de dificultad
                heroe,             // héroe elegido
                nombreJugador,     // nombre del jugador
                listener,          // quien escucha cuando se completa la arena
                1,                 // número de arena (para el cálculo 10/15/20)
                "/Imagenes/Arena.jpg" // fondo de esta arena
        );
    }
}

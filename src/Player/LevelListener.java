package Player;

/**
 * LevelListener
 *
 * Interfaz para que una arena (ArenaBase / Arena1..Arena5)
 * pueda avisar a GameFrameNiveles cuando se termina un nivel
 * (3 rondas + jefe muerto).
 */
public interface LevelListener {

    /**
     * Se llama cuando el jugador termina todas las rondas + jefe
     * de una arena / nivel concreto.
     *
     * @param numeroArena Número de arena (1, 2, 3, ...).
     * @param puntuacion  Puntuación acumulada en ese momento.
     * @param vidaJugador Vida actual del jugador al terminar el nivel.
     */
    void onLevelComplete(int numeroArena, int puntuacion, int vidaJugador);
}

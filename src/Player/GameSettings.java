package Player;

/** Config esperada por tu Juego y Monstruo. */
public class GameSettings {
    public final String dificultadName;

    // Para enemigos
    public final double enemySpeedMul;    // velocidad base de enemigo (se multiplica por roundMulEnemy)
    public final double enemyHealthMul;   // vida base de enemigo
    public final double enemyDamageMul;   // daño base de enemigo

    // Para relación jugador vs enemigos
    public final double playerSpeedAdvantage; // factor > 1: jugador más rápido que enemigos

    // Para jefe
    public final double bossSpeedMul;     // multiplicador de velocidad del jefe (se multiplica por roundMulBoss)
    public final int bossBulletDamage;    // daño de las balas del jefe

    public GameSettings(String dificultadName,
                        double enemySpeedMul,
                        double enemyHealthMul,
                        double enemyDamageMul,
                        double playerSpeedAdvantage,
                        double bossSpeedMul,
                        int bossBulletDamage) {
        this.dificultadName = dificultadName;
        this.enemySpeedMul = enemySpeedMul;
        this.enemyHealthMul = enemyHealthMul;
        this.enemyDamageMul = enemyDamageMul;
        this.playerSpeedAdvantage = playerSpeedAdvantage;
        this.bossSpeedMul = bossSpeedMul;
        this.bossBulletDamage = bossBulletDamage;
    }

    /** Presets por nombre. */
    public static GameSettings fromDificultad(String dif) {
        String s = (dif == null ? "Media" : dif.trim().toLowerCase());
        switch (s) {
            case "facil": case "fácil": case "easy":
                return new GameSettings(
                        "Fácil",
                        0.90,   // enemySpeedMul
                        0.90,   // enemyHealthMul
                        0.90,   // enemyDamageMul
                        2.2,    // playerSpeedAdvantage (jugador ≈ 2.2x más rápido)
                        0.95,   // bossSpeedMul
                        10      // bossBulletDamage
                );
            case "dificil": case "difícil": case "hard":
                return new GameSettings(
                        "Difícil",
                        1.15,
                        1.20,
                        1.15,
                        1.8,
                        1.10,
                        16
                );
            case "media": case "normal": default:
                return new GameSettings(
                        "Media",
                        1.00,
                        1.00,
                        1.00,
                        2.0,
                        1.00,
                        14
                );
        }
    }
}

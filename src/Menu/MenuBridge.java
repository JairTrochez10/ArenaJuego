package Menu;

public final class MenuBridge {

    private MenuBridge() {}

    public interface Listener {
        void startGame(String dificultad, String heroe);
    }

    private static volatile Listener listener;

    public static void setListener(Listener l) {
        listener = l;
    }

    public static void dispatchStart(String dificultad, String heroe) {
        Listener l = listener;
        if (l != null) l.startGame(dificultad, heroe);
    }
}

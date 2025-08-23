package costumutils;

public class InitCheckClass {
    public static boolean initialized;

    static {
        initialized = true;
    }

    public static void reset() {
        initialized = false;
    }
}

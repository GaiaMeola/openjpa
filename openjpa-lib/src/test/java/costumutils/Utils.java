package costumutils;

/**
 * Utils per creare input validi e invalidi per i test di ClassUtil.toClass
 */
public class Utils {

    // =======================
    // STRINGHE VALIDE
    // =======================

    /** Classe interna valida */
    public static String validInnerClass() {
        return "javax.swing.JSpinner$DefaultEditor";
    }

    /** Classe normale con package */
    public static String validNormalClassWithPackage() {
        return "java.lang.String";
    }

    /** Classe normale senza package (primitiva) */
    public static String validPrimitiveClass() {
        return "int";
    }

    /** Classe array (oggetti) */
    public static String validArrayClassObject() {
        return "java.awt.Point[][]";
    }

    // =======================
    // STRINGHE NON VALIDE
    // =======================

    /** Stringa vuota */
    public static String emptyString() {
        return "";
    }

    /** Stringa non valida */
    public static String invalidBinaryName() {
        return "$%&gaia..invalid..binary*-+";
    }

    /** Stringa nulla */
    public static String nullString() {
        return null;
    }

    // =======================
    // BOOLEAN resolve
    // =======================

    public static Boolean resolveTrue() {
        return true;
    }

    public static Boolean resolveFalse() {
        return false;
    }

    // =======================
    // CLASSLOADER
    // =======================

    /** ClassLoader valido */
    public static ClassLoader validClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    /** ClassLoader non valido */
    public static ClassLoader invalidClassLoader() {
        return new ClassLoader() {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                throw new ClassNotFoundException("Cannot load " + name + " (invalid loader)");
            }
        };
    }


    /** ClassLoader nullo */
    public static ClassLoader nullClassLoader() {
        return null;
    }
}

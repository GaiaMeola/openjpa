package costumutils;

import static org.evosuite.shaded.org.mockito.ArgumentMatchers.anyString;
import static org.evosuite.shaded.org.mockito.Mockito.doAnswer;
import static org.evosuite.shaded.org.mockito.Mockito.spy;

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
        // Creo uno spy di un ClassLoader reale
        ClassLoader realLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader spyLoader = spy(realLoader);

        // Intercetto il metodo loadClass per lanciare eccezione sempre
        try {
            doAnswer(invocation -> {
                throw new ClassNotFoundException("Cannot load any class (mocked)");
            }).when(spyLoader).loadClass(anyString());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return spyLoader;
    }

    /** ClassLoader nullo */
    public static ClassLoader nullClassLoader() {
        return null;
    }
}

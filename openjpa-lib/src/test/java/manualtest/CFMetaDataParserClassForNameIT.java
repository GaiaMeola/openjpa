package manualtest;

import org.apache.openjpa.lib.util.ClassUtil;
import org.apache.openjpa.lib.meta.CFMetaDataParser;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class CFMetaDataParserClassForNameIT {

    // =========================
    // Caso 1: nome completo -> toClass chiamato direttamente
    // =========================
    @Test
    void testClassForName_fullName() {
        try (MockedStatic<ClassUtil> classUtilMock = mockStatic(ClassUtil.class)) {
            classUtilMock.when(() -> ClassUtil.toClass("java.lang.String", true, getClass().getClassLoader()))
                    .thenReturn(String.class);

            Class<?> result = CFMetaDataParser.classForName(
                    "java.lang.String", "ignored", true, getClass().getClassLoader());

            classUtilMock.verify(() -> ClassUtil.toClass("java.lang.String", true, getClass().getClassLoader()), times(1));
            assertEquals(String.class, result);
        }
    }

    // =========================
    // Caso 2: nome corto + package -> viene usato il package
    // =========================
    @Test
    void testClassForName_shortNameWithPackage() {
        try (MockedStatic<ClassUtil> classUtilMock = mockStatic(ClassUtil.class)) {
            classUtilMock.when(() -> ClassUtil.toClass("java.lang.Integer", true, getClass().getClassLoader()))
                    .thenReturn(Integer.class);

            Class<?> result = CFMetaDataParser.classForName(
                    "Integer", "java.lang", true, getClass().getClassLoader());

            classUtilMock.verify(() -> ClassUtil.toClass("java.lang.Integer", true, getClass().getClassLoader()), times(1));
            assertEquals(Integer.class, result);
        }
    }

    // =========================
    // Caso 3: classe inesistente -> ritorna null
    // =========================
    @Test
    void testClassForName_notFound() {
        try (MockedStatic<ClassUtil> classUtilMock = mockStatic(ClassUtil.class)) {
            // Tutti i tentativi ritornano null senza eccezione
            classUtilMock.when(() -> ClassUtil.toClass(anyString(), anyBoolean(), any()))
                    .thenReturn(null);

            Class<?> result = CFMetaDataParser.classForName(
                    "ClasseInesistente", "my.pkg", true, getClass().getClassLoader());

            assertNull(result);

            // Verifica che sia stato chiamato solo il tentativo principale: pkg + name
            classUtilMock.verify(() -> ClassUtil.toClass("my.pkg.ClasseInesistente", true, getClass().getClassLoader()), times(1));
            // Non ci sono altri fallback perché non vengono lanciati RuntimeException
        }
    }

    // =========================
    // Caso 4: nome corto senza package -> fallback sul nome stesso
    // =========================
    @Test
    void testClassForName_shortNameNoPackage() {
        try (MockedStatic<ClassUtil> classUtilMock = mockStatic(ClassUtil.class)) {
            // Il primo tentativo (String) va a buon fine
            classUtilMock.when(() -> ClassUtil.toClass("String", true, getClass().getClassLoader()))
                    .thenReturn(String.class);

            Class<?> result = CFMetaDataParser.classForName(
                    "String", null, true, getClass().getClassLoader());

            classUtilMock.verify(() -> ClassUtil.toClass("String", true, getClass().getClassLoader()), times(1));
            assertEquals(String.class, result);
        }
    }

    // =========================
    // Caso 5: nome corto + package, tentativo con package fallisce con eccezione -> fallback su nome
    // =========================
    @Test
    void testClassForName_fallbackOnException() {
        try (MockedStatic<ClassUtil> classUtilMock = mockStatic(ClassUtil.class)) {
            // Il primo tentativo con package lancia eccezione
            classUtilMock.when(() -> ClassUtil.toClass("java.util.ArrayList", true, getClass().getClassLoader()))
                    .thenThrow(new RuntimeException("simulated failure"));
            // Fallback sul nome corto va a buon fine
            classUtilMock.when(() -> ClassUtil.toClass("ArrayList", true, getClass().getClassLoader()))
                    .thenReturn(java.util.ArrayList.class);

            Class<?> result = CFMetaDataParser.classForName(
                    "ArrayList", "java.util", true, getClass().getClassLoader());

            classUtilMock.verify(() -> ClassUtil.toClass("java.util.ArrayList", true, getClass().getClassLoader()), times(1));
            classUtilMock.verify(() -> ClassUtil.toClass("ArrayList", true, getClass().getClassLoader()), times(1));
            assertEquals(java.util.ArrayList.class, result);
        }
    }
}
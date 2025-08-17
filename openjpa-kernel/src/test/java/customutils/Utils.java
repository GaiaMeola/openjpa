package customutils;

import org.apache.openjpa.util.CacheMap;

import java.lang.reflect.Field;
import java.util.Map;

import static org.evosuite.shaded.org.mockito.Mockito.*;

/**
 * Utils per costruire istanze valide e invalide di CacheMap
 * e chiavi.
 */
public class Utils {

    public static final Object VALID_KEY_IN_SOFT = "validKeyInSoft";
    public static final Object VALID_KEY_IN_PINNED_NON_NULL = "validKeyInPinnedNonNull";
    public static final Object VALID_KEY_IN_PINNED_NULL = "validKeyInPinnedNull";

    // =======================
    // CHIAVI VALIDE / INVALIDE
    // =======================

    /** Chiave valida: oggetto reale */
    public static Object validKey() {
        return "realKey";
    }

    /** Chiave invalida: oggetto reale con equals() che lancia eccezione */
    public static Object invalidKeyMock() {
        return new Object() {
            @Override
            public boolean equals(Object obj) {
                throw new RuntimeException("Invalid key equals() called");
            }

            @Override
            public int hashCode() {
                return 42;
            }
        };
    }

    public static Object NULL_KEY() {
        return null;
    }

    // =======================
    // CACHEMAP VALIDE
    // =======================

    public static CacheMap validCacheMapWithKeyInCache() {
        CacheMap map = new CacheMap(true);
        map.put(validKey(), "valueCache");
        return map;
    }

    public static CacheMap validCacheMapAlwaysSoft()  {
        // Creo una CacheMap reale (true = soft cache abilitata)
        CacheMap realMap = new CacheMap(true);

        // Creo uno spy su di essa
        CacheMap spyMap = spy(realMap);

        // Intercetto il metodo 'put' per forzare l'inserimento nella soft cache
        doAnswer(invocation -> {
            Object key = invocation.getArgument(0);
            Object value = invocation.getArgument(1);

            // Inserisco manualmente nella softMap interna
            putInSoftMap(spyMap, key, value);

            return null; // put() è void
        }).when(spyMap).put(any(), any());

        return spyMap;
    }

    public static CacheMap validCacheMapAlwaysPinned() {
        // Creo una CacheMap reale
        CacheMap realMap = new CacheMap(true);

        // Creo uno spy su di essa
        CacheMap spyMap = spy(realMap);

        // Intercetto il metodo 'put' per forzare l'inserimento nella pinnedMap
        doAnswer(invocation -> {
            Object key = invocation.getArgument(0);
            Object value = invocation.getArgument(1);

            // Inserisco manualmente nella pinnedMap usando reflection
            putInPinnedMap(spyMap, key, value);

            return null; // put() è void
        }).when(spyMap).put(any(), any());

        return spyMap;
    }

    // Metodo di utilità per inserire nella softMap usando reflection
    public static void putInSoftMap(CacheMap map, Object key, Object value) throws Exception {
        java.lang.reflect.Field softMapField = CacheMap.class.getDeclaredField("softMap");
        softMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<Object, Object> softMap = (java.util.Map<Object, Object>) softMapField.get(map);
        softMap.put(key, value);
    }

    // Metodo di utilità per inserire nella pinnedMap usando reflection
    public static void putInPinnedMap(CacheMap map, Object key, Object value) throws Exception {
        Field pinnedMapField = CacheMap.class.getDeclaredField("pinnedMap");
        pinnedMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Object, Object> pinnedMap = (Map<Object, Object>) pinnedMapField.get(map);
        pinnedMap.put(key, value);

        // Aggiorno anche _pinnedSize se il valore non è null
        Field pinnedSizeField = CacheMap.class.getDeclaredField("_pinnedSize");
        pinnedSizeField.setAccessible(true);
        int size = (int) pinnedSizeField.get(map);
        if (value != null) {
            pinnedSizeField.set(map, size + 1);
        }
    }

    public static CacheMap emptyValidCacheMap() {
        return new CacheMap(true);
    }

    // =======================
    // CACHEMAP INVALIDE
    // =======================
    public static CacheMap invalidCacheMap() {
        // Creiamo una cache con max=0, che non potrà contenere elementi
        return new CacheMap(true, 0, 100, 0.75f, 1);
    }
}

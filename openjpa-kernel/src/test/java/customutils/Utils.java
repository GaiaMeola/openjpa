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

    // =======================
    // CHIAVI DI TEST
    // =======================
    public static final Object VALID_KEY_IN_CACHE = "validKeyInCache";
    public static final Object VALID_KEY_IN_SOFT = "validKeyInSoft";
    public static final Object VALID_KEY_IN_PINNED_NON_NULL = "validKeyInPinnedNonNull";
    public static final Object VALID_KEY_IN_PINNED_NULL = "validKeyInPinnedNull";
    public static final Object VALID_KEY_NOT_PRESENT = "validKeyNotPresent";

    public static Object NULL_KEY() {
        return null;
    }

    // =======================
    // CHIAVI VALIDE / INVALIDE
    // =======================

    /** Chiave valida: oggetto reale */
    public static Object validKey() {
        return "realKey";
    }

    /** Chiave invalida: mock che lancia eccezione su equals() */
    public static Object invalidKeyMock() {
        Object key = mock(Object.class);
        when(key.hashCode()).thenReturn(42);
        when(key.equals(any())).thenThrow(new RuntimeException("Invalid key equals() called"));
        return key;
    }

    // =======================
    // CACHEMAP VALIDE
    // =======================

    public static CacheMap validCacheMapWithKeyInCache() {
        CacheMap map = new CacheMap(true);
        map.put(VALID_KEY_IN_CACHE, "valueCache");
        return map;
    }

    public static CacheMap validCacheMapWithKeyInSoft() {
        CacheMap map = new CacheMap(true);
        putInInternalMap(map, "softMap", VALID_KEY_IN_SOFT, "valueSoft");
        return map;
    }

    public static CacheMap validCacheMapWithKeyInPinnedNonNull() {
        CacheMap map = new CacheMap(true);
        putInInternalMap(map, "pinnedMap", VALID_KEY_IN_PINNED_NON_NULL, "valuePinned");
        return map;
    }

    public static CacheMap validCacheMapWithKeyInPinnedNull() {
        CacheMap map = new CacheMap(true);
        putInInternalMap(map, "pinnedMap", VALID_KEY_IN_PINNED_NULL, null);
        return map;
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


    // =======================
    // UTILITY PER MAPPE INTERNE
    // =======================
    public static void putInInternalMap(CacheMap map, String fieldName, Object key, Object value) {
        try {
            Field f = CacheMap.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Object, Object> internalMap = (Map<Object, Object>) f.get(map);
            internalMap.put(key, value);
        } catch (Exception e) {
            throw new RuntimeException("Errore inserimento in " + fieldName, e);
        }
    }
}

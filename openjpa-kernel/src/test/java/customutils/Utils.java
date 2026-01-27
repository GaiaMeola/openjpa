package customutils;

import org.apache.openjpa.util.CacheMap;

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
            public int hashCode() {
                throw new RuntimeException("Simulated Bad Key");
            }
        };
    }

    public static Object NULL_KEY() {
        return null;
    }

    // =======================
    // CACHEMAP VALIDE
    // =======================

    // Ritorna una cache con un elemento già dentro (Main Cache)
    public static CacheMap validCacheMapWithKeyInCache() {
        CacheMap map = new CacheMap(true);
        map.put(validKey(), "valueCache");
        return map;
    }

    public static CacheMap validCacheMapAlwaysSoft() {
        return new CacheMap(true, 4);
    }

    public static CacheMap validCacheMapAlwaysPinned() {
        // Black Box: Una CacheMap standard con spazio sufficiente
        // mantiene gli oggetti nella PinnedMap.
        return new CacheMap(true, 100);
    }

    /**
     * Sposta una chiave nella SoftMap tramite Eviction naturale.
     */
    public static void putInSoftMap(CacheMap map, Object key, Object value) {
        // Supponiamo che map sia stata creata con capacità 1
        map.put(key, value);         // Entra in pinned
        map.put("trigger", "extra"); // Sposta 'key' in softMap per fare spazio a 'trigger'
    }

    public static CacheMap emptyValidCacheMap() {
        return new CacheMap(true);
    }

    // =======================
    // CACHEMAP INVALIDE
    // =======================
    public static CacheMap invalidCacheMap() {
        // Configurazione P1: max=0 (capacità nulla), size=100 (mappa interna),
        // lru=true, load=0.75, concurrency=1
        return new CacheMap(true, 0, 100, 0.75f, 1);
    }
}

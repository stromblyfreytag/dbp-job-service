package com.trustwave.dbpjobservice.workflow.api.util;

import java.lang.reflect.ParameterizedType;

/**
 * <p>This class is a holder for objects that need to be periodically
 * re-created.</p>
 * <p>Typical examples are objects that are initialized or configured from
 * external modifiable source (configuration file, network, etc).</p>
 * <p>Creating such object every time it is needed may be too expensive
 * if object is heavily used by application. From the other hand, creating
 * instance of the object once is also not a good solution, because we loose
 * reconfiguration-on-the-fly capability.
 * </p>
 * <p>This class provides a compromise between pure dynamic and pure static
 * solutions:  object is considered up-to-date (not requires re-creation)
 * during limited time (e.g. several seconds), then it expires and will be
 * automatically re-created on the next request.
 * </p>
 * <p>Example:</p>
 * <pre>
 *   // Holder for MyConf object that is auto-recreated every 2 seconds:
 *   static AutoUpdatable&lt;MyConf&gt; myConfHolder =
 *      new AutoUpdatable&lt;MyConf&gt;( MyConf.class, 2 * 1000 );
 *   ...
 *   MyConf getMyConf() {
 *       return myConfHolder.get();
 *   }
 * </pre>
 *
 * @param <T> Type of auto-updatable object. Should have default constructor.
 * @author vlad
 */
public class AutoUpdatable<T> {
    private Class<T> clazz;
    private long expirationTimeMs;
    private long nextRecreateTimestamp = 0;
    private T instance = null;

    public AutoUpdatable(Class<T> clazz, long expirationTimeMs) {
        this.clazz = clazz;
        this.expirationTimeMs = expirationTimeMs;
    }

    // this constructor works only for classes explicitly extending AutoUpdatable!
    @SuppressWarnings("unchecked")
    protected AutoUpdatable(long expirationTimeMs) {
        this.clazz = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        this.expirationTimeMs = expirationTimeMs;
    }

    protected AutoUpdatable(long expirationTimeMs, Class<T> clazz) {
        this.clazz = clazz;
        this.expirationTimeMs = expirationTimeMs;
    }

    public synchronized T get() {
        if (System.currentTimeMillis() >= nextRecreateTimestamp) {
            instance = createInstance();
            nextRecreateTimestamp = System.currentTimeMillis() + expirationTimeMs;
        }
        return instance;
    }

    protected T createInstance() {
        try {
            return clazz.newInstance();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

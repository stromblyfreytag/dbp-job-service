package com.trustwave.dbpjobservice.workflow.light.db;

import java.lang.ref.SoftReference;

import com.googlecode.sarasvati.env.Env;
import com.googlecode.sarasvati.impl.MapEnv;

/**
 * Utility class for holding Sarasvati environment object,
 * with auto-conversion into a string and back
 */
public class EnvironmentHolder {
    private String envString = null;
    private SoftReference<Env> softEnvRef = null;
    private Env hardEnvRef = null;

    public EnvironmentHolder() {
    }

    public synchronized String getEnvString() {
        if (hardEnvRef != null) {
            // env was modified, synchronize:
            envString = LightUtils.encodeEnv(hardEnvRef);
            hardEnvRef = null;
        }
        return envString;
    }

    public synchronized void setEnvString(String envString) {
        this.envString = envString;
        softEnvRef = null;
        hardEnvRef = null;
    }

    public synchronized Env getEnv() {
        Env env = softEnvRef != null ? softEnvRef.get() : null;
        if (env == null) {
            env = createEnv();
            softEnvRef = new SoftReference<Env>(env);
        }
        return env;
    }

    private Env createEnv() {
        Env env = new MapEnv() {
            @Override
            public void setAttribute(final String name, final String value) {
                attributes.put(name, value);
                synchronized (EnvironmentHolder.this) {
                    hardEnvRef = this;
                }
            }

            @Override
            public void removeAttribute(final String name) {
                attributes.remove(name);
                synchronized (EnvironmentHolder.this) {
                    hardEnvRef = this;
                }
            }
        };

        if (envString != null) {
            LightUtils.decodeEnvString(envString, env);
            hardEnvRef = null;
        }
        return env;
    }

    public boolean isModified() {
        return hardEnvRef != null;
    }

}

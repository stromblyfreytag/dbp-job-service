package com.trustwave.dbpjobservice.workflow.light.engine;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.googlecode.sarasvati.env.Env;
import com.googlecode.sarasvati.impl.MapEnv;
import com.trustwave.dbpjobservice.impl.Messages;

/**
 * <p>This class serves as a holder for initial token set member environments,
 * and provides one-time retrieval of environment by index (get and remove).
 * </p>
 *
 * @author vlad
 */
public class InitialEnvironmentHandler {
    private Map<String, List<?>> initialEnv = new HashMap<String, List<?>>();
    private Set<Integer> removedIndexes = new HashSet<>(1);
    // This provides simple memory
    private int firstLiveIndex = 0;

    boolean hasMemberEnv(int index) {
        return index >= firstLiveIndex && !removedIndexes.contains(index);
    }

    int getFirstLiveIndex() {
        return firstLiveIndex;
    }

    public String getAttribute(int index, String name) {
        int ind = index - firstLiveIndex;
        List<?> values = initialEnv.get(name);
        Object value = (values != null ? values.get(ind) : null);
        return (value != null ? value.toString() : null);
    }

    public Collection<String> getAttributeNames() {
        return initialEnv.keySet();
    }

    public void setAttribute(String name, List<?> values) {
        if (firstLiveIndex > 0 || removedIndexes.size() > 0) {
            throw new IllegalStateException(Messages.getString("workflow.initialEnv.alreadyConsumed"));
        }
        initialEnv.put(name, new LinkedList<>(values));
    }

    public Env removeMemberEnv(int index) {
        Env env = new MapEnv();
        int ind = index - firstLiveIndex;

        for (Entry<String, List<?>> entry : initialEnv.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue().get(ind);
            env.setAttribute(name, value);

            if (ind == 0) {
                entry.getValue().remove(0);
            }
            else {
                entry.getValue().set(index, null);
            }
        }

        if (ind == 0) {
            ++firstLiveIndex;
        }
        else {
            removedIndexes.add(index);
        }

        return env;
    }

}

package com.trustwave.dbpjobservice.workflow.light.engine;

import com.googlecode.sarasvati.event.ExecutionEventType;
import com.googlecode.sarasvati.event.PersistedExecutionListener;
import com.trustwave.dbpjobservice.impl.Messages;

public class LightExecutionListener implements PersistedExecutionListener {
    private String type;
    private int mask;

    LightExecutionListener(Class<?> clazz, ExecutionEventType... eventTypes) {
        this.type = clazz.getName();
        this.mask = ExecutionEventType.toMask(eventTypes);
    }

    LightExecutionListener(String str) {
        int ind = str.indexOf('+');
        if (ind == -1) {
            throw new RuntimeException(Messages.getString("workflow.light.listner.invalid", str));
        }
        this.type = str.substring(0, ind);
        this.mask = Integer.valueOf(str.substring(ind + 1));
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public int getEventTypeMask() {
        return mask;
    }

    public String toString() {
        return type + "+" + mask;
    }

}

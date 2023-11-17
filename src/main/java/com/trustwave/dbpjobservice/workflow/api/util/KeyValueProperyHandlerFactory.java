package com.trustwave.dbpjobservice.workflow.api.util;

import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KeyValueProperyHandlerFactory extends PropertyHandlerFactory {
    private String keyProperty;
    private String valueProperty;
    private Set<String> skeys = new HashSet<String>();
    private Set<String> ciKeys = new HashSet<String>();

    public KeyValueProperyHandlerFactory(String keyProperty,
            String valueProperty,
            Collection<String> validKeys) {
        super();
        this.keyProperty = keyProperty;
        this.valueProperty = valueProperty;
        if (validKeys != null) {
            skeys.addAll(validKeys);
            for (String key : validKeys) {
                skeys.add(key);
                ciKeys.add(key.toLowerCase());
            }
        }
    }

    public KeyValueProperyHandlerFactory(String keyProperty,
            String valueProperty) {
        this(keyProperty, valueProperty, null);
    }

    public void addPropertyHandler(Class<?> beanClass,
            String propertyName,
            List<PropertyHandler> chain,
            boolean stickyCollections) {
        int last = chain.size() - 1;
        Class<?> lastClass = (last >= 0 ? chain.get(last).getElementClass() : null);
        if (!beanClass.equals(lastClass)) {
            // first call - let parent create handler for list property
            super.addPropertyHandler(beanClass, propertyName, chain, stickyCollections);
            last = chain.size() - 1;
            if (IndexPropertyHandler.class.isAssignableFrom(chain.get(last).getClass())) {
                // we need the whole array, not the first element, so remove this handler:
                chain.remove(last);
            }
            return;
        }

        checkKey(propertyName);
        PropertyDescriptor pd =
                chain.get(chain.size() - 1).getPropertyDescriptor();
        PropertyHandler handler =
                new KeyValuePairHandler(pd, keyProperty, valueProperty,
                        propertyName, isCaseInsensitive());
        chain.add(handler);
    }

    private void checkKey(String key) {
        if (skeys.isEmpty()) {
            return;
        }
        Set<String> keys = skeys;
        if (isCaseInsensitive()) {
            key = key.toLowerCase();
            keys = ciKeys;
        }
        if (!keys.contains(key)) {
            throw new RuntimeException("Invalid key name: " + key);
        }
    }
}

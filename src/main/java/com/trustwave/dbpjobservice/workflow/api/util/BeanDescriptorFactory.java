package com.trustwave.dbpjobservice.workflow.api.util;

import java.util.HashMap;
import java.util.Map;

public class BeanDescriptorFactory {
    private static BeanDescriptorFactory instance = new BeanDescriptorFactory();
    private Map<Class<?>, BeanDescriptor> beanDescriptors =
            new HashMap<Class<?>, BeanDescriptor>();

    public static BeanDescriptorFactory getInstance() {
        return instance;
    }

    public synchronized BeanDescriptor getDescriptor(Class<?> beanClass) {
        BeanDescriptor bd = beanDescriptors.get(beanClass);
        if (bd == null) {
            bd = register(beanClass, new BeanDescriptor(beanClass));
        }
        return bd;
    }

    public synchronized BeanDescriptor register(Class<?> beanClass,
            BeanDescriptor descriptor) {
        beanDescriptors.put(beanClass, descriptor);
        return descriptor;
    }

    public synchronized void cleanRegistered() {
        beanDescriptors.clear();
    }

}

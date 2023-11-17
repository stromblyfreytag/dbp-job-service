package com.trustwave.dbpjobservice.workflow.api.util;

import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * <p>This handler is responsible for converting collection into a single element,
 * i.e. acts as an 'index' operation; with (initial) index 0.
 * </p>
 * <p>This handler is added to the property handler chain whenever we try to access
 * property of collection element, rather than collection itself; i.e.:
 * <pre>
 *      assetBean.getProperty("endpoints.port")
 *  </pre>
 * where endpoints is a collection, is treated as:
 * <pre>
 *      assetBean.getProperty("endpoints[0].port")
 *  </pre>
 * </p>
 * <p>
 * The index value may be changed explicitly with setIndex() or increaseIndex() methods.
 * Reference to this handler can be obtained with {@link BeanDescriptor} call.
 *
 * @author vlad
 * @see PropertyHandlerFactory
 * @see BeanDescriptor
 */
public class IndexPropertyHandler extends PropertyHandler {
    private int index = 0;

    public IndexPropertyHandler(PropertyDescriptor pd) {
        super(pd);
    }

    public Object getProperty(Object obj) {
        @SuppressWarnings("unchecked")
        Collection<Object> collection = (Collection<Object>) obj;
        if (collection == null || collection.size() <= index) {
            return null;
        }
        Iterator<Object> it = collection.iterator();
        for (int i = index; i > 0 && it.hasNext(); i--) {
            it.next();
        }
        return (it.hasNext() ? it.next() : null);
    }

    public void setProperty(Object obj, Object value) {
        @SuppressWarnings("unchecked")
        Collection<Object> collection = (Collection<Object>) obj;
        if (index == collection.size()) {
            // set 'next-after-the-last' element, i.e. ADD.
            // We can add to any collection type:
            collection.add(value);
        }
        else if (index == 0 && collection.size() == 1) {
            // special case - we can set (replace) single element in any collection
            collection.clear();
            collection.add(value);
        }
        else if (List.class.isAssignableFrom(collection.getClass())) {
            // We need to change/set element somewhere 'in the middle' of collection.
            // This can be done only if order of elements is well-defined, i.e. for Lists:
            List<Object> list = (List<Object>) collection;
            // may throw if index > size
            list.set(index, value);
        }
        else {
            throw new RuntimeException("Changing collection elements is supported only for lists - element index should be well-defined");
        }
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void increaseIndex() {
        ++index;
    }
}

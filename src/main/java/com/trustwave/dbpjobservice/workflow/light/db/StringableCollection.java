package com.trustwave.dbpjobservice.workflow.light.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import com.trustwave.dbpjobservice.workflow.api.util.EscapingEncoder;

/**
 * Utility class for auto-converting list of strings into a string and back
 */
class StringableCollection {
    private Collection<String> collection;
    private EscapingEncoder encoder;
    private boolean modified = false;
    private String stringValue;

    public StringableCollection(Collection<String> collection, EscapingEncoder encoder) {
        this.collection = collection;
        this.encoder = encoder;
    }

    public synchronized void add(String elem) {
        modified = true;
        collection.add(elem);
    }

    public synchronized boolean remove(String elem) {
        modified = true;
        return collection.remove(elem);
    }

    public synchronized String removeFirst() {
        modified = true;
        return ((LinkedList<String>) collection).removeFirst();
    }

    public synchronized String removeLast() {
        modified = true;
        return ((LinkedList<String>) collection).removeLast();
    }

    public synchronized String getStringValue() {
        if (modified) {
            stringValue = encoder.encode(collection);
            modified = false;
        }
        return stringValue;
    }

    public synchronized void setStringValue(String stringValue) {
        this.stringValue = stringValue;
        collection.clear();
        collection.addAll(encoder.decode(stringValue));
        modified = false;
    }

    public synchronized Collection<String> getCollection() {
        return new ArrayList<>(collection);
    }

}

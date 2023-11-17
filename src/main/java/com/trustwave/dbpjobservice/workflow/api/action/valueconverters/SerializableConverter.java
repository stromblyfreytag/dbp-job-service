package com.trustwave.dbpjobservice.workflow.api.action.valueconverters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.codec.binary.Base64;

import com.trustwave.dbpjobservice.workflow.api.action.ValueConverter;

class SerializableConverter implements ValueConverter {
    @Override
    public String objectToString(Object object) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            oos.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        return Base64.encodeBase64String(baos.toByteArray());
    }

    @Override
    public Object stringToObject(String string, Class<?> type) {
        Object obj;
        try {

            byte[] buf = Base64.decodeBase64(string);
            ByteArrayInputStream bis = new ByteArrayInputStream(buf);
            ObjectInputStream ois = new ObjectInputStream(bis);
            obj = ois.readObject();
            ois.close();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        return obj;
    }

    @Override
    public String getShortString(Object obj) {
        return String.valueOf(obj);
    }

}

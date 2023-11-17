package com.trustwave.dbpjobservice.impl;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Messages {
    private static final Logger logger = LogManager.getLogger(Messages.class);
    private static final String BUNDLE_NAME = "messages";
    private static ResourceBundle RESOURCE_BUNDLE = null;

    public static synchronized String getString(String key) {
        try {
            if (RESOURCE_BUNDLE == null) {
                RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);
            }
            return RESOURCE_BUNDLE.getString(key);
        }
        catch (Exception e) {
            logger.warn("Cannot retrieve message for '" + key + "': " + e);
            return '!' + key + '!';
        }
    }

    public static String getString(String key, Object... params) {
        String msg = getString(key);
        try {
            return MessageFormat.format(msg, params);
        }
        catch (Exception e) {
            logger.warn("Cannot format message '" + msg + "': " + e);
            return msg;
        }
    }
}

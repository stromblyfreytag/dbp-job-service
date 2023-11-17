package com.trustwave.dbpjobservice.workflow.light.db;

public class LightDaoHolder {
    private static LightDao instance;

    public LightDaoHolder(LightDao dao) {
        instance = dao;
    }

    public static LightDao getInstance() {
        return instance;
    }

}

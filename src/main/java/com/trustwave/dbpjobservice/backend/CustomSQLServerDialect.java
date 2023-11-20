package com.trustwave.dbpjobservice.backend;

import java.sql.Types;

import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.type.StandardBasicTypes;

public class CustomSQLServerDialect extends SQLServerDialect {

    public CustomSQLServerDialect() {
        super();
        registerHibernateType(Types.NVARCHAR, StandardBasicTypes.STRING.getName());
    }
}
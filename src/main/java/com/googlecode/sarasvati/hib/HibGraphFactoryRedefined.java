package com.googlecode.sarasvati.hib;

import org.hibernate.Session;

import com.trustwave.dbpjobservice.workflow.light.engine.LightGraphFactory;

/**
 * We need this class to provide public constructor for HibGraphFactory,
 * used in {@link LightGraphFactory}
 *
 * @author vlad
 */
public class HibGraphFactoryRedefined extends HibGraphFactory {
    public HibGraphFactoryRedefined(Session session) {
        super(session);
    }
}

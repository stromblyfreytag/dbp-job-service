package com.googlecode.sarasvati.hib;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

import com.googlecode.sarasvati.GraphProcess;
import com.trustwave.dbpjobservice.workflow.light.engine.LightEngine;

/**
 * We need this class to provide public constructor for HibGraphRepository,
 * used in {@link LightEngine}
 *
 * @author vlad
 */
public class HibGraphRepositoryRedefined extends HibGraphRepository {
    public HibGraphRepositoryRedefined(Session session) {
        super(session);
    }

    @Override
    public List<GraphProcess> getActiveNestedProcesses(final GraphProcess process) {
        // TODO
        return new ArrayList<>();
    }
}

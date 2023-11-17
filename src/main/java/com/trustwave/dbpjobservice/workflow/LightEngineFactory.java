package com.trustwave.dbpjobservice.workflow;

import java.util.ArrayList;

import com.googlecode.sarasvati.Engine;
import com.googlecode.sarasvati.Graph;
import com.googlecode.sarasvati.GraphProcess;
import com.googlecode.sarasvati.NodeToken;
import com.googlecode.sarasvati.hib.HibGraph;
import com.googlecode.sarasvati.hib.HibGraphListener;
import com.trustwave.dbpjobservice.impl.Configuration;
import com.trustwave.dbpjobservice.workflow.light.db.LightDao;
import com.trustwave.dbpjobservice.workflow.light.engine.LightEngine;
import com.trustwave.dbpjobservice.workflow.light.engine.LightGraphProcess;

public class LightEngineFactory extends EngineFactory {
    private LightDao dao;
    private Configuration config;

    public LightEngine createEngine() {
        return new LightEngine(dao);
    }

    @Override
    public LightGraphProcess findProcess(long processId) {
        LightGraphProcess process = getDao().findProcess(processId);
        process.setExecutionQueuePolicy(getExecutionQueuePolicy());
        return process;
    }

    ExecutionQueuePolicy getExecutionQueuePolicy() {
        return config.isUseLifoExecutionPolicy() ? ExecutionQueuePolicy.LIFO
                : ExecutionQueuePolicy.FIFO;
    }

    @Override
    protected LightGraphProcess newProcess(Graph graph, Engine engine) {
        HibGraph hibGraph = (HibGraph) graph;
        if (hibGraph.getListeners() == null) {
            hibGraph.setListeners(new ArrayList<HibGraphListener>());
        }
        LightGraphProcess process = getDao().createProcess(hibGraph);
        process.setExecutionQueuePolicy(getExecutionQueuePolicy());
        return process;
    }

    @Override
    public NodeToken getTokenById(long tokenId, long processId) {
        return getDao().findNodeToken(tokenId);

    }

    @Override
    public Long getProcessId(GraphProcess process) {
        return ((LightGraphProcess) process).getId();
    }

    @Override
    public void deleteProcess(long processId) {
        getDao().deleteProcess(processId);
    }

    @Override
    public Long getGraphId(Graph graph) {
        return ((HibGraph) graph).getId();
    }

    @Override
    protected boolean isOwnProcessClass(Class<? extends GraphProcess> clazz) {
        return LightGraphProcess.class.isAssignableFrom(clazz);
    }

    public LightDao getDao() {
        return dao;
    }

    public void setDao(LightDao dao) {
        this.dao = dao;
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }

}

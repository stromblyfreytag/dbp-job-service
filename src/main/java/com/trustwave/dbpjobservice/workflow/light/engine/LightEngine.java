package com.trustwave.dbpjobservice.workflow.light.engine;

import com.googlecode.sarasvati.DelayedTokenScheduler;
import com.googlecode.sarasvati.GraphProcess;
import com.googlecode.sarasvati.event.ExecutionEventType;
import com.googlecode.sarasvati.event.ExecutionListener;
import com.googlecode.sarasvati.hib.HibGraph;
import com.googlecode.sarasvati.hib.HibGraphRepository;
import com.googlecode.sarasvati.hib.HibGraphRepositoryRedefined;
import com.googlecode.sarasvati.impl.BaseEngine;
import com.googlecode.sarasvati.load.GraphLoader;
import com.googlecode.sarasvati.load.GraphLoaderImpl;
import com.googlecode.sarasvati.load.GraphValidator;
import com.trustwave.dbpjobservice.workflow.light.db.LightDao;

public class LightEngine extends BaseEngine {
    private LightDao dao;
    private HibGraphRepository repository = null;
    private LightGraphFactory factory = null;

    public LightEngine(final String applicationContext, LightDao dao) {
        super(applicationContext);
        this.dao = dao;
    }

    public LightEngine(LightDao dao) {
        this(DEFAULT_APPLICATION_CONTEXT, dao);
    }

    @Override
    public LightGraphFactory getFactory() {
        if (factory == null) {
            factory = new LightGraphFactory(dao);
        }
        return factory;
    }

    @Override
    public HibGraphRepository getRepository() {
        if (repository == null) {
            repository = new HibGraphRepositoryRedefined(dao.getSession());
        }
        return repository;
    }

    @Override
    public GraphLoader<HibGraph> getLoader() {
        return getLoader(null);
    }

    @Override
    public GraphLoader<HibGraph> getLoader(final GraphValidator validator) {
        return new GraphLoaderImpl<HibGraph>(getFactory(), getRepository(), null);
    }

    @Override
    public LightEngine newEngine() {
        return new LightEngine(dao);
    }

    public void addExecutionListener(GraphProcess process,
            Class<? extends ExecutionListener> listenerClass,
            ExecutionEventType... eventTypes) {
        ((LightGraphProcess) process).addListener(listenerClass, eventTypes);
    }

    @Override
    public DelayedTokenScheduler getDelayedTokenScheduler() {
        // TODO Auto-generated method stub
        return null;
    }

}
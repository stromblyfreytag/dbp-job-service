package com.trustwave.dbpjobservice.workflow;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.googlecode.sarasvati.Engine;
import com.googlecode.sarasvati.Graph;
import com.googlecode.sarasvati.GraphProcess;
import com.googlecode.sarasvati.NodeToken;
import com.googlecode.sarasvati.event.ProcessEvent;

@Component
public abstract class EngineFactory {
    @Autowired
    private SessionFactory sessionFactory;

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    // this method may be called only for engine-neutral operations,
    // like loading graphs, also for creating new processes..
    // Operations for existing processes should use
    // createEngineForProcess(processId) call
    public abstract Engine createEngine();

    public Engine createEngineForProcess(long processId) {
        return createEngine();
    }

    public abstract GraphProcess findProcess(long processId);

    public GraphProcess createProcess(Graph graph) {
        Engine engine = createEngine();
        GraphProcess process = newProcess(graph, engine);
        ProcessEvent.fireCreatedEvent(engine, process);
        return process;
    }

    protected abstract GraphProcess newProcess(Graph graph, Engine engine);

    public abstract void deleteProcess(long processId);

    public abstract NodeToken getTokenById(long tokenId, long processId);

    public abstract Long getProcessId(GraphProcess process);

    public abstract Long getGraphId(Graph graph);

    protected long getLastProcessId() {
        return -1L;
    }

    protected boolean isOwnProcessClass(Class<? extends GraphProcess> clazz) {
        return false;
    }
}

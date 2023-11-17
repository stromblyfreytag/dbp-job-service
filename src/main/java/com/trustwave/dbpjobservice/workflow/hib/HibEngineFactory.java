package com.trustwave.dbpjobservice.workflow.hib;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.springframework.stereotype.Component;

import com.googlecode.sarasvati.Engine;
import com.googlecode.sarasvati.Graph;
import com.googlecode.sarasvati.GraphProcess;
import com.googlecode.sarasvati.NodeToken;
import com.googlecode.sarasvati.hib.HibEngine;
import com.googlecode.sarasvati.hib.HibGraph;
import com.googlecode.sarasvati.hib.HibGraphListener;
import com.googlecode.sarasvati.hib.HibGraphProcess;
import com.googlecode.sarasvati.hib.HibGraphRepository;
import com.trustwave.dbpjobservice.workflow.EngineFactory;

@Component
public class HibEngineFactory extends EngineFactory {
    public HibEngine createEngine() {
        return new HibEngine(getSessionFactory().getCurrentSession());
    }

    @Override
    public HibGraphProcess findProcess(long processId) {
        HibEngine engine = createEngine();
        HibGraphRepository repository = engine.getRepository();
        return (HibGraphProcess) repository.findProcess(processId);
    }

    @Override
    protected HibGraphProcess newProcess(Graph graph, Engine engine) {
        HibGraphProcess process =
                ((HibEngine) engine).getFactory().newProcess(graph);
        HibGraph hibGraph = (HibGraph) graph;
		if (hibGraph.getListeners() == null) {
			hibGraph.setListeners(new ArrayList<HibGraphListener>());
		}
        return process;
    }

    @Override
    public NodeToken getTokenById(long tokenId, long processId) {
        HibEngine engine = createEngine();
        HibGraphRepository repository = engine.getRepository();
        NodeToken token = repository.findNodeToken(tokenId);
        return token;
    }

    @Override
    public Long getProcessId(GraphProcess process) {
        return ((HibGraphProcess) process).getId();
    }

    @Override
    public Long getGraphId(Graph graph) {
        return ((HibGraph) graph).getId();
    }

    @Override
    public void deleteProcess(long processId) {
        HibGraphProcess process = findProcess(processId);
        getSessionFactory().getCurrentSession().delete(process);
    }

    @Override
    protected long getLastProcessId() {
        Session session = getSessionFactory().getCurrentSession();
        @SuppressWarnings("unchecked")
        List<Object> results =
                session.createQuery("select max(id) from HibGraphProcess").list();
        Number max = results.size() > 0 ? (Number) results.get(0) : null;
        return max != null ? max.longValue() : -1L;
    }

    @Override
    protected boolean isOwnProcessClass(Class<? extends GraphProcess> clazz) {
        return HibGraphProcess.class.isAssignableFrom(clazz);
    }
}

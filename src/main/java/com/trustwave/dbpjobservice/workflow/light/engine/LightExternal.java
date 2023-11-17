package com.trustwave.dbpjobservice.workflow.light.engine;

import com.googlecode.sarasvati.External;
import com.googlecode.sarasvati.Graph;
import com.googlecode.sarasvati.env.ReadEnv;

public class LightExternal implements External {
    protected String name;
    protected Graph graph;
    protected Graph externalGraph;

    protected ReadEnv env;

    public LightExternal(final String name, final Graph graph, final Graph externalGraph, final ReadEnv env) {
        this.name = name;
        this.graph = graph;
        this.externalGraph = externalGraph;
        this.env = env;
    }

    @Override
    public ReadEnv getEnv() {
        return env;
    }

    @Override
    public Graph getExternalGraph() {
        return externalGraph;
    }

    @Override
    public Graph getGraph() {
        return graph;
    }

    @Override
    public String getName() {
        return name;
    }
}

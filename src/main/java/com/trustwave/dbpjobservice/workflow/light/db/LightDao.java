package com.trustwave.dbpjobservice.workflow.light.db;

import java.util.Collection;
import java.util.List;

import org.hibernate.Session;

import com.googlecode.sarasvati.ExecutionType;
import com.googlecode.sarasvati.Node;
import com.googlecode.sarasvati.NodeToken;
import com.googlecode.sarasvati.hib.HibArc;
import com.googlecode.sarasvati.hib.HibGraph;
import com.googlecode.sarasvati.hib.HibNodeRef;
import com.trustwave.dbpjobservice.workflow.light.engine.LightGraphProcess;
import com.trustwave.dbpjobservice.workflow.light.engine.LightNodeToken;
import com.trustwave.dbpjobservice.workflow.light.engine.LightTokenSet;

public interface LightDao {
    public HibGraph findGraph(Long graphId);

    public Node findNode(Long nodeId);

    public LightGraphProcess createProcess(HibGraph graph);

    public LightGraphProcess findProcess(Long processId);

    public void deleteProcess(Long processId);

    public LightNodeToken createNodeToken(LightGraphProcess process,
            HibNodeRef node,
            ExecutionType executionType,
            String envString,
            Collection<String> parentArcIds);

    public LightNodeToken getNodeToken(LightTokenRecord record);

    public LightNodeToken findNodeToken(Long tokenId);

    public List<NodeToken> findTokensOnNode(Long processId, Node node);

    public List<NodeToken> findActiveTokens(Long processId);

    public HibArc findArc(Long arcId);

    public LightTokenSet createTokenSet(LightGraphProcess process,
            String name,
            int maxMemberIndex);

    public LightTokenSet findTokenSet(Long processId, String name);

    public Session getSession();

}

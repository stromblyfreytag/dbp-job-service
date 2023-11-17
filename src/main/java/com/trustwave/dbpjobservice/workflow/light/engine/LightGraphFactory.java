package com.trustwave.dbpjobservice.workflow.light.engine;

import static com.trustwave.dbpjobservice.workflow.light.engine.LightArcToken.ArcTokenState.Pending;

import java.util.ArrayList;
import java.util.List;

import com.googlecode.sarasvati.Arc;
import com.googlecode.sarasvati.ArcToken;
import com.googlecode.sarasvati.ExecutionType;
import com.googlecode.sarasvati.External;
import com.googlecode.sarasvati.Graph;
import com.googlecode.sarasvati.GraphProcess;
import com.googlecode.sarasvati.JoinType;
import com.googlecode.sarasvati.Node;
import com.googlecode.sarasvati.NodeToken;
import com.googlecode.sarasvati.TokenSet;
import com.googlecode.sarasvati.env.Env;
import com.googlecode.sarasvati.hib.HibArc;
import com.googlecode.sarasvati.hib.HibGraph;
import com.googlecode.sarasvati.hib.HibGraphFactory;
import com.googlecode.sarasvati.hib.HibGraphFactoryRedefined;
import com.googlecode.sarasvati.hib.HibNode;
import com.googlecode.sarasvati.hib.HibNodeRef;
import com.googlecode.sarasvati.load.AbstractGraphFactory;
import com.googlecode.sarasvati.load.NodeFactory;
import com.googlecode.sarasvati.load.definition.CustomDefinition;
import com.trustwave.dbpjobservice.impl.Messages;
import com.trustwave.dbpjobservice.workflow.light.db.LightDao;
import com.trustwave.dbpjobservice.workflow.light.db.LightDaoHolder;
import com.trustwave.dbpjobservice.workflow.light.db.LightUtils;

public class LightGraphFactory extends AbstractGraphFactory {
    private HibGraphFactory hibGraphFactory;

    public LightGraphFactory(LightDao dao) {
        super(HibNode.class);
        this.hibGraphFactory = new HibGraphFactoryRedefined(dao.getSession());
    }

    @Override
    public HibGraph newGraph(String name, int version, String customId) {
        return hibGraphFactory.newGraph(name, version, customId);
    }

    @Override
    public HibArc newArc(Graph graph, Node startNode, Node endNode, String name) {
        return hibGraphFactory.newArc(graph, startNode, endNode, name);
    }

    @Override
    public Node newNode(Graph graph,
            String name,
            String type,
            JoinType joinType,
            String joinParam,
            boolean isStart,
            String guard,
            List<Object> customList) {
        return hibGraphFactory.newNode(graph, name, type, joinType, joinParam,
                isStart, guard, customList);
    }

    @Override
    public Node importNode(Graph graph, Node node, External external) {
        return hibGraphFactory.importNode(graph, node, external);
    }

    @Override
    public External newExternal(String name,
            Graph graph,
            Graph externalGraph,
            CustomDefinition customDefinition) {
        return hibGraphFactory.newExternal(name, graph, externalGraph,
                customDefinition);
    }

    @Override
    public ArcToken newArcToken(GraphProcess process,
            Arc arc,
            ExecutionType executionType,
            NodeToken parent,
            boolean isTokenSetMember) {
        // all arc tokens are created in pending state
        return new LightArcToken((LightNodeToken) parent, (HibArc) arc,
                Pending, null);
    }

    @Override
    public NodeToken newNodeToken(GraphProcess process,
            Node node,
            ExecutionType executionType,
            List<ArcToken> parents,
            NodeToken envParent) {
        String envString = getInheritedEnvString(parents, envParent, node);

        List<String> parentArcIds = new ArrayList<>(parents.size());
        for (ArcToken parent : parents) {
            String id = ((LightArcToken) parent).getShortId();
            parentArcIds.add(id);
        }

        if (!(node instanceof HibNodeRef)) {
            throw new RuntimeException(Messages.getString("workflow.light.class.wrong", node.getClass()));
        }

        return getDao().createNodeToken((LightGraphProcess) process,
                (HibNodeRef) node,
                executionType,
                envString,
                parentArcIds);
    }

    String getInheritedEnvString(List<ArcToken> parents,
            NodeToken envParent,
            Node targetNode) {
        List<ArcToken> envTokens =
                (envParent == null ? parents : envParent.getParentTokens());

        if (envTokens.size() == 0 || LightUtils.isTokenSetMergeNode(targetNode)) {
            // nothing to inherit
            return null;
        }

        // yes, this is cheating;
        // but we know that there are only LightArcTokens in the list:
        @SuppressWarnings({"unchecked", "rawtypes"})
        List<LightArcToken> lightEnvTokens = (List) envTokens;

        if (lightEnvTokens.size() == 1) {
            // inherit from one node, use token's env string:
            return lightEnvTokens.get(0).getEnvString();
        }

        // merge all parent token environments:
        String envString = null;
        for (LightArcToken t : lightEnvTokens) {
            envString = LightUtils.mergeEnvStrings(envString, t.getEnvString());
        }
        return envString;
    }

    @Override
    public LightGraphProcess newProcess(Graph graph) {
        return getDao().createProcess((HibGraph) graph);
    }

    @Override
    public LightGraphProcess newNestedProcess(Graph graph,
            NodeToken parentToken) {
        LightGraphProcess process = newProcess(graph);
        process.setParentToken(parentToken);
        return process;
    }

    @Override
    public LightTokenSet newTokenSet(GraphProcess process,
            String name,
            int maxMemberIndex,
            int level) {
        return getDao().createTokenSet((LightGraphProcess) process,
                name, maxMemberIndex);
    }

    @Override
    public LightArcTokenSetMember newArcTokenSetMember(TokenSet tokenSet,
            ArcToken arcToken,
            int memberIndex) {
        //    This method is called from completeWithNewTokenSet(), for every
        // initial token set arc token.
        //    This is a good opportunity to consume initial token set environment
        // for this 'thread' (member) and save it with the arc token.
        //    Arc token here is new and not yet put into any process queue;
        // it is safe to change its initial environment now.
        //    We also remove initial member environment from the token set,
        // so subsequent calls of this method with the same memberIndex
        // for other arcs will not add initial environment to them.

        LightTokenSet lightTokenSet = (LightTokenSet) tokenSet;
        LightArcToken lightArcToken = (LightArcToken) arcToken;
        Env initialEnv = lightTokenSet.removeInitialMemberEnv(memberIndex);
        lightArcToken.setInitialEnvString(LightUtils.encodeEnv(initialEnv));

        LightArcTokenSetMember member =
                new LightArcTokenSetMember(lightTokenSet, lightArcToken, memberIndex);
        if (initialEnv != null) {
            lightArcToken.setTokenSetMember(member);
        }
        return member;
    }

    @Override
    public LightNodeTokenSetMember newNodeTokenSetMember(TokenSet tokenSet,
            NodeToken token,
            int memberIndex) {
        LightNodeToken lightToken = (LightNodeToken) token;
        LightTokenSet lightTokenSet = (LightTokenSet) tokenSet;
        lightToken.addToTokenset(tokenSet, memberIndex);
        return new LightNodeTokenSetMember(lightTokenSet, lightToken, memberIndex);
    }

    private LightDao getDao() {
        return LightDaoHolder.getInstance();
    }

    @Override
    public void addType(final String type, final Class<? extends Node> clazz) {
        super.addType(type, clazz);
        hibGraphFactory.addType(type, clazz);
    }

    @Override
    public void addNodeFactory(final String type, final NodeFactory nodeFactory) {
        super.addNodeFactory(type, nodeFactory);
        hibGraphFactory.addNodeFactory(type, nodeFactory);
    }

}
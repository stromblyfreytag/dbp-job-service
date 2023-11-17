package com.trustwave.dbpjobservice.workflow.light.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import com.googlecode.sarasvati.ExecutionType;
import com.googlecode.sarasvati.Node;
import com.googlecode.sarasvati.NodeToken;
import com.googlecode.sarasvati.ProcessState;
import com.googlecode.sarasvati.hib.HibArc;
import com.googlecode.sarasvati.hib.HibGraph;
import com.googlecode.sarasvati.hib.HibNodeRef;
import com.googlecode.sarasvati.hib.HibPropertyNode;
import com.trustwave.dbpjobservice.workflow.api.util.BooleanStatisticsCollector;
import com.trustwave.dbpjobservice.workflow.api.util.StatisticsCollector;
import com.trustwave.dbpjobservice.workflow.light.engine.LightGraphProcess;
import com.trustwave.dbpjobservice.workflow.light.engine.LightNodeToken;
import com.trustwave.dbpjobservice.workflow.light.engine.LightTokenSet;

public class LightHibernateDao implements LightDao {
    private SessionFactory sessionFactory;
    private LightDao cachingDao = this;
    private BooleanStatisticsCollector containsCollector =
            StatisticsCollector.getInstance().createBooleanCollector(
                    "ensure-attached.contains");
    private BooleanStatisticsCollector lockCollector =
            StatisticsCollector.getInstance().createBooleanCollector(
                    "ensure-attached.lock");

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Session getSession() {
        return sessionFactory.getCurrentSession();
    }

    public HibGraph findGraph(Long graphId) {
        HibGraph graph = (HibGraph) getSession().get(HibGraph.class, graphId);
        readGraph(graph);
        return graph;
    }

    // ensure graph is completely read into memory
    private void readGraph(HibGraph graph) {
        // this call will read all arcs
        graph.getOutputArcs(null);
        graph.getListeners();

        for (Node node : graph.getNodes()) {
            if (node instanceof HibNodeRef) {
                node = ((HibNodeRef) node).getNode();
            }
            if (node instanceof HibPropertyNode) {
                // force reading properties
                ((HibPropertyNode) node).getProperty("someproperty");
            }
        }
    }

    public Node findNode(Long nodeId) {
        HibNodeRef node = (HibNodeRef) getSession().get(HibNodeRef.class, nodeId);
        return node;
    }

    @Override
    public LightGraphProcess createProcess(HibGraph graph) {
        readGraph(graph);
        LightProcessRecord record = new LightProcessRecord();
        record.setGraphId(graph.getId());
        record.setCreateDate(new Date());
        record.setState(ProcessState.Created.name());
        getSession().save(record);
        return new LightGraphProcess(record);
    }

    @Override
    public LightGraphProcess findProcess(Long processId) {
        LightProcessRecord record =
                (LightProcessRecord) getSession().get(
                        LightProcessRecord.class, processId);
        return new LightGraphProcess(record);
    }

    @Override
    public void deleteProcess(Long processId) {
        LightProcessRecord record =
                (LightProcessRecord) getSession().get(
                        LightProcessRecord.class, processId);
        if (record != null) {
            getSession().delete(record);
        }
    }

    @Override
    public LightNodeToken createNodeToken(LightGraphProcess process,
            HibNodeRef node,
            ExecutionType executionType,
            String envString,
            Collection<String> parentArcIds) {
        LightTokenRecord record = new LightTokenRecord();
        record.setProcessId(process.getId());
        record.setExecutionType(executionType.name());
        record.setNodeId(node.getId());
        record.setEnvString(envString);
        record.setCreateDate(new Date());
        record.setParentArcs(parentArcIds);
        getSession().save(record);
        return new LightNodeToken(record);
    }

    @Override
    public LightNodeToken getNodeToken(LightTokenRecord record) {
        return new LightNodeToken(record);
    }

    @Override
    public LightNodeToken findNodeToken(Long tokenId) {
        LightTokenRecord record =
                (LightTokenRecord) getSession().get(LightTokenRecord.class, tokenId);
        return new LightNodeToken(record);
    }

    public List<NodeToken> findTokensOnNode(Long processId, Node node) {
        String query = "from LightTokenRecord "
                + "where processId = :processId"
                + " and node.name = :nodeName";

        @SuppressWarnings("unchecked")
        List<LightTokenRecord> tokenRecords =
                getSession().createQuery(query)
                        .setLong("processId", processId)
                        .setString("nodeName", node.getName())
                        .list();

        List<NodeToken> tokens = new ArrayList<>();
        for (LightTokenRecord record : tokenRecords) {
            tokens.add(cachingDao.getNodeToken(record));
        }
        return tokens;
    }

    public List<NodeToken> findActiveTokens(Long processId) {
        String query = "from LightTokenRecord "
                + "where processId = :processId"
                + " and completeDate is null";

        @SuppressWarnings("unchecked")
        List<LightTokenRecord> tokenRecords =
                getSession().createQuery(query)
                        .setLong("processId", processId)
                        .list();

        List<NodeToken> tokens = new ArrayList<>();
        for (LightTokenRecord record : tokenRecords) {
            tokens.add(cachingDao.getNodeToken(record));
        }
        return tokens;
    }

    @Override
    public HibArc findArc(Long arcId) {
        return (HibArc) getSession().get(HibArc.class, arcId);
    }

    @Override
    public LightTokenSet createTokenSet(LightGraphProcess process, String name,
            int maxMemberIndex) {
        LightTokenSetId id = new LightTokenSetId(process.getId(), name);
        LightTokenSetRecord record = new LightTokenSetRecord();
        record.setId(id);
        record.setSize(maxMemberIndex);
        getSession().save(record);
        return new LightTokenSet(record);
    }

    @Override
    public LightTokenSet findTokenSet(Long processId, String name) {
        LightTokenSetId id = new LightTokenSetId(processId, name);
        LightTokenSetRecord record =
                (LightTokenSetRecord) getSession().get(LightTokenSetRecord.class, id);
        return new LightTokenSet(record);
    }

    void setCachingDao(LightDao dao) {
        this.cachingDao = dao;
    }

    /**
     * The call ensures the specified (record) object is attached
     * to the current session.
     *
     * @param obj
     * @return
     */
    <T> T ensureAttached(T obj) {
        Session session = getSession();

        if (!session.contains(obj)) {
            containsCollector.markFailure();
            try {
                session.lock(obj, LockMode.NONE);
                lockCollector.markSuccess();
            }
            catch (NonUniqueObjectException e) {
                lockCollector.markFailure();
                @SuppressWarnings("unchecked")
                T pobj = (T) session.merge(obj);
                return pobj;
            }
        }
        else {
            containsCollector.markSuccess();
        }
        return obj;
    }
}

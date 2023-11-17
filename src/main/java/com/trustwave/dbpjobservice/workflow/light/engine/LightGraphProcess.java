package com.trustwave.dbpjobservice.workflow.light.engine;

import static com.trustwave.dbpjobservice.workflow.light.engine.LightArcToken.ArcTokenState.Active;
import static com.trustwave.dbpjobservice.workflow.light.engine.LightArcToken.ArcTokenState.Pending;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.googlecode.sarasvati.ArcToken;
import com.googlecode.sarasvati.Engine;
import com.googlecode.sarasvati.Graph;
import com.googlecode.sarasvati.GraphProcess;
import com.googlecode.sarasvati.Node;
import com.googlecode.sarasvati.NodeToken;
import com.googlecode.sarasvati.ProcessState;
import com.googlecode.sarasvati.env.Env;
import com.googlecode.sarasvati.event.CachingExecutionEventQueue;
import com.googlecode.sarasvati.event.ExecutionEventQueue;
import com.googlecode.sarasvati.event.ExecutionEventType;
import com.googlecode.sarasvati.event.ExecutionListener;
import com.trustwave.dbpjobservice.impl.Messages;
import com.trustwave.dbpjobservice.workflow.ExecutionQueuePolicy;
import com.trustwave.dbpjobservice.workflow.light.db.LightDao;
import com.trustwave.dbpjobservice.workflow.light.db.LightDaoHolder;
import com.trustwave.dbpjobservice.workflow.light.db.LightProcessRecord;

public class LightGraphProcess implements GraphProcess {
    private LightProcessRecord record;
    private ProcessState state;
    private SoftReference<Collection<NodeToken>> activeNodeTokensRef = new SoftReference<>(null);
    private SoftReference<Collection<ArcToken>> activeArcTokensRef = new SoftReference<>(null);
    private LinkedList<LightArcToken> executionQueue = null;
    private ExecutionQueuePolicy executionQueuePolicy = ExecutionQueuePolicy.FIFO;
    private CachingExecutionEventQueue eventQueue;
    private List<LightExecutionListener> listeners = null;

    public LightGraphProcess(LightProcessRecord record) {
        this.record = record;
        this.state = ProcessState.valueOf(record.getState());
        this.eventQueue = CachingExecutionEventQueue.newArrayListInstance();
        eventQueue.initFromPersisted(getListeners());
    }

    public long getId() {
        return record.getProcessId();
    }

    @Override
    public synchronized Collection<ArcToken> getActiveArcTokens() {
        Collection<ArcToken> activeArcTokens = activeArcTokensRef.get();
        if (activeArcTokens == null) {
            activeArcTokens = new HashSet<>();
            for (String arcStr : record.getActiveArcs()) {
                activeArcTokens.add(new LightArcToken(arcStr, Active));
            }
            activeArcTokensRef = new SoftReference<>(activeArcTokens);
        }
        return activeArcTokens;
    }

    @Override
    public void addActiveArcToken(final ArcToken token) {
        getActiveArcTokens().add(token);
        record.addActiveArc(((LightArcToken) token).getIdString());
    }

    @Override
    public void removeActiveArcToken(final ArcToken token) {
        getActiveArcTokens().remove(token);
        record.removeActiveArc(((LightArcToken) token).getIdString());
    }

    public LinkedList<LightArcToken> getExecutionQueue() {
        if (executionQueue == null) {
            executionQueue = new LinkedList<>();
            for (String arcId : record.getPendingArcs()) {
                executionQueue.add(new LightArcToken(arcId, Pending));
            }
        }
        return executionQueue;
    }

    public ExecutionQueuePolicy getExecutionQueuePolicy() {
        return executionQueuePolicy;
    }

    public void setExecutionQueuePolicy(ExecutionQueuePolicy executionQueuePolicy) {
        this.executionQueuePolicy = executionQueuePolicy;
    }

    @Override
    public ArcToken dequeueArcTokenForExecution() {
        String arcTokenId;
        LightArcToken arcToken;
        if (executionQueuePolicy == ExecutionQueuePolicy.LIFO) {
            arcTokenId = record.removeLastPendingArc();
            arcToken = getExecutionQueue().removeLast();
        }
        else {
            // FIFO, default:
            arcTokenId = record.removeFirstPendingArc();
            arcToken = getExecutionQueue().removeFirst();
        }
        if (!arcTokenId.equals(arcToken.getIdString())) {
            // we don't really expect that (somehow queues became unsynch-ed),
            // but just in case:
            executionQueue = null;  // force re-reading queue from db
            arcToken = new LightArcToken(arcTokenId, Pending);
        }
        return arcToken;
    }

    @Override
    public void enqueueArcTokenForExecution(final ArcToken token) {
        LightArcToken lightArcToken = (LightArcToken) token;
        getExecutionQueue().add(lightArcToken);
        record.addPendingArc(lightArcToken.getIdString());
    }

    @Override
    public boolean isArcTokenQueueEmpty() {
        return getExecutionQueue().isEmpty();
    }

    @Override
    public synchronized Collection<NodeToken> getActiveNodeTokens() {
        Collection<NodeToken> activeNodeTokens = activeNodeTokensRef.get();
        if (activeNodeTokens == null) {
            activeNodeTokens = new HashSet<>(getDao().findActiveTokens(getId()));
            activeNodeTokensRef = new SoftReference<>(activeNodeTokens);
        }
        return activeNodeTokens;
    }

    @Override
    public void addActiveNodeToken(final NodeToken token) {
        getActiveNodeTokens().add(token);
    }

    @Override
    public void removeActiveNodeToken(final NodeToken token) {
        getActiveNodeTokens().remove(token);
    }

    @Override
    public List<NodeToken> getNodeTokens() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addNodeToken(NodeToken token) {
    }

    public Set<ArcToken> getActiveTokenSetArcTokens(String tsName) {
        HashSet<ArcToken> tsTokens = new HashSet<>();

        for (ArcToken t : getExecutionQueue()) {
            if (tsName.equals(((LightArcToken) t).getTokenSetName())) {
                tsTokens.add(t);
            }
        }
        for (ArcToken t : getActiveArcTokens()) {
            if (tsName.equals(((LightArcToken) t).getTokenSetName())) {
                tsTokens.add(t);
            }
        }
        return tsTokens;
    }

    public Set<NodeToken> getActiveTokenSetNodeTokens(String tsName) {
        HashSet<NodeToken> tsTokens = new HashSet<>();

        for (NodeToken t : getActiveNodeTokens()) {
            if (tsName.equals(((LightNodeToken) t).getTokenSetName())) {
                tsTokens.add(t);
            }
        }
        return tsTokens;
    }

    @Override
    public Env getEnv() {
        return record.getEnv();
    }

    @Override
    public Graph getGraph() {
        return getDao().findGraph(record.getGraphId());
    }

    public NodeToken getParentToken() {
        return null;
    }

    public void setParentToken(final NodeToken parentToken) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProcessState getState() {
        return state;
    }

    @Override
    public void setState(final ProcessState state) {
        if (this.state != state) {
            this.state = state;
            record.setState(state.name());
        }
    }

    @Override
    public boolean isCanceled() {
        return state == ProcessState.PendingCancel || state == ProcessState.Canceled;
    }

    @Override
    public boolean isComplete() {
        return state == ProcessState.PendingCompletion || state == ProcessState.Completed;
    }

    @Override
    public boolean isExecuting() {
        return state == ProcessState.Executing;
    }

    @Override
    public boolean hasActiveTokens() {
        return !getActiveArcTokens().isEmpty() || !getActiveNodeTokens().isEmpty();
    }

    @Override
    public ExecutionEventQueue getEventQueue() {
        return eventQueue;
    }

    @Override
    public List<NodeToken> getTokensOnNode(final Node node, final Engine engine) {
        return getDao().findTokensOnNode(getId(), node);
    }

    public LightProcessRecord getRecord() {
        return record;
    }

    public void setRecord(LightProcessRecord record) {
        this.record = record;
    }

    public List<LightExecutionListener> getListeners() {
        if (listeners == null) {
            Collection<String> listenerStrings = record.getListeners();
            listeners = new ArrayList<>(listenerStrings.size());
            for (String str : listenerStrings) {
                listeners.add(new LightExecutionListener(str));
            }
        }
        return listeners;
    }

    public void addListener(Class<? extends ExecutionListener> clazz,
            ExecutionEventType... eventTypes) {
        LightExecutionListener persistent =
                new LightExecutionListener(clazz, eventTypes);
        getListeners().add(persistent);
        record.addListener(persistent.toString());

        try {
            ExecutionListener listener = clazz.newInstance();
            eventQueue.addListener(listener, eventTypes);
        }
        catch (Exception e) {
            throw new RuntimeException(Messages.getString("workflow.light.listener.canNotCreate"), e);
        }
    }

    protected LightDao getDao() {
        return LightDaoHolder.getInstance();
    }
}
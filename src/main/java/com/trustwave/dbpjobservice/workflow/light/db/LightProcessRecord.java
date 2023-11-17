package com.trustwave.dbpjobservice.workflow.light.db;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;

import com.googlecode.sarasvati.env.Env;
import com.trustwave.dbpjobservice.workflow.api.util.EscapingEncoder;

@Entity
@Table(name = "wfl_process")
public class LightProcessRecord {
    private static EscapingEncoder encoder = new EscapingEncoder(',', '^');
    long processId;
    StringableCollection listeners =
            new StringableCollection(new ArrayList<String>(), encoder);
    private Long graphId;
    private Date createDate;
    private String state;
    private EnvironmentHolder envHolder = new EnvironmentHolder();
    private StringableCollection pendingArcs =
            new StringableCollection(new LinkedList<String>(), encoder);
    private StringableCollection activeArcs =
            new StringableCollection(new HashSet<String>(), encoder);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "process_id")
    public long getProcessId() {
        return processId;
    }

    public void setProcessId(long processId) {
        this.processId = processId;
    }

    @Column(name = "graph_id", nullable = false)
    public Long getGraphId() {
        return graphId;
    }

    public void setGraphId(Long graphId) {
        this.graphId = graphId;
    }

    @Column(name = "create_date", nullable = false)
    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    @Column(name = "state", nullable = false)
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Column(name = "envstring")
    public String getEnvString() {
        String es = envHolder.getEnvString();
        return es;
    }

    public void setEnvString(String envString) {
        envHolder.setEnvString(envString);
    }

    @Transient
    public Env getEnv() {
        return envHolder.getEnv();
    }

    @Column(name = "pending_arcs")
    public String getPendingArcsString() {
        return pendingArcs.getStringValue();
    }

    public void setPendingArcsString(String pendingArcsString) {
        pendingArcs.setStringValue(pendingArcsString);
    }

    public void addPendingArc(String arcId) {
        pendingArcs.add(arcId);
    }

    public String removeFirstPendingArc() {
        return pendingArcs.removeFirst();
    }

    public String removeLastPendingArc() {
        return pendingArcs.removeLast();
    }

    @Transient
    public Collection<String> getPendingArcs() {
        return pendingArcs.getCollection();
    }

    @Column(name = "active_arcs")
    public String getActiveArcsString() {
        return activeArcs.getStringValue();
    }

    public void setActiveArcsString(String activeArcsString) {
        activeArcs.setStringValue(activeArcsString);
    }

    public void addActiveArc(String arcId) {
        activeArcs.add(arcId);
    }

    public void removeActiveArc(String arcId) {
        activeArcs.remove(arcId);
    }

    @Transient
    public Collection<String> getActiveArcs() {
        return activeArcs.getCollection();
    }

    @Column(name = "listeners")
    public String getListenersString() {
        return listeners.getStringValue();
    }

    public void setListenersString(String listenersString) {
        listeners.setStringValue(listenersString);
    }

    @Transient
    public Collection<String> getListeners() {
        return listeners.getCollection();
    }

    public void addListener(String listener) {
        listeners.add(listener);
    }

    @Override
    public String toString() {
        return "LightProcessRecord'[gid=" + graphId + "', pid=" + processId + "]";
    }

}

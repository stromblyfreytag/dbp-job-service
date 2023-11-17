package com.trustwave.dbpjobservice.workflow.light.db;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Collection;
import java.util.Date;

import com.googlecode.sarasvati.env.Env;
import com.trustwave.dbpjobservice.workflow.api.util.EscapingEncoder;

@Entity
@Table(name = "wfl_token")
public class LightTokenRecord {
    private long tokenId;
    private Long processId;
    private Long nodeId;
    private Date createDate;
    private String executionType;
    private String parentArcsStr;
    private String tokenSetName;
    private Integer threadId;
    private Date completeDate;
    private EnvironmentHolder envHolder = new EnvironmentHolder();

    public LightTokenRecord() {
    }

    private static EscapingEncoder getArcsEncoder() {
        return new EscapingEncoder('|', '^');
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id", nullable = false)
    public long getTokenId() {
        return tokenId;
    }

    public void setTokenId(long tokenId) {
        this.tokenId = tokenId;
    }

    @Column(name = "process_id", nullable = false)
    public Long getProcessId() {
        return processId;
    }

    public void setProcessId(Long processId) {
        this.processId = processId;
    }

    @Column(name = "node_id", nullable = false)
    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    @Column(name = "create_date", nullable = false)
    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    @Column(name = "execution_type", nullable = false)
    public String getExecutionType() {
        return executionType;
    }

    public void setExecutionType(String executionType) {
        this.executionType = executionType;
    }

    @Column(name = "tokenset_name")
    public String getTokenSetName() {
        return tokenSetName;
    }

    public void setTokenSetName(String tokenSetName) {
        this.tokenSetName = tokenSetName;
    }

    @Column(name = "thread_id")
    public Integer getThreadId() {
        return threadId;
    }

    public void setThreadId(Integer threadId) {
        this.threadId = threadId;
    }

    @Column(name = "complete_date")
    public Date getCompleteDate() {
        return completeDate;
    }

    public void setCompleteDate(Date completeDate) {
        this.completeDate = completeDate;
    }

    @Column(name = "envstring")
    public String getEnvString() {
        return envHolder.getEnvString();
    }

    public void setEnvString(String envString) {
        envHolder.setEnvString(envString);
    }

    @Transient
    public Env getEnv() {
        return envHolder.getEnv();
    }

    @Column(name = "parent_arcs")
    public String getParentArcsStr() {
        return parentArcsStr;
    }

    public void setParentArcsStr(String parentArcsStr) {
        this.parentArcsStr = parentArcsStr;
    }

    @Transient
    public Collection<String> getParentArcs() {
        return getArcsEncoder().decode(parentArcsStr);
    }

    public void setParentArcs(Collection<String> arcs) {
        parentArcsStr = getArcsEncoder().encode(arcs);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (tokenId ^ (tokenId >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
        LightTokenRecord other = (LightTokenRecord) obj;
		if (tokenId != other.tokenId) {
			return false;
		}
        return true;
    }
}

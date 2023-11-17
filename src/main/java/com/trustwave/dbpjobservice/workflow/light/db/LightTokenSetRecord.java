package com.trustwave.dbpjobservice.workflow.light.db;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.googlecode.sarasvati.env.Env;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "wfl_token_set")
@ToString
@EqualsAndHashCode
public class LightTokenSetRecord {
    private LightTokenSetId id;
    private int size;
    private boolean complete;
    private EnvironmentHolder envHolder = new EnvironmentHolder();

    public LightTokenSetRecord() {
    }

    public LightTokenSetRecord(long processId, String name) {
        this.id = new LightTokenSetId(processId, name);
    }

    @EmbeddedId
    @AttributeOverrides({
            @AttributeOverride(name = "processId", column = @Column(name = "process_id", nullable = false)),
            @AttributeOverride(name = "name", column = @Column(name = "name", nullable = false))})
    public LightTokenSetId getId() {
        return id;
    }

    public void setId(LightTokenSetId id) {
        this.id = id;
    }

    @Column(name = "size", nullable = false)
    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    @Column(name = "complete", nullable = false)
    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
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

}

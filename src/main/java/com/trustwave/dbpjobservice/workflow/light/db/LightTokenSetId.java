package com.trustwave.dbpjobservice.workflow.light.db;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@Embeddable
@ToString
@EqualsAndHashCode
public class LightTokenSetId implements Serializable {
    private static final long serialVersionUID = 1L;

    private long processId;
    private String name;

    public LightTokenSetId() {
    }

    public LightTokenSetId(long processId, String name) {
        this.processId = processId;
        this.name = name;
    }

    @Column(name = "process_id", nullable = false)
    public long getProcessId() {
        return processId;
    }

    public void setProcessId(long processId) {
        this.processId = processId;
    }

    @Column(name = "name", nullable = false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

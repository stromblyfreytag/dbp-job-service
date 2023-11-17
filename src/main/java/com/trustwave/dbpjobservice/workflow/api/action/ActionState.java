package com.trustwave.dbpjobservice.workflow.api.action;

public enum ActionState {
    INITIAL,
    RUNNING,
    WAITING,
    PAUSED,
    PAUSED_WAITING
    // COMPLETED - null indicates completed task; this is required to avoid
    //             special processing for 'state' parameter in JobAction.
    //             (non-null COMPLETED would pass to the next action as initial value)
    ;
}

package com.trustwave.dbpjobservice.workflow.api.action;

import com.trustwave.dbpjobservice.xml.XmlExitCondition;

public class ActionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private XmlExitCondition exitCondition;

    public ActionException(String message, XmlExitCondition exitCondition, Throwable cause) {
        super(message, cause);
        this.exitCondition = exitCondition;
    }

    public ActionException(String message, XmlExitCondition exitCondition) {
        super(message);
        this.exitCondition = exitCondition;
    }

    public ActionException(String message) {
        super(message);
        this.exitCondition = XmlExitCondition.GENERAL_FAILURE;
    }

    public XmlExitCondition getExitCondition() {
        return exitCondition;
    }
}

package com.ppm.integration.agilesdk.connector.jira.model;

/**
 * JIRASubTask are never transferred to PPM, but they are retrieved from JIRA and used to compute aggregated effort of a Task.
 */
public class JIRASubTask extends JIRAIssue {

    private String parentKey;

    public String getParentKey() {
        return parentKey;
    }

    public void setParentKey(String parentKey) {
        this.parentKey = parentKey;
    }
}

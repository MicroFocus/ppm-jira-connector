package com.ppm.integration.agilesdk.connector.jira.model;

import java.util.ArrayList;
import java.util.List;

public class IssueRetrievalResult {

    private List<JIRAIssue> issues = new ArrayList<>();

    private int startAt;

    private int maxResults;

    private int total;

    public IssueRetrievalResult(int startAt, int maxResults, int total) {
        this.startAt = startAt;
        this.maxResults = maxResults;
        this.total = total;
    }

    public List<JIRAIssue> getIssues() {
        return issues;
    }

    public void addIssue(JIRAIssue issue) {
        issues.add(issue);
    }

    public int getStartAt() {
        return startAt;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public int getTotal() {
        return total;
    }
}

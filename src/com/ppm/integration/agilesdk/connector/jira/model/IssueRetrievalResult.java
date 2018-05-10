package com.ppm.integration.agilesdk.connector.jira.model;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class IssueRetrievalResult {

    private List<JSONObject> issues = new ArrayList<>();

    private int startAt;

    private int maxResults;

    private int total;

    public IssueRetrievalResult(int startAt, int maxResults, int total) {
        this.startAt = startAt;
        this.maxResults = maxResults;
        this.total = total;
    }

    public List<JSONObject> getIssues() {
        return issues;
    }

    public void addIssue(JSONObject issue) {
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

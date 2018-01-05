package com.ppm.integration.agilesdk.connector.jira.model;

import com.ppm.integration.agilesdk.connector.jira.JIRAConstants;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This object contains "raw" (non-aggregated) timesheet data, with one line per JIRA issue.
 * It also contains some issue metadata (Epic ID, Project ID) to later group timesheet data into timesheet lines.
 *
 * This class is not thread safe.
 */
public class JIRATimesheetData {


    // Map<Issue Key, JIRAIssue>
    private Map<String, JIRAIssue> issues = new HashMap<>();

    // Map<Issue Key, Map<dateString, effort>>
    private Map<String, Map<String, Double>> effortPerIssue = new HashMap<>();

    // Map<Epic Key, Set<Issue Key>>
    private Map<String, Set<String>> issuesInEpics = new HashMap<>();

    // Map<Project Key, Set<Issue Key>>
    private Map<String, Set<String>> issuesInProjects = new HashMap<>();

    public Map<String, Map<String, Double>> getEffortPerIssue() {
        return effortPerIssue;
    }

    public Map<String, Set<String>> getIssuesInEpics() {
        return issuesInEpics;
    }

    public Map<String, Set<String>> getIssuesInProjects() {
        return issuesInProjects;
    }

    public Map<String, JIRAIssue> getIssues() {
        return issues;
    }

    public void addIssueEffort(JIRAIssue issue, String date, double effort) {
        Map<String, Double> efforts = effortPerIssue.get(issue.getKey());

        if (efforts == null) {

            // We don't know this issue, let's "index" it.
            issues.put(issue.getKey(), issue);

            // Issue in project
            String projectKey = issue.getProjectKey();
            Set<String> projectIssues = issuesInProjects.get(projectKey);
            if (projectIssues == null) {
                projectIssues = new HashSet<String>();
                issuesInProjects.put(projectKey, projectIssues);
            }
            projectIssues.add(issue.getKey());

            // Issue in Epic
            String epicKey = issue.getEpicKey();

            // Epics are in themselves.
            if (JIRAConstants.JIRA_ISSUE_EPIC.equalsIgnoreCase(issue.getType())) {
                epicKey = issue.getKey();
            }

            Set<String> epicIssues = issuesInEpics.get(epicKey);
            if (epicIssues == null) {
                epicIssues = new HashSet<String>();
                issuesInEpics.put(epicKey, epicIssues);
            }
            epicIssues.add(issue.getKey());

            // We create a new effort Map for this Issue.
            efforts =  new HashMap<>();
            effortPerIssue.put(issue.getKey(), efforts);
        }

        Double currentEffort = efforts.get(date);
        if (currentEffort == null) {
            currentEffort = 0d;
        }

        efforts.put(date, currentEffort + effort);
    }

    public boolean hasData() {
        for (Map<String, Double> effort : effortPerIssue.values()) {
            for (Double effortValue :  effort.values()) {
                if (effortValue != null && effortValue.doubleValue() > 0.0d) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean hasData(String issueKey) {

        Map<String, Double> effort = effortPerIssue.get(issueKey);

        if (effort == null) {
            return false;
        }

        for (Double effortValue :  effort.values()) {
            if (effortValue != null && effortValue.doubleValue() > 0.0d) {
                return true;
            }
        }


        return false;
    }
}

package com.ppm.integration.agilesdk.connector.jira.model;

import com.ppm.integration.agilesdk.pm.ExternalTask;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class JIRAEntity extends ExternalTask {
    private JIRAIssue issue;

    public void setIssue(JIRAIssue issue) {
        this.issue = issue;
    }

    @Override public TaskStatus getStatus() {
        return getTaskStatus(issue.getStatus());
    }

    @Override public Date getScheduledStart() {
        return getDate(issue.getScheduledStart());
    }

    @Override public Date getScheduledFinish() {
        return getDate(issue.getScheduledFinish());
    }

    @Override public String getName() {
        return "[" + issue.getType() + "] " + issue.getName();
    }

    @Override public String getId() {

        return null;
    }

    @Override public List<ExternalTask> getChildren() {

        List<JIRAIssue> list = issue.getChildren();
        List<ExternalTask> entityList = new ArrayList<>();
        if (list != null) {
            for (JIRAIssue issue : list) {
                JIRAEntity entity = new JIRAEntity();
                entity.setIssue(issue);
                entityList.add(entity);
            }

        }
        return entityList;

    }

    private TaskStatus getTaskStatus(String status) {
        switch (status) {
            case "To Do":
                return TaskStatus.IN_PLANNING;
            case "In Progress":
                return TaskStatus.IN_PROGRESS;
            case "Done":
                return TaskStatus.READY;
            default:
                return TaskStatus.UNKNOWN;
        }
    }

    private Date getDate(String dateStr) {
        if (dateStr != null) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-mm-dd");
            Date d = null;
            try {
                d = format.parse(dateStr.substring(0, 10));
            } catch (ParseException e) {
                return new Date();
            }
            return d;
        } else {
            return new Date();
        }
    }

}

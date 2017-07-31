
package com.ppm.integration.agilesdk.connector.jira.model;

import com.ppm.integration.agilesdk.pm.ExternalTask;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public abstract class JIRAIssue extends JIRAEntity {

    private String type;

    private String authorName;

    private String creationDate;

    private String lastUpdateDate;

    private String epicKey;

    private String sprintId;

    private String status;

    private String priorityName;

    private List<String> fixVersionIds;

    private Long storyPoints;

    private long assigneePpmUserId = -1;

    private JIRAIssueWork work = new JIRAIssueWork();

    public boolean hasWork() {
        if (getWork() == null) {
            return false;
        }

        if (getWork().getTimeSpentHours() != null && getWork().getTimeSpentHours() > 0) {
            return true;
        }

        if (getWork().getRemainingEstimateHours() != null && getWork().getRemainingEstimateHours() > 0) {
            return true;
        }

        if (getWork().getWorklogs() == null) {
            return false;
        }

        for (JIRAIssueWork.JIRAWorklogEntry worklog : getWork().getWorklogs()) {
            if (worklog != null) {
                if (worklog.getTimeSpentHours() > 0) {
                    return true;
                }
            }
        }

        return false;
    }

    public String getAgileDataBacklogItemType() {
        if (StringUtils.isBlank(type)) {
            return null;
        }

        switch(type) {
            case "Bug":
                return "defect";
            case "Story":
                return "story";
            default:
                return type;
        }
    }

    public String getFullTaskName() {
        return "[" + this.type + "] " + getName();
    }

    public ExternalTask.TaskStatus getExternalTaskStatus() {
        switch (status) {
            case "To Do":
                return ExternalTask.TaskStatus.IN_PLANNING;
            case "In Progress":
                return ExternalTask.TaskStatus.IN_PROGRESS;
            case "Done":
                return ExternalTask.TaskStatus.COMPLETED;
            default:
                return ExternalTask.TaskStatus.UNKNOWN;
        }
    }

    /**
     * Some logic about actuals requires to know whether the task is considered Done or not.
     * If you create new statuses that would classify as "Done", you need to change this method accordingly.
     */
    public boolean isDone() {
        return "Done".equalsIgnoreCase(getStatus());
    }


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(String lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    public String getEpicKey() {
        return epicKey;
    }

    public void setEpicKey(String epicKey) {
        this.epicKey = epicKey;
    }

    public String getSprintId() {
        return sprintId;
    }

    public void setSprintId(String sprintId) {
        this.sprintId = sprintId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriorityName() {
        return priorityName;
    }

    public void setPriorityName(String priorityName) {
        this.priorityName = priorityName;
    }

    public List<String> getFixVersionIds() {
        return fixVersionIds;
    }

    public void setFixVersionIds(List<String> fixVersionIds) {
        this.fixVersionIds = fixVersionIds;
    }

    public Long getStoryPoints() {
        return storyPoints;
    }

    public void setStoryPoints(Long storyPoints) {
        this.storyPoints = storyPoints;
    }

    public long getAssigneePpmUserId() {
        return assigneePpmUserId;
    }

    public void setAssigneePpmUserId(Long assigneePpmUserId) {
        this.assigneePpmUserId = assigneePpmUserId == null ? -1 : assigneePpmUserId;
    }

    public JIRAIssueWork getWork() {
        return work;
    }
}

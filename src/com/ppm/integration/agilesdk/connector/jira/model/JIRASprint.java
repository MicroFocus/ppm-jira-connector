
package com.ppm.integration.agilesdk.connector.jira.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.ppm.integration.agilesdk.pm.ExternalTask;
import com.ppm.integration.agilesdk.pm.ExternalTaskActuals;

import edu.emory.mathcs.backport.java.util.Arrays;

public class JIRASprint extends JIRAEntity {
    private String sprintId;

    private String self;

    private String state;

    private String sprintName;

    private String startDate;

    private String endDate;

    private String completeDate;

    private String originBoardId;

    private String goal;

    private List<JIRAIssue> issues;

    private boolean isBreakdown;

    public JIRASprint(String sprintId, String state, String sprintName, String startDate, String endDate) {
        this.sprintId = sprintId;
        this.state = state;
        this.sprintName = sprintName;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getSprintId() {
        return sprintId;
    }

    public void setSprintId(String sprintId) {
        this.sprintId = sprintId;
    }

    public String getSprintName() {
        return sprintName;
    }

    public void setSprintName(String sprintName) {
        this.sprintName = sprintName;
    }

    public String getSelf() {
        return self;
    }

    public void setSelf(String self) {
        this.self = self;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getCompleteDate() {
        return completeDate;
    }

    public void setCompleteDate(String completeDate) {
        this.completeDate = completeDate;
    }

    public String getOriginBoardId() {
        return originBoardId;
    }

    public void setOriginBoardId(String originBoardId) {
        this.originBoardId = originBoardId;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public List<JIRAIssue> getIssues() {
        return issues;
    }

    public void setIssues(List<JIRAIssue> issues) {
        this.issues = issues;
    }

    public void setBreakdown(boolean isBreakdown) {
        this.isBreakdown = isBreakdown;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((endDate == null) ? 0 : endDate.hashCode());
        result = prime * result + ((sprintId == null) ? 0 : sprintId.hashCode());
        result = prime * result + ((sprintName == null) ? 0 : sprintName.hashCode());
        result = prime * result + ((startDate == null) ? 0 : startDate.hashCode());
        result = prime * result + ((state == null) ? 0 : state.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        JIRASprint other = (JIRASprint)obj;
        if (endDate == null) {
            if (other.endDate != null)
                return false;
        } else if (!endDate.equals(other.endDate))
            return false;
        if (sprintId == null) {
            if (other.sprintId != null)
                return false;
        } else if (!sprintId.equals(other.sprintId))
            return false;
        if (sprintName == null) {
            if (other.sprintName != null)
                return false;
        } else if (!sprintName.equals(other.sprintName))
            return false;
        if (startDate == null) {
            if (other.startDate != null)
                return false;
        } else if (!startDate.equals(other.startDate))
            return false;
        if (state == null) {
            if (other.state != null)
                return false;
        } else if (!state.equals(other.state))
            return false;
        return true;
    }

    @Override
    public TaskStatus getStatus() {
        return getTaskStatus(this.state);
    }

    @Override
    public Date getScheduledStart() {
        return checkDate(this.startDate);
    }

    @Override
    public Date getScheduledFinish() {
        return checkDate(this.endDate);
    }

    @Override
    public String getName() {
        return "[Sprint] " + this.sprintName;
    }

    @Override
    public String getId() {
        return this.sprintId;
    }

    @Override
    public List<ExternalTask> getChildren() {
        if (isBreakdown) {
            List<ExternalTask> ets = new ArrayList<>();
            for (JIRAIssue issue : this.issues) {
                ets.add((ExternalTask)issue);
            }
            return ets;
        }
        return null;
    }

    private TaskStatus getTaskStatus(String status) {

        switch (this.state) {
            case "CLOSED":
                return TaskStatus.COMPLETED;
            case "FUTURE":
                return TaskStatus.READY;
            case "ACTIVE":
                return TaskStatus.ACTIVE;
            default:
                // return TaskStatus.UNKNOWN; the status unkonwn will cause an
                // exception
                return TaskStatus.ON_HOLD;
        }
    }

    @Override
    public List<ExternalTaskActuals> getActuals() {
        ExternalTaskActuals eta = new ExternalTaskActuals() {

            @Override
            public double getPercentComplete() {

                if (!isBreakdown) {
                    double total = 0.0;
                    double done = 0.0;
                    for (JIRAIssue ji : issues) {
                        double scheduledEffort = ji.getScheduledEffort();
                        total += scheduledEffort;
                        done += Double.parseDouble(ji.getPercentComplete()) * scheduledEffort;
                    }
                    if (total != 0) {
                        return done / total;
                    }

                }
                return super.getPercentComplete();
            }

            @Override
            public double getScheduledEffort() {
                if (!isBreakdown) {
                    double actualEffort = 0.0;

                    for (JIRAIssue ji : issues) {
                        actualEffort += ji.getScheduledEffort();
                    }
                    return actualEffort / 3600.0;
                }

                return super.getScheduledEffort();
            }
        };
        return Arrays.asList(new ExternalTaskActuals[] {eta});
    }
}

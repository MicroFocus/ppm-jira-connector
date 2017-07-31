
package com.ppm.integration.agilesdk.connector.jira.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.ppm.integration.agilesdk.pm.ExternalTask;
import com.ppm.integration.agilesdk.pm.ExternalTaskActuals;

import edu.emory.mathcs.backport.java.util.Arrays;

public class JIRASprint extends JIRAEntity {
    private String state;

    private String startDate;

    private String endDate;

    private String completeDate;

    private String originBoardId;

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getStartDate() {
        return startDate;
    }

    public Date getStartDateAsDate() {
        return convertToDate(startDate);
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public Date getEndDateAsDate() {
        return convertToDate(endDate);
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

    public ExternalTask.TaskStatus getExternalTaskStatus(String status) {

        switch (this.state) {
            case "CLOSED":
                return ExternalTask.TaskStatus.COMPLETED;
            case "FUTURE":
                return ExternalTask.TaskStatus.READY;
            case "ACTIVE":
                return ExternalTask.TaskStatus.ACTIVE;
            default:
                // return TaskStatus.UNKNOWN; the status unkonwn will cause an
                // exception
                return ExternalTask.TaskStatus.ON_HOLD;
        }
    }
 }

package com.ppm.integration.agilesdk.connector.jira.model;

import com.ppm.integration.agilesdk.tm.ExternalWorkItem;
import com.ppm.integration.agilesdk.tm.ExternalWorkItemEffortBreakdown;
import com.ppm.integration.agilesdk.tm.TimeSheetLineAgileEntityInfo;

import java.util.Date;

public class JIRAExternalWorkItem extends ExternalWorkItem {


    private String name;

    private TimeSheetLineAgileEntityInfo info;

    private ExternalWorkItemEffortBreakdown effort = new ExternalWorkItemEffortBreakdown();

    public JIRAExternalWorkItem(String name, String projectKey, String epicKey, String issueKey) {
        this.name = name;
        this.info = new TimeSheetLineAgileEntityInfo(projectKey);
        info.setEpicId(epicKey);
        info.setBacklogItemId(issueKey);
    }

    public String getName() {
        return name;
    }

    @Override public Double getTotalEffort() {
        double totalEffort = 0d;
        for (Double effort : getEffortBreakDown().getEffortList().values()) {
            totalEffort += effort;
        }
        return roundValue(totalEffort);
    }

    public ExternalWorkItemEffortBreakdown getEffortBreakDown() {
        return effort;
    }

    public TimeSheetLineAgileEntityInfo getLineAgileEntityInfo() {
        return info;
    }

    public void addEffort(String date, double value) {
        value = roundValue(value);
        getEffortBreakDown().addEffort(date, value);
    }

    public void addEffort(Date date, double value) {
        value = roundValue(value);
        getEffortBreakDown().addEffort(date, value);
    }

    private double roundValue(double value) {
        // We have only 2 digits and small value, so bad rounding due to double shouldn't occur hopefully, no need to go to BigDecimal.
        return Math.round(value * 100d) / 100d;
    }
}

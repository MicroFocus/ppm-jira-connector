
package com.ppm.integration.agilesdk.connector.jira.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.ppm.integration.agilesdk.connector.jira.JIRAWorkPlanIntegration;
import com.ppm.integration.agilesdk.pm.ExternalTask;
import com.ppm.integration.agilesdk.pm.ExternalTaskActuals;

import edu.emory.mathcs.backport.java.util.Arrays;

public class JIRAEpic extends JIRASubTaskableIssue {

    private List<JIRASubTaskableIssue> contents = new ArrayList<JIRASubTaskableIssue>();

    public List<JIRASubTaskableIssue> getContents() {
        return contents;
    }

    public void addContent(JIRASubTaskableIssue issue) {
        contents.add(issue);
    }

    // Sums up the Story Points from all the Epic contents
    public int getAggregatedStoryPoints() {

        int aggSP = 0;

        for (JIRASubTaskableIssue content : contents) {
            aggSP += content.getStoryPoints() == null ? 0 : content.getStoryPoints();
        }

        return aggSP;
    }

    public int getDoneStoryPoints() {

        int doneSP = 0;

        for (JIRASubTaskableIssue content : contents) {
            if (content.isDone()) {
                doneSP += content.getStoryPoints() == null ? 0 : content.getStoryPoints();
            }
        }

        return doneSP;

    }

    /**
     * @return an estimation of the finish date of this Epic, used for inserting Epics as Milestones.
     */
    public Date getEstimatedFinishDate(Map<String, JIRASprint> sprintsInfo) {

        Date epicFinishDate = getSprintFinishDate(this, sprintsInfo);

        if (epicFinishDate != null) {
            return epicFinishDate;
        }

        epicFinishDate = JIRAWorkPlanIntegration.getDefaultStartDate();

        for (JIRAIssue issue : getContents()) {
            Date issueSprintEndDate = getSprintFinishDate(issue, sprintsInfo);

            if (issueSprintEndDate == null) {
                continue;
            }

            if (issueSprintEndDate.after(epicFinishDate)) {
                epicFinishDate = issueSprintEndDate;
            }
        }

        return epicFinishDate;
    }

    private Date getSprintFinishDate(JIRAIssue issue, Map<String, JIRASprint> sprintsInfo) {
        JIRASprint sprint = sprintsInfo != null ? sprintsInfo.get(issue.getSprintId()) : null;

        if (sprint == null) {
            return null;
        }

        return sprint.getEndDateAsDate();
    }
}
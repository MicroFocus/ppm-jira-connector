
package com.ppm.integration.agilesdk.connector.jira.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
    public long getAggregatedStoryPoints() {

        long aggSP = 0;

        for (JIRASubTaskableIssue content : contents) {
            aggSP += content.getStoryPoints() == null ? 0 : content.getStoryPoints();
        }

        return aggSP;
    }
}
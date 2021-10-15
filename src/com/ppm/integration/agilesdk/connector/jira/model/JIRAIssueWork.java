
package com.ppm.integration.agilesdk.connector.jira.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class JIRAIssueWork extends JIRABase {

    private Double remainingEstimateHours;

    private Double timeSpentHours;

    private List<JIRAWorklogEntry> worklogs = new ArrayList<JIRAWorklogEntry>();

    public List<JIRAWorklogEntry> getWorklogs() {
        return worklogs;
    }

    public void addWorklogEntry(JIRAWorklogEntry entry) {
        worklogs.add(entry);
    }

    public static double secToHours(int timeSpentSeconds) {
        return ((double)timeSpentSeconds) / 3600d;
    }

    public Double getRemainingEstimateHours() {
        return remainingEstimateHours;
    }

    public void setRemainingEstimateHours(Double remainingEstimateHours) {
        this.remainingEstimateHours = remainingEstimateHours;
    }

    public Double getTimeSpentHours() {
        return timeSpentHours;
    }

    public void setTimeSpentHours(Double timeSpentHours) {
        this.timeSpentHours = timeSpentHours;
    }

    public static class JIRAWorklogEntry extends JIRABase {

        private double timeSpentHours = 0;

        private String dateStarted;

        private String authorEmail;

        private String authorKey;

        private String authorName;

        public double getTimeSpentHours() {
            return timeSpentHours;
        }

        public void setTimeSpentHours(double timeSpentHours) {
            this.timeSpentHours = timeSpentHours;
        }

        public String getDateStarted() {
            return dateStarted;
        }

        public String getDateStartedAsSimpleDate() {
            return convertToSimpleDate(dateStarted);
        }

        public Date getDateStartedAsDate() {
            return convertToDate(dateStarted);
        }


        public void setDateStarted(String dateStarted) {
            this.dateStarted = dateStarted;
        }

        public String getAuthorEmail() {
            return authorEmail;
        }

        public void setAuthorEmail(String authorEmail) {
            this.authorEmail = authorEmail;
        }

        public String getAuthorKey() {
            return authorKey;
        }

        public void setAuthorKey(String authorKey) {
            this.authorKey = authorKey;
        }

        public String getAuthorName() {
            return authorName;
        }

        public void setAuthorName(String authorName) {
            this.authorName = authorName;
        }
    }

    public static JIRAWorklogEntry getWorklogEntryFromWorklogJSONObject(JSONObject obj) {
        if (obj == null) {
            return null;
        }

        JIRAWorklogEntry worklog = new JIRAWorklogEntry();

        try {

            if (obj.has("author") && obj.getJSONObject("author").has("emailAddress")) {
                worklog.setAuthorEmail(obj.getJSONObject("author").getString("emailAddress"));
            }

            if (obj.has("author") && obj.getJSONObject("author").has("key")) {
                worklog.setAuthorKey(obj.getJSONObject("author").getString("key"));
            }

            if (obj.has("author") && obj.getJSONObject("author").has("name")) {
                worklog.setAuthorName(obj.getJSONObject("author").getString("name"));
            }

            worklog.setDateStarted(obj.getString("started"));

            if (obj.has("timeSpentSeconds") && !obj.isNull("timeSpentSeconds")) {
                worklog.setTimeSpentHours(secToHours(obj.getInt("timeSpentSeconds")));
            }

        } catch (JSONException e) {
            throw new RuntimeException("Error while retrieving worklog info", e);
        }

        return worklog;
    }
}

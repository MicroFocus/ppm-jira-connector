package com.ppm.integration.agilesdk.connector.jira;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.agiledata.*;
import com.ppm.integration.agilesdk.connector.jira.model.*;

import java.util.*;

/**
 * @see AgileDataIntegration
 * @since PPM 9.42
 */
public class JIRAAgileDataIntegration extends AgileDataIntegration {

    private JIRAServiceProvider.JIRAService service = null;

    private AgileDataProject project = null;

    private List<AgileDataSprint> sprints = null;

    private List<AgileDataEpic> epics = null;

    List<AgileDataBacklogItem> bugsAndStories;

    List<AgileDataRelease> releases;

    @Override public void setUp(ValueSet configuration, String projectKey) {
        service = (new JIRAServiceProvider()).useAdminAccount().get(configuration);
        loadProject(projectKey);
        loadVersions(projectKey);
        loadSprints(projectKey);
        loadEpics(projectKey);
        loadBugsAndStories(projectKey);

        
    }

    private void loadVersions(String projectKey) {

        List<JIRAVersion> versions = service.getVersions(projectKey);

        releases = new ArrayList<AgileDataRelease>(versions.size());

        for (JIRAVersion version : versions) {
            AgileDataRelease release = new AgileDataRelease();
            release.setReleaseId(version.getId());
            release.setName(version.getName());
            releases.add(release);
        }
    }

    private void loadBugsAndStories(String projectKey) {

        // We retrieve all bug/stories, even if they are not scheduled in a sprint for now
        List<JIRASubTaskableIssue> issues = service.getAllIssues(projectKey, JIRAConstants.JIRA_ISSUE_STORY, JIRAConstants.JIRA_ISSUE_BUG);

        bugsAndStories = new ArrayList<AgileDataBacklogItem>(issues.size());

        for (JIRASubTaskableIssue issue: issues) {
            AgileDataBacklogItem backlogItem = new AgileDataBacklogItem();
            backlogItem.setAuthor(issue.getAuthorName());
            backlogItem.setBacklogItemId(issue.getKey());
            backlogItem.setSprintId(issue.getSprintId());
            backlogItem.setBacklogType(issue.getAgileDataBacklogItemType());
            backlogItem.setStatus(issue.getStatus());
            backlogItem.setEpicId(issue.getEpicKey());
            backlogItem.setName(issue.getName());
            backlogItem.setPriority(issue.getPriorityName());
            backlogItem.setStoryPoints(issue.getStoryPoints() == null ? 0 : issue.getStoryPoints().intValue());
            if (issue.getFixVersionIds() != null && !issue.getFixVersionIds().isEmpty()) {
                // JIRA supports putting an item in multiple version (smart choice!), but PPM Agile Data tables don't.
                backlogItem.setReleaseId(issue.getFixVersionIds().get(0));
            }

            // No feature in JIRA
            backlogItem.setFeatureId(null);




            bugsAndStories.add(backlogItem);
        }
    }

    private void loadEpics(String projectKey) {

        // We retrieve all bug/stories, even if they are not scheduled in a sprint for now
        List<JIRASubTaskableIssue> jiraEpics = service.getAllIssues(projectKey, JIRAConstants.JIRA_ISSUE_EPIC);

        epics = new ArrayList<AgileDataEpic>(jiraEpics.size());

        for (JIRASubTaskableIssue jiraEpic : jiraEpics) {
            AgileDataEpic epic = new AgileDataEpic();
            epic.setName(jiraEpic.getName());
            epic.setEpicId(jiraEpic.getKey());
            epic.setAuthor(jiraEpic.getAuthorName());
            epic.setPlannedStoryPoints(jiraEpic.getStoryPoints() == null ? 0 : jiraEpic.getStoryPoints().intValue());

            epics.add(epic);
        }
    }

    /**
     * Jira doesn't have the notion of Features built-in.
     * If you have a JIRA plugin to provide this functionality you can modify this code to account for this.
     */
    public List<AgileDataFeature> getFeatures() {return null;}


    public List<AgileDataBacklogItem> getBacklogItems() {return bugsAndStories;}


    private void loadSprints(String projectKey) {
        List<JIRASprint> jiraSprints = service.getAllSprints(projectKey);

        sprints = new ArrayList<AgileDataSprint>(jiraSprints == null ? 0 : jiraSprints.size());

        if (jiraSprints == null) {
            return;
        }

        for (JIRASprint jiraSprint : jiraSprints) {
            AgileDataSprint sprint = new AgileDataSprint();
            // Sprints are not linked to a specific Release/Version in JIRA.
            sprint.setReleaseId(null);
            sprint.setName(jiraSprint.getName());
            sprint.setSprintId(jiraSprint.getKey());
            sprint.setStartDate(jiraSprint.getStartDateAsDate());
            sprint.setFinishDate(jiraSprint.getEndDateAsDate());
            sprints.add(sprint);
        }
    }

    private void loadProject(String projectKey) {
        project = new AgileDataProject();
        project.setProjectId(projectKey);
        JIRAProject jp = service.getProject(projectKey);
        project.setName(jp.getName());
    }

    public AgileDataProject getProject() {return project;}

    /**
     * We do not have any notion of Programs in OOTB Jira.
     */
    public List<AgileDataProgram> getPrograms() {return null;}

    /**
     * JIRA has groups, but these are different from teams and are mostly used for administration purpose.
     * There is a notion of teams, but only in JIRA portfolio, which is not suppported as of PPM 9.42 (as JIRA offers no proper REST interfaces to integrate with it).<br/>
     * As a result, we do not synchronize team information from JIRA.
     */
    public List<AgileDataTeam> getTeams() {return null;}

    /**
     * We only return the sprints for Scrum Boards. Kanban Boards are not synched.
     */
    public List<AgileDataSprint> getSprints() {
        return sprints;
    }

    /**
     * Epics from this project in JIRA
     */
    public List<AgileDataEpic> getEpics() {return epics;}

    public List<AgileDataRelease> getReleases() {return releases;}

    @Override
    public List<AgileDataBacklogConfig> getAgileDataBacklogConfig(ValueSet configuration) {

        // Defects
        List<AgileDataBacklogConfig> list = new ArrayList<AgileDataBacklogConfig>();
        AgileDataBacklogConfig config = new AgileDataBacklogConfig();
        config.setBacklogStatus("TO DO");
        config.setColor("grey");
        config.setBacklogType("Bug");
        config.setIsFinishStatus(false);
        list.add(config);

        config = new AgileDataBacklogConfig();
        config.setBacklogStatus("IN PROGRESS");
        config.setColor("yellow");
        config.setBacklogType("Bug");
        config.setIsFinishStatus(false);
        list.add(config);

        config = new AgileDataBacklogConfig();
        config.setBacklogStatus("DONE");
        config.setColor("green");
        config.setBacklogType("Bug");
        config.setIsFinishStatus(true);
        list.add(config);

        // Stories
        config = new AgileDataBacklogConfig();
        config.setBacklogStatus("TO DO");
        config.setColor("grey");
        config.setBacklogType("Story");
        config.setIsFinishStatus(false);
        list.add(config);

        config = new AgileDataBacklogConfig();
        config.setBacklogStatus("IN PROGRESS");
        config.setColor("yellow");
        config.setBacklogType("Story");
        config.setIsFinishStatus(false);
        list.add(config);

        config = new AgileDataBacklogConfig();
        config.setBacklogStatus("DONE");
        config.setColor("green");
        config.setBacklogType("Story");
        config.setIsFinishStatus(true);
        list.add(config);

        return list;
    }

    @Override
    public List<AgileDataBacklogSeverity> getAgileDataBacklogSeverity(ValueSet configuration) {
        List<AgileDataBacklogSeverity> list = new ArrayList<AgileDataBacklogSeverity>();
        AgileDataBacklogSeverity severity = new AgileDataBacklogSeverity();
        severity.setBacklogType("Bug");
        severity.setSeverity("Highest");
        severity.setSeverityIndex(1);
        list.add(severity);

        severity = new AgileDataBacklogSeverity();
        severity.setBacklogType("Bug");
        severity.setSeverity("High");
        severity.setSeverityIndex(2);
        list.add(severity);

        severity = new AgileDataBacklogSeverity();
        severity.setBacklogType("Bug");
        severity.setSeverity("Medium");
        severity.setSeverityIndex(3);
        list.add(severity);

        severity = new AgileDataBacklogSeverity();
        severity.setBacklogType("Bug");
        severity.setSeverity("Low");
        severity.setSeverityIndex(4);
        list.add(severity);

        severity = new AgileDataBacklogSeverity();
        severity.setBacklogType("Bug");
        severity.setSeverity("Lowest");
        severity.setSeverityIndex(5);
        list.add(severity);



        return list;
    }
}

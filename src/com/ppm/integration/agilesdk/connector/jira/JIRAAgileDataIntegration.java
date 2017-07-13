package com.ppm.integration.agilesdk.connector.jira;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.agiledata.*;
import com.ppm.integration.agilesdk.connector.jira.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Map<String, Boolean> types = new HashMap<String, Boolean>();
        types.put(JIRAConstants.JIRA_ISSUE_STORY, Boolean.TRUE);
        types.put(JIRAConstants.JIRA_ISSUE_BUG, Boolean.TRUE);

        // We retrieve all bug/stories, even if they are not scheduled in a sprint for now
        List<JIRAIssue> issues = service.getIssues(projectKey, types, false);

        bugsAndStories = new ArrayList<AgileDataBacklogItem>(issues.size());

        for (JIRAIssue issue: issues) {
            AgileDataBacklogItem backlogItem = new AgileDataBacklogItem();
            backlogItem.setAuthor(issue.getResources());
            backlogItem.setBacklogItemId(issue.getKey());
            backlogItem.setSprintId(issue.getSprintKey());
            backlogItem.setBacklogType(issue.getType());
            backlogItem.setAuthor(issue.getResources());
            backlogItem.setStatus(issue.getStatusName());
            backlogItem.setEpicId(issue.getEpicKey());
            backlogItem.setName(issue.getIssueName());
            backlogItem.setPriority(issue.getPriorityName());
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
        List<JIRAEpic> jiraEpics = service.getEpics(projectKey);

        epics = new ArrayList<AgileDataEpic>(jiraEpics.size());

        for (JIRAEpic jiraEpic : jiraEpics) {
            AgileDataEpic epic = new AgileDataEpic();
            epic.setName(jiraEpic.getIssueName());
            epic.setEpicId(jiraEpic.getKey());
            epic.setAuthor(jiraEpic.getResources());
            // story points info is not easily available in JIRA API.
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
        List<JIRASprint> jiraSprints = service.getSprints(projectKey);

        sprints = new ArrayList<AgileDataSprint>(jiraSprints == null ? 0 : jiraSprints.size());

        if (jiraSprints == null) {
            return;
        }

        for (JIRASprint jiraSprint : jiraSprints) {
            AgileDataSprint sprint = new AgileDataSprint();
            // Sprints are not linked to a specific Release/Version in JIRA.
            sprint.setReleaseId(null);
            sprint.setName(jiraSprint.getName());
            sprint.setSprintId(jiraSprint.getSprintId());
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
        list.add(severity);

        severity = new AgileDataBacklogSeverity();
        severity.setBacklogType("Bug");
        severity.setSeverity("High");
        list.add(severity);

        severity = new AgileDataBacklogSeverity();
        severity.setBacklogType("Bug");
        severity.setSeverity("Medium");
        list.add(severity);

        severity = new AgileDataBacklogSeverity();
        severity.setBacklogType("Bug");
        severity.setSeverity("Low");
        list.add(severity);

        severity = new AgileDataBacklogSeverity();
        severity.setBacklogType("Bug");
        severity.setSeverity("Lowest");
        list.add(severity);



        return list;
    }
}

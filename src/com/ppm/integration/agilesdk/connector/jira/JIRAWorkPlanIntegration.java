
package com.ppm.integration.agilesdk.connector.jira;

import com.hp.ppm.user.model.User;
import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.jira.model.*;
import com.ppm.integration.agilesdk.connector.jira.rest.util.exception.JIRAConnectivityExceptionHandler;
import com.ppm.integration.agilesdk.connector.jira.rest.util.exception.RestRequestException;
import com.ppm.integration.agilesdk.connector.jira.util.WorkDrivenPercentCompleteExternalTask;
import com.ppm.integration.agilesdk.pm.*;
import com.ppm.integration.agilesdk.provider.UserProvider;
import com.ppm.integration.agilesdk.ui.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wink.client.ClientRuntimeException;

import java.util.*;

public class JIRAWorkPlanIntegration extends WorkPlanIntegration {
    private final Logger logger = Logger.getLogger(this.getClass());

    private JIRAServiceProvider service = new JIRAServiceProvider();

    public JIRAWorkPlanIntegration() {}

    @Override
    public List<Field> getMappingConfigurationFields(WorkPlanIntegrationContext context, ValueSet values) {

        List<Field> fields = Arrays.asList(new Field[] {new PlainText(JIRAConstants.KEY_USERNAME, "USERNAME", "", true),

                new PasswordText(JIRAConstants.KEY_PASSWORD, "PASSWORD", "", true), new LineBreaker(),

                new DynamicDropdown(JIRAConstants.KEY_JIRA_PROJECT, "JIRA_PROJECT", true) {

                    @Override
                    public List<String> getDependencies() {
                        return Arrays.asList(new String[] {JIRAConstants.KEY_USERNAME, JIRAConstants.KEY_PASSWORD});
                    }

                    @Override
                    public List<Option> getDynamicalOptions(ValueSet values) {

                        List<JIRAProject> list = new ArrayList<>();
                        try {
                            list = service.get(values).getProjects();
                        } catch (ClientRuntimeException | RestRequestException e) {
                            logger.error("", e);
                            new JIRAConnectivityExceptionHandler().uncaughtException(Thread.currentThread(), e,
                                    JIRAWorkPlanIntegration.class);
                        } catch (RuntimeException e) {
                            logger.error("", e);
                            new JIRAConnectivityExceptionHandler().uncaughtException(Thread.currentThread(), e,
                                    JIRAWorkPlanIntegration.class);
                        }

                        List<Option> optionList = new ArrayList<>();
                        for (JIRAProject project : list) {
                            Option option = new Option(project.getKey(), project.getName());
                            optionList.add(option);
                        }
                        return optionList;
                    }

                },
                new LineBreaker(),
                new DynamicDropdown(JIRAConstants.KEY_IMPORT_SELECTION, "IMPORT_SELECTION",
                        JIRAConstants.IMPORT_ALL_PROJECT_ISSUES, "", true) {

                    @Override
                    public List<String> getDependencies() {
                        return new ArrayList<String>();
                    }

                    @Override
                    public List<Option> getDynamicalOptions(ValueSet values) {

                        List<Option> optionList = new ArrayList<>();

                        Option option1 =
                                new Option(JIRAConstants.IMPORT_ALL_PROJECT_ISSUES, "All project issues");
                        Option option2 = new Option(JIRAConstants.IMPORT_ONE_EPIC, "One Epic");
                        Option option3 = new Option(JIRAConstants.IMPORT_ONE_VERSION, "One Version");
                        Option option4 = new Option(JIRAConstants.IMPORT_ONE_BOARD, "One Board");

                        optionList.add(option1);
                        optionList.add(option2);
                        optionList.add(option3);
                        optionList.add(option4);

                        return optionList;
                    }

                },
                new DynamicDropdown(JIRAConstants.KEY_IMPORT_SELECTION_DETAILS, "IMPORT_SELECTION_DETAILS", "All project issues", false) {

                    @Override
                    public List<String> getDependencies() {

                        return Arrays.asList(
                                new String[] {JIRAConstants.KEY_JIRA_PROJECT, JIRAConstants.KEY_IMPORT_SELECTION});
                    }

                    @Override
                    public List<Option> getDynamicalOptions(ValueSet values) {
                        String importSelection = values.get(JIRAConstants.KEY_IMPORT_SELECTION);
                        String projectKey = values.get(JIRAConstants.KEY_JIRA_PROJECT);
                        List<Option> options = new ArrayList<>();
                        switch (importSelection) {
                            case JIRAConstants.IMPORT_ALL_PROJECT_ISSUES:
                                // No extra option when importing everything, it will be ignored anyway.
                                options.add(new Option("0", "All project issues"));
                                break;
                            case JIRAConstants.IMPORT_ONE_EPIC:
                                List<JIRASubTaskableIssue> epics = service.get(values).getAllIssues(projectKey, JIRAConstants.JIRA_ISSUE_EPIC);
                                for (JIRAIssue epic : epics) {
                                    Option option = new Option(epic.getKey(), epic.getName());
                                    options.add(option);
                                }
                                break;
                            case JIRAConstants.IMPORT_ONE_BOARD:
                                List<JIRABoard> boards = service.get(values).getAllBoards(projectKey);
                                for (JIRABoard board : boards) {
                                    Option option = new Option(board.getKey(), board.getName());
                                    options.add(option);

                                }
                                break;
                            case JIRAConstants.IMPORT_ONE_VERSION:
                                List<JIRAVersion> versions = service.get(values).getVersions(projectKey);
                                for (JIRAVersion version : versions) {
                                    Option option = new Option(version.getId(), version.getName());
                                    options.add(option);

                                }
                                break;
                        }
                        return options;
                    }

                },
                new LineBreaker(),
                new DynamicDropdown(JIRAConstants.KEY_IMPORT_GROUPS, "IMPORT_GROUPS",
                        JIRAConstants.GROUP_EPIC, "", true) {

                    @Override
                    public List<String> getDependencies() {
                        return new ArrayList<String>();
                    }

                    @Override
                    public List<Option> getDynamicalOptions(ValueSet values) {

                        List<Option> optionList = new ArrayList<>();

                        Option option1 = new Option(JIRAConstants.GROUP_SPRINT, "Sprint");
                        Option option2 = new Option(JIRAConstants.GROUP_STATUS, "Status (Kanban)");
                        Option option3 = new Option(JIRAConstants.GROUP_EPIC, "Epics");

                        optionList.add(option1);
                        optionList.add(option2);
                        optionList.add(option3);

                        return optionList;
                    }

                },
                new DynamicDropdown(JIRAConstants.KEY_PERCENT_COMPLETE, "PERCENT_COMPLETE_CHOICE",
                        JIRAConstants.PERCENT_COMPLETE_DONE_STORY_POINTS, "", true) {

                    @Override
                    public List<String> getDependencies() {
                        return new ArrayList<String>();
                    }

                    @Override
                    public List<Option> getDynamicalOptions(ValueSet values) {

                        List<Option> optionList = new ArrayList<>();

                        Option option1 = new Option(JIRAConstants.PERCENT_COMPLETE_WORK, "% Work Complete");
                        Option option2 = new Option(JIRAConstants.PERCENT_COMPLETE_DONE_STORY_POINTS, "% Story Points Done");

                        optionList.add(option1);
                        optionList.add(option2);

                        return optionList;
                    }

                },
                new LineBreaker(),

                new LabelText(JIRAConstants.LABEL_ISSUES_TO_IMPORT, "SELECT_ISSUES_TYPES_TO_IMPORT",
                        "Select issue types to import:", true),
                new CheckBox(JIRAConstants.JIRA_ISSUE_EPIC, "JIRA_ISSUE_EPIC", true),
                new CheckBox(JIRAConstants.JIRA_ISSUE_STORY, "JIRA_ISSUE_STORY", true),
                new CheckBox(JIRAConstants.JIRA_ISSUE_TASK, "JIRA_ISSUE_TASK", false),
                new CheckBox(JIRAConstants.JIRA_ISSUE_FEATURE, "JIRA_ISSUE_FEATURE", false),
                new CheckBox(JIRAConstants.JIRA_ISSUE_BUG, "JIRA_ISSUE_BUG", false),

                new LineBreaker(),

                new LabelText(JIRAConstants.LABEL_TASKS_OPTIONS, "TASKS_OPTIONS",
                        "Tasks Options:", true),
                new CheckBox(JIRAConstants.OPTION_INCLUDE_ISSUES_NO_GROUP, "OPTION_INCLUDE_ISSUE_NO_GROUP", true),
                new CheckBox(JIRAConstants.OPTION_ADD_ROOT_TASK, "OPTION_ADD_ROOT_TASK", true)


        });

        return fields;
    }



    @Override
    /**
     * This method is in Charge of retrieving all Jira Objects and turning them into a workplan structure to be imported in PPM.
     */
    public ExternalWorkPlan getExternalWorkPlan(WorkPlanIntegrationContext context, ValueSet values) {
        String projectKey = values.get(JIRAConstants.KEY_JIRA_PROJECT);
        String importSelection = values.get(JIRAConstants.KEY_IMPORT_SELECTION);
        String importSelectionDetails = values.get(JIRAConstants.KEY_IMPORT_SELECTION_DETAILS);
        String grouping = values.get(JIRAConstants.KEY_IMPORT_GROUPS);
        final String percentCompleteType = values.get(JIRAConstants.KEY_PERCENT_COMPLETE);
        final boolean addRootTask = values.getBoolean(JIRAConstants.OPTION_ADD_ROOT_TASK, false);
        boolean includeIssuesWithNoGroup = values.getBoolean(JIRAConstants.OPTION_INCLUDE_ISSUES_NO_GROUP, false);

        final TasksCreationContext taskContext = new TasksCreationContext();
        taskContext.workplanIntegrationContext = context;
        taskContext.percentCompleteType = percentCompleteType;
        taskContext.userProvider = service.getUserProvider();
        taskContext.configValues = values;

        // Let's get the sprints info for that project
        List<JIRASprint> sprints = service.get(values).getAllSprints(projectKey);
        final Map <String, JIRASprint> sprintsById = new LinkedHashMap<String, JIRASprint>();
        for (JIRASprint sprint : sprints) {
            sprintsById.put(sprint.getKey(), sprint);
        }
        taskContext.sprints = sprintsById;


        Set<String> issueTypes = new HashSet<>();
        if (values.getBoolean(JIRAConstants.JIRA_ISSUE_TASK, false)) {
            issueTypes.add(JIRAConstants.JIRA_ISSUE_TASK);
        }
        if (values.getBoolean(JIRAConstants.JIRA_ISSUE_STORY, false)) {
            issueTypes.add(JIRAConstants.JIRA_ISSUE_STORY);
        }
        if (values.getBoolean(JIRAConstants.JIRA_ISSUE_BUG, false)) {
            issueTypes.add(JIRAConstants.JIRA_ISSUE_BUG);
        }
        if (values.getBoolean(JIRAConstants.JIRA_ISSUE_EPIC, false)) {
            issueTypes.add(JIRAConstants.JIRA_ISSUE_EPIC);
        }
        if (values.getBoolean(JIRAConstants.JIRA_ISSUE_FEATURE, false)) {
            issueTypes.add(JIRAConstants.JIRA_ISSUE_FEATURE);
        }

        // We always want to retrieve epics if grouping tasks by Epics
        if (JIRAConstants.GROUP_EPIC.equalsIgnoreCase(grouping)) {
            issueTypes.add(JIRAConstants.JIRA_ISSUE_EPIC);
        }

        List<JIRASubTaskableIssue> issues = new ArrayList<JIRASubTaskableIssue>();

        switch(importSelection) {

            case JIRAConstants.IMPORT_ONE_BOARD:
                String boardId = importSelectionDetails;
                issues = service.get(values).getBoardIssues(projectKey, issueTypes, boardId);
                break;
            case JIRAConstants.IMPORT_ONE_EPIC:
                String epicKey = importSelectionDetails;
                issues = service.get(values).getEpicIssues(projectKey, issueTypes, epicKey);
                break;
            case JIRAConstants.IMPORT_ONE_VERSION:
                String versionId = importSelectionDetails;
                issues = service.get(values).getVersionIssues(projectKey, issueTypes, versionId);
                break;
            case JIRAConstants.IMPORT_ALL_PROJECT_ISSUES:
                issues = service.get(values).getAllIssues(projectKey, issueTypes);
                break;
        }

        final List<ExternalTask> rootTasks = new ArrayList<ExternalTask>();

        switch(grouping) {
            case JIRAConstants.GROUP_EPIC:
                // Root level is epics
                final List<JIRASubTaskableIssue> noEpicIssues = new ArrayList<JIRASubTaskableIssue>();

                for (JIRASubTaskableIssue issue : issues) {
                    if (JIRAConstants.JIRA_ISSUE_EPIC.equalsIgnoreCase(issue.getType())) {
                        // Epic, add it to the root tasks
                        final JIRAEpic epic = (JIRAEpic)issue;

                        rootTasks.add(WorkDrivenPercentCompleteExternalTask.forSummaryTask(new ExternalTask() {
                            @Override public String getName() {
                                return epic.getFullTaskName();
                            }

                            @Override public List<ExternalTask> getChildren() {

                                List<ExternalTask> children = new ArrayList<ExternalTask>();

                                if (epic.hasWork()) {
                                    // Epics that will be stored as summary tasks will not have their work taken into account, so we need to create a dummy leaf task for it.
                                    final WorkDrivenPercentCompleteExternalTask epicWorkTask = convertJiraIssueToExternalTask(epic, taskContext);
                                    epicWorkTask.setNameOverride("[Work]"+epicWorkTask.getName());
                                    children.add(epicWorkTask);
                                }

                                children.addAll(issuesToLeafTasks(epic.getContents(), taskContext));

                                return children;
                            }

                            @Override public Date getScheduledStart() {
                                return epic.getContents().isEmpty() ? super.getScheduledStart() : getEarliestScheduledStart(getChildren());
                            }

                            @Override public Date getScheduledFinish() {
                                return epic.getContents().isEmpty() ? super.getScheduledFinish() : getLastestScheduledFinish(getChildren());
                            }

                            @Override public Map<Integer, UserData> getUserDataFields() {


                                Map<Integer, ExternalTask.UserData> userData = new HashMap<Integer, ExternalTask.UserData>();

                                // Epics can have normal story points, like any leaf task
                                String storyPointsUserDataIndex = taskContext.configValues.get(JIRAConstants.SELECT_USER_DATA_STORY_POINTS);

                                if (!StringUtils.isBlank(storyPointsUserDataIndex) && !"0".equals(storyPointsUserDataIndex)) {
                                    if (epic.getStoryPoints() != null) {
                                        userData.put(Integer.parseInt(storyPointsUserDataIndex), new ExternalTask.UserData(epic.getStoryPoints().toString(), epic.getStoryPoints().toString()));
                                    }
                                }

                                // Epics can also have aggregated Story points.
                                String aggregatedStoryPointsUserDataIndex = taskContext.configValues.get(JIRAConstants.SELECT_USER_DATA_AGGREGATED_STORY_POINTS);

                                if (!StringUtils.isBlank(aggregatedStoryPointsUserDataIndex) && !"0".equals(aggregatedStoryPointsUserDataIndex)) {
                                        String aggSP = String.valueOf(epic.getAggregatedStoryPoints());
                                        userData.put(Integer.parseInt(aggregatedStoryPointsUserDataIndex), new ExternalTask.UserData(aggSP, aggSP));
                                }

                                return userData;
                            }

                        }));
                    } else {
                        // Not an epic. Is it part of an Epic or not?
                        if (isBlank(issue.getEpicKey())) {
                            noEpicIssues.add(issue);
                        }
                    }
                }

                if (includeIssuesWithNoGroup && !noEpicIssues.isEmpty()) {
                    rootTasks.add(WorkDrivenPercentCompleteExternalTask.forSummaryTask(new ExternalTask() {
                        @Override public String getName() {
                            return "No Epic Defined";
                        }

                        @Override public List<ExternalTask> getChildren() {
                            return issuesToLeafTasks(noEpicIssues, taskContext);
                        }
                    }));
                }

                break;

            case JIRAConstants.GROUP_STATUS:
                final Map<String, List<JIRASubTaskableIssue>> issuesByStatus = new LinkedHashMap<String, List<JIRASubTaskableIssue>>();

                final List<JIRASubTaskableIssue> noStatusIssues = new ArrayList<JIRASubTaskableIssue>();

                for (JIRASubTaskableIssue issue : issues) {

                    if (isBlank(issue.getStatus())) {
                        noStatusIssues.add(issue);
                        continue;
                    }

                    List<JIRASubTaskableIssue> statusIssues = issuesByStatus.get(issue.getStatus());
                    if (statusIssues == null) {
                        statusIssues = new ArrayList<JIRASubTaskableIssue>();
                        issuesByStatus.put(issue.getStatus(), statusIssues);
                    }
                    statusIssues.add(issue);
                }

                for (final String status : issuesByStatus.keySet()) {
                    rootTasks.add(WorkDrivenPercentCompleteExternalTask.forSummaryTask(new ExternalTask() {
                        @Override public String getName() {
                            return status;
                        }

                        @Override public List<ExternalTask> getChildren() {
                            return issuesToLeafTasks(issuesByStatus.get(status), taskContext);
                        }

                        @Override public Date getScheduledStart() {
                            return getEarliestScheduledStart(getChildren());
                        }

                        @Override public Date getScheduledFinish() {
                            return getLastestScheduledFinish(getChildren());
                        }
                    }));
                }

                if (includeIssuesWithNoGroup && !noStatusIssues.isEmpty()) {
                    rootTasks.add(WorkDrivenPercentCompleteExternalTask.forSummaryTask(new ExternalTask() {
                        @Override public String getName() {
                            return "No Status Defined";
                        }

                        @Override public List<ExternalTask> getChildren() {
                            return issuesToLeafTasks(noStatusIssues, taskContext);
                        }

                        @Override public Date getScheduledStart() {
                            return getEarliestScheduledStart(getChildren());
                        }

                        @Override public Date getScheduledFinish() {
                            return getLastestScheduledFinish(getChildren());
                        }
                    }));
                }

                break;
            case JIRAConstants.GROUP_SPRINT:

                // Now let's group issues by sprints
                final Map<String, List<JIRASubTaskableIssue>> issuesBySprintId = new LinkedHashMap<String, List<JIRASubTaskableIssue>>();

                // We initialize the Map contents to get the sprints in the right order
                for (String sprintId : sprintsById.keySet()) {
                    issuesBySprintId.put(sprintId, new ArrayList<JIRASubTaskableIssue>());
                }

                final List<JIRASubTaskableIssue> backlogIssues = new ArrayList<JIRASubTaskableIssue>();

                for (JIRASubTaskableIssue issue : issues) {

                    if (isBlank(issue.getSprintId())) {
                        backlogIssues.add(issue);
                        continue;
                    }

                    List<JIRASubTaskableIssue> sprintIssues = issuesBySprintId.get(issue.getSprintId());
                    if (sprintIssues == null) {
                        sprintIssues = new ArrayList<JIRASubTaskableIssue>();
                        issuesBySprintId.put(issue.getSprintId(), sprintIssues);
                    }
                    sprintIssues.add(issue);
                }

                for (final String sprintId : issuesBySprintId.keySet()) {

                    final JIRASprint sprint = sprintsById.get(sprintId);
                    rootTasks.add(WorkDrivenPercentCompleteExternalTask.forSummaryTask(new ExternalTask() {
                        @Override public String getName() {
                            return sprint == null ? "Sprint "+sprintId : sprint.getName();
                        }

                        @Override public List<ExternalTask> getChildren() {
                            return issuesToLeafTasks(issuesBySprintId.get(sprintId), taskContext);
                        }

                        @Override public Date getScheduledStart() {
                            return (sprint == null || sprint.getStartDateAsDate() == null) ? getEarliestScheduledStart(getChildren()) : sprint.getStartDateAsDate();
                        }

                        @Override public Date getScheduledFinish() {
                            return (sprint == null || sprint.getEndDateAsDate() == null) ? getLastestScheduledFinish(getChildren()) : sprint.getEndDateAsDate();
                        }
                    }));
                }

                if (includeIssuesWithNoGroup && !backlogIssues.isEmpty()) {
                    rootTasks.add(WorkDrivenPercentCompleteExternalTask.forSummaryTask(new ExternalTask() {
                        @Override public String getName() {
                            return "Backlog";
                        }

                        @Override public List<ExternalTask> getChildren() {
                            return issuesToLeafTasks(backlogIssues, taskContext);
                        }

                        @Override public Date getScheduledStart() {
                            return getEarliestScheduledStart(getChildren());
                        }

                        @Override public Date getScheduledFinish() {
                            return getLastestScheduledFinish(getChildren());
                        }
                    }));
                }

                break;
        }

        return new ExternalWorkPlan() {
            @Override public List<ExternalTask> getRootTasks() {
                if (addRootTask) {

                    if (rootTasks.size() <= 1) {
                        // If we already have only one root task (or no task), we don't need to add one more.
                        return rootTasks;
                    }

                    ExternalTask rootTask = WorkDrivenPercentCompleteExternalTask.forSummaryTask(new ExternalTask() {
                        @Override public List<ExternalTask> getChildren() {
                            return rootTasks;
                        }

                        @Override public String getName() {
                            return "Root Task"; // We'll need something fancier...
                        }

                        @Override public Date getScheduledStart() {
                            return getEarliestScheduledStart(getChildren());
                        }

                        @Override public Date getScheduledFinish() {
                            return getLastestScheduledFinish(getChildren());
                        }
                    });
                    return Arrays.asList(new ExternalTask[] {rootTask});
                } else {
                    return rootTasks;
                }
            }
        };
    }

    private Date getLastestScheduledFinish(List<ExternalTask> children) {
        Date date = null;
        for (ExternalTask child : children) {
            if (child.getScheduledFinish() == null) {
                continue;
            }
            if (date == null || child.getScheduledFinish().after(date)) {
                date = child.getScheduledFinish();
            }
        }

        return date != null ? date : getDefaultFinishDate();
    }

    private Date getEarliestScheduledStart(List<ExternalTask> children) {
        Date date = null;
        for (ExternalTask child : children) {
            if (child.getScheduledStart() == null) {
                continue;
            }
            if (date == null || child.getScheduledStart().before(date)) {
                date = child.getScheduledStart();
            }
        }

        return date != null ? date : getDefaultStartDate();
    }

    /**
     * Returns true if str is null, "", some spaces, or the string "null".
     * @param str
     * @return
     */
    private boolean isBlank(String str) {
        return StringUtils.isBlank(str) || "null".equalsIgnoreCase(str);
    }

    /**
     * This method declares List<ExternalTask>, but we actually return a List<WorkDrivenPercentCompleteExternalTask>.
     */
    private List<ExternalTask> issuesToLeafTasks(List<JIRASubTaskableIssue> issues, TasksCreationContext context) {
        List<ExternalTask> externalTasks = new ArrayList<ExternalTask>(issues.size());

        for (JIRASubTaskableIssue issue : issues) {
            externalTasks.add(convertJiraIssueToExternalTask(issue, context));
        }

        return externalTasks;
    }

    private WorkDrivenPercentCompleteExternalTask convertJiraIssueToExternalTask(final JIRASubTaskableIssue issue, final TasksCreationContext context)
    {
        // First, let's compute the work of that task
        Double doneWork = null;
        Double remainingWork = null;

        switch(context.percentCompleteType) {
            case JIRAConstants.PERCENT_COMPLETE_WORK:
                if (issue.getWork() != null) {
                    doneWork = issue.getWork().getTimeSpentHours();
                    remainingWork = issue.getWork().getRemainingEstimateHours();
                }
                break;
            case JIRAConstants.PERCENT_COMPLETE_DONE_STORY_POINTS:
                // In story points mode, a task is either 0% or 100% complete.
                if (issue.isDone()) {
                    doneWork = (issue.getStoryPoints() == null ? 0d : issue.getStoryPoints().doubleValue());
                    remainingWork = 0d;
                } else {
                    doneWork = 0d;
                    remainingWork = (issue.getStoryPoints() == null ? 0d : issue.getStoryPoints().doubleValue());
                }
                break;
        }


        ExternalTask task = new ExternalTask() {
            @Override public String getId() {
                return issue.getKey();
            }

            @Override public TaskStatus getStatus() {
                return issue.getExternalTaskStatus();
            }

            @Override public String getName() {
                // We prefix the task name with the issue type
                return issue.getFullTaskName();
            }

            @Override public Date getScheduledStart() {


                Date start = getDefaultStartDate();

                JIRASprint sprint = context.sprints.get(issue.getSprintId());
                if (sprint != null && sprint.getStartDateAsDate() != null) {
                    start = sprint.getStartDateAsDate();
                }

                return start;
            }

            @Override public Date getScheduledFinish() {

                Date finish = getDefaultFinishDate();

                JIRASprint sprint = context.sprints.get(issue.getSprintId());
                if (sprint != null && sprint.getEndDateAsDate() != null) {
                    finish = sprint.getEndDateAsDate();
                }

                return finish;
            }

            @Override public List<ExternalTaskActuals> getActuals() {
                return getActualsFromWork(issue, context, getScheduledStart());
            }

            @Override public long getOwnerId() {
                return issue.getAssigneePpmUserId();
            }

            @Override public List<ExternalTask> getChildren() {
                // We are always generating leaf tasks with this method.
                return new ArrayList<ExternalTask>();
            }

            @Override public Map<Integer, UserData> getUserDataFields() {
                Map<Integer, ExternalTask.UserData> userData = new HashMap<Integer, ExternalTask.UserData>();

                String storyPointsUserDataIndex = context.configValues.get(JIRAConstants.SELECT_USER_DATA_STORY_POINTS);

                if (!StringUtils.isBlank(storyPointsUserDataIndex) && !"0".equals(storyPointsUserDataIndex)) {
                    if (issue.getStoryPoints() != null) {
                        userData.put(Integer.parseInt(storyPointsUserDataIndex), new ExternalTask.UserData(issue.getStoryPoints().toString(), issue.getStoryPoints().toString()));
                    }
                }

                return userData;
            }
        };

        return WorkDrivenPercentCompleteExternalTask.forLeafTask(task, doneWork == null ? 0d : doneWork, remainingWork == null ? 0d : remainingWork);
    }

    private Date getDefaultStartDate() {
        Calendar todayMorning = new GregorianCalendar();
        todayMorning.set(Calendar.HOUR, 1);
        todayMorning.set(Calendar.MINUTE, 0);
        todayMorning.set(Calendar.SECOND, 0);
        todayMorning.set(Calendar.MILLISECOND, 0);
        return todayMorning.getTime();

    }

    private Date getDefaultFinishDate() {
        Calendar todayEvening = new GregorianCalendar();
        todayEvening.set(Calendar.HOUR, 23);
        todayEvening.set(Calendar.MINUTE, 0);
        todayEvening.set(Calendar.SECOND, 0);
        todayEvening.set(Calendar.MILLISECOND, 0);
        return todayEvening.getTime();

    }



    private List<ExternalTaskActuals> getActualsFromWork(final JIRAIssue issue, TasksCreationContext context,
            final Date scheduledStart) {

        List<ExternalTaskActuals> actuals = new ArrayList<ExternalTaskActuals>();

        if (ExternalTask.TaskStatus.IN_PROGRESS.equals(issue.getExternalTaskStatus())) {
            // Leaf Tasks that are IN_PROGRESS must always have Actual Start defined on unassigned effort
            actuals.add(new ExternalTaskActuals() {

                @Override public Date getActualStart() {
                    return scheduledStart;
                }

                @Override public double getPercentComplete() {
                    return 0;
                }

                @Override public long getResourceId() {
                    return -1;
                }

                @Override public double getScheduledEffort() {
                    return 0;
                }

                @Override public double getActualEffort() {
                    return 0;
                }

                @Override public Double getEstimatedRemainingEffort() {
                    return 0d;
                }
            });
        }

        if (issue.getWork() != null) {
            for (JIRAIssueWork.JIRAWorklogEntry worklog : issue.getWork().getWorklogs()) {
                if (worklog != null) {
                    actuals.add(convertWorklogEntryToActuals(worklog, context, issue.isDone()));
                }
            }

            // Remaining Effort has a dedicated Actuals assigned to whoever the issue is assigned to.
            if (issue.getWork().getRemainingEstimateHours() != null && issue.getWork().getRemainingEstimateHours() > 0) {
                actuals.add(getRemainingEffortActuals(issue.getWork().getRemainingEstimateHours(), issue.getAssigneePpmUserId()));
            }

            // If the issue is assigned to someone, we need that person to always appear in the actuals even if they did no work to ensure
            // They appear in the list of people assigned to the task.
            if (issue.getAssigneePpmUserId() > 0) {
                actuals.add(new ExternalTaskActuals() {
                    @Override public double getPercentComplete() {
                        return 0;
                    }

                    @Override public long getResourceId() {
                        return issue.getAssigneePpmUserId();
                    }

                    @Override public double getScheduledEffort() {
                        return 0;
                    }

                    @Override public double getActualEffort() {
                        return 0;
                    }

                    @Override public Double getEstimatedRemainingEffort() {
                        return 0d;
                    }
                });
            }
        }

        if (issue instanceof JIRASubTaskableIssue) {
            List<JIRASubTask> subTasks = ((JIRASubTaskableIssue)issue).getSubTasks();

            if (subTasks != null) {
                for (JIRASubTask subTask : subTasks) {
                    actuals.addAll(getActualsFromWork(subTask, context, scheduledStart));
                }
            }
        }

        return actuals;

    }

    private ExternalTaskActuals getRemainingEffortActuals(final double remainingEstimateHours, final long assigneePpmUserId) {
        return new ExternalTaskActuals() {
            @Override public double getScheduledEffort() {
                return remainingEstimateHours;
            }

            @Override public Double getEstimatedRemainingEffort() {
                return remainingEstimateHours;
            }

            @Override public double getActualEffort() {
                return 0d;
            }

            @Override public double getPercentComplete() {
                return 0d;
            }

            @Override public long getResourceId() {
                return assigneePpmUserId;
            }
        };
    }

    private ExternalTaskActuals convertWorklogEntryToActuals(final JIRAIssueWork.JIRAWorklogEntry worklog,
            final TasksCreationContext context, final boolean isTaskDone)
    {
        return new ExternalTaskActuals() {

            @Override public double getScheduledEffort() {
                return worklog.getTimeSpentHours();
            }

            @Override public Date getActualStart() {
                return worklog.getDateStartedAsDate();
            }

            @Override public Double getEstimatedRemainingEffort() {
                return 0d;
            }

            @Override public Date getActualFinish() {
                return isTaskDone ? worklog.getDateStartedAsDate() : null;
            }

            @Override public double getActualEffort() {
                return worklog.getTimeSpentHours();
            }

            @Override public double getPercentComplete() {
                // Each actual line from work log will be considered to be completed only if the task is done.
                return isTaskDone ? 100d : 0d;
            }

            @Override public long getResourceId() {
                User user = context.userProvider.getByEmail(worklog.getAuthorEmail());
                return user == null ? -1 : user.getUserId();
            }

            @Override public Date getEstimatedFinishDate() {
                return worklog.getDateStartedAsDate();
            }
        };
    }

    /**
     * This will allow to have the information in PPM DB table PPMIC_WORKPLAN_MAPPINGS of what entity in JIRA is effectively linked to the PPM work plan task.
     * It is very useful for reporting purpose.
     * @since 9.42
     */
    public LinkedTaskAgileEntityInfo getAgileEntityInfoFromMappingConfiguration(ValueSet values) {
        LinkedTaskAgileEntityInfo info = new LinkedTaskAgileEntityInfo();

        String importSelect = values.get(JIRAConstants.KEY_IMPORT_SELECTION);

        info.setProjectId(values.get(JIRAConstants.KEY_JIRA_PROJECT));

        if (importSelect == null) {
            return info;
        }

        switch (importSelect) {
            case JIRAConstants.IMPORT_ALL_PROJECT_ISSUES:
                return info;
            case JIRAConstants.IMPORT_ONE_BOARD:
                // Technically it is NOT the sprint id but the Board ID - warning.
                info.setSprintId(values.get(JIRAConstants.KEY_IMPORT_SELECTION_DETAILS));
                return info;
            case JIRAConstants.IMPORT_ONE_EPIC:
                // Technically it is NOT the sprint id but the Board ID - warning.
                info.setEpicId(values.get(JIRAConstants.KEY_IMPORT_SELECTION_DETAILS));
                return info;
            case JIRAConstants.IMPORT_ONE_VERSION:
                // Technically it is NOT the sprint id but the Board ID - warning.
                info.setReleaseId(values.get(JIRAConstants.KEY_IMPORT_SELECTION_DETAILS));
                return info;
            default:
                return info;
        }
    }

    private class TasksCreationContext {

        public String percentCompleteType;

        public Map<String, JIRASprint> sprints;

        public WorkPlanIntegrationContext workplanIntegrationContext;

        public UserProvider userProvider;

        public ValueSet configValues;
    }
}

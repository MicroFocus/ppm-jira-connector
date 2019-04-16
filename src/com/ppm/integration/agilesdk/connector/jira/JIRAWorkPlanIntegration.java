
package com.ppm.integration.agilesdk.connector.jira;

import com.hp.ppm.integration.model.WorkplanMapping;
import com.hp.ppm.user.model.User;
import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.jira.model.*;
import com.ppm.integration.agilesdk.connector.jira.rest.util.exception.JIRAConnectivityExceptionHandler;
import com.ppm.integration.agilesdk.connector.jira.rest.util.exception.RestRequestException;
import com.ppm.integration.agilesdk.connector.jira.service.JIRAService;
import com.ppm.integration.agilesdk.connector.jira.util.WorkDrivenPercentCompleteExternalTask;
import com.ppm.integration.agilesdk.pm.*;
import com.ppm.integration.agilesdk.provider.LocalizationProvider;
import com.ppm.integration.agilesdk.provider.Providers;
import com.ppm.integration.agilesdk.provider.UserProvider;
import com.ppm.integration.agilesdk.ui.*;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wink.client.ClientRuntimeException;

import java.util.*;

public class JIRAWorkPlanIntegration extends WorkPlanIntegration {
    private final Logger logger = Logger.getLogger(this.getClass());

    public JIRAWorkPlanIntegration() {}

    @Override
    public List<Field> getMappingConfigurationFields(WorkPlanIntegrationContext context, ValueSet values) {

        final LocalizationProvider lp = Providers.getLocalizationProvider(JIRAIntegrationConnector.class);

        final boolean useAdminPassword = values.getBoolean(JIRAConstants.KEY_USE_ADMIN_PASSWORD_TO_MAP_TASKS, false);

        List<Field> fields = new ArrayList<Field>();

        final JIRAService service = JIRAServiceProvider.get(values).useAdminAccount();

        final Map<String, Set<String>> issueTypesPerProjectKey = service.getIssueTypesPerProject();

        if (!useAdminPassword) {
            fields.add(new PlainText(JIRAConstants.KEY_USERNAME, "USERNAME", "", true));
            fields.add(new PasswordText(JIRAConstants.KEY_PASSWORD, "PASSWORD", "", true));
            fields.add(new LineBreaker());
        }

        SelectList importSelectionSelectList = new SelectList(JIRAConstants.KEY_IMPORT_SELECTION,"IMPORT_SELECTION",JIRAConstants.IMPORT_ALL_PROJECT_ISSUES,true)
                .addLevel(JIRAConstants.KEY_IMPORT_SELECTION, "IMPORT_SELECTION")
                .addOption(new SelectList.Option(JIRAConstants.IMPORT_ALL_PROJECT_ISSUES,"IMPORT_ALL_PROJECT_ISSUES"))
                .addOption(new SelectList.Option(JIRAConstants.IMPORT_ONE_EPIC,"IMPORT_ONE_EPIC"))
                .addOption(new SelectList.Option(JIRAConstants.IMPORT_ONE_VERSION,"IMPORT_ONE_VERSION"))
                .addOption(new SelectList.Option(JIRAConstants.IMPORT_ONE_BOARD,"IMPORT_ONE_BOARD"));

        if (service.isJiraPortfolioEnabled()) {
            for (HierarchyLevelInfo portfolioLevel : service.getJiraPortfolioLevelsInfo()) {
                if (portfolioLevel.getId() <= 2) {
                    // We don't want to display Sub-Task, Story or Epic.
                    continue;
                }
                importSelectionSelectList.addOption(
                        new SelectList.Option(JIRAConstants.IMPORT_PORTFOLIO_PREFIX+portfolioLevel.getId(), portfolioLevel.getTitle()));
            }
        }


        SelectList importGroupSelectList = new SelectList(JIRAConstants.KEY_IMPORT_GROUPS,"IMPORT_GROUPS",JIRAConstants.GROUP_EPIC,true)
                .addLevel(JIRAConstants.KEY_IMPORT_GROUPS, "IMPORT_GROUPS")
                .addOption(new SelectList.Option(JIRAConstants.GROUP_EPIC,"GROUP_EPIC"))
                .addOption(new SelectList.Option(JIRAConstants.GROUP_SPRINT,"GROUP_SPRINT"))
                .addOption(new SelectList.Option(JIRAConstants.GROUP_STATUS,"GROUP_STATUS"));

        if (service.isJiraPortfolioEnabled()) {
            importGroupSelectList.addOption(new SelectList.Option(JIRAConstants.GROUP_JIRA_PORTFOLIO_HIERARCHY,"GROUP_JIRA_PORTFOLIO_HIERARCHY"));
        }

        fields.addAll(Arrays.asList(new Field[] {

                new LabelText(JIRAConstants.LABEL_SELECT_WHAT_TO_IMPORT, "LABEL_SELECT_WHAT_TO_IMPORT",
                        "Select what to import:", true),

                new DynamicDropdown(JIRAConstants.KEY_JIRA_PROJECT, "JIRA_PROJECT", true) {

                    @Override
                    public List<String> getDependencies() {
                        return Arrays.asList(new String[] {JIRAConstants.KEY_USERNAME, JIRAConstants.KEY_PASSWORD});
                    }

                    @Override
                    public List<Option> getDynamicalOptions(ValueSet values) {

                        if (!useAdminPassword) {
                            service.resetUserCredentials(values).useNonAdminAccount();
                        }

                        List<JIRAProject> list = new ArrayList<>();
                        try {
                            list = service.getProjects();
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

                importSelectionSelectList,

                new DynamicDropdown(JIRAConstants.KEY_IMPORT_SELECTION_DETAILS, "IMPORT_SELECTION_DETAILS", "", true) {

                    @Override
                    public List<String> getDependencies() {

                        return Arrays.asList(
                                new String[] {JIRAConstants.KEY_JIRA_PROJECT, JIRAConstants.KEY_IMPORT_SELECTION});
                    }

                    @Override
                    public List<Option> getDynamicalOptions(ValueSet values) {

                        if (!useAdminPassword) {
                            service.resetUserCredentials(values).useNonAdminAccount();
                        }

                        String importSelection = values.get(JIRAConstants.KEY_IMPORT_SELECTION);
                        String projectKey = values.get(JIRAConstants.KEY_JIRA_PROJECT);
                        List<Option> options = new ArrayList<>();
                        switch (importSelection) {
                            case JIRAConstants.IMPORT_ALL_PROJECT_ISSUES:
                                // No extra option when importing everything, it will be ignored anyway.
                                options.add(new Option("0", lp.getConnectorText("IMPORT_ALL_PROJECT_ISSUES")));
                                break;
                            case JIRAConstants.IMPORT_ONE_EPIC:
                                List<JIRASubTaskableIssue> epics = service.getProjectIssuesList(projectKey, JIRAConstants.JIRA_ISSUE_EPIC);
                                for (JIRAIssue epic : epics) {
                                    Option option = new Option(epic.getKey(), epic.getName());
                                    options.add(option);
                                }
                                break;
                            case JIRAConstants.IMPORT_ONE_BOARD:
                                List<JIRABoard> boards = service.getAllBoards(projectKey);
                                for (JIRABoard board : boards) {
                                    Option option = new Option(board.getKey(), board.getName());
                                    options.add(option);

                                }
                                break;
                            case JIRAConstants.IMPORT_ONE_VERSION:
                                List<JIRAVersion> versions = service.getVersions(projectKey);
                                for (JIRAVersion version : versions) {
                                    Option option = new Option(version.getId(), version.getName());
                                    options.add(option);

                                }
                                break;
                            default:
                                // Jira Portfolio Type then?
                                if (importSelection.startsWith(JIRAConstants.IMPORT_PORTFOLIO_PREFIX)) {
                                    long levelId = Long.parseLong(importSelection.substring(JIRAConstants.IMPORT_PORTFOLIO_PREFIX.length()));

                                    List<JIRASubTaskableIssue> portfolioIssues = new ArrayList<>();

                                    for (HierarchyLevelInfo portfolioLevel : service.getJiraPortfolioLevelsInfo()) {
                                        if (portfolioLevel.getId() == levelId) {
                                            portfolioIssues = service.getProjectIssuesList(projectKey, portfolioLevel.getIssueTypeIds());
                                            break;
                                        }
                                    }

                                    for (JIRAIssue portfolioIssue : portfolioIssues) {
                                        Option option = new Option(portfolioIssue.getKey(), portfolioIssue.getName());
                                        options.add(option);
                                    }

                                }
                        }
                        return options;
                    }

                },
                new LineBreaker(),

                importGroupSelectList,

                new LineBreaker(),

                new LabelText(JIRAConstants.LABEL_ISSUES_TO_IMPORT, "SELECT_ISSUES_TYPES_TO_IMPORT",
                        "Select issue types to import:", true)}));

        // List of issue types checkboxes.
        Set<String> allIssueTypes = new HashSet<String>();
        for (Set<String> issueTypes : issueTypesPerProjectKey.values()) {
            allIssueTypes.addAll(issueTypes);
        }


        List<String> sortedIssueTypes = new ArrayList<String>(allIssueTypes);

        Collections.sort(sortedIssueTypes, new Comparator<String>() {
                    @Override public int compare(String o1, String o2) {
                        return o1.compareToIgnoreCase(o2);
                    }
                });

        for (final String issueType : sortedIssueTypes) {
            fields.add( new CheckBox(getParamNameFromIssueTypeName(issueType), issueType, "Epic".equalsIgnoreCase(issueType) || "Story".equalsIgnoreCase(issueType)) {

                @Override public List<String> getStyleDependencies() {
                    return Arrays.asList(new String[] {JIRAConstants.KEY_JIRA_PROJECT, JIRAConstants.KEY_IMPORT_SELECTION});
                }

                @Override public FieldAppearance getFieldAppearance(ValueSet values) {
                    String projectKey = values.get(JIRAConstants.KEY_JIRA_PROJECT);
                    String importSelection = values.get(JIRAConstants.KEY_IMPORT_SELECTION);

                    if ((importSelection != null && importSelection.startsWith(JIRAConstants.IMPORT_PORTFOLIO_PREFIX)) || (issueTypesPerProjectKey.get(projectKey) != null && issueTypesPerProjectKey.get(projectKey).contains(issueType))) {
                        // This issue type is enabled for this project or if picking a Jira Portfolio entity as their contents are cross projects.
                        return new FieldAppearance("", "disabled");
                    } else {
                        // This issue type is disabled for this project
                        return new FieldAppearance("disabled", "");
                    }
                }
            });
        }

                // Last options
                fields.addAll(Arrays.asList(new Field[] {new LineBreaker(),

                        new LineBreaker(),

                        new LabelText(JIRAConstants.LABEL_PROGRESS_AND_ACTUALS, "LABEL_PROGRESS_AND_ACTUALS",
                                "Issues Progress and Actuals", true),
                        new SelectList(JIRAConstants.KEY_PERCENT_COMPLETE,"PERCENT_COMPLETE_CHOICE",JIRAConstants.PERCENT_COMPLETE_DONE_STORY_POINTS,true)
                                .addLevel(JIRAConstants.KEY_PERCENT_COMPLETE, "PERCENT_COMPLETE_CHOICE")
                                .addOption(new SelectList.Option(JIRAConstants.PERCENT_COMPLETE_DONE_STORY_POINTS,"PERCENT_COMPLETE_DONE_STORY_POINTS"))
                                .addOption(new SelectList.Option(JIRAConstants.PERCENT_COMPLETE_WORK,"PERCENT_COMPLETE_WORK")),
                        new LineBreaker(),
                        new SelectList(JIRAConstants.KEY_ACTUALS,"ACTUALS_CHOICE",JIRAConstants.ACTUALS_LOGGED_WORK,true)
                                .addLevel(JIRAConstants.KEY_ACTUALS, "ACTUALS_CHOICE")
                                .addOption(new SelectList.Option(JIRAConstants.ACTUALS_LOGGED_WORK,"ACTUALS_LOGGED_WORK"))
                                .addOption(new SelectList.Option(JIRAConstants.ACTUALS_SP,"ACTUALS_SP"))
                                .addOption(new SelectList.Option(JIRAConstants.ACTUALS_NO_ACTUALS,"ACTUALS_NO_ACTUALS")),
                        new PlainText(JIRAConstants.ACTUALS_SP_RATIO, "ACTUALS_SP_RATIO", "8", false) {
                            @Override
                            public List<String> getStyleDependencies() {
                                return Arrays.asList(new String[]{JIRAConstants.KEY_ACTUALS});
                            }

                            @Override
                            public FieldAppearance getFieldAppearance(ValueSet values) {
                                String actualsChoice = values.get(JIRAConstants.KEY_ACTUALS);
                                if (JIRAConstants.ACTUALS_SP.equals(actualsChoice)) {
                                    return new FieldAppearance("", "disabled");
                                } else {
                                    return new FieldAppearance("disabled", "");
                                }
                            }
                        },


                        new LineBreaker(),

                        new LabelText(JIRAConstants.LABEL_TASKS_OPTIONS, "TASKS_OPTIONS", "Tasks Options:", true),
                        new CheckBox(JIRAConstants.OPTION_INCLUDE_ISSUES_NO_GROUP, "OPTION_INCLUDE_ISSUE_NO_GROUP", true),
                        new LineBreaker(), new CheckBox(JIRAConstants.OPTION_ADD_ROOT_TASK, "OPTION_ADD_ROOT_TASK", true),
                        new PlainText(JIRAConstants.OPTION_ROOT_TASK_NAME, "OPTION_ROOT_TASK_NAME",
                                Providers.getLocalizationProvider(JIRAIntegrationConnector.class).getConnectorText("WORKPLAN_ROOT_TASK_TASK_NAME"), false) {
                            @Override public List<String> getStyleDependencies() {
                                return Arrays.asList(new String[] {JIRAConstants.OPTION_ADD_ROOT_TASK});
                            }

                            @Override public FieldAppearance getFieldAppearance(ValueSet values) {
                                boolean isCreateRootTask = values.getBoolean(JIRAConstants.OPTION_ADD_ROOT_TASK, false);
                                if (isCreateRootTask) {
                                    return new FieldAppearance("", "disabled");
                                } else {
                                    return new FieldAppearance("disabled", "");
                                }
                            }
                        },

                        new LineBreaker(),
                        new SelectList(JIRAConstants.OPTION_ADD_EPIC_MILESTONES, "OPTION_ADD_EPIC_MILESTONES", "", true) {
                            @Override public List<String> getStyleDependencies() {
                                return Arrays.asList(new String[] {JIRAConstants.JIRA_ISSUE_EPIC});
                            }

                            @Override public FieldAppearance getFieldAppearance(ValueSet values) {
                                boolean importEpics = values.getBoolean(JIRAConstants.JIRA_ISSUE_EPIC, true);
                                if (importEpics) {
                                    return new FieldAppearance("", "disabled");
                                } else {
                                    return new FieldAppearance("disabled", "");
                                }
                            }
                        }.addLevel(JIRAConstants.OPTION_ADD_EPIC_MILESTONES, "OPTION_ADD_EPIC_MILESTONES")
                                .addOption(new SelectList.Option("", "OPTION_ADD_EPIC_MILESTONES_NO_MILESTONE"))
                                .addOption(new SelectList.Option("MINOR", "OPTION_ADD_EPIC_MILESTONES_MINOR")).addOption(
                                new SelectList.Option("MAJOR", "OPTION_ADD_EPIC_MILESTONES_MAJOR"))

                }));

        return fields;
    }

    private String getParamNameFromIssueTypeName(String issueTypeName) {
        if (issueTypeName == null) {
            issueTypeName = "";
        }
        return JIRAConstants.JIRA_ISSUE_TYPE_PREFIX + issueTypeName.replace(" ", "__");
    }

    private String getIssueTypeNameFromParamName(String paramName) {
        if (paramName == null) {
            return null;
        }
        return paramName.substring(JIRAConstants.JIRA_ISSUE_TYPE_PREFIX.length()).replace("__", " ");
    }


    @Override
    /**
     * This method is in Charge of retrieving all Jira Objects and turning them into a workplan structure to be imported in PPM.
     */
    public ExternalWorkPlan getExternalWorkPlan(WorkPlanIntegrationContext context, final ValueSet values) {
        String projectKey = values.get(JIRAConstants.KEY_JIRA_PROJECT);
        String importSelection = values.get(JIRAConstants.KEY_IMPORT_SELECTION);
        String importSelectionDetails = values.get(JIRAConstants.KEY_IMPORT_SELECTION_DETAILS);
        String grouping = values.get(JIRAConstants.KEY_IMPORT_GROUPS);
        String percentCompleteType = values.get(JIRAConstants.KEY_PERCENT_COMPLETE);
        String actualsType = values.get(JIRAConstants.KEY_ACTUALS);
        int hoursPerSp = 8;
        try {
            hoursPerSp = Integer.parseInt(values.get(JIRAConstants.ACTUALS_SP_RATIO));
        } catch (Exception e) {
            // We use default value of 8;
        }
        final boolean addRootTask = values.getBoolean(JIRAConstants.OPTION_ADD_ROOT_TASK, false);
        boolean includeIssuesWithNoGroup = values.getBoolean(JIRAConstants.OPTION_INCLUDE_ISSUES_NO_GROUP, false);

        // Following code handles backward compatibility with PPM 9.41 connector.
        if (JIRAConstants.KEY_ALL_EPICS.equals(values.get(JIRAConstants.KEY_IMPORT_SELECTION))) {
            // This key only existed in PPM 9.41 connector. It would import all epics, grouped by Epics.
            importSelection = JIRAConstants.IMPORT_ALL_PROJECT_ISSUES;
            grouping = JIRAConstants.GROUP_EPIC;
            includeIssuesWithNoGroup = false;
            percentCompleteType = JIRAConstants.PERCENT_COMPLETE_DONE_STORY_POINTS;
        }
        if (StringUtils.isBlank(grouping)) {
            grouping = JIRAConstants.GROUP_EPIC;
        }
        if (StringUtils.isBlank(percentCompleteType)) {
            percentCompleteType = JIRAConstants.PERCENT_COMPLETE_DONE_STORY_POINTS;
        }
        if (StringUtils.isBlank(importSelectionDetails)) {
            importSelection = JIRAConstants.IMPORT_ALL_PROJECT_ISSUES;
        }
        // End of backward compatibility code


        final TasksCreationContext taskContext = new TasksCreationContext();
        taskContext.workplanIntegrationContext = context;
        taskContext.percentCompleteType = percentCompleteType;
        taskContext.userProvider = JIRAServiceProvider.getUserProvider();
        taskContext.configValues = values;
        taskContext.actualsType = actualsType;
        taskContext.hoursPerSp = hoursPerSp;

        JIRAService service = JIRAServiceProvider.get(values).useAdminAccount();

        // Let's get the sprints info for that project
        List<JIRASprint> sprints = service.getAllSprints(projectKey);
        final Map <String, JIRASprint> sprintsById = new LinkedHashMap<String, JIRASprint>();
        for (JIRASprint sprint : sprints) {
            sprintsById.put(sprint.getKey(), sprint);
        }
        taskContext.sprints = sprintsById;


        Set<String> issueTypes = new HashSet<>();

        for (String key: values.keySet()) {
            if (key.startsWith(JIRAConstants.JIRA_ISSUE_TYPE_PREFIX)) {
                if (values.getBoolean(key, false)) {
                    issueTypes.add(getIssueTypeNameFromParamName(key));
                }
            }
        }

        // Following code handles backward compatibility with static issue types selection
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
        // End of backward compatibility code

        // We always want to retrieve epics if grouping tasks by Epics
        if (JIRAConstants.GROUP_EPIC.equalsIgnoreCase(grouping)) {
            issueTypes.add(JIRAConstants.JIRA_ISSUE_EPIC);
        }

        List<JIRASubTaskableIssue> issues = new ArrayList<JIRASubTaskableIssue>();

        final List<JIRAEpic> selectedEpics = new ArrayList<>();

        switch(importSelection) {

            case JIRAConstants.IMPORT_ONE_BOARD:
                String boardId = importSelectionDetails;
                issues = service.getBoardIssues(projectKey, issueTypes, boardId);
                break;
            case JIRAConstants.IMPORT_ONE_EPIC:
                String epicKey = importSelectionDetails;
                issues = service.getAllEpicIssues(projectKey, issueTypes, epicKey);
                break;
            case JIRAConstants.IMPORT_ONE_VERSION:
                String versionId = importSelectionDetails;
                issues = service.getVersionIssues(projectKey, issueTypes, versionId);
                break;
            case JIRAConstants.IMPORT_ALL_PROJECT_ISSUES:
                issues = service.getAllIssues(projectKey, issueTypes);
                break;
            default:
                // Jira Portfolio Type then?
                if (importSelection.startsWith(JIRAConstants.IMPORT_PORTFOLIO_PREFIX)) {
                    String portfolioIssueKey = importSelectionDetails;
                    issues = service.getPortfolioIssueDescendants(portfolioIssueKey, issueTypes);
                }
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

                        selectedEpics.add(epic);

                        rootTasks.add(WorkDrivenPercentCompleteExternalTask.forSummaryTask(new ExternalTask() {
                            @Override public String getName() {
                                return epic.getFullTaskName();
                            }
                            
                            @Override public String getId() {
                                return epic.getKey();
                            }

                            @Override public List<ExternalTask> getChildren() {

                                List<ExternalTask> children = new ArrayList<ExternalTask>();

                                if (epic.hasWork()) {
                                    // Epics that will be stored as summary tasks will not have their work taken into account, so we need to create a dummy leaf task for it.
                                    final WorkDrivenPercentCompleteExternalTask epicWorkTask = convertJiraIssueToLeafExternalTask(epic, taskContext);
                                    epicWorkTask.setNameOverride("[Work]"+epicWorkTask.getName());
                                    children.add(epicWorkTask);
                                }

                                children.addAll(issuesToLeafTasks(epic.getContents(), taskContext));

                                return children;
                            }

                            @Override
                            public TaskStatus getStatus() {
                                return epic.getExternalTaskStatus();
                            }

                            @Override public Date getScheduledStart() {
                                return epic.getContents().isEmpty() ? epic.getScheduledStart(taskContext.sprints) : getEarliestScheduledStart(getChildren());
                            }

                            @Override public Date getScheduledFinish() {
                                return epic.getContents().isEmpty() ? epic.getScheduledFinish(taskContext.sprints) : getLatestScheduledFinish(getChildren());
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
                    	
                    	@Override public String getId() {
                            return "WORKPLAN_NO_EPIC_DEFINED_TASK_KEY";
                        }
                    	
                        @Override public String getName() {
                            return Providers.getLocalizationProvider(JIRAIntegrationConnector.class).getConnectorText("WORKPLAN_NO_EPIC_DEFINED_TASK_NAME");
                        }

                        @Override public List<ExternalTask> getChildren() {
                            return issuesToLeafTasks(noEpicIssues, taskContext);
                        }

                        @Override public Date getScheduledStart() {
                            return getEarliestScheduledStart(getChildren());
                        }

                        @Override public Date getScheduledFinish() {
                            return getLatestScheduledFinish(getChildren());
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

                        if (includeIssuesWithNoGroup && JIRAConstants.JIRA_ISSUE_EPIC.equalsIgnoreCase(issue.getType())) {
                            selectedEpics.add((JIRAEpic) issue);
                        }

                        continue;
                    }

                    if (JIRAConstants.JIRA_ISSUE_EPIC.equalsIgnoreCase(issue.getType())) {
                        selectedEpics.add((JIRAEpic) issue);
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
                    	
                        @Override public String getId() {
                            return status;
                        }
                        
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
                            return getLatestScheduledFinish(getChildren());
                        }
                    }));
                }

                if (includeIssuesWithNoGroup && !noStatusIssues.isEmpty()) {
                    rootTasks.add(WorkDrivenPercentCompleteExternalTask.forSummaryTask(new ExternalTask() {
                    	
                    	@Override public String getId() {
                            return "WORKPLAN_NO_STATUS_DEFINED_TASK";
                        }
                    	
                        @Override public String getName() {
                            return Providers.getLocalizationProvider(JIRAIntegrationConnector.class).getConnectorText("WORKPLAN_NO_STATUS_DEFINED_TASK_NAME");
                        }

                        @Override public List<ExternalTask> getChildren() {
                            return issuesToLeafTasks(noStatusIssues, taskContext);
                        }

                        @Override public Date getScheduledStart() {
                            return getEarliestScheduledStart(getChildren());
                        }

                        @Override public Date getScheduledFinish() {
                            return getLatestScheduledFinish(getChildren());
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

                        if (includeIssuesWithNoGroup && JIRAConstants.JIRA_ISSUE_EPIC.equalsIgnoreCase(issue.getType())) {
                            selectedEpics.add((JIRAEpic) issue);
                        }

                        continue;
                    }

                    if (JIRAConstants.JIRA_ISSUE_EPIC.equalsIgnoreCase(issue.getType())) {
                        selectedEpics.add((JIRAEpic) issue);
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
                    	
                        @Override public String getId() {
                            return sprint == null ? "WORKPLAN_SPRINT_PREFIX_TASK_NAME" + " " + sprintId : sprintId;
                        }
                    	
                        @Override public String getName() {
                            return sprint == null ? Providers.getLocalizationProvider(JIRAIntegrationConnector.class).getConnectorText("WORKPLAN_SPRINT_PREFIX_TASK_NAME") + " " + sprintId : sprint.getName();
                        }

                        @Override public List<ExternalTask> getChildren() {
                            return issuesToLeafTasks(issuesBySprintId.get(sprintId), taskContext);
                        }

                        @Override public Date getScheduledStart() {
                            return (sprint == null || sprint.getStartDateAsDate() == null) ? getEarliestScheduledStart(getChildren()) : sprint.getStartDateAsDate();
                        }

                        @Override public Date getScheduledFinish() {
                            return (sprint == null || sprint.getEndDateAsDate() == null) ? getLatestScheduledFinish(getChildren()) : sprint.getEndDateAsDate();
                        }
                    }));
                }

                if (includeIssuesWithNoGroup && !backlogIssues.isEmpty()) {
                    rootTasks.add(WorkDrivenPercentCompleteExternalTask.forSummaryTask(new ExternalTask() {
                    	
                        @Override public String getId() {
                            return "WORKPLAN_SPRINT_PREFIX_TASK_KEY";
                        }
                        
                        @Override public String getName() {
                            return Providers.getLocalizationProvider(JIRAIntegrationConnector.class).getConnectorText("WORKPLAN_BACKLOG_TASK_NAME");
                        }

                        @Override public List<ExternalTask> getChildren() {
                            return issuesToLeafTasks(backlogIssues, taskContext);
                        }

                        @Override public Date getScheduledStart() {
                            return getEarliestScheduledStart(getChildren());
                        }

                        @Override public Date getScheduledFinish() {
                            return getLatestScheduledFinish(getChildren());
                        }
                    }));
                }

                break;
            case JIRAConstants.GROUP_JIRA_PORTFOLIO_HIERARCHY:

                // The Jira Portfolio Hierarchy is based on the "Portfolio Parent" field.
                // So we must first re-build the full hierarchy of tasks, not only with Epic children (Epic only), but with Portfolio Children.
                // Epic Children remain as it's still used as part of Jira Portfolio Hierarchy.
                // However, if an issue has different Epic parent & Portfolio [Epic] Parent, the Portfolio one prevails.

                JIRAPortfolioHierarchy hierarchy = new JIRAPortfolioHierarchy(issues, service);

                // Jira Portfolio allows loops at some point, so we have to check and prevent them.
                List<List<JIRAPortfolioHierarchy.Node>> loops = hierarchy.findLoops();

                if (!loops.isEmpty()) {

                    // Error, loop detected in Hierarchy

                    for (final List<JIRAPortfolioHierarchy.Node>loop : loops) {
                        rootTasks.add(new ExternalTask() {
                            @Override public String getName() {
                                StringBuilder errorMessage = new StringBuilder("ERROR: Loop detected in JIRA issues hierarchy: ");
                                boolean first = true;
                                for (JIRAPortfolioHierarchy.Node node : loop) {
                                    if (first) {
                                        first = false;
                                    } else {
                                        errorMessage.append("->");
                                    }
                                    errorMessage.append(node.getIssue().getKey());
                                }
                                return errorMessage.toString();
                            }
                        });
                    }
                } else {

                    for (JIRAPortfolioHierarchy.Node root : hierarchy.getRootNodes()) {
                        rootTasks.add(convertNodeToExternalTask(root, taskContext));
                    }

                    for (JIRAPortfolioHierarchy.Node standaloneTask : hierarchy.getStandaloneNodes()) {
                        rootTasks.add(convertNodeToExternalTask(standaloneTask, taskContext));
                    }

                    selectedEpics.addAll(hierarchy.getEpics());

                }

                break;
        }

        if (!StringUtils.isBlank(values.get(JIRAConstants.OPTION_ADD_EPIC_MILESTONES))) {

            final boolean isMajorMilestone = "MAJOR".equals(values.get(JIRAConstants.OPTION_ADD_EPIC_MILESTONES));

            if (!selectedEpics.isEmpty()) {

                rootTasks.add(WorkDrivenPercentCompleteExternalTask.forSummaryTask(new ExternalTask() {

                    private List<ExternalTask> children;

                    {
                        children = new ArrayList<ExternalTask>(selectedEpics.size());

                        for (final JIRAEpic epic: selectedEpics) {

                            final Date epicMilestoneDate = epic.getEstimatedFinishDate(taskContext.sprints);

                            children.add(WorkDrivenPercentCompleteExternalTask.forLeafTask(new ExternalTask() {

                                private Date milestoneDate = epicMilestoneDate;
                                
                                @Override public String getId() {
                                    return epic.getKey();
                                }

                                @Override
                                public String getName() {
                                    return epic.getName();
                                }

                                @Override
                                public boolean isMilestone() {
                                    return true;
                                }

                                @Override
                                public boolean isMajorMilestone() {
                                    return isMajorMilestone;
                                }

                                @Override
                                public Date getScheduledStart() {
                                    return adjustStartDateTime(milestoneDate);
                                }

                                @Override
                                public Double getScheduledDurationOverrideValue() {
                                    return 0d;
                                }

                                @Override
                                public TaskStatus getStatus() {
                                    return epic.getExternalTaskStatus();
                                }

                                @Override
                                public Date getScheduledFinish() {
                                    return adjustStartDateTime(milestoneDate);
                                }
                            }, 0, 0));
                        }
                    }
                    
                    @Override
                    public String getId() {
                        return "EPIC_MILESTONE_TASK_KEY";
                    }

                    @Override
                    public String getName() {
                        return Providers.getLocalizationProvider(JIRAIntegrationConnector.class).getConnectorText("EPIC_MILESTONE_TASK_NAME");
                    }

                    @Override
                    public List<ExternalTask> getChildren() {
                        return children;
                    }
                }));
            }
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
                        
                        @Override public String getId() {
                            String rootTaskKey = values.get(JIRAConstants.OPTION_ROOT_TASK_NAME);
                            if (StringUtils.isBlank(rootTaskKey)) {
                            	rootTaskKey = "WORKPLAN_ROOT_TASK_TASK_KEY";
                            }

                            return rootTaskKey;
                        }

                        @Override public String getName() {
                            String rootTaskName = values.get(JIRAConstants.OPTION_ROOT_TASK_NAME);
                            if (StringUtils.isBlank(rootTaskName)) {
                                rootTaskName = Providers.getLocalizationProvider(JIRAIntegrationConnector.class).getConnectorText("WORKPLAN_ROOT_TASK_TASK_NAME");
                            }

                            return rootTaskName;
                        }

                        @Override public Date getScheduledStart() {
                            return getEarliestScheduledStart(getChildren());
                        }

                        @Override public Date getScheduledFinish() {
                            return getLatestScheduledFinish(getChildren());
                        }
                    });
                    return Arrays.asList(new ExternalTask[] {rootTask});
                } else {
                    return rootTasks;
                }
            }
        };
    }

    @Override
    public WorkplanMapping linkTaskWithExternal(WorkPlanIntegrationContext context, WorkplanMapping workplanMapping, ValueSet values) {
        // A config that's too long will exceed varchar 4000 for storing config, so we'll remove all issue types that are not selected.
        return removeUncheckedIssuesTypesFromConfig(workplanMapping);
    }

    private WorkplanMapping removeUncheckedIssuesTypesFromConfig(WorkplanMapping workplanMapping) {

        Set<String> issueTypesToRemove = new HashSet<>();

        // Update the display config JSon
        String displayConfigJson = workplanMapping.getConfigDisplayJson();
        if (displayConfigJson != null) {
            JSONObject json = (JSONObject)JSONSerializer.toJSON(displayConfigJson);
            JSONArray oldConfig = json.getJSONArray("config");
            JSONArray newConfig = new JSONArray();
            for (int i = 0; i < oldConfig.size(); i++) {
                JSONObject entry = oldConfig.getJSONObject(i);
                String label = entry.getString("label");
                String text =  entry.getString("text");

                if ("NO.TXT".equals(text) && !JIRAConstants.OPTION_INCLUDE_ISSUES_NO_GROUP.equals(label)
                        && !JIRAConstants.OPTION_ADD_ROOT_TASK.equals(label)){
                    // Skip that issue type
                    issueTypesToRemove.add(label);
                } else {
                    newConfig.add(entry);
                }
            }

            json.put("config", newConfig);
            workplanMapping.setConfigDisplayJson(json.toString());
        }

        // Update the real config JSon
        String configJson = workplanMapping.getConfigJson();
        if (configJson != null) {
            JSONObject json = (JSONObject)JSONSerializer.toJSON(configJson);
            for (String issueTypeToRemove: issueTypesToRemove) {
                json.remove(getParamNameFromIssueTypeName(issueTypeToRemove));
            }
            workplanMapping.setConfigJson(json.toString());
        }

        return workplanMapping;

    }

    private ExternalTask convertNodeToExternalTask(final JIRAPortfolioHierarchy.Node node, final TasksCreationContext taskContext) {

        if (node.getChildren().isEmpty()) {
            // Leaf task.
            return convertJiraIssueToLeafExternalTask(node.getIssue(), taskContext);
        }

        // Summary Task.
        final JIRASubTaskableIssue issue = node.getIssue();

        return WorkDrivenPercentCompleteExternalTask.forSummaryTask(new ExternalTask() {
        	
            @Override public String getId() {
                return issue.getKey();
            }
            
            @Override public String getName() {
                return issue.getFullTaskName();
            }

            @Override public List<ExternalTask> getChildren() {

                List<ExternalTask> children = new ArrayList<ExternalTask>();

                if (issue.hasWork()) {
                    // Epics that will be stored as summary tasks will not have their work taken into account, so we need to create a dummy leaf task for it.
                    final WorkDrivenPercentCompleteExternalTask workTask = convertJiraIssueToLeafExternalTask(issue, taskContext);
                    workTask.setNameOverride("[Work]"+workTask.getName());
                    children.add(workTask);
                }

                for (JIRAPortfolioHierarchy.Node child: node.getChildren()) {
                    children.add(convertNodeToExternalTask(child, taskContext));
                }

                return children;
            }

            @Override
            public TaskStatus getStatus() {
                return issue.getExternalTaskStatus();
            }

            @Override public Date getScheduledStart() {
                return node.getChildren().isEmpty() ? issue.getScheduledStart(taskContext.sprints) : getEarliestScheduledStart(getChildren());
            }

            @Override public Date getScheduledFinish() {
                return node.getChildren().isEmpty() ? issue.getScheduledFinish(taskContext.sprints) : getLatestScheduledFinish(getChildren());
            }

            @Override public Map<Integer, UserData> getUserDataFields() {


                Map<Integer, ExternalTask.UserData> userData = new HashMap<Integer, ExternalTask.UserData>();

                // Epics can have normal story points, like any leaf task
                String storyPointsUserDataIndex = taskContext.configValues.get(JIRAConstants.SELECT_USER_DATA_STORY_POINTS);

                if (!StringUtils.isBlank(storyPointsUserDataIndex) && !"0".equals(storyPointsUserDataIndex)) {
                    if (issue.getStoryPoints() != null) {
                        userData.put(Integer.parseInt(storyPointsUserDataIndex), new ExternalTask.UserData(issue.getStoryPoints().toString(), issue.getStoryPoints().toString()));
                    }
                }

                // Epics can also have aggregated Story points.
                String aggregatedStoryPointsUserDataIndex = taskContext.configValues.get(JIRAConstants.SELECT_USER_DATA_AGGREGATED_STORY_POINTS);

                if (!StringUtils.isBlank(aggregatedStoryPointsUserDataIndex) && !"0".equals(aggregatedStoryPointsUserDataIndex)) {
                    String aggSP = String.valueOf(node.getAggregatedStoryPoints());
                    userData.put(Integer.parseInt(aggregatedStoryPointsUserDataIndex), new ExternalTask.UserData(aggSP, aggSP));
                }

                return userData;
            }

        });

    }

    private Date getLatestScheduledFinish(List<ExternalTask> children) {
        Date date = null;
        for (ExternalTask child : children) {
            if (child.getScheduledFinish() == null) {
                continue;
            }
            if (date == null || child.getScheduledFinish().after(date)) {
                date = child.getScheduledFinish();
            }
        }

        return date != null ? date : JIRAEntity.getDefaultFinishDate();
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

        return date != null ? date : JIRAEntity.getDefaultStartDate();
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
            externalTasks.add(convertJiraIssueToLeafExternalTask(issue, context));
        }

        return externalTasks;
    }

    private WorkDrivenPercentCompleteExternalTask convertJiraIssueToLeafExternalTask(final JIRASubTaskableIssue issue, final TasksCreationContext context)
    {
        // First, let's compute the work of that task
        double doneWork = 0d;
        double remainingWork = 0d;

        switch(context.percentCompleteType) {
            case JIRAConstants.PERCENT_COMPLETE_WORK:
                if (issue.getWork() != null) {
                    doneWork += getNullSafeDouble(issue.getWork().getTimeSpentHours());
                    remainingWork += getNullSafeDouble(issue.getWork().getRemainingEstimateHours());
                }

                // Sub Tasks can have work logged against them.
                if (issue.getSubTasks() != null) {
                    for (JIRASubTask subTask: issue.getSubTasks()) {
                        if (subTask.getWork() != null) {
                            doneWork += getNullSafeDouble(subTask.getWork().getTimeSpentHours());
                            remainingWork += getNullSafeDouble(subTask.getWork().getRemainingEstimateHours());
                        }
                    }
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
                // Sub Tasks don't have Story Points
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
                return issue.getScheduledStart(context.sprints);
            }

            @Override public Date getScheduledFinish() {
                return issue.getScheduledFinish(context.sprints);
            }

            @Override public List<ExternalTaskActuals> getActuals() {
                return generateActuals(issue, context, getScheduledStart());
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

        return WorkDrivenPercentCompleteExternalTask.forLeafTask(task, doneWork, remainingWork);
    }

    private double getNullSafeDouble(Double d) {
        return d == null ? 0d : d.doubleValue();
    }


    private List<ExternalTaskActuals> generateActuals(final JIRAIssue issue, TasksCreationContext context,
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

        if (JIRAConstants.ACTUALS_NO_ACTUALS.equals(context.actualsType)) {
            // No Actuals!
            return actuals;
        } else if (JIRAConstants.ACTUALS_SP.equals(context.actualsType)) {
            // Generate Actual Hours from Story Points
            actuals.add(convertIssueStoryPointsToActuals(issue, context));
        } else {
            // Default: generate Actuals from Work logged in JIRA
            if (issue.getWork() != null) {
                for (JIRAIssueWork.JIRAWorklogEntry worklog : issue.getWork().getWorklogs()) {
                    if (worklog != null) {
                        // These work done are considered 100% complete only if using % work complete or if using % SP done and the task is done.
                        actuals.add(convertWorklogEntryToActuals(worklog, context,
                                JIRAConstants.PERCENT_COMPLETE_DONE_STORY_POINTS.equals(context.percentCompleteType) ? issue.isDone() : true));
                    }
                }

                // Remaining Effort has a dedicated Actuals assigned to whoever the issue is assigned to.
                if (issue.getWork().getRemainingEstimateHours() != null && issue.getWork().getRemainingEstimateHours() > 0) {
                    actuals.add(getRemainingEffortActuals(issue.getWork().getRemainingEstimateHours(), issue.getAssigneePpmUserId()));
                }
            }

            if (issue instanceof JIRASubTaskableIssue) {
                // Sub-tasks cannot have story points but they can have time logged against them.
                List<JIRASubTask> subTasks = ((JIRASubTaskableIssue)issue).getSubTasks();

                if (subTasks != null) {
                    for (JIRASubTask subTask : subTasks) {
                        actuals.addAll(generateActuals(subTask, context, scheduledStart));
                    }
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

    private ExternalTaskActuals convertIssueStoryPointsToActuals(final JIRAIssue issue, final TasksCreationContext context) {

        final long spEffort = issue.getStoryPoints() == null ? 0 : issue.getStoryPoints() * context.hoursPerSp;

        return new ExternalTaskActuals() {

            @Override public double getScheduledEffort() {
                return spEffort;
            }

            @Override public Date getActualStart() {
                if (ExternalTask.TaskStatus.IN_PLANNING.equals(issue.getExternalTaskStatus())) {
                    return null;
                }

                return issue.getScheduledStart(context.sprints);
            }

            @Override public Double getEstimatedRemainingEffort() {
                if (issue.isDone()) {
                    return 0d;
                } else {
                    return Double.valueOf(spEffort);
                }
            }

            @Override public Date getActualFinish() {
                return issue.isDone() ? issue.getScheduledFinish(context.sprints) : null;
            }

            @Override public double getActualEffort() {
                return issue.isDone() ? spEffort : 0d;
            }

            @Override public double getPercentComplete() {
                // Each actual line from work log will be considered to be completed only if the task is done.
                return issue.isDone() ? 100d : 0d;
            }

            @Override public long getResourceId() {
                return issue.getAssigneePpmUserId();
            }

            @Override public Date getEstimatedFinishDate() {
                return issue.getScheduledFinish(context.sprints);
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
                info.setEpicId(values.get(JIRAConstants.KEY_IMPORT_SELECTION_DETAILS));
                return info;
            case JIRAConstants.IMPORT_ONE_VERSION:
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

        public String actualsType;

        public int hoursPerSp;
    }
}

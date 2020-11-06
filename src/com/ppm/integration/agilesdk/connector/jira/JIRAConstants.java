
package com.ppm.integration.agilesdk.connector.jira;

import com.ppm.integration.agilesdk.connector.jira.service.JIRAService;

public class JIRAConstants {

    public static final String JIRA_NAME_PREFIX = "#@#name#@#";

    public static final String NULL_VALUE = "<null>";

    public static final String REPLACE_PROJECT_KEY = "%PROJECT_KEY%";

    public static final String KEY_BASE_URL = "baseURL";

    public static final String KEY_PROXY_HOST = "proxyHost";

    public static final String KEY_PROXY_PORT = "proxyPort";

    public static final String KEY_ADMIN_USERNAME = "adminUsername";

    public static final String KEY_ADMIN_PASSWORD = "adminPassword";

    public static final String KEY_USE_ADMIN_PASSWORD_TO_MAP_TASKS = "useAdminPasswordToMapTasks";

    public static final String KEY_IMPORT_ASSIGNED_USERS_TO_TASKS = "importAssignedUsersToTasks";

    public static final String KEY_USERNAME = "username";

    public static final String KEY_PASSWORD = "password";

    public static final String KEY_TASK_STATUS_IN_PLANNING = "statusInPlanning";
    public static final String KEY_TASK_STATUS_READY = "statusReady";
    public static final String KEY_TASK_STATUS_ACTIVE = "statusActive";
    public static final String KEY_TASK_STATUS_IN_PROGRESS = "statusInProgress";
    public static final String KEY_TASK_STATUS_COMPLETED = "statusCompleted";
    public static final String KEY_TASK_STATUS_PENDING_PREDECESSOR = "statusPendingPredecessor";
    public static final String KEY_TASK_STATUS_CANCELLED = "statusCancelled";
    public static final String KEY_TASK_STATUS_ON_HOLD = "statusOnHold";
    public static final String KEY_TASK_STATUS_UNKNOWN = "statusUnknown";

    public static final String OPTION_INCLUDE_ISSUES_NO_GROUP = "option_include_issues_in_no_group";

    public static final String OPTION_ADD_ROOT_TASK = "option_add_root_task";

    public static final String OPTION_ROOT_TASK_NAME = "option_root_task_name";

    public static final String OPTION_ADD_EPIC_MILESTONES = "option_add_epic_milestones";

    public static final String LABEL_ISSUES_TO_IMPORT = "jira_issues_to_import";

    public static final String LABEL_TASKS_OPTIONS = "jira_tasks_options";

    public static final String LABEL_PROGRESS_AND_ACTUALS = "label_progress_and_actuals";

    public static final String LABEL_SELECT_WHAT_TO_IMPORT = "label_select_what_to_import";

    public static final String KEY_IMPORT_SELECTION = "import_selection";

    public static final String LABEL_WORK_PLAN_OPTIONS = "jira_work_plan_options";

    public static final String SELECT_USER_DATA_STORY_POINTS = "jira_user_data_story_points";

    public static final String SELECT_USER_DATA_AGGREGATED_STORY_POINTS = "jira_user_data_aggregated_story_points";

    public static final String KEY_IMPORT_SELECTION_DETAILS = "import_selection_details";

    public static final String KEY_JIRA_PROJECT = "jira_project_name";

    public static final String KEY_IMPORT_GROUPS = "import_groups";

    public static final String KEY_PERCENT_COMPLETE = "percent_complete";

    public static final String PERCENT_COMPLETE_WORK = "percent_complete_work";

    public static final String PERCENT_COMPLETE_DONE_STORY_POINTS = "percent_complete_story_points_done";

    public static final String KEY_ACTUALS = "actuals_choice";

    public static final String ACTUALS_LOGGED_WORK = "actuals_logged_work";

    public static final String ACTUALS_SP = "actuals_sp";

    public static final String ACTUALS_NO_ACTUALS = "actuals_no_actuals";

    public static final String ACTUALS_SP_RATIO = "actuals_sp_ratio";

    public static final String GROUP_STATUS = "group_status";

    public static final String GROUP_SPRINT = "group_sprint";

    public static final String GROUP_EPIC = "group_epic";

    public static final String GROUP_JIRA_PORTFOLIO_HIERARCHY = "group_jira_portfolio_hierarchy";

    public static final String IMPORT_ALL_PROJECT_ISSUES = "all_project_planned_issues";

    public static final String IMPORT_ONE_EPIC = "epic";

    public static final String IMPORT_ONE_BOARD = "board";

    public static final String IMPORT_PORTFOLIO_PREFIX  = "portfolio_";

    public static final String IMPORT_ONE_VERSION = "version";

    public static final String API_VERSION2_API_ROOT = "/rest/api/2/";

    public static final String PORTFOLIO_HIERARCHY_REST = "/rest/jpo-api/1.0/hierarchy";

    public static final String SEARCH_USER = API_VERSION2_API_ROOT + "user/search";

    public static final String API_VERSION1_API_ROOT = "/rest/agile/1.0/";

    public static final String PROJECT_SUFFIX = API_VERSION2_API_ROOT + "project";

    public static final String CREATEMETA_SUFFIX = API_VERSION2_API_ROOT + "issue/createmeta";

    public static final String BOARD_SUFFIX = API_VERSION1_API_ROOT + "board";

    public static final String SPRINT_SUFFIX = API_VERSION1_API_ROOT + "sprint";
    
    public static final String KEY_FIELD_TYPE_ARRAY = "array";
    
    public static final String KEY_FIELD_TYPE_USER = "user";
    
    public static final String KEY_FIELD_TYPE_STRING = "string";
    
    public static final String KEY_FIELD_TYPE_NUMBER = "number";
    
    public static final String KEY_FIELD_TYPE_OPTION = "option";
    
    public static final String KEY_FIELD_TYPE_PRIORITY = "priority";

    // Just add JQL string from JiraIssuesRetrieverUrlBuilder class
    public static final String SEARCH_URL =
            API_VERSION2_API_ROOT + "search?expand=schema&jql=";

    public static final String VERSIONS_SUFFIX = API_VERSION2_API_ROOT + "project/" + REPLACE_PROJECT_KEY + "/versions";

    /**
     * @see JIRAService#getIssueTypesPerProject()
     */
    public static final String JIRA_ISSUE_TYPE_PREFIX = "ISSUE_TYPE_";

    /** @deprecated
     * @see JIRAService#getIssueTypesPerProject()
     */
    public static final String JIRA_ISSUE_STORY = "STORY";

    public static final String JIRA_ISSUE_EPIC = "EPIC";

    /** @deprecated
     * @see JIRAService#getIssueTypesPerProject()
     */
    public static final String JIRA_ISSUE_BUG = "BUG";

    /** @deprecated
     * @see JIRAService#getIssueTypesPerProject()
     */
    public static final String JIRA_ISSUE_TASK = "TASK";

    /** @deprecated
     * @see JIRAService#getIssueTypesPerProject()
     */
    public static final String JIRA_ISSUE_FEATURE = "FEATURE";

    public static final String JIRA_STORY_POINTS_CUSTOM_NAME = "Story Points";

    public static final String JIRA_SPRINT_CUSTOM = "com.pyxis.greenhopper.jira:gh-sprint";

    public static final String JIRA_EPIC_LINK_CUSTOM = "com.pyxis.greenhopper.jira:gh-epic-link";

    public static final String JIRA_EPIC_NAME_CUSTOM = "com.pyxis.greenhopper.jira:gh-epic-label";

    public static final String JIRA_PORTFOLIO_PARENT_CUSTOM =  "com.atlassian.jpo:jpo-custom-field-parent";

    public static final String JIRA_REST_ISSUE_URL = API_VERSION2_API_ROOT + "issue/";

    public static final String JIRA_GET_ISSUE_WORKLOG = API_VERSION2_API_ROOT + "issue/%issue%/worklog";

    public static final String JIRA_FIELDS_URL = API_VERSION2_API_ROOT + "field/";

    // Backward compatibility
    public static final String KEY_ALL_PROJECT_PLANNED_ISSUES = "all_project_planned_issues";
    public static final String KEY_EPIC = "epic";
    public static final String KEY_ALL_EPICS = "all_epics";
    public static final String KEY_VERSION = "version";

    // Timesheet import settings

    public static final String TS_ALL_PROJECTS = "ts_all_projects";
    public static final String TS_GROUP_WORK_BY = "ts_group_work_by";
    public static final String TS_GROUP_BY_PROJECT = "ts_group_by_project";
    public static final String TS_GROUP_BY_EPIC = "ts_group_by_epic";
    public static final String TS_GROUP_BY_ISSUE = "ts_group_by_issue";

    public static final String TS_IMPORT_HOURS_OPTION = "ts_import_hours_option";
    public static final String TS_IMPORT_HOURS_HOURS_ONLY = "ts_import_hours_hours_only";
    public static final String TS_IMPORT_HOURS_SP_ONLY = "ts_import_hours_sp_only";
    public static final String TS_IMPORT_HOURS_BOTH = "ts_import_hours_both";

    public static final String TS_SP_TO_HOURS_RATIO = "ts_sp_to_hours_ratio";

    public static final String TS_ADJUST_HOURS = "ts_adjust_hours";
    public static final String TS_ADJUST_HOURS_YES = "ts_adjust_hours_yes";
    public static final String TS_ADJUST_HOURS_NO = "ts_adjust_hours_no";

    public static final String TS_ADJUST_HOURS_CHOICE = "ts_adjust_hours_choice";
    public static final String TS_ADJUST_HOURS_CHOICE_DAILY = "ts_adjust_hours_choice_daily";
    public static final String TS_ADJUST_HOURS_CHOICE_TOTAL = "ts_adjust_hours_choice_total";

    public static final String TS_DAILY_HOURS = "ts_daily_hours";

    public static final String JIRA_FIELD_RESOLUTION_DATE = "resolutiondate";
    
    public static final String SPLIT_CHAR = "#@#";


}

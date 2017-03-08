
package com.ppm.integration.agilesdk.connector.jira;

public class JIRAConstants {

    public static final String NULL_VALUE = "<null>";

    public static final String REPLACE_PROJECT_KEY = "%PROJECT_KEY%";

    public static final String KEY_BASE_URL = "baseURL";

    public static final String KEY_PROXY_HOST = "proxyHost";

    public static final String KEY_PROXY_PORT = "proxyPort";

    public static final String KEY_LEVEL_OF_DETAILS_TO_SYNCHRONIZE = "level_of_details_to_synchronize";

    public static final String KEY_USERNAME = "username";

    public static final String KEY_PASSWORD = "password";

    public static final String APP_CLIENT_ID = "clientId";

    public static final String APP_CLIENT_SECRET = "clientSecret";

    public static final String KEY_SUBSCRIPTION = "SUBSCRIPTION";

    public static final String KEY_WORKSPACE = "WORKSPACE";

    public static final String KEY_PROJECT = "PROJECT";

    public static final String KEY_USE_GLOBAL_PROXY = "use_global_proxy";

    public static final String KEY_JIRA_ISSUES_TO_IMPORT = "jira_issues_to_import";

    public static final String KEY_IMPORT_SELECTION = "import_selection";

    public static final String KEY_IMPORT_SELECTION_DETAILS = "import_selection_details";

    public static final String KEY_JIRA_PROJECT_NAME = "jira_project_name";

    public static final String KEY_JIRA_PROJECT_KEY = "jira_project_key";

    public static final String KEY_JIRA_ISSUE_TYPES = "jira_issue_types";

    public static final String KEY_ALL_PROJECT_PLANNED_ISSUES = "all_project_planned_issues";

    public static final String KEY_EPIC = "epic";

    public static final String KEY_ALL_EPICS = "all_epics";

    public static final String KEY_VERSION = "version";

    public static final String KEY_INCLUDE_ISSUES_BREAKDOWN = "include_issues_breakdown";

    private static final String API_VERSION_API_ROOT = "/rest/api/2/";

    private static final String TEMPO_API_VERSION_API_ROOT = "/rest/tempo-timesheets/3/";

    public static final String PROJECT_SUFFIX = API_VERSION_API_ROOT + "project";

    public static final String ISSUES_IN_SPRINT_SUFFIX =
            API_VERSION_API_ROOT + "search?expand=schema&jql=sprint!=null and issueType!=sub-task and project=";

    public static final String ISSUES_SUFFIX =
            API_VERSION_API_ROOT + "search?expand=schema&jql=issueType!=sub-task and project=";

    public static final String EPICS_SUFFIX = API_VERSION_API_ROOT + "search?expand=schema&jql=project=";

    public static final String ISSUETYPES_SUFFIX = API_VERSION_API_ROOT + "issuetype";

    public static final String TEMPO_WORKLOGS_SUFFIX = TEMPO_API_VERSION_API_ROOT + "worklogs";

    public static final String VERSIONS_SUFFIX = API_VERSION_API_ROOT + "project/" + REPLACE_PROJECT_KEY + "/versions";

    public static final String ISSUES_WORKLOGS_SUFFIX = API_VERSION_API_ROOT + "search?fields=worklog,summary&jql=";

    public static final String ISSUES_ORDER_BY_SPRINT_CREATED = "+order+by+sprint,created+ASC";

    public static final String JIRA_ISSUE_STORY = "STORY";

    public static final String JIRA_ISSUE_EPIC = "EPIC";

    public static final String JIRA_ISSUE_BUG = "BUG";

    public static final String JIRA_ISSUE_TASK = "TASK";

    public static final String JIRA_SPRINT_CUSTOM = "com.pyxis.greenhopper.jira:gh-sprint";

    public static final String JIRA_EPIC_LINK_CUSTOM = "com.pyxis.greenhopper.jira:gh-epic-link";

}

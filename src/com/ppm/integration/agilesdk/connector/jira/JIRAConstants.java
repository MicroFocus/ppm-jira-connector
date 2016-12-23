package com.ppm.integration.agilesdk.connector.jira;

public class JIRAConstants {

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

	public static final String KEY_JIRA_PROJECT_NAME = "jira_project_name";

	public static final String KEY_JIRA_PROJECT_KEY = "jira_project_key";

	public static final String KEY_JIRA_ISSUE_TYPES = "jira_issue_types";

	private static final String API_VERSION_API_ROOT = "/rest/api/2/";
	private static final String TEMPO_API_VERSION_API_ROOT = "/rest/tempo-timesheets/3/";

	public static final String PROJECT_SUFFIX = API_VERSION_API_ROOT + "project";
	public static final String ISSUES_SUFFIX = API_VERSION_API_ROOT + "search?expand=schema&jql=project=";
	public static final String ISSUETYPES_SUFFIX = API_VERSION_API_ROOT + "issuetype";
	public static final String TEMPO_WORKLOGS_SUFFIX = TEMPO_API_VERSION_API_ROOT + "worklogs";

	public static final String ISSUES_WORKLOGS_SUFFIX = API_VERSION_API_ROOT + "search?fields=worklog,summary&jql=";

	public static final String JIRA_ISSUE_STORY = "STORY";
	public static final String JIRA_ISSUE_EPIC = "EPIC";
	public static final String JIRA_ISSUE_BUG = "BUG";
	public static final String JIRA_ISSUE_TASK = "TASK";

	public static final String JIRA_SPRINT_CUSTOM = "com.pyxis.greenhopper.jira:gh-sprint";

}

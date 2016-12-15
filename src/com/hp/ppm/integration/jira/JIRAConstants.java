package com.hp.ppm.integration.jira;

public class JIRAConstants {

	public static final String KEY_BASE_URL = "baseURL";
	public static final String KEY_PROXY_HOST = "proxyHost";
	public static final String KEY_PROXY_PORT = "proxyPort";

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

	public static final String PROJECT_SUFFIX = API_VERSION_API_ROOT + "project";
	public static final String ISSUES_SUFFIX = API_VERSION_API_ROOT + "search?jql=project=";
	public static final String ISSUETYPES_SUFFIX = API_VERSION_API_ROOT + "issuetype";

	public static final String JIRA_ISSUE_STORY = "STORY";
	public static final String JIRA_ISSUE_EPIC = "EPIC";
	public static final String JIRA_ISSUE_BUG = "BUG";
	public static final String JIRA_ISSUE_TASK = "TASK";

}

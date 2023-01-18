package com.ppm.integration.agilesdk.connector.jira.service;

import com.hp.ppm.common.model.AgileEntityIdName;
import com.hp.ppm.common.model.AgileEntityIdProjectDate;
import com.hp.ppm.user.model.User;
import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.jira.JIRAConstants;
import com.ppm.integration.agilesdk.connector.jira.JIRAServiceProvider;
import com.ppm.integration.agilesdk.connector.jira.model.*;
import com.ppm.integration.agilesdk.connector.jira.rest.util.IRestConfig;
import com.ppm.integration.agilesdk.connector.jira.rest.util.JIRARestConfig;
import com.ppm.integration.agilesdk.connector.jira.rest.util.JiraRestWrapper;
import com.ppm.integration.agilesdk.connector.jira.rest.util.exception.RestRequestException;
import com.ppm.integration.agilesdk.connector.jira.util.JiraIssuesRetrieverUrlBuilder;
import com.ppm.integration.agilesdk.connector.jira.util.dm.AgileEntityUtils;
import com.ppm.integration.agilesdk.provider.UserProvider;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wink.client.ClientResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.xml.datatype.XMLGregorianCalendar;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This class is in charge of making all the REST calls to JIRA.
 *
 * It is not thread safe.
 */
public class JIRAService {

    private static final int SUB_TASK_BATCH_SIZE = 250;

    private UserProvider userProvider = null;

    private final Logger logger = Logger.getLogger(this.getClass());

    private String baseUri;

    private Boolean isJiraPortfolioEnabled = null;

    private  List<HierarchyLevelInfo> portfolioHierarchyLevelsInfo = null;

    public JIRAService resetUserCredentials(ValueSet config) {
        IRestConfig userRestConfig = new JIRARestConfig();
        userRestConfig.setProxy(config.get(JIRAConstants.KEY_PROXY_HOST), config.get(JIRAConstants.KEY_PROXY_PORT));
        userRestConfig.setBasicAuthorizationCredentials(config.get(JIRAConstants.KEY_USERNAME),
                config.get(JIRAConstants.KEY_PASSWORD));
        userWrapper = new JiraRestWrapper(userRestConfig);
        return this;
    }

    /**
     * This method retrieves all descendants of the given Portfolio Issue (including the issue itself) as well as their subtasks.
     */
    public List<JIRASubTaskableIssue> getPortfolioIssueDescendants(String portfolioIssueKey, Set<String> issueTypes) {

        // First, pick the Jira Issue itself.
        List<JIRASubTaskableIssue> portfolioIssueDescendants  = getAllIssuesByKeys(new HashSet(Arrays.asList(portfolioIssueKey)));

        if (portfolioIssueDescendants.isEmpty()) {
            throw new RuntimeException("Couldn't retrieve the Portfolio Issue " + portfolioIssueKey);
        }
        JIRASubTaskableIssue rootIssue = portfolioIssueDescendants.get(0);

        List<String> newIssuesKeys = new ArrayList<>();
        Set<String> importedKeys = new HashSet<>();
        newIssuesKeys.add(rootIssue.getKey());
        importedKeys.add(rootIssue.getKey());

        while(!newIssuesKeys.isEmpty()) {
            List<JIRASubTaskableIssue> directDescendants = getAllEpicOrPortfolioDirectDescendants(newIssuesKeys, issueTypes, importedKeys);
            portfolioIssueDescendants.addAll(directDescendants);
            newIssuesKeys.clear();
            for (JIRASubTaskableIssue directDescendant : directDescendants) {
                // Jira Portfolio is not very good at data integrity so it's possible to end up with loops in the hierarchy when changing hierarchy configuration.
                // We need following check to make sure we don't enter an endless loop of importing stuff already imported.
                if (!importedKeys.contains(directDescendant.getKey())) {
                    newIssuesKeys.add(directDescendant.getKey());
                }
            }

            importedKeys.addAll(newIssuesKeys);
        }

        return portfolioIssueDescendants;
    }

    /**
     * Returns Portfolio children or Epic Children of the provided parent issues keys, only if they do match the issue Types.
     *
     * Sub-tasks included.
     */
    private List<JIRASubTaskableIssue> getAllEpicOrPortfolioDirectDescendants(List<String> parentIssueKeys, Set<String> issueTypes, Set<String> excludedIds)
    {
        // First, get all descendant issues (without sub-tasks) and retrieve their Keys.
        JiraIssuesRetrieverUrlBuilder searchUrlBuilder =
                new JiraIssuesRetrieverUrlBuilder(baseUri).setIssuesTypes(issueTypes)
                        .addExtraFields(getCustomFields().getJiraCustomFields());


        StringBuilder constraint = new StringBuilder("(")
                // Retrieving issues children of an epic
                .append("cf["+getCustomFields().epicLinkCustomField.substring("customfield_".length())+"] in (" +StringUtils.join(parentIssueKeys, ",")+")")
                .append(" or ")
                // Retrieving issues children of a portfolio issue
                .append("cf["+getCustomFields().portfolioParentCustomField.substring("customfield_".length())+"] in (" +StringUtils.join(parentIssueKeys, ",")+")")
                .append(") ");

        searchUrlBuilder.addAndConstraint(constraint.toString());

        // We want to exclude the parent nodes themselves, as that could create circular dependencies.
        searchUrlBuilder.addAndConstraint("key not in ("+StringUtils.join(parentIssueKeys, ",")+")");

        // Exclude other IDs
        searchUrlBuilder.addAndConstraint("key not in ("+StringUtils.join(excludedIds, ",")+")");

        List<JIRASubTaskableIssue> descendants = retrieveIssues(searchUrlBuilder, true);

        return descendants;
    }


    public List<AgileEntityIdName> getAgileEntityIdsAndNames(String agileProjectValue, String entityType) {
        JiraIssuesRetrieverUrlBuilder searchUrlBuilder =
                new JiraIssuesRetrieverUrlBuilder(baseUri).retrieveOnlyFields("key", "issuetype", "summary");

        if (!StringUtils.isBlank(agileProjectValue)) {
            searchUrlBuilder.setProjectKey(agileProjectValue);
        }

        if (!StringUtils.isBlank(entityType)) {
            searchUrlBuilder.setStandardIssueTypes(entityType);
        }

        IssueRetrievalResult result =
                runIssueRetrievalRequest(decorateOrderBySprintCreatedUrl(searchUrlBuilder).toUrlString());

        List<AgileEntityIdName> results = new ArrayList<>(result.getIssues().size());

        for (JSONObject obj : result.getIssues()) {
            JIRAIssue issue = getIssueFromJSONObj(obj);
            AgileEntityIdName idAndName = new AgileEntityIdName(issue.getKey(), "[" + issue.getKey() + "] " + issue.getName());
            results.add(idAndName);
        }

        return results;
    }

    private class CustomFields {
        public String epicNameCustomField = null;

        public String epicLinkCustomField = null;

        public String storyPointsCustomField = null;

        public String sprintIdCustomField = null;

        public String portfolioParentCustomField = null;

        /**
         * @return All Jira custom fields needed all of the time: epic name & link, story points, sprint, and (if using Jira Portfolio) Portfolio Parent.
         */
        public String[] getJiraCustomFields() {

            if (portfolioParentCustomField == null) {
                return new String[] {epicNameCustomField, epicLinkCustomField, storyPointsCustomField,
                        sprintIdCustomField};
            } else {
                return new String[] {epicNameCustomField, epicLinkCustomField, storyPointsCustomField,
                        sprintIdCustomField, portfolioParentCustomField};
            }
        }
    }

    private CustomFields customFields = null;

    private Map<String, Set<String>> issueTypesPerProject = new HashMap<>();
    private Set<String> subTasksIssueTypeNames = null;
    private List<JIRAIssueType> allIssueTypes = null;


    private boolean canUseNewCreateMetaAPI() {
        if (canUseNewCreateMetaAPI != null) {
            return canUseNewCreateMetaAPI;
        }

        // We initialize canUseNewCreateMetaAPI based on Jira version

        try {
            String url = baseUri + JIRAConstants.SERVERINFO_SUFFIX;

            ClientResponse response = getWrapper().sendGet(url);

            String jsonStr = response.getEntity(String.class);


            JSONObject serverInfo = new JSONObject(jsonStr);

            JSONArray versionNumbers = serverInfo.getJSONArray("versionNumbers");

            if (versionNumbers != null && (versionNumbers.length() >= 1)) {
                int majorVersion = versionNumbers.getInt(0);
                canUseNewCreateMetaAPI = majorVersion >= 9;
            }

        } catch (Exception e) {
            // If any error occurs, we consider that Jira is too old
            canUseNewCreateMetaAPI = false;
        }

        return canUseNewCreateMetaAPI;
    }

        private Boolean canUseNewCreateMetaAPI = null;


    private JiraRestWrapper userWrapper;
    private JiraRestWrapper adminWrapper;

    // By default we use admin account to do Jira calls.
    // This is because we can't be sure the invoking code will have set a userWrapper.
    private boolean useAdminAccount = true;

    public JIRAService useAdminAccount() {
        useAdminAccount = true;
        return this;
    }

    public JIRAService useNonAdminAccount() {
        useAdminAccount = false;
        return this;
    }

    /**
     * Unless the REST call must be done with Admin account, you should always retrieve the RestWrapper with this method.
     *
     * @return the admin or user wrapper, to make rest calls as the selected user.
     */
    private JiraRestWrapper getWrapper() {
        return useAdminAccount ? adminWrapper : userWrapper;
    }

    public JIRAService(String baseUri, JiraRestWrapper adminWrapper, JiraRestWrapper userWrapper) {

        this.userWrapper = userWrapper;
        this.adminWrapper = adminWrapper;

        // Base URI should not have a trailing slash.
        while (baseUri.endsWith("/")) {
            baseUri = baseUri.substring(0, baseUri.length() - 1);
        }

        this.baseUri = baseUri;
    }

    private synchronized CustomFields getCustomFields() {
        if (customFields == null) {
            initCustomFieldsInfo();
        }

        return customFields;
    }

    private synchronized UserProvider getUserProvider() {
        if (userProvider == null) {
            userProvider = JIRAServiceProvider.getUserProvider();
        }
        return userProvider;
    }

    public boolean isJiraPortfolioEnabled() {

        if (isJiraPortfolioEnabled == null) {

            if (StringUtils.isBlank(getCustomFields().portfolioParentCustomField)) {
                isJiraPortfolioEnabled = false;
            } else {
                // Jira Portfolio Cloud doesn't have a REST API
                try {
                    Collection col = getJiraPortfolioLevelsInfo();
                    if (col != null && !col.isEmpty()) {
                        isJiraPortfolioEnabled = true;
                    } else {
                        isJiraPortfolioEnabled = false;
                    }
                } catch (Exception e) {
                    isJiraPortfolioEnabled = false;
                }
            }
        }

        return isJiraPortfolioEnabled.booleanValue();
    }

    public List<JIRAIssueType> getAllNonSubTaskIssueTypes() {
        return getAllIssueTypes().stream().filter(it -> !it.isSubTask()).collect(Collectors.toList());
    }

    public List<JIRAIssueType> getAllIssueTypes() {
        if (allIssueTypes == null) {
            initIssueTypes();
        }

        return allIssueTypes;
    }

    public String getMyselfInfo() {
        ClientResponse response = getWrapper().sendGet(baseUri + JIRAConstants.MYSELF_SUFFIX);
        return response.getEntity(String.class);
    }

    public List<JIRAProject> getProjects() {
        ClientResponse response = getWrapper().sendGet(baseUri + JIRAConstants.PROJECT_SUFFIX);

        String jsonStr = response.getEntity(String.class);

        List<JIRAProject> list = new ArrayList<JIRAProject>();
        try {
            JSONArray jsonArray = new JSONArray(jsonStr);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                JIRAProject project = JIRAProject.fromJSONObject(obj);
                list.add(project);
            }
        } catch (JSONException e) {
            logger.error("Error when retrieving Projects list", e);
        }
        return list;
    }

    /**
     * Returns ALL issue types for a give project, including sub-tasks & Epic.
     * @param projectKey
     * @return
     */
    public List<JIRAIssueType> getProjectIssueTypes(String projectKey) {

        boolean useNewCreateMetaAPI = canUseNewCreateMetaAPI();

        String url = null;
        if (useNewCreateMetaAPI) {
             url = baseUri + JIRAConstants.CREATEMETA_SUFFIX + "/"+projectKey+"/issuetypes?maxResults=999";
        } else {
            url = baseUri + JIRAConstants.CREATEMETA_SUFFIX + "?projectKeys=" + projectKey;
        }

        if ("*".equals(projectKey) || "".equals(projectKey)) {
            // List of issues types for all projects.
            List<JIRAIssueType> allIssueTypes = getAllNonSubTaskIssueTypes();
            return allIssueTypes;
        }

        ClientResponse response = getWrapper().sendGet(url);

        String jsonStr = response.getEntity(String.class);

        List<JIRAIssueType> jiraIssueTypes = new ArrayList<>();

        Set<String> includedIssueTypeIds = new HashSet<>();

        try {
            JSONObject result = new JSONObject(jsonStr);

            if (useNewCreateMetaAPI) {
                JSONArray issueTypes = result.getJSONArray("values");
                for (int i = 0; i < issueTypes.length(); i++) {
                    JSONObject issueType = issueTypes.getJSONObject(i);
                    if (!issueType.getBoolean("subtask")) {
                        JIRAIssueType jiraIssueType = JIRAIssueType.fromJSONObject(issueType);
                        if (!includedIssueTypeIds.contains(jiraIssueType.getId())) {
                            jiraIssueTypes.add(jiraIssueType);
                            includedIssueTypeIds.add(jiraIssueType.getId());
                        }
                    }
                }
            } else {
                JSONArray projects = result.getJSONArray("projects");
                for (int i = 0; i < projects.length(); i++) {
                    JSONObject projectInfo = projects.getJSONObject(i);
                    JSONArray issueTypes = projectInfo.getJSONArray("issuetypes");
                    for (int j = 0; j < issueTypes.length(); j++) {
                        JSONObject issueType = issueTypes.getJSONObject(j);
                        if (!issueType.getBoolean("subtask")) {
                            JIRAIssueType jiraIssueType = JIRAIssueType.fromJSONObject(issueType);
                            if (!includedIssueTypeIds.contains(jiraIssueType.getId())) {
                                jiraIssueTypes.add(jiraIssueType);
                                includedIssueTypeIds.add(jiraIssueType.getId());
                            }
                        }
                    }
                }
            }
        } catch (JSONException e) {
            logger.error("Error when retrieving Issues Types list for project "+projectKey, e);
        }

        // Let's sort issue types by name.
        Collections.sort(jiraIssueTypes, new Comparator<JIRAIssueType>() {
            @Override
            public int compare(JIRAIssueType o1, JIRAIssueType o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });

        return jiraIssueTypes;
    }

    public JIRAProject getProject(String projectKey) {
        ClientResponse response = getWrapper().sendGet(baseUri + JIRAConstants.PROJECT_SUFFIX + "/" + projectKey);

        String jsonStr = response.getEntity(String.class);
        try {
            JSONObject obj = new JSONObject(jsonStr);
            return JIRAProject.fromJSONObject(obj);
        } catch (JSONException e) {
            logger.error("Error when retrieving info on project " + projectKey, e);
            return null;
        }
    }

    public List<JIRASprint> getAllSprints(String projectKey) {

        // First we get all the Scrum boards for the project
        List<JIRABoard> boards = getAllBoards(projectKey);

        List<JIRASprint> jiraSprints = new ArrayList<JIRASprint>();

        // Then we retrieve Sprints details for each Scrum board.
        for (JIRABoard board : boards) {
            if (!"scrum".equalsIgnoreCase(board.getType())) {
                continue;
            }
            try {
                jiraSprints.addAll(getBoardSprints(board.getId()));
            } catch (RestRequestException e) {
                // JIRA will sometimes throw an Error 500 when retrieving sprints
                // In this case, we'll simply ignore this sprint, come what may.
                logger.error("Error when trying to retrieve JIRA Sprint information for Board ID " + board.getId()
                        + " ('" + board.getName() + "'). Sprints from this board will be ignored.");
            }
        }

        return jiraSprints;
    }

    private List<JIRASprint> getBoardSprints(String boardId) {

        List<JIRASprint> jiraSprints = new ArrayList<JIRASprint>();

        ClientResponse response = getWrapper().sendGet(baseUri + JIRAConstants.BOARD_SUFFIX + "/" + boardId + "/sprint");

        String jsonStr = response.getEntity(String.class);

        try {

            JSONObject obj = new JSONObject(jsonStr);
            JSONArray sprints = obj.getJSONArray("values");

            for (int i = 0; i < sprints.length(); i++) {
                JSONObject jsonSprint = sprints.getJSONObject(i);

                JIRASprint jiraSprint = new JIRASprint();
                jiraSprint.setKey(jsonSprint.getString("id"));
                jiraSprint.setState(jsonSprint.getString("state"));
                jiraSprint.setName(jsonSprint.getString("name"));
                jiraSprint.setStartDate(getStr(jsonSprint, "startDate"));
                jiraSprint.setEndDate(getStr(jsonSprint, "endDate"));
                jiraSprint.setOriginBoardId(boardId);
                jiraSprints.add(jiraSprint);
            }
        } catch (JSONException e) {
            throw new RuntimeException("Error when retrieving sprints info for Board " + boardId, e);
        }

        return jiraSprints;
    }

    /**
     * @return the Issue Key (NOT the ID!)
     */
    public String createIssue(String projectKey, Map<String, String> fields, String issueTypeName, String issueTypeId) {

        String createIssueUri = baseUri + JIRAConstants.JIRA_REST_ISSUE_URL;

        JSONObject createIssuePayload = new JSONObject();

        try {

            JSONObject fieldsObj = new JSONObject();

            JSONObject project = new JSONObject();
            project.put("key", projectKey);

            JSONObject issueType = new JSONObject();
            if (!StringUtils.isBlank(issueTypeName)) {
                issueType.put("name", issueTypeName);
            }
            if (!StringUtils.isBlank(issueTypeId)) {
                issueType.put("id", issueTypeId);
            }

            fieldsObj.put("project", project);
            fieldsObj.put("issuetype", issueType);

            setJiraJSonFields(fieldsObj, fields, projectKey, issueTypeId);

            createIssuePayload.put("fields", fieldsObj);

        } catch (JSONException e) {
            throw new RuntimeException("Error when generating create Issue JSON Payload", e);
        }

        ClientResponse response = getWrapper().sendPost(createIssueUri, createIssuePayload.toString(), 201);
        String jsonStr = response.getEntity(String.class);
        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            return jsonObj.getString("key");
        } catch (Exception e) {
            throw new RuntimeException("Error while parsing Issue creation response from JIRA: " + jsonStr, e);
        }
    }


    public String updateIssue(String projectKey, String issueKey, Map<String, String> fields) {

        String updateIssueUri = baseUri + JIRAConstants.JIRA_REST_ISSUE_URL + issueKey;

        JSONObject updateIssuePayload = new JSONObject();

        JIRAIssue issue = getIssues(projectKey, Arrays.asList(new String[] {issueKey})).get(0);

        try {

            JSONObject fieldsObj = new JSONObject();

            setJiraJSonFields(fieldsObj, fields, projectKey, issue.getTypeId());

            updateIssuePayload.put("fields", fieldsObj);

        } catch (JSONException e) {
            throw new RuntimeException("Error when generating update Issue JSON Payload", e);
        }

        ClientResponse response = getWrapper().sendPut(updateIssueUri, updateIssuePayload.toString(), 204);
        String jsonStr = response.getEntity(String.class);

        return issueKey;
    }

    private void setJiraJSonFields(JSONObject fieldsObj, Map<String, String> fields, String projectKey,
            String issueTypeId) throws JSONException {

        // We need to know the types of the issue fields in order to parse them to number when needed.
        // But that's only valid if we have the issueTypeId, which we won't if creating portfolio Epic - and we don't care.

        Map<String, JIRAFieldInfo> fieldsInfo = new HashMap<>();

        if (!StringUtils.isBlank(issueTypeId)) {
            fieldsInfo = getFields(projectKey, issueTypeId);
        }


        for (Map.Entry<String, String> fieldEntry : fields.entrySet()) {
            String fieldKey = fieldEntry.getKey();
            String value = fieldEntry.getValue();

            JIRAFieldInfo fieldInfo = fieldsInfo.get(fieldKey);

            boolean isNumber = fieldInfo != null && "number".equals(fieldInfo.getType());

            if (value != null && value.startsWith(JIRAConstants.JIRA_NAME_PREFIX)) {
                value = value.substring(JIRAConstants.JIRA_NAME_PREFIX.length());
                JSONObject nameObj = new JSONObject();
                nameObj.put("name", value);
                fieldsObj.put(fieldEntry.getKey(), nameObj);
            } else if((fieldInfo != null && fieldInfo.getType() != null) &&(fieldInfo.getType().equals(JIRAConstants.KEY_FIELD_TYPE_OPTION) || fieldInfo.getType().equals(JIRAConstants.KEY_FIELD_TYPE_PRIORITY))){
            	if(value != null){
            	JSONObject ddlJson = new JSONObject(value);
				fieldsObj.put(fieldEntry.getKey(), ddlJson);
            	} else {
            		fieldsObj.put(fieldEntry.getKey(), JSONObject.NULL);
            	}
            	
			} else if ((fieldInfo != null && fieldInfo.getType() != null) && fieldInfo.getType().equals(JIRAConstants.KEY_FIELD_TYPE_ARRAY)) {
				if (value != null) {
					JSONArray ddlJson = new JSONArray(value);
					fieldsObj.put(fieldEntry.getKey(), ddlJson);
				} else {
					fieldsObj.put(fieldEntry.getKey(), JSONObject.NULL);
				}
			} else if ((fieldInfo != null && fieldInfo.getType() != null) && fieldInfo.getType().equals(JIRAConstants.KEY_FIELD_TYPE_USER)) {
				if (value != null) {
					fieldsObj.put(fieldEntry.getKey(), value);
				} else {
					fieldsObj.put(fieldEntry.getKey(), JSONObject.NULL);
				}
			}
			else {
				if (value == null || (isNumber && StringUtils.isBlank(value))) {
					fieldsObj.put(fieldEntry.getKey(), "");

				} else if (isNumber) {
					fieldsObj.put(fieldEntry.getKey(), Double.parseDouble(value));
				} else {
					fieldsObj.put(fieldEntry.getKey(), value);
				}
			}
        }
    }

    public String createEpic(String projectKey, String epicName, String epicDescription) {
        // Read all custom field info required to create an Epic
        initCustomFieldsInfo();

        Map<String, String> fields = new HashMap<>();

        fields.put("summary", epicName);
        fields.put(getCustomFields().epicNameCustomField, epicName);
        fields.put("description", epicDescription);

        return createIssue(projectKey, fields, "Epic", null);
    }

    /**
     * This method should be called only once on Service instantiation.
     */
    private synchronized void initCustomFieldsInfo() {

        customFields = new CustomFields();

        ClientResponse response = adminWrapper.sendGet(baseUri + JIRAConstants.JIRA_FIELDS_URL);
        try {
            JSONArray fields = new JSONArray(response.getEntity(String.class));
            for (int i = 0; i < fields.length(); i++) {

                JSONObject field = fields.getJSONObject(i);

                String name = field.has("name") ? field.getString("name") : null;

                if (field.has("schema")) {
                    JSONObject schema = field.getJSONObject("schema");

                    if (schema.has("custom")) {
                        String customType = schema.getString("custom");
                        if (JIRAConstants.JIRA_EPIC_NAME_CUSTOM.equalsIgnoreCase(customType)) {
                            customFields.epicNameCustomField = field.getString("id");
                        }
                        if (JIRAConstants.JIRA_EPIC_LINK_CUSTOM.equalsIgnoreCase(customType)) {
                            customFields.epicLinkCustomField = field.getString("id");
                        }
                        if (JIRAConstants.JIRA_SPRINT_CUSTOM.equalsIgnoreCase(customType)) {
                            customFields.sprintIdCustomField = field.getString("id");
                        }

                        if (JIRAConstants.JIRA_STORY_POINTS_CUSTOM_NAME.equalsIgnoreCase(name)) {
                            customFields.storyPointsCustomField = field.getString("id");
                        }
                        if (JIRAConstants.JIRA_PORTFOLIO_PARENT_CUSTOM.equalsIgnoreCase(customType)) {
                            customFields.portfolioParentCustomField = field.getString("id");
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while parsing fields metadata from JIRA", e);
        }

        if (customFields.epicNameCustomField == null) {
            throw new RuntimeException(
                    "We couldn't retrieve the Epic Name mandatory custom field ID from JIRA fields metadata");
        }

        if (customFields.epicLinkCustomField == null) {
            throw new RuntimeException(
                    "We couldn't retrieve the Epic Link mandatory custom field ID from JIRA fields metadata");
        }

        if (customFields.sprintIdCustomField == null) {
            throw new RuntimeException(
                    "We couldn't retrieve the Sprint ID mandatory custom field ID from JIRA fields metadata");
        }

        if (customFields.storyPointsCustomField == null) {
            throw new RuntimeException(
                    "We couldn't retrieve the Story Points mandatory custom field ID from JIRA fields metadata");
        }
    }

    /**
     * Convenience method to {@link #getAllIssues(String, Set)} that takes Strings parameter rather than a Set.
     */
    public List<JIRASubTaskableIssue> getAllIssues(String projectKey, String... issueTypes) {
        Set<String> types = new HashSet<String>();

        for (String issueType : issueTypes) {
            types.add(issueType);
        }
        return getAllIssues(projectKey, types);
    }

    public List<JIRAVersion> getVersions(String projectKey) {
        List<JIRAVersion> list = new ArrayList<JIRAVersion>();
        String query =
                (baseUri + JIRAConstants.VERSIONS_SUFFIX).replace(JIRAConstants.REPLACE_PROJECT_KEY, projectKey);
        ClientResponse response = getWrapper().sendGet(query);
        String jsonStr = response.getEntity(String.class);
        JSONArray array = null;
        try {
            array = new JSONArray(jsonStr);
        } catch (JSONException e) {
            logger.error("", e);
        }

        for (int i = 0; i < array.length(); i++) {
            try {
                JSONObject versionObj = array.getJSONObject(i);

                JIRAVersion version = new JIRAVersion();
                version.setName(getStr(versionObj, "name"));
                version.setId(getStr(versionObj, "id"));
                version.setKey(getStr(versionObj, "id"));
                version.setArchived(versionObj.has("archived") ? versionObj.getBoolean("archived") : false);
                version.setReleased(versionObj.has("released") ? versionObj.getBoolean("released") : false);
                version.setOverdue(versionObj.has("overdue") ? versionObj.getBoolean("overdue") : false);
                version.setDescription(getStr(versionObj, "description"));
                version.setStartDate(getStr(versionObj, "startDate"));
                version.setReleaseDate(getStr(versionObj, "releasedDate"));

                list.add(version);
            } catch (JSONException e) {
                logger.error("", e);
            }
        }

        return list;

    }

    private long getAssigneeUserId(JSONObject fields) throws JSONException {

        JSONObject assignee =
                (fields.has("assignee") && !fields.isNull("assignee")) ? fields.getJSONObject("assignee") : null;
        if (assignee != null && assignee.has("emailAddress")) {
            String assigneeEmail = assignee.getString("emailAddress");
            User ppmUser = getUserProvider().getByEmail(assigneeEmail);
            if (ppmUser != null) {
                return ppmUser.getUserId();
            }
        }

        return -1;
    }

    private List<String> extractFixVersionIds(JSONObject fields) {

        List<String> fixVersionsIds = new ArrayList<String>();

        try {
            JSONArray fixVersions = fields.getJSONArray("fixVersions");
            for (int i = 0; i < fixVersions.length(); i++) {
                JSONObject fixVersion = fixVersions.getJSONObject(i);
                fixVersionsIds.add(fixVersion.getString("id"));
            }
        } catch (JSONException e) {
            return null;
        }

        return fixVersionsIds;
    }

    /**
     * The character '@' is a reserved JQL character. You must enclose it in a string or use the escape '\u0040' instead. (line 1, character 82)"
     *
     * @param value
     * @return encoded value
     */
    private String encodeForJQLQuery(String value) {
        if (value.indexOf('@') > -1) {
            // the value should not contains double quote (")
            value = "%22" + value + "%22";
        }

        return value;
    }

    private JiraIssuesRetrieverUrlBuilder decorateOrderBySprintCreatedUrl(
            JiraIssuesRetrieverUrlBuilder urlBuilder)
    {
        return urlBuilder.setOrderBy(" sprint,created ASC ");
    }

    /**
     * This call gets an issue through /issue/ REST API and not through search.
     */
    public JIRAAgileEntity getSingleAgileEntityIssue(String projectKey, String issueTypeId, String issueKey) {

        JIRAAgileEntity entity = new JIRAAgileEntity();

        entity.setId(issueKey);
        
        Map<String, JIRAFieldInfo> fieldsInfo = new HashMap<>();

        if (!StringUtils.isBlank(issueTypeId)) {
            fieldsInfo = getFields(projectKey, issueTypeId);
        }

        ClientResponse response =
                getWrapper().sendGet(baseUri + JIRAConstants.JIRA_REST_ISSUE_URL + issueKey);

        String jsonStr = response.getEntity(String.class);

        try {
            JSONObject issueObj = new JSONObject(jsonStr);
            return AgileEntityUtils.getAgileEntityFromIssueJSon(fieldsInfo, issueObj, getBaseUrl());

        } catch (JSONException e) {
            throw new RuntimeException("Error when retrieving issue information for issue "+issueKey, e);
        }
    }

    public List<JIRAAgileEntity> getAgileEntityIssuesModifiedSince( Map<String, JIRAFieldInfo> fieldsInfo, Set<String> entityIds, Date modifiedSinceDate) {

        JiraIssuesRetrieverUrlBuilder searchUrlBuilder =
                new JiraIssuesRetrieverUrlBuilder(baseUri).retrieveAllFields();

        searchUrlBuilder.addAndConstraint("key in ("+StringUtils.join(entityIds, ",")+")");
        searchUrlBuilder.addAndConstraint("updated>='"+new SimpleDateFormat("yyyy-MM-dd HH:mm").format(modifiedSinceDate)+"'");

        return retrieveAgileEntities(fieldsInfo, searchUrlBuilder);
    }

    public List<AgileEntityIdProjectDate> getAgileEntityIdsCreatedSince(String agileProjectValue, String entityType, Date createdSinceDate) {

        JiraIssuesRetrieverUrlBuilder searchUrlBuilder =
                new JiraIssuesRetrieverUrlBuilder(baseUri).retrieveOnlyFields("key", "issuetype", "created", "summary");

        if (!StringUtils.isBlank(agileProjectValue)) {
            searchUrlBuilder.setProjectKey(agileProjectValue);
        }

        if (!StringUtils.isBlank(entityType)) {
            searchUrlBuilder.setStandardIssueTypes(entityType);
        }

        if (createdSinceDate != null) {
            searchUrlBuilder.addAndConstraint("created>='" + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(createdSinceDate) + "'");
        }

        IssueRetrievalResult result =
                runIssueRetrievalRequest(decorateOrderBySprintCreatedUrl(searchUrlBuilder).toUrlString());

        List<AgileEntityIdProjectDate> results = new ArrayList<>(result.getIssues().size());

        for (JSONObject obj : result.getIssues()) {
            JIRAIssue issue = getIssueFromJSONObj(obj);
            AgileEntityIdProjectDate idProjectDate = new AgileEntityIdProjectDate(issue.getKey(), issue.getProjectKey(), issue.getCreationDateAsDate());
            results.add(idProjectDate);
        }

        return results;
    }

    /**
     * We use the search issue API instead of the /rest/issue/{key} because we already have all
     * the logic to get the right columns and build the right JIRAIssue in the search API.
     *
     * This method can retrieve sub-tasks issues too ; it will not retrieve sub-tasks of returned issues automatically.
     */
    public List<JIRAIssue> getIssues(String projectKey, Collection<String> issueKeys) {

        if (issueKeys == null || issueKeys.isEmpty()) {
            return new ArrayList<>();
        }

        JiraIssuesRetrieverUrlBuilder searchUrlBuilder =
                new JiraIssuesRetrieverUrlBuilder(baseUri).setProjectKey(projectKey).setExpandLevel("schema")
                        .addAndConstraint("key in(" + StringUtils.join(issueKeys, ',')+")")
                        .addExtraFields(getCustomFields().getJiraCustomFields());

        IssueRetrievalResult result =
                runIssueRetrievalRequest(decorateOrderBySprintCreatedUrl(searchUrlBuilder).toUrlString());

        List<JIRAIssue> issues = new ArrayList<>();

        for (JSONObject obj : result.getIssues()) {
            issues.add(getIssueFromJSONObj(obj));
        }

        return issues;
    }

    /**
     * Gets a single issue, without retrieving the sub-tasks (unless it's a subtask).
     */
    public JIRAIssue getSingleIssue(String issueKey) {

        if (StringUtils.isBlank(issueKey)) {
            return null;
        }

        JiraIssuesRetrieverUrlBuilder searchUrlBuilder =
                new JiraIssuesRetrieverUrlBuilder(baseUri).setExpandLevel("schema")
                        .addAndConstraint("key=" + issueKey)
                        .addExtraFields(getCustomFields().getJiraCustomFields());

        IssueRetrievalResult result =
                runIssueRetrievalRequest(searchUrlBuilder.toUrlString());

        for (JSONObject obj : result.getIssues()) {
            return getIssueFromJSONObj(obj);
        }

        return null;
    }

    public List<JIRABoard> getAllBoards(String projectKey) {
        ClientResponse response =
                getWrapper().sendGet(baseUri + JIRAConstants.BOARD_SUFFIX + "?projectKeyOrId=" + projectKey);

        List<JIRABoard> boards = new ArrayList<JIRABoard>();

        String jsonStr = response.getEntity(String.class);
        try {
            JSONObject obj = new JSONObject(jsonStr);
            JSONArray boardsArray = obj.getJSONArray("values");

            for (int i = 0; i < boardsArray.length(); i++) {
                JSONObject jsonBoard = boardsArray.getJSONObject(i);

                JIRABoard board = new JIRABoard();
                board.setId(jsonBoard.getString("id"));
                board.setKey(board.getId());
                board.setType(getStr(jsonBoard, "type"));
                board.setName(jsonBoard.getString("name"));
                boards.add(board);
            }
        } catch (JSONException e) {
            throw new RuntimeException("Ërror when retrieving all Boards information", e);
        }

        return boards;
    }

    /**
     * Safe method that does all the null checks
     */
    private String getStr(JSONObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.isNull(key)) {
            return null;
        }

        try {
            return obj.getString(key);
        } catch (JSONException e) {
            throw new RuntimeException("Error when retrieving key " + key + " from JSon object " + obj.toString());
        }
    }

    /**
     * This method returns all requested issue types, with sub-tasks always included and returned as part of their parent and never as standalone issues.
     * <br/>Each Epic will have its content available directly as {@link JIRAEpic#getContents()}, but only for the issues types that were requested.
     */
    public List<JIRASubTaskableIssue> getAllIssues(String projectKey, Set<String> issueTypes) {

        if (issueTypes == null || issueTypes.isEmpty()){
            return new ArrayList<JIRASubTaskableIssue>();
        }
        
        JiraIssuesRetrieverUrlBuilder searchUrlBuilder =
                new JiraIssuesRetrieverUrlBuilder(baseUri).setProjectKey(projectKey)
                        .addExtraFields(getCustomFields().getJiraCustomFields()).setIssuesTypes(issueTypes);

        return retrieveIssues(searchUrlBuilder, true);
    }

    /**
     * This method returns all requested issues, with all sub-tasks included and returned as part of their parent and never as standalone issues.
     * <br/>Each Epic will have its content available directly as {@link JIRAEpic#getContents()}, but only for the issues keys that were requested.
     * Note that we don't care about the Project the issues are in.
     */
    public List<JIRASubTaskableIssue> getAllIssuesByKeys(Set<String> issueKeys) {

        JiraIssuesRetrieverUrlBuilder searchUrlBuilder =
                new JiraIssuesRetrieverUrlBuilder(baseUri)
                        .addExtraFields(getCustomFields().getJiraCustomFields())
                        .addAndConstraint("key in(" + StringUtils.join(issueKeys, ',')+")")
                            // We also want sub-tasks of the requested tasks.
                        .addOrConstraint("parent in (" + StringUtils.join(issueKeys, ',')+")");

        return retrieveIssues(searchUrlBuilder, true);
    }

    private List<JIRAAgileEntity> retrieveAgileEntities(Map<String, JIRAFieldInfo> fieldsInfo, JiraIssuesRetrieverUrlBuilder searchUrlBuilder) {

        IssueRetrievalResult result = null;
        int fetchedResults = 0;
        searchUrlBuilder.setStartAt(0);

        List<JIRAAgileEntity> allIssues = new ArrayList<JIRAAgileEntity>();

        do {
            result = runIssueRetrievalRequest(searchUrlBuilder.toUrlString());
            for (JSONObject obj : result.getIssues()) {
                allIssues.add(AgileEntityUtils.getAgileEntityFromIssueJSon(fieldsInfo, obj, getBaseUrl()));
            }

            fetchedResults += result.getMaxResults();
            searchUrlBuilder.setStartAt(fetchedResults);

        } while (fetchedResults < result.getTotal());

        return allIssues;
    }


    private List<JIRAIssue> fetchResults(JiraIssuesRetrieverUrlBuilder searchUrlBuilder) {
        IssueRetrievalResult result = null;
        int fetchedResults = 0;
        searchUrlBuilder.setStartAt(0);

        List<JIRAIssue> issues = new ArrayList<JIRAIssue>();

        do {
            result = runIssueRetrievalRequest(searchUrlBuilder.toUrlString());
            for (JSONObject issueObj : result.getIssues()) {
                issues.add(getIssueFromJSONObj(issueObj));
            }
            fetchedResults += result.getMaxResults();
            searchUrlBuilder.setStartAt(fetchedResults);

        } while (fetchedResults < result.getTotal());

        return issues;
    }

    /**
     * Retrieve the issues using the passed JiraIssuesRetrieverUrlBuilder, and if needed, retrieve the subTasks in a different call and adds them to issues.
     */
    private List<JIRASubTaskableIssue> retrieveIssues(JiraIssuesRetrieverUrlBuilder searchUrlBuilder, boolean retrieveSubTasks) {

        List<JIRAIssue> allIssues = fetchResults(searchUrlBuilder);

        Map<String, JIRASubTaskableIssue> indexedIssues = new HashMap<>();

        for (JIRAIssue issue : allIssues) {
            indexedIssues.put(issue.getKey(), (JIRASubTaskableIssue)issue);
        }

        List<JIRAIssue> subTasks = new ArrayList<JIRAIssue>();

        // We retrieve all sub-tasks in different calls, in batches of 250 parent issues.
        if (retrieveSubTasks) {
            List<String> issuesKeys = new ArrayList<String>(indexedIssues.keySet());
            for (int i = 0; i < issuesKeys.size(); i += SUB_TASK_BATCH_SIZE) {
                List<String> selectedIssuesKeys = issuesKeys.subList(i,
                        i + SUB_TASK_BATCH_SIZE > issuesKeys.size() ? issuesKeys.size() : i + SUB_TASK_BATCH_SIZE);
                JiraIssuesRetrieverUrlBuilder subTasksSearchUrlBuilder = new JiraIssuesRetrieverUrlBuilder(baseUri).addExtraFields(getCustomFields().getJiraCustomFields());
                subTasksSearchUrlBuilder
                        .addAndConstraint("parent in (" + StringUtils.join(selectedIssuesKeys, ",") + ")");

                subTasks.addAll(fetchResults(subTasksSearchUrlBuilder));
            }
        }

        List<JIRASubTaskableIssue> processedIssues = new ArrayList<JIRASubTaskableIssue>();

        Map<String, JIRASubTaskableIssue> subTaskableIssues = new HashMap<String, JIRASubTaskableIssue>();

        // Read all Epics
        Map<String, JIRAEpic> epics = new HashMap<String, JIRAEpic>();
        for (JIRAIssue issue : allIssues) {
            if (JIRAConstants.JIRA_ISSUE_EPIC.equalsIgnoreCase(issue.getType())) {
                processedIssues.add((JIRASubTaskableIssue)issue);
                epics.put(issue.getKey(), (JIRAEpic)issue);
                subTaskableIssues.put(issue.getKey(), (JIRAEpic)issue);
            }
        }

        // Read all non-Epic standard issue types, add them to Epic
        for (JIRAIssue issue : allIssues) {
            if (!JIRAConstants.JIRA_ISSUE_EPIC.equalsIgnoreCase(issue.getType())
                    && issue instanceof JIRASubTaskableIssue) {

                processedIssues.add((JIRASubTaskableIssue)issue);
                subTaskableIssues.put(issue.getKey(), (JIRASubTaskableIssue)issue);

                if (issue.getEpicKey() != null) {
                    JIRAEpic epic = epics.get(issue.getEpicKey());
                    if (epic != null) {
                        epic.addContent((JIRASubTaskableIssue)issue);
                    }
                }
            }
        }

        // Read all sub-tasks, add them to parent
        for (JIRAIssue issue : subTasks) {
            JIRASubTask subTask = (JIRASubTask)issue;
            if (subTask.getParentKey() != null) {
                JIRASubTaskableIssue parent = subTaskableIssues.get(subTask.getParentKey());
                if (parent != null) {
                    parent.addSubTask(subTask);
                }
            }
        }

        return processedIssues;
    }


    /**
     * Retrieve the Timesheet workunit issues. These can include both tasks and sub-tasks, and we must only return SubTaskableTasks.
     * That means we must retrieve parent tasks of sub-tasks that didn't get their parent.
     *
     */
    private Collection<JIRASubTaskableIssue> retrieveTSIssues(JiraIssuesRetrieverUrlBuilder searchUrlBuilder, boolean retrieveSubTasks) {

        List<JIRAIssue> allIssues = fetchResults(searchUrlBuilder);

        Map<String, JIRASubTaskableIssue> indexedIssues = new HashMap<>();

        Map<String, JIRASubTask> subTasks = new HashMap<>();

        for (JIRAIssue issue : allIssues) {
            if (issue instanceof JIRASubTaskableIssue) {
                indexedIssues.put(issue.getKey(), (JIRASubTaskableIssue)issue);
            } else {
                // It's a SubTask.
                JIRASubTask subTask = (JIRASubTask)issue;
                subTasks.put(subTask.getKey(), subTask);
            }
        }

        // We now add SubTasks to their parent or pull their parent info if not already retrieved
        for (String subTaskKey : subTasks.keySet()) {
            JIRASubTask subTask = subTasks.get(subTaskKey);
            JIRASubTaskableIssue parent = indexedIssues.get(subTask.getParentKey());
            if (parent == null) {
                // We must retrieve the parent.
                parent = (JIRASubTaskableIssue)getSingleIssue(subTask.getParentKey());
                if (parent == null) {
                    throw new RuntimeException("Cannot retrieve issue with Key "+subTask.getParentKey());
                }
                indexedIssues.put(parent.getKey(), parent);
            }

            parent.addSubTask(subTask);
        }

        return indexedIssues.values();
    }


    private IssueRetrievalResult runIssueRetrievalRequest(String urlString) {

        ClientResponse response = getWrapper().sendGet(urlString);

        String jsonStr = response.getEntity(String.class);

        try {
            JSONObject resultObj = new JSONObject(jsonStr);

            IssueRetrievalResult result =
                    new IssueRetrievalResult(resultObj.getInt("startAt"), resultObj.getInt("maxResults"),
                            resultObj.getInt("total"));

            JSONArray issues = resultObj.getJSONArray("issues");

            for (int i = 0; i < issues.length(); i++) {
                JSONObject issueObj = issues.getJSONObject(i);

                result.addIssue(issueObj);
            }

            return result;
        } catch (JSONException e) {
            throw new RuntimeException("Problem occurred when retrieving Issue Search results", e);
        }
    }

    private JIRAIssue getIssueFromJSONObj(JSONObject obj) {
        try {
            JSONObject fields = obj.getJSONObject("fields");

            String issueType = fields.getJSONObject("issuetype").getString("name");

            JIRAIssue issue = null;

            if (issueType.equalsIgnoreCase(JIRAConstants.JIRA_ISSUE_EPIC)) {
                issue = new JIRAEpic();
            } else if (getSubTasksIssueTypeNames().contains(issueType)) { // This is the only place where calling getSubTasksIssueTypes() is appropriate.
                issue = new JIRASubTask();

                if (fields.has("parent")) {
                    ((JIRASubTask)issue).setParentKey(fields.getJSONObject("parent").getString("key"));
                }
            } else {
                issue = new JIRAStandardIssue();
            }

            // Common fields for all issues
            issue.setType(issueType);

            issue.setTypeId(fields.getJSONObject("issuetype").getString("id"));

            if (fields.has("status") && !fields.isNull("status") && fields.getJSONObject("status").has("name")) {
                issue.setStatus(fields.getJSONObject("status").getString("name"));
            }
            if (fields.has("creator") && !fields.isNull("creator") && fields.getJSONObject("creator").has("emailAddress")) {
                issue.setAuthorName(fields.getJSONObject("creator").getString("emailAddress"));
            }
            if (fields.has("priority") && !fields.isNull("priority") && fields.getJSONObject("priority").has("name")) {
                issue.setPriorityName(fields.getJSONObject("priority").getString("name"));
            }
            if (getCustomFields().sprintIdCustomField != null && fields.has(getCustomFields().sprintIdCustomField) && !fields.isNull(getCustomFields().sprintIdCustomField)) {
                String sprintId = getSprintIdFromSprintCustomfield(fields.getJSONArray(getCustomFields().sprintIdCustomField));
                issue.setSprintId(sprintId);
            }

            issue.setKey(obj.getString("key"));
            issue.setName(fields.getString("summary"));
            if (issue instanceof JIRAEpic && getCustomFields().epicNameCustomField != null && fields.has(getCustomFields().epicNameCustomField)) {
                // JIRA stores Epic name in a custom field.
                issue.setName(fields.getString(getCustomFields().epicNameCustomField));
            }
            issue.setAssigneePpmUserId(getAssigneeUserId(fields));
            issue.setAuthorName(
                    (fields.has("creator") && !fields.isNull("creator") && fields.getJSONObject("creator")
                            .has("displayName")) ? fields.getJSONObject("creator").getString("displayName") : null);
            issue.setCreationDate(fields.has("created") ? fields.getString("created") : "");
            issue.setLastUpdateDate(fields.has("updated") ? fields.getString("updated") : "");
            issue.setResolutionDate(fields.has(JIRAConstants.JIRA_FIELD_RESOLUTION_DATE) ? fields.getString(JIRAConstants.JIRA_FIELD_RESOLUTION_DATE) : "");
            if (fields.has(getCustomFields().epicLinkCustomField)) {
                issue.setEpicKey(fields.getString(getCustomFields().epicLinkCustomField));
            }

            if (getCustomFields().portfolioParentCustomField != null && fields.has(getCustomFields().portfolioParentCustomField)) {
                issue.setPortfolioParentKey(fields.getString(getCustomFields().portfolioParentCustomField));
            }

            issue.setStoryPoints(getCustomFields().storyPointsCustomField != null && (fields.has(getCustomFields().storyPointsCustomField) && !fields.isNull(getCustomFields().storyPointsCustomField)) ?
                    new Double(fields.getDouble(getCustomFields().storyPointsCustomField)).longValue() : null);

            issue.setFixVersionIds(extractFixVersionIds(fields));

            // Estimates info
            if (fields.has("timetracking") && !fields.isNull("timetracking")) {
                JSONObject timeTracking = fields.getJSONObject("timetracking");
                if (timeTracking.has("remainingEstimateSeconds")) {
                    issue.getWork().setRemainingEstimateHours(
                            JIRAIssueWork.secToHours(timeTracking.getInt("remainingEstimateSeconds")));
                }
                if (timeTracking.has("timeSpentSeconds")) {
                    issue.getWork()
                            .setTimeSpentHours(JIRAIssueWork.secToHours(timeTracking.getInt("timeSpentSeconds")));
                }
            }

            // Worklog info
            if (fields.has("worklog") && !fields.isNull("worklog")) {

                JSONArray worklogs = null;

                JSONObject worklog = fields.getJSONObject("worklog");

                if (worklog.getInt("total") > worklog.getInt("maxResults")) {
                    // If we hit the max number of worklogs per issue detail (20), we'll issue a different call to get all the worklog entries.
                    // So far, JIRA doesn't allow to return all worklogs entries in an Issue detail call.
                    worklogs = getWorklogsJSONArrayForIssue(issue.getKey());
                } else {
                    worklogs = worklog.getJSONArray("worklogs");
                }

                if (worklogs != null) {
                    for (int i = 0; i < worklogs.length(); i++) {
                        JSONObject worklogEntry = worklogs.getJSONObject(i);
                        issue.getWork()
                                .addWorklogEntry(JIRAIssueWork.getWorklogEntryFromWorklogJSONObject(worklogEntry));
                    }
                }
            }

            return issue;
        } catch (JSONException e) {
            throw new RuntimeException("Error when reading info from issue", e);
        }
    }

    /**
     * Retrieves Sprint ID from Sprint custom field.
     * The example of origin format of sprintCustomfield is
     * "com.atlassian.greenhopper.service.sprint.Sprint@1f39706[id=1,rapidViewId=1,state=ACTIVE,name=SampleSprint
     * 2,goal=<null>,startDate=2016-12-07T06:18:24.224+08:00,endDate=2016-12-21T06:38:24.224+08:00,completeDate=<null>,sequence=1]"
     *
     * @param sprintCustomfields
     */
    private String getSprintIdFromSprintCustomfield(JSONArray sprintCustomfields) throws JSONException {

        if (sprintCustomfields == null || sprintCustomfields.length() == 0) {
            return null;
        }

        String sprintId = null;

        for (int i = 0; i < sprintCustomfields.length(); i++) {
            String id = null;
            boolean isActiveOrFutureSprint = false;

            Map<String, String> map = new HashMap<String, String>();
            String reg = ".+@.+\\[(.+)]";
            Pattern pattern = Pattern.compile(reg);
            Matcher matcher = pattern.matcher(sprintCustomfields.getString(i));
            if (matcher.find()) {
                String exp = matcher.group(1);
                String[] kvs = exp.split(",");
                for (String kv : kvs) {
                    String[] splited = kv.split("=");
                    if ("id".equalsIgnoreCase(splited[0])) {
                        if (splited.length == 2) {
                            id = splited[1];
                        }
                    }

                    if ("state".equalsIgnoreCase(splited[0])) {
                        if (splited.length == 2) {
                            isActiveOrFutureSprint =
                                    "ACTIVE".equalsIgnoreCase(splited[1]) || "FUTURE".equalsIgnoreCase(splited[1]);
                        }
                    }
                }
            }

            // We only consider this the sprint ID if either it's an active or future sprint or it's the only (possibly completed) sprint.
            if (sprintId == null || isActiveOrFutureSprint) {
                sprintId = id;
            }
        }

        return sprintId;
    }

    private JSONArray getWorklogsJSONArrayForIssue(String issueKey) {

        ClientResponse response =
                getWrapper().sendGet(baseUri + JIRAConstants.JIRA_GET_ISSUE_WORKLOG.replace("%issue%", issueKey));

        String jsonStr = response.getEntity(String.class);
        try {
            JSONObject obj = new JSONObject(jsonStr);
            JSONArray worklogsArray = obj.getJSONArray("worklogs");
            return worklogsArray;

        } catch (JSONException e) {
            throw new RuntimeException("Ërror when retrieving all worklogs information for issue " + issueKey, e);
        }
    }

    public List<JIRASubTaskableIssue> getBoardIssues(String projectKey, Set<String> issueTypes, String boardId) {
        JiraIssuesRetrieverUrlBuilder boardIssuesUrlBuilder =
                new JiraIssuesRetrieverUrlBuilder(baseUri).setProjectKey(projectKey)
                        .addExtraFields(getCustomFields().getJiraCustomFields()).setBoardType(boardId).setIssuesTypes(issueTypes);

        return retrieveIssues(boardIssuesUrlBuilder, true);
    }

    /**
     * @return all the issues that are part of an Epic. Sub-tasks are included in returned tasks, but not as tasks of the list themselves.
     */
    public List<JIRASubTaskableIssue> getAllEpicIssues(String projectKey, Set<String> issueTypes, String epicKey) {

        JiraIssuesRetrieverUrlBuilder searchUrlBuilder =
                new JiraIssuesRetrieverUrlBuilder(baseUri).setIssuesTypes(issueTypes).setProjectKey(projectKey)
                        .addExtraFields(getCustomFields().getJiraCustomFields());

        // Retrieving only issues belonging to that epic
        searchUrlBuilder.addCustomFieldEqualsConstraint(getCustomFields().epicLinkCustomField, epicKey);

        // We also want to retrieve the Epic itself
        searchUrlBuilder.addOrConstraint("key=" + epicKey);

        return retrieveIssues(searchUrlBuilder, true);
    }

    public List<JIRASubTaskableIssue> getVersionIssues(String projectKey, Set<String> issueTypes, String versionId)
    {
        JiraIssuesRetrieverUrlBuilder versionIssuesUrlBuilder =
                new JiraIssuesRetrieverUrlBuilder(baseUri).setProjectKey(projectKey)
                        .addExtraFields(getCustomFields().getJiraCustomFields()).setIssuesTypes(issueTypes)
                        .addAndConstraint("fixVersion=" + versionId);

        return retrieveIssues(versionIssuesUrlBuilder, true);
    }

    /**
     * @return Timesheet data for issues owned by the passed author and completed within the passed dates as timesheet lines.
     */
    public JIRATimesheetData getSPTimesheetData(final XMLGregorianCalendar dateFrom,
            final XMLGregorianCalendar dateTo, String projectKey, String author, double spToHoursRatio,
            Date[] tsDays, boolean[] tsWorkDays)
    {
        JIRATimesheetData timesheetData = new JIRATimesheetData();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        // By default only Stories & Epics have story points in JIRA, but we'll just filter to keep only issues that have SP defined anyway.
        JiraIssuesRetrieverUrlBuilder spTimesheetUrlBuilder = new JiraIssuesRetrieverUrlBuilder(baseUri)
                .addExtraFields(JIRAConstants.JIRA_FIELD_RESOLUTION_DATE).addExtraFields(getCustomFields().getJiraCustomFields())
                // We want story points to be defined.
                .addNonNullCustomField(getCustomFields().storyPointsCustomField)
                // We only want to get issues that were assigned to the user at some point during timesheet period
                .addAndConstraint("assignee was '"+encodeForJQLQuery(author)+"' during ('"+dateFrom.toString().substring(0, 10) + "','"+ dateTo.toString().substring(0, 10) + "')")
                // We only care about Done and in Progress issues.
                .addAndConstraint("status != 'To Do'");


        // We also don't want to retrieve issues that have been last updated over 1 month before timesheet period, that should remove all issues that have been closed a long time ago.
        Calendar oneMonthBeforeTS = dateFrom.toGregorianCalendar();
        oneMonthBeforeTS.add(Calendar.MONTH, -1);
        String modifiedAfterDate = sdf.format(oneMonthBeforeTS.getTime());
        spTimesheetUrlBuilder.addAndConstraint("updated>"+modifiedAfterDate);

        if (!isBlank(projectKey)) {
            spTimesheetUrlBuilder.setProjectKey(projectKey);
        }

        List<JIRASubTaskableIssue> issues = retrieveIssues(spTimesheetUrlBuilder, false);

        Date fromDate = dateFrom.toGregorianCalendar().getTime();
        Date toDate = dateTo.toGregorianCalendar().getTime();

        for (JIRASubTaskableIssue issue : issues) {

            Long sp = issue.getStoryPoints();
            if (sp == null || sp <= 0) {
                // No SP = no effort
                continue;
            }
            double issueEffort = sp * spToHoursRatio;

            // We check if
            // - issue resolution date is within timesheet period.
            // - status is Done (resolution date is defined).
            Date resolutionDate = issue.getResolutionDateAsDate();
            if (resolutionDate == null) {
                // Issue is not Done.
                continue;
            }
            if (resolutionDate.after(toDate) || resolutionDate.before(fromDate)) {
                // Issue was closed outside of timesheet window
                continue;
            }

            // We distribute issue effort on the (working) days between start of timesheet and completion period

            // We first compute the number of working between which to distribute the effort.
            Date firstWorkingDay = tsDays[tsDays.length-1];
            int totalWorkDaysOfEffort = 0;
            for (int i = 0 ; i < tsDays.length ; i++) {

                Date day = tsDays[i];

                if (tsWorkDays[i]) {
                    // Working day
                    if (day.before(firstWorkingDay)) {
                        firstWorkingDay = day;
                    }
                    if (!day.after(resolutionDate)) {
                        ++totalWorkDaysOfEffort;
                    }
                }
            }

            if (totalWorkDaysOfEffort == 0) {
                // there are no working days within scope, so we put all effort in the first working day, even if it's the last non-working day of the timesheet.
                timesheetData.addIssueEffort(issue, sdf.format(firstWorkingDay), issueEffort);
            } else {
                double dailyEffort = issueEffort / totalWorkDaysOfEffort;

                // We distribute daily effort for each working day.
                for (int i = 0 ; i < tsDays.length ; i++) {
                    Date day = tsDays[i];

                    if (tsWorkDays[i]) {
                        if (day.after(resolutionDate)) {
                            break;
                        } else {
                            timesheetData.addIssueEffort(issue, sdf.format(day), dailyEffort);
                        }
                    }
                }
            }
        }

        return timesheetData;
    }

    /**
     * @return all the work items for worklogs logged by the passed author within the passed dates as timesheet lines.
     */
    public JIRATimesheetData getWorkLogsTimesheetData(final XMLGregorianCalendar dateFrom,
            final XMLGregorianCalendar dateTo, String projectKey, String author)
    {
        JiraIssuesRetrieverUrlBuilder worklogUrlBuilder = new JiraIssuesRetrieverUrlBuilder(baseUri)
                .addExtraFields(getCustomFields().getJiraCustomFields())
                .addAndConstraint("worklogDate>=" + dateFrom.toString().substring(0, 10))
                .addAndConstraint("worklogDate<=" + dateTo.toString().substring(0, 10))
                .addAndConstraint("worklogAuthor=" + encodeForJQLQuery(author));

        if (!isBlank(projectKey)) {
            worklogUrlBuilder.setProjectKey(projectKey);
        }

        Collection<JIRASubTaskableIssue> issues = retrieveTSIssues(worklogUrlBuilder, false);

        JIRATimesheetData timesheetData = new JIRATimesheetData();

        Date fromDate = dateFrom.toGregorianCalendar().getTime();
        Date toDate = dateTo.toGregorianCalendar().getTime();

        for (final JIRASubTaskableIssue issue : issues) {

            final List<JIRAIssueWork.JIRAWorklogEntry> worklogs = new ArrayList<JIRAIssueWork.JIRAWorklogEntry>();

            if (issue.getWork() != null) {
                worklogs.addAll(pickWorklogs(issue.getWork().getWorklogs(), fromDate, toDate, author));
            }

            if (issue.getSubTasks() != null) {
                for (JIRASubTask subTask : issue.getSubTasks()) {
                    if (subTask.getWork() != null) {
                        worklogs.addAll(pickWorklogs(subTask.getWork().getWorklogs(), fromDate, toDate, author));
                    }
                }
            }

            if (!worklogs.isEmpty()) {

                for (JIRAIssueWork.JIRAWorklogEntry worklog : worklogs) {

                    timesheetData.addIssueEffort(issue, worklog.getDateStartedAsSimpleDate(),
                            Math.round(worklog.getTimeSpentHours() * 100) / 100d);
                }

                insertZeroEffortDatesInBreakdown(timesheetData, issue, dateFrom.toGregorianCalendar(),
                        dateTo.toGregorianCalendar());
            }
        }

        return timesheetData;
    }

    // We must have some time info defined for every date of the timesheet, otherwise the timesheet UI in PPM will bug.
    // This was fixed in PPM 9.42 as it will fill the gaps for you, but we want this connector to also work on PPM 9.41.
    private void insertZeroEffortDatesInBreakdown(JIRATimesheetData data, JIRAIssue issue, Calendar startDate,
            Calendar endDate)
    {

        do {
            data.addIssueEffort(issue, issue.convertDateToString(startDate.getTime()), 0d);
            startDate.add(Calendar.DATE, 1);
        } while (!startDate.after(endDate));

    }

    /**
     * Returns true if str is null, "", some spaces, or the string "null".
     *
     * @param str
     * @return
     */
    private boolean isBlank(String str) {
        return StringUtils.isBlank(str) || "null".equalsIgnoreCase(str);
    }

    /**
     * @return the worklogs that match the constraints of date & author.
     */
    private List<JIRAIssueWork.JIRAWorklogEntry> pickWorklogs(List<JIRAIssueWork.JIRAWorklogEntry> worklogs,
            Date fromDate, Date toDate, String author)
    {
        List<JIRAIssueWork.JIRAWorklogEntry> validWorklogs = new ArrayList<JIRAIssueWork.JIRAWorklogEntry>();

        if (worklogs == null || isBlank(author)) {
            return validWorklogs;
        }

        for (JIRAIssueWork.JIRAWorklogEntry worklog : worklogs) {
            if (worklog == null) {
                continue;
            }

            if (author.equalsIgnoreCase(worklog.getAuthorEmail()) || author
                    .equalsIgnoreCase(worklog.getAuthorKey()) || author.equalsIgnoreCase(worklog.getAuthorName())) {
                Date logDate = worklog.getDateStartedAsDate();
                if ((fromDate.before(logDate) && toDate.after(logDate)) || fromDate.equals(logDate) || toDate
                        .equals(logDate)) {
                    validWorklogs.add(worklog);
                }
            }
        }

        return validWorklogs;
    }

    public Map<String, JIRAFieldInfo> getFields(String projectKey, String issuetypeId) {

        boolean useNewRESTAPI = canUseNewCreateMetaAPI();
        boolean useFieldRESTAPI = false;
        String url = null;
        if ("*".equals(projectKey)) {
            if (useNewRESTAPI) {
                // This URL will not list allowedValues for array types,
                // but using CREATEMETA on new REST APIs requires to pass a project key which we don't have here.
                url = baseUri + JIRAConstants.JIRA_FIELDS_URL;
                useFieldRESTAPI = true;
            } else {
                url = baseUri + JIRAConstants.CREATEMETA_SUFFIX + "?issuetypeIds=" + issuetypeId + "&expand=projects.issuetypes.fields";
            }
        } else {
            if (canUseNewCreateMetaAPI()) {
                url = baseUri + JIRAConstants.CREATEMETA_SUFFIX + "/" + projectKey + "/issuetypes/" + issuetypeId + "?expand=projects.issuetypes.fields&maxResults=999";
            } else {
                url = baseUri + JIRAConstants.CREATEMETA_SUFFIX + "?projectKeys=" + projectKey + "&issuetypeIds=" + issuetypeId + "&expand=projects.issuetypes.fields";
            }
        }

        ClientResponse response = getWrapper().sendGet(url);

        String jsonStr = response.getEntity(String.class);

        Map<String, JIRAFieldInfo> jiraFieldsInfo = new HashMap<>();
        try {
            if (useFieldRESTAPI) {
                // When using Jira Server 9+, if we don't have a project Key we need to get fields from /field REST API.
                // This means we won't get the list of allowed values.
                JSONArray fields = new JSONArray(jsonStr);
                for (int i = 0; i < fields.length(); i++) {

                    JSONObject field = fields.getJSONObject(i);
                    try {
                        JIRAFieldInfo fieldInfo = JIRAFieldInfo.fromJSONFieldObject(field);
                        jiraFieldsInfo.put(fieldInfo.getKey(), fieldInfo);
                    } catch (Exception e) {
                        logger.error("Couldn't read fieldInfo information for the following Field JSON, Skipping field:" + field.toString());
                        continue;
                    }
                }
            } else if (useNewRESTAPI) {
                JSONObject result = new JSONObject(jsonStr);
                JSONArray values = result.getJSONArray("values");
                for (int i = 0; i < values.length(); i++) {

                    JSONObject field = values.getJSONObject(i);
                    try {
                        String fieldKey = field.getString("fieldId");
                        JIRAFieldInfo fieldInfo = JIRAFieldInfo.fromJSONObject(field, fieldKey);
                        jiraFieldsInfo.put(fieldInfo.getKey(), fieldInfo);
                    } catch (Exception e) {
                        logger.error("Couldn't read fieldInfo information for the following Field JSON, Skipping field:" + field.toString());
                        continue;
                    }
                }
            } else {
                JSONObject result = new JSONObject(jsonStr);
                JSONArray projects = result.getJSONArray("projects");

                for (int i = 0; i < projects.length(); i++) {

                    JSONObject projectInfo = projects.getJSONObject(i);

                    JSONArray issueTypes = projectInfo.getJSONArray("issuetypes");

                    if (issueTypes != null && issueTypes.length() > 0) {

                        // The issue type is always uniquely filtered so there should be only one record at most
                        JSONObject fields = issueTypes.getJSONObject(0).getJSONObject("fields");

                        for (String fieldKey : JSONObject.getNames(fields)) {
                            if (!jiraFieldsInfo.containsKey(fieldKey)) {
                                JSONObject field = fields.getJSONObject(fieldKey);
                                try {
                                    JIRAFieldInfo fieldInfo = JIRAFieldInfo.fromJSONObject(field, fieldKey);
                                    jiraFieldsInfo.put(fieldInfo.getKey(), fieldInfo);
                                } catch (Exception e) {
                                    logger.error("Couldn't read fieldInfo information for the following Field JSON, Skipping field:" + field.toString());
                                    continue;
                                }
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Error when retrieving Issues Type fields for project "+projectKey+" and issue type ID "+issuetypeId, e);
        }
        return jiraFieldsInfo;

    }

    public String getBaseUrl() {
        return baseUri;
    }

    /**
     * In JIRA, it's possible to login with your email address but the actual user identifier will be different ;
     * searches based on users must use the correct account username.
     */
    public String getAccountUsernameFromLogonUsername(String logonUsername) {

        String accountUsernameFromEmailMatch = null;

        boolean hasMultipleEmailMatch = false;

        ClientResponse response = getWrapper().sendGet(baseUri + JIRAConstants.SEARCH_USER + "?username="+logonUsername);
        String jsonStr = response.getEntity(String.class);

        try {
            JSONArray results = new JSONArray(jsonStr);

            for (int i = 0 ; i < results.length() ; i++) {
                JSONObject userInfo = results.getJSONObject(i);

                if (logonUsername.equalsIgnoreCase(userInfo.getString("name"))) {
                    // Perfect match on username;
                    return logonUsername;
                }

                if (logonUsername.equalsIgnoreCase(userInfo.getString("emailAddress"))) {
                    if (accountUsernameFromEmailMatch != null) {
                        // We have multiple users with the same email, so search by email is inconclusive.
                        hasMultipleEmailMatch = true;
                    }

                    accountUsernameFromEmailMatch = userInfo.getString("name");
                }
            }



        } catch (JSONException e) {
            logger.error("Error when retrieving User account info for user "+logonUsername, e);
        }

        return accountUsernameFromEmailMatch == null || hasMultipleEmailMatch ? logonUsername : accountUsernameFromEmailMatch;
    }

    public String getJiraUsernameFromPpmUser(com.ppm.integration.agilesdk.dm.User user) {
        // We search first by email, or by username, or by full name, whatever comes first.
        String jiraUserSearch = user.getEmail();
        if (StringUtils.isBlank(jiraUserSearch)) {
            jiraUserSearch = user.getUsername();
        }
        if (StringUtils.isBlank(jiraUserSearch)) {
            jiraUserSearch = user.getFullName();
        }
        if (StringUtils.isBlank(jiraUserSearch)) {
            return null;
        }

        ClientResponse response =
                getWrapper().sendGet(baseUri + JIRAConstants.SEARCH_USER + "?username=" + jiraUserSearch);
        String jsonStr = response.getEntity(String.class);

        try {
            JSONArray results = new JSONArray(jsonStr);

            if (results.length() == 0) {
                return null;
            }

            // We only match with the first user returned.
            JSONObject userInfo = results.getJSONObject(0);

            return userInfo.getString("name");

        } catch (JSONException e) {
            logger.error("Error when retrieving User account info for PPM user " + user.getUserId(), e);
        }

        return null;
    }

    /** This method only returns Epics issues, without any content. It should be only used to list Epics. */
    public List<JIRASubTaskableIssue> getProjectIssuesList(String projectKey, String... issueTypes) {

        JiraIssuesRetrieverUrlBuilder searchUrlBuilder =
                new JiraIssuesRetrieverUrlBuilder(baseUri).setProjectKey(projectKey)
                        .addExtraFields(getCustomFields().getJiraCustomFields()).setStandardIssueTypes(issueTypes);

        List<JIRASubTaskableIssue> epics = retrieveIssues(searchUrlBuilder, false);
        Collections.sort(epics, new Comparator<JIRASubTaskableIssue>() {
            @Override public int compare(JIRASubTaskableIssue o1, JIRASubTaskableIssue o2) {
                try {
                    return o1.getName().compareToIgnoreCase(o2.getName());
                } catch (Exception e) {
                    // In case any epic or name is null, which shouldn't happen.
                    return 0;
                }
            }
        });

        return epics;
    }

    // Returns the non-sub-tasks issue types keys for each project key.
    public Set<String> getIssueTypesNamesPerProject(String projectKey) {

        if (projectKey == null) {
            return new HashSet<>();
        }

        if (!issueTypesPerProject.containsKey(projectKey)) {
            List<JIRAIssueType> issueTypes = getProjectIssueTypes(projectKey);
            Set<String> issueTypeNames = new HashSet<>();
            issueTypes.stream().forEach(it ->  {issueTypeNames.add(it.getName());});
            issueTypesPerProject.put(projectKey, issueTypeNames);
        }

        return issueTypesPerProject.get(projectKey);
    }

    public Collection<HierarchyLevelInfo> getJiraPortfolioLevelsInfo() {
        if (portfolioHierarchyLevelsInfo == null) {
            initPortfolioHierarchyInfo();
        }

        return portfolioHierarchyLevelsInfo;
    }


    private void initPortfolioHierarchyInfo() {
        ClientResponse response = adminWrapper.sendGet(baseUri + JIRAConstants.PORTFOLIO_HIERARCHY_REST);
        String jsonStr = response.getEntity(String.class);

        portfolioHierarchyLevelsInfo = new ArrayList<>();

        try {
            JSONArray levelInfos = new JSONArray(jsonStr);

            for (int i = 0 ; i < levelInfos.length() ; i++) {
                JSONObject levelInfo = levelInfos.getJSONObject(i);
                portfolioHierarchyLevelsInfo.add(HierarchyLevelInfo.fromJSon(levelInfo));
            }
        } catch (JSONException e) {
            logger.error("Error when retrieving JIRA Portfolio hierarchy information", e);
        }
    }

    /**
     * We should not need sub-tasks types anymore ; just retrieve all sub-tasks from standard issues by key.
     * @deprecated
     */
    public Set<String> getSubTasksIssueTypeNames() {
        if (subTasksIssueTypeNames == null) {
            initIssueTypes();
        }

        return subTasksIssueTypeNames;
    }

    private void initIssueTypes() {

        subTasksIssueTypeNames = new HashSet<>();
        allIssueTypes = new ArrayList<>();

        ClientResponse response = adminWrapper.sendGet(baseUri + JIRAConstants.ISSUE_TYPES);
        String jsonStr = response.getEntity(String.class);

        try {
            JSONArray results = new JSONArray(jsonStr);
            for (int i = 0; i < results.length(); i++) {
                JSONObject issueType = results.getJSONObject(i);

                if (issueType.getBoolean("subtask")) {
                    String issueTypeName = issueType.getString("name");
                    subTasksIssueTypeNames.add(issueTypeName);
                }

                allIssueTypes.add(JIRAIssueType.fromJSONObject(issueType));
            }
        } catch (JSONException e) {
            logger.error("Error when retrieving issue types per project", e);
        }
    }
}

package com.ppm.integration.agilesdk.connector.jira;

import com.hp.ppm.user.model.User;
import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.jira.model.*;
import com.ppm.integration.agilesdk.connector.jira.rest.util.IRestConfig;
import com.ppm.integration.agilesdk.connector.jira.rest.util.JIRARestConfig;
import com.ppm.integration.agilesdk.connector.jira.rest.util.RestWrapper;
import com.ppm.integration.agilesdk.connector.jira.rest.util.exception.RestRequestException;
import com.ppm.integration.agilesdk.connector.jira.util.JiraIssuesRetrieverUrlBuilder;
import com.ppm.integration.agilesdk.connector.jira.util.dm.AgileEntityUtils;
import com.ppm.integration.agilesdk.provider.Providers;
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

public class JIRAServiceProvider {

    private JIRAService service = null;

    private UserProvider userProvider = Providers.getUserProvider(JIRAIntegrationConnector.class);

    private boolean useAdminAccount = false;

    public UserProvider getUserProvider() {
        return userProvider;
    }

    public JIRAService get(ValueSet config) {
        synchronized (this) {
            if (service == null) {
                service = new JIRAService();
            }

            IRestConfig restConfig = new JIRARestConfig();
            restConfig.setProxy(config.get(JIRAConstants.KEY_PROXY_HOST), config.get(JIRAConstants.KEY_PROXY_PORT));
            if (useAdminAccount) {
                restConfig.setBasicAuthorizaton(config.get(JIRAConstants.KEY_ADMIN_USERNAME),
                        config.get(JIRAConstants.KEY_ADMIN_PASSWORD));
            } else {
                restConfig.setBasicAuthorizaton(config.get(JIRAConstants.KEY_USERNAME),
                        config.get(JIRAConstants.KEY_PASSWORD));
            }
            RestWrapper wrapper = new RestWrapper(restConfig);
            service.setBaseUri(config.get(JIRAConstants.KEY_BASE_URL));
            service.setWrapper(wrapper);
            service.initCustomFieldsInfo();

        }

        return service;
    }

    public JIRAServiceProvider useAdminAccount() {
        useAdminAccount = true;
        return this;
    }

    public JIRAServiceProvider useNonAdminAccount() {
        useAdminAccount = false;
        return this;
    }

    public class JIRAService {
        private final Logger logger = Logger.getLogger(this.getClass());

        private String baseUri;

        private String epicNameCustomField = null;

        private String epicLinkCustomField = null;

        private String storyPointsCustomField = null;

        private String sprintIdCustomField = null;

        private RestWrapper wrapper;

        public RestWrapper getWrapper() {
            return wrapper;
        }

        public void setWrapper(RestWrapper wrapper) {
            this.wrapper = wrapper;
        }

        private JIRAService() {
        }

        public void setBaseUri(String baseUri) {

            // Base URI should not have a trailing slash.
            while (baseUri.endsWith("/")) {
                baseUri = baseUri.substring(0, baseUri.length() - 1);
            }

            this.baseUri = baseUri;
        }

        public List<JIRAProject> getProjects() {
            ClientResponse response = wrapper.sendGet(baseUri + JIRAConstants.PROJECT_SUFFIX);

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

        public List<JIRAIssueType> getProjectIssueTypes(String projectKey) {
            ClientResponse response = wrapper.sendGet(baseUri + JIRAConstants.CREATEMETA_SUFFIX + "?projectKeys="+projectKey);

            String jsonStr = response.getEntity(String.class);

            List<JIRAIssueType> jiraIssueTypes = new ArrayList<>();
            try {
                JSONObject result = new JSONObject(jsonStr);
                JSONObject projectInfo = result.getJSONArray("projects").getJSONObject(0);
                JSONArray issueTypes = projectInfo.getJSONArray("issuetypes");
                for (int i = 0; i < issueTypes.length(); i++) {
                    JSONObject issueType = issueTypes.getJSONObject(i);
                    JIRAIssueType jiraIssueType = JIRAIssueType.fromJSONObject(issueType);

                    jiraIssueTypes.add(jiraIssueType);
                }
            } catch (JSONException e) {
                logger.error("Error when retrieving Issues Types list for project "+projectKey, e);
            }
            return jiraIssueTypes;
        }

        public JIRAProject getProject(String projectKey) {
            ClientResponse response = wrapper.sendGet(baseUri + JIRAConstants.PROJECT_SUFFIX + "/" + projectKey);

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

            ClientResponse response = wrapper.sendGet(baseUri + JIRAConstants.BOARD_SUFFIX + "/" + boardId + "/sprint");

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

            ClientResponse response = wrapper.sendPost(createIssueUri, createIssuePayload.toString(), 201);
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

            ClientResponse response = wrapper.sendPut(updateIssueUri, updateIssuePayload.toString(), 204);
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

                if (value == null || (isNumber && StringUtils.isBlank(value))) {
                    fieldsObj.put(fieldEntry.getKey(), (Object)null);
                } else if (value.startsWith(JIRAConstants.JIRA_NAME_PREFIX)) {
                    value = value.substring(JIRAConstants.JIRA_NAME_PREFIX.length());
                    JSONObject nameObj = new JSONObject();
                    nameObj.put("name", value);
                    fieldsObj.put(fieldEntry.getKey(), nameObj);
                } else {
                    if (isNumber) {
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
            fields.put(epicNameCustomField, epicName);
            fields.put("description", epicDescription);

            return createIssue(projectKey, fields, "Epic", null);
        }

        /**
         * This method should be called only once on Service instantiation.
         */
        private synchronized void initCustomFieldsInfo() {

            ClientResponse response = wrapper.sendGet(baseUri + JIRAConstants.JIRA_FIELDS_URL);
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
                                epicNameCustomField = field.getString("id");
                            }
                            if (JIRAConstants.JIRA_EPIC_LINK_CUSTOM.equalsIgnoreCase(customType)) {
                                epicLinkCustomField = field.getString("id");
                            }
                            if (JIRAConstants.JIRA_SPRINT_CUSTOM.equalsIgnoreCase(customType)) {
                                sprintIdCustomField = field.getString("id");
                            }

                            if (JIRAConstants.JIRA_STORY_POINTS_CUSTOM_NAME.equalsIgnoreCase(name)) {
                                storyPointsCustomField = field.getString("id");
                            }

                            if (epicNameCustomField != null && epicLinkCustomField != null
                                    && sprintIdCustomField != null && storyPointsCustomField != null) {
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Error while parsing fields metadata from JIRA", e);
            }

            if (epicNameCustomField == null) {
                throw new RuntimeException(
                        "We couldn't retrieve the Epic Name mandatory custom field ID from JIRA fields metadata");
            }

            if (epicLinkCustomField == null) {
                throw new RuntimeException(
                        "We couldn't retrieve the Epic Link mandatory custom field ID from JIRA fields metadata");
            }

            if (sprintIdCustomField == null) {
                throw new RuntimeException(
                        "We couldn't retrieve the Sprint ID mandatory custom field ID from JIRA fields metadata");
            }

            if (storyPointsCustomField == null) {
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
            ClientResponse response = wrapper.sendGet(query);
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
                User ppmUser = userProvider.getByEmail(assigneeEmail);
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
        public JIRAAgileEntity getSingleAgileEntityIssue(String projectKey, String issueKey) {

            JIRAAgileEntity entity = new JIRAAgileEntity();

            entity.setId(issueKey);

            ClientResponse response =
                    wrapper.sendGet(baseUri + JIRAConstants.JIRA_REST_ISSUE_URL + issueKey);

            String jsonStr = response.getEntity(String.class);

            try {
                JSONObject issueObj = new JSONObject(jsonStr);
                return AgileEntityUtils.getAgileEntityFromIssueJSon(issueObj, getBaseUrl());

            } catch (JSONException e) {
                throw new RuntimeException("Ërror when retrieving issue information for issue "+issueKey, e);
            }
        }

        public List<JIRAAgileEntity> getAgileEntityIssuesModifiedSince(Set<String> entityIds, Date modifiedSinceDate) {

            JiraIssuesRetrieverUrlBuilder searchUrlBuilder =
                    new JiraIssuesRetrieverUrlBuilder(baseUri).retrieveAllFields();

            searchUrlBuilder.addAndConstraint("key in ("+StringUtils.join(entityIds, ",")+")");
            searchUrlBuilder.addAndConstraint("updated>='"+new SimpleDateFormat("yyyy-MM-dd HH:mm").format(modifiedSinceDate)+"'");

            return retrieveAgileEntities(searchUrlBuilder);
        }

        /**
         * We use the search issue API instead of the /rest/issue/{key} because we already have all
         * the logic to get the right columns and build the right JIRAIssue in the search API.
         */
        public List<JIRAIssue> getIssues(String projectKey, Collection<String> issueKeys) {

            if (issueKeys == null || issueKeys.isEmpty()) {
                return new ArrayList<>();
            }

            JiraIssuesRetrieverUrlBuilder searchUrlBuilder =
                    new JiraIssuesRetrieverUrlBuilder(baseUri).setProjectKey(projectKey).setExpandLevel("schema")
                            .addAndConstraint("key in(" + StringUtils.join(issueKeys, ',')+")")
                            .addExtraFields(epicLinkCustomField, epicNameCustomField, sprintIdCustomField,
                                    storyPointsCustomField);

            IssueRetrievalResult result =
                    runIssueRetrivalRequest(decorateOrderBySprintCreatedUrl(searchUrlBuilder).toUrlString());

            List<JIRAIssue> issues = new ArrayList<>();

            for (JSONObject obj : result.getIssues()) {
                issues.add(getIssueFromJSONObj(obj));
            }

            return issues;
        }

        public List<JIRABoard> getAllBoards(String projectKey) {
            ClientResponse response =
                    wrapper.sendGet(baseUri + JIRAConstants.BOARD_SUFFIX + "?projectKeyOrId=" + projectKey);

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

            // We will always include sub-tasks
            issueTypes.add(JIRAConstants.JIRA_ISSUE_SUB_TASK);

            JiraIssuesRetrieverUrlBuilder searchUrlBuilder =
                    new JiraIssuesRetrieverUrlBuilder(baseUri).setProjectKey(projectKey)
                            .addExtraFields(epicLinkCustomField, epicNameCustomField, sprintIdCustomField,
                                    storyPointsCustomField).setIssuesTypes(issueTypes);

            return retrieveIssues(searchUrlBuilder);
        }

        private List<JIRAAgileEntity> retrieveAgileEntities(JiraIssuesRetrieverUrlBuilder searchUrlBuilder) {

            IssueRetrievalResult result = null;
            int fetchedResults = 0;
            searchUrlBuilder.setStartAt(0);

            List<JIRAAgileEntity> allIssues = new ArrayList<JIRAAgileEntity>();

            do {
                result = runIssueRetrivalRequest(searchUrlBuilder.toUrlString());
                for (JSONObject obj : result.getIssues()) {
                    allIssues.add(AgileEntityUtils.getAgileEntityFromIssueJSon(obj, getBaseUrl()));
                }

                fetchedResults += result.getMaxResults();
                searchUrlBuilder.setStartAt(fetchedResults);

            } while (fetchedResults < result.getTotal());

            return allIssues;
        }





        private List<JIRASubTaskableIssue> retrieveIssues(JiraIssuesRetrieverUrlBuilder searchUrlBuilder) {

            IssueRetrievalResult result = null;
            int fetchedResults = 0;
            searchUrlBuilder.setStartAt(0);

            List<JIRAIssue> allIssues = new ArrayList<JIRAIssue>();

            do {
                result = runIssueRetrivalRequest(searchUrlBuilder.toUrlString());
                for (JSONObject issueObj : result.getIssues()) {
                    allIssues.add(getIssueFromJSONObj(issueObj));
                }
                fetchedResults += result.getMaxResults();
                searchUrlBuilder.setStartAt(fetchedResults);

            } while (fetchedResults < result.getTotal());

            Map<String, JIRAIssue> indexedIssues = new HashMap<>();

            for (JIRAIssue issue : allIssues) {
                indexedIssues.put(issue.getKey(), issue);
            }

            // We first check if all parents of sub-tasks have been retrieved. If not, we retrieve them in a separate call.
            List<String> missingIssues = new ArrayList<>();
            for (JIRAIssue issue: allIssues) {
                if (JIRAConstants.JIRA_ISSUE_SUB_TASK.equalsIgnoreCase(issue.getType())) {
                    JIRASubTask subTask = (JIRASubTask)issue;
                    JIRAIssue parent = indexedIssues.get(subTask.getParentKey());
                    if (parent == null) {
                        missingIssues.add(subTask.getParentKey());
                    }
                }
            }

            if (!missingIssues.isEmpty()) {
                // We retrieved some sub-tasks but missed their parents. Let's retrieve them now.
                List<JIRAIssue> missingParents = getIssues(null, missingIssues);
                for (JIRAIssue missingParent : missingParents) {
                    allIssues.add(missingParent);
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

            // Read all Stories/Tasks/Features, add them to Epic
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
            for (JIRAIssue issue : allIssues) {
                if (JIRAConstants.JIRA_ISSUE_SUB_TASK.equalsIgnoreCase(issue.getType())) {

                    JIRASubTask subTask = (JIRASubTask)issue;

                    if (subTask.getParentKey() != null) {
                        JIRASubTaskableIssue parent = subTaskableIssues.get(subTask.getParentKey());
                        if (parent != null) {
                            parent.addSubTask(subTask);
                        }
                    }
                }
            }

            return processedIssues;
        }

        private IssueRetrievalResult runIssueRetrivalRequest(String urlString) {

            ClientResponse response = wrapper.sendGet(urlString);

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
                throw new RuntimeException("Problem occured when retrieving Issue Search results", e);
            }
        }

        private JIRAIssue getIssueFromJSONObj(JSONObject obj) {
            try {
                JSONObject fields = obj.getJSONObject("fields");

                String issueType = fields.getJSONObject("issuetype").getString("name");

                JIRAIssue issue = null;

                switch (issueType.toUpperCase()) {
                    case JIRAConstants.JIRA_ISSUE_EPIC:
                        issue = new JIRAEpic();
                        break;
                    case JIRAConstants.JIRA_ISSUE_FEATURE:
                        issue = new JIRAFeature();
                        break;
                    case JIRAConstants.JIRA_ISSUE_STORY:
                        issue = new JIRAStory();
                        break;
                    case JIRAConstants.JIRA_ISSUE_TASK:
                        issue = new JIRATask();
                        break;
                    case JIRAConstants.JIRA_ISSUE_SUB_TASK:
                        issue = new JIRASubTask();

                        if (fields.has("parent")) {
                            ((JIRASubTask)issue).setParentKey(fields.getJSONObject("parent").getString("key"));
                        }
                        break;
                    case JIRAConstants.JIRA_ISSUE_BUG:
                        issue = new JIRABug();
                        break;
                    default:
                        throw new RuntimeException("Unknow issue type:" + issueType);
                }

                // Common fields for all issues
                issue.setType(issueType);

                issue.setTypeId(fields.getJSONObject("issuetype").getString("id"));

                if (fields.has("status") && fields.getJSONObject("status").has("name")) {
                    issue.setStatus(fields.getJSONObject("status").getString("name"));
                }
                if (fields.has("creator") && fields.getJSONObject("creator").has("emailAddress")) {
                    issue.setAuthorName(fields.getJSONObject("creator").getString("emailAddress"));
                }
                if (fields.has("priority") && fields.getJSONObject("priority").has("name")) {
                    issue.setPriorityName(fields.getJSONObject("priority").getString("name"));
                }
                if (fields.has(sprintIdCustomField) && !fields.isNull(sprintIdCustomField)) {
                    String sprintId = getSprintIdFromSprintCustomfield(fields.getJSONArray(sprintIdCustomField));
                    issue.setSprintId(sprintId);
                }

                issue.setKey(obj.getString("key"));
                issue.setName(fields.getString("summary"));
                if (issue instanceof JIRAEpic) {
                    // JIRA stores Epic name in a custom field.
                    issue.setName(fields.getString(epicNameCustomField));
                }
                issue.setAssigneePpmUserId(getAssigneeUserId(fields));
                issue.setAuthorName(
                        (fields.has("creator") && !fields.isNull("creator") && fields.getJSONObject("creator")
                                .has("displayName")) ? fields.getJSONObject("creator").getString("displayName") : null);
                issue.setCreationDate(fields.has("created") ? fields.getString("created") : "");
                issue.setLastUpdateDate(fields.has("updated") ? fields.getString("updated") : "");
                issue.setResolutionDate(fields.has(JIRAConstants.JIRA_FIELD_RESOLUTION_DATE) ? fields.getString(JIRAConstants.JIRA_FIELD_RESOLUTION_DATE) : "");
                issue.setEpicKey(fields.getString(epicLinkCustomField));
                issue.setStoryPoints((fields.has(storyPointsCustomField) && !fields.isNull(storyPointsCustomField)) ?
                        new Double(fields.getDouble(storyPointsCustomField)).longValue() : null);

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
                    wrapper.sendGet(baseUri + JIRAConstants.JIRA_GET_ISSUE_WORKLOG.replace("%issue%", issueKey));

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
            // We will always include sub-tasks
            issueTypes.add(JIRAConstants.JIRA_ISSUE_SUB_TASK);

            JiraIssuesRetrieverUrlBuilder boardIssuesUrlBuilder =
                    new JiraIssuesRetrieverUrlBuilder(baseUri).setProjectKey(projectKey)
                            .addExtraFields(epicLinkCustomField, epicNameCustomField, sprintIdCustomField,
                                    storyPointsCustomField).setBoardType(boardId).setIssuesTypes(issueTypes);

            return retrieveIssues(boardIssuesUrlBuilder);
        }

        public List<JIRASubTaskableIssue> getEpicIssues(String projectKey, Set<String> issueTypes, String epicKey) {
            // We will always include sub-tasks
            issueTypes.add(JIRAConstants.JIRA_ISSUE_SUB_TASK);

            JiraIssuesRetrieverUrlBuilder searchUrlBuilder =
                    new JiraIssuesRetrieverUrlBuilder(baseUri).setIssuesTypes(issueTypes).setProjectKey(projectKey)
                            .addExtraFields(epicLinkCustomField, epicNameCustomField, sprintIdCustomField,
                                    storyPointsCustomField);

            // Retrieving only issues belonging to that epic
            searchUrlBuilder.addCustomFieldEqualsConstraint(epicLinkCustomField, epicKey);

            // We also want to retrieve the Epic itself
            searchUrlBuilder.addOrConstraint("key=" + epicKey);

            // We also want sub-tasks of the epic
            searchUrlBuilder.addOrConstraint("parent=" + epicKey);

            return retrieveIssues(searchUrlBuilder);
        }

        public List<JIRASubTaskableIssue> getVersionIssues(String projectKey, Set<String> issueTypes, String versionId)
        {
            issueTypes.add(JIRAConstants.JIRA_ISSUE_SUB_TASK);

            JiraIssuesRetrieverUrlBuilder versionIssuesUrlBuilder =
                    new JiraIssuesRetrieverUrlBuilder(baseUri).setProjectKey(projectKey)
                            .addExtraFields(epicLinkCustomField, epicNameCustomField, sprintIdCustomField,
                                    storyPointsCustomField).setIssuesTypes(issueTypes)
                            .addAndConstraint("fixVersion=" + versionId);

            return retrieveIssues(versionIssuesUrlBuilder);
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
                    .addExtraFields(epicLinkCustomField, epicNameCustomField, sprintIdCustomField,
                            storyPointsCustomField, JIRAConstants.JIRA_FIELD_RESOLUTION_DATE)
                    // We want story points to be defined.
                    .addNonNullCustomField(storyPointsCustomField)
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

            List<JIRASubTaskableIssue> issues = retrieveIssues(spTimesheetUrlBuilder);

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
                    .addExtraFields(epicLinkCustomField, epicNameCustomField, sprintIdCustomField,
                            storyPointsCustomField)
                    .addAndConstraint("worklogDate>=" + dateFrom.toString().substring(0, 10))
                    .addAndConstraint("worklogDate<=" + dateTo.toString().substring(0, 10))
                    .addAndConstraint("worklogAuthor=" + encodeForJQLQuery(author));

            if (!isBlank(projectKey)) {
                worklogUrlBuilder.setProjectKey(projectKey);
            }

            List<JIRASubTaskableIssue> issues = retrieveIssues(worklogUrlBuilder);

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
                        .equalsIgnoreCase(worklog.getAuthorKey())) {
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
            ClientResponse response = wrapper.sendGet(baseUri + JIRAConstants.CREATEMETA_SUFFIX + "?projectKeys="+projectKey+"&issuetypeIds="+issuetypeId+"&expand=projects.issuetypes.fields");

            String jsonStr = response.getEntity(String.class);

            Map<String, JIRAFieldInfo> jiraFieldsInfo = new HashMap<>();
            try {
                JSONObject result = new JSONObject(jsonStr);
                JSONObject projectInfo = result.getJSONArray("projects").getJSONObject(0);
                JSONObject fields = projectInfo.getJSONArray("issuetypes").getJSONObject(0).getJSONObject("fields");

                for (String fieldKey : JSONObject.getNames(fields)) {
                    JSONObject field = fields.getJSONObject(fieldKey);
                    JIRAFieldInfo fieldInfo = JIRAFieldInfo.fromJSONObject(field, fieldKey);
                    jiraFieldsInfo.put(fieldInfo.getKey(), fieldInfo);
                }

            } catch (JSONException e) {
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

            ClientResponse response = wrapper.sendGet(baseUri + JIRAConstants.SEARCH_USER + "?username="+logonUsername);
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
                    wrapper.sendGet(baseUri + JIRAConstants.SEARCH_USER + "?username=" + jiraUserSearch);
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
    }
}

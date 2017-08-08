
package com.ppm.integration.agilesdk.connector.jira;

import com.hp.ppm.user.model.User;
import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.jira.model.*;
import com.ppm.integration.agilesdk.connector.jira.rest.util.IRestConfig;
import com.ppm.integration.agilesdk.connector.jira.rest.util.JIRARestConfig;
import com.ppm.integration.agilesdk.connector.jira.rest.util.RestWrapper;
import com.ppm.integration.agilesdk.connector.jira.util.JiraIssuesRetrieverUrlBuilder;
import com.ppm.integration.agilesdk.epic.PortfolioEpicCreationInfo;
import com.ppm.integration.agilesdk.provider.Providers;
import com.ppm.integration.agilesdk.provider.UserProvider;
import com.ppm.integration.agilesdk.tm.ExternalWorkItem;
import com.ppm.integration.agilesdk.tm.ExternalWorkItemEffortBreakdown;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wink.client.ClientResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.xml.datatype.XMLGregorianCalendar;
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
                baseUri = baseUri.substring(0, baseUri.length()-1);
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
                logger.error("", e);
            }
            return list;
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

                jiraSprints.addAll(getBoardSprints(board.getId()));

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

        public String createEpic(String projectKey, PortfolioEpicCreationInfo epicInfo) {
            // Read all custom field info required to create an Epic
            initCustomFieldsInfo();

            // Create Epic
            String createEpicUri = baseUri + JIRAConstants.JIRA_CREATE_ISSUE_URL;

            String createEpicPayload = "{\n" + "    \"fields\": {\n"
                    + "       \"project\": { \"key\": \"%projectKey%\" },\n" + "       \"summary\": \"%epicName%\",\n"
                    + "       \"%epicNameCustomField%\": \"%epicName%\",\n"
                    + "       \"description\": \"%epicDescription%\",\n" + "       \"issuetype\": {\n"
                    + "          \"name\": \"Epic\"\n" + "       }\n" + "   }\n" + "}";

            createEpicPayload = createEpicPayload.replace("%projectKey%", projectKey).replace("%epicName%", epicInfo.getEpicName())
                    .replace("%epicDescription%", epicInfo.getEpicDescription()).replace("%epicNameCustomField%", epicNameCustomField);

            ClientResponse response = wrapper.sendPost(createEpicUri, createEpicPayload, 201);
            String jsonStr = response.getEntity(String.class);
            try {
                JSONObject jsonObj = new JSONObject(jsonStr);
                return jsonObj.getString("key");
            } catch (Exception e) {
                throw new RuntimeException("Error while parsing Epic creation response from JIRA: "+jsonStr, e);
            }
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
                    version.setArchived(versionObj.has("archived")? versionObj.getBoolean("archived"):false);
                    version.setReleased(versionObj.has("released")? versionObj.getBoolean("released"):false);
                    version.setOverdue(versionObj.has("overdue")? versionObj.getBoolean("overdue"):false);
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

            JSONObject assignee = (fields.has("assignee") &&!fields.isNull("assignee")) ? fields.getJSONObject("assignee") : null;
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
                for (int i = 0 ; i < fixVersions.length() ;  i++) {
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

        private JiraIssuesRetrieverUrlBuilder decorateOrderBySprintCreatedUrl(JiraIssuesRetrieverUrlBuilder urlBuilder) {
            return urlBuilder.setOrderBy(" sprint,created ASC ");
        }

        /**
         * We use the search issue API instead of the /rest/issue/{key} because we already have all
         * the logic to get the right columns and build the right JIRAIssue in the search API.
         */
        public JIRAIssue getSingleIssue(String projectKey, String issueKey) {

            JiraIssuesRetrieverUrlBuilder searchUrlBuilder =
                    new JiraIssuesRetrieverUrlBuilder(baseUri).setProjectKey(projectKey).setExpandLevel("schema").addAndConstraint("key="+issueKey)
                            .addExtraFields(epicLinkCustomField, epicNameCustomField, sprintIdCustomField, storyPointsCustomField);

            IssueRetrievalResult result = runIssueRetrivalRequest(decorateOrderBySprintCreatedUrl(searchUrlBuilder).toUrlString());

            if (result.getIssues().isEmpty()) {
                return null;
            } else if (result.getIssues().size() > 1) {
                throw new RuntimeException("Retrieving issue "+issueKey+" in project "+projectKey+" returned more than 1 result ("+result.getIssues().size()+")");
            } else {
                return result.getIssues().get(0);
            }
        }

        public List<JIRABoard> getAllBoards(String projectKey) {
            ClientResponse response = wrapper.sendGet(baseUri + JIRAConstants.BOARD_SUFFIX + "?projectKeyOrId=" + projectKey);

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
                    board.setType(getStr(jsonBoard,"type"));
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
                throw new RuntimeException("Error when retrieving key "+key +" from JSon object "+obj.toString());
            }
        }

        /**
         * This method returns all requested issue types, with sub-tasks always included and returned as part of their parent and never as standalone issues.
         * <br/>Each Epic will have its content available directly as {@link JIRAEpic#getContents()}, but only for the issues types that were requested.
         *
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

        private List<JIRASubTaskableIssue> retrieveIssues(JiraIssuesRetrieverUrlBuilder searchUrlBuilder) {

            IssueRetrievalResult result = null;
            int fetchedResults = 0;

            List<JIRAIssue> allIssues = new ArrayList<JIRAIssue>();

            do {
                result = runIssueRetrivalRequest(searchUrlBuilder.toUrlString());
                allIssues.addAll(result.getIssues());
                fetchedResults += result.getMaxResults();
                searchUrlBuilder.setStartAt(fetchedResults);

            } while (fetchedResults < result.getTotal());

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
                if (!JIRAConstants.JIRA_ISSUE_EPIC.equalsIgnoreCase(issue.getType()) && issue instanceof JIRASubTaskableIssue) {

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

                IssueRetrievalResult result = new IssueRetrievalResult(resultObj.getInt("startAt"), resultObj.getInt("maxResults"), resultObj.getInt("total"));

                JSONArray issues = resultObj.getJSONArray("issues");

                for (int i = 0; i < issues.length(); i++) {
                    JSONObject issueObj = issues.getJSONObject(i);

                    JIRAIssue issue = getIssueFromJSONObj(issueObj);

                    result.addIssue(issue);
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

                switch(issueType.toUpperCase()) {
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
                        throw new RuntimeException("Unknow issue type:"+issueType);
                }

                // Common fields for all issues
                issue.setType(issueType);

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
                    String sprintId = getSprintIdFromSprintCustomfield(fields.getJSONArray(sprintIdCustomField).getString(0));
                    issue.setSprintId(sprintId);
                }

                issue.setKey(obj.getString("key"));
                issue.setName(fields.getString("summary"));
                if (issue instanceof JIRAEpic) {
                    // JIRA stores Epic name in a custom field.
                    issue.setName(fields.getString(epicNameCustomField));
                }
                issue.setAssigneePpmUserId(getAssigneeUserId(fields));
                issue.setAuthorName((fields.has("creator") && !fields.isNull("creator") && fields.getJSONObject("creator").has("displayName"))
                        ? fields.getJSONObject("creator").getString("displayName") : null);
                issue.setCreationDate(fields.has("created") ? fields.getString("created") : "");
                issue.setLastUpdateDate(fields.has("updated") ? fields.getString("updated") : "");
                issue.setEpicKey(fields.getString(epicLinkCustomField));
                issue.setStoryPoints((fields.has(storyPointsCustomField) && !fields.isNull(storyPointsCustomField)) ? new Double(fields.getDouble(storyPointsCustomField)).longValue() : null);

                issue.setFixVersionIds(extractFixVersionIds(fields));

                // Estimates info
                if (fields.has("timetracking") && !fields.isNull("timetracking")) {
                    JSONObject timeTracking = fields.getJSONObject("timetracking");
                    if (timeTracking.has("remainingEstimateSeconds")) {
                        issue.getWork().setRemainingEstimateHours(JIRAIssueWork.secToHours(timeTracking.getInt("remainingEstimateSeconds")));
                    }
                    if (timeTracking.has("timeSpentSeconds")) {
                        issue.getWork().setTimeSpentHours(JIRAIssueWork.secToHours(timeTracking.getInt("timeSpentSeconds")));
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
                            issue.getWork().addWorklogEntry(JIRAIssueWork.getWorklogEntryFromWorklogJSONObject(worklogEntry));
                        }
                    }
                }

                return issue;
            } catch (JSONException e) {
                throw new RuntimeException("Error when reading info from issue", e);
            }
        }

        /** Retrieves Sprint ID from Sprint custom field.
         * The example of origin format of sprintCustomfield is
         * "com.atlassian.greenhopper.service.sprint.Sprint@1f39706[id=1,rapidViewId=1,state=ACTIVE,name=SampleSprint
         *  2,goal=<null>,startDate=2016-12-07T06:18:24.224+08:00,endDate=2016-12-21T06:38:24.224+08:00,completeDate=<null>,sequence=1]"
         */
        private String getSprintIdFromSprintCustomfield(String sprintCustomfield) {
            Map<String, String> map = new HashMap<String, String>();
            String reg = ".+@.+\\[(.+)]";
            Pattern pattern = Pattern.compile(reg);
            Matcher matcher = pattern.matcher(sprintCustomfield);
            if (matcher.find()) {
                String exp = matcher.group(1);
                String[] kvs = exp.split(",");
                for (String kv : kvs) {
                    String[] splited = kv.split("=");
                    if ("id".equalsIgnoreCase(splited[0])) {
                        if (splited.length == 2) {
                            return splited[1];
                        }
                    }
                }
            }
            return null;
        }

        private JSONArray getWorklogsJSONArrayForIssue(String issueKey) {

            ClientResponse response = wrapper.sendGet(baseUri + JIRAConstants.JIRA_GET_ISSUE_WORKLOG.replace("%issue%", issueKey));

            String jsonStr = response.getEntity(String.class);
            try {
                JSONObject obj = new JSONObject(jsonStr);
                JSONArray worklogsArray = obj.getJSONArray("worklogs");
                return worklogsArray;


            } catch (JSONException e) {
                throw new RuntimeException("Ërror when retrieving all worklogs information for issue "+issueKey, e);
            }
        }

        public List<JIRASubTaskableIssue> getBoardIssues(String projectKey, Set<String> issueTypes, String boardId) {
            // We will always include sub-tasks
            issueTypes.add(JIRAConstants.JIRA_ISSUE_SUB_TASK);

            JiraIssuesRetrieverUrlBuilder boardIssuesUrlBuilder =
                    new JiraIssuesRetrieverUrlBuilder(baseUri).setProjectKey(projectKey)
                            .addExtraFields(epicLinkCustomField, epicNameCustomField, sprintIdCustomField, storyPointsCustomField)
                            .setBoardType(boardId)
                            .setIssuesTypes(issueTypes);

            return retrieveIssues(boardIssuesUrlBuilder);
        }

        public List<JIRASubTaskableIssue> getEpicIssues(String projectKey, Set<String> issueTypes, String epicKey) {
            // We will always include sub-tasks
            issueTypes.add(JIRAConstants.JIRA_ISSUE_SUB_TASK);

            JiraIssuesRetrieverUrlBuilder searchUrlBuilder =
                    new JiraIssuesRetrieverUrlBuilder(baseUri)
                            .setIssuesTypes(issueTypes).setProjectKey(projectKey)
                            .addExtraFields(epicLinkCustomField, epicNameCustomField, sprintIdCustomField, storyPointsCustomField);

            // Retrieving only issues belonging to that epic
            searchUrlBuilder.addCustomFieldEqualsConstraint(epicLinkCustomField, epicKey);

            // We also want to retrieve the Epic itself
            searchUrlBuilder.addOrConstraint("key="+epicKey);

            // We also want sub-tasks of the epic
            searchUrlBuilder.addOrConstraint("parent="+epicKey);

            return retrieveIssues(searchUrlBuilder);
        }

        public List<JIRASubTaskableIssue> getVersionIssues(String projectKey, Set<String> issueTypes, String versionId)
        {
            issueTypes.add(JIRAConstants.JIRA_ISSUE_SUB_TASK);

            JiraIssuesRetrieverUrlBuilder versionIssuesUrlBuilder =
                    new JiraIssuesRetrieverUrlBuilder(baseUri).setProjectKey(projectKey)
                            .addExtraFields(epicLinkCustomField, epicNameCustomField, sprintIdCustomField, storyPointsCustomField)
                            .setIssuesTypes(issueTypes)
                            .addAndConstraint("fixVersion="+versionId);

            return retrieveIssues(versionIssuesUrlBuilder);
        }

        /**
         * @return all the worklog items for the passed author within the passed dates as timesheet lines.
         */
        public List<ExternalWorkItem> getWorkItems(final XMLGregorianCalendar dateFrom, final XMLGregorianCalendar dateTo, String projectKey, String author)
        {
            JiraIssuesRetrieverUrlBuilder worklogUrlBuilder = new JiraIssuesRetrieverUrlBuilder(baseUri)
                    .addExtraFields(epicLinkCustomField, epicNameCustomField, sprintIdCustomField, storyPointsCustomField)
                    .addAndConstraint("worklogDate>=" + dateFrom.toString().substring(0, 10))
                    .addAndConstraint("worklogDate<=" + dateTo.toString().substring(0, 10))
                    .addAndConstraint("worklogAuthor=" + encodeForJQLQuery(author));

            if (!isBlank(projectKey)) {
                worklogUrlBuilder.setProjectKey(projectKey);
            }

            List<JIRASubTaskableIssue> issues = retrieveIssues(worklogUrlBuilder);

            List<ExternalWorkItem> timesheetLines = new ArrayList<ExternalWorkItem>();

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
                    timesheetLines.add(new ExternalWorkItem() {
                        @Override public String getName() {
                            return "["+issue.getKey()+"] "+issue.getFullTaskName();
                        }

                        @Override public ExternalWorkItemEffortBreakdown getEffortBreakDown() {
                            ExternalWorkItemEffortBreakdown effortBreakdown = new ExternalWorkItemEffortBreakdown();
                            for (JIRAIssueWork.JIRAWorklogEntry worklog : worklogs) {
                                // We round time values to 2 decimals as we don't want values to look too ugly in Timesheet UI.
                                // Unfortunately as we're working with Doubles, this won't always work :(
                                // Luckily, the bad display will only happen on the import recap page, once the data gets in the timesheet page there's proper formatting.
                                effortBreakdown.addEffort(worklog.getDateStartedAsDate(),  Math.round(worklog.getTimeSpentHours() * 100) / 100d);
                            }

                            insertZeroEffortDatesInBreakdown(effortBreakdown, dateFrom.toGregorianCalendar(), dateTo.toGregorianCalendar());

                            return effortBreakdown;
                        }
                    });
                }
            }

            return timesheetLines;
        }


    }

    // We must have some time info defined for every date of the timesheet, otherwise the timesheet UI in PPM will bug.
    // This was fixed in PPM 9.42 as it will fill the gaps for you, but we want this connector to also work on PPM 9.41.
    private void insertZeroEffortDatesInBreakdown(ExternalWorkItemEffortBreakdown effortBreakdown,
            Calendar startDate, Calendar endDate) {

        do {
            effortBreakdown.addEffort(startDate.getTime(), 0d);
            startDate.add(Calendar.DATE, 1);
        } while (!startDate.after(endDate));

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
     * @return the worklogs that match the constraints of date & author.
     */
    private List<JIRAIssueWork.JIRAWorklogEntry> pickWorklogs(List<JIRAIssueWork.JIRAWorklogEntry> worklogs, Date fromDate, Date toDate, String author)
    {
        List<JIRAIssueWork.JIRAWorklogEntry> validWorklogs = new ArrayList<JIRAIssueWork.JIRAWorklogEntry>();

        if (worklogs == null || isBlank(author)) {
            return validWorklogs;
        }

        for (JIRAIssueWork.JIRAWorklogEntry worklog : worklogs) {
            if (worklog == null) {
                continue;
            }

            if (author.equalsIgnoreCase(worklog.getAuthorEmail()) || author.equalsIgnoreCase(worklog.getAuthorKey())) {
                Date logDate = worklog.getDateStartedAsDate();
                if ((fromDate.before(logDate) && toDate.after(logDate)) || fromDate.equals(logDate) || toDate.equals(logDate)) {
                    validWorklogs.add(worklog);
                }
            }
        }

        return validWorklogs;
    }
}

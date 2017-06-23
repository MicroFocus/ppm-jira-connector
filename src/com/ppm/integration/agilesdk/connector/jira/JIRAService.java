
package com.ppm.integration.agilesdk.connector.jira;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.datatype.XMLGregorianCalendar;

import com.ppm.integration.agilesdk.connector.jira.util.JiraSearchUrlBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wink.client.ClientResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ppm.integration.agilesdk.connector.jira.model.JIRAEpic;
import com.ppm.integration.agilesdk.connector.jira.model.JIRAIssue;
import com.ppm.integration.agilesdk.connector.jira.model.JIRAIssueType;
import com.ppm.integration.agilesdk.connector.jira.model.JIRAProject;
import com.ppm.integration.agilesdk.connector.jira.model.JIRASprint;
import com.ppm.integration.agilesdk.connector.jira.model.JIRATempoIssue;
import com.ppm.integration.agilesdk.connector.jira.model.JIRATempoWorklog;
import com.ppm.integration.agilesdk.connector.jira.model.JIRAVersion;
import com.ppm.integration.agilesdk.connector.jira.rest.util.RestWrapper;
import com.ppm.integration.agilesdk.connector.jira.util.JIRACustomIdFinder;
import com.ppm.integration.agilesdk.pm.ExternalTask;

public class JIRAService {
    private final Logger logger = Logger.getLogger(this.getClass());

    private String baseUri;

    private RestWrapper wrapper;

    public RestWrapper getWrapper() {
        return wrapper;
    }

    public void setWrapper(RestWrapper wrapper) {
        this.wrapper = wrapper;
    }

    public JIRAService() {
    }

    public JIRAService(String baseUri, RestWrapper wrapper) {
        this.baseUri = baseUri;
        this.wrapper = wrapper;
    }

    public JIRAService(String baseUri) {
        this.baseUri = baseUri;
    }

    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
    }

    public List<JIRAProject> getProjects() {
        ClientResponse response = wrapper.sendGet(baseUri + JIRAConstants.PROJECT_SUFFIX);

        String jsonStr = response.getEntity(String.class);

        List<JIRAProject> list = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(jsonStr);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                JIRAProject project =
                        new JIRAProject(obj.getString("expand"), obj.getString("self"), obj.getString("id"),
                                obj.getString("key"), obj.getString("name"), null, obj.getString("projectTypeKey"));
                JSONObject avatarUrlsJson = obj.getJSONObject("avatarUrls");
                HashMap<String, String> avatarUrls = new HashMap<>();
                avatarUrls.put("48x48", avatarUrlsJson.getString("48x48"));
                avatarUrls.put("32x32", avatarUrlsJson.getString("32x32"));
                avatarUrls.put("24x24", avatarUrlsJson.getString("24x24"));
                avatarUrls.put("16x16", avatarUrlsJson.getString("16x16"));
                project.setAvatarUrls(avatarUrls);
                list.add(project);

            }
        } catch (JSONException e) {
            logger.error("", e);
        }
        return list;
    }

    private List<JIRAIssue> getIssues(String projectKey, Map<String, Boolean> issuesTypesMap, boolean isInSprint) {
        List<JIRAIssue> list = new ArrayList<>();

        JiraSearchUrlBuilder searchUrlBuilder = new JiraSearchUrlBuilder(baseUri).setProjectKey(projectKey)
                .setExpandLevel("schema").setIssuesTypes(issuesTypesMap).setOnlyIncludePlanned(isInSprint);


          // if the Issues To Import contains only epic type
        if (issuesTypesMap.get(JIRAConstants.JIRA_ISSUE_EPIC) && issuesTypesMap.size() == 1) {
            searchUrlBuilder.setOnlyIncludePlanned(false).setExcludeSubTasks(false);
        } else {
            searchUrlBuilder.setExcludeSubTasks(true);
        }

        ClientResponse response = wrapper.sendGet(decorateOrderBySprintCreatedUrl(searchUrlBuilder).toUrlString());

        String jsonStr = response.getEntity(String.class);
        try {

            JSONObject jsonObj = new JSONObject(jsonStr);

            JSONArray jsonIssues = jsonObj.getJSONArray("issues");
            JSONObject schemas = null;
            if(jsonObj.has("schema")) {
                schemas = jsonObj.getJSONObject("schema");
            }
            
            if(jsonIssues != null && jsonIssues.length() > 0 && schemas != null) {
                String sprintCustomId = JIRACustomIdFinder.findId(schemas, JIRAConstants.JIRA_SPRINT_CUSTOM);
                String epicLinkId = JIRACustomIdFinder.findId(schemas, JIRAConstants.JIRA_EPIC_LINK_CUSTOM);
                for (int i = 0; i < jsonIssues.length(); i++) {
                    JSONObject fields = jsonIssues.getJSONObject(i).getJSONObject("fields");
                    String issueKey = jsonIssues.getJSONObject(i).getString("key");
                    boolean isSubTask = fields.getJSONObject("issuetype").getBoolean("subtask");
                    if (!isSubTask) {
                        JIRAIssue jiraIssue =
                                resolveIssue(fields, false, null, null, null, issueKey, sprintCustomId, epicLinkId);
                        list.add(jiraIssue);
                    }

                }
            }
        } catch (JSONException e) {
            logger.error("", e);
        }
        return list;
    }

    private List<JIRASprint> getSprintsWithIssues(JiraSearchUrlBuilder searchUrlBuider, boolean isBreakdown) {
        List<JIRASprint> sprints = new ArrayList<>();
        ClientResponse response = wrapper.sendGet(decorateOrderBySprintCreatedUrl(searchUrlBuider).toUrlString());
        String jsonStr = response.getEntity(String.class);
        try {

            JSONObject jsonObj = new JSONObject(jsonStr);

            JSONArray jsonIssues = jsonObj.getJSONArray("issues");
            JSONObject schemas = null;
            if(jsonObj.has("schema")) {
                schemas = jsonObj.getJSONObject("schema");
            }
            
            if(jsonIssues != null && jsonIssues.length() > 0 && schemas != null) {
                String sprintCustomId = JIRACustomIdFinder.findId(schemas, JIRAConstants.JIRA_SPRINT_CUSTOM);
                String epicLinkCustomId = JIRACustomIdFinder.findId(schemas, JIRAConstants.JIRA_EPIC_LINK_CUSTOM);
                Map<JIRASprint, List<JIRAIssue>> sprintMap = resolveIssue(jsonIssues, sprintCustomId, epicLinkCustomId);
                Set<Entry<JIRASprint, List<JIRAIssue>>> set = sprintMap.entrySet();
                for (Entry<JIRASprint, List<JIRAIssue>> e : set) {
                    JIRASprint js = e.getKey();
                    js.setBreakdown(isBreakdown);
                    js.setIssues(e.getValue());
                    sprints.add(js);
                }
            }

        } catch (JSONException e) {
            logger.error("", e);
        }

        return sprints;
    }

    public List<ExternalTask> getExternalTasks(String projectKey, Map<String, Boolean> map, String importSelection,
            String importSelectionDetails, boolean isBreakdown, boolean isIncludeOnlyPlanned)
    {
        List<ExternalTask> ets = null;
        JiraSearchUrlBuilder searchUrlBuilder = new JiraSearchUrlBuilder(baseUri)
                .setProjectKey(projectKey).setIssuesTypes(map).setOnlyIncludePlanned(isIncludeOnlyPlanned).setExpandLevel("schema");
        switch (importSelection) {
            case JIRAConstants.KEY_EPIC:
                String epicLinkCustomId = "";
                ClientResponse response = wrapper.sendGet(searchUrlBuilder.toUrlString());

                String jsonStr = response.getEntity(String.class);

                try {

                    JSONObject jsonObj = new JSONObject(jsonStr);
                    JSONObject schemas = null;                    
                    if(jsonObj.has("schema")) {
                        schemas = jsonObj.getJSONObject("schema");
                    }
                    
                    if(schemas != null) {
                        epicLinkCustomId = JIRACustomIdFinder.findId(schemas, JIRAConstants.JIRA_EPIC_LINK_CUSTOM);
                        epicLinkCustomId = epicLinkCustomId.substring(epicLinkCustomId.indexOf("_") + 1);
                    }
                } catch (JSONException e) {
                    logger.error("", e);
                }
                
                if(! "".equals(epicLinkCustomId)) {
                    if (!"".equals(importSelectionDetails)) {
                        // Specific epic
                        searchUrlBuilder.addCustomFieldEqualsConstraint(epicLinkCustomId, importSelectionDetails);
                    } else {
                        // Epic is not null
                        searchUrlBuilder.addNonNullCustomField(epicLinkCustomId);
                    }
                    ets = toExternalTasks(searchUrlBuilder, isBreakdown);
                } else {
                    ets = new ArrayList<>();
                }
                break;
            case JIRAConstants.KEY_ALL_EPICS:
                List<JIRAEpic> list = getEpicsWithIssues(projectKey, map, isBreakdown);
                ets = toExternalTasks(list);
                break;
            case JIRAConstants.KEY_VERSION:

                if (!"".equals(importSelectionDetails)) {
                    // Specific version
                    searchUrlBuilder.addAndConstraint("fixVersion=" + importSelectionDetails);
                } else {
                    // All versions
                    searchUrlBuilder.addAndConstraint("fixVersion!=null");
                }
                ets = toExternalTasks(searchUrlBuilder, isBreakdown);
                break;
            case JIRAConstants.KEY_ALL_PROJECT_PLANNED_ISSUES:
                ets = toExternalTasks(searchUrlBuilder, isBreakdown);
        }
        return ets;
    }

    private List<ExternalTask> toExternalTasks(JiraSearchUrlBuilder searchUrlBuider, boolean isBreakdown) {
        List<ExternalTask> ets = new ArrayList<>();
        List<JIRASprint> sprints = getSprintsWithIssues(searchUrlBuider, isBreakdown);
        for (JIRASprint sprint : sprints) {
            ets.add((ExternalTask)sprint);
        }
        return ets;
    }

    private List<ExternalTask> toExternalTasks(List<JIRAEpic> list) {
        List<ExternalTask> ets = new ArrayList<>();
        for (JIRAEpic epic : list) {
            ets.add((ExternalTask)epic);
        }
        return ets;
    }

    public List<JIRAIssue> getIssues(String projectKey, String issueType) {
        Map<String, Boolean> map = new HashMap<>();
        map.put(issueType, true);
        return getIssues(projectKey, map, true);
    }

    private List<JIRAEpic> getEpicsWithIssues(String projectKey, Map<String, Boolean> issuesToImport,
            boolean isBreakdown)
    {
        List<JIRAEpic> list = new ArrayList<>();
        issuesToImport.put(JIRAConstants.JIRA_ISSUE_EPIC, true);
        List<JIRAIssue> allIssues = getIssues(projectKey, issuesToImport, false);
        List<JIRAIssue> issues = new ArrayList<>();
        for (JIRAIssue is : allIssues) {
            if ("Epic".equals(is.getType())) {

                JIRAEpic je = new JIRAEpic(is.getIssueName(), is.getType(), is.getKey(), is.getStatusName(),
                        is.getScheduledDuration(), is.getScheduledFinishDate(), is.getScheduledDuration(),
                        is.getScheduledEffort(), is.getActualStart(), is.getPercentComplete(), is.getActualFinish(),
                        is.getPredecessors(), is.getRole(), is.getResources(), is.getCreatedDate(), is.getUpdatedDate(),
                        is.getSubTasks(), is.getEpicLink());
                je.setBreakdown(isBreakdown);
                list.add(je);
            } else {
                issues.add(is);
            }

        }

        for (JIRAIssue ji : issues) {
            String el = ji.getEpicLink();
            for (JIRAEpic e : list) {
                if (el.equals(e.getKey())) {
                    List<JIRAIssue> subTasks = e.getSubTasks();
                    if (subTasks == null) {
                        subTasks = new ArrayList<>();
                    }
                    subTasks.add(ji);
                    e.setSubTasks(subTasks);
                }
            }
        }

        return list;
    }

/*    public List<JIRAIssueType> getIssuetypes() {

        ClientResponse response = wrapper.sendGet(baseUri + JIRAConstants.ISSUETYPES_SUFFIX);
        String jsonStr = response.getEntity(String.class);
        List<JIRAIssueType> list = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(jsonStr);
            for (int i = 0; i < jsonArray.length(); i++) {

                JSONObject obj = jsonArray.getJSONObject(i);

                if (!obj.getBoolean("subtask")) {
                    JIRAIssueType jit = new JIRAIssueType(obj.getString("self"), obj.getString("id"),
                            obj.getString("description"), obj.getString("iconUrl"), obj.getString("name"),
                            obj.getBoolean("subtask"), obj.getInt("avatarId"));

                    list.add(jit);
                }
            }
        } catch (JSONException e) {
            logger.error("", e);
        }
        return list;

    }*/

 /*   public Map<String, Map<String, Long>> getJIRATempoWorklogs(String username, XMLGregorianCalendar dateFrom,
            XMLGregorianCalendar dateTo, String projectKey)
    {
        String requestParameter =
                "?dateFrom=" + dateFrom.toString().substring(0, 10) + "&dateTo=" + dateTo.toString().substring(0, 10);

        ClientResponse response = wrapper.sendGet(baseUri + JIRAConstants.TEMPO_WORKLOGS_SUFFIX + requestParameter);
        String jsonStr = response.getEntity(String.class);
        List<JIRATempoWorklog> jtls = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(jsonStr);
            for (int i = 0; i < jsonArray.length(); i++) {

                JSONObject obj = jsonArray.getJSONObject(i);

                Long timeSpentSeconds = obj.getLong("timeSpentSeconds");
                String dateStarted = obj.getString("dateStarted");
                String comment = obj.getString("comment");
                String self = obj.getString("self");
                String id = obj.getString("id");

                JSONObject issue = obj.getJSONObject("issue");
                String isuSelf = issue.getString("self");
                String isuId = issue.getString("id");
                Long remainingEstimateSeconds = issue.getLong("remainingEstimateSeconds");
                String isuProId = issue.getString("projectId");
                String isuKey = issue.getString("key");
                String isuType = issue.getJSONObject("issueType").getString("name");
                String isuSummary = issue.getString("summary");

                JIRATempoIssue jti = new JIRATempoIssue(isuSelf, isuId, isuProId, isuKey, remainingEstimateSeconds,
                        isuType, isuSummary);

                JIRATempoWorklog jtw = new JIRATempoWorklog(timeSpentSeconds, dateStarted, comment, self, id, jti);
                jtls.add(jtw);

            }
        } catch (JSONException e) {
            logger.error("", e);
        }
        return resolveJIRATempoWorklogs(jtls);
    } */

    public Map<String, Map<String, Long>> getJIRATempoWorklogs(XMLGregorianCalendar dateFrom,
            XMLGregorianCalendar dateTo, String projectKey, String author)
    {
        JiraSearchUrlBuilder worklogUrlBuilder = new JiraSearchUrlBuilder(baseUri).setFields("worklog,summary")
                .addAndConstraint("worklogDate>=" + dateFrom.toString().substring(0, 10)).addAndConstraint("worklogDate<="
                + dateTo.toString().substring(0, 10)).addAndConstraint("worklogAuthor=" + encodeForJQLQuery(author));

        if (!StringUtils.isBlank(projectKey)) {
            worklogUrlBuilder.setProjectKey(projectKey);
        }

        ClientResponse response =
                wrapper.sendGet(worklogUrlBuilder.toUrlString());

        String jsonStr = response.getEntity(String.class);
        List<JIRATempoWorklog> jtls = new ArrayList<>();
        try {
            JSONArray issues = new JSONObject(jsonStr).getJSONArray("issues");

            for (int i = 0; i < issues.length(); i++) {
                JSONObject issue = issues.getJSONObject(i);
                JSONObject fields = issue.getJSONObject("fields");

                JSONArray worklogs = fields.getJSONObject("worklog").getJSONArray("worklogs");
                for (int j = 0; j < worklogs.length(); j++) {
                    JSONObject obj = worklogs.getJSONObject(j);
                    // It's possible that a user will authenticate with their email address, not their username.
                    String authorKey = obj.getJSONObject("author").getString("key");
                    String authorEmail = obj.getJSONObject("author").has("emailAddress")? obj.getJSONObject("author").getString("emailAddress") : null;
                    if (author.equalsIgnoreCase(authorKey) || author.equalsIgnoreCase(authorEmail)) {
                        Long timeSpentSeconds = obj.getLong("timeSpentSeconds");
                        String started = obj.getString("started");
                        String comment = obj.getString("comment");
                        String self = obj.getString("self");
                        String id = obj.getString("id");
                        String isuSelf = issue.getString("self");
                        String isuId = issue.getString("id");
                        String isuKey = issue.getString("key");
                        String isuSummary = fields.getString("summary");
                        JIRATempoIssue jti = new JIRATempoIssue(isuSelf, isuId, "", isuKey, 0L, "", isuSummary);

                        JIRATempoWorklog jtw = new JIRATempoWorklog(timeSpentSeconds, started, comment, self, id, jti);
                        jtls.add(jtw);
                    }

                }

            }

        } catch (JSONException e) {
            logger.error("", e);
        }

        return resolveJIRATempoWorklogs(jtls);
    }

    public List<JIRAVersion> getVersions(String projectKey) {
        List<JIRAVersion> list = new ArrayList<>();
        String query = (baseUri + JIRAConstants.VERSIONS_SUFFIX).replace(JIRAConstants.REPLACE_PROJECT_KEY, projectKey);
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
                JIRAVersion version = resolveVersion(array.getJSONObject(i));
                list.add(version);
            } catch (JSONException e) {
                logger.error("", e);
            }
        }

        return list;

    }

    private JIRAVersion resolveVersion(JSONObject obj) {
        Class versionClz = JIRAVersion.class;
        JIRAVersion version = null;

        try {
            version = (JIRAVersion)versionClz.newInstance();
            Map<String, Method> methods = new HashMap<>();
            for (Method m : versionClz.getDeclaredMethods()) {
                methods.put(m.getName(), m);

            }
            Iterator<String> i = obj.keys();

            while (i.hasNext()) {
                String key = i.next();
                String methodName = "set" + (key.substring(0, 1).toUpperCase() + key.substring(1, key.length()));
                Method method = methods.get(methodName);
                String parameterTypeName = method.getParameterTypes()[0].getSimpleName();
                if ("String".equals(parameterTypeName)) {
                    method.invoke(version, obj.getString(key));
                }

                if ("boolean".equals(parameterTypeName)) {
                    method.invoke(version, obj.getBoolean(key));
                }

            }
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | JSONException e) {
            logger.error("", e);
        }

        return version;
    }

    private JIRAIssue resolveIssue(JSONObject fields, boolean isSubtask, String scheduledStart, String scheduledFinish,
            String actualFinish, String issueKey, String sprintCustomId, String epicLinkCustomId) throws JSONException
    {
        String name = fields.getString("summary");
        String status = fields.getJSONObject("status").getString("name");
        String type = fields.getJSONObject("issuetype").getString("name");
        Object assignee = fields.has("assignee") ? fields.get("assignee") : "null";
        String resources = "null".equals(assignee.toString()) ? "" : ((JSONObject)assignee).getString("displayName");
        String createdDate = fields.has("created") ? fields.getString("created") : "";
        String updatedDate = fields.has("updated") ? fields.getString("updated") : "";
        String epicLink = fields.getString(epicLinkCustomId);
        if (fields.has("resolutionDate")) {
            Object resolutionDate = fields.get("resolutionDate");
            actualFinish = "null".equals(resolutionDate.toString()) ? null : (String)resolutionDate;
        }
        Object timeestimate = fields.has("timeestimate") ? fields.getString("timeestimate") : "null";

        Long scheduledEffort = 0L;
        String total = null;
        String percentComplete = null;
        List<JIRAIssue> children = new ArrayList<>();
        if (!isSubtask) {

            Map<String, String> sprintCustomfield = null;
            if (!"null".equals(fields.get(sprintCustomId).toString())) {
                sprintCustomfield = resolveSprintCustomfield(fields.getJSONArray(sprintCustomId).getString(0));
                scheduledStart = sprintCustomfield.get("startDate");
                scheduledFinish = sprintCustomfield.get("endDate");
            }

            JSONObject progressObj = fields.has("progress") ? fields.getJSONObject("progress") : null;
            if (progressObj != null) {
                String progress = progressObj.getString("progress");
                total = progressObj.getString("total");
                scheduledEffort = Long.parseLong(total);
                percentComplete =
                        "0".equals(total) ? "0" : (Double.parseDouble(progress) / Integer.parseInt(total) * 100) + "";
            }
        }
        return new JIRAIssue(name, type, issueKey, status, scheduledStart, scheduledFinish, null, scheduledEffort, null,
                percentComplete, actualFinish, null, null, resources, createdDate, updatedDate, children, epicLink);

    }

    private Map<JIRASprint, List<JIRAIssue>> resolveIssue(JSONArray jsonIssues, String sprintCustomId,
            String epicLinkCustomId) throws JSONException
    {
        Map<JIRASprint, List<JIRAIssue>> map = new LinkedHashMap<>();
        for (int i = 0; i < jsonIssues.length(); i++) {
            JSONObject fields = jsonIssues.getJSONObject(i).getJSONObject("fields");
            String issueKey = jsonIssues.getJSONObject(i).getString("key");
            String name = fields.getString("summary");
            String status = fields.getJSONObject("status").getString("name");
            String type = fields.getJSONObject("issuetype").getString("name");
            Object assignee = fields.has("assignee") ? fields.get("assignee") : "null";
            String resources =
                    "null".equals(assignee.toString()) ? "" : ((JSONObject)assignee).getString("displayName");
            String createdDate = fields.has("created") ? fields.getString("created") : "";
            String updatedDate = fields.has("updated") ? fields.getString("updated") : "";
            String epicLink = fields.getString(epicLinkCustomId);
            String actualFinish = null;
            if (fields.has("resolutiondate")) {
                Object resolutionDate = fields.get("resolutiondate");
                actualFinish = "null".equals(resolutionDate.toString()) ? null : (String)resolutionDate;
            }
            Object timeestimate = fields.has("timeestimate") ? fields.getString("timeestimate") : "null";
            Long scheduledEffort = "null".equals(timeestimate.toString()) ? 0 : Long.parseLong((String)timeestimate);

            String total = null;
            String percentComplete = null;

            Map<String, String> sprintCustomfield = null;



            if (!fields.isNull(sprintCustomId)) {

                sprintCustomfield = resolveSprintCustomfield(fields.getJSONArray(sprintCustomId).getString(0));

                String scheduledStart = sprintCustomfield.get("startDate");
                String scheduledFinish = sprintCustomfield.get("endDate");
                String sprintName = sprintCustomfield.get("name");
                String sprintId = sprintCustomfield.get("id");
                String sprintState = sprintCustomfield.get("state");

                JSONObject progressObj = fields.has("progress") ? fields.getJSONObject("progress") : null;
                if (progressObj != null) {
                    String progress = progressObj.getString("progress");
                    total = progressObj.getString("total");
                    scheduledEffort = Long.parseLong(total);
                    percentComplete =
                            "0".equals(total) ? "0" : (Double.parseDouble(progress) / Integer.parseInt(total) * 100) + "";

                }

                if (scheduledStart.compareTo(createdDate) > 0) {
                    createdDate = scheduledStart;
                }

                JIRAIssue issue = new JIRAIssue(name, type, issueKey, status, scheduledStart, scheduledFinish, null,
                        scheduledEffort, null, percentComplete, actualFinish, null, null, resources, createdDate,
                        updatedDate, null, epicLink);
                JIRASprint sprint = new JIRASprint(sprintId, sprintState, sprintName, scheduledStart, scheduledFinish);

                if (map.containsKey(sprint)) {
                    map.get(sprint).add(issue);
                } else {
                    List<JIRAIssue> issues = new ArrayList<>();
                    issues.add(issue);
                    map.put(sprint, issues);
                }

            }

        }

        return map;
    }

    // Convert SprintCustomfield from String to Map.The example of origin format
    // of sprintCustomfield is
    // "com.atlassian.greenhopper.service.sprint.Sprint@1f39706[id=1,rapidViewId=1,state=ACTIVE,name=SampleSprint
    // 2,goal=<null>,startDate=2016-12-07T06:18:24.224+08:00,endDate=2016-12-21T06:38:24.224+08:00,completeDate=<null>,sequence=1]"
    private static Map<String, String> resolveSprintCustomfield(String sprintCustomfield) {
        Map<String, String> map = new HashMap<>();
        String reg = ".+@.+\\[(.+)]";
        Pattern pattern = Pattern.compile(reg);
        Matcher matcher = pattern.matcher(sprintCustomfield);
        if (matcher.find()) {
            String exp = matcher.group(1);
            String[] kvs = exp.split(",");
            for (String kv : kvs) {
                String[] splited = kv.split("=");
                map.put(splited[0], splited.length == 2 ? splited[1] : null);
            }
        }
        return map;
    }



    // Convert JIRATempoWorklog from list to map, for the map, the key is issue
    // key concating issue summary, the value is also a map whose key is date
    // and
    // value is time spent seconds
    private Map<String, Map<String, Long>> resolveJIRATempoWorklogs(List<JIRATempoWorklog> list) {
        Map<String, Map<String, Long>> map = new HashMap<>();
        for (JIRATempoWorklog jtw : list) {
            String issueKey = jtw.getIssue().getKey() + "	" + jtw.getIssue().getSummary();
            if (map.containsKey(issueKey)) {
                String dateStarted = jtw.getDateStarted().substring(0, 10);
                Map<String, Long> timeSpent = map.get(issueKey);
                if (timeSpent.containsKey(dateStarted)) {
                    timeSpent.put(dateStarted, jtw.getTimeSpentSeconds() + timeSpent.get(dateStarted));
                } else {

                    timeSpent.put(dateStarted, jtw.getTimeSpentSeconds());
                }

            } else {
                Map<String, Long> timeSpentNew = new HashMap<>();
                timeSpentNew.put(jtw.getDateStarted().substring(0, 10), jtw.getTimeSpentSeconds());
                map.put(issueKey, timeSpentNew);
            }
        }

        return map;
    }
    
    /**
     *  The character '@' is a reserved JQL character. You must enclose it in a string or use the escape '\u0040' instead. (line 1, character 82)"
     * @param value
     * @return encoded value
     */
    private String encodeForJQLQuery(String value) {
        if(value.indexOf('@') > -1) {
            // the value should not contains double quote (")
            value = "%22" + value + "%22";
        }
        
        return value;
    }

    private JiraSearchUrlBuilder decorateOrderBySprintCreatedUrl(JiraSearchUrlBuilder urlBuilder) {
        return urlBuilder.setOrderBy(" sprint,created ASC ");
    }

}

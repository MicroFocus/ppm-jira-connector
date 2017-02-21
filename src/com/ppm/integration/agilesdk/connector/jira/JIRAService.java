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
				JIRAProject project = new JIRAProject(obj.getString("expand"), obj.getString("self"),
						obj.getString("id"), obj.getString("key"), obj.getString("name"), null,
						obj.getString("projectTypeKey"));
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

		}
		return list;
	}

	public List<JIRAIssue> getIssues(String projectKey, Map<String, Boolean> map, boolean isInSprint) {
		List<JIRAIssue> list = new ArrayList<>();
		String query = encodeUrl(
				baseUri + JIRAConstants.ISSUES_IN_SPRINT_SUFFIX + projectKey + getIssueQueryString(map));

		// if the Issues To Import contains only epic type
		if (map.get(JIRAConstants.JIRA_ISSUE_EPIC) && map.size() == 1) {
			query = encodeUrl(baseUri + JIRAConstants.EPICS_SUFFIX + projectKey + getIssueQueryString(map));
		}

		if (!isInSprint) {
			query = encodeUrl(baseUri + JIRAConstants.ISSUES_SUFFIX + projectKey + getIssueQueryString(map));
		}

		ClientResponse response = wrapper.sendGet(query);

		String jsonStr = response.getEntity(String.class);
		try {

			JSONObject jsonObj = new JSONObject(jsonStr);

			JSONArray jsonIssues = jsonObj.getJSONArray("issues");
			JSONObject schemas = jsonObj.getJSONObject("schema");
			String sprintCustomId = JIRACustomIdFinder.findId(schemas, JIRAConstants.JIRA_SPRINT_CUSTOM);
			String epicLinkId = JIRACustomIdFinder.findId(schemas, JIRAConstants.JIRA_EPIC_LINK_CUSTOM);
			for (int i = 0; i < jsonIssues.length(); i++) {
				JSONObject fields = jsonIssues.getJSONObject(i).getJSONObject("fields");
				String issueKey = jsonIssues.getJSONObject(i).getString("key");
				boolean isSubTask = fields.getJSONObject("issuetype").getBoolean("subtask");
				if (!isSubTask) {
					JIRAIssue jiraIssue = resolveIssue(fields, false, null, null, null, issueKey, sprintCustomId,
							epicLinkId);
					list.add(jiraIssue);
				}

			}
		} catch (JSONException e) {

		}
		return list;
	}

	public List<JIRASprint> getSprintsWithIssues(String query, boolean isBreakdown) {
		List<JIRASprint> sprints = new ArrayList<>();
		ClientResponse response = wrapper.sendGet(query);
		String jsonStr = response.getEntity(String.class);
		try {

			JSONObject jsonObj = new JSONObject(jsonStr);

			JSONArray jsonIssues = jsonObj.getJSONArray("issues");
			JSONObject schemas = jsonObj.getJSONObject("schema");
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

		} catch (JSONException e) {

		}

		return sprints;
	}

	public List<ExternalTask> getExternalTasks(String projectKey, Map<String, Boolean> map, String queryCase,
			String attachedArg, boolean isBreakdown) {
		List<ExternalTask> ets = null;
		String query = baseUri + JIRAConstants.ISSUES_IN_SPRINT_SUFFIX + projectKey + getIssueQueryString(map);
		switch (queryCase) {
		case JIRAConstants.KEY_EPIC:
			String epicLinkCustomId = "";
			ClientResponse response = wrapper.sendGet(encodeUrl(query));

			String jsonStr = response.getEntity(String.class);

			try {

				JSONObject jsonObj = new JSONObject(jsonStr);
				JSONObject schemas = jsonObj.getJSONObject("schema");
				epicLinkCustomId = JIRACustomIdFinder.findId(schemas, JIRAConstants.JIRA_EPIC_LINK_CUSTOM);
				epicLinkCustomId = epicLinkCustomId.substring(epicLinkCustomId.indexOf("_") + 1);
			} catch (JSONException e) {

			}

			if (!"".equals(attachedArg)) {
				query += " and cf[" + epicLinkCustomId + "]=" + attachedArg;
			} else {
				query += " and cf[" + epicLinkCustomId + "]!=null";
			}
			ets = toExternalTasks(query, isBreakdown);
			break;
		case JIRAConstants.KEY_ALL_EPICS:
			List<JIRAEpic> list = getEpicsWithIssues(projectKey, map, isBreakdown);
			ets = toExternalTasks(list);
			break;
		case JIRAConstants.KEY_VERSION:

			if (!"".equals(attachedArg)) {
				query += " and fixVersion=" + attachedArg;
			} else {
				query += " and fixVersion!=null";
			}
			ets = toExternalTasks(query, isBreakdown);
			break;
		case JIRAConstants.KEY_ALL_PROJECT_PLANNED_ISSUES:
			ets = toExternalTasks(query, isBreakdown);
		}
		return ets;
	}

	private List<ExternalTask> toExternalTasks(String query, boolean isBreakdown) {
		List<ExternalTask> ets = new ArrayList<>();
		List<JIRASprint> sprints = getSprintsWithIssues(encodeUrl(query), isBreakdown);
		for (JIRASprint sprint : sprints) {
			ets.add((ExternalTask) sprint);
		}
		return ets;
	}

	private List<ExternalTask> toExternalTasks(List<JIRAEpic> list) {
		List<ExternalTask> ets = new ArrayList<>();
		for (JIRAEpic epic : list) {
			ets.add((ExternalTask) epic);
		}
		return ets;
	}

	public List<JIRAIssue> getIssues(String projectKey, String issueType) {
		Map<String, Boolean> map = new HashMap<>();
		map.put(issueType, true);
		return getIssues(projectKey, map, true);
	}

	public List<JIRAEpic> getEpicsWithIssues(String projectKey, Map<String, Boolean> issuesToImport,
			boolean isBreakdown) {
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

	public List<JIRAIssueType> getIssuetypes() {

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

		}
		return list;

	}

	public Map<String, Map<String, Long>> getJIRATempoWorklogs(String username, XMLGregorianCalendar dateFrom,
			XMLGregorianCalendar dateTo, String projectKey) {
		String requestParameter = "?dateFrom=" + dateFrom.toString().substring(0, 10) + "&dateTo="
				+ dateTo.toString().substring(0, 10);

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

		}
		return resolveJIRATempoWorklogs(jtls);
	}

	public Map<String, Map<String, Long>> getJIRATempoWorklogs(XMLGregorianCalendar dateFrom,
			XMLGregorianCalendar dateTo, String projectKey, String author) {

		String requestParameter = "worklogDate>=" + dateFrom.toString().substring(0, 10) + " and worklogDate<="
				+ dateTo.toString().substring(0, 10) + " and worklogAuthor=" + author;

		requestParameter += "".equals(projectKey) ? "" : " and project=" + projectKey;

		ClientResponse response = wrapper
				.sendGet(baseUri + JIRAConstants.ISSUES_WORKLOGS_SUFFIX + encodeUrl(requestParameter));

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
					String authorKey = obj.getJSONObject("author").getString("key");
					if (authorKey.equals(author)) {
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

		}

		for (int i = 0; i < array.length(); i++) {
			try {
				JIRAVersion version = resolveVersion(array.getJSONObject(i));
				list.add(version);
			} catch (JSONException e) {

			}
		}

		return list;

	}

	private JIRAVersion resolveVersion(JSONObject obj) {
		Class versionClz = JIRAVersion.class;
		JIRAVersion version = null;

		try {
			version = (JIRAVersion) versionClz.newInstance();
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

		}

		return version;
	}

	private JIRAIssue resolveIssue(JSONObject fields, boolean isSubtask, String scheduledStart, String scheduledFinish,
			String actualFinish, String issueKey, String sprintCustomId, String epicLinkCustomId) throws JSONException {
		String name = fields.getString("summary");
		String status = fields.getJSONObject("status").getString("name");
		String type = fields.getJSONObject("issuetype").getString("name");
		Object assignee = fields.has("assignee") ? fields.get("assignee") : "null";
		String resources = "null".equals(assignee.toString()) ? "" : ((JSONObject) assignee).getString("displayName");
		String createdDate = fields.has("created") ? fields.getString("created") : "";
		String updatedDate = fields.has("updated") ? fields.getString("updated") : "";
		String epicLink = fields.getString(epicLinkCustomId);
		if (fields.has("resolutionDate")) {
			Object resolutionDate = fields.get("resolutionDate");
			actualFinish = "null".equals(resolutionDate.toString()) ? null : (String) resolutionDate;
		}
		Object timeestimate = fields.has("timeestimate") ? fields.getString("timeestimate") : "null";

		Long scheduledEffort = 0L;
		String total = null;
		String percentComplete = null;
		List<JIRAIssue> children = null;
		if (!isSubtask) {

			Map<String, String> sprintCustomfield = null;
			if (!"null".equals(fields.get(sprintCustomId).toString())) {
				sprintCustomfield = resolveSprintCustomfield(fields.getJSONArray(sprintCustomId).getString(0));
				scheduledStart = sprintCustomfield.get("startDate");
				scheduledFinish = sprintCustomfield.get("endDate");
			}

			children = new ArrayList<>();
			JSONArray childrenJson = fields.getJSONArray("subtasks");
			for (int i = 0; i < childrenJson.length(); i++) {
				String key = childrenJson.getJSONObject(i).getString("key");
				JSONObject subfields = childrenJson.getJSONObject(i).getJSONObject("fields");
				JIRAIssue subIssue = resolveIssue(subfields, true, scheduledStart, scheduledFinish, actualFinish, key,
						sprintCustomId, epicLinkCustomId);
				children.add(subIssue);
			}
			JSONObject progressObj = fields.has("progress") ? fields.getJSONObject("progress") : null;
			if (progressObj != null) {
				String progress = progressObj.getString("progress");
				total = progressObj.getString("total");
				scheduledEffort = Long.parseLong(total);
				percentComplete = "0".equals(total) ? "0"
						: (Double.parseDouble(progress) / Integer.parseInt(total) * 100) + "";
			}
		}
		return new JIRAIssue(name, type, issueKey, status, scheduledStart, scheduledFinish, null, scheduledEffort, null,
				percentComplete, actualFinish, null, null, resources, createdDate, updatedDate, children, epicLink);

	}

	private Map<JIRASprint, List<JIRAIssue>> resolveIssue(JSONArray jsonIssues, String sprintCustomId,
			String epicLinkCustomId) throws JSONException {
		Map<JIRASprint, List<JIRAIssue>> map = new LinkedHashMap<>();
		for (int i = 0; i < jsonIssues.length(); i++) {
			JSONObject fields = jsonIssues.getJSONObject(i).getJSONObject("fields");
			String issueKey = jsonIssues.getJSONObject(i).getString("key");
			String name = fields.getString("summary");
			String status = fields.getJSONObject("status").getString("name");
			String type = fields.getJSONObject("issuetype").getString("name");
			Object assignee = fields.has("assignee") ? fields.get("assignee") : "null";
			String resources = "null".equals(assignee.toString()) ? ""
					: ((JSONObject) assignee).getString("displayName");
			String createdDate = fields.has("created") ? fields.getString("created") : "";
			String updatedDate = fields.has("updated") ? fields.getString("updated") : "";
			String epicLink = fields.getString(epicLinkCustomId);
			String actualFinish = null;
			if (fields.has("resolutiondate")) {
				Object resolutionDate = fields.get("resolutiondate");
				actualFinish = "null".equals(resolutionDate.toString()) ? null : (String) resolutionDate;
			}
			Object timeestimate = fields.has("timeestimate") ? fields.getString("timeestimate") : "null";
			Long scheduledEffort = "null".equals(timeestimate.toString()) ? 0 : Long.parseLong((String) timeestimate);

			String total = null;
			String percentComplete = null;

			Map<String, String> sprintCustomfield = null;

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
				percentComplete = "0".equals(total) ? "0"
						: (Double.parseDouble(progress) / Integer.parseInt(total) * 100) + "";
			}

			if (scheduledStart.compareTo(createdDate) == 1) {
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

		return map;
	}

	// Convert SprintCustomfield from String to Map.The example of origin format
	// of sprintCustomfield is
	// "com.atlassian.greenhopper.service.sprint.Sprint@1f39706[id=1,rapidViewId=1,state=ACTIVE,name=SampleSprint
	// 2,goal=<null>,startDate=2016-12-07T06:18:24.224+08:00,endDate=2016-12-21T06:38:24.224+08:00,completeDate=<null>,sequence=1]"
	private Map<String, String> resolveSprintCustomfield(String sprintCustomfield) {
		Map<String, String> map = new HashMap<>();
		String reg = ".+@.+\\[(.+=.*),(.+=.*),(.+=.*),(.+=.*),(.+=.*),(.+=.*),(.+=.*),(.+=.*),(.+=.*)\\]";
		Pattern pattern = Pattern.compile(reg);
		Matcher matcher = pattern.matcher(sprintCustomfield);
		if (matcher.find()) {
			for (int i = 1; i <= matcher.groupCount(); i++) {
				String exp = matcher.group(i);
				String[] splited = exp.split("=");
				map.put(splited[0], splited.length == 2 ? splited[1] : null);
			}
		}
		return map;
	}

	private String getIssueQueryString(Map<String, Boolean> map) {
		String query = "";

		Set<Entry<String, Boolean>> set = map.entrySet();
		for (Entry<String, Boolean> e : set) {
			if (e.getValue()) {
				query += e.getKey() + ",";
			}
		}

		if (!"".equals(query)) {
			query = " and issuetype in(" + query.substring(0, query.length() - 1) + ")";
		}

		return query.replaceAll(" ", "%20");
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

	private String encodeUrl(String url) {
		return url.replaceAll(" ", "%20").replaceAll(">", "%3E").replaceAll("<", "%3C").replaceAll("-", "%2D")
				.replaceAll("!", "%21");
	}

}

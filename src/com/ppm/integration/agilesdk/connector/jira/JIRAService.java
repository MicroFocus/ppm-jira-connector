package com.ppm.integration.agilesdk.connector.jira;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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

import com.ppm.integration.agilesdk.connector.jira.model.JIRAIssue;
import com.ppm.integration.agilesdk.connector.jira.model.JIRAIssueType;
import com.ppm.integration.agilesdk.connector.jira.model.JIRAProject;
import com.ppm.integration.agilesdk.connector.jira.model.JIRATempoIssue;
import com.ppm.integration.agilesdk.connector.jira.model.JIRATempoWorklog;
import com.ppm.integration.agilesdk.connector.jira.rest.util.RestWrapper;

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

	public List<JIRAIssue> getIssues(String projectKey, Map<String, Boolean> map) {
		List<JIRAIssue> list = new ArrayList<>();

		ClientResponse response = wrapper
				.sendGet(baseUri + JIRAConstants.ISSUES_SUFFIX + projectKey + getIssueQueryString(map));

		String jsonStr = response.getEntity(String.class);
		try {

			JSONObject jsonObj = new JSONObject(jsonStr);

			JSONArray jsonIssues = jsonObj.getJSONArray("issues");
			JSONObject schemas = jsonObj.getJSONObject("schema");
			String sprintCustomId = findSprintCustomId(schemas);

			for (int i = 0; i < jsonIssues.length(); i++) {
				JSONObject fields = jsonIssues.getJSONObject(i).getJSONObject("fields");
				boolean isSubTask = fields.getJSONObject("issuetype").getBoolean("subtask");
				if (!isSubTask) {
					JIRAIssue jiraIssue = resolveIssue(fields, false, null, null, null, sprintCustomId);
					list.add(jiraIssue);
				}

			}
		} catch (JSONException e) {

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
			XMLGregorianCalendar dateTo, String projectKey) {

		String requestParameter = "worklogDate>=" + dateFrom.toString().substring(0, 10) + " and worklogDate<="
				+ dateTo.toString().substring(0, 10);

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

		} catch (JSONException e) {

		}

		return resolveJIRATempoWorklogs(jtls);
	}

	private JIRAIssue resolveIssue(JSONObject fields, boolean isSubtask, String scheduledStart, String scheduledFinish,
			String actualFinish, String sprintCustomId) throws JSONException {
		String name = fields.getString("summary");
		String status = fields.getJSONObject("status").getString("name");
		String type = fields.getJSONObject("issuetype").getString("name");
		Object assignee = fields.get("assignee");
		String resources = "null".equals(assignee.toString()) ? "" : ((JSONObject) assignee).getString("displayName");
		String createdDate = fields.getString("created");
		String updatedDate = fields.getString("updated");
		if (fields.has("resolutionDate")) {
			Object resolutionDate = fields.get("resolutionDate");
			actualFinish = "null".equals(resolutionDate.toString()) ? null : (String) resolutionDate;
		}
		Object timeestimate = fields.get("timeestimate");
		Long scheduledEffort = "null".equals(timeestimate.toString()) ? 0 : (Long) timeestimate;

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
				JSONObject subfields = childrenJson.getJSONObject(i).getJSONObject("fields");
				JIRAIssue subIssue = resolveIssue(subfields, true, scheduledStart, scheduledFinish, actualFinish,
						sprintCustomId);
				children.add(subIssue);
			}
			JSONObject progressObj = fields.getJSONObject("progress");
			String progress = progressObj.getString("progress");
			total = progressObj.getString("total");
			percentComplete = "0".equals(total) ? "0"
					: (Double.parseDouble(progress) / Integer.parseInt(total) * 100) + "";
		}
		return new JIRAIssue(name, type, status, scheduledStart, scheduledFinish, null, scheduledEffort, null,
				percentComplete, actualFinish, null, null, resources, createdDate, updatedDate, children);

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
		return url.replaceAll(" ", "%20").replaceAll(">", "%3E").replaceAll("<", "%3C");
	}

	// Different Jira Instance has different customfield Id including sprint
	// customfield, so the sprint id needs to be found.
	private String findSprintCustomId(JSONObject schemas) {
		Iterator<String> i = schemas.keys();
		String sprintCutomId = "";
		while (i.hasNext()) {
			String key = i.next();
			try {
				String custom = schemas.getJSONObject(key).getString("custom");
				if (JIRAConstants.JIRA_SPRINT_CUSTOM.equals(custom)) {
					sprintCutomId = key;
				}
			} catch (JSONException e) {

			}

		}
		return sprintCutomId;
	}
}

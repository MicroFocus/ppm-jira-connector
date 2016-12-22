package com.ppm.integration.agilesdk.connector.jira;

import java.util.ArrayList;
import java.util.HashMap;
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

			for (int i = 0; i < jsonIssues.length(); i++) {
				JSONObject fields = jsonIssues.getJSONObject(i).getJSONObject("fields");
				boolean isSubTask = fields.getJSONObject("issuetype").getBoolean("subtask");
				if (!isSubTask) {
					JIRAIssue jiraIssue = resolveIssue(fields, false, null, null, null);
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

					// String isuProId = issue.getString("projectId");
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
			String actualFinish) throws JSONException {
		String name = fields.getString("summary");
		String status = fields.getJSONObject("status").getString("name");
		String type = fields.getJSONObject("issuetype").getString("name");

		String total = null;
		String percentComplete = null;
		// String timespent = fields.getString("timespent");
		List<JIRAIssue> children = null;
		if (!isSubtask) {

			Map<String, String> customfield_10118 = null;
			if (!"null".equals(fields.get("customfield_10118").toString())) {
				customfield_10118 = resolveCustomfield_10118(fields.getJSONArray("customfield_10118").getString(0));
				scheduledStart = customfield_10118.get("startDate");
				scheduledFinish = customfield_10118.get("endDate");
				actualFinish = customfield_10118.get("completeDate");
			}

			children = new ArrayList<>();
			JSONArray childrenJson = fields.getJSONArray("subtasks");
			for (int i = 0; i < childrenJson.length(); i++) {
				JSONObject subfields = childrenJson.getJSONObject(i).getJSONObject("fields");
				JIRAIssue subIssue = resolveIssue(subfields, true, scheduledStart, scheduledFinish, actualFinish);
				children.add(subIssue);
			}
			JSONObject progressObj = fields.getJSONObject("progress");
			String progress = progressObj.getString("progress");
			total = progressObj.getString("total");
			percentComplete = "0".equals(total) ? "0"
					: (Double.parseDouble(progress) / Integer.parseInt(total) * 100) + "";
		}
		return new JIRAIssue(name, type, status, scheduledStart, scheduledFinish, null, total, null, percentComplete,
				actualFinish, null, null, null, children);
	}

	private Map<String, String> resolveCustomfield_10118(String customfield_10118) {
		Map<String, String> map = new HashMap<>();
		String reg = ".+@.+\\[(.+=.*),(.+=.*),(.+=.*),(.+=.*),(.+=.*),(.+=.*),(.+=.*),(.+=.*),(.+=.*)\\]";
		Pattern pattern = Pattern.compile(reg);
		Matcher matcher = pattern.matcher(customfield_10118);
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

}

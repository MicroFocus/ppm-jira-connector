package com.hp.ppm.integration.jira;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.wink.client.ClientResponse;
import org.apache.wink.client.Resource;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.hp.ppm.integration.jira.model.JIRAIssue;
import com.hp.ppm.integration.jira.model.JIRAIssueType;
import com.hp.ppm.integration.jira.model.JIRAProject;
import com.hp.ppm.integration.jira.rest.util.RestWrapper;

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
		Resource resource = wrapper.getJIRAResource(baseUri + JIRAConstants.PROJECT_SUFFIX);
		ClientResponse response = resource.get();

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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return list;
	}

	public List<JIRAIssue> getIssues(String projectKey, Map<String, Boolean> map) {
		List<JIRAIssue> list = new ArrayList<>();

		Resource resource = wrapper
				.getJIRAResource(baseUri + JIRAConstants.ISSUES_SUFFIX + projectKey + getIssueQueryString(map));

		ClientResponse response = resource.get();

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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return list;
	}

	public List<JIRAIssueType> getIssuetypes() {

		Resource resource = wrapper.getJIRAResource(baseUri + JIRAConstants.ISSUETYPES_SUFFIX);
		ClientResponse response = resource.get();
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return list;

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
}

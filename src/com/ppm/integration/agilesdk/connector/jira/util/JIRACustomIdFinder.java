package com.ppm.integration.agilesdk.connector.jira.util;

import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

public class JIRACustomIdFinder {
	// Different Jira Instance has different customfield Id
	public static String findId(JSONObject schemas, String customConstant) {
		Iterator<String> i = schemas.keys();
		String sprintCutomId = "";
		while (i.hasNext()) {
			String key = i.next();
			try {
				String custom = schemas.getJSONObject(key).getString("custom");
				if (customConstant.equals(custom)) {
					sprintCutomId = key;
					break;
				}
			} catch (JSONException e) {

			}

		}
		return sprintCutomId;
	}
}

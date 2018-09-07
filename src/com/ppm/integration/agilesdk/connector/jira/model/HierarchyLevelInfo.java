package com.ppm.integration.agilesdk.connector.jira.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HierarchyLevelInfo {

    private String title;
    private String[] issueTypeIds;
    private long id;

    private HierarchyLevelInfo() {}

    public static HierarchyLevelInfo fromJSon(JSONObject obj) throws JSONException {

        HierarchyLevelInfo info = new HierarchyLevelInfo();

        info.id = obj.getLong("id");
        info.title = obj.getString("title");
        JSONArray typeIds = obj.getJSONArray("issueTypeIds");
        info.issueTypeIds = new String[typeIds.length()];
        for (int i = 0 ; i < typeIds.length() ; i++) {
            info.issueTypeIds[i] = typeIds.getString(i);
        }

        return info;
    }

    public String getTitle() {
        return title;
    }

    public String[] getIssueTypeIds() {
        return issueTypeIds;
    }

    public long getId() {
        return id;
    }
}

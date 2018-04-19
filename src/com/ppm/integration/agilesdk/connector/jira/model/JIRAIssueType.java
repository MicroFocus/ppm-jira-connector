
package com.ppm.integration.agilesdk.connector.jira.model;

import org.json.JSONException;
import org.json.JSONObject;

public class JIRAIssueType {

    private String id;

    private String name;

    private String description;


    public static JIRAIssueType fromJSONObject(JSONObject obj) {
        try {
            JIRAIssueType issueType = new JIRAIssueType();
            issueType.setName(obj.getString("name"));
            issueType.setId(obj.getString("id"));
            issueType.setDescription(obj.getString("description"));
            return issueType;
        } catch (JSONException e) {
            throw new RuntimeException("Error while reading JSon defintion of Issue Type", e);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

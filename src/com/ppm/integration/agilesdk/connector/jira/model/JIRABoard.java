package com.ppm.integration.agilesdk.connector.jira.model;

/**
 * Created by canaud on 7/20/2017.
 */
public class JIRABoard extends JIRAEntity {

    private String id;

    private String type;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}

package com.ppm.integration.agilesdk.connector.jira.model;

import java.util.HashMap;

public class JIRAProject {
	private String expand;
	private String self;
	private String id;
	private String key;
	private String name;
	private HashMap<String, String> avatarUrls;
	private String projectKey;

	public JIRAProject(String expand, String self, String id, String key, String name,
			HashMap<String, String> avatarUrls, String projectKey) {
		this.expand = expand;
		this.self = self;
		this.id = id;
		this.key = key;
		this.name = name;
		this.avatarUrls = avatarUrls;
		this.projectKey = projectKey;
	}

	public String getExpand() {
		return expand;
	}

	public void setExpand(String expand) {
		this.expand = expand;
	}

	public String getSelf() {
		return self;
	}

	public void setSelf(String self) {
		this.self = self;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public HashMap<String, String> getAvatarUrls() {
		return avatarUrls;
	}

	public void setAvatarUrls(HashMap<String, String> avatarUrls) {
		this.avatarUrls = avatarUrls;
	}

	public String getProjectKey() {
		return projectKey;
	}

	public void setProjectKey(String projectKey) {
		this.projectKey = projectKey;
	}

}

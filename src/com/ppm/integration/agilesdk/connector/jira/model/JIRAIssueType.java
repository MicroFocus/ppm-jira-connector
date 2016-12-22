package com.ppm.integration.agilesdk.connector.jira.model;

public class JIRAIssueType {
	private String self;
	private String id;
	private String description;
	private String iconUrl;
	private String name;
	private boolean subtask;
	private Integer avatarId;

	public JIRAIssueType(String self, String id, String description, String iconUrl, String name, boolean subtask,
			Integer avatarId) {
		this.self = self;
		this.id = id;
		this.description = description;
		this.iconUrl = iconUrl;
		this.name = name;
		this.subtask = subtask;
		this.avatarId = avatarId;
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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getIconUrl() {
		return iconUrl;
	}

	public void setIconUrl(String iconUrl) {
		this.iconUrl = iconUrl;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isSubtask() {
		return subtask;
	}

	public void setSubtask(boolean subtask) {
		this.subtask = subtask;
	}

	public Integer getAvatarId() {
		return avatarId;
	}

	public void setAvatarId(Integer avatarId) {
		this.avatarId = avatarId;
	}

}

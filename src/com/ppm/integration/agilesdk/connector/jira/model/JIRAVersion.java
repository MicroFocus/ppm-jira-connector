package com.ppm.integration.agilesdk.connector.jira.model;

public class JIRAVersion {
	private String id;
	private String self;
	private String name;
	private boolean archived;
	private boolean released;
	private String startDate;
	private String releaseDate;
	private boolean overdue;
	private String userStartDate;
	private String userReleaseDate;
	private String projectId;
	private String description;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSelf() {
		return self;
	}

	public void setSelf(String self) {
		this.self = self;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isArchived() {
		return archived;
	}

	public void setArchived(boolean archived) {
		this.archived = archived;
	}

	public boolean isReleased() {
		return released;
	}

	public void setReleased(boolean released) {
		this.released = released;
	}

	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public String getReleaseDate() {
		return releaseDate;
	}

	public void setReleaseDate(String releaseDate) {
		this.releaseDate = releaseDate;
	}

	public boolean isOverdue() {
		return overdue;
	}

	public void setOverdue(boolean overdue) {
		this.overdue = overdue;
	}

	public String getUserStartDate() {
		return userStartDate;
	}

	public void setUserStartDate(String userStartDate) {
		this.userStartDate = userStartDate;
	}

	public String getUserReleaseDate() {
		return userReleaseDate;
	}

	public void setUserReleaseDate(String userReleaseDate) {
		this.userReleaseDate = userReleaseDate;
	}

	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}

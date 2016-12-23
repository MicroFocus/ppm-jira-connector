package com.ppm.integration.agilesdk.connector.jira.model;

public class JIRATempoIssue {
	private String self;
	private String id;
	private String projectId;
	private String key;
	private Long remainingEstimateSeconds;
	private String issueType;
	private String summary;

	public JIRATempoIssue(String self, String id, String projectId, String key, Long remainingEstimateSeconds,
			String issueType, String summary) {
		this.self = self;
		this.id = id;
		this.projectId = projectId;
		this.key = key;
		this.remainingEstimateSeconds = remainingEstimateSeconds;
		this.issueType = issueType;
		this.summary = summary;
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

	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Long getRemainingEstimateSeconds() {
		return remainingEstimateSeconds;
	}

	public void setRemainingEstimateSeconds(Long remainingEstimateSeconds) {
		this.remainingEstimateSeconds = remainingEstimateSeconds;
	}

	public String getIssueType() {
		return issueType;
	}

	public void setIssueType(String issueType) {
		this.issueType = issueType;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

}

package com.ppm.integration.agilesdk.connector.jira.model;

public class JIRATempoWorklog {
	private Long timeSpentSeconds;
	private String dateStarted;
	private String comment;
	private String self;
	private String id;
	private JIRATempoIssue issue;

	public JIRATempoWorklog(Long timeSpentSeconds, String dateStarted, String comment, String self, String id,
			JIRATempoIssue issue) {
		this.timeSpentSeconds = timeSpentSeconds;
		this.dateStarted = dateStarted;
		this.comment = comment;
		this.self = self;
		this.id = id;
		this.issue = issue;
	}

	public Long getTimeSpentSeconds() {
		return timeSpentSeconds;
	}

	public void setTimeSpentSeconds(Long timeSpentSeconds) {
		this.timeSpentSeconds = timeSpentSeconds;
	}

	public String getDateStarted() {
		return dateStarted;
	}

	public void setDateStarted(String dateStarted) {
		this.dateStarted = dateStarted;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
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

	public JIRATempoIssue getIssue() {
		return issue;
	}

	public void setIssue(JIRATempoIssue issue) {
		this.issue = issue;
	}

}

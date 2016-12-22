package com.hp.ppm.integration.jira.model;

import java.util.List;

public class JIRAIssue {
	private String name;
	private String type;
	private String status;
	private String scheduledStart;
	private String scheduledFinish;
	private String scheduledDuration;
	private String scheduledEffort;
	private String actualStart;
	private String percentComplete;
	private String actualFinish;
	private String predecessors;
	private String role;
	private String resources;
	private List<JIRAIssue> children;

	public JIRAIssue(String name, String type, String status, String scheduledStart, String scheduledFinish,
			String scheduledDuration, String scheduledEffort, String actualStart, String percentComplete,
			String actualFinish, String predecessors, String role, String resources, List<JIRAIssue> children) {
		this.name = name;
		this.type = type;
		this.status = status;
		this.scheduledStart = scheduledStart;
		this.scheduledFinish = scheduledFinish;
		this.scheduledDuration = scheduledDuration;
		this.scheduledEffort = scheduledEffort;
		this.actualStart = actualStart;
		this.percentComplete = percentComplete;
		this.actualFinish = actualFinish;
		this.predecessors = predecessors;
		this.role = role;
		this.resources = resources;
		this.children = children;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getScheduledStart() {
		return scheduledStart;
	}

	public void setScheduledStart(String scheduledStart) {
		this.scheduledStart = scheduledStart;
	}

	public String getScheduledFinish() {
		return scheduledFinish;
	}

	public void setScheduledFinish(String scheduledFinish) {
		this.scheduledFinish = scheduledFinish;
	}

	public String getScheduledDuration() {
		return scheduledDuration;
	}

	public void setScheduledDuration(String scheduledDuration) {
		this.scheduledDuration = scheduledDuration;
	}

	public String getScheduledEffort() {
		return scheduledEffort;
	}

	public void setScheduledEffort(String scheduledEffort) {
		this.scheduledEffort = scheduledEffort;
	}

	public String getActualStart() {
		return actualStart;
	}

	public void setActualStart(String actualStart) {
		this.actualStart = actualStart;
	}

	public String getPercentComplete() {
		return percentComplete;
	}

	public void setPercentComplete(String percentComplete) {
		this.percentComplete = percentComplete;
	}

	public String getActualFinish() {
		return actualFinish;
	}

	public void setActualFinish(String actualFinish) {
		this.actualFinish = actualFinish;
	}

	public String getPredecessors() {
		return predecessors;
	}

	public void setPredecessors(String predecessors) {
		this.predecessors = predecessors;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getResources() {
		return resources;
	}

	public void setResources(String resources) {
		this.resources = resources;
	}

	public List<JIRAIssue> getChildren() {
		return children;
	}

	public void setChildren(List<JIRAIssue> children) {
		this.children = children;
	}

}

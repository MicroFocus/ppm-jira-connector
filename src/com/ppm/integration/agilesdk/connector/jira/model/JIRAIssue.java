package com.ppm.integration.agilesdk.connector.jira.model;

import java.util.Date;
import java.util.List;

import com.ppm.integration.agilesdk.pm.ExternalTaskActuals;

import edu.emory.mathcs.backport.java.util.Arrays;

public class JIRAIssue extends JIRAEntity {
	private String issueName;
	private String type;
	private String key;
	private String statusName;
	private String scheduledStartDate;
	private String scheduledFinishDate;
	private String scheduledDuration;
	private Long scheduledEffort;
	private String actualStart;
	private String percentComplete;
	private String actualFinish;
	private String predecessors;
	private String role;
	private String resources;
	private String createdDate;
	private String updatedDate;
	private List<JIRAIssue> subTasks;
	private String epicLink;

	public JIRAIssue(String issueName, String type, String key, String statusName, String scheduledStartDate,
			String scheduledFinishDate, String scheduledDuration, Long scheduledEffort, String actualStart,
			String percentComplete, String actualFinish, String predecessors, String role, String resources,
			String createdDate, String updatedDate, List<JIRAIssue> subTasks, String epicLink) {

		this.issueName = issueName;
		this.type = type;
		this.key = key;
		this.statusName = statusName;
		this.scheduledStartDate = scheduledStartDate;
		this.scheduledFinishDate = scheduledFinishDate;
		this.scheduledDuration = scheduledDuration;
		this.scheduledEffort = scheduledEffort;
		this.actualStart = actualStart;
		this.percentComplete = percentComplete;
		this.actualFinish = actualFinish;
		this.predecessors = predecessors;
		this.role = role;
		this.resources = resources;
		this.createdDate = createdDate;
		this.updatedDate = updatedDate;
		this.subTasks = subTasks;
		this.epicLink = epicLink;
	}

	public String getIssueName() {
		return issueName;
	}

	public void setIssueName(String issueName) {
		this.issueName = issueName;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getStatusName() {
		return statusName;
	}

	public void setStatusName(String statusName) {
		this.statusName = statusName;
	}

	public String getScheduledStartDate() {
		return scheduledStartDate;
	}

	public void setScheduledStartDate(String scheduledStartDate) {
		this.scheduledStartDate = scheduledStartDate;
	}

	public String getScheduledFinishDate() {
		return scheduledFinishDate;
	}

	public void setScheduledFinishDate(String scheduledFinishDate) {
		this.scheduledFinishDate = scheduledFinishDate;
	}

	public String getScheduledDuration() {
		return scheduledDuration;
	}

	public void setScheduledDuration(String scheduledDuration) {
		this.scheduledDuration = scheduledDuration;
	}

	public Long getScheduledEffort() {
		return scheduledEffort;
	}

	public void setScheduledEffort(Long scheduledEffort) {
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

	public String getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(String createdDate) {
		this.createdDate = createdDate;
	}

	public String getUpdatedDate() {
		return updatedDate;
	}

	public void setUpdatedDate(String updatedDate) {
		this.updatedDate = updatedDate;
	}

	public List<JIRAIssue> getSubTasks() {
		return subTasks;
	}

	public void setSubTasks(List<JIRAIssue> subTasks) {
		this.subTasks = subTasks;
	}

	public String getEpicLink() {
		return epicLink;
	}

	public void setEpicLink(String epicLink) {
		this.epicLink = epicLink;
	}

	@Override
	public TaskStatus getStatus() {
		return getTaskStatus(this.statusName);
	}

	@Override
	public Date getScheduledStart() {
		return checkDate(this.createdDate);
	}

	@Override
	public Date getScheduledFinish() {
		return checkDate(this.scheduledFinishDate);
	}

	@Override
	public String getName() {
		return "[" + this.type + "] " + this.issueName;
	}

	@Override
	public String getId() {

		return null;
	}

	@Override
	public Double getPercentCompleteOverrideValue() {
		return Double.parseDouble(this.percentComplete);
	}

	@Override
	public List<ExternalTaskActuals> getActuals() {
		ExternalTaskActuals etl = new ExternalTaskActuals() {
			@Override
			public double getScheduledEffort() {
				return scheduledEffort / 3600;
			}

			@Override
			public Date getActualFinish() {
				return super.getActualFinish();
			}

			@Override
			public Date getActualStart() {
				return super.getActualStart();
			}

			@Override
			public Date getEstimatedFinishDate() {
				return super.getEstimatedFinishDate();
			}

			@Override
			public Double getEstimatedRemainingEffort() {
				return super.getEstimatedRemainingEffort();
			}

			@Override
			public double getPercentComplete() {
				return Double.parseDouble(percentComplete);
			}

		};
		return Arrays.asList(new ExternalTaskActuals[] { etl });
	}

	private TaskStatus getTaskStatus(String status) {
		switch (status) {
		case "To Do":
			return TaskStatus.IN_PLANNING;
		case "In Progress":
			return TaskStatus.IN_PROGRESS;
		case "Done":
			return TaskStatus.READY;
		default:
			return TaskStatus.UNKNOWN;
		}
	}

}

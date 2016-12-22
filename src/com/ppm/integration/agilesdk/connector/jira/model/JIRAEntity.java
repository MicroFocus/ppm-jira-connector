package com.ppm.integration.agilesdk.connector.jira.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.hp.ppm.integration.pm.IExternalTask;
import com.hp.ppm.integration.pm.ITaskActual;

public class JIRAEntity implements IExternalTask {
	private JIRAIssue issue;

	public void setIssue(JIRAIssue issue) {
		this.issue = issue;
	}

	@Override
	public boolean isMilestone() {

		return false;
	}

	@Override
	public TaskStatus getStatus() {

		return getTaskStatus(issue.getStatus());
	}

	@Override
	public Date getScheduleStart() {

		return getDate(issue.getScheduledStart());
	}

	@Override
	public Date getScheduleFinish() {

		return getDate(issue.getScheduledFinish());
	}

	@Override
	public String getOwnerRole() {

		return "admin";
	}

	@Override
	public long getOwnerId() {

		return 0;
	}

	@Override
	public String getName() {

		return "[" + issue.getType() + "] " + issue.getName();
	}

	@Override
	public String getId() {

		return null;
	}

	@Override
	public List<IExternalTask> getChildren() {

		List<JIRAIssue> list = issue.getChildren();
		List<IExternalTask> entityList = new ArrayList<>();
		if (list != null) {
			for (JIRAIssue issue : list) {
				JIRAEntity entity = new JIRAEntity();
				entity.setIssue(issue);
				entityList.add(entity);
			}

		}
		return entityList;

	}

	@Override
	public List<ITaskActual> getActuals() {

		return null;
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

	private Date getDate(String dateStr) {
		if (dateStr != null) {
			SimpleDateFormat format = new SimpleDateFormat("yyyy-mm-dd");
			Date d = null;
			try {
				d = format.parse(dateStr.substring(0, 10));
			} catch (ParseException e) {

			}
			return d;
		} else {
			//
			return new Date();
		}
	}

}

package com.hp.ppm.integration.jira.model;

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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public TaskStatus getStatus() {
		// TODO Auto-generated method stub
		return getTaskStatus(issue.getStatus());
	}

	@Override
	public Date getScheduleStart() {
		// TODO Auto-generated method stub
		return getDate(issue.getScheduledStart());
	}

	@Override
	public Date getScheduleFinish() {
		// TODO Auto-generated method stub
		return getDate(issue.getScheduledFinish());
	}

	@Override
	public String getOwnerRole() {
		// TODO Auto-generated method stub
		return "admin";
	}

	@Override
	public long getOwnerId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "[" + issue.getType() + "] " + issue.getName();
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<IExternalTask> getChildren() {
		// TODO Auto-generated method stub
		List<JIRAIssue> list = issue.getChildren();
		List<IExternalTask> entityList = new ArrayList<>();
		if (list != null) {
			for (JIRAIssue issue : list) {
				JIRAEntity entity = new JIRAEntity();
				entity.setIssue(issue);
				entityList.add(entity);
			}
			return entityList;
		}
		return null;

	}

	@Override
	public List<ITaskActual> getActuals() {
		// TODO Auto-generated method stub
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return d;
		} else {
			//
			return new Date();
		}
	}

}

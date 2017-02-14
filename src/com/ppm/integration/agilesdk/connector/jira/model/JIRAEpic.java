package com.ppm.integration.agilesdk.connector.jira.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.ppm.integration.agilesdk.pm.ExternalTask;
import com.ppm.integration.agilesdk.pm.ExternalTaskActuals;

import edu.emory.mathcs.backport.java.util.Arrays;

public class JIRAEpic extends JIRAIssue {
	private boolean isBreakdown;

	public JIRAEpic(String issueName, String type, String key, String statusName, String scheduledStartDate,
			String scheduledFinishDate, String scheduledDuration, Long scheduledEffort, String actualStart,
			String percentComplete, String actualFinish, String predecessors, String role, String resources,
			String createdDate, String updatedDate, List<JIRAIssue> subTasks, String epicLink) {

		super(issueName, type, key, statusName, scheduledStartDate, scheduledFinishDate, scheduledDuration,
				scheduledEffort, actualStart, percentComplete, actualFinish, predecessors, role, resources, createdDate,
				updatedDate, subTasks, epicLink);
	}

	public void setBreakdown(boolean isBreakdown) {
		this.isBreakdown = isBreakdown;
	}

	private String getPercent() {
		return this.getPercentComplete();
	}

	@Override
	public Date getScheduledStart() {

		return checkDate(this.getCreatedDate());
	}

	@Override
	public Date getScheduledFinish() {
		return checkDate(this.getUpdatedDate());
	}

	@Override
	public List<ExternalTask> getChildren() {
		if (isBreakdown) {
			List<ExternalTask> ets = new ArrayList<>();
			for (JIRAIssue issue : this.getSubTasks()) {
				ets.add((ExternalTask) issue);
			}
			return ets;
		}
		return null;
	}

	@Override
	public String getName() {
		return "[" + this.getType() + "] " + this.getIssueName();
	}

	@Override
	public List<ExternalTaskActuals> getActuals() {
		ExternalTaskActuals eta = new ExternalTaskActuals() {
			@Override
			public double getPercentComplete() {
				return Double.parseDouble(getPercent());
			}

			@Override
			public double getScheduledEffort() {
				if (!isBreakdown) {
					double actualEffort = 0.0;

					for (JIRAIssue ji : getSubTasks()) {
						actualEffort += ji.getScheduledEffort();
					}
					return actualEffort / 3600;
				}

				return super.getScheduledEffort();
			}
		};
		return Arrays.asList(new ExternalTaskActuals[] { eta });
	}
}
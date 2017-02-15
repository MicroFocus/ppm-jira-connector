package com.ppm.integration.agilesdk.connector.jira.model;

import java.util.List;

import com.ppm.integration.agilesdk.pm.ExternalTask;
import com.ppm.integration.agilesdk.pm.ExternalWorkPlan;

public class JIRAExternalWorkPlan extends ExternalWorkPlan {
	private List<ExternalTask> ets;

	public JIRAExternalWorkPlan(List<ExternalTask> ets) {
		this.ets = ets;
	}

	@Override
	public List<ExternalTask> getRootTasks() {
		return ets;
	}

}

package com.ppm.integration.agilesdk.connector.jira;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.jira.model.JIRAEntity;
import com.ppm.integration.agilesdk.connector.jira.model.JIRAIssue;
import com.ppm.integration.agilesdk.connector.jira.model.JIRAProject;
import com.ppm.integration.agilesdk.connector.jira.rest.util.IRestConfig;
import com.ppm.integration.agilesdk.connector.jira.rest.util.JIRARestConfig;
import com.ppm.integration.agilesdk.connector.jira.rest.util.RestWrapper;
import com.ppm.integration.agilesdk.pm.ExternalTask;
import com.ppm.integration.agilesdk.pm.ExternalWorkPlan;
import com.ppm.integration.agilesdk.pm.WorkPlanIntegration;
import com.ppm.integration.agilesdk.pm.WorkPlanIntegrationContext;
import com.ppm.integration.agilesdk.ui.CheckBox;
import com.ppm.integration.agilesdk.ui.DynamicDropdown;
import com.ppm.integration.agilesdk.ui.Field;
import com.ppm.integration.agilesdk.ui.LabelText;
import com.ppm.integration.agilesdk.ui.LineBreaker;
import com.ppm.integration.agilesdk.ui.PasswordText;
import com.ppm.integration.agilesdk.ui.PlainText;

public class JIRAWorkPlanIntegration extends WorkPlanIntegration {
	private JIRAService service;

	public JIRAWorkPlanIntegration() {

	}

	public JIRAWorkPlanIntegration(String username, String password, String baseUri) {
		configureService(null, null, username, password, baseUri);
	}

	public JIRAWorkPlanIntegration(String proxyHost, String proxyPort, String username, String password,
			String baseUri) {
		configureService(proxyHost, proxyPort, username, password, baseUri);

	}

	@Override
	public List<Field> getMappingConfigurationFields(WorkPlanIntegrationContext context, ValueSet values) {

		List<Field> fields = Arrays
				.asList(new Field[] { new PlainText(JIRAConstants.KEY_USERNAME, "USERNAME", "admin", true),

						new PasswordText(JIRAConstants.KEY_PASSWORD, "PASSWORD", "hpe1990", true), new LineBreaker(),
						new DynamicDropdown(JIRAConstants.KEY_JIRA_PROJECT_NAME, "JIRA_PRPOJECT", true) {

							@Override
							public List<String> getDependencies() {
								return Arrays.asList(
										new String[] { JIRAConstants.KEY_USERNAME, JIRAConstants.KEY_PASSWORD });
							}

							@Override
							public List<Option> getDynamicalOptions(ValueSet values) {
								configureService(values.get(JIRAConstants.KEY_PROXY_HOST),
										values.get(JIRAConstants.KEY_PROXY_PORT),
										values.get(JIRAConstants.KEY_USERNAME), values.get(JIRAConstants.KEY_PASSWORD),
										values.get(JIRAConstants.KEY_BASE_URL));

								List<JIRAProject> list = service.getProjects();

								List<Option> optionList = new ArrayList<>();
								for (JIRAProject project : list) {
									Option option = new Option(project.getKey(), project.getName());

									optionList.add(option);
								}
								return optionList;
							}
						}, new LineBreaker(),

						new LabelText(JIRAConstants.KEY_LEVEL_OF_DETAILS_TO_SYNCHRONIZE,
								"LEVEL_OF_DETAILS_TO_SYNCHRONIZE", "Level of Details to Synchronize", true),
						new CheckBox(JIRAConstants.JIRA_ISSUE_TASK, "INCLUDE_JIRA_ISSUE_TASK", true),
						new CheckBox(JIRAConstants.JIRA_ISSUE_STORY, "INCLUDE_JIRA_ISSUE_STORY", true),
						new CheckBox(JIRAConstants.JIRA_ISSUE_EPIC, "INCLUDE_JIRA_ISSUE_EPIC", true),
						new CheckBox(JIRAConstants.JIRA_ISSUE_BUG, "INCLUDE_JIRA_ISSUE_BUG", true) });

		return fields;
	}

	@Override
	public ExternalWorkPlan getExternalWorkPlan(WorkPlanIntegrationContext context, ValueSet values) {
		String projectKey = values.get(JIRAConstants.KEY_JIRA_PROJECT_NAME);
		configureService(values.get(JIRAConstants.KEY_PROXY_HOST), values.get(JIRAConstants.KEY_PROXY_PORT),
				values.get(JIRAConstants.KEY_USERNAME), values.get(JIRAConstants.KEY_PASSWORD),
				values.get(JIRAConstants.KEY_BASE_URL));
		Map<String, Boolean> map = new HashMap<>();
		map.put(JIRAConstants.JIRA_ISSUE_TASK, values.getBoolean(JIRAConstants.JIRA_ISSUE_TASK, true));
		map.put(JIRAConstants.JIRA_ISSUE_STORY, values.getBoolean(JIRAConstants.JIRA_ISSUE_STORY, true));
		map.put(JIRAConstants.JIRA_ISSUE_EPIC, values.getBoolean(JIRAConstants.JIRA_ISSUE_EPIC, true));
		map.put(JIRAConstants.JIRA_ISSUE_BUG, values.getBoolean(JIRAConstants.JIRA_ISSUE_BUG, true));

		final List<JIRAIssue> issues = service.getIssues(projectKey, map);
		return new ExternalWorkPlan() {

			@Override
			public List<ExternalTask> getRootTasks() {
				List<ExternalTask> externalTasks = new ArrayList<>();
				for (final JIRAIssue issue : issues) {
					JIRAEntity entity = new JIRAEntity();
					entity.setIssue(issue);
					
					externalTasks.add(entity);
				}

				return externalTasks;
			}
		};
	}

	private void configureService(String proxyHost, String proxyPort, String username, String password,
			String baseUri) {
		service = service == null ? new JIRAService() : service;
		IRestConfig config = new JIRARestConfig();
		config.setProxy(proxyHost, proxyPort);
		config.setBasicAuthorizaton(username, password);
		RestWrapper wrapper = new RestWrapper(config);
		service.setBaseUri(baseUri);
		service.setWrapper(wrapper);
	}

}

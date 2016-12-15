package com.hp.ppm.integration.jira;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hp.ppm.integration.ValueSet;
import com.hp.ppm.integration.jira.model.JIRAEntity;
import com.hp.ppm.integration.jira.model.JIRAIssue;
import com.hp.ppm.integration.jira.model.JIRAProject;
import com.hp.ppm.integration.jira.rest.util.IRestConfig;
import com.hp.ppm.integration.jira.rest.util.JIRARestConfig;
import com.hp.ppm.integration.jira.rest.util.RestWrapper;
import com.hp.ppm.integration.pm.IExternalTask;
import com.hp.ppm.integration.pm.IExternalWorkPlan;
import com.hp.ppm.integration.pm.WorkPlanIntegration;
import com.hp.ppm.integration.pm.WorkPlanIntegrationContext;
import com.hp.ppm.integration.ui.CheckBox;
import com.hp.ppm.integration.ui.DynamicalDropdown;
import com.hp.ppm.integration.ui.Field;
import com.hp.ppm.integration.ui.LineBreaker;
import com.hp.ppm.integration.ui.PasswordText;
import com.hp.ppm.integration.ui.PlainText;

public class JIRAWorkPlanIntegration implements WorkPlanIntegration {
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

		// configureService(values.get(JIRAConstants.KEY_PROXY_HOST),
		// values.get(JIRAConstants.KEY_PROXY_PORT),
		// values.get(JIRAConstants.KEY_USERNAME),
		// values.get(JIRAConstants.KEY_PASSWORD),
		// values.get(JIRAConstants.KEY_BASE_URL));
		//
		// List<JIRAIssueType> issueTypes = service.getIssuetypes();
		// List<Field> cbs = new ArrayList<>();
		// // use this to get the checkbox value when needed because the
		// checkboxes
		// // are not created statically
		// String issueNames = "";
		// for (JIRAIssueType jit : issueTypes) {
		// String name = jit.getName();
		// CheckBox cb = new CheckBox(name, "SHOW_" + name.toUpperCase(), true);
		// issueNames += name + "|";
		// cbs.add(cb);
		// }
		// System.out.println(context.currentTask());
		List<Field> fields = Arrays
				.asList(new Field[] { new PlainText(JIRAConstants.KEY_USERNAME, "USERNAME", "admin", true),

						new PasswordText(JIRAConstants.KEY_PASSWORD, "PASSWORD", "hpe1990", true), new LineBreaker(),
						new DynamicalDropdown(JIRAConstants.KEY_JIRA_PROJECT_NAME, "JIRA_PRPOJECT", true) {

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
						new CheckBox(JIRAConstants.JIRA_ISSUE_TASK, "SHOW_JIRA_ISSUE_TASK", true),
						new CheckBox(JIRAConstants.JIRA_ISSUE_STORY, "SHOW_JIRA_ISSUE_STORY", true),
						new CheckBox(JIRAConstants.JIRA_ISSUE_EPIC, "SHOW_JIRA_ISSUE_EPIC", true),
						new CheckBox(JIRAConstants.JIRA_ISSUE_BUG, "SHOW_JIRA_ISSUE_BUG", true) });
		// fields.addAll(cbs);
		return fields;
	}

	@Override
	public boolean linkTaskWithExternal(WorkPlanIntegrationContext context, ValueSet values) {
		return false;
	}

	@Override
	public IExternalWorkPlan getExternalWorkPlan(WorkPlanIntegrationContext context, ValueSet values) {
		String projectKey = values.get(JIRAConstants.KEY_JIRA_PROJECT_NAME);
		configureService(values.get(JIRAConstants.KEY_PROXY_HOST), values.get(JIRAConstants.KEY_PROXY_PORT),
				values.get(JIRAConstants.KEY_USERNAME), values.get(JIRAConstants.KEY_PASSWORD),
				values.get(JIRAConstants.KEY_BASE_URL));
		Map<String,Boolean> map = new HashMap<>();
		map.put(JIRAConstants.JIRA_ISSUE_TASK, values.getBoolean(JIRAConstants.JIRA_ISSUE_TASK, true));
		map.put(JIRAConstants.JIRA_ISSUE_STORY, values.getBoolean(JIRAConstants.JIRA_ISSUE_STORY, true));
		map.put(JIRAConstants.JIRA_ISSUE_EPIC, values.getBoolean(JIRAConstants.JIRA_ISSUE_EPIC, true));
		map.put(JIRAConstants.JIRA_ISSUE_BUG, values.getBoolean(JIRAConstants.JIRA_ISSUE_BUG, true));
		
		final List<JIRAIssue> issues = service.getIssues(projectKey,map);
		return new IExternalWorkPlan() {

			@Override
			public List<IExternalTask> getRootTasks() {
				List<IExternalTask> externalTasks = new ArrayList<>();
				// TODO Auto-generated method stub

				for (final JIRAIssue issue : issues) {
					JIRAEntity entity = new JIRAEntity();
					entity.setIssue(issue);
					externalTasks.add(entity);
				}

				return externalTasks;
			}
		};
	}

	@Override
	public boolean unlinkTaskWithExternal(WorkPlanIntegrationContext context, ValueSet values) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getCustomDetailPage() {
		// TODO Auto-generated method stub
		return null;
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

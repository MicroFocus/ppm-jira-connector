package com.ppm.integration.agilesdk.connector.jira;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.datatype.XMLGregorianCalendar;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.jira.model.JIRAProject;
import com.ppm.integration.agilesdk.connector.jira.rest.util.IRestConfig;
import com.ppm.integration.agilesdk.connector.jira.rest.util.JIRARestConfig;
import com.ppm.integration.agilesdk.connector.jira.rest.util.RestWrapper;
import com.ppm.integration.agilesdk.tm.ExternalWorkItem;
import com.ppm.integration.agilesdk.tm.TimeSheetIntegration;
import com.ppm.integration.agilesdk.tm.TimeSheetIntegrationContext;
import com.ppm.integration.agilesdk.ui.DynamicDropdown;
import com.ppm.integration.agilesdk.ui.Field;
import com.ppm.integration.agilesdk.ui.LineBreaker;
import com.ppm.integration.agilesdk.ui.PasswordText;
import com.ppm.integration.agilesdk.ui.PlainText;

public class JIRATimeSheetIntegration extends TimeSheetIntegration {

	private JIRAService service;

	@Override
	public List<ExternalWorkItem> getExternalWorkItems(TimeSheetIntegrationContext arg0, ValueSet values) {

		// Synchronized ?
		final List<ExternalWorkItem> items = Collections.synchronizedList(new LinkedList<ExternalWorkItem>());
		XMLGregorianCalendar start = arg0.currentTimeSheet().getPeriodStartDate();
		XMLGregorianCalendar end = arg0.currentTimeSheet().getPeriodEndDate();

		configureService(values.get(JIRAConstants.KEY_PROXY_HOST), values.get(JIRAConstants.KEY_PROXY_PORT),
				values.get(JIRAConstants.KEY_USERNAME), values.get(JIRAConstants.KEY_PASSWORD),
				values.get(JIRAConstants.KEY_BASE_URL));
		String projectKey = values.get(JIRAConstants.KEY_JIRA_PROJECT_NAME);
		Map<String, Map<String, Long>> map = service.getJIRATempoWorklogs(start, end, projectKey);

		Set<Entry<String, Map<String, Long>>> entrySet = map.entrySet();

		for (Entry<String, Map<String, Long>> entry : entrySet) {
			double actualEffort = 0;
			Map<String, Double> actualEffortsFormatted = new HashMap<>();

			Map<String, Long> actualEfforts = entry.getValue();
			Set<String> keys = actualEfforts.keySet();
			for (String key : keys) {
				actualEffort += actualEfforts.get(key) / 3600.0;
				actualEffortsFormatted.put(key, actualEfforts.get(key) / 3600.0);
			}

			items.add(new JIRAExternalWorkItem(entry.getKey(), actualEffort, entry.getKey() + " error", start, end,
					actualEffortsFormatted));
		}

		return items;
	}

	@Override
	public List<Field> getMappingConfigurationFields(ValueSet arg0) {
		return Arrays.asList(new Field[] { new PlainText(JIRAConstants.KEY_USERNAME, "USERNAME", "admin", true),
				new PasswordText(JIRAConstants.KEY_PASSWORD, "PASSWORD", "hpe1990", true), new LineBreaker(),
				new DynamicDropdown(JIRAConstants.KEY_JIRA_PROJECT_NAME, "JIRA_PRPOJECT", false) {

					@Override
					public List<String> getDependencies() {
						return Arrays.asList(new String[] { JIRAConstants.KEY_USERNAME, JIRAConstants.KEY_PASSWORD });
					}

					@Override
					public List<Option> getDynamicalOptions(ValueSet values) {
						configureService(values.get(JIRAConstants.KEY_PROXY_HOST),
								values.get(JIRAConstants.KEY_PROXY_PORT), values.get(JIRAConstants.KEY_USERNAME),
								values.get(JIRAConstants.KEY_PASSWORD), values.get(JIRAConstants.KEY_BASE_URL));

						List<JIRAProject> list = service.getProjects();

						List<Option> optionList = new ArrayList<>();
						for (JIRAProject project : list) {
							Option option = new Option(project.getKey(), project.getName());

							optionList.add(option);
						}
						return optionList;
					}
				}

		});
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


package com.ppm.integration.agilesdk.connector.jira;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.wink.client.ClientRuntimeException;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.jira.model.JIRAExternalWorkPlan;
import com.ppm.integration.agilesdk.connector.jira.model.JIRAIssue;
import com.ppm.integration.agilesdk.connector.jira.model.JIRAProject;
import com.ppm.integration.agilesdk.connector.jira.model.JIRAVersion;
import com.ppm.integration.agilesdk.connector.jira.rest.util.IRestConfig;
import com.ppm.integration.agilesdk.connector.jira.rest.util.JIRARestConfig;
import com.ppm.integration.agilesdk.connector.jira.rest.util.RestWrapper;
import com.ppm.integration.agilesdk.connector.jira.rest.util.exception.JIRAConnectivityExceptionHandler;
import com.ppm.integration.agilesdk.connector.versionone.rest.util.exception.RestRequestException;
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
    private final Logger logger = Logger.getLogger(this.getClass());

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

        List<Field> fields = Arrays.asList(new Field[] {new PlainText(JIRAConstants.KEY_USERNAME, "USERNAME", "", true),

                new PasswordText(JIRAConstants.KEY_PASSWORD, "PASSWORD", "", true), new LineBreaker(),

                new DynamicDropdown(JIRAConstants.KEY_JIRA_PROJECT_NAME, "JIRA_PRPOJECT", true) {

                    @Override
                    public List<String> getDependencies() {
                        return Arrays.asList(new String[] {JIRAConstants.KEY_USERNAME, JIRAConstants.KEY_PASSWORD});
                    }

                    @Override
                    public List<Option> getDynamicalOptions(ValueSet values) {
                        configureService(values.get(JIRAConstants.KEY_PROXY_HOST),
                                values.get(JIRAConstants.KEY_PROXY_PORT), values.get(JIRAConstants.KEY_USERNAME),
                                values.get(JIRAConstants.KEY_PASSWORD), values.get(JIRAConstants.KEY_BASE_URL));

                        List<JIRAProject> list = new ArrayList<>();
                        try {
                            list = service.getProjects();
                        } catch (ClientRuntimeException | RestRequestException e) {
                            logger.error("", e);
                            new JIRAConnectivityExceptionHandler().uncaughtException(Thread.currentThread(), e,
                                    JIRAWorkPlanIntegration.class);
                        } catch (RuntimeException e) {
                            logger.error("", e);
                            new JIRAConnectivityExceptionHandler().uncaughtException(Thread.currentThread(), e,
                                    JIRAWorkPlanIntegration.class);
                        }

                        List<Option> optionList = new ArrayList<>();
                        for (JIRAProject project : list) {
                            Option option = new Option(project.getKey(), project.getName());
                            optionList.add(option);
                        }
                        return optionList;
                    }

                }, new LineBreaker(), new DynamicDropdown(JIRAConstants.KEY_IMPORT_SELECTION, "IMPORT_SELECTION",
                        "All project planned issues", "", true) {

                    @Override
                    public List<String> getDependencies() {
                        return new ArrayList<String>();
                    }

                    @Override
                    public List<Option> getDynamicalOptions(ValueSet values) {

                        List<Option> optionList = new ArrayList<>();

                        Option option1 =
                                new Option(JIRAConstants.KEY_ALL_PROJECT_PLANNED_ISSUES, "All project planned issues");
                        Option option2 = new Option(JIRAConstants.KEY_EPIC, "Epic");
                        Option option3 = new Option(JIRAConstants.KEY_ALL_EPICS, "All Epics");
                        Option option4 = new Option(JIRAConstants.KEY_VERSION, "Version");

                        optionList.add(option1);
                        optionList.add(option2);
                        optionList.add(option3);
                        optionList.add(option4);

                        return optionList;
                    }

                }, new DynamicDropdown(JIRAConstants.KEY_IMPORT_SELECTION_DETAILS, "IMPORT_SELECTION_DETAILS", false) {

                    @Override
                    public List<String> getDependencies() {

                        return Arrays.asList(
                                new String[] {JIRAConstants.KEY_JIRA_PROJECT_NAME, JIRAConstants.KEY_IMPORT_SELECTION});
                    }

                    @Override
                    public List<Option> getDynamicalOptions(ValueSet values) {
                        String importSelection = values.get(JIRAConstants.KEY_IMPORT_SELECTION);
                        String projectKey = values.get(JIRAConstants.KEY_JIRA_PROJECT_NAME);
                        List<Option> options = new ArrayList<>();
                        switch (importSelection) {
                            case JIRAConstants.KEY_ALL_PROJECT_PLANNED_ISSUES:
                                break;
                            case JIRAConstants.KEY_EPIC:
                                configureService(values.get(JIRAConstants.KEY_PROXY_HOST),
                                        values.get(JIRAConstants.KEY_PROXY_PORT),
                                        values.get(JIRAConstants.KEY_USERNAME), values.get(JIRAConstants.KEY_PASSWORD),
                                        values.get(JIRAConstants.KEY_BASE_URL));
                                List<JIRAIssue> list = service.getIssues(projectKey, JIRAConstants.JIRA_ISSUE_EPIC);
                                for (JIRAIssue issue : list) {
                                    Option option = new Option(issue.getKey(), issue.getName());
                                    options.add(option);

                                }
                                break;
                            case JIRAConstants.KEY_ALL_EPICS:
                                break;
                            case JIRAConstants.KEY_VERSION:
                                configureService(values.get(JIRAConstants.KEY_PROXY_HOST),
                                        values.get(JIRAConstants.KEY_PROXY_PORT),
                                        values.get(JIRAConstants.KEY_USERNAME), values.get(JIRAConstants.KEY_PASSWORD),
                                        values.get(JIRAConstants.KEY_BASE_URL));
                                List<JIRAVersion> list1 = service.getVersions(projectKey);
                                for (JIRAVersion version : list1) {
                                    Option option = new Option(version.getId(), version.getName());
                                    options.add(option);

                                }
                                break;
                        }
                        return options;
                    }

                }, new CheckBox(JIRAConstants.KEY_INCLUDE_ISSUES_BREAKDOWN, "INCLUDE_ISSUES_BREAKDOWN", false),
                new LineBreaker(),
                new LabelText(JIRAConstants.KEY_JIRA_ISSUES_TO_IMPORT, "SELECT_ISSUES_TO_IMPORT",
                        "Select Issues to Import", true),
                new CheckBox(JIRAConstants.JIRA_ISSUE_TASK, "JIRA_ISSUE_TASK", true),
                new CheckBox(JIRAConstants.JIRA_ISSUE_STORY, "JIRA_ISSUE_STORY", true),
                new CheckBox(JIRAConstants.JIRA_ISSUE_BUG, "JIRA_ISSUE_BUG", false)});

        return fields;
    }

    @Override
    public ExternalWorkPlan getExternalWorkPlan(WorkPlanIntegrationContext context, ValueSet values) {
        String projectKey = values.get(JIRAConstants.KEY_JIRA_PROJECT_NAME);
        String importSelection = values.get(JIRAConstants.KEY_IMPORT_SELECTION);
        String importSelectionDetails = values.get(JIRAConstants.KEY_IMPORT_SELECTION_DETAILS);
        boolean isBreakdown = values.getBoolean(JIRAConstants.KEY_INCLUDE_ISSUES_BREAKDOWN, true);
        configureService(values.get(JIRAConstants.KEY_PROXY_HOST), values.get(JIRAConstants.KEY_PROXY_PORT),
                values.get(JIRAConstants.KEY_USERNAME), values.get(JIRAConstants.KEY_PASSWORD),
                values.get(JIRAConstants.KEY_BASE_URL));
        Map<String, Boolean> map = new HashMap<>();
        map.put(JIRAConstants.JIRA_ISSUE_TASK, values.getBoolean(JIRAConstants.JIRA_ISSUE_TASK, true));
        map.put(JIRAConstants.JIRA_ISSUE_STORY, values.getBoolean(JIRAConstants.JIRA_ISSUE_STORY, true));
        map.put(JIRAConstants.JIRA_ISSUE_BUG, values.getBoolean(JIRAConstants.JIRA_ISSUE_BUG, true));

        List<ExternalTask> ets =
                service.getExternalTasks(projectKey, map, importSelection, importSelectionDetails, isBreakdown);

        return new JIRAExternalWorkPlan(ets);
    }

    @Override
    public String getCustomDetailPage() {
        return null;
    }

    private void configureService(String proxyHost, String proxyPort, String username, String password,
            String baseUri)
    {
        service = service == null ? new JIRAService() : service;
        IRestConfig config = new JIRARestConfig();
        config.setProxy(proxyHost, proxyPort);
        config.setBasicAuthorizaton(username, password);
        RestWrapper wrapper = new RestWrapper(config);
        service.setBaseUri(baseUri);
        service.setWrapper(wrapper);
    }

}


package com.ppm.integration.agilesdk.connector.jira;

import java.util.*;
import java.util.Map.Entry;

import javax.xml.datatype.XMLGregorianCalendar;

import com.ppm.integration.agilesdk.connector.jira.model.JIRAIssueWork;
import com.ppm.integration.agilesdk.tm.ExternalWorkItemEffortBreakdown;
import org.apache.log4j.Logger;
import org.apache.wink.client.ClientRuntimeException;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.jira.model.JIRAProject;
import com.ppm.integration.agilesdk.connector.jira.rest.util.exception.JIRAConnectivityExceptionHandler;
import com.ppm.integration.agilesdk.connector.jira.rest.util.exception.RestRequestException;
import com.ppm.integration.agilesdk.tm.ExternalWorkItem;
import com.ppm.integration.agilesdk.tm.TimeSheetIntegration;
import com.ppm.integration.agilesdk.tm.TimeSheetIntegrationContext;
import com.ppm.integration.agilesdk.ui.DynamicDropdown;
import com.ppm.integration.agilesdk.ui.Field;
import com.ppm.integration.agilesdk.ui.LineBreaker;
import com.ppm.integration.agilesdk.ui.PasswordText;
import com.ppm.integration.agilesdk.ui.PlainText;

public class JIRATimeSheetIntegration extends TimeSheetIntegration {

    private final Logger logger = Logger.getLogger(this.getClass());

    private JIRAServiceProvider service = new JIRAServiceProvider();

    @Override
    public List<ExternalWorkItem> getExternalWorkItems(TimeSheetIntegrationContext timesheetContext, ValueSet values) {

        XMLGregorianCalendar start = timesheetContext.currentTimeSheet().getPeriodStartDate();
        XMLGregorianCalendar end = timesheetContext.currentTimeSheet().getPeriodEndDate();

        String projectKey = values.get(JIRAConstants.KEY_JIRA_PROJECT);
        String author = values.get(JIRAConstants.KEY_USERNAME);
        List<ExternalWorkItem> authorWorklogs = service.get(values).getWorkItems(start, end, projectKey, author);

        return authorWorklogs;
    }

    @Override
    public List<Field> getMappingConfigurationFields(ValueSet arg0) {
        return Arrays.asList(new Field[] {new PlainText(JIRAConstants.KEY_USERNAME, "USERNAME", "", true),
                new PasswordText(JIRAConstants.KEY_PASSWORD, "PASSWORD", "", true), new LineBreaker(),
                new DynamicDropdown(JIRAConstants.KEY_JIRA_PROJECT, "JIRA_PROJECT", false) {

                    @Override
                    public List<String> getDependencies() {
                        return Arrays.asList(new String[] {JIRAConstants.KEY_USERNAME, JIRAConstants.KEY_PASSWORD});
                    }

                    @Override
                    public List<Option> getDynamicalOptions(ValueSet values) {

                        List<JIRAProject> list = new ArrayList<>();
                        try {
                            list = service.get(values).getProjects();
                        } catch (ClientRuntimeException | RestRequestException e) {
                            new JIRAConnectivityExceptionHandler().uncaughtException(Thread.currentThread(), e,
                                    JIRATimeSheetIntegration.class);
                        } catch (RuntimeException e) {
                            new JIRAConnectivityExceptionHandler().uncaughtException(Thread.currentThread(), e,
                                    JIRATimeSheetIntegration.class);
                        }

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
}

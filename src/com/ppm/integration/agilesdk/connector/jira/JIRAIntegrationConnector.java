
package com.ppm.integration.agilesdk.connector.jira;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ppm.integration.agilesdk.FunctionIntegration;
import com.ppm.integration.agilesdk.IntegrationConnector;
import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.jira.model.JIRAProject;
import com.ppm.integration.agilesdk.model.AgileProject;
import com.ppm.integration.agilesdk.ui.*;

/**
 * @author baijuy The connector provides the integration for ppm with Jira. The
 *         field InstanceName and BaseUrl are required and field Proxy is
 *         optional. Once the connector is saved, it can be used to integrate
 *         with workplan or timesheet.
 */
public class JIRAIntegrationConnector extends IntegrationConnector {

    @Override
    public String getExternalApplicationName() {
        return "Atlassian JIRA";
    }

    @Override
    public String getExternalApplicationVersionIndication() {
        return "7.2.6+";
    }

    @Override
    public String getConnectorVersion() {
        return "1.0";
    }

    @Override
    public String getTargetApplicationIcon() {
        return "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsQAAA7EAZUrDhsAAAFtSURBVDhPY/wPBAwUACYoTTbAasCfv3/BdGDFXAaPghlg9p9//8A0OsAw4MSVhwysLJYMT998ZPA112BwM1Fl+P7zNwMrsyXD1mPXoaqQACgM0MGK3eegLARYsQdTDARQXFA3axsDs1UhAxsTI5i/cvc5htV7L4DZgpzsDFyOZQzF3WvAfDiAGgQHDKpJ/xm008BsCc+a/xzWxWA2g076fwaVRDAbGWAYUNC/7j+nXcl/XocyqMj//yKulf+5gfyElmVQEQTAmg7Uw9sYXrz9xKAiJczAxcnGcPHuCwZBbg6GhxvroSoQAGdCkvVtYPj26zcDCzMTgyg/N8OV5RVQGVSAMyE93tzAwA7U/PvPXwZTbTmoKBYAcgE6uHjn6X8GyUgwm9Ei/z+DYfb/IxfvgvnoAKsLSidvYmBSlGSYu+EYw75pOQzMwHAom7wZKosGoAahAAaTnP8MMtFQHpAPZDNopkJ5qGCgcyMDAwAu2lvvmsJuBwAAAABJRU5ErkJggg==";
    }

    @Override
    public List<Field> getDriverConfigurationFields() {
        return Arrays.asList(new Field[] {new PlainText(JIRAConstants.KEY_BASE_URL, "BASE_URL", "", true),
                new PlainText(JIRAConstants.KEY_PROXY_HOST, "PROXY_HOST", "", false),
                new PlainText(JIRAConstants.KEY_PROXY_PORT, "PROXY_PORT", "", false),
                new LineBreaker(),
                new LabelText("", "ADMIN_INFO_FOR_EPIC_AND_AGILE_DATA", "block", false),
                new PlainText(JIRAConstants.KEY_ADMIN_USERNAME, "ADMIN_USERNAME", "", true),
                new PasswordText(JIRAConstants.KEY_ADMIN_PASSWORD, "ADMIN_PASSWORD", "", true),
                new LineBreaker(),
                new LabelText(JIRAConstants.LABEL_USER_DATA_FIELDS, "USER_DATA_OPTIONS",
                        "User Data Options:", true),
                getUserDataDDL(JIRAConstants.SELECT_USER_DATA_STORY_POINTS, "USER_DATA_STORY_POINTS"),
                getUserDataDDL(JIRAConstants.SELECT_USER_DATA_AGGREGATED_STORY_POINTS, "USER_DATA_AGGREGATED_STORY_POINTS")
        });
    }

    @Override
    public List<AgileProject> getAgileProjects(ValueSet instanceConfigurationParameters) {
        List<JIRAProject> jiraProjects = new JIRAServiceProvider().useAdminAccount().get(instanceConfigurationParameters).getProjects();
        List<AgileProject> agileProjects = new ArrayList<AgileProject>(jiraProjects.size());

        for (JIRAProject jiraProject : jiraProjects) {
            AgileProject agileProject = new AgileProject();
            agileProject.setDisplayName(jiraProject.getName());
            agileProject.setValue(jiraProject.getKey());
            agileProjects.add(agileProject);
        }

        return agileProjects;
    }

    @Override
    public List<FunctionIntegration> getIntegrations() {
        return Arrays.asList(new FunctionIntegration[] {new JIRAWorkPlanIntegration(), new JIRATimeSheetIntegration()});
    }

    @Override
    public List<String> getIntegrationClasses() {
        return Arrays.asList(new String[] {"com.ppm.integration.agilesdk.connector.jira.JIRAWorkPlanIntegration","com.ppm.integration.agilesdk.connector.jira.JIRATimeSheetIntegration", "com.ppm.integration.agilesdk.connector.jira.JIRAPortfolioEpicIntegration", "com.ppm.integration.agilesdk.connector.jira.JIRAAgileDataIntegration"});
    }

    private DynamicDropdown getUserDataDDL(String elementName,
                                           String labelKey) {

        DynamicDropdown udDDL = new DynamicDropdown(elementName, labelKey, "0", "", false) {

            @Override public List<String> getDependencies() {
                return new ArrayList<String>();
            }

            @Override public List<Option> getDynamicalOptions(ValueSet values) {
                List<Option> options = new ArrayList<Option>();
                options.add(new Option("0", "Do not sync"));

                for (int i = 1 ; i <= 20 ; i++) {
                    options.add(new Option(String.valueOf(i), "USER_DATA"+i));
                }

                return options;
            }
        };

        return udDDL;
    }

}

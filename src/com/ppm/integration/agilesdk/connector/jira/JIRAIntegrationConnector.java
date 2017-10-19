
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
        return "data:img/jpg;base64,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UmVhZHlxyWU8AAADhmlUWHRYTUw6Y29tLmFkb2JlLnhtcAAAAAAAPD94cGFja2V0IGJlZ2luPSLvu78iIGlkPSJXNU0wTXBDZWhpSHpyZVN6TlRjemtjOWQiPz4gPHg6eG1wbWV0YSB4bWxuczp4PSJhZG9iZTpuczptZXRhLyIgeDp4bXB0az0iQWRvYmUgWE1QIENvcmUgNS42LWMxMzggNzkuMTU5ODI0LCAyMDE2LzA5LzE0LTAxOjA5OjAxICAgICAgICAiPiA8cmRmOlJERiB4bWxuczpyZGY9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkvMDIvMjItcmRmLXN5bnRheC1ucyMiPiA8cmRmOkRlc2NyaXB0aW9uIHJkZjphYm91dD0iIiB4bWxuczp4bXBNTT0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wL21tLyIgeG1sbnM6c3RSZWY9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZVJlZiMiIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIgeG1wTU06T3JpZ2luYWxEb2N1bWVudElEPSJ4bXAuZGlkOjZiMWQ3MzlkLTc2ZTMtNDc1Yy05MDNlLWZhOGQ1M2RjNzhjOCIgeG1wTU06RG9jdW1lbnRJRD0ieG1wLmRpZDoxNTFBNEE4ODVCMDgxMUU3OTJCQUJDMUE4MENFRjIxNCIgeG1wTU06SW5zdGFuY2VJRD0ieG1wLmlpZDoxNTFBNEE4NzVCMDgxMUU3OTJCQUJDMUE4MENFRjIxNCIgeG1wOkNyZWF0b3JUb29sPSJBZG9iZSBQaG90b3Nob3AgQ0MgMjAxNyAoTWFjaW50b3NoKSI+IDx4bXBNTTpEZXJpdmVkRnJvbSBzdFJlZjppbnN0YW5jZUlEPSJ4bXAuaWlkOjk2ZmJjMmZiLTAzYzItNDAxZC04M2M5LTYyOTU0YWQxM2E2MyIgc3RSZWY6ZG9jdW1lbnRJRD0iYWRvYmU6ZG9jaWQ6cGhvdG9zaG9wOjFjYzM0M2VkLWEzNmItMTE3YS1iMTFmLWUwZDZhZTEwNDY3ZCIvPiA8L3JkZjpEZXNjcmlwdGlvbj4gPC9yZGY6UkRGPiA8L3g6eG1wbWV0YT4gPD94cGFja2V0IGVuZD0iciI/Pglps/EAAAFdSURBVHjajNPLK4RRHMbxY9xWLhu3FOspGzYWFpaUKGLhVmNnYYUo5S+gyMZGQ4hYuNWQBWsLrCRLiTCahcSCGN+jZ3LmjMGvPs2Z3/ue572c82bE43FTWtViVEV4xCv6MYtl9Op4Ce7t4O5s56sRMN8VRBQPyFWIrUz97tp56HLmmCxnHNXVnzQpjG3EdPwFzwpJCajHMQpNcsWccbszrsGp+wgHuNHzJqoTrQghT71JXOMkcVIiYAUF6MaSegPYwLweqwNDKMeFHzCKD4170Ic67KMY1Vh37m7YD7hFm3NCWEvXqNs/co5NIeIHGL3xEef/om7/HDnqrWIw3TLamsCbrmL0Am29Y03vKKkCJrWm0eD1Dn+anC4gqDWudXp23PTfgD1t56j2ga18bKLsr4BmVDpBW87mytY++DVgzBnP6Xfc6YX8AH8VIvpY7Je4oN4lZlCBKz/gU4ABAGJxSuC/6ksFAAAAAElFTkSuQmCC";
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
                new LabelText(JIRAConstants.LABEL_WORK_PLAN_OPTIONS, "WORK_PLAN_OPTIONS",
                        "User Data Options:", true),
                getUserDataDDL(JIRAConstants.SELECT_USER_DATA_STORY_POINTS, "USER_DATA_STORY_POINTS"),
                getUserDataDDL(JIRAConstants.SELECT_USER_DATA_AGGREGATED_STORY_POINTS, "USER_DATA_AGGREGATED_STORY_POINTS"),
                new LineBreaker(),
                new CheckBox(JIRAConstants.KEY_USE_ADMIN_PASSWORD_TO_MAP_TASKS, "KEY_USE_ADMIN_PASSWORD_TO_MAP_TASKS", false),
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

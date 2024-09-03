package com.ppm.integration.agilesdk.connector.jira;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.jira.rest.util.IRestConfig;
import com.ppm.integration.agilesdk.connector.jira.rest.util.JIRARestConfig;
import com.ppm.integration.agilesdk.connector.jira.rest.util.JiraRestWrapper;
import com.ppm.integration.agilesdk.connector.jira.service.JIRAService;
import com.ppm.integration.agilesdk.provider.Providers;
import com.ppm.integration.agilesdk.provider.UserProvider;
import org.apache.commons.lang3.StringUtils;

public class JIRAServiceProvider {

    public static UserProvider getUserProvider() {
        return Providers.getUserProvider(JIRAIntegrationConnector.class);
    }

    public static JIRAService get(ValueSet config) {
        IRestConfig adminRestConfig = new JIRARestConfig();
        adminRestConfig.setProxy(config.get(JIRAConstants.KEY_PROXY_HOST), config.get(JIRAConstants.KEY_PROXY_PORT));
        adminRestConfig.setBasicAuthorizationCredentials(config.get(JIRAConstants.KEY_ADMIN_USERNAME),
                config.get(JIRAConstants.KEY_ADMIN_PASSWORD), config.get(JIRAConstants.KEY_ADMIN_PAT));
        JiraRestWrapper adminWrapper = new JiraRestWrapper(adminRestConfig);

        IRestConfig userRestConfig = new JIRARestConfig();
        userRestConfig.setProxy(config.get(JIRAConstants.KEY_PROXY_HOST), config.get(JIRAConstants.KEY_PROXY_PORT));
        userRestConfig.setBasicAuthorizationCredentials(config.get(JIRAConstants.KEY_USERNAME),
                config.get(JIRAConstants.KEY_PASSWORD), config.get(JIRAConstants.KEY_PAT));
        JiraRestWrapper userWrapper = new JiraRestWrapper(userRestConfig);

        JIRAService service = new JIRAService(config.get(JIRAConstants.KEY_BASE_URL), adminWrapper, userWrapper);
        service.setEpicIssueType(getEpicIssueType(config));

        return service;
    }

    public static String getEpicIssueType(ValueSet config) {
        String epicIssueType = config.get(JIRAConstants.KEY_JIRA_EPIC_TYPE_NAME);
        if (StringUtils.isBlank(epicIssueType) || epicIssueType.equalsIgnoreCase(JIRAConstants.JIRA_ISSUE_EPIC)) {
            epicIssueType = JIRAConstants.JIRA_ISSUE_EPIC;
        }
        return epicIssueType;
    }

}

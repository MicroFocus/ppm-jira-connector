package com.ppm.integration.agilesdk.connector.jira;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.jira.model.*;
import com.ppm.integration.agilesdk.model.AgileProject;
import com.ppm.integration.agilesdk.epic.PortfolioEpicCreationInfo;
import com.ppm.integration.agilesdk.epic.PortfolioEpicIntegration;
import com.ppm.integration.agilesdk.epic.PortfolioEpicSyncInfo;
import com.ppm.integration.agilesdk.provider.Providers;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @see PortfolioEpicIntegration
 * @since PPM 9.42
 */
public class JIRAPortfolioEpicIntegration extends PortfolioEpicIntegration {

    // All operations for Portfolio Epics are using the admin account as users are not prompted for account info upon sync.
    private JIRAServiceProvider service = new JIRAServiceProvider().useAdminAccount();

    @Override public String createEpicInAgileProject(PortfolioEpicCreationInfo epicInfo, String agileProjectValue,
            ValueSet instanceConfigurationParameters)
    {
        return service.get(instanceConfigurationParameters).createEpic(agileProjectValue, epicInfo.getEpicName(), epicInfo.getEpicDescription());
    }

    @Override public PortfolioEpicSyncInfo getPortfolioEpicSyncInfo(String epicId, String agileProjectValue,
            ValueSet instanceConfigurationParameters)
    {
        if (StringUtils.isBlank(epicId)) {
            return null;
        }

        // We want to retrieve the epic and all of its contents to be able to compute aggregated story points & percent SP complete
        // That means retrieve all issue types except Sub-Tasks.

        List<JIRAIssueType> jiraIssueTypes =  service.get(instanceConfigurationParameters).getProjectIssueTypes(agileProjectValue);

        Set<String> issueTypes = new HashSet<String>();
        for (JIRAIssueType jiraIssueType : jiraIssueTypes) {
            if (!JIRAConstants.JIRA_ISSUE_SUB_TASK.equalsIgnoreCase(jiraIssueType.getName())) {
                issueTypes.add(jiraIssueType.getName().toUpperCase());
            }
        }

        List<JIRASubTaskableIssue> issues = null;

        try {
            issues = service.get(instanceConfigurationParameters).getEpicIssues(agileProjectValue, issueTypes, epicId);
        } catch (Exception e) {
            String errorEpicName = Providers.getLocalizationProvider(JIRAIntegrationConnector.class).getConnectorText("ERROR_EPIC_CANNOT_BE_RETRIEVED");
            // If there's an error when retrieving the Epic, it may mean that the Epic was deleted, that the JIRA server is down or that JIRA user doesn't have access to it anymore.
            PortfolioEpicSyncInfo epicSyncInfoError = new PortfolioEpicSyncInfo();
            epicSyncInfoError.setEpicName(errorEpicName);
            epicSyncInfoError.setDoneStoryPoints(0);
            epicSyncInfoError.setTotalStoryPoints(0);
            return epicSyncInfoError;
        }

        JIRAEpic jiraEpic = null;

        for (JIRAIssue issue: issues) {
            if (epicId.equalsIgnoreCase(issue.getKey())) {
                jiraEpic = (JIRAEpic) issue;
                break;
            }
        }

        if (jiraEpic == null) {
            // The Epic must have been deleted in Jira, or there's some problem to get it
            return null;
        }

        PortfolioEpicSyncInfo epicSyncInfo = new PortfolioEpicSyncInfo();
        epicSyncInfo.setEpicName(jiraEpic.getName());
        epicSyncInfo.setDoneStoryPoints(jiraEpic.getDoneStoryPoints());
        epicSyncInfo.setTotalStoryPoints(jiraEpic.getAggregatedStoryPoints());

        return epicSyncInfo;
    }

    @Override public String getEpicURI(String epicId, String agileProjectValue) {
        // The epic ID is actually the Issue Key
        return "/browse/"+epicId;
    }
}

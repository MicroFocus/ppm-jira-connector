package com.ppm.integration.agilesdk.connector.jira;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.jira.model.JIRAIssue;
import com.ppm.integration.agilesdk.connector.jira.model.JIRAProject;
import com.ppm.integration.agilesdk.model.AgileProject;
import com.ppm.integration.agilesdk.epic.PortfolioEpicCreationInfo;
import com.ppm.integration.agilesdk.epic.PortfolioEpicIntegration;
import com.ppm.integration.agilesdk.epic.PortfolioEpicSyncInfo;

import java.util.ArrayList;
import java.util.List;

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
        return service.get(instanceConfigurationParameters).createEpic(agileProjectValue, epicInfo);
    }

    @Override public PortfolioEpicSyncInfo getPortfolioEpicSyncInfo(String epicId, String agileProjectValue,
            ValueSet instanceConfigurationParameters)
    {
        JIRAIssue jiraEpic = service.get(instanceConfigurationParameters).getSingleIssue(agileProjectValue, epicId);

        if (jiraEpic == null) {
            // The Epic must have been deleted in Jira, or there's some problem to get it
            return null;
        }

        PortfolioEpicSyncInfo epicSyncInfo = new PortfolioEpicSyncInfo();
        epicSyncInfo.setEpicName(jiraEpic.getName());

        // There's no easy standard way in Jira to get the total number of SP in an Epic.
        epicSyncInfo.setDoneStoryPoints(0);
        epicSyncInfo.setTotalStoryPoints(0);

        return null;
    }

    @Override public String getEpicURI(String epicId, String agileProjectValue) {
        // The epic ID is actually the Issue Key
        return "/browse/"+epicId;
    }
}

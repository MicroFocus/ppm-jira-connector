
package com.ppm.integration.agilesdk.connector.jira;

import java.util.*;

import javax.xml.datatype.XMLGregorianCalendar;

import com.ppm.integration.agilesdk.connector.jira.model.JIRAExternalWorkItem;
import com.ppm.integration.agilesdk.connector.jira.model.JIRAIssue;
import com.ppm.integration.agilesdk.connector.jira.model.JIRATimesheetData;
import com.ppm.integration.agilesdk.pm.LinkedTaskAgileEntityInfo;
import com.ppm.integration.agilesdk.provider.LocalizationProvider;
import com.ppm.integration.agilesdk.provider.Providers;
import com.ppm.integration.agilesdk.tm.*;
import com.ppm.integration.agilesdk.ui.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wink.client.ClientRuntimeException;

import com.ppm.integration.agilesdk.ValueSet;
import com.ppm.integration.agilesdk.connector.jira.model.JIRAProject;
import com.ppm.integration.agilesdk.connector.jira.rest.util.exception.JIRAConnectivityExceptionHandler;
import com.ppm.integration.agilesdk.connector.jira.rest.util.exception.RestRequestException;

public class JIRATimeSheetIntegration extends TimeSheetIntegration {

    private final Logger logger = Logger.getLogger(this.getClass());

    private JIRAServiceProvider service = new JIRAServiceProvider();

    @Override
    public List<ExternalWorkItem> getExternalWorkItems(TimeSheetIntegrationContext timesheetContext, ValueSet values) {

        // Retrieving external work items is done with admin account.
        service.useAdminAccount();
        JIRAServiceProvider.JIRAService s = service.get(values);

        XMLGregorianCalendar start = timesheetContext.currentTimeSheet().getPeriodStartDate();
        XMLGregorianCalendar end = timesheetContext.currentTimeSheet().getPeriodEndDate();

        String projectKey = values.get(JIRAConstants.KEY_JIRA_PROJECT);
        String author = values.get(JIRAConstants.KEY_USERNAME);

        String groupBy = values.get(JIRAConstants.TS_GROUP_WORK_BY);
        String importEffortBy = values.get(JIRAConstants.TS_IMPORT_HOURS_OPTION);
        String adjustHours = values.get(JIRAConstants.TS_ADJUST_HOURS);
        String adjustHoursChoice = values.get(JIRAConstants.TS_ADJUST_HOURS_CHOICE);

        double spToHoursRatio = 8.0d;
        try {
            spToHoursRatio = Double.parseDouble(values.get(JIRAConstants.TS_SP_TO_HOURS_RATIO));
        } catch (Exception e) {
            // keep default value;
        }

        double dailyHours = 8.0d;
        try {
            dailyHours = Double.parseDouble(values.get(JIRAConstants.TS_DAILY_HOURS));
        } catch (Exception e) {
            // keep default value;
        }


        Date[] tsDays = getTsDays(timesheetContext);
        boolean[] tsWorkDays = getTsWorkDays(timesheetContext);

        JIRATimesheetData timesheetData = null;

        // First, we retrieve all timesheet lines with "raw" effort from Jira
        if (JIRAConstants.TS_IMPORT_HOURS_SP_ONLY.equals(importEffortBy)) {
            timesheetData = service.get(values).getSPTimesheetData(start, end, projectKey, author, spToHoursRatio,
                    tsDays, tsWorkDays);
        } else if (JIRAConstants.TS_IMPORT_HOURS_BOTH.equals(importEffortBy)) {
            timesheetData = s.getWorkLogsTimesheetData(start, end, projectKey, author);
            mergeTimeSheetData(timesheetData, s.getSPTimesheetData(start, end, projectKey, author, spToHoursRatio, tsDays, tsWorkDays));
        } else {
            // default = work logs only
            timesheetData = s.getWorkLogsTimesheetData(start, end, projectKey, author);
        }

        // Group by
        List<JIRAExternalWorkItem> timesheetLines = convertTimesheetDataToTimesheetLines(timesheetData, groupBy, s);

        // Then we adjust all timesheet lines effort to match daily hours if needed
        if (JIRAConstants.TS_ADJUST_HOURS_YES.equals(adjustHours)) {
            if (JIRAConstants.TS_ADJUST_HOURS_CHOICE_DAILY.equals(adjustHoursChoice)) {
                // Adjust day per day for any day with effort
                adjustPerDayTo(timesheetLines, dailyHours);

            } else if (JIRAConstants.TS_ADJUST_HOURS_CHOICE_TOTAL.equals(adjustHoursChoice)) {
                // Adjust totals & distribute over working days
                adjustPerTotalAverageTo(timesheetLines, dailyHours, tsDays, tsWorkDays);
            }
        }

        List<ExternalWorkItem> apiTimesheetLines = new ArrayList<>();
        apiTimesheetLines.addAll(timesheetLines);
        return apiTimesheetLines;
    }

    private void adjustPerTotalAverageTo(List<JIRAExternalWorkItem> lines, double dailyHours, Date[] tsDays,
            boolean[] tsWorkDays) {

        int numWorkingDaysCount = 0;
        for (boolean isWorkingDay: tsWorkDays) {
            if (isWorkingDay) {
                ++numWorkingDaysCount;
            }
        }
        double targetTotalWork = numWorkingDaysCount * dailyHours;

        double totalWork = 0;
        for (ExternalWorkItem wi : lines) {
            totalWork += wi.getTotalEffort();
        }

        // Adjust work in each line
        for (JIRAExternalWorkItem wi : lines) {

            double lineTotalEffort = wi.getTotalEffort();

            // We clean up all effort in the line.
            for (String day: new HashSet<String>(wi.getEffortBreakDown().getEffortList().keySet())) {
                wi.getEffortBreakDown().removeEffort(day);
            }

            if (numWorkingDaysCount == 0 || totalWork == 0 || lineTotalEffort == 0 ) {
                // We don't want a divide by zero
                continue;
            }

            // Insert new effort in each working day.
            double lineWorkPerWorkDay = lineTotalEffort * (targetTotalWork / totalWork) / numWorkingDaysCount;
            for (int i = 0 ; i < tsWorkDays.length ; i++) {
                if (tsWorkDays[i]) {
                    wi.addEffort(tsDays[i], lineWorkPerWorkDay);
                }
            }
        }
    }

    private void adjustPerDayTo(List<JIRAExternalWorkItem> lines, double dailyHours) {
        // First, we compute the total number of hours for each day
        Map<String, Double> totalEffortPerDay = new HashMap<>();
        for (JIRAExternalWorkItem wi : lines) {
            for (Map.Entry<String, Double> effort: wi.getEffortBreakDown().getEffortList().entrySet()) {
                Double dayTotal = totalEffortPerDay.get(effort.getKey());
                if (dayTotal == null) {
                    dayTotal = 0d;
                }
                dayTotal += effort.getValue();
                totalEffortPerDay.put(effort.getKey(), dayTotal);
            }
        }

        // Adjusting effort for each days, line per line.
        for (JIRAExternalWorkItem wi : lines) {
            ExternalWorkItemEffortBreakdown efforts = wi.getEffortBreakDown();
            Map<String, Double> effortListCopy = new HashMap<>(efforts.getEffortList());
            for (Map.Entry<String, Double> effort : effortListCopy.entrySet()) {
                Double dayTotal = totalEffortPerDay.get(effort.getKey());
                Double newDayEffort = effort.getValue() / dayTotal * dailyHours;
                efforts.removeEffort(effort.getKey());
                wi.addEffort(effort.getKey(), newDayEffort);
            }
        }
    }

    private List<JIRAExternalWorkItem> convertTimesheetDataToTimesheetLines(final JIRATimesheetData data, String groupBy,
            JIRAServiceProvider.JIRAService s)
    {
        List<JIRAExternalWorkItem> timesheetLines = new ArrayList<>();

        if (!data.hasData()) {
            return timesheetLines;
        }

        if (JIRAConstants.TS_GROUP_BY_PROJECT.equalsIgnoreCase(groupBy)) {

            // Group by Project
            timesheetLines.addAll(getTimesheetLinesGroupedByProject(data.getIssuesInProjects(), data.getEffortPerIssue(), s, ""));

        } else if (JIRAConstants.TS_GROUP_BY_EPIC.equalsIgnoreCase(groupBy)) {

            // Group by Epic
            // We must first make sure we have all the names of Epics that have effort against them

            Set<String> epicsWithoutInfo = new HashSet<>();

            for (String epicKey : data.getIssuesInEpics().keySet()) {
                if (epicKey != null && !"null".equals(epicKey) && data.getIssues().get(epicKey) == null) {
                    epicsWithoutInfo.add(epicKey);
                }
            }

            // We now retrieve the Epics info from Jira and store them into data.
            for (JIRAIssue epicWithoutInfo : s.getIssues(null, epicsWithoutInfo)) {
                data.getIssues().put(epicWithoutInfo.getKey(), epicWithoutInfo);
            }

            // One timesheet line per epic
            for (Map.Entry<String, Set<String>> issuesInEpic : data.getIssuesInEpics().entrySet()) {
                String epicKey = issuesInEpic.getKey();

                if (epicKey == null || "null".equals(epicKey)) {
                    // These are all the issues without an Epic. We need to group them by projects and have one line per project.
                    Map<String, Set<String>> issuesPerProject = new HashMap<>();
                    for (String issueKey : issuesInEpic.getValue()) {
                        JIRAIssue issue = data.getIssues().get(issueKey);
                        if (issue == null) {
                            // Shouldn't happen since we retrieve all issues
                            throw new RuntimeException("Could not find Jira Issue information for Issue "+issueKey);
                        }
                        Set<String> issues = issuesPerProject.get(issue.getProjectKey());
                        if (issues == null) {
                            issues = new HashSet<>();
                            issuesPerProject.put(issue.getProjectKey(), issues);
                        }
                        issues.add(issueKey);
                    }

                    // Create one line per project for issues without Epics
                    timesheetLines.addAll(getTimesheetLinesGroupedByProject(issuesPerProject, data.getEffortPerIssue(), s, Providers.getLocalizationProvider(JIRAIntegrationConnector.class).getConnectorText("NO_EPIC_TIMESHEET_LINE_PREFIX")));

                    continue;
                }

                final Set<String> epicIssuesKeys = issuesInEpic.getValue();

                final JIRAIssue epic = data.getIssues().get(epicKey);
                if (epic == null) {
                    // Shouldn't happen since we retrieve all issues
                    throw new RuntimeException("Could not find Jira Issue information for Epic "+epicKey);
                }

                JIRAExternalWorkItem workItem = new JIRAExternalWorkItem(epic.getName(), epic.getProjectKey(), epic.getKey(), null);
                ExternalWorkItemEffortBreakdown effort = workItem.getEffortBreakDown();

                for (String issueKey : epicIssuesKeys) {
                    Map<String, Double> issueEfforts = data.getEffortPerIssue().get(issueKey);
                    if (issueEfforts == null) {
                        continue;
                    }
                    for (Map.Entry<String, Double> dailyEffort : issueEfforts.entrySet()) {
                        workItem.addEffort(dailyEffort.getKey(), dailyEffort.getValue());
                    }
                }

                timesheetLines.add(workItem);
            }
        } else {
            // No group, one line per Issue
            for (Map.Entry<String, Map<String, Double>> effortEntry : data.getEffortPerIssue().entrySet()) {
                String issueKey = effortEntry.getKey();
                final Map<String, Double> effortPerDay = effortEntry.getValue();
                final JIRAIssue issue = data.getIssues().get(issueKey);

                JIRAExternalWorkItem workItem = new JIRAExternalWorkItem(issue.getName(), issue.getProjectKey(), issue.getEpicKey(), issue.getKey());
                workItem.getLineAgileEntityInfo().setSprintId(issue.getSprintId());
                ExternalWorkItemEffortBreakdown effort = workItem.getEffortBreakDown();
                for (Map.Entry<String, Double> dailyEffort : effortPerDay.entrySet()) {
                    workItem.addEffort(dailyEffort.getKey(), dailyEffort.getValue());
                }

                timesheetLines.add(workItem);
            }
        }

        return timesheetLines;
    }

    private List<JIRAExternalWorkItem> getTimesheetLinesGroupedByProject(final Map<String, Set<String>> issuesInProjects,
            final Map<String, Map<String, Double>> effortPerIssue, JIRAServiceProvider.JIRAService s, final String lineNamePrefix)
    {
        List<JIRAExternalWorkItem> workItems = new ArrayList<>();

        // We need project names
        List<JIRAProject> projects = s.getProjects();

        // One timesheet line per project
        for (Map.Entry<String, Set<String>> issuesInProject : issuesInProjects.entrySet()) {
            final String projectKey = issuesInProject.getKey();
            final Set<String> projectIssuesKeys = issuesInProject.getValue();

            String projectName = "?";

            for (JIRAProject proj : projects) {
                if (projectKey.equals(proj.getKey())) {
                    projectName = proj.getName();
                    break;
                }
            }

            final String lineName = projectName;

            JIRAExternalWorkItem workItem = new JIRAExternalWorkItem((StringUtils.isBlank(lineNamePrefix) ? lineName :  lineNamePrefix + lineName),
                    projectKey, null, null);
            ExternalWorkItemEffortBreakdown effort = workItem.getEffortBreakDown();

            for (String issueKey : projectIssuesKeys) {
                Map<String, Double> issueEfforts = effortPerIssue.get(issueKey);
                if (issueEfforts == null) {
                    continue;
                }
                for (Map.Entry<String, Double> dailyEffort : issueEfforts.entrySet()) {
                    workItem.addEffort(dailyEffort.getKey(), dailyEffort.getValue());
                }
            }

            workItems.add(workItem);
        }

        return workItems;
    }

    /**
     *
     * If a spData for an issue is not defined in the workLogsData for an issue ID, adds it.
     *
     */
    private void mergeTimeSheetData(JIRATimesheetData workLogsData, JIRATimesheetData spData)
    {
        Set<String> spIssues = spData.getEffortPerIssue().keySet();

        for (String spIssue : spIssues) {
            if (!workLogsData.hasData(spIssue)) {
                // Inserting sp effort into worklogEffort.
                JIRAIssue issue = spData.getIssues().get(spIssue);
                for (Map.Entry<String, Double> effort : spData.getEffortPerIssue().get(spIssue).entrySet()) {
                    workLogsData.addIssueEffort(issue, effort.getKey(), effort.getValue());
                }
            }
        }
    }

    private boolean[] getTsWorkDays(TimeSheetIntegrationContext timesheetContext) {
        try {
            return timesheetContext.getTimesheetDaysWorkDay();
        }
        catch (NoSuchMethodError e) {
            // PPM 9.4X --> We consider Saturday & Sundays are not worked.
            // We could get a better behavior by using PPM private APIs to retrieve Resource & Regional calendars,
            // but let's not do that in an Agile SDK connector. Just upgrade to PPM 9.50+ !
            List<Boolean> workDays = new ArrayList<Boolean>();

            Calendar fromDate = timesheetContext.currentTimeSheet().getPeriodStartDate().toGregorianCalendar();
            Calendar toDate = timesheetContext.currentTimeSheet().getPeriodEndDate().toGregorianCalendar();
            do {
                int dayOfWeek = fromDate.get(Calendar.DAY_OF_WEEK);
                if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                    workDays.add(Boolean.FALSE);
                } else {
                    workDays.add(Boolean.TRUE);
                }

                fromDate.add(Calendar.DATE, 1);
            } while (!fromDate.after(toDate));

            return ArrayUtils.toPrimitive(workDays.toArray(new Boolean[workDays.size()]), false);

        }
    }

    private Date[] getTsDays(TimeSheetIntegrationContext timesheetContext) {
        try {
            return timesheetContext.getTimesheetDays();
        }
        catch (NoSuchMethodError e) {
            // PPM 9.4X
            List<Date> days = new ArrayList<Date>();

            Calendar fromDate = timesheetContext.currentTimeSheet().getPeriodStartDate().toGregorianCalendar();
            Calendar toDate = timesheetContext.currentTimeSheet().getPeriodEndDate().toGregorianCalendar();
            do {
                days.add(fromDate.getTime());
                fromDate.add(Calendar.DATE, 1);
            } while (!fromDate.after(toDate));

            return days.toArray(new Date[days.size()]);
        }
    }

    @Override
    public List<Field> getMappingConfigurationFields(ValueSet arg0) {

        final LocalizationProvider lp = Providers.getLocalizationProvider(JIRAIntegrationConnector.class);


        return Arrays.asList(new Field[] {new PlainText(JIRAConstants.KEY_USERNAME, "USERNAME", "", true),
                new PasswordText(JIRAConstants.KEY_PASSWORD, "PASSWORD", "", true),
                new LineBreaker(),
                new DynamicDropdown(JIRAConstants.KEY_JIRA_PROJECT, "JIRA_PROJECT", JIRAConstants.TS_ALL_PROJECTS, "", false) {

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


                        Option option = new Option(JIRAConstants.TS_ALL_PROJECTS, lp.getConnectorText("TS_ALL_PROJECTS"));
                        optionList.add(option);

                        for (JIRAProject project : list) {
                            option = new Option(project.getKey(), project.getName());

                            optionList.add(option);
                        }

                        return optionList;
                    }
                },

                new DynamicDropdown(JIRAConstants.TS_GROUP_WORK_BY,"TS_GROUP_WORK_BY",JIRAConstants.TS_GROUP_BY_EPIC, "", true) {

                    @Override public List<String> getDependencies() {
                        return Arrays.asList(new String[] {JIRAConstants.KEY_USERNAME});
                    }

                    @Override public List<Option> getDynamicalOptions(ValueSet values) {

                        return Arrays.asList(new Option[] { new Option(JIRAConstants.TS_GROUP_BY_PROJECT,lp.getConnectorText("TS_GROUP_BY_PROJECT")),
                                new Option(JIRAConstants.TS_GROUP_BY_EPIC,lp.getConnectorText("TS_GROUP_BY_EPIC")),
                                new Option(JIRAConstants.TS_GROUP_BY_ISSUE,lp.getConnectorText("TS_GROUP_BY_ISSUE"))});
                    }
                },


                new LineBreaker(),

                new DynamicDropdown(JIRAConstants.TS_IMPORT_HOURS_OPTION,"TS_IMPORT_HOURS_OPTION",JIRAConstants.TS_IMPORT_HOURS_BOTH, "", true) {
                    @Override public List<String> getDependencies() {
                        return Arrays.asList(new String[] {JIRAConstants.KEY_USERNAME});
                    }

                    @Override public List<Option> getDynamicalOptions(ValueSet values) {

                        return Arrays.asList(new Option[] { new Option(JIRAConstants.TS_IMPORT_HOURS_HOURS_ONLY,lp.getConnectorText("TS_IMPORT_HOURS_HOURS_ONLY")),
                                new Option(JIRAConstants.TS_IMPORT_HOURS_SP_ONLY,lp.getConnectorText("TS_IMPORT_HOURS_SP_ONLY")),
                                new Option(JIRAConstants.TS_IMPORT_HOURS_BOTH,lp.getConnectorText("TS_IMPORT_HOURS_BOTH"))});
                    }
                },

                new NumberText(JIRAConstants.TS_SP_TO_HOURS_RATIO, "TS_SP_TO_HOURS_RATIO", "8", "", false) {
                    @Override
                    public List<String> getStyleDependencies() {
                        return Arrays.asList(new String[] { JIRAConstants.TS_IMPORT_HOURS_OPTION});
                    }

                    @Override
                    public FieldAppearance getFieldAppearance(ValueSet values) {

                        String importHoursType = values.get(JIRAConstants.TS_IMPORT_HOURS_OPTION);
                        if (JIRAConstants.TS_IMPORT_HOURS_SP_ONLY.equals(importHoursType) || JIRAConstants.TS_IMPORT_HOURS_BOTH.equals(importHoursType)) {
                            return new FieldAppearance("required", "disabled");
                        } else  {
                            // We don't need SP to Hours ratio if we import hours only.
                            return new FieldAppearance("disabled", "required");
                        }
                    }
                },

                new LineBreaker(),

                new DynamicDropdown(JIRAConstants.TS_ADJUST_HOURS,"TS_ADJUST_HOURS",JIRAConstants.TS_ADJUST_HOURS_NO, "", true) {
                    @Override public List<String> getDependencies() {
                        return Arrays.asList(new String[] { JIRAConstants.KEY_USERNAME});
                    }

                    @Override public List<Option> getDynamicalOptions(ValueSet values) {

                        return Arrays.asList(new Option[] { new Option(JIRAConstants.TS_ADJUST_HOURS_NO,lp.getConnectorText("TS_ADJUST_HOURS_NO")),
                                new Option(JIRAConstants.TS_ADJUST_HOURS_YES,lp.getConnectorText("TS_ADJUST_HOURS_YES"))});
                    }
                },

                new DynamicDropdown(JIRAConstants.TS_ADJUST_HOURS_CHOICE,"TS_ADJUST_HOURS_CHOICE",JIRAConstants.TS_ADJUST_HOURS_CHOICE_DAILY, "", true) {
                    @Override public List<String> getDependencies() {
                        return Arrays.asList(new String[] { JIRAConstants.TS_ADJUST_HOURS});
                    }

                    @Override public List<Option> getDynamicalOptions(ValueSet values) {

                        return Arrays.asList(new Option[] { new Option(JIRAConstants.TS_ADJUST_HOURS_CHOICE_DAILY,lp.getConnectorText("TS_ADJUST_HOURS_CHOICE_DAILY")),
                                new Option(JIRAConstants.TS_ADJUST_HOURS_CHOICE_TOTAL,lp.getConnectorText("TS_ADJUST_HOURS_CHOICE_TOTAL"))});
                    }

                    @Override
                    public List<String> getStyleDependencies() {
                        return Arrays.asList(new String[] { JIRAConstants.TS_ADJUST_HOURS});
                    }

                    @Override
                    public FieldAppearance getFieldAppearance(ValueSet values) {

                        String adjust = values.get(JIRAConstants.TS_ADJUST_HOURS);
                        if (JIRAConstants.TS_ADJUST_HOURS_YES.equals(adjust)) {
                            return new FieldAppearance("required", "disabled");
                        } else  {
                            // We don't need this if we do not adjust to daily hours
                            return new FieldAppearance("disabled", "required");
                        }
                    }
                },

                new NumberText(JIRAConstants.TS_DAILY_HOURS, "TS_DAILY_HOURS", "8", "", false) {
                    @Override
                    public List<String> getStyleDependencies() {
                        return Arrays.asList(new String[] { JIRAConstants.TS_ADJUST_HOURS});
                    }

                    @Override
                    public FieldAppearance getFieldAppearance(ValueSet values) {

                        String adjust = values.get(JIRAConstants.TS_ADJUST_HOURS);
                        if (JIRAConstants.TS_ADJUST_HOURS_YES.equals(adjust)) {
                            return new FieldAppearance("required", "disabled");
                        } else  {
                            // We don't need daily hours if we do not adjust to daily hours
                            return new FieldAppearance("disabled", "required");
                        }
                    }
                }
        });
    }


    @Override
    /**
     * There's no feature linkage in JIRA. You link a whole project or a version or an Epic in a work plan.
     * In timesheets, you group by project or Epic (not by version since one issue can belong to multiple versions).
     * So we only need to care about Epics, since Projects will be a match by definition.
     * We must also ignore
     */
    public List<String> getBestTasksMatches(Map<String, LinkedTaskAgileEntityInfo> candidateTasksInfo, TimeSheetLineAgileEntityInfo timesheetLineAgileEntityInfo) {

        List<String> taskIds = new ArrayList<>(candidateTasksInfo.keySet());

        // Fist, we remove all candidate tasks that are linked to a specific Release (i.e. JIRA Version), they will never be a match for a timesheet line.
        for (Map.Entry<String, LinkedTaskAgileEntityInfo> entry : new HashMap<>(candidateTasksInfo).entrySet()) {
            if (!StringUtils.isBlank(entry.getValue().getReleaseId())) {
                candidateTasksInfo.remove(entry.getKey());
            }
        }

        boolean hasTimesheetEpic = !StringUtils.isBlank(timesheetLineAgileEntityInfo.getEpicId());
        boolean hasTimesheetIssue = !StringUtils.isBlank(timesheetLineAgileEntityInfo.getBacklogItemId());

        if (hasTimesheetIssue) {
            // A timesheet line about a specific issue is a match for:
            // - (First choice): Matching Project (always), Matching Epic between WP & TS info
            // - (Second Choice): Matching Project (always), No Epic defined in WP but Epic defined in TS.

            // First choice
            for (Map.Entry<String, LinkedTaskAgileEntityInfo> wpInfo : candidateTasksInfo.entrySet()) {

                if (!hasTimesheetEpic) {
                    // No epic defined for timesheet issue: Both epics should be empty
                    if (StringUtils.isBlank(wpInfo.getValue().getEpicId())) {
                        taskIds.add(wpInfo.getKey());
                    }
                } else {
                    // Epic defined for timesheet issue: Both Epics should match
                    if (timesheetLineAgileEntityInfo.getEpicId().equals(wpInfo.getValue().getEpicId())) {
                        taskIds.add(wpInfo.getKey());
                    }
                }
            }

            // Second Choice
            for (Map.Entry<String, LinkedTaskAgileEntityInfo> wpInfo : candidateTasksInfo.entrySet()) {
                if (hasTimesheetEpic) {
                    // Epic defined for timesheet issue: WP Epic should not be defined
                    if (StringUtils.isBlank(wpInfo.getValue().getEpicId())) {
                        taskIds.add(wpInfo.getKey());
                    }
                }
            }
        } else if (hasTimesheetEpic) {
            // A timesheet line about a specific Epic is a match for:
            // - (Perfect Match): Matching Epics
            // - (Second Choice): WP has empty epic (but project will match)

            // Perfect Match
            for (Map.Entry<String, LinkedTaskAgileEntityInfo> wpInfo : candidateTasksInfo.entrySet()) {
                if (timesheetLineAgileEntityInfo.getEpicId().equals(wpInfo.getValue().getEpicId())) {
                    taskIds.add(wpInfo.getKey());
                }
            }

            // Second Choice
            for (Map.Entry<String, LinkedTaskAgileEntityInfo> wpInfo : candidateTasksInfo.entrySet()) {
                if (StringUtils.isBlank(wpInfo.getValue().getEpicId())) {
                    taskIds.add(wpInfo.getKey());
                }
            }
        } else {
            // A timesheet line about a specific project is only a match for a

        }

        return taskIds;
    }
}

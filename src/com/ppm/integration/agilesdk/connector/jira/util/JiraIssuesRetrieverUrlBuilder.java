package com.ppm.integration.agilesdk.connector.jira.util;

import com.ppm.integration.agilesdk.connector.jira.JIRAConstants;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * A Jira Search URL can have many parameters, even outside of the JQL expression. This class will hold the values of all the parameters,
 * can be passed around and modified, and will only generate the actual URL when the Issues retrieval needs to be made.
 */
public class JiraIssuesRetrieverUrlBuilder {

    private IssueRetrievalType retrievalType = IssueRetrievalType.SEARCH;

    private String baseUri = null;

    private String expandLevel = null;

    private int maxResults = 1000;

    private int startAt = 0;

    private String boardId;

    // JQL Parameters //////////////////

    private String projectKey = null;

    private Set<String> nonNullCustomFields = new HashSet<String>();

    private Map<String, String> customFieldsEqualsConstraints = new HashMap<String, String>();

    private String orderBy = "created";

    private List<String> andConstraints = new ArrayList<String>();

    private List<String> orConstraints = new ArrayList<String>();

    private Set<String> issuesTypes = new HashSet<String>();

    // These are the fields that we always want to retrieve from the issues.
    private String[] fields = {"key", "issuetype", "fixVersions", "summary", "worklog", "creator",
            "parent", "created", "priority", "assignee", "updated", "status","timetracking"};

    private List<String> extraFields = new ArrayList<String>();

    public JiraIssuesRetrieverUrlBuilder(String baseUri) {

        // Base URI should not have a trailing slash.
        while (baseUri.endsWith("/")) {
            baseUri = baseUri.substring(0, baseUri.length()-1);
        }

        this.baseUri = baseUri;
    }

    public JiraIssuesRetrieverUrlBuilder addExtraFields(String... extras) {
        for (String extraField: extras) {
            extraFields.add(extraField);
        }
        return this;
    }

    public JiraIssuesRetrieverUrlBuilder setBoardType(String boardId) {
        this.boardId = boardId;
        this.retrievalType = IssueRetrievalType.BOARD;
        return this;
    }

    public String toUrlString() {


        StringBuilder searchUrl = new StringBuilder(baseUri);

        switch(retrievalType) {
            case BOARD:
                searchUrl.append(JIRAConstants.BOARD_SUFFIX + "/" + boardId + "/issue?");
                break;
            default: // SEARCH
                searchUrl.append(JIRAConstants.API_VERSION2_API_ROOT + "search?");
        }


        List<String> urlParameters = new ArrayList<String>();

        if (maxResults > 0) {
            urlParameters.add("maxResults=" + maxResults);
        }

        urlParameters.add("startAt=" + startAt);

        if (!StringUtils.isBlank(expandLevel)) {
            urlParameters.add("expand=" + expandLevel);
        }

        List<String> fieldsNames = new ArrayList<String>(extraFields);
        fieldsNames.addAll(Arrays.asList(fields));

        urlParameters.add("fields=" + StringUtils.join(fieldsNames, ","));

        String jql = getJQLString();

        if (!StringUtils.isBlank(jql)) {
            urlParameters.add("jql=" + jql);
        }

        searchUrl.append(StringUtils.join(urlParameters, "&"));

        String url = encodeUrl(searchUrl.toString());

        return url;

    }

    private String encodeUrl(String url) {
        return url.replaceAll(" ", "%20").replaceAll(">", "%3E").replaceAll("<", "%3C").replaceAll("-", "%2D")
                .replaceAll("!", "%21");
    }

    private String getJQLString() {
        List<String> constraints = new ArrayList<String>(andConstraints);

        if (!StringUtils.isBlank(projectKey)) {
            constraints.add("project="+projectKey);
        }

        for (String nonNullCf : nonNullCustomFields) {
            constraints.add("cf["+nonNullCf+"]!=null");
        }

        for (Map.Entry<String, String> cfEqualsConstraint : customFieldsEqualsConstraints.entrySet()) {
            constraints.add("cf["+cfEqualsConstraint.getKey()+"]="+cfEqualsConstraint.getValue());
        }

        String jql = StringUtils.join(constraints, " and ");

        jql += getIssueTypeConstraintJQL();

        if (!orConstraints.isEmpty()) {
            jql = "("+jql+") or " + StringUtils.join(orConstraints, " or ");
        }

        if (!StringUtils.isBlank(orderBy)) {
            jql += " order by "+orderBy;
        }

        return jql;
    }

    private String getIssueTypeConstraintJQL() {

        if (issuesTypes == null || issuesTypes.isEmpty()) {
            return "";
        }

        if (issuesTypes != null && !issuesTypes.isEmpty()) {
            return " and issuetype in(" + StringUtils.join(issuesTypes, ",") + ") ";
        }

        return "";
    }

    public JiraIssuesRetrieverUrlBuilder setProjectKey(String projectKey) {
        this.projectKey = projectKey;
        return this;
    }

    public JiraIssuesRetrieverUrlBuilder setExpandLevel(String expandLevel) {
        this.expandLevel = expandLevel;
        return this;
    }

    public JiraIssuesRetrieverUrlBuilder setOrderBy(String orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    public JiraIssuesRetrieverUrlBuilder setIssuesTypes(Set<String> issuesTypes) {
        this.issuesTypes = issuesTypes;
        return this;
    }

    public JiraIssuesRetrieverUrlBuilder addNonNullCustomField(String cf) {
        nonNullCustomFields.add(cf.substring("customfield_".length()));
        return this;
    }

    /**
     * Jira will refer to custom fields as "customfield_12345" in the JSon, but if you want to put a constraint on this field
     * you must use cf[12345]=foobar.
     */
    public JiraIssuesRetrieverUrlBuilder addCustomFieldEqualsConstraint(String cf, String value) {
        customFieldsEqualsConstraints.put(cf.substring("customfield_".length()), value);
        return this;
    }

    public JiraIssuesRetrieverUrlBuilder addAndConstraint(String constraint) {
        andConstraints.add(constraint);
        return this;
    }

    public JiraIssuesRetrieverUrlBuilder setMaxResults(int maxResults) {
        this.maxResults = maxResults;
        return this;
    }

    public void setStartAt(int startAt) {
        this.startAt = startAt;
    }

    // The OR constraints will be added next to all the AND constraints.
    // You can use it if you want to make some exceptions to all the AND constraints.
    public void addOrConstraint(String orConstraint) {
        orConstraints.add(orConstraint);
    }

    public enum IssueRetrievalType {
        SEARCH,
        BOARD
    }
}

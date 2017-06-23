package com.ppm.integration.agilesdk.connector.jira.util;

import com.ppm.integration.agilesdk.connector.jira.JIRAConstants;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * A Jira Search URL can have many parameters, even outside of the JQL expression. This class will hold the values of all the parameters,
 * can be passed around and modified, and will only generate the actual URL when the Search needs to be made.
 */
public class JiraSearchUrlBuilder {

    private String baseUri = null;

    private String expandLevel = null;

    private String fields = null;

    private int maxResults = 1000;

    // JQL Parameters //////////////////

    private String projectKey = null;

    // If true, add a constraints that sprint!=null
    private boolean onlyIncludePlanned = false;

    // If true, add a constraint that issueType!=sub-task
    private boolean excludeSubTasks = true;

    private Set<String> nonNullCustomFields = new HashSet<String>();

    private Map<String, String> customFieldsEqualsConstraints = new HashMap<String, String>();

    private String orderBy = null;

    private List<String> andConstraints = new ArrayList<String>();

    private Map<String, Boolean> issuesTypes = null;

    public JiraSearchUrlBuilder(String baseUri) {

        // Base URI should not have a trailing slash.
        while (baseUri.endsWith("/")) {
            baseUri = baseUri.substring(0, baseUri.length()-1);
        }

        this.baseUri = baseUri;
    }

    public String toUrlString() {
        StringBuilder searchUrl = new StringBuilder(baseUri + JIRAConstants.API_VERSION_API_ROOT + "search?");

        List<String> urlParameters = new ArrayList<String>();

        if (maxResults > 0) {
            urlParameters.add("maxResults="+maxResults);
        }

        if (!StringUtils.isBlank(expandLevel)) {
            urlParameters.add("expand="+expandLevel);
        }

        if (!StringUtils.isBlank(fields)) {
            urlParameters.add("fields="+fields);
        }

        String jql = getJQLString();

        if (!StringUtils.isBlank(jql)) {
            urlParameters.add("jql="+jql);
        }

        searchUrl.append(StringUtils.join(urlParameters, "&"));

        String url = encodeUrl(searchUrl.toString());

        // System.out.println("###URL: "+url);

        return url;

    }

    private String encodeUrl(String url) {
        return url.replaceAll(" ", "%20").replaceAll(">", "%3E").replaceAll("<", "%3C").replaceAll("-", "%2D")
                .replaceAll("!", "%21");
    }

    private String getJQLString() {
        List<String> constraints = new ArrayList<String>(andConstraints);

        if (onlyIncludePlanned) {
            constraints.add("sprint!=null");
        }

        if (excludeSubTasks) {
            constraints.add("issueType!=sub-task");
        }

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

        if (!StringUtils.isBlank(orderBy)) {
            jql += " order by "+orderBy;
        }

        return jql;
    }

    private String getIssueTypeConstraintJQL() {

        if (issuesTypes == null || issuesTypes.isEmpty()) {
            return "";
        }

        List<String> selectedIssuesTypes = new ArrayList<String>();

        for (Map.Entry<String, Boolean> e : issuesTypes.entrySet()) {
            if (e.getValue()) {
                selectedIssuesTypes.add(e.getKey());
            }
        }

        if (!selectedIssuesTypes.isEmpty()) {
            return " and issuetype in(" + StringUtils.join(selectedIssuesTypes, ",") + ") ";
        }

        return "";
    }

     public JiraSearchUrlBuilder setOnlyIncludePlanned(boolean onlyIncludePlanned) {
        this.onlyIncludePlanned = onlyIncludePlanned;
        return this;
    }


    public JiraSearchUrlBuilder setExcludeSubTasks(boolean excludeSubTasks) {
        this.excludeSubTasks = excludeSubTasks;
        return this;
    }

    public JiraSearchUrlBuilder setProjectKey(String projectKey) {
        this.projectKey = projectKey;
        return this;
    }

    public JiraSearchUrlBuilder setExpandLevel(String expandLevel) {
        this.expandLevel = expandLevel;
        return this;
    }

     public JiraSearchUrlBuilder setFields(String fields) {
        this.fields = fields;
        return this;
    }

    public JiraSearchUrlBuilder setOrderBy(String orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    public JiraSearchUrlBuilder setIssuesTypes(Map<String, Boolean> issuesTypes) {
        this.issuesTypes = issuesTypes;
        return this;
    }

    public JiraSearchUrlBuilder addNonNullCustomField(String cf) {
        nonNullCustomFields.add(cf);
        return this;
    }

    public JiraSearchUrlBuilder addCustomFieldEqualsConstraint(String cf, String value) {
        customFieldsEqualsConstraints.put(cf, value);
        return this;
    }

    public JiraSearchUrlBuilder addAndConstraint(String constraint) {
        andConstraints.add(constraint);
        return this;
    }

    public JiraSearchUrlBuilder setMaxResults(int maxResults) {
        this.maxResults = maxResults;
        return this;
    }


}


package com.ppm.integration.agilesdk.connector.jira.rest.util;

public interface IRestConfig {

    void setProxy(String proxyHost, String proxyPort);

    String getProxyHost();

    Integer getProxyPort();

    void setBasicAuthorizationCredentials(String username, String password, String pat);

    String getBasicAuthorizationToken();
}

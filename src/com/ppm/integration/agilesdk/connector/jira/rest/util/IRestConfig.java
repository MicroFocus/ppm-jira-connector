
package com.ppm.integration.agilesdk.connector.jira.rest.util;

import org.apache.wink.client.ClientConfig;

public interface IRestConfig {

    ClientConfig setProxy(String proxyHost, String proxyPort);

    void setBasicAuthorizationCredentials(String username, String password, String pat);

    String getBasicAuthorizationToken();

    ClientConfig getClientConfig();
}

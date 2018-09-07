
package com.ppm.integration.agilesdk.connector.jira.rest.util;

import org.apache.commons.codec.binary.Base64;
import org.apache.wink.client.ClientConfig;

public class JIRARestConfig implements IRestConfig {
    private ClientConfig clientConfig;

    private String basicAuthenticationToken;

    public ClientConfig getClientConfig() {
        return clientConfig;
    }

    public JIRARestConfig() {
        clientConfig = new ClientConfig();
    }

    @Override
    public ClientConfig setProxy(String proxyHost, String proxyPort) {

        if (proxyHost != null && !proxyHost.isEmpty() && proxyPort != null && !proxyPort.isEmpty()) {
            clientConfig.proxyHost(proxyHost);
            clientConfig.proxyPort(Integer.parseInt(proxyPort));
        }
        return clientConfig;
    }


    // recommend this method to set the authentication for now
    @Override
    public String getBasicAuthorizationToken() {

        return basicAuthenticationToken;
    }

    @Override
    public void setBasicAuthorizationCredentials(String username, String password) {

        String basicToken = new String(Base64.encodeBase64((username + ":" + password).getBytes()));
        basicAuthenticationToken = RestConstants.BASIC_AUTHENTICATION_PREFIX + basicToken;
    }

}

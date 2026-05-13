
package com.ppm.integration.agilesdk.connector.jira.rest.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class JIRARestConfig implements IRestConfig {
    private String proxyHost;
    private Integer proxyPort;

    private String basicAuthenticationToken;

    public JIRARestConfig() {
        // no-op
    }

    @Override
    public void setProxy(String proxyHost, String proxyPort) {

        if (proxyHost != null && !proxyHost.isEmpty() && proxyPort != null && !proxyPort.isEmpty()) {
            this.proxyHost = proxyHost;
            this.proxyPort = Integer.parseInt(proxyPort);
        } else {
            this.proxyHost = null;
            this.proxyPort = null;
        }
    }

    @Override
    public String getProxyHost() {
        return proxyHost;
    }

    @Override
    public Integer getProxyPort() {
        return proxyPort;
    }


    // recommend this method to set the authentication for now
    @Override
    public String getBasicAuthorizationToken() {

        return basicAuthenticationToken;
    }

    @Override
    public void setBasicAuthorizationCredentials(String username, String password, String pat) {
        {
            String basicToken = Base64.getEncoder().encodeToString((username + ":" + pat).getBytes(StandardCharsets.UTF_8));
            basicAuthenticationToken = RestConstants.BASIC_AUTHENTICATION_PREFIX + basicToken;
        }
       /* if (!StringUtils.isBlank(pat)) {
            basicAuthenticationToken = RestConstants.BEARER_AUTHENTICATION_PREFIX + pat;
        } else {
            String basicToken = new String(Base64.encodeBase64((username + ":" + password).getBytes()));
            basicAuthenticationToken = RestConstants.BASIC_AUTHENTICATION_PREFIX + basicToken;
        }*/
    }

}

package com.hp.ppm.integration.jira.rest.util;

import org.apache.wink.client.ClientConfig;

public interface IRestConfig {
	ClientConfig setProxy(String proxyHost, String proxyPort);

	ClientConfig setBasicAuthorizatonWithBasicAuthHandler(String username, String password);

	void setBasicAuthorizaton(String username, String password);

	String getBasicAuthorizaton();

	ClientConfig getClientConfig();
}

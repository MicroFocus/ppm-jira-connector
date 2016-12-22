package com.hp.ppm.integration.jira.rest.util;

import org.apache.commons.codec.binary.Base64;
import org.apache.wink.client.ClientConfig;
import org.apache.wink.client.handlers.BasicAuthSecurityHandler;

import com.hp.ppm.integration.jira.JIRAConstants;

public class JIRARestConfig implements IRestConfig {
	private ClientConfig clientConfig;
	private BasicAuthSecurityHandler basicAuthHandler;
	private String basicAuthentication;

	public ClientConfig getClientConfig() {
		return clientConfig;
	}

	public JIRARestConfig() {
		clientConfig = new ClientConfig();
	}

	@Override
	public ClientConfig setProxy(String proxyHost, String proxyPort) {
		// TODO Auto-generated method stub
		if (proxyHost != null && !proxyHost.isEmpty() && proxyPort != null && !proxyPort.isEmpty()) {
			clientConfig.proxyHost(proxyHost);
			clientConfig.proxyPort(Integer.parseInt(proxyPort));
		}
		return clientConfig;
	}

	// ?? some responses not expected when querying with this method
	@Override
	public ClientConfig setBasicAuthorizatonWithBasicAuthHandler(String username, String password) {
		// TODO Auto-generated method stub
		basicAuthHandler = basicAuthHandler == null ? new BasicAuthSecurityHandler() : basicAuthHandler;
		basicAuthHandler.setUserName(username);
		basicAuthHandler.setPassword(password);
		clientConfig.handlers(basicAuthHandler);
		return clientConfig;
	}

	// recommend this method to set the authentication for now
	@Override
	public String getBasicAuthorizaton() {
		// TODO Auto-generated method stub

		return basicAuthentication;
	}

	@Override
	public void setBasicAuthorizaton(String username, String password) {
		// TODO Auto-generated method stub
		String basicToken = new String(Base64.encodeBase64((username + ":" + password).getBytes()));
		basicAuthentication = RestConstants.BASIC_AUTHENTICATION_PREFIX + basicToken;
	}

}

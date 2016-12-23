package com.ppm.integration.agilesdk.connector.jira.rest.util;

import javax.ws.rs.core.MediaType;

import org.apache.wink.client.ClientResponse;
import org.apache.wink.client.Resource;
import org.apache.wink.client.RestClient;

import com.ppm.integration.agilesdk.connector.jira.rest.util.exception.RestRequestException;

public class RestWrapper {
	private RestClient restClient;
	private IRestConfig config;

	public IRestConfig getConfig() {
		return config;
	}

	public void setConfig(IRestConfig config) {
		this.config = config;
	}

	public RestClient getRestClient() {
		return restClient;
	}

	public void setRestClient(RestClient restClient) {
		this.restClient = restClient;
	}

	public RestWrapper() {
		restClient = new RestClient();
	}

	public RestWrapper(IRestConfig config) {
		this.config = config;
		restClient = createRestClient(config);
	}

	public RestClient createRestClient(IRestConfig config) {
		restClient = new RestClient(config.getClientConfig());
		return restClient;
	}

	public Resource getResource(String uri) {
		return restClient.resource(uri);
	}

	public Resource getResource(IRestConfig config, String uri) {
		restClient = createRestClient(config);
		return restClient.resource(uri);
	}

	public Resource getJIRAResource(IRestConfig config, String uri) {
		restClient = createRestClient(config);
		Resource resource = restClient.resource(uri).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON).header("Authorization", config.getBasicAuthorizaton());
		return resource;
	}

	public Resource getJIRAResource(String uri) {
		Resource resource = restClient.resource(uri).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON).header("Authorization", config.getBasicAuthorizaton());
		return resource;
	}

	public ClientResponse sendGet(String uri) {
		Resource resource = this.getJIRAResource(uri);
		ClientResponse response = resource.get();

		int statusCode = response.getStatusCode();

		if (statusCode != 200) {
			throw new RestRequestException(statusCode, response.getMessage());
		}

		return response;
	}
}

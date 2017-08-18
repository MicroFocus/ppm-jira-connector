
package com.ppm.integration.agilesdk.connector.jira.rest.util;

import com.ppm.integration.agilesdk.connector.jira.rest.util.exception.RestRequestException;
import org.apache.wink.client.ClientResponse;
import org.apache.wink.client.Resource;
import org.apache.wink.client.RestClient;
import java.net.*;

import javax.ws.rs.core.MediaType;
import java.io.UnsupportedEncodingException;

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

    public Resource getJIRAResource(String urlAdd) {
        Resource resource;
        try {
            URL url = new URL(urlAdd);
            String urlPath = url.getHost();
            if (url.getPort() > 0) {
                urlPath = urlPath + ":" + url.getPort();
            }
            URI uri = null;
            try {
                uri = new URI(url.getProtocol(), urlPath, url.getPath(), url.getQuery() == null ? null : URLDecoder.decode(url.getQuery(), "UTF-8"), null);
            } catch (UnsupportedEncodingException e) {
                // This will never happen.
                throw new RuntimeException("Impossible encoding error occured", e);
            }
            resource = restClient.resource(uri).contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON).header("Authorization", config.getBasicAuthorizaton());
        } catch (MalformedURLException e) {
            throw new RestRequestException( // is a malformed URL
                    400, String.format("%s is a malformed URL", urlAdd));
        } catch (URISyntaxException e) {
            throw new RestRequestException(400, String.format("%s is a malformed URL", urlAdd));
        }
        return resource;
    }

    public ClientResponse sendGet(String uri) {
        Resource resource = this.getJIRAResource(uri);
        ClientResponse response = resource.get();

        int statusCode = response.getStatusCode();

        if (statusCode != 200) {
            // for easier troubleshooting, include the request URI in the exception message
            throw new RestRequestException(
                    statusCode,
                    String.format("Unexpected HTTP response status code %s for %s", statusCode, uri));
        }

        return response;
    }

    public ClientResponse sendPost(String uri, String jsonPayload, int expectedHttpStatusCode) {
        Resource resource = this.getJIRAResource(uri);
        ClientResponse response = resource.post(jsonPayload);

        int statusCode = response.getStatusCode();

        if (statusCode != expectedHttpStatusCode) {
            // for easier troubleshooting, include the request URI in the exception message
            throw new RestRequestException(statusCode,
                    String.format("Unexpected HTTP response status code %s for %s, expected %s", statusCode, uri, expectedHttpStatusCode));
        }

        return response;
    }
}

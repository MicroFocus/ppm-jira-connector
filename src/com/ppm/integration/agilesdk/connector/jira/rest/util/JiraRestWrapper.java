
package com.ppm.integration.agilesdk.connector.jira.rest.util;

import com.ppm.integration.agilesdk.connector.jira.rest.util.exception.RestRequestException;
import org.apache.commons.lang.StringUtils;
import org.apache.wink.client.ClientResponse;
import org.apache.wink.client.Resource;
import org.apache.wink.client.RestClient;

import javax.ws.rs.core.MediaType;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.UUID;

public class JiraRestWrapper {

    private RestClient restClient;
    private IRestConfig config;

    public JiraRestWrapper(IRestConfig config) {
        this.config = config;
        restClient = createRestClient(config);
    }

    private RestClient createRestClient(IRestConfig config) {
        restClient = new RestClient(config.getClientConfig());
        return restClient;
    }

    /**

     * @param includeContentTypeHeader if true, we'll include the JSon "Content-Type" header. If false, we'll not include any Content-type header (to use when using GET or DELETE).
     * @return
     */
    private Resource getJIRAResource(String urlAdd, boolean includeContentTypeHeader, String uuid) {
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
                throw new RuntimeException("Impossible encoding error occurred", e);
            }
            resource = restClient.resource(uri).accept(MediaType.APPLICATION_JSON).header("Authorization", config.getBasicAuthorizationToken());

            // Following header is required for easy HTTP request tracing in systems such as DataPower.
            if (uuid != null) {
                resource.header("X-B3-TraceId", uuid);
            }

            if (includeContentTypeHeader) {
                resource.contentType(MediaType.APPLICATION_JSON);
            }

        } catch (MalformedURLException e) {
            throw new RestRequestException( // is a malformed URL
                    400, String.format("%s is a malformed URL", urlAdd));
        } catch (URISyntaxException e) {
            throw new RestRequestException(400, String.format("%s is a malformed URL", urlAdd));
        }
        return resource;
    }

    public ClientResponse sendGet(String uri) {
        String uuid = UUID.randomUUID().toString();
        Resource resource = this.getJIRAResource(uri, false, uuid);
        ClientResponse response = resource.get();

        checkResponseStatus(200, response, uri, "GET", null, uuid);

        return response;
    }

    private void checkResponseStatus(int expectedHttpStatusCode, ClientResponse response, String uri, String verb, String payload, String uuid) {

        if (response.getStatusCode() != expectedHttpStatusCode) {
            StringBuilder errorMessage = new StringBuilder(String.format("## Unexpected HTTP response status code %s for %s uri %s, expected %s", response.getStatusCode(), verb,  uri, expectedHttpStatusCode));
            if (uuid != null) {
                errorMessage.append(System.lineSeparator()).append("Value of HTTP tracking header X-B3-TraceId:").append(uuid);
            }
            if (payload != null) {
                errorMessage.append(System.lineSeparator()).append(System.lineSeparator()).append("# Sent Payload:").append(System.lineSeparator()).append(payload);
            }
            String responseStr = null;
            try {
                responseStr = response.getEntity(String.class);
            } catch (Exception e) {
                // we don't do anything if we cannot get the response.
            }
            if (!StringUtils.isBlank(responseStr)) {
                errorMessage.append(System.lineSeparator()).append(System.lineSeparator()).append("# Received Response:").append(System.lineSeparator()).append(responseStr);
            }

            throw new RestRequestException(response.getStatusCode(), errorMessage.toString());
        }

    }

    public ClientResponse sendPost(String uri, String jsonPayload, int expectedHttpStatusCode) {
        String uuid = UUID.randomUUID().toString();
        Resource resource = this.getJIRAResource(uri, true, uuid);
        ClientResponse response = resource.post(jsonPayload);
        checkResponseStatus(expectedHttpStatusCode, response, uri, "POST", jsonPayload, uuid);

        return response;
    }

    public ClientResponse sendPut(String uri, String jsonPayload, int expectedHttpStatusCode) {
        String uuid = UUID.randomUUID().toString();
        Resource resource = this.getJIRAResource(uri,true, uuid);
        ClientResponse response = resource.put(jsonPayload);

        checkResponseStatus(expectedHttpStatusCode, response, uri, "PUT", jsonPayload, uuid);

        return response;
    }
}

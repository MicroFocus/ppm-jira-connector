
package com.ppm.integration.agilesdk.connector.jira.rest.util;

import com.ppm.integration.agilesdk.connector.jira.rest.util.exception.RestRequestException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class JiraRestWrapper {

    private final IRestConfig config;

    public JiraRestWrapper(IRestConfig config) {
        this.config = config;
    }

    /**

     * @param includeContentTypeHeader if true, we'll include the JSon "Content-Type" header. If false, we'll not include any Content-type header (to use when using GET or DELETE).
     * @return
     */
    private HttpURLConnection getJIRAConnection(String urlAdd, boolean includeContentTypeHeader, String uuid, HttpMethod method) {
        HttpURLConnection connection;
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

            Proxy proxy = null;
            if (!StringUtils.isBlank(config.getProxyHost()) && config.getProxyPort() != null) {
                proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(config.getProxyHost(), config.getProxyPort()));
            }

            connection = (HttpURLConnection) (proxy == null ? uri.toURL().openConnection() : uri.toURL().openConnection(proxy));
            connection.setRequestMethod(method.name());
            connection.setRequestProperty(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
            connection.setRequestProperty(HttpHeaders.AUTHORIZATION, config.getBasicAuthorizationToken());
            connection.setDoInput(true);

            // Following header is required for easy HTTP request tracing in systems such as DataPower.
            if (uuid != null) {
                connection.setRequestProperty("X-B3-TraceId", uuid);
            }

            if (includeContentTypeHeader) {
                connection.setRequestProperty(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            }

        } catch (MalformedURLException e) {
            throw new RestRequestException( // is a malformed URL
                    400, String.format("%s is a malformed URL", urlAdd));
        } catch (URISyntaxException e) {
            throw new RestRequestException(400, String.format("%s is a malformed URL", urlAdd));
        } catch (IOException e) {
            throw new RestRequestException(400, String.format("Unable to open HTTP connection to %s", urlAdd));
        }
        return connection;
    }

    public ClientResponse sendGet(String uri) {
        String uuid = UUID.randomUUID().toString();
        HttpURLConnection connection = this.getJIRAConnection(uri, false, uuid, HttpMethod.GET);
        ClientResponse response = executeRequest(connection, null);

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
            // when response code is 401.it's response data is html text. it to long to show
            if(response.getStatusCode() == HttpStatus.UNAUTHORIZED.value()) {
            	responseStr = "Authentication failed";
            } else {
                try {
                    responseStr = response.getEntity(String.class);
                } catch (Exception e) {
                    // we don't do anything if we cannot get the response.
                }
            }
            if (!StringUtils.isBlank(responseStr)) {
                errorMessage.append(System.lineSeparator()).append(System.lineSeparator()).append("# Received Response:").append(System.lineSeparator()).append(responseStr);
            }

            throw new RestRequestException(response.getStatusCode(), errorMessage.toString());
        }

    }

    public ClientResponse sendPost(String uri, String jsonPayload, int expectedHttpStatusCode) {
        String uuid = UUID.randomUUID().toString();
        HttpURLConnection connection = this.getJIRAConnection(uri, true, uuid, HttpMethod.POST);
        ClientResponse response = executeRequest(connection, jsonPayload);
        checkResponseStatus(expectedHttpStatusCode, response, uri, "POST", jsonPayload, uuid);

        return response;
    }

    public ClientResponse sendPut(String uri, String jsonPayload, int expectedHttpStatusCode) {
        String uuid = UUID.randomUUID().toString();
        HttpURLConnection connection = this.getJIRAConnection(uri,true, uuid, HttpMethod.PUT);
        ClientResponse response = executeRequest(connection, jsonPayload);

        checkResponseStatus(expectedHttpStatusCode, response, uri, "PUT", jsonPayload, uuid);

        return response;
    }

    private ClientResponse executeRequest(HttpURLConnection connection, String payload) {
        try {
            if (payload != null) {
                connection.setDoOutput(true);
                byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
                connection.getOutputStream().write(payloadBytes);
            }

            int statusCode = connection.getResponseCode();
            String responseBody = readResponseBody(connection);
            return new ClientResponse(statusCode, responseBody);
        } catch (IOException e) {
            throw new RestRequestException(400, "Error while executing HTTP request");
        } finally {
            connection.disconnect();
        }
    }

    private String readResponseBody(HttpURLConnection connection) throws IOException {
        InputStream stream = connection.getErrorStream();
        if (stream == null) {
            stream = connection.getInputStream();
        }
        if (stream == null) {
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
            return body.toString();
        }
    }
}

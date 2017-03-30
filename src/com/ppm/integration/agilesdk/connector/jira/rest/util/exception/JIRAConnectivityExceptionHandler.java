
package com.ppm.integration.agilesdk.connector.jira.rest.util.exception;

import java.lang.Thread.UncaughtExceptionHandler;

import org.apache.wink.client.ClientRuntimeException;

import com.ppm.integration.IntegrationException;
import com.ppm.integration.agilesdk.connector.jira.JIRAIntegrationConnector;

public class JIRAConnectivityExceptionHandler implements UncaughtExceptionHandler {
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        uncaughtException(t, e, JIRAIntegrationConnector.class);
    }

    public void uncaughtException(Thread t, Throwable e, Class cls) {
        if (e instanceof ClientRuntimeException) {
            handleClientRuntimeException((ClientRuntimeException)e, cls);
        } else if (e instanceof RestRequestException) {
            handleClientException((RestRequestException)e, cls);
        } else {
            throw IntegrationException.build(cls).setErrorCode("PPM_INT_JIRA_ERR_202").setMessage("ERROR_UNKNOWN_ERROR",
                    e.getMessage());
        }
    }

    private void handleClientException(RestRequestException e, Class cls) {
        switch (e.getErrorCode()) {
            case "404":
                throw IntegrationException.build(cls).setErrorCode("PPM_INT_JIRA_ERR_" + e.getErrorCode())
                        .setMessage("ERROR_DOMAIN_NOT_FOUND");
            case "502":
                throw IntegrationException.build(cls).setErrorCode("PPM_INT_JIRA_ERR_" + e.getErrorCode())
                        .setMessage("ERROR_BAD_GETWAY");
            case "400":
                throw IntegrationException.build(cls).setErrorCode("PPM_INT_JIRA_ERR_" + e.getErrorCode())
                        .setMessage("ERROR_BAD_REQUEST");
            case "401":
                throw IntegrationException.build(cls).setErrorCode("PPM_INT_JIRA_ERR_" + e.getErrorCode())
                        .setMessage("ERROR_AUTHENTICATION_FAILED");
            default:
                throw IntegrationException.build(cls).setErrorCode("PPM_INT_JIRA_ERR_202")
                        .setMessage("ERROR_CONNECTIVITY_ERROR", e.getMessage());
        }

    }

    private void handleClientRuntimeException(Exception e, Class cls) {
        Throwable t = extractException(e);

        if (t instanceof java.security.cert.CertificateException) {

            IntegrationException.build(cls).setErrorCode("PPM_INT_JIRA_ERR_202")
                    .setMessage("ERROR_CERTIFICATE_EXCEPTION");
        }

        if (t instanceof java.net.UnknownHostException) {
            throw IntegrationException.build(cls).setErrorCode("PPM_INT_JIRA_ERR_202")
                    .setMessage("ERROR_UNKNOWN_HOST_EXCEPTION");
        }

        if (t instanceof java.net.ConnectException) {
            throw IntegrationException.build(cls).setErrorCode("PPM_INT_JIRA_ERR_202")
                    .setMessage("ERROR_CONNECT_EXCEPTION");
        }
        if (t instanceof java.net.NoRouteToHostException) {
            throw IntegrationException.build(cls).setErrorCode("PPM_INT_JIRA_ERR_202")
                    .setMessage("ERROR_NO_ROUTE_EXCEPTION");
        }
        if (t instanceof java.lang.IllegalArgumentException) {

            throw IntegrationException.build(cls).setErrorCode("PPM_INT_JIRA_ERR_202")
                    .setMessage("ERROR_ILLEGAL_ARGUMENT__EXCEPTION");
        }
        throw IntegrationException.build(cls).setErrorCode("PPM_INT_JIRA_ERR_202")
                .setMessage("ERROR_CONNECTIVITY_ERROR", e.getMessage());

    }

    @SuppressWarnings("unchecked")
    protected <T extends Throwable> T extractException(Exception e, Class<T> clazz) {

        Throwable t = e;
        while (!clazz.isInstance(t) && t != null) {
            t = t.getCause();
        }

        return (T)t;
    }

    protected <T extends Throwable> T extractException(Exception e) {

        Throwable t = e;

        while (t.getCause() != null) {
            t = t.getCause();

        }
        return (T)t;
    }

}
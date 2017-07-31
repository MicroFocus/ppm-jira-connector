
package com.ppm.integration.agilesdk.connector.jira.rest.util.exception;

public class RestRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int statusCode;

    public RestRequestException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
   }

    public int getStatusCode() {
        return statusCode;
    }
}

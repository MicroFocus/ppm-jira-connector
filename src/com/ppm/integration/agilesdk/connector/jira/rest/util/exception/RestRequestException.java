
package com.ppm.integration.agilesdk.connector.jira.rest.util.exception;

public class RestRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;

    private final String msgKey;

    private final String[] params;

    public RestRequestException(String code, String msgKey, String... params) {
        this.errorCode = code;
        this.msgKey = msgKey;
        this.params = params;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMsgKey() {
        return msgKey;
    }

    public String[] getParams() {
        return params;
    }

}

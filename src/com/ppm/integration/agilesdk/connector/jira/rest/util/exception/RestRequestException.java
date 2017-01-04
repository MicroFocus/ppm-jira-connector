package com.ppm.integration.agilesdk.connector.jira.rest.util.exception;

public class RestRequestException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public RestRequestException(int statusCode, String msg) {
		super("StatusCode:" + statusCode + ",ErrorMessage:" + msg);
	}

}

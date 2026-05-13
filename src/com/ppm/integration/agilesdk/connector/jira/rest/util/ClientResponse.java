package com.ppm.integration.agilesdk.connector.jira.rest.util;

/**
 * Minimal response wrapper to preserve previous ClientResponse call patterns.
 */
public class ClientResponse {

    private final int statusCode;
    private final String entity;

    public ClientResponse(int statusCode, String entity) {
        this.statusCode = statusCode;
        this.entity = entity;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public <T> T getEntity(Class<T> clazz) {
        if (String.class.equals(clazz)) {
            return clazz.cast(entity);
        }
        throw new IllegalArgumentException("Only String entity extraction is supported");
    }
}


package com.payments.config;

public final class ApiConfig {
    public static final int PORT = 8089;
    public static final String BASE_URL = "http://localhost:" + PORT;
    public static final String PACS008_ENDPOINT = "/payments/pacs008";
    public static final String CONTENT_TYPE_XML = "application/xml";

    private ApiConfig() {
    }
}

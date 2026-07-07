package com.payments.api;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.payments.config.ApiConfig;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class WireMockSetup {
    private final WireMockServer server = new WireMockServer(ApiConfig.PORT);

    public void start() {
        server.start();
        configureFor("localhost", ApiConfig.PORT);
    }
    public void stop() { server.stop(); }

    public void reset() {
        server.resetAll();
        stubFor(post(urlEqualTo(ApiConfig.PACS008_ENDPOINT + "/error"))
            .willReturn(json(500, "{\"status\":\"ERROR\",\"message\":\"Internal server error\"}")));

        stubFor(post(urlEqualTo(ApiConfig.PACS008_ENDPOINT)).atPriority(1)
            .withHeader("Content-Type", containing("application/json"))
            .willReturn(json(415, rejected("PACS008_006", "Content-Type", "Unsupported media type"))));
        rejectScenario("MISSING_MSG_ID", 400, "PACS008_001", "MsgId");
        rejectScenario("INVALID_BIC", 422, "PACS008_002", "BICFI");
        rejectScenario("INVALID_IBAN", 422, "PACS008_004", "IBAN");
        rejectScenario("ZERO_AMOUNT", 422, "PACS008_003", "IntrBkSttlmAmt");
        rejectScenario("NEGATIVE_AMOUNT", 422, "PACS008_003", "IntrBkSttlmAmt");
        rejectScenario("INVALID_CURRENCY", 422, "PACS008_004", "Ccy");
        rejectScenario("AMOUNT_MISMATCH", 422, "PACS008_005", "TtlIntrBkSttlmAmt");
        rejectScenario("MALFORMED_XML", 400, "PACS008_007", "Document");

        stubFor(post(urlEqualTo(ApiConfig.PACS008_ENDPOINT)).atPriority(2)
            .withRequestBody(matching("\\s*"))
            .willReturn(json(400, rejected("PACS008_008", "body", "Request body is empty"))));

        for (String bic : new String[]{"DEUT", "DEUTDEDBB", "DEUTDEDBBERR", "12345678", "DEUT DE DB"}) {
            stubFor(post(urlEqualTo(ApiConfig.PACS008_ENDPOINT)).atPriority(3)
                .withRequestBody(containing("<BICFI>" + bic + "</BICFI>"))
                .willReturn(json(422, rejected("PACS008_002", "BICFI", "Invalid BIC"))));
        }

        stubFor(post(urlEqualTo(ApiConfig.PACS008_ENDPOINT)).atPriority(10)
            .willReturn(json(202, "{\"status\":\"ACCEPTED\",\"msgId\":\"MSG20251231001\","
                + "\"uetr\":\"550e8400-e29b-41d4-a716-446655440000\",\"settlementDate\":\"2026-07-06\"}")));
    }

    private void rejectScenario(String scenario, int status, String code, String field) {
        stubFor(post(urlEqualTo(ApiConfig.PACS008_ENDPOINT)).atPriority(2)
            .withRequestBody(containing("<TestScenario>" + scenario + "</TestScenario>"))
            .willReturn(json(status, rejected(code, field, "Payment validation failed"))));
    }

    private static String rejected(String code, String field, String message) {
        return "{\"status\":\"REJECTED\",\"errorCode\":\"" + code
            + "\",\"field\":\"" + field + "\",\"message\":\"" + message + "\"}";
    }

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder json(int status, String body) {
        return aResponse().withStatus(status).withHeader("Content-Type", "application/json").withBody(body);
    }
}

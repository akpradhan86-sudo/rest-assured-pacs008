package com.payments.api;

import com.payments.config.ApiConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

/**
 * Pacs008Client — HTTP client for the pacs.008 payment endpoint.
 *
 * Why this class exists:
 *   Without it, every test class calls given().spec(requestSpec).body(payload)
 *   .when().post("/payments/pacs008") directly. If the base URL changes, the
 *   auth header is added, or the endpoint path shifts, you edit every test.
 *
 *   With this client, tests just call pacs008Client.submit(payload) —
 *   they describe WHAT to test, not HOW to send an HTTP request.
 *
 * This is the API-layer equivalent of SwiftPaymentPage.java in your
 * Selenium POM: one class owns all transport details, tests stay clean.
 *
 * Analogy to Selenium POM:
 *   SwiftPaymentPage.enterSenderBic()  →  Pacs008Client.submit()
 *   SwiftPaymentPage.clickSubmit()     →  Pacs008Client.submitAndExpect()
 *   SwiftPaymentPage.isSuccessMsg()    →  Pacs008Client.extract()
 */
public class Pacs008Client {

    private final RequestSpecification requestSpec;

    public Pacs008Client(RequestSpecification requestSpec) {
        this.requestSpec = requestSpec;
    }

    // ── Core HTTP methods ─────────────────────────────────────────────────

    /**
     * POST a pacs.008 XML payload to the payment endpoint.
     * Returns the raw Response so the test can assert anything on it.
     */
    public Response submit(String xmlPayload) {
        return given()
            .spec(requestSpec)
            .body(xmlPayload)
        .when()
            .post(ApiConfig.PACS008_ENDPOINT)
        .then()
            .extract().response();
    }

    /**
     * Submit and assert the expected HTTP status code inline.
     * Use this when you only care about status — no body assertions.
     *
     * Example:
     *   pacs008Client.submitAndExpectStatus(payload, 202);
     */
    public Response submitAndExpectStatus(String xmlPayload, int expectedStatus) {
        return given()
            .spec(requestSpec)
            .body(xmlPayload)
        .when()
            .post(ApiConfig.PACS008_ENDPOINT)
        .then()
            .statusCode(expectedStatus)
            .extract().response();
    }

    /**
     * Submit to a custom path — used for system failure tests
     * that target error-simulation endpoints.
     */
    public Response submitTo(String path, String xmlPayload) {
        return given()
            .spec(requestSpec)
            .body(xmlPayload)
        .when()
            .post(path)
        .then()
            .extract().response();
    }

    /**
     * Submit with a custom Content-Type header.
     * Useful for testing how the API handles wrong content types.
     */
    public Response submitWithContentType(String xmlPayload, String contentType) {
        return given()
            .spec(requestSpec)
            .contentType(contentType)
            .body(xmlPayload)
        .when()
            .post(ApiConfig.PACS008_ENDPOINT)
        .then()
            .extract().response();
    }

    // ── Response extraction helpers ───────────────────────────────────────

    /** Extract a JSON field value from the response body. */
    public String extractField(Response response, String jsonPath) {
        return response.jsonPath().getString(jsonPath);
    }

    /** Extract the HTTP status code. */
    public int extractStatus(Response response) {
        return response.getStatusCode();
    }

    /** Extract the full response body as a string. */
    public String extractBody(Response response) {
        return response.getBody().asString();
    }
}

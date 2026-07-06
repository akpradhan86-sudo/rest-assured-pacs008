package com.payments.tests;

import com.payments.api.Pacs008Client;
import com.payments.api.WireMockSetup;
import com.payments.config.ApiConfig;
import com.payments.validation.Pacs008ResponseAssertions;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

/**
 * BaseApiTest — REST Assured + WireMock infrastructure for all pacs.008 tests.
 *
 * Phase 1 changes from the prototype:
 *   - Exposes `pacs008Client` (not just requestSpec) — tests use the client,
 *     never call given()...post() directly
 *   - Exposes `assertions` — tests use the assertion helpers, never inline
 *     .body("status", equalTo("ACCEPTED")) in every test method
 *
 * Layer responsibilities:
 *   BaseApiTest          → infrastructure (WireMock, REST Assured config)
 *   Pacs008Client        → transport (how to send an HTTP request)
 *   Pacs008ResponseAssertions → assertions (what to check in the response)
 *   Test classes         → intent (what scenario is being tested)
 */
public class BaseApiTest {

    protected static WireMockSetup wireMock;
    protected Pacs008Client pacs008Client;
    protected Pacs008ResponseAssertions assertions;

    @BeforeSuite
    public void startWireMock() {
        wireMock = new WireMockSetup();
        wireMock.start();
    }

    @AfterSuite
    public void stopWireMock() {
        if (wireMock != null) wireMock.stop();
    }

    @BeforeMethod
    public void setUp() {
        // Reset all WireMock stubs — clean state before every test
        wireMock.reset();

        // Build the REST Assured request spec once
        RequestSpecification requestSpec = new RequestSpecBuilder()
            .setBaseUri(ApiConfig.BASE_URL)
            .setContentType(ApiConfig.CONTENT_TYPE_XML)
            .setRelaxedHTTPSValidation()
            .build();

        // Wire the client and assertions — injected into every test
        pacs008Client = new Pacs008Client(requestSpec);
        assertions    = new Pacs008ResponseAssertions();

        // Log request/response detail only on failure
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }
}

package com.payments.tests;

import com.payments.data.Pacs008PayloadBuilder;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Pacs008SystemFailureTest — infrastructure and 5xx resilience scenarios.
 *
 * Why separated from NegativeTest?
 *   NegativeTest covers BUSINESS validation failures (wrong BIC format,
 *   missing field, invalid amount) — the API understood the request but
 *   rejected it per business rules.
 *
 *   This class covers INFRASTRUCTURE failures (server errors, timeouts,
 *   service unavailability) — the API couldn't process the request at all.
 *
 *   Keeping them separate means when the SIT environment is unstable
 *   you can skip this class alone with:
 *     mvn test -Dgroups="business" (skips system group)
 *   ...without touching business validation tests.
 *
 * TC-SYS-001  Server returns 500 — client handles without throwing
 * TC-SYS-002  Response body is present even on server error
 * TC-SYS-003  Multiple rapid submissions — no state leakage between calls
 */
public class Pacs008SystemFailureTest extends BaseApiTest {

    // ── TC-SYS-001: Server error — client handles gracefully ─────────────

    @Test(
        description = "TC-SYS-001: API should return 500 on server error",
        groups      = "system"
    )
    public void testServerErrorReturns500() {
        // Submit to the error-simulation path (stubbed to return 500)
        Response response = pacs008Client.submitTo(
            "/payments/pacs008/error",
            Pacs008PayloadBuilder.validPacs008()
        );

        Assert.assertEquals(response.getStatusCode(), 500,
            "Server error endpoint should return 500");
        assertions.assertFieldPresent(response, "message");
        assertions.assertContentTypeIsJson(response);
    }

    // ── TC-SYS-002: Error response body is structured ────────────────────

    @Test(
        description = "TC-SYS-002: 500 response should have status:ERROR and a message",
        groups      = "system"
    )
    public void testServerErrorResponseIsStructured() {
        Response response = pacs008Client.submitTo(
            "/payments/pacs008/error",
            Pacs008PayloadBuilder.validPacs008()
        );

        Assert.assertEquals(response.getStatusCode(), 500);
        assertions.assertField(response, "status", "ERROR");
        assertions.assertFieldPresent(response, "message");
    }

    // ── TC-SYS-003: Multiple rapid submissions — no state leakage ─────────

    @Test(
        description = "TC-SYS-003: Repeated submissions should all return 202 independently",
        groups      = "system"
    )
    public void testRepeatedSubmissionsAreStateless() {
        // Submit the same payload 3 times in rapid succession
        // Each should return 202 independently — no shared state
        for (int i = 1; i <= 3; i++) {
            String payload = Pacs008PayloadBuilder.validPacs008(
                "MSG2025REP" + String.format("%03d", i),
                "E2E2025REP" + String.format("%03d", i),
                "USD", "1000.00",
                "DEUTDEDBXXX", "HSBCGB2LXXX"
            );

            Response response = pacs008Client.submit(payload);

            assertions.assertAccepted(response);
        }
    }
}

package com.payments.tests;

import com.payments.data.Pacs008PayloadBuilder;
import io.restassured.response.Response;
import org.testng.annotations.Test;

/**
 * Pacs008HappyPathTest — valid pacs.008 payment scenarios.
 *
 * Phase 1 refactor — notice what's gone:
 *   ✗ given().spec(requestSpec).body(payload).when().post(...)
 *   ✗ .body("status", equalTo("ACCEPTED")) scattered inline
 *   ✗ .body("uetr", matchesPattern("...")) copy-pasted per test
 *
 * And what replaced it:
 *   ✓ pacs008Client.submit(payload)     — transport hidden
 *   ✓ assertions.assertAccepted(resp)   — assertion logic centralised
 *   ✓ assertions.assertUetrValid(resp)  — UETR rule in one place
 *
 * Test intent is now immediately clear — each test reads like a spec.
 */
public class Pacs008HappyPathTest extends BaseApiTest {

    // ── TC-HP-001: Standard USD cross-border credit transfer ─────────────

    @Test(description = "TC-HP-001: Valid pacs.008 USD payment should be accepted")
    public void testValidUsdPaymentAccepted() {
        Response response = pacs008Client.submit(
            Pacs008PayloadBuilder.validPacs008()
        );

        assertions.assertAccepted(response);
        assertions.assertContentTypeIsJson(response);
    }

    // ── TC-HP-002: EUR transfer with different BIC pair ──────────────────

    @Test(description = "TC-HP-002: Valid pacs.008 EUR payment should be accepted")
    public void testValidEurPaymentAccepted() {
        String payload = Pacs008PayloadBuilder.validPacs008(
            "MSG20251231EUR", "E2E20251231EUR",
            "EUR", "5000.00",
            "BNPAFRPPXXX", "CITIUS33XXX"
        );

        Response response = pacs008Client.submit(payload);

        assertions.assertAccepted(response);
    }

    // ── TC-HP-003: UETR present and valid UUID v4 ────────────────────────

    @Test(description = "TC-HP-003: Accepted response should contain a valid UETR (gpi)")
    public void testResponseContainsValidUetr() {
        Response response = pacs008Client.submit(
            Pacs008PayloadBuilder.validPacs008()
        );

        assertions.assertAccepted(response);
        assertions.assertUetrValid(response);
    }

    // ── TC-HP-004: Settlement date in ISO 8601 format ────────────────────

    @Test(description = "TC-HP-004: Accepted response should include valid settlement date")
    public void testResponseContainsSettlementDate() {
        Response response = pacs008Client.submit(
            Pacs008PayloadBuilder.validPacs008()
        );

        assertions.assertAccepted(response);
        assertions.assertSettlementDateValid(response);
    }

    // ── TC-HP-005: Large amount near boundary ────────────────────────────

    @Test(description = "TC-HP-005: Large payment amount should be accepted")
    public void testLargeAmountAccepted() {
        String payload = Pacs008PayloadBuilder.validPacs008(
            "MSG20251231LRG", "E2E20251231LRG",
            "USD", "999999999.99",
            "DEUTDEDBXXX", "HSBCGB2LXXX"
        );

        Response response = pacs008Client.submit(payload);

        assertions.assertAccepted(response);
    }

    // ── TC-HP-006: Minimum valid amount ──────────────────────────────────

    @Test(description = "TC-HP-006: Minimum payment amount (0.01) should be accepted")
    public void testMinimumAmountAccepted() {
        String payload = Pacs008PayloadBuilder.validPacs008(
            "MSG20251231MIN", "E2E20251231MIN",
            "USD", "0.01",
            "DEUTDEDBXXX", "HSBCGB2LXXX"
        );

        Response response = pacs008Client.submit(payload);

        assertions.assertAccepted(response);
    }

    // ── TC-HP-007: Response time within 3-second SLA ────────────────────

    @Test(description = "TC-HP-007: API response time should be within 3-second SLA")
    public void testResponseTimeWithinSla() {
        Response response = pacs008Client.submit(
            Pacs008PayloadBuilder.validPacs008()
        );

        assertions.assertAccepted(response);
        assertions.assertResponseTimeWithinSla(response, 3_000L);
    }

    // ── TC-HP-008: Status field is valid enum ────────────────────────────

    @Test(description = "TC-HP-008: Response status should be a valid payment status enum")
    public void testStatusFieldIsValidEnum() {
        Response response = pacs008Client.submit(
            Pacs008PayloadBuilder.validPacs008()
        );

        assertions.assertStatusIsValidEnum(response);
    }

    // ── TC-HP-009: GBP transfer — India to Germany ───────────────────────

    @Test(description = "TC-HP-009: GBP remittance via Indian bank should be accepted")
    public void testGbpRemittanceAccepted() {
        String payload = Pacs008PayloadBuilder.validPacs008(
            "MSG20251231GBP", "E2E20251231GBP",
            "GBP", "250.00",
            "ICICIINBBXXX", "DEUTDEDBXXX"
        );

        Response response = pacs008Client.submit(payload);

        assertions.assertAccepted(response);
    }
}

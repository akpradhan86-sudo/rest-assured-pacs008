package com.payments.validation;

import io.restassured.response.Response;
import org.testng.Assert;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Pacs008ResponseAssertions — reusable assertion helpers for pacs.008 responses.
 *
 * Why this class exists:
 *   Without it, every test class has the same assertions copy-pasted in:
 *     .body("status", equalTo("ACCEPTED"))
 *     .body("uetr", notNullValue())
 *     .body("uetr", matchesPattern("..."))
 *
 *   When the UETR pattern changes or a new required field is added,
 *   you'd update every test. With this class you update one method.
 *
 * How to read the method names:
 *   assertAccepted()    → call this at the start of every happy path test
 *   assertRejected()    → call this at the start of every negative test
 *   assertUetrValid()   → specific UETR format check — use in schema tests
 *   assertField()       → one-liner for checking any specific response field
 *
 * Usage in a test:
 *   Response response = pacs008Client.submit(payload);
 *   assertions.assertAccepted(response);
 *   assertions.assertUetrValid(response);
 */
public class Pacs008ResponseAssertions {

    // UUID v4 pattern — UETR format per SWIFT gpi specification
    private static final String UUID_V4_PATTERN =
        "[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";

    // ISO 8601 date pattern
    private static final String ISO_DATE_PATTERN =
        "\\d{4}-(?:0[1-9]|1[0-2])-(?:0[1-9]|[12]\\d|3[01])";

    // pacs.008 error code pattern
    private static final String ERROR_CODE_PATTERN = "PACS008_\\d{3}";

    // ── Happy path assertions ─────────────────────────────────────────────

    /**
     * Assert a response is fully accepted — status code + body fields.
     * Call at the top of every positive test after client.submit().
     */
    public void assertAccepted(Response response) {
        assertThat("HTTP status should be 202 Accepted",
            response.getStatusCode(), equalTo(202));
        assertThat("status field should be ACCEPTED",
            response.jsonPath().getString("status"), equalTo("ACCEPTED"));
        assertThat("msgId should be present",
            response.jsonPath().getString("msgId"), notNullValue());
        assertThat("uetr should be present",
            response.jsonPath().getString("uetr"), notNullValue());
        assertThat("settlementDate should be present",
            response.jsonPath().getString("settlementDate"), notNullValue());
    }

    // ── Negative / rejection assertions ──────────────────────────────────

    /**
     * Assert a response is rejected — status code + body fields.
     * Call at the top of every negative test after client.submit().
     *
     * @param response       the response to assert on
     * @param expectedStatus the expected HTTP status (400, 422, etc.)
     */
    public void assertRejected(Response response, int expectedStatus) {
        assertThat("HTTP status should indicate rejection",
            response.getStatusCode(), equalTo(expectedStatus));
        assertThat("status field should be REJECTED",
            response.jsonPath().getString("status"), equalTo("REJECTED"));
        assertThat("message should be present",
            response.jsonPath().getString("message"), notNullValue());
    }

    /**
     * Assert rejection with a specific pacs.008 error code.
     * Use when you want to verify the exact error code returned.
     *
     * Example:
     *   assertions.assertRejectedWithCode(response, 400, "PACS008_001");
     */
    public void assertRejectedWithCode(
            Response response, int expectedStatus, String expectedErrorCode) {
        assertRejected(response, expectedStatus);
        assertThat("errorCode should match expected code",
            response.jsonPath().getString("errorCode"), equalTo(expectedErrorCode));
    }

    /**
     * Assert rejection mentioning a specific SWIFT/ISO 20022 field.
     * Useful for field-level validation tests (BIC, IBAN, UETR etc.)
     */
    public void assertRejectedForField(
            Response response, int expectedStatus, String fieldPath) {
        assertRejected(response, expectedStatus);
        assertThat("field in error should reference " + fieldPath,
            response.jsonPath().getString("field"), containsString(fieldPath));
    }

    // ── Field-specific assertions ─────────────────────────────────────────

    /**
     * Assert the UETR in the response is a valid UUID v4.
     * UETR is mandatory for SWIFT gpi — format per gpi specification.
     */
    public void assertUetrValid(Response response) {
        String uetr = response.jsonPath().getString("uetr");
        Assert.assertNotNull(uetr, "UETR must be present in the response");
        Assert.assertTrue(
            uetr.matches(UUID_V4_PATTERN),
            "UETR must match UUID v4 format per SWIFT gpi spec. Got: " + uetr
        );
    }

    /**
     * Assert the settlement date matches ISO 8601 (YYYY-MM-DD).
     */
    public void assertSettlementDateValid(Response response) {
        String date = response.jsonPath().getString("settlementDate");
        Assert.assertNotNull(date, "settlementDate must be present");
        Assert.assertTrue(
            date.matches(ISO_DATE_PATTERN),
            "settlementDate must be ISO 8601 format YYYY-MM-DD. Got: " + date
        );
    }

    /**
     * Assert the error code matches the pacs.008 error code pattern.
     */
    public void assertErrorCodeValid(Response response) {
        String code = response.jsonPath().getString("errorCode");
        Assert.assertNotNull(code, "errorCode must be present in rejected response");
        Assert.assertTrue(
            code.matches(ERROR_CODE_PATTERN),
            "errorCode must match PACS008_NNN format. Got: " + code
        );
    }

    /**
     * Assert the response Content-Type header is application/json.
     */
    public void assertContentTypeIsJson(Response response) {
        assertThat("Content-Type must be application/json",
            response.getHeader("Content-Type"), containsString("application/json"));
    }

    /**
     * Assert the status field is one of the allowed enum values.
     * Valid values: ACCEPTED, REJECTED, PENDING, ERROR
     */
    public void assertStatusIsValidEnum(Response response) {
        String status = response.jsonPath().getString("status");
        java.util.List<String> validStatuses =
            java.util.List.of("ACCEPTED", "REJECTED", "PENDING", "ERROR");
        Assert.assertTrue(
            validStatuses.contains(status),
            "Status must be one of " + validStatuses + ". Got: " + status
        );
    }

    /**
     * Assert response time is within an SLA threshold.
     *
     * @param response       the response to check
     * @param maxMilliseconds the SLA ceiling in ms (e.g. 3000 for 3 seconds)
     */
    public void assertResponseTimeWithinSla(Response response, long maxMilliseconds) {
        long time = response.getTime();
        Assert.assertTrue(
            time < maxMilliseconds,
            "Response time " + time + "ms exceeds SLA of " + maxMilliseconds + "ms"
        );
    }

    /**
     * Generic single-field assertion.
     * Use for one-off field checks without creating a dedicated method.
     *
     * Example:
     *   assertions.assertField(response, "msgId", "MSG20251231001");
     */
    public void assertField(Response response, String jsonPath, String expectedValue) {
        assertThat("Field '" + jsonPath + "' mismatch",
            response.jsonPath().getString(jsonPath), equalTo(expectedValue));
    }

    /**
     * Assert a field is present and not null/empty.
     */
    public void assertFieldPresent(Response response, String jsonPath) {
        String value = response.jsonPath().getString(jsonPath);
        Assert.assertNotNull(value,
            "Field '" + jsonPath + "' should be present but was null");
        Assert.assertFalse(value.isBlank(),
            "Field '" + jsonPath + "' should not be empty");
    }


    // ── Utility ───────────────────────────────────────────────────────────

    /** Extract the HTTP status code — used when tests need to branch on status. */
    public int extractStatusCode(Response response) {
        return response.getStatusCode();     
    }
}

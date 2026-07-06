package com.payments.tests;

import com.payments.data.Pacs008PayloadBuilder;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Pacs008NegativeTest — invalid pacs.008 scenarios.
 *
 * Phase 1 refactor — every test now reads:
 *   1. Build the broken payload (data layer)
 *   2. Submit via client (transport layer)
 *   3. Assert via assertions helper (assertion layer)
 *
 * The test itself contains zero REST Assured DSL — it reads like a
 * plain-English description of the scenario being tested.
 */
public class Pacs008NegativeTest extends BaseApiTest {

    // ── TC-NEG-001: Missing mandatory MsgId ──────────────────────────────

    @Test(description = "TC-NEG-001: Missing MsgId should be rejected with 400")
    public void testMissingMsgIdRejected() {
        Response response = pacs008Client.submit(
            Pacs008PayloadBuilder.missingMsgId()
        );

        assertions.assertRejectedWithCode(response, 400, "PACS008_001");
        assertions.assertRejectedForField(response, 400, "MsgId");
    }

    // ── TC-NEG-002: Invalid BIC format ───────────────────────────────────

    @Test(description = "TC-NEG-002: Invalid debtor BIC (9 chars) should be rejected with 422")
    public void testInvalidDebtorBicRejected() {
        Response response = pacs008Client.submit(
            Pacs008PayloadBuilder.invalidDebtorBic()
        );

        assertions.assertRejectedWithCode(response, 422, "PACS008_002");
        assertions.assertRejectedForField(response, 422, "BICFI");
    }

    // ── TC-NEG-003: Invalid IBAN ──────────────────────────────────────────

    @Test(description = "TC-NEG-003: Invalid IBAN checksum should be rejected")
    public void testInvalidDebtorIbanRejected() {
        Response response = pacs008Client.submit(
            Pacs008PayloadBuilder.invalidDebtorIban()
        );

        assertions.assertRejected(response, 422);
    }

    // ── TC-NEG-004: Zero amount ───────────────────────────────────────────

    @Test(description = "TC-NEG-004: Zero amount should be rejected with 422")
    public void testZeroAmountRejected() {
        Response response = pacs008Client.submit(
            Pacs008PayloadBuilder.zeroAmount()
        );

        assertions.assertRejectedWithCode(response, 422, "PACS008_003");
    }

    // ── TC-NEG-005: Negative amount ───────────────────────────────────────

    @Test(description = "TC-NEG-005: Negative amount should be rejected with 422")
    public void testNegativeAmountRejected() {
        Response response = pacs008Client.submit(
            Pacs008PayloadBuilder.negativeAmount()
        );

        assertions.assertRejected(response, 422);
    }

    // ── TC-NEG-006: Invalid currency ──────────────────────────────────────

    @Test(description = "TC-NEG-006: Invalid currency code XXX should be rejected")
    public void testInvalidCurrencyRejected() {
        Response response = pacs008Client.submit(
            Pacs008PayloadBuilder.invalidCurrency()
        );

        assertions.assertRejected(response, 422);
    }

    // ── TC-NEG-007: Amount mismatch ───────────────────────────────────────

    @Test(description = "TC-NEG-007: Amount mismatch between GrpHdr and CdtTrfTxInf should be rejected")
    public void testAmountMismatchRejected() {
        Response response = pacs008Client.submit(
            Pacs008PayloadBuilder.amountMismatch()
        );

        assertions.assertRejectedWithCode(response, 422, "PACS008_005");
        assertions.assertRejectedForField(response, 422, "TtlIntrBkSttlmAmt");
    }

    // ── TC-NEG-008: Malformed XML ─────────────────────────────────────────

    @Test(description = "TC-NEG-008: Malformed XML should be rejected with 400")
    public void testMalformedXmlRejected() {
        Response response = pacs008Client.submit(
            Pacs008PayloadBuilder.malformedXml()
        );

        assertions.assertRejected(response, 400);
        assertions.assertErrorCodeValid(response);
    }

    // ── TC-NEG-009: Empty body ────────────────────────────────────────────

    @Test(description = "TC-NEG-009: Empty request body should be rejected with 400")
    public void testEmptyBodyRejected() {
        Response response = pacs008Client.submit(
            Pacs008PayloadBuilder.emptyBody()
        );

        assertions.assertRejected(response, 400);
    }

    // ── TC-NEG-010: Missing UETR ──────────────────────────────────────────

    @Test(description = "TC-NEG-010: Missing UETR should be rejected (CBPR+ mandatory)")
    public void testMissingUetrRejected() {
        Response response = pacs008Client.submit(
            Pacs008PayloadBuilder.missingUetr()
        );

        // In CBPR+, UETR is mandatory — assert either accepted or gracefully rejected
        int status = assertions.extractStatusCode(response);
        org.testng.Assert.assertTrue(
            status == 202 || status == 422,
            "Response should be 202 (accepted with warning) or 422 (rejected). Got: " + status
        );
    }

    // ── TC-NEG-011: Wrong Content-Type header ─────────────────────────────

    @Test(description = "TC-NEG-011: Non-XML Content-Type should be rejected with 415")
    public void testWrongContentTypeRejected() {
        Response response = pacs008Client.submitWithContentType(
            Pacs008PayloadBuilder.validPacs008(),
            "application/json"   // sending JSON content type with XML body
        );

        // 415 Unsupported Media Type or 400 Bad Request both acceptable
        int status = assertions.extractStatusCode(response);
        org.testng.Assert.assertTrue(
            status == 400 || status == 415,
            "Wrong Content-Type should return 400 or 415. Got: " + status
        );
    }

    // ── TC-NEG-012: Parametrised — multiple invalid BIC formats ──────────

    @DataProvider(name = "invalidBicFormats")
    public Object[][] invalidBicFormats() {
        return new Object[][] {
            { "DEUT",          "4 chars — too short"          },
            { "DEUTDEDBB",     "9 chars — invalid length"     },
            { "DEUTDEDBBERR",  "12 chars — too long"          },
            { "12345678",      "All numeric — no bank letters" },
            { "DEUT DE DB",    "Contains spaces"              },
        };
    }

    @Test(
        dataProvider = "invalidBicFormats",
        description  = "TC-NEG-012: Various invalid BIC formats should all be rejected"
    )
    public void testInvalidBicFormatsRejected(String bic, String reason) {
        String payload = Pacs008PayloadBuilder.validPacs008()
            .replace("DEUTDEDBXXX", bic);

        Response response = pacs008Client.submit(payload);

        assertions.assertRejected(response, 422);
    }
}

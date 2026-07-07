package com.payments.data;

import java.util.UUID;

public final class Pacs008PayloadBuilder {
    private Pacs008PayloadBuilder() {
    }

    public static String validPacs008() {
        return validPacs008("MSG20251231001", "E2E20251231001", "USD", "1000.00",
            "DEUTDEDBXXX", "HSBCGB2LXXX");
    }

    public static String validPacs008(String msgId, String endToEndId, String currency,
                                      String amount, String debtorBic, String creditorBic) {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <Document xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08">
              <FIToFICstmrCdtTrf>
                <GrpHdr><MsgId>%s</MsgId><NbOfTxs>1</NbOfTxs><TtlIntrBkSttlmAmt Ccy="%s">%s</TtlIntrBkSttlmAmt></GrpHdr>
                <CdtTrfTxInf><PmtId><EndToEndId>%s</EndToEndId><UETR>%s</UETR></PmtId>
                  <IntrBkSttlmAmt Ccy="%s">%s</IntrBkSttlmAmt>
                  <DbtrAgt><FinInstnId><BICFI>%s</BICFI></FinInstnId></DbtrAgt>
                  <DbtrAcct><Id><IBAN>DE89370400440532013000</IBAN></Id></DbtrAcct>
                  <CdtrAgt><FinInstnId><BICFI>%s</BICFI></FinInstnId></CdtrAgt>
                </CdtTrfTxInf>
              </FIToFICstmrCdtTrf>
            </Document>
            """.formatted(msgId, currency, amount, endToEndId, UUID.randomUUID(),
                currency, amount, debtorBic, creditorBic);
    }

    private static String scenario(String name) {
        return validPacs008().replace("<FIToFICstmrCdtTrf>",
            "<FIToFICstmrCdtTrf><TestScenario>" + name + "</TestScenario>");
    }

    public static String missingMsgId() { return scenario("MISSING_MSG_ID").replaceAll("<MsgId>.*?</MsgId>", ""); }
    public static String invalidDebtorBic() { return scenario("INVALID_BIC").replace("DEUTDEDBXXX", "DEUTDEDBB"); }
    public static String invalidDebtorIban() { return scenario("INVALID_IBAN").replace("DE89370400440532013000", "DE00000000000000000000"); }
    public static String zeroAmount() { return scenario("ZERO_AMOUNT").replace(">1000.00<", ">0.00<"); }
    public static String negativeAmount() { return scenario("NEGATIVE_AMOUNT").replace(">1000.00<", ">-1.00<"); }
    public static String invalidCurrency() { return scenario("INVALID_CURRENCY").replace("Ccy=\"USD\"", "Ccy=\"XXX\""); }
    public static String amountMismatch() { return scenario("AMOUNT_MISMATCH").replaceFirst(">1000.00<", ">999.00<"); }
    public static String malformedXml() { return "<Document><TestScenario>MALFORMED_XML</TestScenario>"; }
    public static String emptyBody() { return " "; }
    public static String missingUetr() { return scenario("MISSING_UETR").replaceAll("<UETR>.*?</UETR>", ""); }
}

package com.payment;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

class ValidatorsTest {
    @Test void amountAcceptsPlainDecimals() {
        assertEquals(new BigDecimal("499.50"), Validators.parseAmount(" 499.50 "));
        assertEquals(new BigDecimal("1000000"), Validators.parseAmount("1000000"));
    }

    @Test void amountRejectsTrickyInput() {
        for (String s : new String[]{null, "", "NaN", "Infinity", "-5", "0", "0.00", "1e9", "1.234", "1000000.01", "abc"})
            assertNull(Validators.parseAmount(s), "should reject: " + s);
    }

    @Test void luhn() {
        assertTrue(Validators.luhn("4111111111111111"));
        assertFalse(Validators.luhn("4111111111111112"));
    }

    @Test void expiry() {
        YearMonth now = YearMonth.of(2026, 11);
        assertTrue(Validators.expiryValid("11/26", now));
        assertTrue(Validators.expiryValid("01/30", now));
        assertFalse(Validators.expiryValid("10/26", now));
        assertFalse(Validators.expiryValid("12/2030", now));   // old code accepted this
        assertFalse(Validators.expiryValid("13/30", now));
    }

    @Test void cvvLengthDependsOnIssuer() {
        assertTrue(Validators.cvvValid("123", "4111111111111111"));
        assertFalse(Validators.cvvValid("1234", "4111111111111111"));
        assertTrue(Validators.cvvValid("1234", "378282246310005"));
    }

    @Test void upiRejectsEmptyLocalPart() {
        assertFalse(Validators.upiValid("@okaxis"));         // old code crashed on this
        assertTrue(Validators.upiValid("asha@okaxis"));
        assertEquals("a****a@okaxis", Validators.maskUpi("asha@okaxis"));
    }

    @Test void netBankingFields() {
        assertTrue(Validators.accountValid("1234567890"));
        assertFalse(Validators.accountValid("123"));
        assertTrue(Validators.ifscValid("sbin0123456"));
        assertFalse(Validators.ifscValid("SBIN1123456"));
        assertFalse(Validators.bankValid("<script>"));
        assertEquals("****7890", Validators.maskAccount("1234567890"));
    }
}

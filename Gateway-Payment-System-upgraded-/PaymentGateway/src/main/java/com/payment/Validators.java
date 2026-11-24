package com.payment;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure validation + masking helpers. No servlet or DB code, so they are easy to unit test. */
public final class Validators {
    private Validators() {}

    public static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000");
    public static final Set<String> BANKS = Set.of("SBI", "HDFC", "ICICI", "Axis");

    private static final Pattern AMOUNT  = Pattern.compile("\\d{1,7}(\\.\\d{1,2})?");
    private static final Pattern EXPIRY  = Pattern.compile("(0[1-9]|1[0-2])/(\\d{2})");
    private static final Pattern UPI     = Pattern.compile("[A-Za-z0-9._-]{2,64}@[A-Za-z]{2,32}");
    private static final Pattern ACCOUNT = Pattern.compile("\\d{10,16}");
    private static final Pattern IFSC    = Pattern.compile("[A-Z]{4}0[A-Z0-9]{6}");
    private static final Pattern PAN     = Pattern.compile("\\d{12,19}");

    /** Plain decimal only (rejects NaN, Infinity, 1e9, negatives). Returns null if invalid. */
    public static BigDecimal parseAmount(String s) {
        if (s == null) return null;
        s = s.trim();
        if (!AMOUNT.matcher(s).matches()) return null;
        BigDecimal a = new BigDecimal(s);
        if (a.signum() <= 0 || a.compareTo(MAX_AMOUNT) > 0) return null;
        return a;
    }

    public static String digitsOnly(String s) {
        return s == null ? "" : s.replaceAll("[\\s-]", "");
    }

    public static boolean panFormat(String pan) {
        return pan != null && PAN.matcher(pan).matches();
    }

    public static boolean luhn(String number) {
        int sum = 0;
        boolean alt = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int n = Character.digit(number.charAt(i), 10);
            if (n < 0) return false;
            if (alt) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alt = !alt;
        }
        return sum % 10 == 0;
    }

    /** Strict MM/YY. The card is valid through the last day of its expiry month. */
    public static boolean expiryValid(String expiry, YearMonth now) {
        if (expiry == null) return false;
        Matcher m = EXPIRY.matcher(expiry.trim());
        if (!m.matches()) return false;
        int month = Integer.parseInt(m.group(1));
        int year = 2000 + Integer.parseInt(m.group(2));
        return !YearMonth.of(year, month).isBefore(now);
    }

    /** American Express (34/37) uses a 4-digit CID; everything else uses 3 digits. */
    public static boolean cvvValid(String cvv, String pan) {
        if (cvv == null) return false;
        boolean amex = pan != null && (pan.startsWith("34") || pan.startsWith("37"));
        return cvv.matches(amex ? "\\d{4}" : "\\d{3}");
    }

    public static boolean upiValid(String upi)      { return upi != null && UPI.matcher(upi.trim()).matches(); }
    public static boolean accountValid(String acc)  { return acc != null && ACCOUNT.matcher(acc.trim()).matches(); }
    public static boolean ifscValid(String ifsc)    { return ifsc != null && IFSC.matcher(ifsc.trim().toUpperCase()).matches(); }
    public static boolean bankValid(String bank)    { return bank != null && BANKS.contains(bank); }

    public static String maskCard(String pan) {
        return "**** **** **** " + pan.substring(pan.length() - 4);
    }

    /** Safe only after upiValid(): the local part is guaranteed to have 2+ chars. */
    public static String maskUpi(String upi) {
        int at = upi.indexOf('@');
        String local = upi.substring(0, at);
        return local.charAt(0) + "****" + local.charAt(local.length() - 1) + upi.substring(at);
    }

    /** Safe only after accountValid(). */
    public static String maskAccount(String acc) {
        return "****" + acc.substring(acc.length() - 4);
    }
}

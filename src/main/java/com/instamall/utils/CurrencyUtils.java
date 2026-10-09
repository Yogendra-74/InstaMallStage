package com.instamall.utils;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CurrencyUtils {

    /**
     * First money-looking token in a string: "1,234.56" out of
     * "Checkout • $1,234.56".
     */
    private static final Pattern AMOUNT =
            Pattern.compile("([0-9][0-9,]*(?:\\.[0-9]{1,2})?)");

    private CurrencyUtils() {

    }

    /**
     * Reads a displayed amount into BigDecimal. Money is never parsed as
     * double — floating point makes totals compare unequal by a cent.
     *
     * Accepts the shapes the app actually renders: "$750.00",
     * "Checkout • $750.00", "Pay $962.49", "$1,234.56".
     */
    public static BigDecimal parse(String text) {

        if (text == null || text.isBlank()) {

            throw new IllegalArgumentException(
                    "Cannot read an amount from empty screen text");
        }

        Matcher matcher = AMOUNT.matcher(text);

        if (!matcher.find()) {

            throw new IllegalArgumentException(
                    "No amount found in \"" + text + "\"");
        }

        return new BigDecimal(matcher.group(1).replace(",", ""));
    }

    /**
     * Compares amounts by value, ignoring scale — "750" and "750.00" are
     * the same money, but BigDecimal.equals() says otherwise.
     */
    public static boolean equalAmounts(BigDecimal first, BigDecimal second) {

        return first != null && second != null && first.compareTo(second) == 0;
    }

}

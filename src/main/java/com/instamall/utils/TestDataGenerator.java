package com.instamall.utils;

import java.util.concurrent.ThreadLocalRandom;

public final class TestDataGenerator {

    private TestDataGenerator() {

    }

    /**
     * Random valid-format 10-digit US number. NANP requires the area code
     * and the exchange code to both start with 2–9, so those are generated
     * separately — a flat range over all 10-digit numbers can produce
     * values the app rejects (e.g. "4000000000"), making tests flaky.
     *
     * Use for UI-state tests only. Flows that actually hit the backend
     * (real OTP send) need a whitelisted number instead.
     */
    public static String randomPhoneNumber() {

        ThreadLocalRandom random = ThreadLocalRandom.current();

        int areaCode = random.nextInt(200, 1_000);
        int exchangeCode = random.nextInt(200, 1_000);
        int lineNumber = random.nextInt(0, 10_000);

        return String.format("%03d%03d%04d", areaCode, exchangeCode, lineNumber);
    }

    /**
     * Random incomplete number (5 digits) for negative scenarios.
     */
    public static String shortPhoneNumber() {

        return String.valueOf(
                ThreadLocalRandom.current().nextInt(10_000, 99_999));
    }

}
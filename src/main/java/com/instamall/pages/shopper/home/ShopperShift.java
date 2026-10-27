package com.instamall.pages.shopper.home;

import com.instamall.base.BasePage;
import com.instamall.driver.DriverManager;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.Duration;

/**
 * Turns the shopper's shift on after the Shopper app launches, which is
 * what every shopper journey needs before it can be given an order.
 *
 * {@link #ensureOnShift()} is the one entry point. Two cases, confirmed
 * live:
 *
 *   1. the "Go on shift?" sheet is already up (a shopper who logged in
 *      while Offline gets it on Home a moment after the permission
 *      alerts) — tap "Go on shift" and give the badge two seconds;
 *   2. no sheet — read the badge and turn the shift on from whatever
 *      state it is in.
 *
 * Either way the badge is read once at the end, so the caller gets the
 * status the app actually settled on.
 */
public class ShopperShift extends BasePage {

    /**
     * What the shift can be, and the colour the badge's status dot is
     * drawn in for it — the developer's own palette.
     */
    public enum ShiftStatus {

        ON_SHIFT(0x2cbf6e, "ON SHIFT"),
        ON_BREAK(0xFF8C00, "ON BREAK"),
        IN_ORDER(0xA48F66, "IN ORDER"),
        OFFLINE(0x6c737f, "OFFLINE");

        private final Color dot;
        private final String label;

        ShiftStatus(int rgb, String label) {

            this.dot = new Color(rgb);
            this.label = label;
        }

        public String label() {

            return label;
        }
    }

    // How long the "Go on shift?" sheet gets to appear after launch. It
    // arrives a second or two after the permission alerts are answered,
    // so a window this side of that is what tells "not coming" apart
    // from "not here yet".
    private static final Duration CARD_APPEARS_WITHIN = Duration.ofSeconds(5);

    // How long the badge gets to change over after the shift is turned
    // on, before it is read back.
    private static final long BADGE_SETTLE_MILLIS = 2000;

    // When the shift is turned on by hand — the "Go on shift?" sheet,
    // OFFLINE, or ON BREAK — the order the shopper is waiting for does not
    // land on Home immediately; confirmed live at about 30s behind the
    // turn-on. A shift that launched already ON SHIFT or IN ORDER has its
    // order waiting already, so it gets no such wait.
    private static final Duration ORDER_APPEARS_AFTER_MANUAL_SHIFT = Duration.ofSeconds(30);

    // The dot sits in the pill's left padding, somewhere in this band of
    // offsets from the badge's left edge. Scanned rather than sampled at
    // one fixed offset: the pill's width and padding follow its label,
    // so a fixed offset lands on padding on some states.
    private static final int DOT_SCAN_FROM_PT = 4;
    private static final int DOT_SCAN_TO_PT = 24;

    // Anything further than this from every palette colour is not one of
    // the four dots — a redesigned badge, say — and is reported as such
    // rather than guessed at.
    private static final double DOT_MATCH_TOLERANCE = 60.0;

    /**
     * Leaves the shopper on shift, whatever state the app launched in,
     * and reports the status the badge settled on.
     */
    public ShiftStatus ensureOnShift() {

        // Whether this call actually flipped the shift on — the sheet,
        // OFFLINE or ON BREAK — as opposed to finding it on already. Only a
        // hand turn-on makes the order land 30s late (see the constant).
        boolean turnedOnManually = false;

        if (WaitUtils.isVisible(ShopperHomePageLocators.GO_ON_SHIFT_BUTTON, CARD_APPEARS_WITHIN)) {

            // 1. The "Go on shift?" sheet is up — that is the whole job.
            System.out.println(">>> \"Go on shift?\" sheet is up — tapping \"Go on shift\"");
            click(ShopperHomePageLocators.GO_ON_SHIFT_BUTTON);
            settleBadge();
            turnedOnManually = true;

        } else {

            // 2. No sheet, so the badge says where the shift stands.
            ShiftStatus status = readStatus();

            if (status == ShiftStatus.ON_SHIFT) {

                System.out.println(">>> Status is ON SHIFT (green dot) — nothing to do");

            } else if (status == ShiftStatus.IN_ORDER) {

                System.out.println(">>> Status is IN ORDER (gold dot) — nothing to do");

            } else if (status == ShiftStatus.OFFLINE) {

                System.out.println(">>> Status is OFFLINE (grey dot) — tapping it, then \"Go on shift\"");
                click(ShopperHomePageLocators.SHIFT_STATUS_BADGE);
                click(ShopperHomePageLocators.GO_ON_SHIFT_BUTTON);
                settleBadge();
                turnedOnManually = true;

            } else {

                // Confirmed live: "Resume shift" is asked for twice —
                // once on the options sheet the badge opens, then again
                // on the "Resume shift?" confirmation that follows,
                // under the same button name both times.
                System.out.println(">>> Status is ON BREAK (orange dot) — tapping it, then \"Resume shift\" twice");
                click(ShopperHomePageLocators.SHIFT_STATUS_BADGE);
                click(ShopperHomePageLocators.RESUME_SHIFT_BUTTON);
                click(ShopperHomePageLocators.RESUME_SHIFT_BUTTON);
                settleBadge();
                turnedOnManually = true;
            }
        }

        ShiftStatus settled = readStatus();

        if (settled != ShiftStatus.ON_SHIFT && settled != ShiftStatus.IN_ORDER) {

            throw new IllegalStateException(
                    "The shift is still " + settled.label() + " after trying to turn it on");
        }

        System.out.println(">>> Shift confirmed " + settled.label());

        // Only when the shift was just turned on by hand: the order is not
        // on Home yet, so give it the ~30s it takes to arrive before the
        // caller goes looking for its card.
        if (turnedOnManually) {

            System.out.println(">>> Shift turned on manually — waiting "
                    + ORDER_APPEARS_AFTER_MANUAL_SHIFT.getSeconds()
                    + "s for the order to appear on Home");
            waitForManualShiftOrder();
        }

        return settled;
    }

    /**
     * What the badge reads, by the colour of its status dot.
     *
     * Confirmed live, by dumping the full accessibility tree while the
     * badge visibly read "ON SHIFT": none of the four labels appears
     * anywhere as a name, label or value — the badge's own accessible
     * name stays the state-blind "View shift status options" whatever it
     * displays, and it has no child elements. There is no accessible
     * signal to read at all, so the pixels are what is left.
     */
    public ShiftStatus readStatus() {

        try {

            WebElement badge = WaitUtils.waitForPresence(ShopperHomePageLocators.SHIFT_STATUS_BADGE);
            Rectangle rect = badge.getRect();

            File screenshotFile = DriverManager.getDriver().getScreenshotAs(OutputType.FILE);
            BufferedImage screenshot = ImageIO.read(screenshotFile);

            double scale = screenshot.getWidth()
                    / (double) DriverManager.getDriver().manage().window().getSize().getWidth();

            int lineY = (int) Math.round((rect.getY() + rect.getHeight() / 2.0) * scale);

            Color padding = averageAround(screenshot,
                    (int) Math.round((rect.getX() + DOT_SCAN_FROM_PT) * scale), lineY);

            Color dot = padding;
            double furthest = -1;
            int dotOffset = -1;

            for (int offset = DOT_SCAN_FROM_PT; offset <= DOT_SCAN_TO_PT; offset++) {

                Color sample = averageAround(screenshot,
                        (int) Math.round((rect.getX() + offset) * scale), lineY);

                double distance = distance(sample, padding);

                if (distance > furthest) {

                    furthest = distance;
                    dot = sample;
                    dotOffset = offset;
                }
            }

            ShiftStatus nearest = null;
            double nearestDistance = Double.MAX_VALUE;

            for (ShiftStatus status : ShiftStatus.values()) {

                double distance = distance(dot, status.dot);

                if (distance < nearestDistance) {

                    nearestDistance = distance;
                    nearest = status;
                }
            }

            String sampled = String.format("#%02x%02x%02x", dot.getRed(), dot.getGreen(), dot.getBlue());

            if (nearestDistance > DOT_MATCH_TOLERANCE) {

                throw new IllegalStateException("The shift badge's status dot (" + sampled + " at "
                        + dotOffset + "pt in) matches none of the four palette colours");
            }

            System.out.println(">>> Shift badge dot reads " + sampled + " at "
                    + dotOffset + "pt in — " + nearest.label());

            return nearest;

        } catch (IOException unreadableScreenshot) {

            throw new RuntimeException("Could not read the shift status dot's colour", unreadableScreenshot);
        }
    }

    private void settleBadge() {

        try {

            Thread.sleep(BADGE_SETTLE_MILLIS);

        } catch (InterruptedException interrupted) {

            Thread.currentThread().interrupt();
        }
    }

    private void waitForManualShiftOrder() {

        try {

            Thread.sleep(ORDER_APPEARS_AFTER_MANUAL_SHIFT.toMillis());

        } catch (InterruptedException interrupted) {

            Thread.currentThread().interrupt();
        }
    }

    private static Color averageAround(BufferedImage image, int centreX, int centreY) {

        long red = 0;
        long green = 0;
        long blue = 0;
        int sampled = 0;

        for (int dx = -3; dx <= 3; dx++) {

            for (int dy = -3; dy <= 3; dy++) {

                int rgb = image.getRGB(centreX + dx, centreY + dy);
                red += (rgb >> 16) & 0xFF;
                green += (rgb >> 8) & 0xFF;
                blue += rgb & 0xFF;
                sampled++;
            }
        }

        return new Color((int) (red / sampled), (int) (green / sampled), (int) (blue / sampled));
    }

    private static double distance(Color first, Color second) {

        int dr = first.getRed() - second.getRed();
        int dg = first.getGreen() - second.getGreen();
        int db = first.getBlue() - second.getBlue();

        return Math.sqrt(dr * dr + dg * dg + db * db);
    }

}

package com.instamall.pages.product;

import com.instamall.components.navigation.HeaderLocators;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * Product detail page, in both of the shapes the redesigned build gives
 * it. Verified against page source dumps.
 *
 * Opened from a store grid, the full page shows sizes as a row of inline
 * buttons. Opened from the wishlist's MOVE TO BAG, the app shows the
 * older compact sheet instead: a "Size …" card that opens a Select Size
 * sheet, a "Color …" card, and the same Add to bag bar. ProductDetailPage
 * checks which one it has and reads it accordingly.
 */
public final class ProductDetailPageLocators {

    private ProductDetailPageLocators() {

    }

    // The "BACK" link is gone; the header chevron is the way back.
    public static final By BACK_BUTTON = HeaderLocators.BACK_CHEVRON;

    public static final By TRY_ON =
            AppiumBy.accessibilityId("AI Try-On");

    public static final By PRODUCT_DETAILS_SECTION =
            AppiumBy.iOSNsPredicateString(
                    "name == 'PRODUCT DETAILS' AND accessible == 1"
            );

    public static final By SIMILAR_ITEMS_SECTION =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'SIMILAR ITEMS' AND accessible == 1"
            );

    /**
     * Headline price, e.g. "$750" — note the PDP prints no decimals while
     * the bag and checkout print "$750.00".
     *
     * The price appears twice — under the title and again in the bottom
     * bar beside Add to bag — and the headline one comes first in the
     * tree. SIMILAR ITEMS prices live inside tile names, not in their own
     * StaticTexts. Not restricted to accessible text: the compact sheet
     * exposes its price without that flag.
     */
    public static final By PRICE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name BEGINSWITH '$' AND visible == 1"
            );

    /**
     * Case-insensitive: the grid tile and the page do not always agree on
     * capitals ("Micro Check-Stitch Nylon Jacket" on the tile, "Micro
     * Check-stitch Nylon Jacket" here).
     */
    public static By productTitle(String productName) {

        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND name ==[c] '" + productName + "' AND visible == 1"
        );
    }

    // The brand line is printed in capitals on the full page ("BURBERRY")
    // and in title case on the compact sheet ("Burberry").
    public static By brand(String brandName) {

        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND name ==[c] '" + brandName + "' AND visible == 1"
        );
    }

    // ---------- Variants: full page (inline buttons) ----------

    /**
     * The "Size" heading over the size buttons. Absent on products with
     * nothing to choose; a one-size product still has it, with a single
     * "One Size" button already selected.
     */
    public static final By SIZE_CARD =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Size' AND accessible == 1"
            );

    /**
     * Every size button, in the order shown ("XS", "S", "M" …, or just
     * "One Size"). The chosen one carries the Selected trait and reports
     * value "1".
     *
     * They sit in the same flat container as the page's other buttons, so
     * they are told apart by shape: 47pt pills, narrower than the
     * full-width AI Try-On bar and taller than the "Size &amp; fit guide"
     * link.
     */
    public static final By SIZE_OPTIONS =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND accessible == 1"
                            + " AND rect.height > 40 AND rect.width < 200"
            );

    public static By sizeOption(String size) {

        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name == '" + size + "' AND accessible == 1"
                        + " AND rect.height > 40 AND rect.width < 200"
        );
    }

    // ---------- Variants: compact sheet (cards + Select Size sheet) ----------

    /**
     * Size card on the compact sheet. Reads "Size Select Size" until a
     * size is chosen, then "Size M" — or "Size One Size", disabled, when
     * there is nothing to choose. Its presence is what says the page is
     * in this shape at all.
     */
    public static final By SIZE_CARD_LEGACY =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeOther' AND name BEGINSWITH 'Size' AND accessible == 1"
            );

    /**
     * Colour line in either shape: the full page's "Color · Iron Grey"
     * text, or the compact sheet's "Color Iron Grey" card.
     */
    public static final By COLOR_CARD =
            AppiumBy.iOSNsPredicateString(
                    "name BEGINSWITH 'Color' AND accessible == 1 AND visible == 1"
            );

    public static final By SIZE_SHEET_TITLE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Select Size' AND accessible == 1"
            );

    /**
     * A size row inside the Select Size sheet. Rows carry their stock
     * note as well — "M Only 1 left!" — so the size is matched as a
     * prefix, with the trailing space keeping "S" from also matching
     * "XS"/"XL" rows.
     */
    public static By sizeSheetOption(String size) {

        return AppiumBy.iOSNsPredicateString(
                "name BEGINSWITH '" + size + " ' AND accessible == 1"
        );
    }

    /**
     * Every row currently offered in the Select Size sheet. While the
     * sheet is up everything behind it reports visible == false, which is
     * what keeps this from matching the page underneath. The sheet's own
     * scrim and close control match too but carry no name, so callers
     * skip the blank ones.
     */
    public static final By SIZE_SHEET_OPTIONS =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeOther' AND accessible == 1 AND visible == 1"
            );

    /**
     * Close control on the Select Size sheet — the only accessible direct
     * child of the sheet container, since the app leaves it unnamed.
     */
    public static final By SIZE_SHEET_CLOSE =
            AppiumBy.iOSClassChain(
                    "**/XCUIElementTypeOther[`name BEGINSWITH \"Select Size\"`]"
                            + "/XCUIElementTypeOther[`accessible == true`][1]"
            );

    // ---------- Actions ----------

    /**
     * The page's primary action, whichever of the two it currently is.
     *
     * A product already sitting in the bag shows "View bag (n)" where Add
     * to bag would be, so this is the only dependable sign that the page
     * has finished loading. Not restricted to accessible elements: on the
     * compact sheet the bar is exposed without that flag, and on the full
     * page the outer wrapper carries the same name — a tap on either lands
     * on the button.
     */
    public static final By PRIMARY_ACTION_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "(name == 'Add to bag' OR name BEGINSWITH 'View bag') AND visible == 1"
            );

    public static final By ADD_TO_BAG =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Add to bag' AND visible == 1"
            );

    // Takes Add to bag's place once the variant is in the bag:
    // "View bag (1)".
    public static final By VIEW_BAG =
            AppiumBy.iOSNsPredicateString(
                    "name BEGINSWITH 'View bag' AND visible == 1"
            );

    /**
     * What the page says instead of offering a size and a button when the
     * store cannot currently sell the product: "Temporarily unavailable.
     * Save it to your wishlist for priority access when it returns."
     *
     * VERIFY: not met on the redesigned build yet, so matched loosely.
     */
    public static final By TEMPORARILY_UNAVAILABLE =
            AppiumBy.iOSNsPredicateString(
                    "name BEGINSWITH[c] 'Temporarily unavailable' AND accessible == 1"
            );

    /**
     * The header strip: the Other whose direct children are the back
     * chevron, the heart, the share icon and the bag count. Its own name
     * is the brand plus that count ("Burberry 2"), so it is found by the
     * chevron it holds instead.
     */
    public static final By HEADER =
            AppiumBy.xpath(
                    "//XCUIElementTypeOther[XCUIElementTypeOther[@accessible='true'"
                            + " and (not(@name) or @name='') and @width='12' and @height='15']]"
            );

    /**
     * A header element's own children, in render order.
     *
     * The icons carry no name, label or id of any kind, so position is all
     * there is to identify them by. Leaving out the back chevron and the
     * header's own unnamed background, the first unnamed accessible
     * visible child is the wishlist heart, the second the share icon
     * beside it. Confirmed by tapping the first and finding the product in
     * the wishlist afterwards.
     *
     * An accessibility id on the heart would replace all of this.
     */
    public static final By HEADER_CHILDREN = AppiumBy.xpath("./*");

    // ---------- Size-selection helper text ----------

    /**
     * Shown under the size row before any size is picked: "Select a size
     * to add this item to your bag."
     */
    public static final By SELECT_SIZE_PROMPT =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText'"
                            + " AND name == 'Select a size to add this item to your bag.' AND accessible == 1"
            );

    /**
     * Shown instead of {@link #SELECT_SIZE_PROMPT} on a genuinely
     * one-size product: "One size. See product dimensions when
     * supplied." — the size button is already the only option, so there
     * is nothing to prompt a choice for.
     */
    public static final By ONE_SIZE_NOTE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText'"
                            + " AND name == 'One size. See product dimensions when supplied.' AND accessible == 1"
            );

    // ---------- Independent Shopping Service ----------

    /**
     * Small badge near the image carousel — an internal literal name
     * ("independent_shopping_badge"), not a real accessibility id, so
     * matched by accessibilityId all the same.
     */
    public static final By INDEPENDENT_SHOPPING_BADGE =
            AppiumBy.accessibilityId("independent_shopping_badge");

    // ---------- Colour selection ----------

    /**
     * A colour swatch button, addressed by the colour name it carries
     * (e.g. "Nude", "Pink") — same shape as {@link #sizeOption(String)}
     * but for the colour row instead of the size row.
     */
    public static By colorOption(String colorName) {

        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name == '" + colorName + "' AND accessible == 1"
        );
    }

}

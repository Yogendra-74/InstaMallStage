package com.instamall.pages.product;

import com.instamall.base.BasePage;
import com.instamall.driver.DriverManager;
import com.instamall.pages.bag.ShoppingBagPageLocators;
import com.instamall.waits.WaitUtils;

import java.time.Duration;

public class ProductDetailPageValidator extends BasePage {

    private static final Duration PRODUCT_LOAD_TIMEOUT = Duration.ofSeconds(45);

    public boolean isDisplayed() {

        return isDisplayed(ProductDetailPageLocators.PRIMARY_ACTION_BUTTON, PRODUCT_LOAD_TIMEOUT);
    }

    public boolean isProductTitleDisplayed(String productName) {

        return isDisplayed(ProductDetailPageLocators.productTitle(productName));
    }

    public boolean isBrandDisplayed(String brandName) {

        return isDisplayed(ProductDetailPageLocators.brand(brandName));
    }

    public boolean isPriceDisplayed() {

        return isDisplayed(ProductDetailPageLocators.PRICE);
    }

    // Presence, not visibility: the size row sits under the fixed Add to
    // bag bar on products with a long title.
    public boolean isSizeCardDisplayed() {

        return isPresent(ProductDetailPageLocators.SIZE_CARD)
                || WaitUtils.isPresent(ProductDetailPageLocators.SIZE_CARD_LEGACY, Duration.ofSeconds(1));
    }

    /**
     * False when there is only one size: the app picks it for you (a
     * single pre-selected "One Size" button, say), so there is nothing to
     * choose.
     */
    public boolean isSizeCardEnabled() {

        // Compact sheet: the app disables the card when only one size is
        // left. Full page: a single button means nothing to choose.
        if (new ProductDetailPage().hasLegacySizeCard()) {

            return DriverManager.getDriver()
                    .findElement(ProductDetailPageLocators.SIZE_CARD_LEGACY)
                    .isEnabled();
        }

        return DriverManager.getDriver()
                .findElements(ProductDetailPageLocators.SIZE_OPTIONS)
                .size() > 1;
    }

    public boolean isColorCardDisplayed() {

        return isDisplayed(ProductDetailPageLocators.COLOR_CARD);
    }

    public boolean isSizeOptionDisplayed(String size) {

        return isDisplayed(ProductDetailPageLocators.sizeOption(size))
                || isDisplayed(ProductDetailPageLocators.sizeSheetOption(size));
    }

    public boolean isAddToBagDisplayed() {

        return isDisplayed(ProductDetailPageLocators.ADD_TO_BAG);
    }

    /**
     * True once the item is in the bag — Add to bag turns into a
     * "View bag (n)" button, or, from the compact sheet, the bag screen
     * itself opens.
     */
    public boolean isViewBagDisplayed() {

        return isDisplayed(ProductDetailPageLocators.VIEW_BAG)
                || WaitUtils.isVisibleNow(ShoppingBagPageLocators.TITLE);
    }

    /**
     * True before a size is picked on a product that genuinely has more
     * than one to choose from.
     */
    public boolean isSelectSizePromptDisplayed() {

        return isDisplayed(ProductDetailPageLocators.SELECT_SIZE_PROMPT);
    }

    /**
     * True on a one-size product, where there is nothing to prompt a
     * choice for — see {@link #isSelectSizePromptDisplayed()} for its
     * counterpart on a product with real sizes to pick from.
     */
    public boolean isOneSizeNoteDisplayed() {

        return isDisplayed(ProductDetailPageLocators.ONE_SIZE_NOTE);
    }

    public boolean isIndependentShoppingBadgeDisplayed() {

        return isDisplayed(ProductDetailPageLocators.INDEPENDENT_SHOPPING_BADGE);
    }

}

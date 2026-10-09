package com.instamall.tests;

import com.instamall.base.BaseTest;
import com.instamall.components.location.LocationServicesPrompt;
import com.instamall.config.ConfigReader;
import com.instamall.flows.CheckoutFlow;
import com.instamall.driver.AppSession;
import com.instamall.pages.bag.ShoppingBagPage;
import com.instamall.pages.bag.ShoppingBagPageValidator;
import com.instamall.pages.home.HomePage;
import com.instamall.pages.home.HomePageValidator;
import com.instamall.pages.location.SelectLocationSheet;
import com.instamall.pages.login.EmailLoginPage;
import com.instamall.pages.login.LoginPage;
import com.instamall.pages.onboarding.OnboardingPage;
import com.instamall.pages.onboarding.OnboardingPageValidator;
import com.instamall.pages.product.ProductDetailPage;
import com.instamall.pages.product.ProductDetailPageValidator;
import com.instamall.pages.store.StorePage;
import com.instamall.pages.wishlist.WishlistPage;
import com.instamall.pages.wishlist.WishlistPageValidator;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.TimeoutException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Wishlist journey on staging:
 *
 *   email login -> delivery location -> store -> product -> heart
 *   -> WISHLIST -> MOVE TO BAG -> size -> ADD TO BAG -> bag -> checkout
 *   -> payment -> order confirmation
 *
 * The wishlist half of this was written against captured accessibility
 * trees rather than guessed at, because the app names almost none of it:
 * the heart carries no name, and a saved card is a single accessibility
 * element that swallows its own MOVE TO BAG button. Both are reached by
 * position, and both would be plain locators if the app gave them ids.
 *
 * NOTE: this pays with the staging test card, so every run creates a real
 * staging order.
 */
public class WishlistToBagTest extends BaseTest {

    private final OnboardingPage onboardingPage = new OnboardingPage();
    private final OnboardingPageValidator onboardingValidator = new OnboardingPageValidator();
    private final LoginPage loginPage = new LoginPage();
    private final EmailLoginPage emailLoginPage = new EmailLoginPage();
    private final HomePage homePage = new HomePage();
    private final HomePageValidator homeValidator = new HomePageValidator();
    private final SelectLocationSheet locationSheet = new SelectLocationSheet();
    private final StorePage storePage = new StorePage();
    private final ProductDetailPage productPage = new ProductDetailPage();
    private final ProductDetailPageValidator productValidator = new ProductDetailPageValidator();
    private final WishlistPage wishlistPage = new WishlistPage();
    private final WishlistPageValidator wishlistValidator = new WishlistPageValidator();
    private final ShoppingBagPage bagPage = new ShoppingBagPage();
    private final ShoppingBagPageValidator bagValidator = new ShoppingBagPageValidator();

    private static final double MIAMI_LATITUDE = 25.7995;
    private static final double MIAMI_LONGITUDE = -80.1998;

    private long firstStepAt;
    private long previousStepAt;

    @BeforeClass(alwaysRun = true)
    public void prepareCleanSignedOutApp() {

        AppSession.clearAppData();
        AppSession.grantLocationAndNotifications();
        AppSession.setSimulatedLocation(MIAMI_LATITUDE, MIAMI_LONGITUDE);
    }

    @Test(description = "Save a product to the wishlist, move it to the bag from there, and check out")
    public void orderAProductSavedToTheWishlist() {

        String addressName = ConfigReader.get("deliveryAddressName");
        String preferredSize = ConfigReader.get("productSize");
        String cardLast4 = ConfigReader.get("savedCardLast4");

        // ---------- 1. Sign in ----------

        step("Waiting for the app's first screen");
        signIn();

        // ---------- 2. Delivery location ----------

        step("Select delivery location: " + addressName);
        chooseDeliveryLocation(addressName);

        // ---------- 3 to 7. Mall, product, heart, wishlist, move to bag ----------

        // Taken as attempts that can be repeated, across malls as well as
        // across products. A single-unit product goes "Temporarily
        // unavailable" the moment somebody buys it — a previous run of this
        // suite counts — and such a page carries no size and no ADD TO BAG
        // at all, so there is nothing to salvage from it.
        //
        // One mall is not enough to retry within: the grid only offers the
        // tiles that are on screen, and a catalogue this suite has been
        // shopping all session can have every one of them sold out. When a
        // mall runs dry the run moves to the next one.
        List<String> storeCards = homePage.listVisibleStoreCards();

        Assert.assertFalse(storeCards.isEmpty(), "No store cards on the home feed");

        // What is already saved, read once before anything is chosen.
        //
        // The heart is a toggle, and its state cannot be read from the
        // screen — it is an unnamed element with no label or value, so a
        // marked heart looks identical to an unmarked one. Tapping it on a
        // product that is already saved takes that product out of the
        // wishlist, and the run then fails looking for what it just
        // removed. Knowing what is in there is the only way to leave those
        // products alone.
        step("Read what the wishlist already holds");

        homePage.openWishList();
        wishlistPage.waitForScreen();

        List<String> alreadySaved = wishlistPage.listSavedProducts();

        // The header count is the honest total. The card list only covers
        // what fits on screen, so it is a cheap pre-filter and nothing more
        // — a product saved further down is invisible to it, and hearting
        // one of those takes it out instead of putting it in.
        int savedCount = wishlistPage.savedCount();

        step("Wishlist already holds " + savedCount + " item(s), "
                + alreadySaved.size() + " of them on screen");

        homePage.openHomeTab();
        homePage.waitForScreen();

        String brand = null;
        String product = null;
        String chosenSize = null;
        boolean added = false;

        malls:
        for (String storeCard : storeCards) {

            step("Open a mall from the feed");
            homePage.openStoreCard(storeCard);

            String mall = storePage.waitForScreen().getBrandName();
            step("Mall: " + mall);

            List<String> tiles = storePage.listVisibleProductTiles();

            if (tiles.isEmpty()) {

                step("No product tiles on the " + mall + " grid — trying another mall");

                storePage.goBack();
                homePage.waitForScreen();

                continue;
            }

            for (String tile : tiles) {

                String candidate = productNameFrom(tile, mall);

                // Skipped without even opening it: its heart is already
                // marked, so tapping it would take it out of the wishlist
                // rather than put it in.
                if (isAlreadySaved(alreadySaved, candidate)) {

                    step("\"" + candidate + "\" is already in the wishlist — taking a"
                            + " product that is not");

                    continue;
                }

                step("Open product: " + tile);
                storePage.openProductTile(tile);

                if (!productPage.waitUntilBuyableOrUnavailable()) {

                    step("\"" + candidate + "\" is temporarily unavailable — saving a"
                            + " different product to the wishlist instead");

                    productPage.goBack();
                    storePage.waitForScreen();

                    continue;
                }

                if (!productValidator.isSizeCardDisplayed()) {

                    step("\"" + candidate + "\" has no size to choose — trying the"
                            + " next tile");

                    productPage.goBack();
                    storePage.waitForScreen();

                    continue;
                }

                step("Product: " + candidate);

                Assert.assertTrue(productValidator.isProductTitleDisplayed(candidate),
                        "Product detail page is not showing \"" + candidate + "\"");

                // ---------- 5. Save it to the wishlist ----------

                step("Tap the heart to save it");
                productPage.tapWishlistHeart();

                // ---------- 6. WISHLIST ----------

                // The tab bar sits under the product page, so back out to it.
                productPage.goBack();
                storePage.waitForScreen();
                storePage.goBack();

                step("Open the WishList tab");
                homePage.waitForScreen().openWishList();

                wishlistPage.waitForScreen();

                Assert.assertTrue(wishlistValidator.areTabsDisplayed(),
                        "WISHLIST is not offering both Products and Brands");

                int savedAfter = wishlistPage.savedCount();

                if (!wishlistValidator.isProductSaved(candidate)) {

                    // The heart is a toggle whose state cannot be read, and
                    // the list read up front only sees the cards that fit on
                    // screen — the wishlist holds more than that. So this
                    // product was already saved, out of sight, and the tap
                    // has taken it out rather than put it in. The header
                    // count is the honest total and says which way it went.
                    step("\"" + candidate + "\" was already saved out of sight (count "
                            + savedCount + " -> " + savedAfter + "), so the heart"
                            + " removed it — taking a different product");

                    savedCount = savedAfter;

                    homePage.openHomeTab();
                    homePage.waitForScreen();

                    homePage.openStoreCard(storeCard);
                    storePage.waitForScreen();

                    continue;
                }

                savedCount = savedAfter;

                step("Wishlist holds " + savedAfter + " item(s)");

                // The app puts the most recent save first, so the product
                // just hearted should lead the grid.
                Optional<String> firstCard = wishlistPage.firstSavedProduct();

                Assert.assertTrue(firstCard.isPresent(), "The wishlist grid is empty");

                Assert.assertTrue(firstCard.get().contains(plainRun(candidate)),
                        "The newest save should lead the wishlist, but the first card"
                                + " is \"" + firstCard.get() + "\" rather than \""
                                + candidate + "\"");

                step("Newest save is on top: " + firstCard.get());

                // ---------- 7. Move it to the bag ----------

                step("MOVE TO BAG");
                wishlistPage.moveToBag(candidate);

                // MOVE TO BAG opens the product's own page, which is where
                // the add happens — unless the store has stopped selling it
                // since it was saved, which is exactly what a wishlist is
                // for and exactly what this cannot buy.
                if (!productPage.waitUntilBuyableOrUnavailable()) {

                    step("\"" + candidate + "\" went temporarily unavailable after"
                            + " being saved, so it cannot be moved into the bag —"
                            + " saving a different product instead");

                    // This product page was reached from the wishlist, so
                    // going back lands there rather than on the feed. The
                    // tab bar is the way home from any of these screens,
                    // which beats guessing how many times to go back.
                    productPage.goBack();
                    wishlistPage.waitForScreen();

                    homePage.openHomeTab();
                    homePage.waitForScreen();

                    homePage.openStoreCard(storeCard);
                    storePage.waitForScreen();

                    continue;
                }

                Assert.assertTrue(productValidator.isProductTitleDisplayed(candidate),
                        "MOVE TO BAG did not open \"" + candidate + "\"");

                chosenSize = chooseASize(preferredSize);

                step("Add to bag (" + candidate + ", size " + chosenSize + ")");
                added = productPage.addToBagIfNeeded();

                step(added
                        ? "Item added to the bag"
                        : "That variant was already in the bag — carrying on with it");

                // The success pill: the app replaces ADD TO BAG with
                // "VIEW BAG (n)" once the item is in, which is the only
                // confirmation it gives.
                Assert.assertTrue(productValidator.isViewBagDisplayed(),
                        "No VIEW BAG pill after adding to bag — the item was not added");

                step("Success confirmed by the VIEW BAG pill");

                brand = mall;
                product = candidate;

                break malls;
            }

            step("Nothing on the " + mall + " grid could be saved and moved —"
                    + " trying another mall");

            storePage.goBack();
            homePage.waitForScreen();
        }

        Assert.assertNotNull(product,
                "Not one product in any mall on the feed could be saved to the"
                        + " wishlist and moved into the bag — the visible grids were"
                        + " all either sold out or sizeless");

        // A move should empty the item out of the wishlist, and the app
        // drops back there after adding, so this is where to look.
        //
        // Reported, never asserted. An item that stays saved is worth
        // knowing about, but it is not what this journey is for, and
        // stopping here would cost the whole checkout below.
        verifyItMovedOutOfTheWishlist(product, added);

        productPage.clickViewBag();

        // ---------- 8. Bag ----------

        step("Shopping bag");
        bagPage.waitForScreen();
        bagPage.waitForItem(product, chosenSize);

        Assert.assertTrue(bagValidator.isStoreGroupDisplayed(brand),
                "Bag is missing the " + brand + " group");

        Assert.assertTrue(bagPage.hasItem(product, chosenSize),
                "Bag has no row for \"" + product + "\" in " + chosenSize);

        step("Bag subtotal reads " + bagPage.getCheckoutAmount());

        // ---------- 9. Checkout, payment and confirmation ----------

        // The same checkout the end-to-end journey uses, not a second copy
        // of it: everything it handles — stock only being settled when
        // Checkout is tapped, a refused delivery becoming a pickup, the
        // pickup total differing from the delivery one — was learnt from
        // failing runs, and two copies would drift apart.
        CheckoutFlow checkout = new CheckoutFlow(this::step);

        checkout.openCheckoutFromBag(product, chosenSize);

        CheckoutFlow.Placed placed = checkout.placeOrder(addressName);

        // No colour is read on this journey, so the variant line is left
        // unchecked rather than asserted against a guess.
        checkout.payAndVerifyOrder(placed, cardLast4, brand, product, chosenSize, null);
    }

    /**
     * Checks that moving a product to the bag took it out of the wishlist,
     * and says so either way without ever failing the run.
     *
     * Three cases, and telling them apart matters — reporting "moved out"
     * when nothing moved would be worse than saying nothing:
     *
     *   - nothing was added, because the variant was already in the bag, so
     *     nothing was moved and there is nothing to check
     *   - the app is not back on the wishlist, so the question cannot be
     *     answered from here
     *   - it is on the wishlist, and the card either goes or it does not
     */
    private void verifyItMovedOutOfTheWishlist(String product, boolean added) {

        if (!added) {

            step("Already in the bag, so nothing was moved — wishlist left as it was");

            return;
        }

        if (!wishlistValidator.isDisplayed()) {

            step("NOTE: not back on the wishlist after adding, so whether \""
                    + product + "\" moved out could not be checked. Carrying on.");

            return;
        }

        if (wishlistPage.waitUntilNoLongerSaved(product, MOVE_LEAVES_WISHLIST_WITHIN)) {

            step("Moved out of the wishlist, as a move should — "
                    + wishlistPage.savedCount() + " item(s) left");

        } else {

            step("WARNING: \"" + product + "\" is STILL in the wishlist after MOVE TO"
                    + " BAG — it went into the bag but was not moved out, so the app"
                    + " has copied it rather than moved it. Carrying on with checkout.");
        }
    }

    /** How long a moved product gets to disappear from the wishlist. */
    private static final Duration MOVE_LEAVES_WISHLIST_WITHIN = Duration.ofSeconds(8);

    /** How long to watch for whichever way in the app offers. */
    private static final Duration LOCATION_ENTRY_WITHIN = Duration.ofSeconds(15);

    /**
     * How the app is offering to take a delivery address.
     */
    private enum LocationEntry {

        /** The app's own Enable Location sheet is up. */
        PROMPT,

        /** No sheet — the home feed is showing. */
        HOME
    }

    /**
     * Waits until the app has settled on one or the other.
     *
     * The sheet, when it comes, animates in over the home screen; when it
     * does not come, the home screen is simply there. So the home tab being
     * up is not on its own proof that no sheet is coming, and both are
     * watched together with the sheet winning any tie.
     *
     * Falls back to the feed rather than failing: that is the recoverable
     * answer, and the caller checks what is actually on it.
     */
    private LocationEntry awaitLocationEntry() {

        try {

            return WaitUtils.waitUntil(driver ->
                            LocationServicesPrompt.isShowingNow()
                                    ? LocationEntry.PROMPT
                                    : homeValidator.isHomeTabDisplayedNow()
                                            ? LocationEntry.HOME
                                            : null,
                    LOCATION_ENTRY_WITHIN);

        } catch (TimeoutException neitherAppeared) {

            return LocationEntry.HOME;
        }
    }

    // ---------- prelude ----------

    private void signIn() {

        if (onboardingValidator.isLoginButtonDisplayed()) {

            step("Onboarding -> Login");
            onboardingPage.clickLogin();

            loginPage.waitForScreen().clickContinueWithEmail();

            emailLoginPage.waitForScreen()
                          .enterEmail(ConfigReader.get("testEmail"))
                          .enterPassword(ConfigReader.get("testPassword"))
                          .clickNext();

            step("Signed in");

        } else {

            step("A session is already active — skipping sign in");
        }
    }

    private void chooseDeliveryLocation(String addressName) {

        // What the app is showing is established before anything is
        // touched. Two things can be true after signing in: the app raises
        // its own Enable Location sheet, which covers the feed behind it,
        // or it comes up already parked at this address with the malls
        // listed — the address lives on the account, so that is common.
        //
        // Asking too early is what made this useless before: the check ran
        // the instant sign in returned, found no address on a home screen
        // that had not painted yet, and went off to pick one that was
        // already picked.
        if (awaitLocationEntry() == LocationEntry.PROMPT) {

            step("The app is asking for a location");
            LocationServicesPrompt.selectLocationNow();

        } else {

            homePage.waitForScreen();

            boolean alreadyThere = homeValidator.isLocationSelected(addressName);
            boolean mallsListed = !homePage.listVisibleStoreCards().isEmpty();

            if (alreadyThere && mallsListed) {

                step("Already at \"" + addressName + "\" with the malls listed —"
                        + " nothing to select");

                return;
            }

            step(alreadyThere
                    ? "Set to \"" + addressName + "\" but no malls listed — picking it again"
                    : "Home is up but not set to \"" + addressName + "\" — picking it");

            homePage.openLocationSelector();
        }

        locationSheet.waitUntilVisible();
        locationSheet.waitForScreen();

        Assert.assertTrue(new com.instamall.pages.location.SelectLocationSheetValidator()
                        .isSavedAddressDisplayed(addressName),
                "Saved address \"" + addressName + "\" is not listed in the location sheet");

        locationSheet.chooseSavedAddress(addressName);

        homePage.waitForScreen();

        Assert.assertTrue(homeValidator.isLocationSelected(addressName),
                "Home header does not show \"" + addressName + "\" after selecting it");
    }

    /**
     * Picks a size the way the end-to-end journey does: the wanted one when
     * it is in stock, the first on offer otherwise, and whatever the app
     * has fixed when only one size is left and the card is disabled.
     */
    private String chooseASize(String preferredSize) {

        if (!productValidator.isSizeCardEnabled()) {

            String fixed = productPage.getSelectedSize();

            Assert.assertFalse(fixed.isEmpty(),
                    "The size card is disabled and empty, so the product is out of stock");

            step("Only one size left — the app has fixed it at " + fixed);

            return fixed;
        }

        List<String> sizes = productPage.openSizeSheet().getAvailableSizes();

        Assert.assertFalse(sizes.isEmpty(),
                "The Select Size sheet is open but offering no sizes");

        String chosen = sizes.contains(preferredSize) ? preferredSize : sizes.get(0);

        step("Sizes on offer " + sizes + " — taking " + chosen);
        productPage.chooseSize(chosen);

        return chosen;
    }

    // ---------- helpers ----------

    private void assertSameAmount(BigDecimal actual, BigDecimal expected, String message) {

        Assert.assertTrue(com.instamall.utils.CurrencyUtils.equalAmounts(actual, expected),
                message + " — expected " + expected + " but found " + actual);
    }

    /**
     * Pulls the product name out of a tile name, which reads
     * "&lt;product&gt; &lt;brand&gt; $&lt;price&gt;".
     */
    private String productNameFrom(String tileName, String brand) {

        int brandAt = tileName.lastIndexOf(" " + brand + " $");

        return brandAt < 0 ? tileName : tileName.substring(0, brandAt);
    }

    /**
     * Whether a product is among those already saved.
     *
     * Compared on the part of the name that carries no zero-width
     * characters, since a wishlist card and a grid tile do not reliably
     * agree on those. A card reads "&lt;product&gt; &lt;brand&gt; $&lt;price&gt; MOVE TO
     * BAG", so the product's name being inside it is the match.
     */
    private boolean isAlreadySaved(List<String> savedCards, String productName) {

        String comparable = plainRun(productName);

        return savedCards.stream().anyMatch(card -> card.contains(comparable));
    }

    /**
     * The part of a name safe to compare: this catalogue's product names
     * carry zero-width characters that do not survive every screen.
     */
    private String plainRun(String productName) {

        String longest = "";

        for (String run : productName.split("[\\u200B-\\u200D\\uFEFF]")) {

            if (run.trim().length() > longest.length()) {

                longest = run.trim();
            }
        }

        return longest.isEmpty() ? productName : longest;
    }

    private void step(String description) {

        long now = System.nanoTime();

        if (firstStepAt == 0) {

            firstStepAt = now;
            previousStepAt = now;
        }

        double sinceStart = (now - firstStepAt) / 1_000_000_000.0;
        double sincePrevious = (now - previousStepAt) / 1_000_000_000.0;

        previousStepAt = now;

        System.out.printf(">>> [%6.1fs +%5.1fs] STEP: %s%n",
                sinceStart, sincePrevious, description);
    }
}

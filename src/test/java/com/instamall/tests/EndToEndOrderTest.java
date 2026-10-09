package com.instamall.tests;

import com.instamall.base.BaseTest;
import com.instamall.components.location.LocationServicesPrompt;
import com.instamall.components.permissions.SystemAlerts;
import com.instamall.config.ConfigReader;
import com.instamall.flows.CheckoutFlow;
import com.instamall.driver.AppSession;
import com.instamall.pages.bag.ShoppingBagPage;
import com.instamall.pages.bag.ShoppingBagPageValidator;
import com.instamall.pages.home.HomePage;
import com.instamall.pages.home.HomePageValidator;
import com.instamall.pages.location.SelectLocationSheet;
import com.instamall.pages.location.SelectLocationSheetValidator;
import com.instamall.pages.login.EmailLoginPage;
import com.instamall.pages.login.EmailLoginPageValidator;
import com.instamall.pages.login.LoginPage;
import com.instamall.pages.onboarding.OnboardingPage;
import com.instamall.pages.onboarding.OnboardingPageValidator;
import com.instamall.pages.product.ProductDetailPage;
import com.instamall.pages.product.ProductDetailPageValidator;
import com.instamall.pages.store.StorePage;
import com.instamall.pages.store.StorePageValidator;
import com.instamall.utils.CurrencyUtils;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.TimeoutException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Full purchase journey on staging:
 *
 *   email login -> permissions -> delivery location -> store -> product
 *   -> bag -> checkout -> payment -> order confirmation
 *
 * Kept as a single test method on purpose. BaseTest relaunches the app
 * before every @Test, so splitting these steps would restart the app
 * halfway through the journey.
 *
 * NOTE: this pays with the staging test card, so every run creates a real
 * staging order.
 */
public class EndToEndOrderTest extends BaseTest {

    /**
     * Permission prompts must reach a real tap rather than being
     * auto-denied, per instruction to grant everything this run meets.
     * Location and notifications are pre-granted through the driver
     * regardless (see prepareCleanSignedOutApp()) and so do not actually
     * show as alerts even with this off; what this does expose is Save
     * Password and App Tracking Transparency, both real alerts neither
     * pre-grant reaches.
     */
    @Override
    protected boolean autoDismissAlerts() {

        return false;
    }

    private final OnboardingPage onboardingPage = new OnboardingPage();
    private final OnboardingPageValidator onboardingValidator = new OnboardingPageValidator();
    private final LoginPage loginPage = new LoginPage();
    private final EmailLoginPage emailLoginPage = new EmailLoginPage();
    private final EmailLoginPageValidator emailLoginValidator = new EmailLoginPageValidator();
    private final HomePage homePage = new HomePage();
    private final HomePageValidator homeValidator = new HomePageValidator();
    private final SelectLocationSheet locationSheet = new SelectLocationSheet();
    private final SelectLocationSheetValidator locationValidator = new SelectLocationSheetValidator();
    private final StorePage storePage = new StorePage();
    private final StorePageValidator storeValidator = new StorePageValidator();
    private final ProductDetailPage productPage = new ProductDetailPage();
    private final ProductDetailPageValidator productValidator = new ProductDetailPageValidator();
    private final ShoppingBagPage bagPage = new ShoppingBagPage();
    private final ShoppingBagPageValidator bagValidator = new ShoppingBagPageValidator();
    // private final CheckoutPage checkoutPage = new CheckoutPage();
    // private final CheckoutPageValidator checkoutValidator = new CheckoutPageValidator();
    // private final PaymentSheet paymentSheet = new PaymentSheet();
    // private final PaymentSheetValidator paymentValidator = new PaymentSheetValidator();
    // private final OrderConfirmationPage orderPage = new OrderConfirmationPage();
    // private final OrderConfirmationPageValidator orderValidator = new OrderConfirmationPageValidator();

    /**
     * The delivery address this journey uses, so the store feed is served
     * for the right city no matter where the machine thinks it is.
     */
    /** How long the payment sheet gets before the flow tries pickup. */
    // private static final Duration PAYMENT_SHEET_APPEARS_WITHIN = Duration.ofSeconds(25);

    /**
     * How long the app gets to paint its opening screen. Generous on
     * purpose: the data has just been wiped, so this is a cold start with
     * a JS bundle to load, and reading it too early is what makes a
     * signed-out app look signed in.
     */
    private static final Duration APP_FIRST_SCREEN_WITHIN = Duration.ofSeconds(45);

    /**
     * How long to watch for a way into the address sheet. Same ceiling the
     * Enable Location sheet had on its own, so nothing that used to be
     * caught is missed — it is only reached now when neither route shows.
     */
    private static final Duration LOCATION_ENTRY_WITHIN = Duration.ofSeconds(15);

    /** Step timing, so the log shows where the journey spends its time. */
    private long firstStepAt;
    private long previousStepAt;

    private static final double MIAMI_LATITUDE = 25.7995;
    private static final double MIAMI_LONGITUDE = -80.1998;

    /**
     * Prepares the app before its next launch: signed out, location
     * granted, and parked in the delivery city.
     *
     * Location has to be granted, not merely asked for — refuse it and the
     * app answers "Instamall is not live in your area yet" instead of
     * showing a store feed. It is granted through the driver rather than
     * by tapping the iOS alert, because an alert on screen swallows the
     * taps meant for the app underneath.
     *
     * Runs after BaseTest.startSession — TestNG invokes superclass
     * &#64;BeforeClass methods first — and before BaseTest.resetApp
     * relaunches the app.
     */
    @BeforeClass(alwaysRun = true)
    public void prepareCleanSignedOutApp() {

        AppSession.clearAppData();
        AppSession.grantLocationAndNotifications();
        AppSession.setSimulatedLocation(MIAMI_LATITUDE, MIAMI_LONGITUDE);
    }

    @Test(description = "Place an order end to end: email login, location, store, product, bag, checkout, payment, confirmation")
    public void placeOrderWithSavedCard() {

        String email = ConfigReader.get("testEmail");
        String password = ConfigReader.get("testPassword");
        String addressName = ConfigReader.get("deliveryAddressName");
        String size = ConfigReader.get("productSize");
        String cardLast4 = ConfigReader.get("savedCardLast4");

        // ---------- 1. Sign in with email ----------

        // Waited for properly rather than glanced at. The app data has
        // just been wiped, so this is a cold start — bundle, splash, then
        // onboarding — and a short look decides "no Login button, so a
        // session must be active" while the app is still painting. The
        // run then skips sign in and fails a minute later on a home
        // screen that was never coming.
        step("Waiting for the app's first screen");

        if (awaitFirstScreen() == FirstScreen.ONBOARDING) {

            step("Onboarding -> Login");
            onboardingPage.clickLogin();

            step("Switch to email login");
            loginPage.waitForScreen()
                     .clickContinueWithEmail();

            emailLoginPage.waitForScreen();

            Assert.assertTrue(emailLoginValidator.isEmailFieldDisplayed(),
                    "Email field is not displayed on the email login screen");

            Assert.assertTrue(emailLoginValidator.isPasswordFieldDisplayed(),
                    "Password field is not displayed on the email login screen");

            step("Sign in as " + email);
            emailLoginPage.enterEmail(email)
                          .enterPassword(password);

            // Split out so the log separates what typing costs from what
            // the sign-in round trip costs — they were one number before,
            // and most of it turned out to be typing.
            step("Credentials entered — submitting");
            emailLoginPage.clickNext();

        } else {

            step("A session is already active — skipping sign in");
        }

        // ---------- 2. iOS permission alerts ----------

        // Only worth doing when iOS is leaving the alerts to us. With
        // autoDismissAlerts on, iOS has already answered them and these
        // calls can only sit out their timeouts before finding nothing —
        // about eleven seconds of every run, for no information.
        if (autoDismissAlerts()) {

            step("iOS is answering the permission alerts itself");

        } else {

            step("Handle permission alerts");
            SystemAlerts.allowLocationWhileUsingApp();
            SystemAlerts.allowNotifications();
        }

        // ---------- 3. Delivery location ----------

        // Location comes before any home assertion on purpose. On a fresh
        // install the app raises its own "Enable Location" sheet, and
        // while that is up everything behind it reports visible == false —
        // so waiting on the home screen first would simply time out.
        step("Select delivery location: " + addressName);

        // Two ways in, and the app decides which: its own Enable Location
        // sheet on a fresh install, or the header chip once a location is
        // already known. Both are watched at once — waiting on the sheet
        // alone means paying its full deadline on every run that is never
        // going to get one, which measured at fifteen seconds.
        if (awaitLocationEntry() == LocationEntry.PROMPT) {

            step("Taking the Enable Location sheet's \"Select location\"");
            LocationServicesPrompt.selectLocationNow();

            chooseSavedAddressFromSheet(addressName);

        } else {

            homePage.waitForScreen();

            // The address lives on the account, so a signed-in app often
            // comes up already parked at it with the malls listed. There is
            // nothing to choose then, and going through the sheet anyway
            // only spends time re-picking what is already picked.
            boolean alreadyThere = homeValidator.isLocationSelected(addressName);
            boolean mallsListed = !homePage.listVisibleStoreCards().isEmpty();

            if (alreadyThere && mallsListed) {

                step("Already at \"" + addressName + "\" with the malls listed —"
                        + " nothing to select");

            } else {

                step(alreadyThere
                        ? "Set to \"" + addressName + "\" but no malls listed —"
                                + " picking it again"
                        : "No Enable Location sheet — going in via the header chip");

                // The chip reads "LOCATION Select location" until an
                // address is chosen and "LOCATION <address>" after.
                homePage.openLocationSelector();

                chooseSavedAddressFromSheet(addressName);
            }
        }

        // ---------- 4. Home ----------

        step("Home screen");
        homePage.waitForScreen();

        Assert.assertTrue(homeValidator.isLocationSelected(addressName),
                "Home header does not show \"" + addressName + "\" after selecting it");

        Assert.assertTrue(homeValidator.isSearchBarDisplayed(),
                "Search bar is not displayed on home");

        Assert.assertTrue(homeValidator.areAllTabsDisplayed(),
                "Bottom tab bar is incomplete on home");

        // ---------- 5. Store ----------

        // Whichever store the feed leads with. Nothing here is pinned to a
        // brand: a journey that names one fails the day it stops being
        // merchandised, which says nothing about the app.
        step("Open a store from the feed");

        List<String> storeCards = homePage.listVisibleStoreCards();

        Assert.assertFalse(storeCards.isEmpty(),
                "No store cards on the home feed");

        // Any store, picked fresh each run: pinning one brand would only
        // ever exercise that brand's catalogue. Overridable with
        // -DstoreName for when a particular store's bag is the thing under
        // investigation.
        String storeCard = pinnedStoreCard(storeCards).orElseGet(() -> anyOf(storeCards));

        homePage.openStoreCard(storeCard);

        String brand = storePage.waitForScreen().getBrandName();

        step("Store: " + brand + "  (card read: " + storeCard + ")");

        // ---------- 6. Product ----------

        // Tiles read "<product> <brand> $<price>", and the catalogue runs
        // on single units — so walk the grid until a product turns out to
        // have a size in stock.
        List<String> tiles = storePage.listVisibleProductTiles();

        Assert.assertFalse(tiles.isEmpty(),
                "No product tiles on the " + brand + " grid");

        // Shuffled so runs do not keep buying the same thing — and so the
        // suite covers a different corner of the catalogue each time.
        List<String> shuffledTiles = new ArrayList<>(tiles);
        Collections.shuffle(shuffledTiles);

        String productName = null;
        String chosenSize = null;
        String color = null;
        // Kept for the record of what this run bought; the bag line is
        // checked against the candidate price inside the loop.
        BigDecimal listedPrice = null;

        // Everything binned along the way, for the record at the end.
        List<String> clearedFromBag = new ArrayList<>();

        CheckoutFlow checkout = new CheckoutFlow(this::step);

        // Tiles are taken in order across attempts, never restarted: a
        // product the bag has just rejected as out of stock is not worth
        // opening a second time.
        int nextTile = 0;

        while (productName == null) {

            String candidate = null;
            String candidateSize = null;
            String candidateTilePrice = null;
            BigDecimal candidatePrice = null;

            while (nextTile < shuffledTiles.size()) {

                String tile = shuffledTiles.get(nextTile++);
                String name = productNameFrom(tile, brand);

                step("Open product: " + tile);
                storePage.openProductTile(tile);
                productPage.waitForScreen();

                // Accessories carry no size card at all; the flow keeps
                // its assertions uniform by sticking to products that do.
                if (!productValidator.isSizeCardDisplayed()) {

                    step("\"" + name + "\" has no size to choose — trying the next tile");
                    productPage.goBack();
                    storePage.waitForScreen();

                    continue;
                }

                if (!productValidator.isSizeCardEnabled()) {

                    // Down to a single size: the app fixes it and disables
                    // the card, so there is no sheet to open. An empty card
                    // here means nothing is left at all.
                    candidateSize = productPage.getSelectedSize();

                    if (candidateSize.isEmpty()) {

                        step("\"" + name + "\" is out of stock — trying the next tile");
                        productPage.goBack();
                        storePage.waitForScreen();

                        continue;
                    }

                    step("Only one size left — the app has fixed it at " + candidateSize);

                } else {

                    List<String> sizes = productPage.openSizeSheet().getAvailableSizes();

                    if (sizes.isEmpty()) {

                        step("\"" + name + "\" has no size in stock — trying the next tile");
                        productPage.closeSizeSheet().goBack();
                        storePage.waitForScreen();

                        continue;
                    }

                    candidateSize = sizes.contains(size) ? size : sizes.get(0);

                    step("Sizes on offer " + sizes + " — taking " + candidateSize);
                    productPage.chooseSize(candidateSize);
                }

                candidate = name;
                candidateTilePrice = priceFromTile(tile);
                candidatePrice = productPage.getPrice();

                break;
            }

            Assert.assertNotNull(candidate,
                    "Not one product on the " + brand + " grid could be bought"
                            + " — every tile was out of stock or sizeless");

            Assert.assertTrue(productValidator.isProductTitleDisplayed(candidate),
                    "Product detail page is not showing \"" + candidate + "\"");

            Assert.assertTrue(productValidator.isBrandDisplayed(brand),
                    "Product detail page is not showing the brand \"" + brand + "\"");

            assertSameAmount(candidatePrice, CurrencyUtils.parse(candidateTilePrice),
                    "Product page price does not match the price on its grid tile");

            Assert.assertTrue(productPage.getSizeCardText().contains(candidateSize),
                    "Size card still reads \"" + productPage.getSizeCardText()
                            + "\" after selecting " + candidateSize);

            String candidateColor = colorFrom(productPage.getColorCardText());

            step("Add to bag (" + candidate + ", size " + candidateSize
                    + ", " + candidateColor + ", " + candidatePrice + ")");
            boolean added = productPage.addToBagIfNeeded();

            step(added
                    ? "Item added to the bag"
                    : "That variant was already in the bag — carrying on with it");

            Assert.assertTrue(productValidator.isViewBagDisplayed(),
                    "No VIEW BAG pill after adding to bag — the item was not added");

            productPage.clickViewBag();

            // ---------- 7. Bag ----------

            step("Shopping bag");
            bagPage.waitForScreen();

            // The screen's header arrives before its rows do, so wait for
            // the row just added before reading anything off the group.
            // Its own row is the one thing certain to be here, out of
            // stock or not.
            bagPage.waitForItem(candidate, candidateSize);

            Assert.assertTrue(bagValidator.isStoreGroupDisplayed(brand),
                    "Bag is missing the " + brand + " group");

            // The variant's own row, not "is this size on screen anywhere":
            // the latter reads a standalone "Size: X" line, and a one-size
            // product has none for it to find — the bag prints
            // "Check Crossbody Bag $1,150.00 1" and nothing about size.
            Assert.assertTrue(bagPage.hasItem(candidate, candidateSize),
                    "Bag has no row for \"" + candidate + "\" in " + candidateSize);

            // The item's own line, not the bag subtotal: everything bought
            // from one store shares that store's bag, so the subtotal
            // covers whatever else is already in the group.
            //
            // Looked up by size as well as name for the same reason — this
            // catalogue has two "Check Cotton Poplin Shirt" variants, and
            // once both are in the Burberry group the name alone no longer
            // picks out a row.
            assertSameAmount(bagPage.getItemPrice(candidate, candidateSize), candidatePrice,
                    "Bag line price does not match the price on the product page");

            step("Bag subtotal reads " + bagPage.getCheckoutAmount());

            // Nothing is asked about stock before this point, because there
            // is nothing to ask: the app settles stock when Checkout is
            // tapped and marks an unsellable line only once it has refused
            // to leave. Watching the bag beforehand finds a clean one every
            // time, however long it is watched.
            //
            // Shared with the wishlist journey. The outcome comes back
            // rather than being asserted on, because this journey can go
            // back and choose a different product when its own item is the
            // one the store cannot sell.
            if (checkout.tryOpenCheckoutFromBag(candidate, candidateSize, clearedFromBag)
                    == CheckoutFlow.BagOutcome.OURS_UNSELLABLE) {

                step("\"" + candidate + "\" (" + candidateSize + ") is what the store"
                        + " could not sell — taking another product instead");

                bagPage.closeBag();
                returnToStoreGrid(brand);

                continue;
            }

            productName = candidate;
            chosenSize = candidateSize;
            color = candidateColor;
            listedPrice = candidatePrice;
        }

        if (!clearedFromBag.isEmpty()) {

            step("Deleted " + clearedFromBag.size()
                    + " unsellable line(s) from the bag over this run: " + clearedFromBag);
        }

        // ---------- 8. Checkout, payment and confirmation ----------

        // Shared with the wishlist journey rather than copied — every
        // awkward case in there came from a failing run, and a second copy
        // would drift from this one.
        CheckoutFlow.Placed placed = checkout.placeOrder(addressName);

        checkout.payAndVerifyOrder(placed, cardLast4, brand, productName, chosenSize, color);
    }

    /**
     * Which screen the app came up on.
     */
    private enum FirstScreen {

        /** Signed out: onboarding, with Login on it. */
        ONBOARDING,

        /** Already signed in: straight to home. */
        HOME
    }

    /**
     * Waits for the app to settle on one of its two opening screens and
     * says which.
     *
     * Both are checked without waiting, so whichever paints first wins
     * and neither is mistaken for the other. The point is the deadline:
     * "no Login button yet" and "already signed in" look identical for
     * the first few seconds of a cold start, and only time tells them
     * apart. Failing here if neither arrives is also far more use than
     * guessing and failing later somewhere unrelated.
     */
    private FirstScreen awaitFirstScreen() {

        try {

            return WaitUtils.waitUntil(driver ->
                            onboardingValidator.isLoginButtonDisplayedNow()
                                    ? FirstScreen.ONBOARDING
                                    : homeValidator.isHomeTabDisplayedNow()
                                            ? FirstScreen.HOME
                                            : null,
                    APP_FIRST_SCREEN_WITHIN);

        } catch (TimeoutException neitherAppeared) {

            throw new AssertionError(
                    "The app settled on neither onboarding nor home within "
                            + APP_FIRST_SCREEN_WITHIN.toSeconds() + "s of launch",
                    neitherAppeared);
        }
    }

    /**
     * How the app is offering to take a delivery address.
     */
    private enum LocationEntry {

        /** The app's own Enable Location sheet is up. */
        PROMPT,

        /** No sheet — the home header's location chip is the way in. */
        HEADER_CHIP
    }

    /**
     * Waits for whichever route into the address sheet the app offers.
     *
     * The sheet, when it comes, animates in over the home screen; when it
     * does not come, the home screen is simply there. So the home tab
     * being up is not on its own proof that no sheet is coming — hence
     * both being watched together, with the sheet winning any tie.
     *
     * Falls back to the chip rather than failing: that is what the old
     * code concluded on running out of time, and it is recoverable.
     */
    private LocationEntry awaitLocationEntry() {

        try {

            return WaitUtils.waitUntil(driver ->
                            LocationServicesPrompt.isShowingNow()
                                    ? LocationEntry.PROMPT
                                    : homeValidator.isHomeTabDisplayedNow()
                                            ? LocationEntry.HEADER_CHIP
                                            : null,
                    LOCATION_ENTRY_WITHIN);

        } catch (TimeoutException neitherAppeared) {

            return LocationEntry.HEADER_CHIP;
        }
    }

    /**
     * Picks the saved address out of the SELECT LOCATION sheet.
     *
     * Reached two ways — the app's own Enable Location sheet, and the home
     * header's chip — and identical from here on, so both share this.
     */
    private void chooseSavedAddressFromSheet(String addressName) {

        // Split so the log separates the sheet arriving from the wait for
        // it to stop moving — the first is the app's time, the second is
        // ours, and one number covered both.
        locationSheet.waitUntilVisible();
        step("Address sheet on screen");

        locationSheet.waitForScreen();
        step("Address sheet settled");

        Assert.assertTrue(locationValidator.isSavedAddressDisplayed(addressName),
                "Saved address \"" + addressName + "\" is not listed in the location sheet");

        step("Saved address listed");

        locationSheet.chooseSavedAddress(addressName);
    }

    /**
     * Gets back to the store grid after leaving the bag.
     *
     * The bag opens over the product page, so closing it normally lands
     * there and one step back reaches the grid. Checked rather than
     * assumed: if the close already returned to the store, going back
     * again would leave the store altogether.
     */
    private void returnToStoreGrid(String brand) {

        if (storeValidator.isDisplayed(brand)) {

            storePage.waitForScreen();

            return;
        }

        productPage.goBack();
        storePage.waitForScreen();
    }

    /**
     * BigDecimal.equals() is scale sensitive, and this app mixes scales —
     * the product page prints "$750" where the bag prints "$750.00".
     * Compare by value.
     */
    private void assertSameAmount(BigDecimal actual, BigDecimal expected, String message) {

        Assert.assertTrue(CurrencyUtils.equalAmounts(actual, expected),
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
     * The price printed on a tile.
     *
     * Read from the tile that was actually opened, never looked up by
     * product name afterwards: this catalogue lists two "Check Cotton
     * Poplin Shirt" tiles at $750 and $680, so a name lookup can hand back
     * the wrong one.
     */
    private String priceFromTile(String tileName) {

        return tileName.substring(tileName.lastIndexOf('$'));
    }

    /**
     * Reads the colour off the colour line, which renders as
     * "Color · Grain Beige".
     */
    private String colorFrom(String colorCardText) {

        return colorCardText.replaceFirst("^Color\\s*·?\\s*", "").trim();
    }

    /**
     * The store named by -DstoreName, when a run asked for one.
     *
     * The feed pick is random by design and should stay that way — it is
     * what gives the suite a different corner of the catalogue each run.
     * But a random pick is no use when one particular store's bag is the
     * thing being investigated, and only one of them holds the state that
     * matters. This makes a run repeatable without making the default
     * permanently narrow.
     */
    private Optional<String> pinnedStoreCard(List<String> storeCards) {

        String wanted = System.getProperty("storeName", "").trim();

        if (wanted.isEmpty()) {

            return Optional.empty();
        }

        Optional<String> pinned = storeCards.stream()
                .filter(card -> card.contains(wanted))
                .findFirst();

        Assert.assertTrue(pinned.isPresent(),
                "-DstoreName=\"" + wanted + "\" is not on the feed, which offers: "
                        + storeCards);

        step("Store pinned by -DstoreName to \"" + wanted + "\"");

        return pinned;
    }

    /**
     * Picks one entry at random, so a run is not tied to whatever the feed
     * happens to be leading with.
     */
    private String anyOf(List<String> options) {

        return options.get(ThreadLocalRandom.current().nextInt(options.size()));
    }

    /**
     * Prints the step with the wall-clock cost of the one before it.
     *
     * A journey this long is only tunable if it says where the time goes:
     * "+25.0s" against a step is what tells you a wait is being paid in
     * full rather than being satisfied early.
     */
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

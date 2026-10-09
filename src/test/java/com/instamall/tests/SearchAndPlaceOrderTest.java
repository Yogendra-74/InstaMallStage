package com.instamall.tests;

import com.instamall.base.BaseTest;
import com.instamall.components.location.LocationServicesPrompt;
import com.instamall.config.ConfigReader;
import com.instamall.driver.AppSession;
import com.instamall.flows.CheckoutFlow;
import com.instamall.pages.bag.ShoppingBagPage;
import com.instamall.pages.bag.ShoppingBagPageValidator;
import com.instamall.pages.home.HomePage;
import com.instamall.pages.home.HomePageValidator;
import com.instamall.pages.location.SelectLocationSheet;
import com.instamall.pages.location.SelectLocationSheetValidator;
import com.instamall.pages.login.EmailLoginPage;
import com.instamall.pages.login.LoginPage;
import com.instamall.pages.onboarding.OnboardingPage;
import com.instamall.pages.onboarding.OnboardingPageValidator;
import com.instamall.pages.product.ProductDetailPage;
import com.instamall.pages.product.ProductDetailPageValidator;
import com.instamall.pages.search.SearchPage;
import com.instamall.pages.store.StorePage;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.TimeoutException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Purchase journey via the Search screen, on staging:
 *
 *   email login -> delivery location -> Home search -> Stores tab
 *   -> store -> product -> bag -> checkout -> payment -> order
 *   confirmation
 *
 * Distinct from EndToEndOrderTest, which opens a store directly off a
 * Home feed card. This one exercises the Search screen instead — the
 * route confirmed live to be the reliable way to reach a specific store
 * by name. Two other routes were tried and found NOT to work for that:
 *
 *   - Tapping a mall card (Home's EXPLORE MALLS section, or the dedicated
 *     Malls list behind its "See all") opens an "Explore the mall" page
 *     that is not a working per-mall store directory — it shows the same
 *     static "Your Local Edit" card regardless of which mall was tapped.
 *   - The Search screen opens on its PRODUCTS tab by default; searching a
 *     store name there returns "No pieces found". The STORES tab has to
 *     be selected first.
 *
 * KNOWN GAP: a checkout can fail with an "Error — Product not available
 * for product &lt;Name&gt;" toast on the Delivery &amp; checkout screen
 * itself, after Place Order is tapped and the payment sheet would
 * otherwise open. This was confirmed for two different products at one
 * store (Ema Savahl Couture, Aventura Mall) and is suspected to be a
 * store-level outage there rather than per-SKU stock. CheckoutFlow does
 * not watch for this toast — it only races the payment sheet against the
 * DeliveryUnavailablePrompt — so a run against a store in that state
 * fails with an assertion about the payment sheet never appearing rather
 * than a clear message about the toast. searchStoreName is pinned to
 * Moncler, confirmed to complete this journey end to end, to avoid it.
 *
 * NOTE: this pays with the staging test card, so every run creates a real
 * staging order.
 */
public class SearchAndPlaceOrderTest extends BaseTest {

    private final OnboardingPage onboardingPage = new OnboardingPage();
    private final OnboardingPageValidator onboardingValidator = new OnboardingPageValidator();
    private final LoginPage loginPage = new LoginPage();
    private final EmailLoginPage emailLoginPage = new EmailLoginPage();
    private final HomePage homePage = new HomePage();
    private final HomePageValidator homeValidator = new HomePageValidator();
    private final SelectLocationSheet locationSheet = new SelectLocationSheet();
    private final SelectLocationSheetValidator locationValidator = new SelectLocationSheetValidator();
    private final SearchPage searchPage = new SearchPage();
    private final StorePage storePage = new StorePage();
    private final ProductDetailPage productPage = new ProductDetailPage();
    private final ProductDetailPageValidator productValidator = new ProductDetailPageValidator();
    private final ShoppingBagPage bagPage = new ShoppingBagPage();
    private final ShoppingBagPageValidator bagValidator = new ShoppingBagPageValidator();

    private static final double MIAMI_LATITUDE = 25.7995;
    private static final double MIAMI_LONGITUDE = -80.1998;

    private static final Duration LOCATION_ENTRY_WITHIN = Duration.ofSeconds(15);

    private long firstStepAt;
    private long previousStepAt;

    @BeforeClass(alwaysRun = true)
    public void prepareCleanSignedOutApp() {

        AppSession.clearAppData();
        AppSession.grantLocationAndNotifications();
        AppSession.setSimulatedLocation(MIAMI_LATITUDE, MIAMI_LONGITUDE);
    }

    @Test(description = "Find a store via Search, buy one product from it, and check out")
    public void placeOrderViaSearch() {

        String addressName = ConfigReader.get("deliveryAddressName");
        String storeName = ConfigReader.get("searchStoreName");
        String preferredSize = ConfigReader.get("productSize");
        String cardLast4 = ConfigReader.get("savedCardLast4");

        step("Waiting for the app's first screen");
        signIn();

        step("Select delivery location: " + addressName);
        chooseDeliveryLocation(addressName);

        // ---------- Search -> Stores tab -> store ----------

        step("Open search");
        homePage.openSearch();
        searchPage.waitForScreen();

        step("Search for \"" + storeName + "\"");
        searchPage.search(storeName)
                  .openStoresTab();

        step("Open the store result");
        searchPage.openStoreResultContaining(storeName);

        String brand = storePage.waitForScreen().getBrandName();
        step("Store: " + brand);

        // ---------- Product ----------

        List<String> tiles = storePage.listVisibleProductTiles();

        Assert.assertFalse(tiles.isEmpty(), "No product tiles on the " + brand + " grid");

        List<String> shuffledTiles = new ArrayList<>(tiles);
        Collections.shuffle(shuffledTiles);

        String productName = null;
        String chosenSize = null;

        for (String tile : shuffledTiles) {

            String candidate = productNameFrom(tile, brand);

            step("Open product: " + tile);
            storePage.openProductTile(tile);
            productPage.waitForScreen();

            if (!productValidator.isSizeCardDisplayed()) {

                step("\"" + candidate + "\" has no size to choose — trying the next tile");
                productPage.goBack();
                storePage.waitForScreen();

                continue;
            }

            String size;

            if (!productValidator.isSizeCardEnabled()) {

                size = productPage.getSelectedSize();

                if (size.isEmpty()) {

                    step("\"" + candidate + "\" is out of stock — trying the next tile");
                    productPage.goBack();
                    storePage.waitForScreen();

                    continue;
                }

                step("Only one size left — the app has fixed it at " + size);

            } else {

                List<String> sizes = productPage.openSizeSheet().getAvailableSizes();

                if (sizes.isEmpty()) {

                    step("\"" + candidate + "\" has no size in stock — trying the next tile");
                    productPage.closeSizeSheet().goBack();
                    storePage.waitForScreen();

                    continue;
                }

                size = sizes.contains(preferredSize) ? preferredSize : sizes.get(0);

                step("Sizes on offer " + sizes + " — taking " + size);
                productPage.chooseSize(size);
            }

            productName = candidate;
            chosenSize = size;
            break;
        }

        Assert.assertNotNull(productName,
                "Not one product on the " + brand + " grid could be bought — every"
                        + " tile was out of stock or sizeless");

        Assert.assertTrue(productValidator.isProductTitleDisplayed(productName),
                "Product detail page is not showing \"" + productName + "\"");

        step("Add to bag (" + productName + ", size " + chosenSize + ")");
        boolean added = productPage.addToBagIfNeeded();

        step(added
                ? "Item added to the bag"
                : "That variant was already in the bag — carrying on with it");

        Assert.assertTrue(productValidator.isViewBagDisplayed(),
                "No View bag pill after adding to bag — the item was not added");

        productPage.clickViewBag();

        // ---------- Bag ----------

        step("Shopping bag");
        bagPage.waitForScreen();
        bagPage.waitForItem(productName, chosenSize);

        Assert.assertTrue(bagValidator.isStoreGroupDisplayed(brand),
                "Bag is missing the " + brand + " group");

        Assert.assertTrue(bagPage.hasItem(productName, chosenSize),
                "Bag has no row for \"" + productName + "\" in " + chosenSize);

        step("Bag subtotal reads " + bagPage.getCheckoutAmount());

        // ---------- Checkout, payment and confirmation ----------

        // The same checkout every purchase journey in this suite uses —
        // see CheckoutFlow's own doc comment for why it is shared rather
        // than copied.
        CheckoutFlow checkout = new CheckoutFlow(this::step);

        checkout.openCheckoutFromBag(productName, chosenSize);

        CheckoutFlow.Placed placed = checkout.placeOrder(addressName);

        checkout.payAndVerifyOrder(placed, cardLast4, brand, productName, chosenSize, null);
    }

    /**
     * Pulls the product name out of a tile name, which reads
     * "&lt;product&gt; &lt;brand&gt; $&lt;price&gt;".
     */
    private String productNameFrom(String tileName, String brand) {

        int brandAt = tileName.lastIndexOf(" " + brand + " $");

        return brandAt < 0 ? tileName : tileName.substring(0, brandAt);
    }

    // ---------- prelude, shared in shape with the other journeys ----------

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

        Assert.assertTrue(locationValidator.isSavedAddressDisplayed(addressName),
                "Saved address \"" + addressName + "\" is not listed in the location sheet");

        locationSheet.chooseSavedAddress(addressName);

        homePage.waitForScreen();

        Assert.assertTrue(homeValidator.isLocationSelected(addressName),
                "Home header does not show \"" + addressName + "\" after selecting it");
    }

    /**
     * How the app is offering to take a delivery address.
     */
    private enum LocationEntry {

        /** The app's own Enable Location sheet is up. */
        PROMPT,

        /** No sheet — the home feed is showing. */
        HOME
    }

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

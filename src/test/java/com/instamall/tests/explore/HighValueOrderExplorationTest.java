package com.instamall.tests.explore;

import com.instamall.base.BaseTest;
import com.instamall.config.ConfigReader;
import com.instamall.flows.CheckoutFlow;
import com.instamall.pages.bag.ShoppingBagPage;
import com.instamall.pages.home.HomePage;
import com.instamall.pages.login.EmailLoginPage;
import com.instamall.pages.login.LoginPage;
import com.instamall.pages.onboarding.OnboardingPage;
import com.instamall.pages.onboarding.OnboardingPageValidator;
import com.instamall.pages.product.ProductDetailPage;
import com.instamall.pages.product.ProductDetailPageValidator;
import com.instamall.pages.store.StorePage;
import com.instamall.utils.CurrencyUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.List;

/**
 * TEMPORARY — not part of the suite. Places one order guaranteed to clear
 * the $500 Customer PIN threshold, so the PIN screen can be mapped for
 * Model 2. Reuses CheckoutFlow rather than duplicating checkout/payment.
 */
public class HighValueOrderExplorationTest extends BaseTest {

    private static final BigDecimal PIN_THRESHOLD = new BigDecimal("500");

    private final OnboardingPage onboardingPage = new OnboardingPage();
    private final OnboardingPageValidator onboardingValidator = new OnboardingPageValidator();
    private final LoginPage loginPage = new LoginPage();
    private final EmailLoginPage emailLoginPage = new EmailLoginPage();
    private final HomePage homePage = new HomePage();
    private final StorePage storePage = new StorePage();
    private final ProductDetailPage productPage = new ProductDetailPage();
    private final ProductDetailPageValidator productValidator = new ProductDetailPageValidator();
    private final ShoppingBagPage bagPage = new ShoppingBagPage();

    @Test
    public void placeOrderOverPinThreshold() {

        String email = ConfigReader.get("testEmail");
        String password = ConfigReader.get("testPassword");
        String addressName = ConfigReader.get("deliveryAddressName");
        String cardLast4 = ConfigReader.get("savedCardLast4");
        String size = ConfigReader.get("productSize");

        if (onboardingValidator.isLoginButtonDisplayed()) {

            onboardingPage.clickLogin();
            loginPage.waitForScreen().clickContinueWithEmail();
            emailLoginPage.waitForScreen()
                          .enterEmail(email)
                          .enterPassword(password)
                          .clickNext();
        }

        homePage.waitForScreen();

        List<String> storeCards = homePage.listVisibleStoreCards();
        String storeCard = storeCards.stream()
                .filter(card -> card.contains("Burberry"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Burberry not on the feed"));

        homePage.openStoreCard(storeCard);
        String brand = storePage.waitForScreen().getBrandName();

        List<String> tiles = storePage.listVisibleProductTiles();

        String chosenName = null;
        String chosenSize = null;
        String chosenColor = null;
        BigDecimal chosenPrice = null;

        for (String tile : tiles) {

            BigDecimal tilePrice = CurrencyUtils.parse(tile.substring(tile.lastIndexOf('$')));

            if (tilePrice.compareTo(PIN_THRESHOLD) <= 0) {

                continue;
            }

            int brandAt = tile.lastIndexOf(" " + brand + " $");
            String name = brandAt < 0 ? tile : tile.substring(0, brandAt);

            storePage.openProductTile(tile);
            productPage.waitForScreen();

            if (!productValidator.isSizeCardDisplayed()) {

                productPage.goBack();
                storePage.waitForScreen();
                continue;
            }

            String pickedSize = productValidator.isSizeCardEnabled()
                    ? productPage.selectSizePreferring(size)
                    : productPage.getSelectedSize();

            if (pickedSize.isEmpty()) {

                productPage.goBack();
                storePage.waitForScreen();
                continue;
            }

            chosenName = name;
            chosenSize = pickedSize;
            chosenColor = productPage.getColorCardText()
                    .replaceFirst("^Color\\s*\\u00b7?\\s*", "").trim();
            chosenPrice = productPage.getPrice();
            break;
        }

        Assert.assertNotNull(chosenName,
                "No tile over $" + PIN_THRESHOLD + " on the " + brand + " grid could be bought");

        System.out.println(">>> Chosen: " + chosenName + " (" + chosenSize + ", "
                + chosenColor + ") at " + chosenPrice);

        productPage.addToBagIfNeeded();
        productPage.clickViewBag();
        bagPage.waitForScreen();
        bagPage.waitForItem(chosenName, chosenSize);

        CheckoutFlow checkout = new CheckoutFlow(msg -> System.out.println(">>> " + msg));

        checkout.tryOpenCheckoutFromBag(chosenName, chosenSize, new java.util.ArrayList<>());

        CheckoutFlow.Placed placed = checkout.placeOrder(addressName);

        checkout.payAndVerifyOrder(placed, cardLast4, brand, chosenName, chosenSize, chosenColor);
    }

}

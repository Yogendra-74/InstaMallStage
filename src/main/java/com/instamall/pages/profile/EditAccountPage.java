package com.instamall.pages.profile;

import com.instamall.base.BasePage;
import com.instamall.driver.DriverManager;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class EditAccountPage extends BasePage {

    public EditAccountPage waitForScreen() {

        waitForVisibility(EditAccountPageLocators.TITLE);

        // Presence, not visibility: WDA's visible flag is unreliable for
        // nested RN inputs (same caveat as the email login screen).
        WaitUtils.waitForPresence(EditAccountPageLocators.FIRST_NAME_FIELD);
        WaitUtils.waitForPresence(EditAccountPageLocators.EMAIL_FIELD);

        return this;
    }

    // ---------- Reading the form ----------

    public String getFirstName() {

        return fieldValue(EditAccountPageLocators.FIRST_NAME_FIELD);
    }

    public String getLastName() {

        return fieldValue(EditAccountPageLocators.LAST_NAME_FIELD);
    }

    public String getPhone() {

        return fieldValue(EditAccountPageLocators.PHONE_FIELD);
    }

    public String getEmail() {

        return fieldValue(EditAccountPageLocators.EMAIL_FIELD);
    }

    // ---------- Filling the form ----------

    public EditAccountPage enterLastName(String lastName) {

        typeInto(EditAccountPageLocators.LAST_NAME_FIELD, lastName);

        return this;
    }

    public EditAccountPage enterPhone(String phone) {

        typeInto(EditAccountPageLocators.PHONE_FIELD, phone);

        return this;
    }

    public EditAccountPage enterEmail(String email) {

        typeInto(EditAccountPageLocators.EMAIL_FIELD, email);

        return this;
    }

    public EditAccountPage clearFirstName() {

        clearField(EditAccountPageLocators.FIRST_NAME_FIELD);

        return this;
    }

    /**
     * Taps SAVE, dismissing the keyboard first when it is in the way.
     *
     * Editing the upper fields (first/last name) leaves the keyboard over
     * the SAVE row and the tap times out — the first run of the suite
     * failed exactly there. The lower fields scroll the form up, which is
     * why the email and phone tests never saw it.
     */
    public EditAccountPage tapSave() {

        if (!WaitUtils.isVisibleNow(EditAccountPageLocators.SAVE_BUTTON)) {

            dismissKeyboard();
        }

        // SAVE sits at the very bottom of a form that is taller than the
        // screen, so it is dragged into view when the keyboard alone was
        // not what hid it.
        scrollIntoView(WaitUtils.waitForPresence(EditAccountPageLocators.SAVE_BUTTON));

        click(EditAccountPageLocators.SAVE_BUTTON);

        return this;
    }

    /**
     * Best-effort keyboard dismiss. Never throws — if the keyboard stays,
     * the SAVE click that follows reports it.
     */
    private void dismissKeyboard() {

        try {

            DriverManager.getDriver().hideKeyboard();

        } catch (Exception couldNotHide) {

            System.out.println(">>> hideKeyboard failed ("
                    + couldNotHide.getMessage() + ") — tapping the screen title instead.");

            try {

                DriverManager.getDriver()
                        .findElement(EditAccountPageLocators.TITLE)
                        .click();

            } catch (Exception ignored) {

                // Leave it to the SAVE click to fail loudly.
            }
        }
    }

    // ---------- Internals ----------

    private String fieldValue(By locator) {

        String value = WaitUtils.waitForPresence(locator).getAttribute("value");

        return value == null ? "" : value;
    }

    /**
     * Clears via select-all style deletion: RN inputs on this app ignore
     * WebElement#clear() when the field is not focused, so the field is
     * tapped first and then cleared, and the result is verified.
     */
    private void clearField(By locator) {

        WebElement field = WaitUtils.waitForPresence(locator);

        field.click();
        field.clear();

        String left = field.getAttribute("value");

        if (left != null && !left.isEmpty()) {

            // clear() can leave RN-formatted fields untouched — fall back
            // to deleting a character at a time.
            for (int i = left.length(); i > 0; i--) {

                field.sendKeys("\b");
            }
        }
    }

    private void typeInto(By locator, String text) {

        clearField(locator);

        WebElement field = WaitUtils.waitForPresence(locator);

        field.sendKeys(text);
    }

}

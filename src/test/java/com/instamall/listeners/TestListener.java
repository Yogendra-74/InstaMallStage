package com.instamall.listeners;

import com.instamall.constants.FrameworkConstants;
import com.instamall.driver.DriverManager;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.OutputType;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * On every test failure, captures a screenshot and the full page source
 * so failures can be debugged without re-running the test.
 */
public class TestListener implements ITestListener {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    @Override
    public void onTestStart(ITestResult result) {

        System.out.println(">>> STARTING: " + testName(result));
    }

    @Override
    public void onTestSuccess(ITestResult result) {

        System.out.println(">>> PASSED: " + testName(result));
    }

    @Override
    public void onTestFailure(ITestResult result) {

        System.out.println(">>> FAILED: " + testName(result));

        IOSDriver driver = DriverManager.getDriver();

        if (driver == null) {

            System.out.println(">>> Driver is null — skipping failure artifacts.");

            return;
        }

        String fileBaseName = testName(result) + "_" + LocalDateTime.now().format(TIMESTAMP);

        captureScreenshot(driver, fileBaseName);
        capturePageSource(driver, fileBaseName);
    }

    @Override
    public void onTestSkipped(ITestResult result) {

        System.out.println(">>> SKIPPED: " + testName(result));
    }

    private String testName(ITestResult result) {

        return result.getTestClass().getRealClass().getSimpleName()
                + "." + result.getMethod().getMethodName();
    }

    private void captureScreenshot(IOSDriver driver, String fileBaseName) {

        try {

            File source = driver.getScreenshotAs(OutputType.FILE);

            Path target = Paths.get(FrameworkConstants.SCREENSHOT_PATH,
                    fileBaseName + ".png");

            Files.createDirectories(target.getParent());
            Files.copy(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);

            System.out.println(">>> Screenshot saved: " + target.toAbsolutePath());

        } catch (IOException exception) {

            System.out.println(">>> Could not save screenshot: " + exception.getMessage());
        }
    }

    private void capturePageSource(IOSDriver driver, String fileBaseName) {

        try {

            Path target = Paths.get(FrameworkConstants.PAGE_SOURCE_PATH,
                    fileBaseName + ".xml");

            Files.createDirectories(target.getParent());
            Files.writeString(target, driver.getPageSource());

            System.out.println(">>> Page source saved: " + target.toAbsolutePath());

        } catch (IOException exception) {

            System.out.println(">>> Could not save page source: " + exception.getMessage());
        }
    }

}
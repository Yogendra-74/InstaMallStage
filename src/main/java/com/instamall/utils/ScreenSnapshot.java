package com.instamall.utils;

import com.instamall.driver.DriverManager;
import org.openqa.selenium.WebDriverException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * One read of the whole screen, taken through the driver's page source.
 *
 * For a page object that needs the name and position of many elements
 * at once — pairing a column of labels with a column of amounts, or
 * assembling a bag row out of the title, variant, price and buttons
 * that the list lays out as separate siblings. Done element by element
 * that is a WebDriver round trip per attribute per element, at roughly
 * a quarter of a second each on the simulator: confirmed in the Appium
 * log, the bag spent ~20s on about 80 such calls between "VIEW BAG" and
 * "Review delivery &amp; checkout", and the checkout another ~25s on
 * close to a hundred between that tap and "Get it Today". A screen that
 * re-renders between two of those calls also hands back a stale element
 * and the read fails outright.
 *
 * The page source is a single call and a single, self-consistent
 * snapshot: every element in it was read in the same instant, so
 * nothing can go stale between them, and it carries the same name,
 * type and frame attributes a predicate query would match on.
 *
 * Taken WITHOUT the "visible" and "accessible" attributes, on purpose.
 * Both are computed per element by WebDriverAgent as the tree is
 * serialised, and "accessible" in particular costs it a round trip into
 * the app for every single element — the Appium log prints "Fetching of
 * XC_kAXXCAttributeIsElement value for … took 0.07s" over and over —
 * so a plain page source of a large screen came out at 10-13s (60KB,
 * confirmed in the log), which is exactly the dead time the checkout
 * screen kept showing after this reader first moved to page source.
 * Excluded, the same read is a fraction of a second.
 *
 * What "accessible == 1" used to do for the pages — keep each text from
 * being counted twice, since the app nests every StaticText (and some
 * Others) inside an identically named, non-accessible wrapper — is done
 * structurally instead: an element whose child carries its own type and
 * name is that wrapper, and only the inner one is kept. Checked against
 * every saved page-source dump under test-output/page-sources: for
 * StaticTexts and Buttons this rule reproduces the accessible set
 * exactly.
 *
 * Nothing here answers "is it visible": a page that is about to tap
 * something still goes through a locator with visible == 1 for that one
 * element, which is cheap, rather than paying for the flag on all of
 * them.
 */
public final class ScreenSnapshot {

    public static final String STATIC_TEXT = "XCUIElementTypeStaticText";
    public static final String BUTTON = "XCUIElementTypeButton";

    // See the class doc comment for why these two, and why it matters.
    private static final String EXCLUDED_ATTRIBUTES = "visible,accessible";

    /**
     * One accessible element, as the snapshot saw it.
     */
    public static final class Item {

        public final String type;
        public final String name;

        /**
         * The human-readable text, where the name is something else.
         * Screens built around test ids put the id in the name
         * ("bag_item_<id>_variant_button") and the words a shopper reads
         * in the label ("Size: 44 Color: Chino Grey").
         */
        public final String label;

        public final int x;
        public final int y;
        public final int width;
        public final int height;

        private Item(String type, String name, String label,
                     int x, int y, int width, int height) {

            this.type = type;
            this.name = name;
            this.label = label;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public boolean isStaticText() {

            return STATIC_TEXT.equals(type);
        }

        public boolean isButton() {

            return BUTTON.equals(type);
        }

        public boolean named(String exact) {

            return name.equals(exact);
        }

        public boolean nameStartsWith(String prefix) {

            return name.startsWith(prefix);
        }

        public boolean nameContainsIgnoreCase(String part) {

            return name.toLowerCase().contains(part.toLowerCase());
        }

        /**
         * Within a few points of the same line, as the pages already
         * judge it.
         */
        public boolean onSameLineAs(Item other) {

            return Math.abs(y - other.y) < 5;
        }

        @Override
        public String toString() {

            return type.replace("XCUIElementType", "") + " \"" + name + "\" at (" + x + ", " + y + ")";
        }
    }

    private final List<Item> items;

    private ScreenSnapshot(List<Item> items) {

        this.items = Collections.unmodifiableList(items);
    }

    /**
     * Reads the screen as it is right now.
     */
    public static ScreenSnapshot capture() {

        Object source;

        try {

            source = DriverManager.getDriver().executeScript("mobile: source",
                    Map.of("format", "xml", "excludedAttributes", EXCLUDED_ATTRIBUTES));

        } catch (WebDriverException unsupported) {

            // An older driver without the option: the plain, slow read
            // still works, and the attributes it carries are simply
            // ignored below.
            source = DriverManager.getDriver().getPageSource();
        }

        return of(String.valueOf(source));
    }

    /**
     * The same read over page source already in hand — a dump saved
     * under test-output/page-sources, say, to check a page object's
     * reading of a screen without a simulator.
     */
    public static ScreenSnapshot of(String pageSource) {

        List<Item> items = new ArrayList<>();

        collect(parse(pageSource).getDocumentElement(), items);

        return new ScreenSnapshot(items);
    }

    /**
     * Every accessible element on the screen, in document order — which
     * for a vertical list is top to bottom.
     */
    public List<Item> items() {

        return items;
    }

    public List<Item> staticTexts() {

        return where(Item::isStaticText);
    }

    public List<Item> buttons() {

        return where(Item::isButton);
    }

    public List<Item> where(Predicate<Item> test) {

        List<Item> matching = new ArrayList<>();

        for (Item item : items) {

            if (test.test(item)) {

                matching.add(item);
            }
        }

        return matching;
    }

    public Optional<Item> first(Predicate<Item> test) {

        for (Item item : items) {

            if (test.test(item)) {

                return Optional.of(item);
            }
        }

        return Optional.empty();
    }

    public boolean has(Predicate<Item> test) {

        return first(test).isPresent();
    }

    // Walks the tree in document order rather than asking for one tag
    // name at a time, so that items() keeps the on-screen order across
    // element types — a row's title (a text) really does come before its
    // variant (a button).
    private static void collect(Node node, List<Item> into) {

        if (node.getNodeType() != Node.ELEMENT_NODE) {

            return;
        }

        Element element = (Element) node;

        if (!isWrapper(element)) {

            into.add(new Item(
                    element.getAttribute("type"),
                    element.getAttribute("name"),
                    element.getAttribute("label"),
                    integer(element.getAttribute("x")),
                    integer(element.getAttribute("y")),
                    integer(element.getAttribute("width")),
                    integer(element.getAttribute("height"))));
        }

        NodeList children = element.getChildNodes();

        for (int index = 0; index < children.getLength(); index++) {

            collect(children.item(index), into);
        }
    }

    /**
     * The non-accessible shell the app puts around a text: same type,
     * same name, one level up. Only the inner one counts.
     */
    private static boolean isWrapper(Element element) {

        NodeList children = element.getChildNodes();

        for (int index = 0; index < children.getLength(); index++) {

            Node child = children.item(index);

            if (child.getNodeType() == Node.ELEMENT_NODE
                    && element.getAttribute("type").equals(((Element) child).getAttribute("type"))
                    && element.getAttribute("name").equals(((Element) child).getAttribute("name"))) {

                return true;
            }
        }

        return false;
    }

    private static Document parse(String source) {

        try {

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            // The source is the app's own accessibility tree; it never
            // carries a DTD or an entity, so none are honoured.
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);
            factory.setXIncludeAware(false);
            factory.setNamespaceAware(false);

            DocumentBuilder builder = factory.newDocumentBuilder();

            return builder.parse(new InputSource(new StringReader(source)));

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Could not read the screen's page source", exception);
        }
    }

    /**
     * Frames come back as whole points ("723"), but WebDriverAgent has
     * been known to print a fraction ("318.7"); either reads.
     */
    private static int integer(String attribute) {

        if (attribute == null || attribute.isBlank()) {

            return 0;
        }

        return (int) Math.round(Double.parseDouble(attribute));
    }

}

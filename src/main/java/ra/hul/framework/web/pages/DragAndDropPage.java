package ra.hul.framework.web.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import ra.hul.framework.core.constants.Endpoints;

/**
 * NOTE: the-internet's drag-and-drop uses HTML5 drag/drop which is notoriously
 * buggy with Selenium's Actions API. We use a JavaScript-based workaround.
 * This is a great interview talking point.
 */
public class DragAndDropPage extends BasePage {

    private final By columnA = By.id("column-a");
    private final By columnB = By.id("column-b");
    private final By columnAHeader = By.cssSelector("#column-a header");
    private final By columnBHeader = By.cssSelector("#column-b header");

    private static final String JS_DND_HELPER =
            "function simulate(sourceNode, destinationNode) {" +
            "  var EVENT_TYPES = { DRAG_END: 'dragend', DRAG_START: 'dragstart', DROP: 'drop' };" +
            "  function createCustomEvent(type) {" +
            "    var event = new CustomEvent('Event'); event.initEvent(type, true, true);" +
            "    event.dataTransfer = { data: {}, setData: function(type, val) { this.data[type] = val; }," +
            "      getData: function(type) { return this.data[type]; } }; return event; }" +
            "  var startEvent = createCustomEvent(EVENT_TYPES.DRAG_START);" +
            "  sourceNode.dispatchEvent(startEvent);" +
            "  var dropEvent = createCustomEvent(EVENT_TYPES.DROP);" +
            "  dropEvent.dataTransfer = startEvent.dataTransfer;" +
            "  destinationNode.dispatchEvent(dropEvent);" +
            "  var endEvent = createCustomEvent(EVENT_TYPES.DRAG_END);" +
            "  endEvent.dataTransfer = startEvent.dataTransfer;" +
            "  sourceNode.dispatchEvent(endEvent);" +
            "}" +
            "simulate(arguments[0], arguments[1]);";

    @Step("Open drag and drop page")
    public DragAndDropPage open() {
        navigateToPath(Endpoints.DRAG_AND_DROP);
        return this;
    }

    @Step("Drag column A to column B")
    public DragAndDropPage dragAToB() {
        executeJs(JS_DND_HELPER,
                getDriver().findElement(columnA),
                getDriver().findElement(columnB));
        return this;
    }

    @Step("Drag column B to column A")
    public DragAndDropPage dragBToA() {
        executeJs(JS_DND_HELPER,
                getDriver().findElement(columnB),
                getDriver().findElement(columnA));
        return this;
    }

    public String getColumnAHeader() {
        return getText(columnAHeader);
    }

    public String getColumnBHeader() {
        return getText(columnBHeader);
    }

    @Override
    public boolean isLoaded() {
        return isDisplayed(columnA) && isDisplayed(columnB);
    }
}

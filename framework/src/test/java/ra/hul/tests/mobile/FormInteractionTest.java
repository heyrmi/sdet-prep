package ra.hul.tests.mobile;

import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import ra.hul.framework.mobile.screens.FormScreen;
import ra.hul.framework.mobile.screens.HomeScreen;
import ra.hul.tests.base.BaseMobileTest;

@Epic("Mobile Automation")
@Feature("Form Interactions")
public class FormInteractionTest extends BaseMobileTest {

    @Test(groups = {"regression"},
          description = "Type text in input field and verify result")
    @Severity(SeverityLevel.NORMAL)
    @Story("Text Input")
    public void formInput_typeText_shouldShowResult() {
        HomeScreen home = new HomeScreen();
        FormScreen formScreen = home.navigateToForms();

        formScreen.typeInInput("Hello Appium");
        String result = formScreen.getInputResult();
        Assert.assertEquals(result, "Hello Appium");
    }

    @Test(groups = {"regression"},
          description = "Toggle switch and verify state change")
    @Severity(SeverityLevel.NORMAL)
    @Story("Switch Toggle")
    public void formSwitch_toggle_shouldChangeState() {
        HomeScreen home = new HomeScreen();
        FormScreen formScreen = home.navigateToForms();

        String initialText = formScreen.getSwitchText();
        formScreen.toggleSwitch();
        String toggledText = formScreen.getSwitchText();
        Assert.assertNotEquals(initialText, toggledText,
                "Switch text should change after toggle");
    }
}

package ra.hul.tests.web;

import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import ra.hul.framework.web.pages.DropdownPage;
import ra.hul.tests.base.BaseWebTest;

@Epic("Web Automation")
@Feature("Dropdown")
public class DropdownTest extends BaseWebTest {

    @Test(groups = {"smoke", "regression"},
          description = "Select Option 1 from dropdown")
    @Severity(SeverityLevel.NORMAL)
    @Story("Dropdown Selection")
    public void dropdown_selectOption1_shouldBeSelected() {
        DropdownPage page = new DropdownPage().open();
        page.selectOption("Option 1");
        Assert.assertEquals(page.getSelectedOption(), "Option 1");
    }

    @Test(groups = {"regression"},
          description = "Select Option 2 from dropdown")
    @Severity(SeverityLevel.NORMAL)
    @Story("Dropdown Selection")
    public void dropdown_selectOption2_shouldBeSelected() {
        DropdownPage page = new DropdownPage().open();
        page.selectOption("Option 2");
        Assert.assertEquals(page.getSelectedOption(), "Option 2");
    }

    @Test(groups = {"regression"},
          description = "Select by value attribute")
    @Severity(SeverityLevel.MINOR)
    @Story("Dropdown Selection")
    public void dropdown_selectByValue_shouldWork() {
        DropdownPage page = new DropdownPage().open();
        page.selectByValue("1");
        Assert.assertEquals(page.getSelectedOption(), "Option 1");
    }
}

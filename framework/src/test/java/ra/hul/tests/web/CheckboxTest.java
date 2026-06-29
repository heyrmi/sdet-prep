package ra.hul.tests.web;

import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import ra.hul.framework.web.pages.CheckboxPage;
import ra.hul.tests.base.BaseWebTest;

@Epic("Web Automation")
@Feature("Checkboxes")
public class CheckboxTest extends BaseWebTest {

    @Test(groups = {"smoke", "regression"},
          description = "Verify checkboxes page loads with 2 checkboxes")
    @Severity(SeverityLevel.NORMAL)
    @Story("Checkbox Display")
    public void checkboxes_pageLoad_shouldShowTwoCheckboxes() {
        CheckboxPage page = new CheckboxPage().open();
        Assert.assertEquals(page.getCheckboxCount(), 2);
    }

    @Test(groups = {"regression"},
          description = "Toggle first checkbox and verify state change")
    @Severity(SeverityLevel.NORMAL)
    @Story("Checkbox Interaction")
    public void checkbox_toggleFirst_shouldChangeState() {
        CheckboxPage page = new CheckboxPage().open();
        boolean initialState = page.isChecked(0);
        page.toggleCheckbox(0);
        Assert.assertNotEquals(page.isChecked(0), initialState);
    }

    @Test(groups = {"regression"},
          description = "Toggle second checkbox and verify state change")
    @Severity(SeverityLevel.NORMAL)
    @Story("Checkbox Interaction")
    public void checkbox_toggleSecond_shouldChangeState() {
        CheckboxPage page = new CheckboxPage().open();
        boolean initialState = page.isChecked(1);
        page.toggleCheckbox(1);
        Assert.assertNotEquals(page.isChecked(1), initialState);
    }
}

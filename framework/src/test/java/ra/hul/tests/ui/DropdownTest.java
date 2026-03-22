package ra.hul.tests.ui;

import org.testng.Assert;
import org.testng.annotations.Test;
import ra.hul.framework.pages.DropdownPage;
import ra.hul.tests.base.BaseTest;

/**
 * UI tests for the-internet.herokuapp.com/dropdown
 */
public class DropdownTest extends BaseTest {

    @Test(description = "Verify selecting Option 1 from dropdown")
    public void dropdown_selectOption1_shouldBeSelected() {
        DropdownPage page = new DropdownPage();
        page.open().selectOption("Option 1");

        Assert.assertEquals(page.getSelectedOption(), "Option 1",
                "Selected option should be Option 1");
    }

    @Test(description = "Verify selecting Option 2 from dropdown")
    public void dropdown_selectOption2_shouldBeSelected() {
        DropdownPage page = new DropdownPage();
        page.open().selectOption("Option 2");

        Assert.assertEquals(page.getSelectedOption(), "Option 2",
                "Selected option should be Option 2");
    }

    @Test(description = "Verify changing selection from Option 1 to Option 2")
    public void dropdown_changeSelection_shouldUpdateCorrectly() {
        DropdownPage page = new DropdownPage();
        page.open()
                .selectOption("Option 1");
        Assert.assertEquals(page.getSelectedOption(), "Option 1");

        page.selectOption("Option 2");
        Assert.assertEquals(page.getSelectedOption(), "Option 2");
    }
}
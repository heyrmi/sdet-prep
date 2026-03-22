package ra.hul.tests.ui;

import org.testng.Assert;
import org.testng.annotations.Test;
import ra.hul.framework.pages.CheckboxPage;
import ra.hul.tests.base.BaseTest;

/**
 * UI tests for the-internet.herokuapp.com/checkboxes
 */
public class CheckboxTest extends BaseTest {

    @Test(description = "Verify checkbox default states")
    public void checkboxes_defaultState_shouldBeCorrect() {
        CheckboxPage page = new CheckboxPage();
        page.open();

        Assert.assertFalse(page.isChecked(0), "First checkbox should be unchecked by default");
        Assert.assertTrue(page.isChecked(1), "Second checkbox should be checked by default");
    }

    @Test(description = "Verify toggling checkbox changes state")
    public void checkboxes_toggle_shouldChangeState() {
        CheckboxPage page = new CheckboxPage();
        page.open();

        // Toggle first checkbox ON
        page.toggleCheckbox(0);
        Assert.assertTrue(page.isChecked(0), "First checkbox should be checked after toggle");

        // Toggle second checkbox OFF
        page.toggleCheckbox(1);
        Assert.assertFalse(page.isChecked(1), "Second checkbox should be unchecked after toggle");
    }

    @Test(description = "Verify page has exactly 2 checkboxes")
    public void checkboxes_count_shouldBeTwo() {
        CheckboxPage page = new CheckboxPage();
        page.open();

        Assert.assertEquals(page.getCheckboxCount(), 2, "Page should have 2 checkboxes");
    }
}
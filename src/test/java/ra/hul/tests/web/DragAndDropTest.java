package ra.hul.tests.web;

import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import ra.hul.framework.web.pages.DragAndDropPage;
import ra.hul.tests.base.BaseWebTest;

@Epic("Web Automation")
@Feature("Drag and Drop")
public class DragAndDropTest extends BaseWebTest {

    @Test(groups = {"regression"},
          description = "Drag column A to column B using JavaScript workaround")
    @Severity(SeverityLevel.NORMAL)
    @Story("HTML5 Drag and Drop")
    public void dragAndDrop_AtoB_shouldSwapHeaders() {
        DragAndDropPage page = new DragAndDropPage().open();
        String originalA = page.getColumnAHeader();
        String originalB = page.getColumnBHeader();

        page.dragAToB();

        Assert.assertEquals(page.getColumnAHeader(), originalB);
        Assert.assertEquals(page.getColumnBHeader(), originalA);
    }

    @Test(groups = {"regression"},
          description = "Drag and swap back to original positions")
    @Severity(SeverityLevel.MINOR)
    @Story("HTML5 Drag and Drop")
    public void dragAndDrop_AtoB_thenBtoA_shouldRestoreOriginal() {
        DragAndDropPage page = new DragAndDropPage().open();
        String originalA = page.getColumnAHeader();

        page.dragAToB();
        page.dragBToA();

        Assert.assertEquals(page.getColumnAHeader(), originalA);
    }
}

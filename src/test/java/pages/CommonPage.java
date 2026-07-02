package pages;

import constants.CommonPageConstants;
import helpers.PageContext;
import io.cucumber.datatable.DataTable;
import org.openqa.selenium.By;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static constants.CommonPageConstants.menuItemList;

public class CommonPage extends BasePage {

    private static final Logger logger = LoggerFactory.getLogger(CommonPage.class);

    public CommonPage(PageContext ctx) {
        super(ctx);
    }

    public void navigateToMenu(DataTable menuTable) {
        List<Map<String, String>> rows = menuTable.asMaps();
        for (Map<String, String> row : rows) {
            String menuItem = row.get(CommonPageConstants.MENU_ITEM_KEY).trim();
            By locator = menuItemList(menuItem);

            logger.info("Navigating to menu item: {}", menuItem);
            ctx.asserts.assertVisible(locator, "The menu item is not displayed. %s ".formatted(menuItem));
            ctx.element.clickElement(ctx.element.findElement(locator), 10);
            logger.info("Successfully navigated to menu item: {}", menuItem);
        }
    }
}

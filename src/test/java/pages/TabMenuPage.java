package pages;

import helpers.GestureHelper;
import helpers.PageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static constants.TabMenuConstants.*;

public class TabMenuPage extends BasePage {

    private static final Logger logger = LoggerFactory.getLogger(TabMenuPage.class);

    public TabMenuPage(PageContext ctx) {
        super(ctx);
    }

    public void theUserScrollsToMenu(String tabMenu) {
        ctx.gesture.swipeUntilElementVisible(ctx.element.findElement(tabListMenu(tabMenu)), 2, GestureHelper.Direction.VERTICAL);
        ctx.wait.waitSeconds(5);
        ctx.element.clickElement(tabListMenu(tabMenu));
        logger.info("The user scrolls to the {} menu.", tabMenu);
    }

    public void theUserNavigatesToMenu(String menu) {
        ctx.asserts.assertVisible(tabListMenu(menu), "The {} menu is not displayed.");
        ctx.element.clickElement(tabListMenu(menu));
        logger.info("The user navigates to the {} menu.", menu);
    }

    public void theUserClicksTheTab(String tabNumber) {
        int alreadySwiped = 0;
        int maxSwipe = 10;

        while (alreadySwiped < maxSwipe) {
            if (ctx.wait.isElementVisible(scrollableTabs(tabNumber), 5)) break;
            ctx.gesture.scrollRight(ctx.element.findElement(SCROLL_VIEW_CLASS), 85, 15, 700);
            alreadySwiped++;
        }

        ctx.element.clickElement(scrollableTabs(tabNumber));
        logger.info("The user clicks the {} tab.", tabNumber);
    }

    public void theOpenedTabInformationShouldBelongTo(String tabName) {
        ctx.asserts.assertVisible(scrollTabsText(tabName), "The opened tab information is not displayed.");
        String tabTxt = "Content for tab with tag %s".formatted(tabName);
        ctx.asserts.assertEquals(ctx.element.getText(scrollTabsText(tabName)), tabTxt, "The opened tab information is not correct.");
        logger.info("The opened tab information should belong to {}.", tabName);
    }
}

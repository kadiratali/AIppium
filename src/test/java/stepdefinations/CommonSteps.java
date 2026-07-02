package stepdefinations;

import helpers.PageContext;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import pages.CommonPage;

public class CommonSteps {

    private final CommonPage commonPage;

    public CommonSteps(PageContext ctx) {
        this.commonPage = new CommonPage(ctx);
    }

    @Given("the user navigates to the following menu")
    public void theUserNavigatesToMenu(DataTable menuTable) {
        commonPage.navigateToMenu(menuTable);
    }
}

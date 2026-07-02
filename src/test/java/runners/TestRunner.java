package runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

/**
 * TestNG test runner class used to execute Cucumber tests.
 * This class configures Cucumber by specifying the feature files (`features`)
 * and the step definition files (`glue`).
 * <p>
 * The {@link CucumberOptions} annotation defines how Cucumber runs:
 * <ul>
 * <li>`tags`: Determines which scenarios run. Usually set dynamically
 * via command-line arguments.</li>
 * <li>`features`: Specifies the path to the feature files.</li>
 * <li>`glue`: Specifies the package containing the step definition files.</li>
 * <li>`plugin`: Used to integrate test results with the Allure reporting tool.</li>
 * </ul>
 */
@CucumberOptions(
        tags = "${cucumber.filter.tags}",
        features = "${cucumber.features}",
        glue = "stepdefinations",
        plugin = {
                "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm",
                // Stable JSON report read by the AI Failure Analyzer (relative to project root)
                "json:reports/cucumber-report.json"
        }
)

public class TestRunner extends AbstractTestNGCucumberTests {

    /**
     * Data provider method that allows TestNG to run scenarios in parallel.
     * This method overrides {@link AbstractTestNGCucumberTests#scenarios()} so that
     * tests run concurrently on different threads.
     *
     * @return an Object array containing the Cucumber scenarios.
     */
    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
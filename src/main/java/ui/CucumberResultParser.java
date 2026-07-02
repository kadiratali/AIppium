package ui;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the Cucumber JSON report (written by TestRunner's
 * {@code json:reports/cucumber-report.json} plugin) into a flat list of
 * scenario results for the local run-UI.
 */
final class CucumberResultParser {

    record ScenarioResult(String feature, String scenario, boolean passed, String failedStep, String errorMessage) {
    }

    private CucumberResultParser() {
    }

    static List<ScenarioResult> parse(Path reportPath) {
        List<ScenarioResult> results = new ArrayList<>();
        if (!Files.exists(reportPath)) {
            return results;
        }

        JsonArray report;
        try {
            report = new Gson().fromJson(Files.readString(reportPath), JsonArray.class);
        } catch (IOException e) {
            throw new RuntimeException("Report could not be read: " + reportPath, e);
        }
        if (report == null) {
            return results;
        }

        for (JsonElement fe : report) {
            JsonObject feature = fe.getAsJsonObject();
            String featureName = str(feature, "name");
            JsonArray elements = feature.getAsJsonArray("elements");
            if (elements == null) {
                continue;
            }
            for (JsonElement ee : elements) {
                JsonObject element = ee.getAsJsonObject();
                if (!"scenario".equals(str(element, "type"))) {
                    continue;
                }
                results.add(scenarioResult(featureName, element));
            }
        }
        return results;
    }

    private static ScenarioResult scenarioResult(String featureName, JsonObject element) {
        JsonArray steps = element.getAsJsonArray("steps");
        boolean passed = true;
        String failedStep = null;
        String errorMessage = null;

        if (steps != null) {
            for (JsonElement se : steps) {
                JsonObject step = se.getAsJsonObject();
                JsonObject result = step.getAsJsonObject("result");
                String status = result == null ? null : str(result, "status");
                if ("failed".equals(status)) {
                    passed = false;
                    failedStep = str(step, "keyword") + str(step, "name");
                    errorMessage = str(result, "error_message");
                    break;
                }
            }
        }

        return new ScenarioResult(featureName, str(element, "name"), passed, failedStep, errorMessage);
    }

    private static String str(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return (el == null || el.isJsonNull()) ? "" : el.getAsString();
    }
}

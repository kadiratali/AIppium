package ai;

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
 * Reads failed scenarios from a Cucumber JSON report and classifies them with AI
 * (app-bug / test-bug / flaky / environment), producing {@code reports/ai-analysis.md}.
 *
 * The report is written by the {@code json:reports/cucumber-report.json} plugin in TestRunner.
 */
public final class FailureAnalyzer {

    private final AnthropicClient client;
    private final Redactor redactor;
    private final Gson gson = new Gson();

    public FailureAnalyzer(AnthropicClient client, Redactor redactor) {
        this.client = client;
        this.redactor = redactor;
    }

    public record Failure(String feature, String scenario, String failedStep, String errorMessage) {
    }

    /** Extracts failed scenarios from a Cucumber JSON report (no API key required). */
    public static List<Failure> extractFailures(Path reportPath) {
        List<Failure> failures = new ArrayList<>();
        JsonArray report;
        try {
            report = new Gson().fromJson(Files.readString(reportPath), JsonArray.class);
        } catch (IOException e) {
            throw new RuntimeException("Rapor okunamadı: " + reportPath + " -> " + e.getMessage(), e);
        }
        if (report == null) {
            return failures;
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
                JsonArray steps = element.getAsJsonArray("steps");
                if (steps == null) {
                    continue;
                }
                for (JsonElement se : steps) {
                    JsonObject step = se.getAsJsonObject();
                    JsonObject result = step.getAsJsonObject("result");
                    if (result != null && "failed".equals(str(result, "status"))) {
                        failures.add(new Failure(
                                featureName,
                                str(element, "name"),
                                str(step, "keyword") + str(step, "name"),
                                str(result, "error_message")));
                        break; // the first failed step per scenario is enough
                    }
                }
            }
        }
        return failures;
    }

    public record Analysis(String scenario, String category, String rootCause,
                           String suggestedFix, String confidence) {
    }

    public record AnalysisResult(List<Analysis> analyses, String summary) {
    }

    public AnalysisResult analyze(List<Failure> failures) {
        // Redact the failure data (especially error_message) before it goes to the LLM;
        // error messages can contain expected/actual values, the biggest leak vector.
        JsonArray safe = new JsonArray();
        for (Failure f : failures) {
            JsonObject o = new JsonObject();
            o.addProperty("feature", redactor.redact(f.feature()));
            o.addProperty("scenario", redactor.redact(f.scenario()));
            o.addProperty("failedStep", redactor.redact(f.failedStep()));
            o.addProperty("errorMessage", redactor.redact(f.errorMessage()));
            safe.add(o);
        }

        String user = "Analyze these failed scenarios:\n\n" + gson.toJson(safe)
                + "\n\nRespond with ONLY a single JSON object (no markdown fences, no prose) "
                + "matching exactly this shape:\n" + Prompts.ANALYZER_SCHEMA_HINT;

        String raw = client.complete(Prompts.ANALYZER_SYSTEM, user, 8000);
        JsonObject obj = gson.fromJson(JsonText.extractJsonObject(raw), JsonObject.class);

        List<Analysis> analyses = new ArrayList<>();
        JsonArray arr = obj.getAsJsonArray("analyses");
        if (arr != null) {
            for (JsonElement el : arr) {
                JsonObject a = el.getAsJsonObject();
                analyses.add(new Analysis(
                        str(a, "scenario"),
                        str(a, "category"),
                        str(a, "rootCause"),
                        str(a, "suggestedFix"),
                        str(a, "confidence")));
            }
        }
        return new AnalysisResult(analyses, str(obj, "summary"));
    }

    /** Writes the analysis to {@code reports/ai-analysis.md} and returns the path. */
    public Path writeReport(AnalysisResult result, Path root) {
        StringBuilder md = new StringBuilder();
        md.append("# AI Failure Analysis\n\n");
        md.append("> ").append(result.summary()).append("\n\n");

        for (Analysis a : result.analyses()) {
            md.append("## ").append(a.scenario()).append("\n\n");
            md.append("- **Category:** `").append(a.category())
                    .append("` (confidence: ").append(a.confidence()).append(")\n");
            md.append("- **Root cause:** ").append(a.rootCause()).append("\n");
            md.append("- **Suggested fix:** ").append(a.suggestedFix()).append("\n\n");
        }

        Path target = root.resolve("reports").resolve("ai-analysis.md");
        try {
            Files.createDirectories(target.getParent());
            Files.writeString(target, md.toString());
        } catch (IOException e) {
            throw new RuntimeException("Analiz raporu yazılamadı: " + e.getMessage(), e);
        }
        return target;
    }

    private static String str(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return (el == null || el.isJsonNull()) ? "" : el.getAsString();
    }
}

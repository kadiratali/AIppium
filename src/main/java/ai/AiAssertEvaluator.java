package ai;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Evaluates a natural-language UI expectation against the current screen
 * (screenshot + page source) with Claude and returns a structured verdict.
 * Used by {@code helpers.AssertHelper#assertAi(String)}.
 * <p>
 * The page source and the expectation are passed through {@link Redactor}
 * before leaving the machine, per this project's rule that every piece of
 * text sent to the LLM is redacted first. The screenshot is sent as-is
 * (image content can't be pattern-redacted).
 */
public final class AiAssertEvaluator {

    private static final String SYSTEM = """
            You are a strict mobile UI test assertion evaluator. You receive one
            expectation, a screenshot of the current screen, and the UI page source XML.

            Rules:
            - Judge ONLY from the provided screenshot and page source. Never assume
              anything that is not visible in them.
            - Be conservative: if the evidence is insufficient or ambiguous, the verdict
              is "fail" with a reason explaining what could not be verified.
            - You may derive values from what is visible to evaluate data expectations:
              sums, counts, ordering, date/number formats, consistency between fields.
            - When the expectation is not met, the reason must be concrete and actionable:
              state what was expected, what is actually on screen (quote the relevant
              texts or resource-ids), and where they differ - phrased so an engineer can
              fix the test or file a bug without opening the device. Under 120 words.

            Respond with ONLY this JSON object (no markdown fences, no prose):
            {"verdict":"pass" or "fail","reason":"<explanation>"}
            """;

    private final AnthropicClient client;
    private final Redactor redactor;
    private final Gson gson = new Gson();

    public AiAssertEvaluator() {
        this(new AnthropicClient(), Redactor.withDefaults());
    }

    public AiAssertEvaluator(AnthropicClient client, Redactor redactor) {
        this.client = client;
        this.redactor = redactor;
    }

    public record Verdict(boolean passed, String reason) {
    }

    public Verdict evaluate(String expectation, String pageSourceXml, String screenshotBase64Png) {
        String user = "Expectation to verify:\n" + redactor.redact(expectation)
                + "\n\nCurrent page source XML:\n" + redactor.redact(pageSourceXml)
                + "\n\nRespond with ONLY the JSON object.";

        String raw = client.complete(SYSTEM, user, screenshotBase64Png, 1000);
        JsonObject obj = gson.fromJson(JsonText.extractJsonObject(raw), JsonObject.class);

        boolean passed = obj.has("verdict")
                && "pass".equalsIgnoreCase(obj.get("verdict").getAsString());
        String reason = obj.has("reason") && !obj.get("reason").isJsonNull()
                ? obj.get("reason").getAsString()
                : "(no reason provided by the model)";
        return new Verdict(passed, reason);
    }
}

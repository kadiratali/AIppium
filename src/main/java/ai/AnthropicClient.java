package ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Thin client that calls the Anthropic Messages API over raw HTTP (java.net.http).
 * Works with the JDK + Gson only, no extra SDK dependency.
 *
 * Lookup order for the {@code ANTHROPIC_API_KEY} api key:
 * <ol>
 *   <li>OS environment variable ({@code export ANTHROPIC_API_KEY=...})</li>
 *   <li>JVM system property ({@code -DANTHROPIC_API_KEY=...})</li>
 *   <li>{@code .env} file at the project root ({@code ANTHROPIC_API_KEY=...})</li>
 * </ol>
 * The {@code .env} file is listed in {@code .gitignore}; it is never committed.
 */
public final class AnthropicClient {

    public static final String MODEL = "claude-sonnet-5";
    private static final String API_KEY_NAME = "ANTHROPIC_API_KEY";
    private static final String ENDPOINT = "https://api.anthropic.com/v1/messages";
    private static final String API_VERSION = "2023-06-01";

    private final String apiKey;
    private final HttpClient http;
    private final Gson gson = new Gson();

    public AnthropicClient() {
        this.apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(API_KEY_NAME + " bulunamadı. Şu yollardan biriyle tanımlayın:\n"
                    + "  1) export " + API_KEY_NAME + "=sk-ant-...\n"
                    + "  2) mvn ... -D" + API_KEY_NAME + "=sk-ant-...\n"
                    + "  3) Proje köküne .env dosyası: " + API_KEY_NAME + "=sk-ant-...");
        }
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    /** Resolves the key in order: env var → system property → project-root .env. */
    private static String resolveApiKey() {
        String fromEnv = System.getenv(API_KEY_NAME);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        String fromProp = System.getProperty(API_KEY_NAME);
        if (fromProp != null && !fromProp.isBlank()) {
            return fromProp.trim();
        }
        return readFromDotEnv(Path.of(System.getProperty("user.dir"), ".env"));
    }

    /** Simple .env reader: looks for the {@code ANTHROPIC_API_KEY=...} line (strips quotes). */
    private static String readFromDotEnv(Path envFile) {
        if (!Files.exists(envFile)) {
            return null;
        }
        try {
            for (String line : Files.readAllLines(envFile)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                if (trimmed.startsWith("export ")) {
                    trimmed = trimmed.substring("export ".length()).trim();
                }
                int eq = trimmed.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, eq).trim();
                if (API_KEY_NAME.equals(key)) {
                    String value = trimmed.substring(eq + 1).trim();
                    if ((value.startsWith("\"") && value.endsWith("\""))
                            || (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    return value.isBlank() ? null : value;
                }
            }
        } catch (IOException e) {
            System.err.println("Uyarı: .env okunamadı (" + envFile + "): " + e.getMessage());
        }
        return null;
    }

    /**
     * Calls the model with a system prompt and a single user message, returning the combined text response.
     *
     * @param system      system prompt
     * @param userMessage user message
     * @param maxTokens   maximum tokens to generate
     * @return the model's text response
     */
    public String complete(String system, String userMessage, int maxTokens) {
        JsonObject body = new JsonObject();
        body.addProperty("model", MODEL);
        body.addProperty("max_tokens", maxTokens);
        body.addProperty("system", system);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);
        JsonArray messages = new JsonArray();
        messages.add(userMsg);
        body.add("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .timeout(Duration.ofMinutes(5))
                .header("content-type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", API_VERSION)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();

        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException("Anthropic API çağrısı başarısız: " + e.getMessage(), e);
        }

        if (response.statusCode() / 100 != 2) {
            throw new RuntimeException(
                    "Anthropic API hatası (HTTP " + response.statusCode() + "): " + response.body());
        }

        JsonObject json = gson.fromJson(response.body(), JsonObject.class);

        if (json.has("stop_reason") && !json.get("stop_reason").isJsonNull()
                && "refusal".equals(json.get("stop_reason").getAsString())) {
            throw new RuntimeException("Model isteği reddetti (stop_reason: refusal).");
        }

        JsonArray content = json.getAsJsonArray("content");
        if (content == null) {
            throw new RuntimeException("Modelden içerik alınamadı: " + response.body());
        }

        StringBuilder text = new StringBuilder();
        for (JsonElement el : content) {
            JsonObject block = el.getAsJsonObject();
            if ("text".equals(block.get("type").getAsString())) {
                text.append(block.get("text").getAsString());
            }
        }
        if (text.length() == 0) {
            throw new RuntimeException("Modelden metin yanıtı alınamadı.");
        }
        return text.toString();
    }
}

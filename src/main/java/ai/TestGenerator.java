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
 * Generates Appium BDD test artifacts for this framework from a user story:
 * feature + step definition + page object(s) + locator constants.
 * Never overwrites an existing file; on conflict, leaves a {@code .generated} sibling file.
 */
public final class TestGenerator {

    private final AnthropicClient client;
    private final Redactor redactor;
    private final Gson gson = new Gson();

    public TestGenerator(AnthropicClient client, Redactor redactor) {
        this.client = client;
        this.redactor = redactor;
    }

    public record FileArtifact(String fileName, String content) {
    }

    public record Result(String featureFileName, String featureContent,
                         String stepsFileName, String stepsContent,
                         List<FileArtifact> pages, List<FileArtifact> constants,
                         String notes) {
    }

    public Result generate(String userStory) {
        String safeStory = redactor.redact(userStory);

        String user = "Generate the Appium BDD test artifacts for this user story:\n\n"
                + safeStory
                + "\n\nRespond with ONLY a single JSON object (no markdown fences, no prose) "
                + "matching exactly this shape:\n" + Prompts.GENERATOR_SCHEMA_HINT;

        String raw = client.complete(Prompts.GENERATOR_SYSTEM, user, 16000);
        JsonObject obj = gson.fromJson(JsonText.extractJsonObject(raw), JsonObject.class);

        return new Result(
                str(obj, "featureFileName"),
                str(obj, "featureContent"),
                str(obj, "stepsFileName"),
                str(obj, "stepsContent"),
                files(obj, "pages"),
                files(obj, "constants"),
                str(obj, "notes")
        );
    }

    /**
     * Writes the artifacts to their target directories; preserves any existing file.
     *
     * @return the list of paths written (or saved as a sibling file)
     */
    public List<String> write(Result r, Path root) {
        List<String> written = new ArrayList<>();
        writeFile(root.resolve("src/test/resources/Feature"), r.featureFileName(), r.featureContent(), written);
        writeFile(root.resolve("src/test/java/stepdefinations"), r.stepsFileName(), r.stepsContent(), written);
        for (FileArtifact p : r.pages()) {
            writeFile(root.resolve("src/test/java/pages"), p.fileName(), p.content(), written);
        }
        for (FileArtifact c : r.constants()) {
            writeFile(root.resolve("src/main/java/constants"), c.fileName(), c.content(), written);
        }
        return written;
    }

    private void writeFile(Path dir, String fileName, String content, List<String> written) {
        if (fileName == null || fileName.isBlank() || content == null) {
            return;
        }
        try {
            Files.createDirectories(dir);
            // Drop any directory parts the model output might contain, keep just the file name.
            Path target = dir.resolve(Path.of(fileName).getFileName().toString());
            if (Files.exists(target)) {
                Path backup = target.resolveSibling(target.getFileName() + ".generated");
                Files.writeString(backup, content);
                written.add(backup + "  (mevcut dosyanın üzerine yazılmadı)");
            } else {
                Files.writeString(target, content);
                written.add(target.toString());
            }
        } catch (IOException e) {
            throw new RuntimeException("Dosya yazılamadı: " + fileName + " -> " + e.getMessage(), e);
        }
    }

    private static String str(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return (el == null || el.isJsonNull()) ? "" : el.getAsString();
    }

    private List<FileArtifact> files(JsonObject obj, String key) {
        List<FileArtifact> list = new ArrayList<>();
        JsonArray arr = obj.getAsJsonArray(key);
        if (arr == null) {
            return list;
        }
        for (JsonElement el : arr) {
            JsonObject o = el.getAsJsonObject();
            list.add(new FileArtifact(str(o, "fileName"), str(o, "content")));
        }
        return list;
    }
}

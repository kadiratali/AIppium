package ui;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Thin GitHub REST API client for triggering the CI workflow, polling its
 * job/step status, and downloading the resulting artifact zip. Plain JDK
 * HttpClient + Gson - same "no extra tool chain" approach as the rest of
 * this project.
 * <p>
 * Requires a token with {@code repo} + {@code workflow} scope, resolved (in
 * order) from the {@code GITHUB_TOKEN} env var, the {@code GITHUB_TOKEN}
 * system property, or a {@code GITHUB_TOKEN=...} line in the project's
 * {@code .env} file.
 */
final class GitHubClient {

    private static final String API = "https://api.github.com";
    private static final String WORKFLOW_FILE = "ci.yml";
    private static final Pattern SSH_REMOTE = Pattern.compile("git@github\\.com:(.+?)/(.+?)(?:\\.git)?$");
    private static final Pattern HTTPS_REMOTE = Pattern.compile("https://github\\.com/(.+?)/(.+?)(?:\\.git)?$");

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final Gson gson = new Gson();
    private final String token;
    final String owner;
    final String repo;

    GitHubClient() {
        this.token = resolveToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("GITHUB_TOKEN not found. Provide it via:\n"
                    + "  1) export GITHUB_TOKEN=ghp_...\n"
                    + "  2) mvn ... -DGITHUB_TOKEN=ghp_...\n"
                    + "  3) a GITHUB_TOKEN=... line in the project's .env file\n"
                    + "Needs 'repo' and 'workflow' scope to dispatch the CI workflow and download artifacts.");
        }
        String[] ownerRepo = resolveOwnerRepo();
        this.owner = ownerRepo[0];
        this.repo = ownerRepo[1];
    }

    /** Triggers the CI workflow with the given tag input; returns the moment just before dispatch. */
    Instant dispatch(String tag) throws IOException, InterruptedException {
        Instant before = Instant.now();
        JsonObject inputs = new JsonObject();
        inputs.addProperty("tag", tag);
        JsonObject body = new JsonObject();
        body.addProperty("ref", "main");
        body.add("inputs", inputs);

        HttpResponse<String> response = send(request("POST",
                "/repos/" + owner + "/" + repo + "/actions/workflows/" + WORKFLOW_FILE + "/dispatches",
                gson.toJson(body)));
        if (response.statusCode() != 204) {
            throw new IOException("Workflow dispatch failed (HTTP " + response.statusCode() + "): " + response.body());
        }
        return before;
    }

    /**
     * Finds the run this dispatch created. GitHub's dispatch endpoint doesn't
     * return a run id, so this looks up the newest workflow_dispatch-triggered
     * run and waits for it to actually appear (it can take a few seconds).
     */
    long findDispatchedRun(Instant dispatchedAfter) throws IOException, InterruptedException {
        for (int attempt = 0; attempt < 15; attempt++) {
            HttpResponse<String> response = send(request("GET",
                    "/repos/" + owner + "/" + repo + "/actions/workflows/" + WORKFLOW_FILE
                            + "/runs?event=workflow_dispatch&per_page=5", null));
            JsonObject body = gson.fromJson(response.body(), JsonObject.class);
            JsonArray runs = body.getAsJsonArray("workflow_runs");
            if (runs != null) {
                for (JsonElement re : runs) {
                    JsonObject run = re.getAsJsonObject();
                    Instant createdAt = Instant.parse(run.get("created_at").getAsString());
                    if (!createdAt.isBefore(dispatchedAfter.minusSeconds(5))) {
                        return run.get("id").getAsLong();
                    }
                }
            }
            Thread.sleep(2000);
        }
        throw new IOException("Could not find the dispatched run after 30s");
    }

    record Step(String name, String status, String conclusion) {
    }

    record Job(String name, String status, String conclusion, List<Step> steps) {
    }

    record RunStatus(String status, String conclusion, List<Job> jobs) {
    }

    RunStatus getRunStatus(long runId) throws IOException, InterruptedException {
        HttpResponse<String> runResponse = send(request("GET",
                "/repos/" + owner + "/" + repo + "/actions/runs/" + runId, null));
        JsonObject run = gson.fromJson(runResponse.body(), JsonObject.class);

        HttpResponse<String> jobsResponse = send(request("GET",
                "/repos/" + owner + "/" + repo + "/actions/runs/" + runId + "/jobs", null));
        JsonObject jobsBody = gson.fromJson(jobsResponse.body(), JsonObject.class);

        List<Job> jobs = new java.util.ArrayList<>();
        JsonArray jobArray = jobsBody.getAsJsonArray("jobs");
        if (jobArray != null) {
            for (JsonElement je : jobArray) {
                JsonObject j = je.getAsJsonObject();
                List<Step> steps = new java.util.ArrayList<>();
                JsonArray stepArray = j.getAsJsonArray("steps");
                if (stepArray != null) {
                    for (JsonElement se : stepArray) {
                        JsonObject s = se.getAsJsonObject();
                        steps.add(new Step(str(s, "name"), str(s, "status"), str(s, "conclusion")));
                    }
                }
                jobs.add(new Job(str(j, "name"), str(j, "status"), str(j, "conclusion"), steps));
            }
        }
        return new RunStatus(str(run, "status"), str(run, "conclusion"), jobs);
    }

    String runUrl(long runId) {
        return "https://github.com/" + owner + "/" + repo + "/actions/runs/" + runId;
    }

    /** Downloads the named artifact for a run and extracts it under {@code targetDir}. */
    void downloadArtifact(long runId, String artifactName, Path targetDir) throws IOException, InterruptedException {
        HttpResponse<String> listResponse = send(request("GET",
                "/repos/" + owner + "/" + repo + "/actions/runs/" + runId + "/artifacts", null));
        JsonObject body = gson.fromJson(listResponse.body(), JsonObject.class);
        JsonArray artifacts = body.getAsJsonArray("artifacts");

        Long artifactId = null;
        if (artifacts != null) {
            for (JsonElement ae : artifacts) {
                JsonObject a = ae.getAsJsonObject();
                if (artifactName.equals(str(a, "name"))) {
                    artifactId = a.get("id").getAsLong();
                    break;
                }
            }
        }
        if (artifactId == null) {
            throw new IOException("Artifact '" + artifactName + "' not found for run " + runId);
        }

        // GitHub answers with a 302 to a pre-signed, unauthenticated download URL;
        // HttpClient's default redirect policy strips the Authorization header on
        // a cross-host redirect (correct behavior), so the redirect is followed
        // manually here in two explicit requests instead.
        HttpRequest first = request("GET",
                "/repos/" + owner + "/" + repo + "/actions/artifacts/" + artifactId + "/zip", null);
        HttpClient noRedirect = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        HttpResponse<Void> redirect = noRedirect.send(first, HttpResponse.BodyHandlers.discarding());
        String location = redirect.headers().firstValue("Location")
                .orElseThrow(() -> new IOException("No redirect location for artifact download"));

        HttpResponse<byte[]> zipResponse = http.send(
                HttpRequest.newBuilder(URI.create(location)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
        if (zipResponse.statusCode() != 200) {
            throw new IOException("Artifact download failed (HTTP " + zipResponse.statusCode() + ")");
        }

        Files.createDirectories(targetDir);
        unzip(zipResponse.body(), targetDir);
    }

    private static void unzip(byte[] zipBytes, Path targetDir) throws IOException {
        try (var zin = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(zipBytes))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                Path out = targetDir.resolve(entry.getName()).normalize();
                if (!out.startsWith(targetDir)) {
                    continue; // guard against a malicious zip entry escaping targetDir
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    Files.write(out, zin.readAllBytes());
                }
            }
        }
    }

    private HttpRequest request(String method, String path, String jsonBody) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(API + path))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer " + token)
                .header("X-GitHub-Api-Version", "2022-11-28");
        builder = jsonBody == null
                ? builder.GET()
                : builder.header("Content-Type", "application/json")
                        .method(method, HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));
        return builder.build();
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String resolveToken() {
        String fromEnv = System.getenv("GITHUB_TOKEN");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        String fromProp = System.getProperty("GITHUB_TOKEN");
        if (fromProp != null && !fromProp.isBlank()) {
            return fromProp.trim();
        }
        Path envFile = Path.of(System.getProperty("user.dir"), ".env");
        if (!Files.exists(envFile)) {
            return null;
        }
        try {
            for (String line : Files.readAllLines(envFile)) {
                String trimmed = line.trim();
                if (trimmed.startsWith("GITHUB_TOKEN=")) {
                    String value = trimmed.substring("GITHUB_TOKEN=".length()).trim();
                    if ((value.startsWith("\"") && value.endsWith("\""))
                            || (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    return value.isBlank() ? null : value;
                }
            }
        } catch (IOException ignored) {
            // fall through to null
        }
        return null;
    }

    /** Derives owner/repo from `git remote get-url origin` (supports SSH and HTTPS forms). */
    private static String[] resolveOwnerRepo() {
        try {
            Process process = new ProcessBuilder("git", "remote", "get-url", "origin")
                    .redirectErrorStream(true)
                    .start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            process.waitFor();

            Matcher ssh = SSH_REMOTE.matcher(output);
            if (ssh.matches()) {
                return new String[] {ssh.group(1), ssh.group(2)};
            }
            Matcher https = HTTPS_REMOTE.matcher(output);
            if (https.matches()) {
                return new String[] {https.group(1), https.group(2)};
            }
            throw new IllegalStateException("Could not parse owner/repo from git remote: " + output);
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("Could not read git remote 'origin': " + e.getMessage(), e);
        }
    }

    private static String str(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return (el == null || el.isJsonNull()) ? "" : el.getAsString();
    }
}

package ui;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * Local dashboard that triggers the project's CI workflow (GitHub Actions) by
 * tag, tails its job/step status as a pseudo-log, and once it finishes,
 * downloads the "appium-test-reports" artifact and shows the scenario results
 * plus the generated Allure report on the page.
 * <p>
 * Runs are always executed on CI (a real emulator on a GitHub-hosted runner),
 * never locally - this keeps a single, consistent execution environment for
 * everyone using the dashboard. Pure JDK ({@code com.sun.net.httpserver}) -
 * no extra web framework.
 * <p>
 * Start with: {@code mvn -q compile exec:java} then open http://localhost:8090.
 * Requires a GITHUB_TOKEN (repo + workflow scope) - see {@link GitHubClient}.
 */
public final class TestRunnerServer {

    private static final int PORT = 8090;
    private static final Path PROJECT_ROOT = Path.of(System.getProperty("user.dir"));
    private static final Path CI_RUN_DIR = PROJECT_ROOT.resolve("reports").resolve("ci-run");
    private static final Path ALLURE_REPORT_DIR = PROJECT_ROOT.resolve("reports").resolve("allure-report");
    private static final String ARTIFACT_NAME = "appium-test-reports";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Tag expressions only ever need these characters; reject anything else outright. */
    private static final Pattern SAFE_TAG_EXPRESSION = Pattern.compile("^[@\\w\\s()!|&,-]+$");

    private final Gson gson = new Gson();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<String> lastError = new AtomicReference<>();
    private final AtomicReference<Long> lastRunId = new AtomicReference<>();
    private final AtomicReference<String> lastRunUrl = new AtomicReference<>();
    private final AtomicReference<List<CucumberResultParser.ScenarioResult>> lastResults =
            new AtomicReference<>(List.of());
    private final AtomicLong logGeneration = new AtomicLong();
    private final List<String> logLines = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        new TestRunnerServer().start();
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", this::serveStatic);
        server.createContext("/api/run", this::handleRun);
        server.createContext("/api/status", this::handleStatus);
        server.createContext("/api/results", this::handleResults);
        server.createContext("/api/logs/stream", this::handleLogStream);
        server.createContext("/allure/", this::serveAllureReport);
        // A long-lived SSE connection (handleLogStream) must not block every
        // other request, so this needs a real pool instead of the default
        // single-threaded executor.
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Test runner UI: http://localhost:" + PORT);
    }

    private void handleRun(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            respondJson(exchange, 405, jsonError("POST required"));
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject payload = gson.fromJson(body.isBlank() ? "{}" : body, JsonObject.class);
        String tag = payload.has("tag") ? payload.get("tag").getAsString().trim() : "";

        if (tag.isEmpty() || !SAFE_TAG_EXPRESSION.matcher(tag).matches()) {
            respondJson(exchange, 400, jsonError("Invalid tag expression: " + tag));
            return;
        }
        if (!running.compareAndSet(false, true)) {
            respondJson(exchange, 409, jsonError("A run is already in progress"));
            return;
        }

        resetLog();
        Thread.ofVirtual().start(() -> runViaCi(tag));
        respondJson(exchange, 202, "{\"started\":true}");
    }

    private void runViaCi(String tag) {
        try {
            appendLog("Resolving GitHub credentials and repo...");
            GitHubClient client = new GitHubClient();

            appendLog("Dispatching CI workflow (tag=" + tag + ")...");
            Instant dispatchedAt = client.dispatch(tag);

            appendLog("Waiting for the dispatched run to appear...");
            long runId = client.findDispatchedRun(dispatchedAt);
            lastRunId.set(runId);
            lastRunUrl.set(client.runUrl(runId));
            appendLog("Run found: " + client.runUrl(runId));

            pollUntilComplete(client, runId);

            appendLog("Downloading artifact '" + ARTIFACT_NAME + "'...");
            resetDir(CI_RUN_DIR);
            client.downloadArtifact(runId, ARTIFACT_NAME, CI_RUN_DIR);

            appendLog("Parsing results...");
            Path reportPath = CI_RUN_DIR.resolve("reports").resolve("cucumber-report.json");
            lastResults.set(CucumberResultParser.parse(reportPath));

            Optional<Path> allureResults = findAllureResultsDir(CI_RUN_DIR);
            if (allureResults.isPresent()) {
                appendLog("Generating Allure report...");
                generateAllureReport(allureResults.get());
            } else {
                appendLog("No allure-results found in the artifact.");
            }

            appendLog("Done.");
            lastError.set(null);
        } catch (Exception e) {
            appendLog("ERROR: " + e.getMessage());
            lastError.set(e.getMessage());
        } finally {
            running.set(false);
        }
    }

    private void pollUntilComplete(GitHubClient client, long runId) throws IOException, InterruptedException {
        Map<String, String> lastSeenStatus = new ConcurrentHashMap<>();
        while (true) {
            GitHubClient.RunStatus status = client.getRunStatus(runId);
            for (GitHubClient.Job job : status.jobs()) {
                for (GitHubClient.Step step : job.steps()) {
                    String key = job.name() + " > " + step.name();
                    String current = step.conclusion().isEmpty() ? step.status() : step.conclusion();
                    if (!current.equals(lastSeenStatus.put(key, current)) && !current.isEmpty()) {
                        appendLog(key + ": " + current);
                    }
                }
            }
            if ("completed".equals(status.status())) {
                appendLog("Workflow run completed: " + status.conclusion());
                return;
            }
            Thread.sleep(4000);
        }
    }

    private Optional<Path> findAllureResultsDir(Path root) {
        try (var children = Files.list(root)) {
            return children
                    .filter(p -> Files.isDirectory(p) && p.getFileName().toString().startsWith("target-"))
                    .map(p -> p.resolve("allure-results"))
                    .filter(Files::isDirectory)
                    .findFirst();
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private void generateAllureReport(Path resultsDir) {
        try {
            Files.createDirectories(ALLURE_REPORT_DIR.getParent());
            Process process = new ProcessBuilder(
                    "allure", "generate", resultsDir.toString(), "-o", ALLURE_REPORT_DIR.toString(), "--clean")
                    .directory(PROJECT_ROOT.toFile())
                    .redirectErrorStream(true)
                    .start();
            try (InputStream in = process.getInputStream()) {
                in.transferTo(OutputStream.nullOutputStream());
            }
            process.waitFor();
        } catch (Exception e) {
            appendLog("Allure report generation failed: " + e.getMessage());
        }
    }

    private static void resetDir(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (var walk = Files.walk(dir)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException ignored) {
                        // best effort cleanup
                    }
                });
            }
        }
        Files.createDirectories(dir);
    }

    // ---- log buffer + SSE tailing ----

    private void resetLog() {
        synchronized (logLines) {
            logLines.clear();
        }
        logGeneration.incrementAndGet();
    }

    private void appendLog(String message) {
        String line = "[" + TIME.format(java.time.LocalTime.now()) + "] " + message;
        synchronized (logLines) {
            logLines.add(line);
        }
    }

    private void handleLogStream(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
        exchange.getResponseHeaders().add("Cache-Control", "no-cache");
        exchange.sendResponseHeaders(200, 0);
        long generationAtStart = logGeneration.get();

        try (OutputStream os = exchange.getResponseBody()) {
            int index = 0;
            while (true) {
                List<String> newLines;
                synchronized (logLines) {
                    newLines = index < logLines.size() ? List.copyOf(logLines.subList(index, logLines.size()))
                            : List.of();
                }
                for (String line : newLines) {
                    os.write(("data: " + line.replace("\n", " ") + "\n\n").getBytes(StandardCharsets.UTF_8));
                }
                if (!newLines.isEmpty()) {
                    os.flush();
                    index += newLines.size();
                }
                boolean staleGeneration = logGeneration.get() != generationAtStart;
                if (staleGeneration || (!running.get() && index >= logLines.size())) {
                    os.write("event: done\ndata: end\n\n".getBytes(StandardCharsets.UTF_8));
                    os.flush();
                    return;
                }
                Thread.sleep(500);
            }
        } catch (IOException e) {
            // client disconnected - nothing to clean up
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ---- status/results/static ----

    private void handleStatus(HttpExchange exchange) throws IOException {
        respondJson(exchange, 200, "{\"running\":" + running.get() + "}");
    }

    private void handleResults(HttpExchange exchange) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("hasRun", lastRunId.get() != null);
        if (lastError.get() != null) {
            root.addProperty("error", lastError.get());
        }
        if (lastRunId.get() != null) {
            root.addProperty("runId", lastRunId.get());
            root.addProperty("runUrl", lastRunUrl.get());
        }
        root.add("scenarios", gson.toJsonTree(lastResults.get()));
        root.addProperty("allureReady", Files.isRegularFile(ALLURE_REPORT_DIR.resolve("index.html")));
        respondJson(exchange, 200, gson.toJson(root));
    }

    /** Serves the generated Allure static site from disk (not a classpath resource). */
    private void serveAllureReport(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath().substring("/allure".length());
        if (path.isEmpty() || path.equals("/")) {
            path = "/index.html";
        }
        Path file = ALLURE_REPORT_DIR.resolve(path.substring(1)).normalize();

        if (!file.startsWith(ALLURE_REPORT_DIR) || !Files.isRegularFile(file)) {
            respondJson(exchange, 404, "\"not found\"");
            return;
        }

        byte[] bytes = Files.readAllBytes(file);
        String contentType = Files.probeContentType(file);
        exchange.getResponseHeaders().add("Content-Type", contentType != null ? contentType : "application/octet-stream");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void serveStatic(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/")) {
            path = "/index.html";
        }
        String resourcePath = "/ui" + path;

        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            if (in == null) {
                respondJson(exchange, 404, "\"not found\"");
                return;
            }
            byte[] bytes = in.readAllBytes();
            exchange.getResponseHeaders().add("Content-Type", contentType(path));
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private static String contentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=utf-8";
        if (path.endsWith(".css")) return "text/css; charset=utf-8";
        if (path.endsWith(".js")) return "application/javascript; charset=utf-8";
        return "application/octet-stream";
    }

    private String jsonError(String message) {
        JsonObject obj = new JsonObject();
        obj.addProperty("error", message);
        return gson.toJson(obj);
    }

    private void respondJson(HttpExchange exchange, int status, String jsonBody) throws IOException {
        byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}

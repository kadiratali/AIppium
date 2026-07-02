package ai;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * AI QA Agent CLI.
 *
 * <pre>
 *   mvn -q compile exec:java -Dexec.args="generate stories/login.txt"
 *   mvn -q compile exec:java -Dexec.args="generate 'As a user I want to ...'"
 *   mvn -q compile exec:java -Dexec.args="analyze"                  # reports/cucumber-report.json
 *   mvn -q compile exec:java -Dexec.args="analyze path/to/report.json"
 * </pre>
 */
public final class Cli {

    private static final String USAGE = """
            AI QA Agent CLI

            Kullanım:
              generate <user-story.txt | "user story metni">
                  User story'den feature + step definition + page object + locator constants üretir.

              analyze [<rapor-yolu>]
                  Cucumber JSON raporundaki hataları AI ile sınıflandırır.
                  Varsayılan rapor: reports/cucumber-report.json

            Örnek (Maven):
              mvn -q compile exec:java -Dexec.args="generate stories/login.txt"
              mvn -q compile exec:java -Dexec.args="analyze"
            """;

    private static final Path ROOT = Path.of(System.getProperty("user.dir"));

    public static void main(String[] args) {
        try {
            run(args);
        } catch (Exception e) {
            System.err.println("Hata: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void run(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println(USAGE);
            return;
        }

        String command = args[0];
        switch (command) {
            case "generate" -> generate(args);
            case "analyze" -> analyze(args);
            default -> System.out.println(USAGE);
        }
    }

    private static void generate(String[] args) throws Exception {
        String input = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)).trim();
        if (input.isEmpty()) {
            System.err.println("Hata: user story dosyası veya metni verin.\n\n" + USAGE);
            System.exit(1);
            return;
        }

        Path maybeFile = Path.of(input);
        String story = Files.exists(maybeFile) ? Files.readString(maybeFile) : input;

        Redactor redactor = Redactor.withDefaults();
        TestGenerator generator = new TestGenerator(new AnthropicClient(), redactor);

        System.out.println("User story işleniyor, test artifact'ları üretiliyor...");
        TestGenerator.Result result = generator.generate(story);
        List<String> written = generator.write(result, ROOT);

        System.out.println("\nOluşturulan dosyalar:");
        for (String f : written) {
            System.out.println("  - " + f);
        }
        System.out.println("\nNotlar:\n" + result.notes());
        System.out.println("\nÇalıştırmak için: mvn test -Dcucumber.filter.tags=\"@Regression\"");
    }

    private static void analyze(String[] args) {
        Path reportPath = args.length > 1
                ? Path.of(args[1])
                : ROOT.resolve("reports").resolve("cucumber-report.json");

        if (!Files.exists(reportPath)) {
            System.err.println("Hata: rapor bulunamadı: " + reportPath
                    + "\nÖnce testleri çalıştırın (mvn test).");
            System.exit(1);
            return;
        }

        // Extracting failures doesn't need an API key; only build the client if there are any.
        List<FailureAnalyzer.Failure> failures = FailureAnalyzer.extractFailures(reportPath);
        if (failures.isEmpty()) {
            System.out.println("Başarısız senaryo yok - analiz gerekmiyor. ✅");
            return;
        }

        System.out.println(failures.size() + " başarısız senaryo analiz ediliyor...");

        Redactor redactor = Redactor.withDefaults();
        FailureAnalyzer analyzer = new FailureAnalyzer(new AnthropicClient(), redactor);

        FailureAnalyzer.AnalysisResult result = analyzer.analyze(failures);
        Path reportFile = analyzer.writeReport(result, ROOT);

        System.out.println("\nÖzet: " + result.summary() + "\n");
        for (FailureAnalyzer.Analysis a : result.analyses()) {
            System.out.println("  [" + a.category() + "] " + a.scenario());
            System.out.println("     Neden : " + a.rootCause());
            System.out.println("     Çözüm : " + a.suggestedFix() + "\n");
        }
        System.out.println("Detaylı rapor: " + reportFile);
    }

    private Cli() {
    }
}

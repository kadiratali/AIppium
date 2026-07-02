package ai;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * PII / sensitive data redaction. EVERY piece of text sent to the LLM (Anthropic API)
 * passes through here first. Two layers:
 * <ol>
 *   <li>Pattern-based: email, IBAN, credit card, Turkish national ID (TCKN), phone formats</li>
 *   <li>Value-based (denylist): real values in {@code ai-secrets.local.txt} are masked
 *       verbatim (even if they don't match a pattern)</li>
 * </ol>
 * Design bias: over-redact rather than under-redact.
 */
public final class Redactor {

    /** Optional, gitignored denylist file at the project root. */
    public static final Path DEFAULT_DENYLIST = Path.of("ai-secrets.local.txt");

    private final Set<String> denylist = new LinkedHashSet<>();

    private record NamedPattern(String name, Pattern pattern) {
    }

    private static final List<NamedPattern> PATTERNS = List.of(
            new NamedPattern("EMAIL", Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.-]+")),
            new NamedPattern("IBAN", Pattern.compile("\\bTR\\d{2}[\\d\\s]{20,30}\\b")),
            new NamedPattern("CARD", Pattern.compile("\\b(?:\\d[ -]?){13,19}\\b")),
            new NamedPattern("TCKN", Pattern.compile("\\b\\d{11}\\b")),
            new NamedPattern("PHONE", Pattern.compile("\\b(?:\\+?90|0)?5\\d{9}\\b"))
    );

    /** Creates a Redactor that loads the default denylist file, if present. */
    public static Redactor withDefaults() {
        Redactor r = new Redactor();
        r.loadDenylist(DEFAULT_DENYLIST);
        return r;
    }

    /** Loads values from the optional denylist file (one value per line). */
    public void loadDenylist(Path file) {
        if (file == null || !Files.exists(file)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(file)) {
                String v = line.trim();
                if (v.length() >= 4 && !v.startsWith("#")) {
                    denylist.add(v);
                }
            }
        } catch (IOException e) {
            System.err.println("Uyarı: denylist okunamadı (" + file + "): " + e.getMessage());
        }
    }

    /** Adds known secret values to the denylist (only those >= 4 characters). */
    public void addSecrets(Iterable<String> values) {
        for (String v : values) {
            if (v != null && v.trim().length() >= 4) {
                denylist.add(v.trim());
            }
        }
    }

    /** Redacts a single piece of text. */
    public String redact(String input) {
        if (input == null) {
            return null;
        }
        String out = input;

        // Exact secret values first (most specific)
        for (String secret : denylist) {
            out = out.replace(secret, "[REDACTED]");
        }

        // Then patterns
        for (NamedPattern p : PATTERNS) {
            out = p.pattern().matcher(out).replaceAll("[REDACTED:" + p.name() + "]");
        }

        return out;
    }
}

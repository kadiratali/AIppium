package helpers;

import org.openqa.selenium.By;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Proposes a replacement locator for a broken one by scoring every element in
 * the current page source against the locator's last known-good attributes
 * (see {@link LocatorCache}). Pure XML/string logic - no driver access and no
 * dependencies beyond the JDK parser.
 * <p>
 * A candidate is accepted only if its weighted score reaches {@code threshold}
 * AND it beats the second-best candidate by at least {@code margin}; ambiguous
 * matches (e.g. similar list rows) are rejected so the test fails normally
 * instead of healing onto the wrong element.
 */
public final class LocatorHealer {

    private static final Logger logger = LoggerFactory.getLogger(LocatorHealer.class);

    /**
     * Identity attributes and their weights. bounds/index are deliberately
     * excluded: they change with screen size and list order.
     */
    private static final Map<String, Double> WEIGHTS = Map.of(
            "resource-id", 0.40,
            "content-desc", 0.25,
            "text", 0.20,
            "class", 0.15);

    /** Matching on a single attribute is too risky; require at least this many. */
    private static final int MIN_SIGNALS = 2;

    private final double threshold;
    private final double margin;

    public LocatorHealer(double threshold, double margin) {
        this.threshold = threshold;
        this.margin = margin;
    }

    public record HealResult(By by, double score) {
    }

    private record Scored(Map<String, String> attrs, double score) {
    }

    public Optional<HealResult> heal(Map<String, String> baseline, String pageSourceXml) {
        Map<String, String> signals = usableSignals(baseline);
        if (signals.size() < MIN_SIGNALS) {
            logger.debug("Healing skipped: baseline has fewer than {} usable attributes: {}",
                    MIN_SIGNALS, baseline);
            return Optional.empty();
        }

        List<Map<String, String>> candidates = parseCandidates(pageSourceXml);
        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        // Same widget class first (Button stays Button far more often than ids
        // survive a rename); widen to everything only if that yields nothing.
        String baseClass = signals.get("class");
        List<Map<String, String>> pool = baseClass == null ? candidates
                : candidates.stream().filter(c -> baseClass.equals(c.get("class"))).toList();
        if (pool.isEmpty()) {
            pool = candidates;
        }

        List<Scored> scored = pool.stream()
                .map(c -> new Scored(c, score(signals, c)))
                .sorted(Comparator.comparingDouble(Scored::score).reversed())
                .toList();

        Scored best = scored.get(0);
        if (best.score() < threshold) {
            logger.info("Healing rejected: best candidate score {} below threshold {} ({})",
                    String.format("%.2f", best.score()), threshold, best.attrs());
            return Optional.empty();
        }
        if (scored.size() > 1 && best.score() - scored.get(1).score() < margin) {
            logger.info("Healing rejected: ambiguous candidates (best {} vs second {})",
                    String.format("%.2f", best.score()), String.format("%.2f", scored.get(1).score()));
            return Optional.empty();
        }

        return buildBy(best.attrs()).map(by -> new HealResult(by, best.score()));
    }

    /** Keeps only weighted, non-blank baseline attributes. */
    private static Map<String, String> usableSignals(Map<String, String> baseline) {
        Map<String, String> signals = new HashMap<>();
        baseline.forEach((key, value) -> {
            if (WEIGHTS.containsKey(key) && value != null && !value.isBlank()) {
                signals.put(key, value);
            }
        });
        return signals;
    }

    /**
     * Weighted similarity of a candidate against the baseline signals, with the
     * weights renormalized over the attributes the baseline actually has.
     */
    private static double score(Map<String, String> signals, Map<String, String> candidate) {
        double total = 0;
        double weightSum = 0;
        for (Map.Entry<String, String> signal : signals.entrySet()) {
            double weight = WEIGHTS.get(signal.getKey());
            weightSum += weight;
            total += weight * similarity(signal.getKey(), signal.getValue(),
                    candidate.getOrDefault(signal.getKey(), ""));
        }
        return total / weightSum;
    }

    private static double similarity(String attribute, String baselineValue, String candidateValue) {
        if (candidateValue == null || candidateValue.isBlank()) {
            return 0;
        }
        if ("class".equals(attribute)) {
            return baselineValue.equals(candidateValue) ? 1 : 0;
        }
        String a = baselineValue;
        String b = candidateValue;
        if ("resource-id".equals(attribute)) {
            // Compare only the part after "package:id/" so a package change
            // doesn't drown out an otherwise identical id.
            a = idSuffix(a);
            b = idSuffix(b);
        }
        return normalizedLevenshtein(a, b);
    }

    private static String idSuffix(String id) {
        int slash = id.lastIndexOf('/');
        return slash >= 0 ? id.substring(slash + 1) : id;
    }

    private static double normalizedLevenshtein(String a, String b) {
        if (a.equals(b)) {
            return 1;
        }
        int max = Math.max(a.length(), b.length());
        return max == 0 ? 1 : 1.0 - (double) levenshtein(a, b) / max;
    }

    private static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[b.length()];
    }

    /** Extracts every element's identity attributes from a UiAutomator2/XCUITest page source. */
    private static List<Map<String, String>> parseCandidates(String xml) {
        List<Map<String, String>> out = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document doc = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            collect(doc.getDocumentElement(), out);
        } catch (Exception e) {
            logger.warn("Page source could not be parsed for healing: {}", e.getMessage());
        }
        return out;
    }

    private static void collect(Element node, List<Map<String, String>> out) {
        String className = node.getAttribute("class");
        if (className.isBlank()) {
            className = node.getTagName();
        }
        if (!"hierarchy".equals(className)) {
            Map<String, String> attrs = new HashMap<>();
            attrs.put("class", className);
            putIfPresent(attrs, node, "resource-id");
            putIfPresent(attrs, node, "content-desc");
            putIfPresent(attrs, node, "text");
            out.add(attrs);
        }
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element element) {
                collect(element, out);
            }
        }
    }

    private static void putIfPresent(Map<String, String> attrs, Element node, String name) {
        String value = node.getAttribute(name);
        if (!value.isBlank()) {
            attrs.put(name, value);
        }
    }

    /** Builds a precise xpath from the winning candidate's own attributes. */
    private static Optional<By> buildBy(Map<String, String> candidate) {
        String tag = candidate.getOrDefault("class", "*");
        List<String> conditions = new ArrayList<>();
        for (String attr : List.of("resource-id", "content-desc", "text")) {
            String value = candidate.get(attr);
            if (value != null && !value.isBlank()) {
                conditions.add("@" + attr + "=" + xpathLiteral(value));
            }
        }
        if (conditions.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(By.xpath("//" + tag + "[" + String.join(" and ", conditions) + "]"));
    }

    private static String xpathLiteral(String value) {
        if (!value.contains("'")) {
            return "'" + value + "'";
        }
        if (!value.contains("\"")) {
            return "\"" + value + "\"";
        }
        // Contains both quote kinds: stitch it together with concat().
        return "concat('" + value.replace("'", "', \"'\", '") + "')";
    }
}

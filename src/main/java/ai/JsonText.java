package ai;

/**
 * Helper that extracts a bare JSON body from a model response.
 * Even when we ask the model for "JSON only", it sometimes wraps the
 * response in a markdown fence or adds surrounding prose; this class tolerates that.
 */
final class JsonText {

    private JsonText() {
    }

    /**
     * Strips ```json ... ``` fences and returns the JSON object between
     * the first '{' and the last '}'.
     */
    static String extractJsonObject(String raw) {
        String s = raw.trim();

        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            if (firstNewline >= 0) {
                s = s.substring(firstNewline + 1);
            }
            int fenceEnd = s.lastIndexOf("```");
            if (fenceEnd >= 0) {
                s = s.substring(0, fenceEnd);
            }
            s = s.trim();
        }

        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start < 0 || end < 0 || end < start) {
            throw new RuntimeException("Model yanıtında JSON bulunamadı:\n" + raw);
        }
        return s.substring(start, end + 1);
    }
}

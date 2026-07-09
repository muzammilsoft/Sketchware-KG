package pro.sketchware.activities.chat.port;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pro.sketchware.util.SemanticFileSearcher;
import pro.sketchware.util.SketchwareFileDecryptor;

/**
 * Android implementation of the AI-regex workflow sketched by Void.
 */
public final class VoidPortAiRegexService {
    private VoidPortAiRegexService() {
    }

    public static final class RegexPlan {
        public final String regex;
        public final String replacement;
        public final String explanation;
        public final boolean caseInsensitive;

        RegexPlan(String regex, String replacement, String explanation, boolean caseInsensitive) {
            this.regex = regex == null ? "" : regex;
            this.replacement = replacement == null ? "" : replacement;
            this.explanation = explanation == null ? "" : explanation;
            this.caseInsensitive = caseInsensitive;
        }

        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("regex", regex);
                obj.put("replacement", replacement);
                obj.put("explanation", explanation);
                obj.put("caseInsensitive", caseInsensitive);
            } catch (Exception ignored) {
            }
            return obj;
        }
    }

    public static RegexPlan generateRegex(Context context, String task, String sampleText,
                                          String replacementTask) throws Exception {
        String systemPrompt = "You generate Java Pattern-compatible regex for Sketchware KG. "
                + "Return compact JSON only, with keys regex, replacement, explanation, caseInsensitive. "
                + "Do not include markdown.";
        String userPrompt = "Task:\n" + safe(task)
                + "\n\nSample text:\n" + trim(sampleText, 4000)
                + "\n\nReplacement intent, if any:\n" + safe(replacementTask)
                + "\n\nRules:\n"
                + "- regex must be valid for java.util.regex.Pattern.\n"
                + "- replacement must be valid for Matcher.replaceAll, or empty when not requested.\n"
                + "- Keep regex precise and avoid catastrophic backtracking.\n"
                + "- Return only JSON.";
        VoidPortLlmRequestService.TextResult result = VoidPortLlmRequestService.completeText(
                context,
                systemPrompt,
                userPrompt,
                512,
                0.1,
                List.of()
        );
        JSONObject json = parseJsonObject(result.text);
        String regex = json.optString("regex", "");
        if (regex.isEmpty()) {
            throw new Exception("AI did not return a regex.");
        }
        boolean caseInsensitive = json.optBoolean("caseInsensitive", false);
        validateRegex(regex, caseInsensitive);
        return new RegexPlan(
                regex,
                json.optString("replacement", ""),
                json.optString("explanation", ""),
                caseInsensitive
        );
    }

    public static JSONObject search(Context context, String scId, String task, String searchInFolder,
                                    int maxFiles) throws Exception {
        RegexPlan plan = generateRegex(context, task, "", "");
        List<SemanticFileSearcher.SearchResult> candidates =
                SemanticFileSearcher.searchByContentRegex(plan.regex, scId);
        JSONArray matches = new JSONArray();
        int limit = Math.max(1, maxFiles <= 0 ? 20 : maxFiles);
        for (SemanticFileSearcher.SearchResult candidate : candidates) {
            if (candidate == null || candidate.filePath == null) {
                continue;
            }
            if (searchInFolder != null && !searchInFolder.trim().isEmpty()
                    && !candidate.filePath.contains(searchInFolder.trim())) {
                continue;
            }
            matches.put(new JSONObject()
                    .put("uri", candidate.filePath)
                    .put("relevance", candidate.relevance)
                    .put("preview", candidate.snippet));
            if (matches.length() >= limit) {
                break;
            }
        }
        return new JSONObject()
                .put("plan", plan.toJson())
                .put("matches", matches);
    }

    public static JSONObject replacePreview(Context context, String scId, String uri, String task,
                                            String replacementTask, int maxMatches) throws Exception {
        String content = SketchwareFileDecryptor.decryptFile(scId, uri);
        if (content == null) {
            throw new Exception("File not found or could not be decrypted: " + uri);
        }
        RegexPlan plan = generateRegex(context, task, trim(content, 4000), replacementTask);
        if (plan.replacement.isEmpty()) {
            throw new Exception("AI did not return a replacement.");
        }
        Pattern pattern = compile(plan.regex, plan.caseInsensitive);
        Matcher matcher = pattern.matcher(content);
        JSONArray previews = new JSONArray();
        int limit = Math.max(1, maxMatches <= 0 ? 20 : maxMatches);
        StringBuffer replacementBuffer = new StringBuffer();
        int previousAppendPosition = 0;
        while (matcher.find() && previews.length() < limit) {
            int beforeAppendLength = replacementBuffer.length();
            String original = matcher.group();
            matcher.appendReplacement(replacementBuffer, plan.replacement);
            int literalLength = matcher.start() - previousAppendPosition;
            String replacement = replacementBuffer.substring(Math.min(replacementBuffer.length(), beforeAppendLength + literalLength));
            previousAppendPosition = matcher.end();
            previews.put(new JSONObject()
                    .put("start", matcher.start())
                    .put("end", matcher.end())
                    .put("original", trim(original, 240))
                    .put("replacementPreview", trim(replacement, 240)));
        }
        return new JSONObject()
                .put("plan", plan.toJson())
                .put("uri", uri)
                .put("previews", previews);
    }

    public static Pattern validateRegex(String regex, boolean caseInsensitive) {
        return compile(regex, caseInsensitive);
    }

    private static Pattern compile(String regex, boolean caseInsensitive) {
        int flags = Pattern.MULTILINE;
        if (caseInsensitive) {
            flags |= Pattern.CASE_INSENSITIVE;
        }
        return Pattern.compile(regex, flags);
    }

    private static JSONObject parseJsonObject(String raw) throws Exception {
        String text = raw == null ? "" : raw.trim();
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                text = text.substring(firstNewline + 1, lastFence).trim();
            }
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            text = text.substring(start, end + 1);
        }
        return new JSONObject(text);
    }

    private static String trim(String value, int maxChars) {
        String safeValue = safe(value);
        return safeValue.length() <= maxChars ? safeValue : safeValue.substring(0, maxChars) + "\n... truncated ...";
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

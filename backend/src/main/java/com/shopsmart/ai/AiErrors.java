package com.shopsmart.ai;

import java.util.Locale;

/** Turns provider/database exceptions into short, safe, actionable messages (never echoes keys or stack traces). */
public final class AiErrors {
    private AiErrors() {}

    public static String explain(Throwable e) {
        StringBuilder sb = new StringBuilder();
        int depth = 0;
        for (Throwable t = e; t != null && depth < 6; t = t.getCause(), depth++) {
            sb.append(t.getClass().getSimpleName()).append(' ').append(t.getMessage()).append(' ');
        }
        String m = sb.toString().toLowerCase(Locale.ROOT);
        if (m.contains("api key") || m.contains("api_key") || m.contains("permission_denied") || m.contains("401")
                || m.contains("403") || m.contains("unauthenticated") || m.contains("not-configured")) {
            return "Gemini API key is missing or invalid. Set the GEMINI_API_KEY environment variable and restart the backend.";
        }
        if (m.contains("429") || m.contains("resource_exhausted") || m.contains("quota") || m.contains("too many requests")) {
            return "Gemini quota or rate limit reached. Wait a minute, or set GEMINI_MODEL=gemini-2.5-flash and restart.";
        }
        if (m.contains("404") || m.contains("is not found") || m.contains("not_found")) {
            return "Gemini model not found. Check GEMINI_MODEL and GEMINI_EMBEDDING_MODEL.";
        }
        if (m.contains("timed out") || m.contains("timeout") || m.contains("unknownhost") || m.contains("connect")) {
            return "Cannot reach Gemini (network problem or timeout). Check your internet connection and try again.";
        }
        if (m.contains("psqlexception") || m.contains("policy_documents") || m.contains("vector")) {
            return "Database error while storing or searching embeddings. Run: DROP TABLE policy_documents; DELETE FROM documents; and restart.";
        }
        return "Unexpected AI/provider error (see the backend console for details).";
    }
}

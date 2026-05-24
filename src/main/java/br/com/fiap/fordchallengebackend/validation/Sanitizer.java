package br.com.fiap.fordchallengebackend.validation;

public final class Sanitizer {

    private Sanitizer() {}

    public static String sanitizeAgainstXss(String input) {
        if (input == null) return "";
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;");
    }

    public static String sanitizeAgainstSqlInjection(String input) {
        if (input == null) return "";
        var result = input
            .replace("'", "''")
            .replace("--", "")
            .replaceAll("/\\*.*?\\*/", "")
            .replace(";", "");
        return result;
    }

    public static String sanitizeAll(String input) {
        if (input == null) return "";
        return sanitizeAgainstXss(sanitizeAgainstSqlInjection(input));
    }
}

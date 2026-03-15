package com.rag.app.shared.utils;

public final class StringUtils {
    private StringUtils() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}

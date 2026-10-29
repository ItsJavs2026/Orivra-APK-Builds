package com.chroomareborn.keyboard;

/**
 * UI-only status text; never accesses editor contents or stores personal data.
 * Shows BOTH the chosen palette mode and current light/dark appearance.
 */
public final class ThemeStatusLabel {
    private ThemeStatusLabel() {}

    public static String compact(String modeName, boolean dark) {
        return "Theme: " + safe(modeName) + " • " + (dark ? "Dark" : "Light");
    }

    public static String details(String modeName, boolean dark,
                                 boolean followsSystem, String appliedSource) {
        String selected=compact(modeName,dark);
        String source=safe(appliedSource);
        return selected + " • " +
                (followsSystem ? "Follows system" : "Manual light/dark") +
                (source.equals("Unknown") ? "" : " • Applied: " + source);
    }

    private static String safe(String text) {
        return text==null || text.trim().isEmpty() ? "Unknown" : text.trim();
    }
}

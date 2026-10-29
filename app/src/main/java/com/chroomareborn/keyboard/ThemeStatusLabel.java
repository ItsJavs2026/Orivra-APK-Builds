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

    /**
     * Readable in the expanded toolbar's compact theme control. The full
     * selected name and applied source remain in its accessibility label.
     * D/L = resolved appearance, not the user's manual/system preference.
     */
    public static String expandedChip(String selectedMode,boolean dark) {
        String name=safe(selectedMode);
        String compact;
        switch(name) {
            case "App Match — Shaded": compact="Shaded"; break;
            case "App Match": compact="Match"; break;
            case "Material You": compact="MYou"; break;
            case "Clean Neutral": compact="Clean"; break;
            default: compact=name;
        }
        if(compact.length()>8)compact=compact.substring(0,8);
        return compact+" "+(dark?"D":"L");
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

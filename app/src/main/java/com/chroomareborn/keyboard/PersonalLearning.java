package com.chroomareborn.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

/**
 * Local, opt-in learning from explicitly accepted corrections only.
 * No raw sentences, surrounding text, or keystroke history are stored.
 * Sensitive editors must never call recordAcceptance.
 */
final class PersonalLearning {
    static final String PREFS = "orivra_personal_learning_v1";
    static final String ENABLED = "enabled";
    private static final String PREFIX = "accepted:";
    private static final int MAX_ENTRIES = 300;
    private PersonalLearning() {}

    static boolean enabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(ENABLED, false);
    }

    static void setEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(ENABLED, enabled).apply();
    }

    static void clear(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }

    static void recordAcceptance(Context context, String typed, String accepted) {
        if (!enabled(context) || typed == null || accepted == null) return;
        String input = typed.trim().toLowerCase(Locale.ROOT);
        String output = accepted.trim().toLowerCase(Locale.ROOT);
        // Restrict initial learning to short single-word corrections.
        // Sentence and phrase learning needs separate privacy safeguards.
        if (!input.matches("[a-z]{2,24}") || !output.matches("[a-z]{2,24}")
                || input.equals(output)) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String key = PREFIX + input + ":" + output;
        if (!prefs.contains(key) && countEntries(prefs) >= MAX_ENTRIES) return;
        prefs.edit().putInt(key, Math.min(1000, prefs.getInt(key, 0) + 1)).apply();
    }

    static String preferredCorrection(Context context, String typed) {
        if (!enabled(context) || typed == null) return null;
        String input = typed.toLowerCase(Locale.ROOT);
        if (!input.matches("[a-z]{2,24}")) return null;
        String prefix = PREFIX + input + ":";
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String best = null;
        int bestCount = 1; // Require at least two intentional acceptances.
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            if (!entry.getKey().startsWith(prefix) || !(entry.getValue() instanceof Integer))
                continue;
            int count = (Integer) entry.getValue();
            if (count > bestCount) {
                best = entry.getKey().substring(prefix.length());
                bestCount = count;
            }
        }
        return best;
    }

    /** Safe aggregate diagnostics: no typed words, phrases, or editor content. */
    static String diagnosticSummary(Context context) {
        SharedPreferences prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        int entries=0;
        int acceptances=0;
        int reinforced=0;
        for(Map.Entry<String,?> entry:prefs.getAll().entrySet()) {
            if(!entry.getKey().startsWith(PREFIX) || !(entry.getValue() instanceof Integer))
                continue;
            entries++;
            int count=(Integer)entry.getValue();
            acceptances+=count;
            if(count>=2)reinforced++;
        }
        return "Orivra local learning v1\n"+
                "Enabled: "+(enabled(context)?"yes":"no")+"\n"+
                "Distinct accepted correction pairs: "+entries+"\n"+
                "Total accepted corrections: "+acceptances+"\n"+
                "Reinforced pairs (2+): "+reinforced+"\n"+
                "Stored only on device. No raw text included.";
    }

    /** Explicitly inspect learned correction pairs in the settings screen only. */
    static String learnedPairs(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        java.util.List<String> pairs = new java.util.ArrayList<>();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            if (!entry.getKey().startsWith(PREFIX) || !(entry.getValue() instanceof Integer)) continue;
            String pair = entry.getKey().substring(PREFIX.length());
            int separator = pair.indexOf(':');
            if (separator < 1) continue;
            pairs.add(pair.substring(0, separator) + " → " + pair.substring(separator + 1)
                    + " (" + entry.getValue() + ")");
        }
        java.util.Collections.sort(pairs);
        if (pairs.isEmpty()) return "No learned corrections yet.";
        StringBuilder output = new StringBuilder();
        for (String pair : pairs) output.append(pair).append('\n');
        return output.toString().trim();
    }

    static int countEntries(SharedPreferences prefs) {
        int count = 0;
        for (String key : prefs.getAll().keySet())
            if (key.startsWith(PREFIX)) count++;
        return count;
    }
}

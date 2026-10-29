package com.chroomareborn.keyboard;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Experimental offline trigram language model: "word1<TAB>word2<TAB>next<TAB>count".
 * Independent implementation, no user data or network, not yet connected to IME.
 * Any shipped training data needs separate provenance and license review.
 */
final class NextWordTrigramModel {
    private static final int MAX_LINES = 100_000;
    private static final long MAX_COUNT = 1_000_000_000L;
    private final Map<String, List<String>> byContext;
    private final int entries;

    private NextWordTrigramModel(Map<String, List<String>> byContext, int entries) {
        this.byContext = byContext;
        this.entries = entries;
    }

    private static String word(String token) {
        if (token == null) return null;
        String value = token.trim().toLowerCase(Locale.ROOT);
        return value.matches("[a-z][a-z']{0,31}") ? value : null;
    }

    /** Reads a bounded, deterministic, offline TSV fixture or approved model. */
    static NextWordTrigramModel fromTsv(Reader source) throws IOException {
        if (source == null) throw new IllegalArgumentException("source must not be null");
        Map<String, Map<String, Long>> counts = new HashMap<>();
        int entries = 0;
        try (BufferedReader br = new BufferedReader(source)) {
            String line;
            int lines = 0;
            while ((line = br.readLine()) != null) {
                if (++lines > MAX_LINES) throw new IOException("Model line limit exceeded");
                if (line.isEmpty() || line.charAt(0) == '#') continue;
                String[] fields = line.split("\\t", -1);
                if (fields.length != 4) continue;
                String first = word(fields[0]);
                String second = word(fields[1]);
                String next = word(fields[2]);
                if (first == null || second == null || next == null) continue;
                long frequency;
                try { frequency = Long.parseLong(fields[3].trim()); }
                catch (NumberFormatException ignored) { continue; }
                if (frequency <= 0 || frequency > MAX_COUNT) continue;
                String context = first + " " + second;
                Map<String, Long> continuations = counts.get(context);
                if (continuations == null) {
                    continuations = new HashMap<>();
                    counts.put(context, continuations);
                }
                Long previous = continuations.get(next);
                if (previous == null) entries++;
                long summed = previous == null ? frequency :
                        Math.min(MAX_COUNT, previous + frequency);
                continuations.put(next, summed);
            }
        }
        Map<String, List<String>> ranked = new HashMap<>();
        for (Map.Entry<String, Map<String, Long>> row : counts.entrySet()) {
            Map<String, Long> frequencies = row.getValue();
            ArrayList<String> choices = new ArrayList<>(frequencies.keySet());
            choices.sort(Comparator.<String>comparingLong(x -> -frequencies.get(x))
                    .thenComparing(Comparator.naturalOrder()));
            ranked.put(row.getKey(), Collections.unmodifiableList(choices));
        }
        return new NextWordTrigramModel(Collections.unmodifiableMap(ranked), entries);
    }

    /** Current sentence only; requires whitespace after a completed word. */
    List<String> suggest(String committedContext, int limit) {
        if (committedContext == null || limit <= 0 || committedContext.isEmpty())
            return Collections.emptyList();
        if (!Character.isWhitespace(committedContext.charAt(committedContext.length()-1)))
            return Collections.emptyList();
        String context = committedContext.toLowerCase(Locale.ROOT);
        int boundary = -1;
        for (int i=0; i<context.length(); i++) {
            char c = context.charAt(i);
            if (c=='.' || c=='!' || c=='?' || c==';' || c=='\n' || c=='\r') boundary = i;
        }
        String[] tokens = context.substring(boundary+1).trim().split("[^a-z']+");
        if (tokens.length < 2) return Collections.emptyList();
        String first = word(tokens[tokens.length-2]);
        String second = word(tokens[tokens.length-1]);
        if (first == null || second == null) return Collections.emptyList();
        List<String> matches = byContext.get(first + " " + second);
        if (matches == null) return Collections.emptyList();
        return new ArrayList<>(matches.subList(0, Math.min(limit, matches.size())));
    }

    int entries() { return entries; }
    int contexts() { return byContext.size(); }
}

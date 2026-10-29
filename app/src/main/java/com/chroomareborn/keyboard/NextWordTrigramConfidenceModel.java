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
 * Experimental conservative trigram gate. Not connected to the live IME.
 * Shares the same offline TSV contract as NextWordTrigramModel.
 *
 * "Confidence" below is a count-based heuristic, NOT a calibrated probability.
 */
final class NextWordTrigramConfidenceModel {
    private static final int MAX_LINES = 100_000;
    private static final long MAX_COUNT = 1_000_000_000L;
    private final Map<String, Context> contexts;

    private static final class Candidate {
        final String word;
        final long count;
        Candidate(String word, long count) { this.word = word; this.count = count; }
    }
    private static final class Context {
        final List<Candidate> candidates;
        final long total;
        Context(List<Candidate> candidates, long total) {
            this.candidates = candidates;
            this.total = total;
        }
    }
    private NextWordTrigramConfidenceModel(Map<String, Context> contexts) {
        this.contexts = Collections.unmodifiableMap(contexts);
    }

    private static String validWord(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toLowerCase(Locale.ROOT);
        return s.matches("[a-z][a-z']{0,31}") ? s : null;
    }

    static NextWordTrigramConfidenceModel fromTsv(Reader input) throws IOException {
        if (input == null) throw new IllegalArgumentException("input required");
        Map<String, Map<String, Long>> raw = new HashMap<>();
        try (BufferedReader br = new BufferedReader(input)) {
            String line;
            int lineCount = 0;
            while ((line = br.readLine()) != null) {
                if (++lineCount > MAX_LINES) throw new IOException("line limit exceeded");
                if (line.isEmpty() || line.charAt(0) == '#') continue;
                String[] fields = line.split("\\t", -1);
                if (fields.length != 4) continue;
                String a = validWord(fields[0]);
                String b = validWord(fields[1]);
                String c = validWord(fields[2]);
                if (a == null || b == null || c == null) continue;
                long count;
                try { count = Long.parseLong(fields[3].trim()); }
                catch (NumberFormatException ignored) { continue; }
                if (count < 1 || count > MAX_COUNT) continue;
                String key = a + " " + b;
                Map<String, Long> group = raw.get(key);
                if (group == null) { group = new HashMap<>(); raw.put(key, group); }
                long old = group.containsKey(c) ? group.get(c) : 0L;
                group.put(c, Math.min(MAX_COUNT, old + count));
            }
        }
        Map<String, Context> out = new HashMap<>();
        for (Map.Entry<String, Map<String, Long>> group : raw.entrySet()) {
            List<Candidate> candidates = new ArrayList<>();
            long total = 0;
            for (Map.Entry<String, Long> entry : group.getValue().entrySet()) {
                candidates.add(new Candidate(entry.getKey(), entry.getValue()));
                total += entry.getValue(); // <= 100000 * 1e9, fits long
            }
            candidates.sort(Comparator.<Candidate>comparingLong(c -> -c.count)
                    .thenComparing(c -> c.word));
            out.put(group.getKey(), new Context(Collections.unmodifiableList(candidates), total));
        }
        return new NextWordTrigramConfidenceModel(out);
    }

    /**
     * Only returns candidates when top count has sufficient support and
     * dominates its context. Lower-ranked words must also meet support.
     * Thresholds are deliberately conservative initial heuristics.
     */
    List<String> suggest(String committed, int limit) {
        if (committed == null || committed.isEmpty() || limit <= 0 ||
                !Character.isWhitespace(committed.charAt(committed.length() - 1)))
            return Collections.emptyList();
        String lower = committed.toLowerCase(Locale.ROOT);
        int boundary = -1;
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (c == '.' || c == '!' || c == '?' || c == ';' || c == '\n' || c == '\r')
                boundary = i;
        }
        String[] words = lower.substring(boundary + 1).trim().split("[^a-z']+");
        if (words.length < 2) return Collections.emptyList();
        String a = validWord(words[words.length - 2]);
        String b = validWord(words[words.length - 1]);
        if (a == null || b == null) return Collections.emptyList();
        Context context = contexts.get(a + " " + b);
        if (context == null || context.candidates.isEmpty()) return Collections.emptyList();
        Candidate first = context.candidates.get(0);
        if (context.total < 5 || first.count < 3 ||
                first.count * 2 < context.total) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        for (Candidate candidate : context.candidates) {
            if (out.size() >= limit) break;
            if (candidate.count < 2 || candidate.count * 4 < context.total) break;
            out.add(candidate.word);
        }
        return out;
    }

    int contexts() { return contexts.size(); }
}

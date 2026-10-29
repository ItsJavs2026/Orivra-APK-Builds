package com.chroomareborn.keyboard;

import java.util.Locale;
import java.util.List;

/**
 * Candidate-only diagnostics formatting for non-sensitive, user-initiated
 * inspection. Nothing is persisted, sent, or inserted in the editor.
 * Predictor ranking is left unchanged; this merely expands the full text.
 */
final class CandidatePreviewReport {
    private CandidatePreviewReport() {}
    static boolean supported(String text) {
        return text!=null && text.matches("[A-Za-z]{1,65}");
    }
    static String instructions() {
        return "Use 1–65 letters without spaces in the test field. "
                +"This inspector is intended only for non-sensitive phrases.";
    }
    // Expected text is optional, ephemeral, and is never supplied to the predictor.
    static boolean supportedExpected(String expected) {
        return expected==null || expected.trim().isEmpty()
                || (expected.length()<=100
                    && expected.matches("[A-Za-z'’ ]+")
                    && expected.matches(".*[A-Za-z].*"));
    }
    static String normalizeExpected(String text) {
        return text.toLowerCase(Locale.US).replace("’","")
                .replace("'","").trim().replaceAll("\\s+"," ");
    }
    static int expectedRank(String expected,List<String> candidates) {
        if(expected==null || expected.trim().isEmpty()
                || !supportedExpected(expected) || candidates==null)return -1;
        String want=normalizeExpected(expected);
        for(int i=0;i<candidates.size();i++)
            if(candidates.get(i)!=null
                    && want.equals(normalizeExpected(candidates.get(i))))return i+1;
        return -1;
    }
    private static String verdict(int rank) {
        if(rank==1)return "TOP-1";
        if(rank>=2 && rank<=3)return "TOP-3 (rank "+rank+")";
        if(rank>3)return "OUTSIDE TOP-3 (rank "+rank+")";
        return "MISS";
    }
    static String format(String typed,List<String> results) {
        return format(typed,results,null);
    }
    static String format(String typed,List<String> results,String expected) {
        if(!supported(typed))return instructions();
        if(!supportedExpected(expected))
            return "Expected phrase must contain 1–100 letters, spaces, or apostrophes. "
                    +"Use non-sensitive example text.";
        List<String> clean=SuggestionDisplayPolicy.compact(typed,results);
        StringBuilder out=new StringBuilder("Full prediction results (read-only)\n");
        if(expected!=null && !expected.trim().isEmpty()) {
            out.append("Intended phrase: ").append(expected.trim()).append("\n");
            out.append("Model exact-match ranking: ")
                    .append(verdict(expectedRank(expected,results))).append("\n");
            out.append("Visible candidate ranking: ")
                    .append(verdict(expectedRank(expected,clean))).append("\n");
            out.append("(Case and apostrophes ignored; word boundaries matter.)\n\n");
        }
        if(clean.isEmpty())out.append("No correction candidates yet.\n");
        int n=Math.min(clean.size(),8);
        for(int i=0;i<n;i++){
            out.append(i+1).append(". ").append(clean.get(i)).append("\n\n");
        }
        out.append("Original input (unchanged): ").append(typed).append("\n");
        out.append("This report is shown locally only; nothing was accepted or saved.");
        return out.toString();
    }
}

package com.chroomareborn.keyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.HashSet;

/**
 * Offline, pure-Java protections for correcting a completed word at the caret.
 * No edits are made by this class. InputConnection is re-read at tap time,
 * so stale suggestions cannot delete surrounding text.
 */
final class CursorWordCorrectionPolicy {
    static final class Plan {
        final int lettersBefore;
        final int lettersAfter;
        final String replacement;

        Plan(int lettersBefore,int lettersAfter,String replacement) {
            this.lettersBefore=lettersBefore;
            this.lettersAfter=lettersAfter;
            this.replacement=replacement;
        }
    }

    private CursorWordCorrectionPolicy() {}

    static Plan plan(CharSequence before,CharSequence after,String expected,
                     String replacement,boolean selected,boolean sensitive) {
        if(sensitive || selected || before==null || after==null ||
                expected==null || replacement==null || expected.length()<2 ||
                expected.length()>64 || replacement.length()<2 ||
                replacement.length()>64 || expected.equals(replacement) ||
                !singleWord(replacement))return null;

        int left=before.length(),right=0;
        while(left>0 && EnglishApostrophePolicy.isTokenChar(before.charAt(left-1)))left--;
        while(right<after.length() &&
                EnglishApostrophePolicy.isTokenChar(after.charAt(right)))right++;
        int beforeCount=before.length()-left;
        if(beforeCount+right==0)return null;
        // A 64-character snapshot can cut off a longer word. Refuse a
        // partial rewrite rather than replace only its visible fragment.
        if((left==0 && before.length()>=64) ||
                (right==after.length() && after.length()>=64))return null;
        String live=before.subSequence(left,before.length()).toString()+
                after.subSequence(0,right).toString();
        if(!expected.equals(live))return null;
        // Do not auto-insert a space: this is a replacement, not next-word
        // acceptance, even if the caret is at the document end.
        return new Plan(beforeCount,right,replacement);
    }

    /**
     * Preserve the case of the word actually being corrected, independent
     * of the keyboard's temporary Shift state. Corrections are not new
     * sentences: a lowercase typo in the middle of a sentence must remain
     * lowercase when the user taps its replacement.
     *
     * all-lower  corect -> correct
     * initial-up Corect -> Correct
     * all-upper  CORECT -> CORRECT
     * mixed-case iPhne -> keep the candidate's canonical spelling.
     */
    static String matchCase(String original,String suggestion) {
        if(original==null || original.isEmpty() || suggestion==null || suggestion.isEmpty())
            return suggestion;
        int letters=0, upper=0, lower=0;
        int first=original.codePointAt(0);
        for(int i=0;i<original.length();) {
            int cp=original.codePointAt(i);
            if(Character.isLetter(cp)) {
                letters++;
                if(Character.isUpperCase(cp))upper++;
                if(Character.isLowerCase(cp))lower++;
            }
            i+=Character.charCount(cp);
        }
        java.util.Locale root=java.util.Locale.ROOT;
        if(letters>0 && lower==letters) {
            String normalized=suggestion.toLowerCase(root);
            // A first-person contraction is always written with capital I:
            // im -> I'm, i've -> I've. Do not lower-case the chosen chip
            // simply because the text field committed the raw word as lower.
            if(normalized.startsWith("i'") || normalized.startsWith("i’"))
                return "I"+normalized.substring(1);
            return normalized;
        }
        if(letters>=2 && upper==letters)
            return suggestion.toUpperCase(root);
        if(Character.isUpperCase(first) && upper==1 && lower==letters-1) {
            String normalized=suggestion.toLowerCase(root);
            int initial=normalized.codePointAt(0);
            return new String(Character.toChars(Character.toTitleCase(initial)))+
                    normalized.substring(Character.charCount(initial));
        }
        // Preserve mixed-case proper names/brands instead of forcing
        // a lossy guessed capitalization pattern.
        return suggestion;
    }

    private static boolean singleWord(String value) {
        if(value==null || value.length()<2 || value.length()>64)return false;
        boolean previousLetter=false;
        for(int i=0;i<value.length();) {
            int codepoint=value.codePointAt(i);
            if(Character.isLetter(codepoint)) {
                previousLetter=true;
            } else if((codepoint=='\'' || codepoint==0x2019) &&
                    previousLetter && i+Character.charCount(codepoint)<value.length() &&
                    Character.isLetter(value.codePointAt(i+Character.charCount(codepoint)))) {
                previousLetter=false;
            } else return false;
            i+=Character.charCount(codepoint);
        }
        return previousLetter;
    }

    /**
     * Cursor-editing suggestions are replacements of ONE word, never
     * sentence completions or new words. Keep the original visible even
     * if the predictor offers no confident alternatives.
     */
    static List<String> safeOptions(String expected,List<String> predicted) {
        if(expected==null || expected.isEmpty())return Collections.emptyList();
        ArrayList<String> result=new ArrayList<>();
        result.add(expected);
        if(predicted==null)return result;
        HashSet<String> seen=new HashSet<>();
        seen.add(expected.toLowerCase(Locale.ROOT));
        for(String suggestion:predicted) {
            if(!singleWord(suggestion))continue;
            String normalized=suggestion.toLowerCase(Locale.ROOT);
            if(seen.add(normalized))result.add(suggestion);
            if(result.size()>=5)break;
        }
        return result;
    }
}

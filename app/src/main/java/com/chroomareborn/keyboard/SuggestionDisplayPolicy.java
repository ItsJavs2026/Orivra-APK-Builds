package com.chroomareborn.keyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pure display policy: keep the predictor's top rank, remove redundant
 * near-identical long chips, and preserve the original typed input.
 * No learning, corpus overrides, logging, or persistent text storage.
 */
final class SuggestionDisplayPolicy {
    private SuggestionDisplayPolicy() {}
    static boolean longSentence(String input,String candidate) {
        return input!=null && input.length()>=16
                && candidate!=null && candidate.indexOf(' ')>=0;
    }
    private static boolean sameEndingWithOneMissingLetter(String first,String second) {
        String[] a=first.split(" "),b=second.split(" ");
        if(a.length!=b.length || a.length<3)return false;
        for(int i=0;i<a.length-1;i++)
            if(!a[i].equals(b[i]))return false;
        String lastA=a[a.length-1],lastB=b[b.length-1];
        if(Math.abs(lastA.length()-lastB.length())!=1)return false;
        String shortWord=lastA.length()<lastB.length()?lastA:lastB;
        String longWord=lastA.length()>lastB.length()?lastA:lastB;
        return shortWord.length()>=4 && longWord.startsWith(shortWord);
    }
    /**
     * A long correction gets the readable first position, instead of hiding
     * behind a scroll-width raw-input chip. Short-word centered layout is
     * preserved. The raw input is always an explicit, tappable alternative.
     */
    static List<String> arrange(String input,List<String> options,boolean nextWordMode) {
        ArrayList<String> ranked=new ArrayList<>();
        if(options!=null)for(String candidate:options)
            if(candidate!=null && !candidate.isEmpty() && !ranked.contains(candidate))
                ranked.add(candidate);
        if(input==null || input.isEmpty() || ranked.isEmpty())return ranked;
        String best=nextWordMode?input:ranked.remove(0);
        if(nextWordMode)ranked.remove(input);
        ArrayList<String> arranged=new ArrayList<>();
        if(longSentence(input,best)) {
            arranged.add(best);
            if(!best.equalsIgnoreCase(input))arranged.add(input);
        }else{
            if(!best.equalsIgnoreCase(input))arranged.add(input);
            else if(!ranked.isEmpty())arranged.add(ranked.remove(0));
            arranged.add(best);
        }
        for(String candidate:ranked)
            if(!arranged.contains(candidate))arranged.add(candidate);
        if(!arranged.contains(input))arranged.add(input);
        return arranged;
    }
    static boolean usefulRiseAlternative(String input,String candidate) {
        if(input==null || candidate==null)return false;
        String normalized=candidate.trim().replaceAll("\\s+"," ");
        if(normalized.isEmpty())return false;
        // Avoid distracting Rise suggestions while correcting a single word.
        // For short input, the primary strip already contains the useful
        // correction; Rise should not invent unrelated words or boundaries.
        String rawInput=input.trim().replaceAll("\\s+","");
        if(input.trim().indexOf(' ')<0 && rawInput.length()<=8)return false;
        if(input.trim().indexOf(' ')<0 && rawInput.length()<=16
                && normalized.indexOf(' ')>=0
                && !normalized.replace(" ","").equalsIgnoreCase(rawInput))
            return false;
        String[] words=normalized.split(" ");
        if(words.length<2)return true;
        // Reject word-salad segmentations with excessive tiny fragments.
        int tiny=0;
        for(String word:words)if(word.length()<=2)tiny++;
        if(words.length>=4 && tiny*2>=words.length)return false;
        // A legitimate single-word prefix should not spawn speculative
        // multiword alternatives composed mostly of short fragments.
        String raw=input.trim().replaceAll("\\s+","");
        if(raw.length()>=10 && words.length>=4 && tiny>=2)return false;
        return true;
    }
    static boolean confidentMultiword(String input,String candidate) {
        if(input==null || candidate==null || input.trim().isEmpty())return false;
        String normalized=candidate.trim().replaceAll("\\s+"," ");
        String[] words=normalized.split(" ");
        if(words.length<2)return false;
        // A one-space guess is too weak to rearrange the entire strip.
        // Require at least three meaningful tokens or two substantial words.
        if(words.length==2 && (words[0].length()<3 || words[1].length()<4))return false;
        String raw=input.toLowerCase(Locale.US).replaceAll("[^a-z0-9]","");
        String prediction=normalized.toLowerCase(Locale.US).replaceAll("[^a-z0-9]","");
        if(raw.length()<8 || prediction.length()<8)return false;
        // Compare the typed letters with the predicted phrase while tolerating
        // punctuation, a contraction and a small number of typos.
        int maxDistance=Math.max(2,raw.length()/5);
        if(Math.abs(raw.length()-prediction.length())>maxDistance)return false;
        int[] prev=new int[prediction.length()+1];
        for(int j=0;j<prev.length;j++)prev[j]=j;
        for(int i=1;i<=raw.length();i++) {
            int[] next=new int[prediction.length()+1];
            next[0]=i;
            for(int j=1;j<=prediction.length();j++) {
                int cost=raw.charAt(i-1)==prediction.charAt(j-1)?0:1;
                next[j]=Math.min(Math.min(next[j-1]+1,prev[j]+1),prev[j-1]+cost);
            }
            prev=next;
        }
        return prev[prediction.length()]<=maxDistance;
    }
    static List<String> compact(String input,List<String> options) {
        ArrayList<String> out=new ArrayList<>();
        if(options==null)return out;
        for(String option:options) {
            if(option==null || option.trim().isEmpty())continue;
            String normalized=option.trim().replaceAll("\\s+"," ")
                    .toLowerCase(Locale.US);
            boolean duplicate=false;
            for(String previous:out) {
                String prior=previous.trim().replaceAll("\\s+"," ")
                        .toLowerCase(Locale.US);
                if(prior.equals(normalized)) { duplicate=true;break; }
                // Merge only a final-word truncation (e.g. "typin" /
                // "typing"). Shared prefixes alone are NOT duplicates:
                // "while waiting" and "while typing" are distinct meanings.
                if(longSentence(input,option) && longSentence(input,previous)
                        && sameEndingWithOneMissingLetter(normalized,prior)) {
                    duplicate=true;break;
                }
            }
            if(!duplicate)out.add(option);
        }
        return out;
    }
}

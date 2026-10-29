package com.chroomareborn.keyboard;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Research-only failure attribution for the PRE-FROZEN synthetic 297 audit.
 * Does not change prediction, import fixture answers into a model, or read
 * messages, preferences, clipboard, user typing, or editor text.
 *
 * This QA differentiates:
 * - missing vocabulary (gold words not in boot lexicon);
 * - gold output ranked 4-8 (already generated but under-ranked);
 * - gold output missing from all 8 offered suggestions;
 * - inherited accurate or safe exact-word behavior.
 * It cannot distinguish non-generated from beam-pruned candidates.
 */
public final class SentenceFlowFailureAttribution {
    private static final String[] GROUPS={
            "joined_exact", "two_words_one_each",
            "one_long_word_two", "one_short_word_two"};
    private static final class Stats {
        final String name;
        int total,top1,top3,top8,fullLexiconCoverage;
        int missingVocabulary,ranked4to8,unrecalledCovered,rejectedLength;
        int missingExactWord,unexpectedExactWord;
        final List<String> examples=new ArrayList<>();
        Stats(String n){name=n;}
        void record(String raw,String expected,List<String> options,
                boolean allKnown,boolean knownWord) {
            total++;
            boolean one=false,three=false,eight=false;
            for(int i=0;i<options.size();i++) {
                boolean match=eq(options.get(i),expected);
                if(i==0&&match)one=true;
                if(i<3&&match)three=true;
                if(i<8&&match)eight=true;
            }
            if(one)top1++;
            if(three)top3++;
            if(eight)top8++;
            if(allKnown)fullLexiconCoverage++;
            else missingVocabulary++;
            if(raw.length()>65)rejectedLength++;
            if(!three&&eight)ranked4to8++;
            if(!eight&&allKnown)unrecalledCovered++;
            if(knownWord&&!allKnown)missingExactWord++;
            if(knownWord&&allKnown&&!one)unexpectedExactWord++;
            if(!one&&examples.size()<7){
                String first=options.isEmpty()?"(empty)":options.get(0);
                examples.add("MISS "+name+" input="+raw+
                        " first="+first+" gold="+expected+
                        " completeVocabulary="+allKnown+
                        " top8Recall="+eight);
            }
        }
        String summary() {
            return String.format(Locale.ROOT,
                    "ATTRIBUTION %s n=%d top1=%d top3=%d top8=%d "+
                    "fullyKnown=%d missingLexicon=%d ranked4to8=%d "+
                    "coveredButNotOffered=%d lengthOver65=%d "+
                    "knownWordMissing=%d knownWordChanged=%d",
                    name,total,top1,top3,top8,fullLexiconCoverage,
                    missingVocabulary,ranked4to8,unrecalledCovered,rejectedLength,
                    missingExactWord,unexpectedExactWord);
        }
    }
    private static boolean eq(String actual,String expected){
        return actual.toLowerCase(Locale.ROOT).replaceAll("\\s+"," ").trim()
                .equals(expected.toLowerCase(Locale.ROOT).replaceAll("\\s+"," ").trim());
    }
    @SuppressWarnings("unchecked")
    private static Map<String,Integer> words()throws Exception{
        Field f=PredictionLite.class.getDeclaredField("WORDS");
        f.setAccessible(true);
        return (Map<String,Integer>)f.get(null);
    }
    private static Object fixture(String name)throws Exception{
        Field f=SentenceFlowBlind297Audit.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(null);
    }
    private static String[] corrupt(String[] words,String group)throws Exception{
        Method m=SentenceFlowBlind297Audit.class.getDeclaredMethod(
                "corrupt",String[].class,String.class);
        m.setAccessible(true);
        return (String[])m.invoke(null,(Object)words,group);
    }
    private static String joined(String[] words)throws Exception{
        Method m=SentenceFlowBlind297Audit.class.getDeclaredMethod("joined",String[].class);
        m.setAccessible(true);
        return (String)m.invoke(null,(Object)words);
    }
    private static boolean known(String expected,Map<String,Integer> lexicon){
        for(String token:expected.toLowerCase(Locale.ROOT).split(" ")){
            if(!lexicon.containsKey(token))return false;
        }
        return true;
    }
    public static void main(String[] args)throws Exception{
        Map<String,Integer> lexicon=words();
        PredictionLite.suggestions("hello");
        Map<String,Integer> unknown=new HashMap<>();
        Map<String,Stats> stats=new LinkedHashMap<>();
        String[] phrases=(String[])fixture("PHRASES");
        String[] completeWords=(String[])fixture("COMPLETE_WORDS");
        if(phrases.length!=20 || completeWords.length!=20)
            throw new AssertionError("Expected immutable frozen 100-case fixture");
        for(String group:GROUPS) {
            Stats bucket=new Stats(group);
            stats.put(group,bucket);
            for(String phrase:phrases) {
                String[] original=phrase.split(" ");
                String input=joined(corrupt(original,group));
                boolean complete=known(phrase,lexicon);
                if(!complete)
                    for(String word:original)if(!lexicon.containsKey(word))
                        unknown.merge(word,1,Integer::sum);
                bucket.record(input,phrase,PredictionLite.suggestions(input),complete,false);
            }
        }
        Stats wordsBucket=new Stats("known_words");
        stats.put("known_words",wordsBucket);
        for(String word:completeWords){
            boolean complete=lexicon.containsKey(word);
            if(!complete)unknown.merge(word,1,Integer::sum);
            wordsBucket.record(word,word,PredictionLite.suggestions(word),complete,true);
        }
        int total=0,top1=0,top3=0,top8=0,missing=0,coveredUnrecalled=0;
        for(Stats stat:stats.values()) {
            total+=stat.total;
            top1+=stat.top1;top3+=stat.top3;top8+=stat.top8;
            missing+=stat.missingVocabulary;
            coveredUnrecalled+=stat.unrecalledCovered;
            System.out.println(stat.summary());
            for(String example:stat.examples)System.out.println(example);
        }
        List<Map.Entry<String,Integer>> missingTokens=new ArrayList<>(unknown.entrySet());
        missingTokens.sort((a,b)->{
            int c=Integer.compare(b.getValue(),a.getValue());
            return c!=0?c:a.getKey().compareTo(b.getKey());
        });
        System.out.println("MISSING_LEXICON_WORDS distinct="+missingTokens.size());
        for(Map.Entry<String,Integer> e:missingTokens)
            System.out.println("MISSING_TOKEN "+e.getKey()+" freq="+e.getValue());
        System.out.printf(Locale.ROOT,
                "ATTRIBUTION ALL n=%d top1=%d top3=%d top8=%d "+
                "casesMissingLexicon=%d coveredButNotOffered=%d%n",
                total,top1,top3,top8,missing,coveredUnrecalled);
        if(total!=100)throw new AssertionError("Unexpected fixture drift "+total);
        if(top1>top3||top3>top8||top8>total)
            throw new AssertionError("Impossible accuracy totals");
        if(lexicon.isEmpty())throw new AssertionError("Empty lexicon");
    }
}

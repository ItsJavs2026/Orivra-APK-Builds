package com.chroomareborn.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Deliberately saved offline personal words, in this app's private storage.
 * Nothing is learned from raw typing, adjacent text, clipboard or sensitive fields.
 * No network, backup, telemetry or cross-application storage access.
 */
final class PersonalDictionary {
    private static final String PREFS="orivra_manual_dictionary_v2";
    private static final String PREFIX="word:";
    enum Add { ADDED,EXISTS,INVALID,FULL }
    private PersonalDictionary(){}

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
    }
    static Add add(Context ctx,String input) {
        String canonical=PersonalDictionaryPolicy.canonical(input);
        if(canonical==null)return Add.INVALID;
        SharedPreferences p=prefs(ctx);
        String key=PREFIX+canonical;
        if(p.contains(key))return Add.EXISTS;
        if(wordCount(ctx)>=PersonalDictionaryPolicy.MAX_WORDS)return Add.FULL;
        String display=PersonalDictionaryPolicy.acceptedDisplay(input);
        p.edit().putString(key,display).apply();
        return Add.ADDED;
    }
    static boolean remove(Context ctx,String input) {
        String canonical=PersonalDictionaryPolicy.canonical(input);
        if(canonical==null)return false;
        String key=PREFIX+canonical;
        SharedPreferences p=prefs(ctx);
        if(!p.contains(key))return false;
        p.edit().remove(key).apply();
        return true;
    }
    static int wordCount(Context ctx) {
        int total=0;
        for(String key:prefs(ctx).getAll().keySet())if(key.startsWith(PREFIX))total++;
        return total;
    }
    static List<String> savedWords(Context ctx) {
        List<String> words=new ArrayList<>();
        for(Map.Entry<String,?> e:prefs(ctx).getAll().entrySet()) {
            if(!e.getKey().startsWith(PREFIX) ||
                !(e.getValue() instanceof String))continue;
            String display=(String)e.getValue();
            if(PersonalDictionaryPolicy.canonical(display)!=null)
                words.add(display);
        }
        words.sort(Comparator.comparing(s->s.toLowerCase(Locale.ROOT)));
        if(words.size()>PersonalDictionaryPolicy.MAX_WORDS)
            return new ArrayList<>(words.subList(0,PersonalDictionaryPolicy.MAX_WORDS));
        return words;
    }
    static List<String> matching(Context ctx,String prefix) {
        String normalized=EnglishApostrophePolicy.prefix(prefix);
        if(normalized==null)return List.of();
        List<String> matches=new ArrayList<>();
        for(String word:savedWords(ctx)) {
            if(PersonalDictionaryPolicy.canonical(word).startsWith(normalized))
                matches.add(word);
            if(matches.size()>=PersonalDictionaryPolicy.MAX_SUGGESTIONS)break;
        }
        // Explicit exact word should outrank prefix completions.
        matches.sort((a,b)->{
            boolean ae=PersonalDictionaryPolicy.canonical(a).equals(
                    EnglishApostrophePolicy.canonical(prefix));
            boolean be=PersonalDictionaryPolicy.canonical(b).equals(
                    EnglishApostrophePolicy.canonical(prefix));
            if(ae!=be)return ae?-1:1;
            return Integer.compare(a.length(),b.length());
        });
        return matches;
    }
    static String summary(Context ctx) {
        return "Saved words: "+wordCount(ctx)+"/"+PersonalDictionaryPolicy.MAX_WORDS+
            ". Stored only on this device; never collected from typing.";
    }
}

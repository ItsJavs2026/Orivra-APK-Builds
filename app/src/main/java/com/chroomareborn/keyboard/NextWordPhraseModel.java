package com.chroomareborn.keyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Small, opt-in, offline phrase-aware continuations for common conversation
 * chains. These are curated language examples, not personal text or logs.
 *
 * Exact longest-suffix matching prevents generic bigrams such as "the -> best"
 * from overriding a more specific context like "I am testing the -> keyboard".
 * Unknown contexts deliberately fall back to the bundled seed model.
 */
final class NextWordPhraseModel {
    private NextWordPhraseModel() {}

    // Ordered continuations: original human-curated ranking, never user data.
    private static final String[][] PHRASES = {
        {"i am testing", "the", "it", "this"},
        {"i am testing the", "keyboard", "app", "new"},
        {"i am testing this", "keyboard", "app", "feature"},
        {"i am testing the keyboard", "and", "today", "now"},
        {"how are you", "doing", "feeling", "today"},
        {"how are you doing", "today", "now", "so"},
        {"what are you", "doing", "thinking", "looking"},
        {"what are you doing", "today", "now", "tonight"},
        {"can you", "help", "please", "check"},
        {"can you help", "me", "us", "with"},
        {"can you help me", "with", "find", "understand"},
        {"can you help me with", "the", "this", "my"},
        {"i need to", "go", "check", "finish"},
        {"i need to go", "home", "back", "now"},
        {"i need to check", "the", "my", "this"},
        {"i want to", "know", "see", "try"},
        {"i want to know", "what", "how", "if"},
        {"thank you", "so", "for", "very"},
        {"thank you for", "your", "the", "helping"},
        {"thank you for your", "help", "time", "support"},
        {"looking for", "a", "the", "something"},
        {"i am looking for", "a", "the", "something"},
        {"i am looking for a", "new", "good", "better"},
        {"going to", "be", "try", "get"},
        {"i am going to", "be", "try", "get"},
        {"i am going to be", "there", "working", "home"},
        {"let me", "know", "check", "see"},
        {"let me know", "if", "when", "what"},
        {"let me know if", "you", "it", "that"},
        {"see you", "soon", "later", "tomorrow"},
        {"see you soon", "everyone", "there", "then"},
        {"good morning", "everyone", "to", "and"},
        // Everyday messaging: deterministic, offline phrase continuations.
        {"i will be", "there", "back", "home"},
        {"i will be there", "soon", "tomorrow", "today"},
        {"i will let you", "know", "see", "decide"},
        {"i will let you know", "when", "if", "tomorrow"},
        {"i have to", "go", "work", "leave"},
        {"i have to go", "home", "now", "back"},
        {"i am on my", "way", "phone", "break"},
        {"i am on my way", "home", "there", "now"},
        {"i am at", "work", "home", "the"},
        {"i am at work", "right", "today", "now"},
        {"are you", "going", "ready", "sure"},
        {"are you going to", "be", "the", "work"},
        {"do you want", "to", "me", "some"},
        {"do you want to", "go", "see", "try"},
        {"could you", "please", "help", "check"},
        {"could you please", "help", "send", "check"},
        {"i think", "we", "it", "that"},
        {"i think we", "should", "can", "need"},
        {"that sounds", "good", "great", "interesting"},
        {"that sounds good", "to", "thanks", "and"},
        {"talk to you", "later", "soon", "tomorrow"},
        {"talk to you later", "today", "tonight", "then"},
        {"i am not", "sure", "going", "ready"},
        {"i am not sure", "if", "what", "how"}
    };

    static List<String> suggest(String committedContext,int limit) {
        if(committedContext==null || limit<=0)return Collections.emptyList();
        // Only the current sentence can influence the phrase continuation.
        String context=committedContext.toLowerCase(Locale.ROOT);
        int boundary=-1;
        for(int i=0;i<context.length();i++) {
            char c=context.charAt(i);
            if(c=='.' || c=='!' || c=='?' || c==';' || c=='\n' || c=='\r')
                boundary=i;
        }
        String normalized=context.substring(boundary+1)
                .replaceAll("[^a-z']+"," ").trim().replaceAll(" +"," ");
        if(normalized.isEmpty())return Collections.emptyList();
        String[] best=null;
        for(String[] row:PHRASES) {
            if(normalized.equals(row[0]) || normalized.endsWith(" "+row[0])) {
                if(best==null || row[0].length()>best[0].length())best=row;
            }
        }
        if(best==null)return Collections.emptyList();
        ArrayList<String> result=new ArrayList<>(Math.min(limit,best.length-1));
        for(int i=1;i<best.length && result.size()<limit;i++)result.add(best[i]);
        return result;
    }
}

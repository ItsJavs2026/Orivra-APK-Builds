package com.chroomareborn.keyboard;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

/**
 * Offline vocabulary research. Adds only curated complete words or regular
 * third-person forms backed by an existing base + past + -ing family.
 * No user typing, network access, dynamic suffix guesses, or personal data.
 */
final class LexiconFamilyExpander {
    private LexiconFamilyExpander() {}

    // Broad, generic vocabulary groups: everyday nouns, compound words,
    // descriptors and normal conversational forms. This is a SOURCE
    // dictionary, not a list of typo inputs or full benchmark sentences.
    // Never infer a word from a user's private text in this component.
    private static final String VERIFIED_GENERAL_WORDS =
        "movement improvement achievement agreement requirement arrangement "+
        "management government department environment equipment payment "+
        "shipment investment employment development excitement disappointment "+
        "announcement adjustment commitment enjoyment encouragement "+
        "treatment replacement involvement retirement advertisement "+
        "workshop workplace workbook workbench workforce teamwork "+
        "bookstore wallpaper haircut hairbrush lighthouse skateboard "+
        "backyard bathroom bedroom classroom doorstep hometown "+
        "friendship leadership membership ownership partnership "+
        "scholarship citizenship relationship neighborhood "+
        "postpone postponed postponing postpones thoughtful "+
        "thoughtfully colorful colorfuls colorfulness carefulness "+
        "beautifully peaceful peacefulness helpfulness "+
        "wirelessly accurately accuracy responsibly responsible "+
        "confident confidently comfortable comfortably "+
        "interesting interestingly fascinating fascinatingly "+
        "wonderfully wonderfully personal personally "+
        "unexpected unexpectedly surprising surprisingly "+
        "convenient conveniently gradually quickly quietly "+
        "difficult difficulty difficulties knowledgeable "+
        "knowledgeable knowledgeably "+
        "batteries battery chargers charged charging "+
        "customer customers colleagues colleagues "+
        "documents document paperwork reports reporting "+
        "photography photographer photograph photographs "+
        "opportunity opportunities possibilities probability "+
        "captured captures capturing "+
        "observed observes observing "+
        "mentioned mentions mentioning "+
        "selected selecting selection "+
        "several numerous unusual unusually "+
        "postponement reminder reminders "+
        "vacation vacations destination destinations "+
        "neighbor neighbors neighboring "+
        "exercise exercises exercising "+
        "suggested suggesting suggestions "+
        "available availability responsibility "+
        "evaluation evaluations diagnostics diagnostic";

    static void enrich(Map<String,Integer> words) {
        if(words==null) return;
        for(String token:VERIFIED_GENERAL_WORDS.split(" ")) {
            if(token.matches("[a-z]{3,20}"))words.putIfAbsent(token,14);
        }
        // Freeze source lexemes BEFORE adding any forms, preventing a chain
        // of invented inflections that amplifies dictionary errors.
        for(String stem:new ArrayList<>(words.keySet())) {
            if(stem.length()<3 || stem.length()>12 ||
                    !stem.matches("[a-z]+") || stem.endsWith("s"))continue;
            String past,participle,third;
            int n=stem.length();
            char last=stem.charAt(n-1);
            char before=stem.charAt(n-2);
            if(last=='y' && "aeiou".indexOf(before)<0) {
                past=stem.substring(0,n-1)+"ied";
                participle=stem+"ing";
                third=stem.substring(0,n-1)+"ies";
            }else if(last=='e' && !stem.endsWith("ee")) {
                past=stem+"d";
                participle=stem.substring(0,n-1)+"ing";
                third=stem+"s";
            }else{
                past=stem+"ed";
                participle=stem+"ing";
                if("sxzo".indexOf(last)>=0 || stem.endsWith("ch") ||
                        stem.endsWith("sh"))third=stem+"es";
                else third=stem+"s";
                // CVC doubled-consonant past and gerund: stop/stopped/stopping.
                if(!words.containsKey(past) && !words.containsKey(participle)
                        && n>=3 && "aeiou".indexOf(before)>=0 &&
                        "aeiou".indexOf(last)<0 &&
                        "wxy".indexOf(last)<0 &&
                        "aeiou".indexOf(stem.charAt(n-3))<0) {
                    past=stem+last+"ed";
                    participle=stem+last+"ing";
                }
            }
            if(words.containsKey(past) && words.containsKey(participle) &&
                    third.length()<=15)words.putIfAbsent(third,14);
        }
    }
}

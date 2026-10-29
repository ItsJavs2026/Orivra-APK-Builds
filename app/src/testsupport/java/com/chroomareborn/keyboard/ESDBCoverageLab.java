package com.chroomareborn.keyboard;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Research-only, non-shipping coverage comparison using a separately
 * extracted and attributed ESDB/SCOWL size-60 American English wordlist.
 * Does not add words to the predictor, infer word frequencies, enable
 * correction, or read any real user's keyboard or personal data.
 */
public final class ESDBCoverageLab {
    @SuppressWarnings("unchecked")
    private static Map<String,Integer> builtIn()throws Exception{
        Field field=PredictionLite.class.getDeclaredField("WORDS");
        field.setAccessible(true);
        return (Map<String,Integer>)field.get(null);
    }
    private static String[] fixture(String fieldName)throws Exception{
        Field field=SentenceFlowBlind297Audit.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (String[])field.get(null);
    }
    private static boolean complete(String expected,Set<String> dictionary) {
        for(String token:expected.toLowerCase(Locale.ROOT).split(" "))
            if(!dictionary.contains(token))return false;
        return true;
    }
    private static void require(boolean condition,String message) {
        if(!condition)throw new AssertionError(message);
    }
    public static void main(String[] args)throws Exception{
        require(args.length==1,"Usage: ESDBCoverageLab path/to/generated-us-size60.txt");
        Path source=Path.of(args[0]);
        long bytes=Files.size(source);
        require(bytes>100_000 && bytes<10_000_000,"Unexpected external source size");
        Set<String> esdb=new HashSet<>();
        int rejected=0;
        try(BufferedReader reader=Files.newBufferedReader(source,StandardCharsets.UTF_8)) {
            String line;
            while((line=reader.readLine())!=null) {
                String word=line.trim().toLowerCase(Locale.ROOT);
                if(word.matches("[a-z]{2,24}"))esdb.add(word);
                else rejected++;
                require(esdb.size()<=250_000,"Word source too large for this research limit");
            }
        }
        require(esdb.size()>20_000,"Source is not a broad English lexicon");
        Map<String,Integer> core=builtIn();
        Set<String> combined=new HashSet<>(core.keySet());
        combined.addAll(esdb);
        String[] phrases=fixture("PHRASES");
        String[] words=fixture("COMPLETE_WORDS");
        require(phrases.length==20 && words.length==20,"Frozen QA corpus drift");
        int previousWords=0,proposedWords=0,previousPhrases=0,proposedPhrases=0;
        for(String word:words){
            if(complete(word,core.keySet()))previousWords++;
            if(complete(word,combined))proposedWords++;
            if(!core.containsKey(word)&&esdb.contains(word))
                System.out.println("NEW_EXACT_ONLY_WORD "+word);
        }
        for(String phrase:phrases) {
            if(complete(phrase,core.keySet()))previousPhrases++;
            if(complete(phrase,combined))proposedPhrases++;
        }
        System.out.printf(Locale.ROOT,"ESDB_PACK sourceBytes=%d words=%d "+
                "excludedNonASCIIOrNonWord=%d builtinWords=%d exactNew=%d%n",
                bytes,esdb.size(),rejected,core.size(),combined.size()-core.size());
        System.out.printf(Locale.ROOT,
                "ESDB_AUDIT knownWords20 baseline=%d proposed=%d "+
                "joinedSentences20FullyCovered baseline=%d proposed=%d%n",
                previousWords,proposedWords,previousPhrases,proposedPhrases);
        require(previousPhrases==8 && previousWords==8,
                "Candidate 298 source changed or initial analysis inconsistent");
        require(proposedWords>=previousWords && proposedPhrases>=previousPhrases,
                "A vocabulary union cannot reduce coverage");
        System.out.println("SCOPE: coverage only, no decoder scores, "+
                "no APK, no saved user data, ESDB frequencies unspecified");
    }
}

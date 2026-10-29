package com.chroomareborn.keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/**
 * Fully synthetic, deterministic SentenceFlow QA. No user typing or stored
 * editor contents. Reports separate top-1/top-3 and response time, rather
 * than declaring isolated screenshot cases a broad model improvement.
 * This class is NEVER installed as part of the production IME.
 */
public final class SentenceFlowAccuracyBenchmark {
    private static final class Example {
        final String group,input,expected;
        Example(String g,String i,String e){group=g;input=i;expected=e;}
    }
    private static final class Stats {
        final String group;int n,top1,top3,misses;long totalNanos;
        final List<Long> timings=new ArrayList<>();
        final List<String> missedExamples=new ArrayList<>();
        Stats(String name){group=name;}
        void record(Example e,List<String> results,long elapsed){
            n++;totalNanos+=elapsed;timings.add(elapsed);
            boolean one=!results.isEmpty() && eq(results.get(0),e.expected);
            boolean three=false;
            for(int i=0;i<Math.min(results.size(),3);i++)
                if(eq(results.get(i),e.expected)){three=true;break;}
            if(one)top1++;
            if(three)top3++;
            else {
                misses++;
                if(missedExamples.size()<5)missedExamples.add(
                    e.input+" => "+(results.isEmpty()?"(none)":results.get(0))
                    +" [wanted "+e.expected+"]");
            }
        }
        String summary(){
            Collections.sort(timings);
            double p95=timings.isEmpty()?0:timings.get((int)Math.ceil(n*.95)-1)/1e6;
            return String.format(Locale.US,
                    "GROUP %-20s n=%3d top1=%3d (%.1f%%) top3=%3d (%.1f%%) p95=%.1fms",
                    group,n,top1,percent(top1,n),top3,percent(top3,n),p95);
        }
    }
    private static double percent(int amount,int total) {
        return total==0?0:100.0*amount/total;
    }
    private static String norm(String text){
        return text==null?"":text.toLowerCase(Locale.US).replace('’','\'')
                .replaceAll("\\s+"," ").trim();
    }
    private static boolean eq(String a,String b) {return norm(a).equals(norm(b));}
    private static void add(List<Example> data,String group,String expected,String input){
        data.add(new Example(group,input,expected));
    }
    private static String joined(String phrase){
        return phrase.replaceAll("[^A-Za-z]","");
    }
    /** Seed-derived typo positions are not hand-picked for a specific word. */
    private static String typo(String raw,int seed,int errors) {
        char[] letters=raw.toCharArray();
        int len=letters.length;
        if(len<5)return raw;
        for(int i=0;i<errors;i++) {
            // Interior positions avoid case and initial-letter bias.
            int at=1+Math.floorMod(seed*13+i*17+len*7,len-2);
            char c=letters[at];
            letters[at]=c=='s'?'t':c=='e'?'r':c=='a'?'o':c=='o'?'a':
                    c=='i'?'u':c=='t'?'r':c=='n'?'m':c=='r'?'t':'e';
        }
        return new String(letters);
    }
    private static final String[] PHRASES={
        "how are you doing today","what are you doing now",
        "can you let me know","thank you very much",
        "please let me know","where are you going",
        "how was your weekend","we are getting there",
        "this is a good idea","i am going home",
        "what time is it","we need to talk",
        "the keyboard is working","you are doing great",
        "can we try again","i will see you tomorrow",
        "the weather is nice","i need some help",
        "we have more time","let me check that",
        "i am on my way","she is coming home",
        "let us try something","they are going to work",
        "i want to learn more","you can call me later",
        "thanks for your help","this could be better",
        "how are things going","what happens next"
    };
    private static final String[] WORDS={
        "vibration","single","weather","language","calendar",
        "picture","friend","morning","beautiful","meeting",
        "keyboard","typing","spelling","problem","battery",
        "correct","number","question","computer","tomorrow",
        "message","important","together","remember","continue",
        "suggestion","prediction","different","learning","working"
    };
    private static final String[] STABLE={
        "also","were","working","hello","question","keyboard",
        "weather","orange","morning","beautiful","meeting",
        "friend","battery","message","important","continue"
    };

    private static List<Example> samples() {
        List<Example> all=new ArrayList<>();
        for(String word:STABLE)add(all,"known_word",word,word);
        for(int i=0;i<WORDS.length;i++)
            add(all,"word_one_typo",WORDS[i],typo(WORDS[i],i+11,1));
        for(int i=0;i<PHRASES.length;i++) {
            String p=PHRASES[i],raw=joined(p);
            add(all,"joined_exact",p,raw);
            add(all,"joined_one_typo",p,typo(raw,i+101,1));
            add(all,"joined_two_typos",p,typo(raw,i+201,2));
        }
        String[][] reported={
            {"vibrstion","vibration"},
            {"singke","single"},
            {"singkewoods","single words"},
            {"ofanimation","of animation"},
            {"hiwsitgoijg","how's it going"},
            {"howsotgkikf","how's it going"},
            {"okayniwweregettijgsokewjere","okay now we're getting somewhere"},
            {"aksoadd","also add"},
            {"aksoaddsomwmore","also add some more"},
            {"letsteysayingsimetbingcompltekekyunrelated",
                "let's try saying something completely unrelated"},
            {"inusingtheeybjsrdtotyoethis",
                "I'm using the keyboard to type this"}
        };
        for(String[] row:reported)add(all,"reported_difficult",row[1],row[0]);
        return all;
    }
    public static void main(String[] argv){
        final List<Example> corpus=samples();
        final Map<String,Stats> groups=new LinkedHashMap<>();
        Stats all=new Stats("ALL");
        // Load dictionary before taking measurements.
        PredictionLite.suggestions("hello");
        long start=System.nanoTime();
        for(Example e:corpus) {
            Stats current=groups.computeIfAbsent(e.group,Stats::new);
            long t0=System.nanoTime();
            List<String> output=PredictionLite.suggestions(e.input);
            long elapsed=System.nanoTime()-t0;
            if(output==null)throw new AssertionError("Null predictor output");
            current.record(e,output,elapsed);
            all.record(e,output,elapsed);
            if(e.group.equals("known_word") &&
                    (output.isEmpty() || !eq(output.get(0),e.expected))) {
                // A true false-correction risk worth investigating, but
                // reporting the entire benchmark is more useful than
                // aborting on the first unrecognized dictionary entry.
                System.out.println("KNOWN_WORD_RISK="+e.input+
                        " top="+(output.isEmpty()?"(none)":output.get(0)));
            }
            if((e.input.equals("also") || e.input.equals("were")) &&
                    (output.isEmpty() || !eq(output.get(0),e.expected)))
                throw new AssertionError("Protected verified word changed: "+e.input);
        }
        for(Stats group:groups.values()) {
            System.out.println(group.summary());
            for(String missing:group.missedExamples)System.out.println("  MISS "+missing);
        }
        System.out.println(all.summary());
        System.out.println("WALL_MS="+Math.round((System.nanoTime()-start)/1e6));
        System.out.println("FIXTURE_COUNT="+all.n);
        if(all.n<140)throw new AssertionError("Benchmark corpus unexpectedly small");
    }
}

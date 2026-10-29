package com.chroomareborn.keyboard;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Collections;

/**
 * Held-out synthetic QA set: phrases are NOT imported into the predictor,
 * phrase corpus, training vocabulary, or scoring tables. No user data.
 * Metrics are comparison signals only, not representative field accuracy.
 */
public final class SentenceFlowHoldoutBenchmark {
    private static final String[] SENTENCES={
        "the screen looks beautiful today",
        "we should try another way",
        "you can write a better message",
        "she wants to learn something new",
        "my phone battery needs attention",
        "your suggestion looks much better",
        "they are waiting outside",
        "he forgot about the meeting",
        "i wanted to finish this",
        "the keyboard needs more testing",
        "we can fix the problem later",
        "please check your messages today",
        "the car is working again",
        "i will call you after work",
        "they want to visit tomorrow",
        "my friend has a question",
        "the new update looks good",
        "we need to find another solution",
        "he can send the email tonight",
        "you should remember this message",
        "the family is going shopping",
        "i need the address again",
        "please send me your answer",
        "we are going home soon",
        "the app looks much cleaner",
        "the phone screen feels smooth",
        "the battery lasts much longer",
        "i have an important question",
        "you can change the settings",
        "the new version is working",
        "i love the color of the sky",
        "we should clean the keyboard",
        "please take care of yourself",
        "my brother works on weekends",
        "they started learning yesterday",
        "we will finish the project",
        "i need a little more time",
        "the next meeting is tomorrow",
        "she forgot to call her friend",
        "i was thinking about that",
        "we should talk about this",
        "the food was really good",
        "you can help me with this",
        "the app needs a fresh design",
        "we want to improve accuracy",
        "he was walking in the park",
        "we have a lot to learn",
        "they will arrive tomorrow morning",
        "please show me the results",
        "the typing speed feels good",
        "i am trying something different",
        "do you want to go outside",
        "we need the right answer",
        "you are doing a great job",
        "the weather changed this morning",
        "we should check the next word",
        "the new phone feels great",
        "she needs to finish her work",
        "i forgot to bring my keys",
        "we will talk again later"
    };
    private static final String[] WHOLE_WORDS={
        "keyboard","message","important","battery","screen",
        "question","suggestion","prediction","learning","working",
        "today","tomorrow","morning","helpful","good",
        "beautiful","attention","answer","another","about",
        "different","understand","remember","computer","application",
        "smooth","settings","version","testing","language",
        "people","phone","family","friend","shopping",
        "afternoon","address","conversation","correction","accuracy",
        "number","time","weather","together","finish",
        "problem","project","evening","hello","were"
    };
    private static final class Stats {
        final String name;int n,one,three;final List<Long> nanos=new ArrayList<>();
        final List<String> missing=new ArrayList<>();
        Stats(String n){name=n;}
        void take(String input,String wanted,List<String> found,long elapsed) {
            n++;nanos.add(elapsed);
            boolean topOne=!found.isEmpty()&&same(found.get(0),wanted);
            boolean topThree=false;
            for(int i=0;i<Math.min(3,found.size());i++)
                if(same(found.get(i),wanted)){topThree=true;break;}
            if(topOne)one++;
            if(topThree)three++;
            else if(missing.size()<4)
                missing.add(input+" -> "+
                        (found.isEmpty()?"(none)":found.get(0))
                        +" (wanted "+wanted+")");
        }
        String report(){
            Collections.sort(nanos);
            long p95=nanos.get((int)Math.ceil(n*.95)-1);
            return String.format(Locale.US,
                    "HOLDOUT %-18s n=%3d top1=%3d top3=%3d p95=%.2fms",
                    name,n,one,three,p95/1e6);
        }
    }
    private static String clean(String s) {
        return s.toLowerCase(Locale.US).replace('’','\'').trim()
                .replaceAll("\\s+"," ");
    }
    private static boolean same(String a,String b){return clean(a).equals(clean(b));}
    private static String joined(String v){return v.replaceAll("[^A-Za-z]","");}
    private static String mistype(String s,int seed,int count) {
        char[] arr=s.toCharArray();
        if(arr.length<5)return s;
        for(int i=0;i<count;i++) {
            int where=1+Math.floorMod((seed+11)*17+(i+1)*41,
                    arr.length-2);
            char old=arr[where];
            arr[where]=old=='e'?'r':old=='a'?'s':old=='o'?'p':
                    old=='t'?'r':old=='i'?'o':old=='n'?'m':
                    old=='s'?'a':old=='r'?'t':'e';
        }
        return new String(arr);
    }
    public static void main(String[] args) {
        Map<String,Stats> groups=new LinkedHashMap<>();
        Stats all=new Stats("ALL");
        PredictionLite.suggestions("hello");
        for(int index=0;index<SENTENCES.length;index++) {
            String target=SENTENCES[index],raw=joined(target);
            for(int typos=0;typos<=3;typos++) {
                String key="joined_"+typos+"_typos";
                String input=mistype(raw,index*7+typos*3,typos);
                record(groups,all,key,input,target);
            }
        }
        for(int i=0;i<WHOLE_WORDS.length;i++) {
            record(groups,all,"stable_words",
                    WHOLE_WORDS[i],WHOLE_WORDS[i]);
            record(groups,all,"word_typos",
                    mistype(WHOLE_WORDS[i],i+4,1),
                    WHOLE_WORDS[i]);
        }
        for(Stats s:groups.values()){
            System.out.println(s.report());
            for(String miss:s.missing)System.out.println("  HOLDOUT_MISS "+miss);
        }
        System.out.println(all.report());
        System.out.println("HOLDOUT_FIXTURES="+all.n);
        if(all.n<300)throw new AssertionError("Holdout corpus too small: "+all.n);
    }
    private static void record(Map<String,Stats> groups,Stats all,
                               String group,String input,String desired) {
        long start=System.nanoTime();
        List<String> predicted=PredictionLite.suggestions(input);
        long nanos=System.nanoTime()-start;
        if(predicted==null)throw new AssertionError("Null prediction");
        groups.computeIfAbsent(group,Stats::new).take(input,desired,predicted,nanos);
        all.take(input,desired,predicted,nanos);
    }
}

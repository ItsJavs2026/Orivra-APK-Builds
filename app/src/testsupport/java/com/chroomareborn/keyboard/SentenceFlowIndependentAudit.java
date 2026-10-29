package com.chroomareborn.keyboard;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Independent synthetic audit; the sentences below are NEVER incorporated into
 * the dictionary, predictor, ranking, or model. No personal typing is used.
 * Output is comparison evidence only, not a representative accuracy estimate.
 */
public final class SentenceFlowIndependentAudit {
    private static final String[] PHRASES = {
        "my neighbor enjoys quiet mornings",
        "we should review these options tomorrow",
        "the weather feels unusually pleasant",
        "please forward the updated documents",
        "our family plans a short vacation",
        "she always carries extra batteries",
        "the package arrived earlier today",
        "the workshop begins next Wednesday",
        "you mentioned several possible improvements",
        "they decided to postpone the meeting",
        "i appreciate thoughtful suggestions",
        "his camera captured colorful pictures",
        "we want to make this work",
        "the next step should be simple",
        "you can see the full result",
        "i will share my notes later"
    };

    private static String mutate(String word,int position) {
        char[] chars=word.toCharArray();
        if(position<0||position>=chars.length)return word;
        chars[position]=chars[position]=='x'?'z':'x';
        return new String(chars);
    }

    private static String joined(String[] words) {
        StringBuilder out=new StringBuilder();
        for(String word:words)out.append(word);
        return out.toString();
    }

    private static String[] corrupt(String[] original,String group) {
        String[] words=original.clone();
        if(group.equals("joined_exact"))return words;
        int first=-1,second=-1,longest=-1;
        for(int i=0;i<words.length;i++){
            if(words[i].length()>=4 && first<0)first=i;
            if(words[i].length()>=4 && i>first && second<0)second=i;
            if(words[i].length()>=7 &&
                    (longest<0 || words[i].length()>words[longest].length()))
                longest=i;
        }
        if(group.equals("two_words_one_each") && first>=0 && second>=0){
            words[first]=mutate(words[first],1);
            words[second]=mutate(words[second],words[second].length()-2);
        }else if(group.equals("one_long_word_two") && longest>=0){
            words[longest]=mutate(words[longest],2);
            words[longest]=mutate(words[longest],words[longest].length()-2);
        }else if(group.equals("one_short_word_two")){
            int chosen=-1;
            // Prefer 4/5-letter words *after* a predecessor for context.
            for(int i=1;i<words.length;i++){
                if(words[i].length()>=4&&words[i].length()<=5){chosen=i;break;}
            }
            if(chosen>=0){
                words[chosen]=mutate(words[chosen],1);
                words[chosen]=mutate(words[chosen],words[chosen].length()-2);
            }else if(second>=0){
                words[second]=mutate(words[second],1);
                words[second]=mutate(words[second],words[second].length()-2);
            }
        }
        return words;
    }

    public static void main(String[] args) {
        String[] groups={"joined_exact","two_words_one_each",
                         "one_long_word_two","one_short_word_two"};
        for(String group:groups){
            int count=0,top1=0,top3=0,nonEmpty=0;
            long[] nanos=new long[PHRASES.length];
            for(String sentence:PHRASES){
                String expected=sentence.toLowerCase(Locale.ROOT);
                String input=joined(corrupt(expected.split(" "),group));
                if(input.length()>65)continue;
                long start=System.nanoTime();
                List<String> suggestions=PredictionLite.suggestions(input);
                nanos[count]=System.nanoTime()-start;
                count++;
                if(!suggestions.isEmpty())nonEmpty++;
                if(!suggestions.isEmpty() &&
                        expected.equals(suggestions.get(0).toLowerCase(Locale.ROOT)))
                    top1++;
                boolean match=false;
                for(int i=0;i<Math.min(3,suggestions.size());i++){
                    if(expected.equals(suggestions.get(i).toLowerCase(Locale.ROOT))){
                        match=true;break;
                    }
                }
                if(match)top3++;
                String first=suggestions.isEmpty()?"":suggestions.get(0);
                System.out.printf("CASE\t%s\t%s\t%s\t%s%n",
                        group,input,expected,first);
            }
            java.util.Arrays.sort(nanos,0,count);
            double p95=count==0?0:nanos[(int)Math.ceil(count*.95)-1]/1e6;
            System.out.printf(Locale.ROOT,
                    "AUDIT %s n=%d top1=%d top3=%d nonempty=%d p95=%.2fms%n",
                    group,count,top1,top3,nonEmpty,p95);
        }
    }
}

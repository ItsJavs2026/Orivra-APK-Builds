package com.chroomareborn.keyboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Pure, tap-only prefix-completion ranking for opt-in learned word counts. */
final class FrequentWordCompletionPolicy328 {
    static final int MAX_WORDS=160;
    private FrequentWordCompletionPolicy328(){}
    static boolean isCompletion(String raw,String choice) {
        String a=EnglishApostrophePolicy.prefix(raw);
        String b=EnglishApostrophePolicy.canonical(choice);
        return a!=null&&b!=null&&b.length()>a.length()&&b.startsWith(a)
                &&choice.indexOf(' ')<0;
    }
    static boolean qualifies(String raw,String word,int count) {
        if(!isCompletion(raw,word)||count<1||count>1000)return false;
        String p=EnglishApostrophePolicy.prefix(raw);
        if(p.length()==1)
            return word.length()<=3&&count>=5;
        return count>=(word.length()<=3?3:2);
    }
    static List<String> matching(String raw,Map<String,Integer> counts,int limit) {
        if(counts==null||limit<=0||EnglishApostrophePolicy.prefix(raw)==null)
            return List.of();
        ArrayList<Map.Entry<String,Integer>> good=new ArrayList<>();
        for(Map.Entry<String,Integer> e:counts.entrySet())
            if(e.getValue()!=null&&qualifies(raw,e.getKey(),e.getValue()))
                good.add(e);
        good.sort(Comparator.<Map.Entry<String,Integer>>comparingInt(e->-e.getValue())
                .thenComparingInt(e->e.getKey().length())
                .thenComparing(Map.Entry::getKey));
        ArrayList<String> out=new ArrayList<>();
        for(Map.Entry<String,Integer> e:good) {
            if(out.size()>=Math.min(5,limit))break;
            String w=e.getKey();
            if(raw.length()>1&&raw.equals(raw.toUpperCase(Locale.ROOT)))
                w=w.toUpperCase(Locale.ROOT);
            else if(Character.isUpperCase(raw.charAt(0)))
                w=Character.toUpperCase(w.charAt(0))+w.substring(1);
            out.add(w);
        }
        return out;
    }
    static List<String> merge(String input,List<String> base,List<String> frequent,int limit) {
        if(base==null)base=List.of();
        if(frequent==null||frequent.isEmpty())return base;
        ArrayList<String> out=new ArrayList<>();
        Set<String> seen=new HashSet<>();
        boolean exact=!base.isEmpty()&&
                EnglishApostrophePolicy.canonical(base.get(0))!=null&&
                EnglishApostrophePolicy.canonical(base.get(0)).equals(
                        EnglishApostrophePolicy.prefix(input));
        if(exact)add(out,seen,base.get(0),limit);
        for(String v:frequent)add(out,seen,v,limit);
        for(String v:base)add(out,seen,v,limit);
        add(out,seen,input,limit);
        return out;
    }
    private static void add(List<String> out,Set<String> seen,String v,int limit) {
        if(v!=null&&!v.isEmpty()&&out.size()<limit&&
                seen.add(v.toLowerCase(Locale.ROOT)))out.add(v);
    }
}

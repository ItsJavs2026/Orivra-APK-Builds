package com.chroomareborn.keyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Experimental, offline bounded vocabulary index. No Android dependencies.
 * The IME does not currently load this class or the research corpus.
 */
final class SymSpellCandidateBridge {
    private final Map<String,List<Word>> buckets=new HashMap<>();

    private static final class Word {
        final String text;
        final long frequency;
        Word(String text,long frequency){this.text=text;this.frequency=frequency;}
    }
    SymSpellCandidateBridge(Map<String,Long> corpus,int limit) {
        List<Map.Entry<String,Long>> sorted=new ArrayList<>(corpus.entrySet());
        sorted.sort((a,b)->Long.compare(b.getValue(),a.getValue()));
        int n=0;
        for(Map.Entry<String,Long> e:sorted){
            String word=e.getKey().toLowerCase(Locale.US);
            if(word.length()<5||word.length()>15||!word.matches("[a-z]+"))continue;
            String key=word.substring(0,2)+":"+word.length();
            buckets.computeIfAbsent(key,k->new ArrayList<>()).add(new Word(word,e.getValue()));
            if(++n>=limit)break;
        }
    }
    List<String> alternatives(String typed,int cap) {
        if(typed==null || !typed.matches("[A-Za-z]{6,14}")||cap<=0)
            return Collections.emptyList();
        String input=typed.toLowerCase(Locale.US);
        ArrayList<Word> found=new ArrayList<>();
        Map<String,Integer> distances=new HashMap<>();
        for(int length=Math.max(5,input.length()-1);length<=Math.min(15,input.length()+1);length++){
            List<Word> group=buckets.get(input.substring(0,2)+":"+length);
            if(group==null)continue;
            for(Word word:group){
                if(word.text.equals(input))continue;
                int edits=distance(input,word.text);
                if(edits<=2){
                    found.add(word);
                    distances.put(word.text,edits);
                }
            }
        }
        found.sort(Comparator.comparingInt((Word w)->distances.get(w.text))
                .thenComparing((Word a,Word b)->Long.compare(b.frequency,a.frequency)));
        List<String> result=new ArrayList<>();
        for(Word word:found){
            String corrected=Character.isUpperCase(typed.charAt(0))?
                    Character.toUpperCase(word.text.charAt(0))+word.text.substring(1):word.text;
            if(!result.contains(corrected))result.add(corrected);
            if(result.size()>=cap)break;
        }
        return result;
    }
    static List<String> conservativeMerge(String typed,List<String> current,
            List<String> additional,Map<String,Long> frequency) {
        if(typed==null||!typed.matches("[A-Za-z]{6,14}")||additional.isEmpty())
            return current;
        String raw=typed.toLowerCase(Locale.US);
        // Correctly spelled corpus entries are negative controls, not
        // candidates for correction. Never add or promote alternatives.
        if(frequency.containsKey(raw) ||
                (!current.isEmpty() && current.get(0).equalsIgnoreCase(typed)))
            return current;
        List<String> out=new ArrayList<>(current);
        String top=out.isEmpty()?"":out.get(0).toLowerCase(Locale.US);
        int oldEdits=top.matches("[a-z]+")?distance(raw,top):99;
        long oldFrequency=frequency.getOrDefault(top,0L);

        // First choose the strongest candidate, rather than repeatedly
        // promoting entries as we traverse the list.
        String best=null;
        int bestEdits=99;
        long bestFrequency=0;
        for(String extra:additional) {
            String word=extra.toLowerCase(Locale.US);
            if(!word.matches("[a-z]+") || word.length()<6 ||
                    !word.startsWith(raw.substring(0,2)) ||
                    word.charAt(word.length()-1)!=raw.charAt(raw.length()-1))continue;
            int edits=distance(raw,word);
            long count=frequency.getOrDefault(word,0L);
            if(edits<1||edits>2||count<=0)continue;
            if(edits<bestEdits || (edits==bestEdits && count>bestFrequency)){
                best=extra;bestEdits=edits;bestFrequency=count;
            }
        }
        if(best==null)return out;
        boolean promote=false;
        if(top.isEmpty())promote=true;
        else if(top.matches("[a-z]+")){
            // Never replace a one-edit match with a two-edit candidate.
            // Only override equal-distance ambiguity with decisive corpus
            // evidence, and do not displace exact correctly typed words.
            promote=oldEdits>=2 && (bestEdits<oldEdits ||
                    (bestEdits==oldEdits && oldFrequency>0 &&
                            bestFrequency>=oldFrequency*5L));
        }else if(raw.length()>=9 && top.contains(" ")){
            // Multiword suggestions are normally protected. Exception:
            // a long input with a strong intact word boundary and at least
            // three speculative segments. This is an experiment, not enabled
            // in the installed keyboard.
            int spaces=0;
            for(int i=0;i<top.length();i++)if(top.charAt(i)==' ')spaces++;
            promote=spaces>=2 && bestEdits<=2 && bestFrequency>=1000000L;
        }
        // Do not displace a valid existing prediction with a previously
        // unseen single-word candidate. Exception: the existing top is a
        // speculative multiword split, already guarded above.
        if(promote && top.matches("[a-z]+") && !out.contains(best))
            promote=false;
        if(promote){
            out.remove(best);
            out.add(0,best);
        }else if(!out.contains(best))out.add(best);
        while(out.size()>8)out.remove(out.size()-1);
        return out;
    }
    private static int distance(String a,String b) {
        int[] prev=new int[b.length()+1];
        for(int j=0;j<=b.length();j++)prev[j]=j;
        for(int i=1;i<=a.length();i++){
            int[] curr=new int[b.length()+1];curr[0]=i;
            for(int j=1;j<=b.length();j++)
                curr[j]=Math.min(Math.min(curr[j-1]+1,prev[j]+1),
                        prev[j-1]+(a.charAt(i-1)==b.charAt(j-1)?0:1));
            prev=curr;
        }
        return prev[b.length()];
    }
}

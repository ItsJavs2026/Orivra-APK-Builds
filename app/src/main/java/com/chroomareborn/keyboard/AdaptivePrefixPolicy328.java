package com.chroomareborn.keyboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Prefix completion from repeat evidence, not fuzzy autocorrection.
 * Unlike learning a previous-word pair, a repeatedly finished word may
 * be offered on its own (e.g. typing "con" -> "continue").
 * No automatic replacement, raw sentences or tap-stream persistence.
 */
final class AdaptivePrefixPolicy328 {
    private AdaptivePrefixPolicy328() {}
    static final int MAX_CANDIDATES=3;
    static boolean eligible(String prefix,String word,int uses) {
        if(prefix==null||word==null || uses<1 ||
                prefix.length()>28 || word.length()>28)return false;
        String raw=EnglishApostrophePolicy.prefix(prefix);
        String target=EnglishApostrophePolicy.canonical(word);
        if(raw==null||target==null || raw.length()>=target.length() ||
                !target.startsWith(raw))return false;
        if(raw.length()==1)
            return target.length()<=4 && uses>=7;
        if(raw.length()==2)
            return uses>=5;
        return uses>=3;
    }
    static List<String> candidates(String prefix,Map<String,Integer> counts,
            int max) {
        String raw=EnglishApostrophePolicy.prefix(prefix);
        if(raw==null||counts==null||counts.isEmpty()||max<=0)return List.of();
        ArrayList<Map.Entry<String,Integer>> eligible=new ArrayList<>();
        for(Map.Entry<String,Integer> entry:counts.entrySet()) {
            int count=entry.getValue()==null?0:entry.getValue();
            if(eligible(raw,entry.getKey(),count))eligible.add(entry);
        }
        eligible.sort(Comparator.<Map.Entry<String,Integer>>comparingInt(
                item->-item.getValue()).thenComparing(Map.Entry::getKey));
        ArrayList<String> out=new ArrayList<>();
        for(Map.Entry<String,Integer> entry:eligible) {
            if(out.size()>=Math.min(MAX_CANDIDATES,max))break;
            String target=entry.getKey();
            if(prefix.equals(prefix.toUpperCase(Locale.ROOT)))
                target=target.toUpperCase(Locale.ROOT);
            else if(Character.isUpperCase(prefix.charAt(0)))
                target=Character.toUpperCase(target.charAt(0))+target.substring(1);
            out.add(target);
        }
        return out;
    }
    /** Never hide correctly completed words like can, at, my, to or in.
     * For an incomplete word, put the learned completion first. Every item
     * remains tap-only and editor text is untouched until acceptance.
     */
    static List<String> merge(String prefix,List<String> base,
            List<String> preferred,boolean exactKnown) {
        if(base==null)base=List.of();
        if(preferred==null || preferred.isEmpty())return base;
        LinkedHashSet<String> items=new LinkedHashSet<>();
        if(exactKnown)items.add(prefix);
        items.addAll(preferred);
        items.addAll(base);
        ArrayList<String> out=new ArrayList<>();
        for(String v:items) {
            if(v==null||v.isEmpty())continue;
            boolean same=false;
            for(String previous:out)if(previous.equalsIgnoreCase(v)) {
                same=true;break;
            }
            if(!same)out.add(v);
            if(out.size()>=8)break;
        }
        return out;
    }
}

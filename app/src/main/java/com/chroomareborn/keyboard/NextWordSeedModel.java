package com.chroomareborn.keyboard;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Offline cold-start bigrams in "previous<TAB>next<TAB>weight" format.
 * Compatible with SwiftFloris's attributed seed table; parsing and ranking
 * logic here are independently written for Orivra.
 *
 * No network, persistence or user-generated data. Not wired into the IME yet.
 */
final class NextWordSeedModel {
    private final Map<String,Map<String,Double>> byPrevious=new HashMap<>();
    private int entries;

    private NextWordSeedModel() {}

    private static String word(String token) {
        if(token==null)return null;
        String w=token.trim().toLowerCase(Locale.US);
        return w.matches("[a-z][a-z']{0,31}")?w:null;
    }

    static NextWordSeedModel fromTsv(Reader source) throws IOException {
        NextWordSeedModel model=new NextWordSeedModel();
        try(BufferedReader br=new BufferedReader(source)) {
            String line;
            int lines=0;
            while((line=br.readLine())!=null) {
                if(++lines>100000)throw new IOException("Model line limit exceeded");
                if(line.isEmpty() || line.charAt(0)=='#')continue;
                String[] fields=line.split("\\t",-1);
                if(fields.length!=3)continue;
                String prev=word(fields[0]),next=word(fields[1]);
                if(prev==null || next==null)continue;
                double weight;
                try { weight=Double.parseDouble(fields[2].trim()); }
                catch(NumberFormatException e) { continue; }
                if(!Double.isFinite(weight) || weight<=0 || weight>1)continue;
                Map<String,Double> continuations=model.byPrevious.get(prev);
                if(continuations==null) {
                    continuations=new HashMap<>();
                    model.byPrevious.put(prev,continuations);
                }
                Double before=continuations.get(next);
                if(before==null)model.entries++;
                if(before==null || weight>before)continuations.put(next,weight);
            }
        }
        return model;
    }

    List<String> suggest(String committedContext,int limit) {
        if(limit<=0 || committedContext==null)return Collections.emptyList();
        String[] chunks=committedContext.toLowerCase(Locale.US).trim()
                .split("[^a-z']+");
        if(chunks.length==0)return Collections.emptyList();
        String prev=word(chunks[chunks.length-1]);
        if(prev==null)return Collections.emptyList();
        Map<String,Double> values=byPrevious.get(prev);
        if(values==null || values.isEmpty())return Collections.emptyList();
        ArrayList<String> result=new ArrayList<>(values.keySet());
        result.sort(Comparator.<String>comparingDouble(k->-values.get(k))
                .thenComparing(Comparator.naturalOrder()));
        return result.size()>limit
                ?new ArrayList<>(result.subList(0,limit)):result;
    }

    int entries() { return entries; }
    int contexts() { return byPrevious.size(); }
}

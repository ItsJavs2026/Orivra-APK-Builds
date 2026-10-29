package com.chroomareborn.keyboard;

import java.util.Locale;
import java.util.Map;

/**
 * Research-only optional word-boundary *candidate* from exact ESDB words.
 * Unlike the earlier failed approach, secondary vocabulary NEVER joins the
 * fuzzy beam search: no edits, no inferred word frequencies or typo scans.
 *
 * Only a substantially shorter exact-letter split may challenge a clearly
 * fragmentary baseline. All candidates require an explicit user tap.
 */
final class ExactOnlySegmentation {
    private ExactOnlySegmentation() {}
    private static final class State {
        final String text;
        final int tokens,novel;
        final double cost;
        State(String t,int words,int additions,double score) {
            text=t;tokens=words;novel=additions;cost=score;
        }
    }
    private static String compact(String sentence) {
        return sentence.toLowerCase(Locale.ROOT).replaceAll("[^a-z]","");
    }
    private static int editDistanceAtMostOne(String a,String b) {
        if(Math.abs(a.length()-b.length())>1)return 2;
        int[][] d=new int[2][b.length()+1];
        for(int j=0;j<=b.length();j++)d[0][j]=j;
        for(int i=1;i<=a.length();i++) {
            int cur=i&1,prev=cur^1;
            d[cur][0]=i;
            int min=i;
            for(int j=1;j<=b.length();j++) {
                int sub=d[prev][j-1]+(a.charAt(i-1)==b.charAt(j-1)?0:1);
                d[cur][j]=Math.min(sub,Math.min(d[prev][j]+1,d[cur][j-1]+1));
                min=Math.min(min,d[cur][j]);
            }
            if(min>1)return 2;
        }
        return d[a.length()&1][b.length()];
    }
    static String propose(String raw,String baselineTop,Map<String,Integer> core,
            ExactOnlyLanguagePack pack) {
        if(pack==null || raw==null || baselineTop==null ||
                raw.length()<16 || raw.length()>65 || core==null)return null;
        if(!raw.matches("[a-z]+"))return null;
        String[] current=baselineTop.trim().split(" +");
        if(current.length<4)return null;
        String baseline=compact(baselineTop);
        // The existing predictor already corrected multiple typos? Keep it.
        // Only explore when top suggestion is exact or one edit away.
        if(editDistanceAtMostOne(raw,baseline)>1)return null;
        final int maxTokens=Math.min(11,current.length-1);
        final int maxNovel=2;
        State[][][] dp=new State[raw.length()+1][maxTokens+1][maxNovel+1];
        dp[0][0][0]=new State("",0,0,0);
        for(int pos=0;pos<raw.length();pos++) {
            for(int end=pos+1;end<=Math.min(pos+24,raw.length());end++) {
                String word=raw.substring(pos,end);
                boolean known=core.containsKey(word);
                if(word.length()==1 && !word.equals("i")&&!word.equals("a"))
                    continue;
                if(!known && (word.length()<6 || !pack.contains(word)))
                    continue;
                int additional=known?0:1;
                for(int tokens=0;tokens<maxTokens;tokens++) {
                    for(int novel=0;novel+additional<=maxNovel;novel++) {
                        State from=dp[pos][tokens][novel];
                        if(from==null)continue;
                        // Prefer complete long lexemes, but never assume ESDB
                        // provides meaningful usage frequencies.
                        double nextCost=from.cost+10+(additional==1?2.6:0)
                                -Math.min(word.length(),12)*0.12;
                        String text=from.text.isEmpty()?word:from.text+" "+word;
                        State previous=dp[end][tokens+1][novel+additional];
                        if(previous==null || nextCost<previous.cost)
                            dp[end][tokens+1][novel+additional]=new State(
                                text,tokens+1,novel+additional,nextCost);
                    }
                }
            }
        }
        State best=null;
        for(int tokens=3;tokens<=maxTokens;tokens++){
            for(int novel=1;novel<=maxNovel;novel++) {
                if(tokens-novel<2)continue; // evidence from core vocabulary
                State s=dp[raw.length()][tokens][novel];
                if(s==null)continue;
                if(best==null||s.cost<best.cost)best=s;
            }
        }
        return best==null?null:best.text;
    }
}

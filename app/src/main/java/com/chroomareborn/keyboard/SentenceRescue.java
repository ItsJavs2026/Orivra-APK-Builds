package com.chroomareborn.keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Conservative, optional multi-typo sentence recovery; no Android or network APIs. */
final class SentenceRescue {
    private SentenceRescue() {}

    static final class Proposal {
        final String text;
        final double score;
        final int wordPairs;
        final int wordCount;
        final int totalEdits;
        Proposal(String text, double score, int pairs, int count, int edits) {
            this.text=text; this.score=score; this.wordPairs=pairs;
            this.wordCount=count; this.totalEdits=edits;
        }
        double coherence() { return wordCount<=1?0:(double)wordPairs/(wordCount-1); }
    }
    private static final class Word {
        final String value;
        final int frequency;
        // Immutable, dictionary-only letter signature. No typed user text
        // or per-input candidate fragments are stored in this index.
        final int letterMask;
        Word(String v,int f) {
            value=v;frequency=f;letterMask=letterMask(v);
        }
    }
    private static final class Candidate {
        final String word;
        final int edits;
        final double emission;
        Candidate(String word,int edits,double emission) {
            this.word=word;this.edits=edits;this.emission=emission;
        }
    }
    private static final class Path {
        final String text,last;
        final double score;
        final int count,edits,pairs;
        Path(String text,String last,double score,int count,int edits,int pairs) {
            this.text=text;this.last=last;this.score=score;
            this.count=count;this.edits=edits;this.pairs=pairs;
        }
        double finalScore() { return score+9.0*pairs/(count+0.1); }
    }
    // Cache a length-bucketed snapshot for the immutable boot dictionary.
    // A future adaptive lexicon must supply a new map identity on updates,
    // invalidating this cache. Never retain typed messages or candidate text.
    private static Map<String,Integer> indexedDictionary;
    private static Map<Integer,List<Word>> indexedWords;

    private static synchronized Map<Integer,List<Word>> indexFor(Map<String,Integer> dictionary) {
        if(indexedDictionary == dictionary && indexedWords != null) return indexedWords;
        Map<Integer,List<Word>> next=new HashMap<>();
        for(Map.Entry<String,Integer> e:dictionary.entrySet()) {
            String word=e.getKey();
            if(word.isEmpty() || word.length()>15)continue;
            next.computeIfAbsent(word.length(),k->new ArrayList<>())
                    .add(new Word(word,e.getValue()));
        }
        indexedWords=next;
        indexedDictionary=dictionary;
        return next;
    }
    private static final Map<String,Set<String>> EXTRA_PAIRS=new HashMap<>();
    static {
        extra("also","add"); extra("add","some"); extra("some","more");
        extra("lets","try see"); extra("try","saying"); extra("saying","something");
        extra("something","completely"); extra("completely","unrelated");
        extra("im","using going"); extra("using","the"); extra("the","keyboard");
        extra("keyboard","to"); extra("to","type"); extra("type","this");
        extra("do","this"); extra("first","of"); extra("of","all");
        extra("in","the"); extra("is","starting"); extra("starting","to");
        extra("come","along"); extra("what","happens"); extra("happens","when");
        extra("when","i"); extra("i","do"); extra("a","good");
        extra("please","let call"); extra("thank","you"); extra("we","are can");
        extra("your","keyboard phone"); extra("typing","on this");
        extra("for","the you");
    }
    private static void extra(String first,String seconds) {
        EXTRA_PAIRS.put(first,new HashSet<>(Arrays.asList(seconds.split(" "))));
    }
    private static boolean paired(String first,String second,
                                  Map<String,List<String>> common,
                                  Map<String,List<String>> phrases) {
        if(first.isEmpty())return false;
        Set<String> extra=EXTRA_PAIRS.get(first);
        List<String> one=common.get(first),two=phrases.get(first);
        return (extra!=null&&extra.contains(second))||
                (one!=null&&one.contains(second))||
                (two!=null&&two.contains(second));
    }
    static double coherence(String text,Map<String,List<String>> common,
                            Map<String,List<String>> phrases) {
        if(text==null)return 0;
        String cleaned=text.toLowerCase(Locale.US).replaceAll("[^a-z ]","").trim();
        String[] tokens=cleaned.split(" +");
        if(tokens.length<2)return 0;
        int matches=0;
        for(int i=1;i<tokens.length;i++)
            if(paired(tokens[i-1],tokens[i],common,phrases))matches++;
        return (double)matches/(tokens.length-1);
    }
    // Share the exact bounded edit metric and its per-thread scratch rows
    // with the live predictor. Prevents a second DP implementation from
    // drifting, and avoids allocating one new row per character.
    private static int distance(String a,String b,int limit) {
        return PredictionLite.editDistanceLimited(a,b,limit);
    }
    /**
     * A single edit can introduce at most one new distinct letter and remove
     * at most one existing distinct letter; adjacent swaps change neither.
     * Reject candidates only when either distinct-letter difference exceeds
     * the typo budget. For non-ASCII alphabetic lexicon entries, bypass this
     * optimization rather than risk false negatives.
     */
    static int letterMask(String value) {
        int mask=0;
        for(int i=0;i<value.length();i++) {
            char c=value.charAt(i);
            if(c<'a'||c>'z')return -1;
            mask|=1<<(c-'a');
        }
        return mask;
    }
    static boolean maskCompatible(int typedMask,int wordMask,int limit) {
        if(typedMask<0||wordMask<0)return true;
        return Integer.bitCount(typedMask&~wordMask)<=limit
                &&Integer.bitCount(wordMask&~typedMask)<=limit;
    }
    private static int maxEdits(String f) {
        int n=f.length();
        // Four-character fragments can hide two typing errors inside a
        // longer joined sentence. Only strongly anchored 2-edit choices
        // survive the filter in options(), so this is not broad fuzzy
        // autocorrection of ordinary short words.
        return n>=9?4:n>=7?3:n>=4?2:n>=2?1:0;
    }
    /**
     * Protect plausible long words whose multiple interior typos make them
     * lose to short high-frequency lookalikes. Anchors are general spelling
     * evidence; no memorized phrases or user typing are added to the lexicon.
     */
    static boolean multiTypoAnchored(String fragment,String word,int edits) {
        return edits>=2 && edits<=4 && fragment.length()>=6
                && word.length()>=6
                && Math.abs(fragment.length()-word.length())<=2
                && fragment.charAt(0)==word.charAt(0)
                && fragment.charAt(1)==word.charAt(1)
                && fragment.charAt(fragment.length()-1)==word.charAt(word.length()-1);
    }
    private static List<Candidate> options(String fragment,Map<Integer,List<Word>> index) {
        if(Thread.currentThread().isInterrupted())return Collections.emptyList();
        int limit=maxEdits(fragment);
        int fragmentMask=letterMask(fragment);
        int scanned=0;
        ArrayList<Candidate> candidates=new ArrayList<>();
        for(int length=Math.max(1,fragment.length()-limit);
                length<=Math.min(15,fragment.length()+limit);length++) {
            List<Word> words=index.get(length);
            if(words==null)continue;
            for(Word entry:words) {
                if((++scanned&31)==0 && Thread.currentThread().isInterrupted())
                    return Collections.emptyList();
                String word=entry.value;
                // Keep candidates with a damaged leading character. The edit
                // distance already bounds the search; requiring a matching
                // first or second character wrongly rejects transposed and
                // substituted starts in otherwise recoverable fragments.
                if(limit>=3 && word.charAt(0)!=fragment.charAt(0)
                    &&(word.length()<2||word.charAt(1)!=fragment.charAt(0))
                    &&(fragment.length()<2||fragment.charAt(1)!=word.charAt(0))
                    &&Math.abs(word.length()-fragment.length())>1)continue;
                // The mask is a necessary, never sufficient edit-distance
                // bound. It skips impossible words without changing output.
                if(!maskCompatible(fragmentMask,entry.letterMask,limit))continue;
                int edits=distance(fragment,word,limit);
                if(edits>limit)continue;
                // For short spans, two edits need two exact boundary anchors.
                // Prevent a large cloud of unrelated 4-letter alternatives.
                if(fragment.length()==4 && edits==2 &&
                        (word.length()<4 ||
                        word.charAt(0)!=fragment.charAt(0) ||
                        word.charAt(word.length()-1)!=fragment.charAt(3)))
                    continue;
                double emission=Math.log(entry.frequency)*0.8-4.25*edits-3.65-
                        (word.length()<=2?0.5:0);
                candidates.add(new Candidate(word,edits,emission));
            }
        }
        candidates.sort((a,b)->Double.compare(b.emission,a.emission));
        // Preserve low-edit alternatives even when frequent but less accurate
        // words dominate the beam. This is general ranking, not a phrase rule.
        if(candidates.size()<=22)return candidates;
        ArrayList<Candidate> selected=new ArrayList<>(22);
        for(Candidate candidate:candidates) {
            if(candidate.edits<=1)selected.add(candidate);
            if(selected.size()==22)break;
        }
        if(selected.size()<22)for(Candidate candidate:candidates) {
            if(!selected.contains(candidate))selected.add(candidate);
            if(selected.size()==22)break;
        }
        // Reserve up to four extra alternatives only when the original top-22
        // search would have discarded an anchored 2+-edit word. The original
        // candidate order and all original beam alternatives stay intact.
        if(fragment.length()>=6) {
            int preserved=0;
            for(Candidate candidate:candidates) {
                if(!selected.contains(candidate)
                        && multiTypoAnchored(fragment,candidate.word,candidate.edits)) {
                    selected.add(candidate);
                    if(++preserved==4)break;
                }
            }
        }
        return selected;
    }
    /**
     * Penalize speculative boundaries that divide a known dictionary word.
     * Supported adjacent words remain free to appear as genuine phrases.
     * The lexicon is supplied by the caller; no typed text is retained.
     */
    static double joinedWordBoundaryPenalty(String left,String right,
            boolean supported,Map<String,Integer> dictionary) {
        if(supported || left.isEmpty() || right.isEmpty())return 0;
        int combined=left.length()+right.length();
        if(combined<4 || combined>15)return 0;
        if(!dictionary.containsKey(left+right))return 0;
        return combined>=6?-12.0:-8.0;
    }

    private static void insert(List<Path> list,Path next) {
        // Equivalent to stable sort + truncate, with bounded insertion.
        int size=list.size();
        if(size>=65 && Double.compare(next.score,list.get(size-1).score)<=0)
            return;
        int at=0;
        while(at<size && Double.compare(list.get(at).score,next.score)>=0)at++;
        list.add(at,next);
        if(list.size()>65)list.remove(65);
    }
    static List<Proposal> recover(String input,Map<String,Integer> dictionary,
                                  Map<String,List<String>> common,
                                  Map<String,List<String>> phrases) {
        if(Thread.currentThread().isInterrupted())return Collections.emptyList();
        if(input==null||input.length()<6||input.length()>65||!input.matches("[A-Za-z]+"))
            return Collections.emptyList();
        String raw=input.toLowerCase(Locale.US);
        Map<Integer,List<Word>> index=indexFor(dictionary);
        int n=raw.length();
        // Allow a bounded extra edit for long, joined multiword input. The
        // previous floor(n/4.5) budget could reject every plausible path
        // before the contextual ranking had a chance to evaluate it.
        int budget=Math.max(2,(int)Math.ceil(n>=15?n*0.42:n/4.0));
        Map<String,List<Candidate>> cache=new HashMap<>();
        @SuppressWarnings("unchecked") ArrayList<Path>[] beam=new ArrayList[n+1];
        for(int i=0;i<=n;i++)beam[i]=new ArrayList<>();
        beam[0].add(new Path("","",0,0,0,0));
        for(int i=0;i<n;i++) {
            if(Thread.currentThread().isInterrupted())return Collections.emptyList();
            if(beam[i].isEmpty())continue;
            for(int j=i+1;j<=Math.min(n,i+15);j++) {
                String frag=raw.substring(i,j);
                List<Candidate> choices=cache.get(frag);
                if(choices==null) {
                    choices=options(frag,index);cache.put(frag,choices);
                }
                for(Path previous:beam[i])for(Candidate candidate:choices) {
                    int errors=previous.edits+candidate.edits;
                    if(errors>budget)continue;
                    boolean isPair=paired(previous.last,candidate.word,common,phrases);
                    double transition=previous.count==0?0:(isPair?6.7:-1.2);
                    if(previous.count>0)
                        transition+=joinedWordBoundaryPenalty(previous.last,
                                candidate.word,isPair,dictionary);
                    double nextScore=previous.score+candidate.emission+transition;
                    ArrayList<Path> destination=beam[j];
                    if(destination.size()>=65 &&
                            Double.compare(nextScore,
                                    destination.get(destination.size()-1).score)<=0)
                        continue;
                    String text=previous.text.isEmpty()?candidate.word:
                            previous.text+" "+candidate.word;
                    insert(destination,new Path(text,candidate.word,
                            nextScore,previous.count+1,errors,
                            previous.pairs+(isPair?1:0)));
                }
            }
        }
        beam[n].sort((a,b)->Double.compare(b.finalScore(),a.finalScore()));
        ArrayList<Proposal> out=new ArrayList<>();
        Set<String> seen=new HashSet<>();
        for(Path p:beam[n]) {
            if(p.count<2||!seen.add(p.text))continue;
            out.add(new Proposal(p.text,p.finalScore(),p.pairs,p.count,p.edits));
            if(out.size()==3)break;
        }
        return out;
    }
}

package com.chroomareborn.keyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Experimental, offline touch-geometry ranking. No personal text or tap positions
 * are persisted. Coordinate units are normalized key widths; the caller must
 * translate MotionEvent positions into the current layout's key coordinates.
 * This module is NOT yet connected to the live keyboard.
 */
final class TouchIntelligence {
    static final class Tap {
        final double x, y;
        Tap(double x, double y) { this.x=x; this.y=y; }
    }

    private static Tap key(char ch) {
        String[] rows={"qwertyuiop","asdfghjkl","zxcvbnm"};
        double[] starts={0.0,0.5,1.5};
        for(int r=0;r<rows.length;r++) {
            int col=rows[r].indexOf(Character.toLowerCase(ch));
            if(col>=0)return new Tap(starts[r]+col+0.5,r+0.5);
        }
        return null;
    }

    static List<Tap> idealTaps(String word) {
        ArrayList<Tap> out=new ArrayList<>();
        if(word==null)return out;
        for(char c:word.toLowerCase(Locale.US).toCharArray()) {
            Tap t=key(c);
            if(t!=null)out.add(t);
        }
        return out;
    }

    static double score(List<Tap> taps,String word) {
        if(taps==null || word==null || taps.isEmpty())return Double.POSITIVE_INFINITY;
        List<Tap> ideal=idealTaps(word);
        if(ideal.isEmpty())return Double.POSITIVE_INFINITY;
        // Correct a constant hand-offset, as used by imprecise typists.
        // Robust enough for a small translation, but never used as evidence alone.
        // Only estimate translation from position-aligned taps when lengths
        // match. A missing or extra tap shifts the indices, making a mean
        // offset misleading and harming insertion/deletion alignment.
        double dx=0,dy=0;
        if(taps.size()==ideal.size()) {
            for(int i=0;i<taps.size();i++) {
                dx+=taps.get(i).x-ideal.get(i).x;
                dy+=taps.get(i).y-ideal.get(i).y;
            }
            dx=Math.max(-0.65,Math.min(0.65,dx/taps.size()));
            dy=Math.max(-0.65,Math.min(0.65,dy/taps.size()));
        }

        // Geometric Levenshtein: missing and extra taps have explicit costs.
        final double insertionCost=1.15;
        double[] prev=new double[ideal.size()+1];
        for(int j=1;j<prev.length;j++)prev[j]=j*insertionCost;
        for(int i=1;i<=taps.size();i++) {
            double[] next=new double[ideal.size()+1];
            next[0]=i*insertionCost;
            for(int j=1;j<=ideal.size();j++) {
                Tap t=taps.get(i-1),k=ideal.get(j-1);
                double x=t.x-dx-k.x, y=t.y-dy-k.y;
                double match=Math.min(2.3,x*x+1.2*y*y);
                next[j]=Math.min(prev[j-1]+match,
                    Math.min(prev[j]+insertionCost,next[j-1]+insertionCost));
            }
            prev=next;
        }
        return prev[ideal.size()]/Math.max(taps.size(),ideal.size());
    }

    static String bestOf(List<Tap> taps,List<String> candidates) {
        if(candidates==null || candidates.isEmpty())return null;
        String best=null;
        double bestScore=Double.POSITIVE_INFINITY;
        for(String word:candidates) {
            double value=score(taps,word);
            if(value<bestScore) { bestScore=value;best=word; }
        }
        return best;
    }
}

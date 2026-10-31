package com.chroomareborn.keyboard;

import java.util.List;

/**
 * Tiny, deterministic swipe-selector for the existing prediction strip.
 * A gesture begins on a visible suggestion (never the letter keys or
 * spacebar); left/right navigate between physical chips. Swiping upward
 * chooses the first visible RISE alternative only if the panel is open.
 *
 * It does not access text editors, infer words, trigger actions, persist
 * touch paths, or participate in InputConnection timing.
 */
final class PredictionSwipe372 {
    private PredictionSwipe372() {}
    static final String PREF="prediction_slide_select_372";
    enum Direction { NONE, LEFT, RIGHT, UP }

    static Direction direction(float dx,float dy,float slop,boolean riseVisible) {
        if(!Float.isFinite(dx)||!Float.isFinite(dy))return Direction.NONE;
        final float threshold=Math.max(20f,slop*2f);
        final float ax=Math.abs(dx),ay=Math.abs(dy);
        if(ax<threshold && ay<threshold)return Direction.NONE;
        if(ax>=threshold && ax>=ay*1.2f)
            return dx<0?Direction.LEFT:Direction.RIGHT;
        if(riseVisible && dy<=-threshold && ay>=ax*1.2f)
            return Direction.UP;
        return Direction.NONE;
    }

    static String choose(Direction direction,int from,
            List<String> row,List<String> rise,boolean startOnRise) {
        if(direction==null || direction==Direction.NONE)return null;
        if(direction==Direction.UP) {
            if(startOnRise || rise==null)return null;
            for(String option:rise)if(valid(option))return option;
            return null;
        }
        List<String> active=startOnRise?rise:row;
        if(active==null || from<0 || from>=active.size())return null;
        int step=direction==Direction.LEFT?-1:1;
        for(int index=from+step;index>=0 && index<active.size();index+=step) {
            String option=active.get(index);
            if(valid(option))return option;
        }
        return null; // no wraparound and no accidental insertion
    }

    static boolean valid(String s) {
        return s!=null && !s.trim().isEmpty() && s.length()<=160;
    }

    static boolean safeRelease(String expected,String current,
            boolean sameEditor,boolean sameSession,boolean sameSelection,
            boolean privateEditor,boolean allowsSuggestions,
            boolean numeric,boolean selected,boolean symbols,
            boolean activePointer) {
        return expected!=null && expected.equals(current) &&
                sameEditor && sameSession && sameSelection &&
                !privateEditor && allowsSuggestions && !numeric &&
                !selected && !symbols && activePointer;
    }
}

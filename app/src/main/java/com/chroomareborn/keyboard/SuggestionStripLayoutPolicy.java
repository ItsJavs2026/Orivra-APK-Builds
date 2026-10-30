package com.chroomareborn.keyboard;

/**
 * Pure-Java, testable position mapping for an in-place suggestion row.
 * Rebuilding the entire chip strip during a single keystroke causes flicker.
 */
final class SuggestionStripLayoutPolicy {
    private SuggestionStripLayoutPolicy() {}

    /** Center compact next-word tiles only when no editor text is composing.
     * Keep active correction, cursor-edit and protected-field rows unchanged.
     * HorizontalScrollView.fillViewport supplies the extra row width.
     */
    static boolean centerIdleSuggestions(String input,int count,boolean sensitive) {
        return !sensitive && (input==null || input.isEmpty()) && count>0;
    }

    static int shownCount(int optionCount, boolean stableSentenceStrip) {
        int count=Math.max(0,optionCount);
        return stableSentenceStrip ? Math.min(1,count) : count;
    }

    static boolean dividerAfter(int index,int count,boolean emptyInput) {
        return index>=0 && index<count-1 && (emptyInput||index>=2);
    }

    static int childCount(int count,boolean emptyInput) {
        int shown=Math.max(0,count);
        return shown+(emptyInput?Math.max(0,shown-1):Math.max(0,shown-3));
    }

    static int chipChildIndex(int index,boolean emptyInput) {
        if(index<0)return -1;
        return emptyInput ? index*2 : index+Math.max(0,index-2);
    }
}

package com.chroomareborn.keyboard;

/**
 * Pure, deterministic visual-only suggestion styling.
 * A highlighted option is the current display candidate, NOT a promise
 * that a language model verified spelling or correctness.
 */
public final class SuggestionVisualStyle {
    private SuggestionVisualStyle() {}

    public static boolean highlight(String input,String candidate,
                                    int index,int emphasisIndex) {
        if(input==null || candidate==null || input.trim().isEmpty()
                || candidate.trim().isEmpty() || index!=emphasisIndex)return false;
        // Never portray unchanged input as a confirmed correction.
        return !input.trim().equalsIgnoreCase(candidate.trim());
    }

    private static int channel(int a,int b,float targetWeight) {
        return Math.max(0,Math.min(255,Math.round(a*(1f-targetWeight)+b*targetWeight)));
    }
    private static int blend(int a,int b,float towardB) {
        int alpha=channel((a>>>24)&255,(b>>>24)&255,towardB);
        int red=channel((a>>>16)&255,(b>>>16)&255,towardB);
        int green=channel((a>>>8)&255,(b>>>8)&255,towardB);
        int blue=channel(a&255,b&255,towardB);
        return alpha<<24|red<<16|green<<8|blue;
    }

    /** Neutral chips blend into the theme; the selected correction is
     * slightly darker in light mode, and slightly lighter in dark mode,
     * so the contrast remains perceptible without over-promising accuracy.
     */
    public static int chipColor(int suggestionBg,int keyBg,boolean dark,
                                boolean emphasis) {
        int base=blend(suggestionBg,keyBg,emphasis?.31f:.17f);
        if(!emphasis)return base;
        // Subtle contrast lift: 6% darker light chips, 7% lighter dark chips.
        return blend(base,dark?0xffffffff:0xff000000,dark?.07f:.06f);
    }

    public static int textSizeSp(boolean sentence,int codePoints) {
        if(sentence || codePoints>28)return 15;
        if(codePoints>18)return 16;
        return 18;
    }

    public static float letterSpacing(boolean sentence) {
        return sentence?.004f:.006f;
    }
}

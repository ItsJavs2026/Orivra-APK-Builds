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
        int neutral=blend(suggestionBg,keyBg,.17f);
        if(!emphasis)return neutral;
        // Shift the same neutral tone toward dark/light so emphasis cannot
        // inadvertently reverse when a theme's key color is very different.
        return blend(neutral,dark?0xffffffff:0xff000000,dark?.09f:.08f);
    }

    private static double linear(int byteChannel) {
        double v=byteChannel/255.0;
        return v<=.04045 ? v/12.92 : Math.pow((v+.055)/1.055,2.4);
    }

    public static double contrastRatio(int foreground,int background) {
        double fg=.2126*linear((foreground>>>16)&255)+
                .7152*linear((foreground>>>8)&255)+
                .0722*linear(foreground&255);
        double bg=.2126*linear((background>>>16)&255)+
                .7152*linear((background>>>8)&255)+
                .0722*linear(background&255);
        return (Math.max(fg,bg)+.05)/(Math.min(fg,bg)+.05);
    }

    /**
     * Preserve the selected theme's text where it is legible. Otherwise
     * fall back to a high-contrast neutral WITHOUT altering suggestion text,
     * ranking, or the editor. Target WCAG AA 4.5:1 for small labels.
     */
    public static int readableText(int preferred,int tileColor) {
        if(contrastRatio(preferred,tileColor)>=4.5d)return preferred;
        int nearBlack=0xff202229;
        int white=0xffffffff;
        return contrastRatio(nearBlack,tileColor)>=
                contrastRatio(white,tileColor) ? nearBlack : white;
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

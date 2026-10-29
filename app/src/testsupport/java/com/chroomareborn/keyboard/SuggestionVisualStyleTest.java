package com.chroomareborn.keyboard;

import java.util.Random;

/** Visual-only policy: no predictor scores or editor mutations. */
public final class SuggestionVisualStyleTest {
    private static long checks;
    private static void ok(boolean good,String reason) {
        checks++;
        if(!good)throw new AssertionError(reason);
    }
    private static int brightness(int c) {
        return (int)Math.round(((c>>>16)&255)*.2126+
                ((c>>>8)&255)*.7152+(c&255)*.0722);
    }
    public static void main(String[] args) {
        ok(SuggestionVisualStyle.highlight("likethis","like this",0,0),
                "connected text correction is eligible");
        ok(SuggestionVisualStyle.highlight("helo","hello",1,1),
                "center suggestion may be highlighted");
        ok(!SuggestionVisualStyle.highlight("hello","hello",1,1),
                "original text must not be presented as corrected");
        ok(!SuggestionVisualStyle.highlight("hello","hello",2,1),
                "noncentral option should not claim emphasis");
        ok(!SuggestionVisualStyle.highlight("","hello",0,0),
                "empty input is not a spelling correction");
        ok(!SuggestionVisualStyle.highlight(null,"word",0,0),"null input");
        ok(!SuggestionVisualStyle.highlight("word",null,0,0),"null suggestion");
        ok(SuggestionVisualStyle.textSizeSp(true,4)==15,"long sentence font size");
        ok(SuggestionVisualStyle.textSizeSp(false,8)==18,"single word font size");
        ok(SuggestionVisualStyle.textSizeSp(false,22)==16,"longer word font size");
        ok(SuggestionVisualStyle.letterSpacing(true)<.01f,"sentence letter spacing");
        Random random=new Random(294303L);
        for(int i=0;i<75000;i++) {
            int r=64+random.nextInt(128),g=64+random.nextInt(128),
                    b=64+random.nextInt(128);
            int base=0xff000000|r<<16|g<<8|b;
            int key=0xff000000|(r+17)%256<<16|(g+13)%256<<8|(b+19)%256;
            for(boolean dark:new boolean[]{false,true}) {
                int neutral=SuggestionVisualStyle.chipColor(base,key,dark,false);
                int emphasized=SuggestionVisualStyle.chipColor(base,key,dark,true);
                ok((neutral>>>24)==255 && (emphasized>>>24)==255,
                        "alpha must stay opaque");
                ok(dark ? brightness(emphasized)>=brightness(
                        SuggestionVisualStyle.chipColor(base,key,dark,false))
                        : brightness(emphasized)<=brightness(neutral),
                        "emphasis must honor appearance");
                // Every chip/text pair must remain legible even when a
                // Material You or app-derived palette supplies low-contrast
                // suggestion text.
                int riskyText=random.nextBoolean()?neutral:0xff888888;
                int text=SuggestionVisualStyle.readableText(riskyText,emphasized);
                ok(SuggestionVisualStyle.contrastRatio(text,emphasized)>=4.5d,
                        "highlight text did not meet minimum readability");
                ok(SuggestionVisualStyle.readableText(0xff202229,0xffffffff)
                        ==0xff202229,"already-legible text was modified");
                int size=SuggestionVisualStyle.textSizeSp(random.nextBoolean(),
                        random.nextInt(50));
                ok(size>=15 && size<=18,"font must remain legible");
            }
        }
        System.out.println("SuggestionVisualStyleTest passed: 75k colors, "+
                checks+" assertions");
    }
}

package com.chroomareborn.keyboard;

import java.util.Random;

/** Standalone off-device fixtures. No app screen/video or user data. */
public final class AppIconColorSignature366Research {
    private static int checks;
    private static void ok(boolean yes,String label) {
        checks++;
        if(!yes)throw new AssertionError(label);
    }
    private static void hue(float expected,float actual,float tolerance,
            String label) {
        checks++;
        float diff=Math.abs(expected-actual);
        diff=Math.min(diff,360f-diff);
        if(diff>tolerance)
            throw new AssertionError(label+" expected "+expected+
                    "±"+tolerance+" got "+actual);
    }
    private static final int N=64;
    private static int[] fill(int color) {
        int[] pixels=new int[N*N];
        java.util.Arrays.fill(pixels,color);
        return pixels;
    }
    private static void rect(int[] pixels,int l,int t,int r,int b,int color) {
        for(int y=t;y<b;y++)for(int x=l;x<r;x++)pixels[y*N+x]=color;
    }
    private static AppIconColorSignature366.Match detect(int[] pixels) {
        return AppIconColorSignature366.detect(pixels,N,N);
    }
    public static void main(String[] args) {
        int red=0xFFFF0000,blue=0xFF0000FF,green=0xFF00CC44;
        AppIconColorSignature366.Match c=detect(fill(red));
        ok(c!=null,"Solid app red detected");
        hue(0,c.hue,6,"Red hue accurate");
        c=detect(fill(blue));
        ok(c!=null,"Solid app blue detected");
        hue(240,c.hue,6,"Blue hue accurate");

        int[] icon=fill(0xFFFFFFFF);
        rect(icon,10,12,54,50,red);
        c=detect(icon);
        ok(c!=null,"White background with red center identified");
        hue(0,c.hue,6,"Do not select white");
        ok(c.chromaticCoverage>.2f,"Has meaningful colorful coverage");

        icon=fill(0xFF202124);
        rect(icon,14,14,50,50,green);
        c=detect(icon);
        ok(c!=null,"Black/dark app icon with green symbol");
        hue(140,c.hue,16,"Green brand accent");

        // A tiny saturated notification dot is NOT a brand color.
        icon=fill(0xFF888888);
        rect(icon,0,0,6,6,red);
        ok(detect(icon)==null,"Tiny red corner should not tint keyboard");

        ok(detect(fill(0xFF111111))==null,"Dark monochrome fallback");
        ok(detect(fill(0xFFFFFFFF))==null,"White monochrome fallback");
        ok(detect(fill(0xFF777777))==null,"Grey monochrome fallback");
        ok(detect(fill(0x00000000))==null,"Fully transparent fallback");

        icon=fill(0xFFF7F7F7);
        rect(icon,0,0,32,64,red);
        rect(icon,32,0,64,64,blue);
        ok(detect(icon)==null,"Equal unrelated brand colors are ambiguous");

        // Hue-circle wrap must not classify red 355° and 5° as opposite.
        icon=fill(0x00000000);
        rect(icon,0,0,32,64,0xFFFF0022);
        rect(icon,32,0,64,64,0xFFFF2200);
        c=detect(icon);
        ok(c!=null,"Related red hues merge across zero");
        hue(0,c.hue,15,"Circular mean at red wrap");

        ok(AppIconColorSignature366.detect(null,64,64)==null,
                "Null pixels fail closed");
        ok(AppIconColorSignature366.detect(new int[20],64,64)==null,
                "Bad dimensions fail closed");
        ok(AppIconColorSignature366.detect(new int[65536],1000,1000)==null,
                "Excessive dimensions fail closed");

        // Random input fuzz: no crash and any reported hue is finite/in-range.
        Random rand=new Random(36620261030L);
        for(int i=0;i<2000;i++) {
            int[] random=new int[N*N];
            for(int j=0;j<random.length;j++)
                random[j]=rand.nextInt()|0xFF000000;
            c=detect(random);
            if(c!=null) {
                ok(!Float.isNaN(c.hue) && c.hue>=0 && c.hue<360,
                        "Fuzz hue bounded "+i);
                ok(c.concentration>=.53f,"Fuzz concentration "+i);
            } else ok(true,"Fuzz multi-color fallback "+i);
        }
        System.out.println("PASS Research 366: "+checks+
                " contrast, ambiguity, opaque/transparent and color fixtures");
    }
}

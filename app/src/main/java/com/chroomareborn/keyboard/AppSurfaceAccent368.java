package com.chroomareborn.keyboard;

/**
 * Neutral shaded keyboard surface with a deliberately tiny app-brand hint.
 * The brand hue NEVER controls the overall background or letter-key colors.
 * No image, package name, screenshot, editor text, or Android dependency.
 */
final class AppSurfaceAccent368 {
    private AppSurfaceAccent368() {}
    static final int BG=0,KEY=1,SPECIAL=2,ACCENT=3,SUGGESTION=4;

    static int[] colors(Float brandHue,boolean dark) {
        // Fixed neutral surfaces for predictable readability in app UIs.
        int bg=dark?0xFF202328:0xFFF3F5F7;
        int key=dark?0xFF353A43:0xFFFFFFFF;
        int baseSpecial=dark?0xFF414752:0xFFE5EAF0;
        if(brandHue==null || !Float.isFinite(brandHue)) {
            return new int[]{bg,key,baseSpecial,
                    dark?0xFFB6BECA:0xFF687589,key};
        }
        float hue=(brandHue%360f+360f)%360f;
        int accent=hsv(hue,dark?.48f:.60f,dark?.94f:.59f);
        int special=mix(baseSpecial,accent,dark?.12f:.09f);
        int suggestion=mix(key,accent,dark?.075f:.04f);
        return new int[]{bg,key,special,accent,suggestion};
    }

    private static int hsv(float h,float s,float v) {
        float c=v*s, hp=h/60f, x=c*(1-Math.abs(hp%2f-1f));
        float r=0,g=0,b=0;
        if(hp<1){r=c;g=x;}
        else if(hp<2){r=x;g=c;}
        else if(hp<3){g=c;b=x;}
        else if(hp<4){g=x;b=c;}
        else if(hp<5){r=x;b=c;}
        else {r=c;b=x;}
        float m=v-c;
        return 0xFF000000|(clamp((r+m)*255)<<16)|
                (clamp((g+m)*255)<<8)|clamp((b+m)*255);
    }
    private static int clamp(float value) {
        return Math.max(0,Math.min(255,Math.round(value)));
    }
    private static int mix(int a,int b,float t) {
        float keep=1-t;
        return 0xFF000000|
                (clamp((((a>>16)&255)*keep)+(((b>>16)&255)*t))<<16)|
                (clamp((((a>>8)&255)*keep)+(((b>>8)&255)*t))<<8)|
                clamp(((a&255)*keep)+((b&255)*t));
    }
}

package com.chroomareborn.keyboard;

/**
 * Synthetic color research, not proof of app UI color matching.
 * Verifies app-specific accents cannot tint the whole keyboard surface.
 */
public final class AppSurfaceAccent368Research {
    private static int checks;
    private static void yes(boolean cond,String label) {
        checks++;if(!cond)throw new AssertionError(label);
    }
    private static int rgbDistance(int a,int b) {
        return Math.max(Math.max(Math.abs((a>>16&255)-(b>>16&255)),
                Math.abs((a>>8&255)-(b>>8&255))),
                Math.abs((a&255)-(b&255)));
    }
    private static double luminance(int color) {
        double r=(color>>16&255)/255d,g=(color>>8&255)/255d,
                b=(color&255)/255d;
        r=r<=.04045?r/12.92:Math.pow((r+.055)/1.055,2.4);
        g=g<=.04045?g/12.92:Math.pow((g+.055)/1.055,2.4);
        b=b<=.04045?b/12.92:Math.pow((b+.055)/1.055,2.4);
        return .2126*r+.7152*g+.0722*b;
    }
    public static void main(String[] args) {
        for(boolean dark:new boolean[]{false,true}) {
            int[] noHue=AppSurfaceAccent368.colors(null,dark);
            int[] red=AppSurfaceAccent368.colors(0f,dark);
            int[] blue=AppSurfaceAccent368.colors(240f,dark);
            int[] green=AppSurfaceAccent368.colors(142f,dark);
            for(int[] palette:new int[][]{noHue,red,blue,green}) {
                yes(palette.length==5,"Complete palette");
                for(int color:palette)
                    yes((color>>>24)==255,"All colors opaque");
                yes(dark?luminance(palette[0])<.12:
                        luminance(palette[0])>.80,
                        "Surface remains appropriately dark/light");
                yes(dark?luminance(palette[1])<.18:
                        luminance(palette[1])>.97,
                        "Letter keys remain neutral readable");
            }
            // Changing apps must not cause the whole keyboard to flash.
            yes(red[0]==blue[0] && red[0]==green[0],
                    "App hue cannot change main background");
            yes(red[1]==blue[1] && red[1]==green[1],
                    "App hue cannot change letter keys");
            yes(red[3]!=blue[3] && red[3]!=green[3],
                    "App identity still visible through accent");
            yes(rgbDistance(red[2],blue[2])<=35,
                    "Special keys never become saturated");
            yes(rgbDistance(red[4],blue[4])<=25,
                    "Suggestion row gets only a faint tint");
            yes(noHue[4]==noHue[1],
                    "Unknown app keeps suggestion background neutral");
            yes(noHue[3]!=0,"Neutral fallback accent exists");
            yes(AppSurfaceAccent368.colors(Float.NaN,dark)[0]==noHue[0],
                    "Bad hue safe fallback");
        }
        for(int degree=0;degree<360;degree++) {
            int[] p=AppSurfaceAccent368.colors((float)degree,false);
            yes(p[0]==0xFFF3F5F7,"Light surface invariant "+degree);
            yes(p[1]==0xFFFFFFFF,"Light key invariant "+degree);
            yes(rgbDistance(p[4],p[1])<25,
                    "Suggestion bar faint accent "+degree);
            p=AppSurfaceAccent368.colors((float)degree,true);
            yes(p[0]==0xFF202328,"Dark surface invariant "+degree);
            yes(p[1]==0xFF353A43,"Dark key invariant "+degree);
            yes(rgbDistance(p[4],p[1])<25,
                    "Dark suggestion bar faint accent "+degree);
        }
        System.out.println("PASS Research 368: "+checks+
                " neutral-surface, contrast and per-app accent assertions");
    }
}

package com.chroomareborn.keyboard;

/** Android-independent relative luminance contrast for keyboard legibility. */
final class AppPaletteText {
    private AppPaletteText() {}
    private static double linear(int channel) {
        double c=channel/255.0;
        return c<=.04045?c/12.92:Math.pow((c+.055)/1.055,2.4);
    }
    static int contrast(int background) {
        double y=.2126*linear((background>>16)&255)+
                .7152*linear((background>>8)&255)+
                .0722*linear(background&255);
        // Black on lighter accent colors, white on darker surfaces.
        // Compare WCAG contrast ratios for each candidate.
        double white=1.05/(y+.05),black=(y+.05)/.05;
        return white>=black?0xFFFFFFFF:0xFF202229;
    }
}

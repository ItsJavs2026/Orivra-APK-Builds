package com.chroomareborn.keyboard;

/** Stable Android dp sizing. Key boundaries do not move while fingers type. */
final class KeyboardSizing {
    static final String PREFS="reborn_size";
    static final String KEY_MODE="height_mode";
    static final int STANDARD=0, TALL=1, EXTRA_TALL=2;
    static final int MODE_COUNT=3, DEFAULT=TALL;
    private KeyboardSizing() {}

    static int normalize(int mode) {
        return mode>=STANDARD && mode<MODE_COUNT ? mode : DEFAULT;
    }
    static int next(int mode) { return (normalize(mode)+1)%MODE_COUNT; }
    static String name(int mode) {
        switch(normalize(mode)) {
            case STANDARD: return "Standard";
            case EXTRA_TALL: return "Extra Tall";
            default: return "Tall";
        }
    }
    static int keyHeightDp(int mode,int row,boolean landscape) {
        int chosen=normalize(mode);
        int base;
        if(landscape)base=chosen==STANDARD?44:chosen==EXTRA_TALL?56:50;
        else base=chosen==STANDARD?58:chosen==EXTRA_TALL?84:72;
        // Top row gets a little extra height where thumb misses were reported.
        return base + (row==0 && chosen!=STANDARD ? (landscape?2:5):0);
    }
}

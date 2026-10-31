package com.chroomareborn.keyboard;

/**
 * A deliberately tiny, static edge-key width A/B experiment.
 * Redistributes weight INSIDE each alphabet row; never moves rows, symbols,
 * Space, Enter, Backspace or Shift, and never changes bounds mid-touch.
 * Normal interior widths shrink by ~3%; first and last letters gain 8-10%.
 */
final class EdgeKeyWeights378 {
    static final String PREF="fat_thumb_edge_letters_378";
    private EdgeKeyWeights378() {}
    static float weight(int row,int col,int count){
        if(row<0 || row>2 || count<7 || count>10 ||
                col<0 || col>=count)return 1f;
        final float edge=row==2?1.08f:1.10f;
        return col==0 || col==count-1?edge:
                (count-2f*edge)/(count-2);
    }
}

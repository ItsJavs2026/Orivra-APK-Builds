package com.chroomareborn.keyboard;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

/**
 * Keep the ordinary active-word prediction strip structurally stable while
 * new synchronous and asynchronous suggestions arrive during rapid typing.
 *
 * Exactly three persistent TextViews (even for one visible prediction). An
 * INVISIBLE empty cell reserves its width, but is not clickable. Neither
 * prediction generation nor automatic Rise behavior is modified here.
 * Longer full-sentence correction layouts retain their existing path.
 */
final class PredictionStripFrame344 {
    private static final String ACTIVE_ROW_TAG="active-three-slots-344";
    private PredictionStripFrame344() {}

    static boolean stableWord(String input,boolean sensitive,
            boolean editorAllowsSuggestions,boolean longPhrase,
            boolean stableSentenceStrip) {
        if(sensitive || !editorAllowsSuggestions || longPhrase ||
                stableSentenceStrip || input==null ||
                input.isEmpty() || input.length()>24)return false;
        // Ordinary words including incomplete contractions; no raw input saved.
        return input.matches("[A-Za-z]+(?:'[A-Za-z]+)*'?");
    }

    static int shownCount(int suppliedCount,boolean stable) {
        return stable?3:Math.max(0,suppliedCount);
    }

    /** Only structural transitions create views. Calls for every character
     * reuse exactly the same TextView objects and layout positions.
     */
    static boolean ensureThreeSlots(LinearLayout row,Context context) {
        if(row==null || context==null)
            throw new IllegalArgumentException("Missing candidate row");
        boolean reusable=ACTIVE_ROW_TAG.equals(row.getTag()) &&
                row.getChildCount()==3;
        if(reusable){
            for(int i=0;i<3;i++)
                if(!(row.getChildAt(i) instanceof TextView)){
                    reusable=false;
                    break;
                }
        }
        if(reusable)return false;
        row.removeAllViews();
        row.setTag(ACTIVE_ROW_TAG);
        for(int i=0;i<3;i++) {
            TextView cell=new TextView(context);
            cell.setVisibility(View.INVISIBLE);
            row.addView(cell);
        }
        return true;
    }

    static String slotValue(List<String> options,int index) {
        return index>=0 && options!=null && index<options.size() ?
                options.get(index):"";
    }
}

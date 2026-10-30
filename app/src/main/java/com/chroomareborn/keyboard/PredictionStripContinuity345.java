package com.chroomareborn.keyboard;

import android.view.View;
import android.widget.TextView;

/**
 * Visible normal-bar continuity between a one-choice immediate key frame and
 * the fuller asynchronous prediction result. Research from Samsung screen
 * recording 85088.mp4: text/selection updates quickly while chips 2/3
 * disappeared and reappeared at roughly frame cadence. Build 344 kept the
 * TextView instances but still toggled their visibility on each key.
 *
 * For at most 500 ms after a validated multi-choice batch, preserve the
 * PREVIOUS visible text of side chips when the next key extends/backspaces
 * the same short word. Preserved chips are NOT actionable, focusable or
 * exposed to accessibility until a current candidate is rendered. This does
 * not keep model predictions, raw key events or user text outside the
 * current editor's in-memory view. Rise, ranking and backup are unaffected.
 */
final class PredictionStripContinuity345 {
    static final long MAX_STALE_AGE_MS=500L;
    private PredictionStripContinuity345(){}

    static boolean oneStepSameWord(String previous,String current) {
        if(previous==null || current==null || previous.length()<2 ||
                current.length()<2 || previous.length()>24 ||
                current.length()>24)return false;
        if(!previous.matches("[A-Za-z]+(?:'[A-Za-z]+)*'?") ||
                !current.matches("[A-Za-z]+(?:'[A-Za-z]+)*'?"))
            return false;
        if(Math.abs(previous.length()-current.length())>1)return false;
        // Only the same word in progress, with one character typed, erased
        // or a duplicate re-render. New words never inherit stale chips.
        return previous.regionMatches(true,0,current,0,
                Math.min(previous.length(),current.length()));
    }

    static boolean preserveInterimSideChips(String previous,String current,
            boolean preliminary,boolean stableWord,boolean sameEditorSession,
            long ageSinceCompleteMs) {
        return preliminary && stableWord && sameEditorSession &&
                ageSinceCompleteMs>=0 && ageSinceCompleteMs<=MAX_STALE_AGE_MS &&
                oneStepSameWord(previous,current);
    }

    /** Keeps prior text/background/geometry, but disables stale interaction.
     * Caller invokes only for an UNUSED slot of a preliminary prediction
     * render, never for a real current prediction.
     */
    static boolean holdVisualOnly(TextView cell) {
        if(cell==null || cell.getVisibility()!=View.VISIBLE ||
                cell.getText()==null || cell.length()==0)return false;
        cell.setOnClickListener(null);
        cell.setOnLongClickListener(null);
        cell.setClickable(false);
        cell.setContentDescription(null);
        cell.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return true;
    }

    /** Re-enable access only after the caller installs a CURRENT tap handler. */
    static void currentChip(TextView cell) {
        if(cell!=null && cell.getImportantForAccessibility()!=
                View.IMPORTANT_FOR_ACCESSIBILITY_AUTO)
            cell.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
    }
}

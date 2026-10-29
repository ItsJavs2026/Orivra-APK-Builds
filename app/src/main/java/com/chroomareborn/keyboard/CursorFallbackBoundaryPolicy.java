package com.chroomareborn.keyboard;

/**
 * Prevents DPAD cursor fallback from leaving an editor's text field.
 *
 * Some custom editors report no absolute SurroundingText offset. When an
 * absolute setSelection is unavailable, arrow-key fallback must only run
 * when a live editor snapshot proves there is text in that direction.
 * Otherwise a DPAD_RIGHT at the end may transfer focus to an adjacent view.
 */
final class CursorFallbackBoundaryPolicy {
    private CursorFallbackBoundaryPolicy() {}

    static int allowedArrowEvents(int requestedSteps, CharSequence adjacentText) {
        if(requestedSteps==0 || adjacentText==null || adjacentText.length()==0)
            return 0;
        // Code points keep a surrogate-pair emoji from counting as two
        // separate right-key opportunities at the end of an editor.
        int codepoints=Character.codePointCount(adjacentText,0,adjacentText.length());
        if(codepoints<=0)return 0;
        int requested=requestedSteps==Integer.MIN_VALUE?
                Integer.MAX_VALUE:Math.abs(requestedSteps);
        return Math.min(2,Math.min(requested,codepoints));
    }
}

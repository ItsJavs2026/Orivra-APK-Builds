package com.chroomareborn.keyboard;

/**
 * Research 393: narrow, passive stale-state recovery for chat composers
 * externally cleared by a host Send action without an input-session restart.
 *
 * No input field text is persisted, logged or modified. A user selection or
 * nonempty live editor always fails closed. Bounded 1-character snapshots
 * are taken only at a reported caret position of 0.
 */
final class ChatComposerReset393 {
    private ChatComposerReset393(){}

    static boolean shouldRecover(int cursorStart,int cursorEnd,
            CharSequence liveBefore,CharSequence liveAfter,
            String composition,String visualInput,boolean staleMiddle,
            int visibleCount,boolean idleTicketCurrent,boolean privateEditor) {
        if(privateEditor || cursorStart!=0 || cursorEnd!=0 ||
                liveBefore==null || liveAfter==null ||
                liveBefore.length()!=0 || liveAfter.length()!=0 ||
                composition==null || visualInput==null ||
                visibleCount<0)return false;
        return !composition.isEmpty() || !visualInput.isEmpty() ||
                staleMiddle || visibleCount==0 || !idleTicketCurrent;
    }
}

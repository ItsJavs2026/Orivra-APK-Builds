package com.chroomareborn.keyboard;

/**
 * Build 416C: keep the same *validated* cursor-word ticket across the
 * immediate-to-worker paint transition, including keyboard hide/reopen.
 *
 * No editor reads, text persistence or network activity in this policy.
 * The actual tap MUST still call CursorEditSnapshotGuard.current() against
 * fresh editor text and CursorWordCorrectionPolicy.plan() before mutation.
 */
final class CursorWordChipTicket416C {
    private CursorWordChipTicket416C() {}

    static boolean preserve(String ticketWord,String displayedInput,
            long ticketRevision,long currentRevision,
            int ticketStart,int ticketEnd,int currentStart,int currentEnd,
            boolean composing,boolean selected,boolean sensitive) {
        return ticketWord!=null && !ticketWord.isEmpty() &&
                displayedInput!=null && displayedInput.equals(ticketWord) &&
                ticketRevision==currentRevision &&
                ticketStart==ticketEnd &&
                currentStart==currentEnd &&
                ticketStart==currentStart && ticketEnd==currentEnd &&
                !composing && !selected && !sensitive;
    }
}

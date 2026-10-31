package com.chroomareborn.keyboard;

import java.util.Random;

/** Synthetic selection-callback traces; no typed user text or editor IO. */
public final class LongDraftCaretState375Research {
    private static int tests;
    private static void check(boolean expected,boolean actual,String reason) {
        tests++;
        if(expected!=actual)throw new AssertionError(reason);
    }
    public static void main(String[] args) {
        check(false,LongDraftCaretState375.editingWithoutComposition(false,false),
                "Caret at end of known word must not be middle edit");
        check(true,LongDraftCaretState375.editingWithoutComposition(false,true),
                "Caret before any suffix is editing in middle");
        check(true,LongDraftCaretState375.editingWithoutComposition(true,false),
                "Selection at end still editing selection");
        check(true,LongDraftCaretState375.editingWithoutComposition(true,true),
                "Selected range with suffix");
        check(false,LongDraftCaretState375.afterHostEndedComposition(false,false),
                "Host -1 composing bounds at document end safe");
        check(true,LongDraftCaretState375.afterHostEndedComposition(false,true),
                "Host -1 composing bounds in middle retains editor control");
        check(true,LongDraftCaretState375.afterHostEndedComposition(true,false),
                "Host -1 selected range must remain selected");
        Random random=new Random(37520261030L);
        boolean editing=true;
        // Model up to 100,000 callbacks where the editor repeatedly reports
        // no composition bounds. No text-end state may remain sticky.
        for(int i=0;i<100000;i++) {
            boolean selected=random.nextInt(25)==0;
            boolean following=random.nextBoolean();
            boolean hostJustEnded=random.nextBoolean();
            editing=hostJustEnded
                    ?LongDraftCaretState375.afterHostEndedComposition(
                            selected,following)
                    :LongDraftCaretState375.editingWithoutComposition(
                            selected,following);
            check(selected||following,editing,
                    "No sticky end-of-document state at callback "+i);
            if(i%101==0) {
                check(false,LongDraftCaretState375.editingWithoutComposition(
                        false,false),"Recover to real text end after random edits");
            }
        }
        System.out.println("PASS Research 375: "+tests+
                " trailing-text/selection/caret state assertions");
    }
}

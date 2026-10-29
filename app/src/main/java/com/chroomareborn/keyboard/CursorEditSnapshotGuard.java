package com.chroomareborn.keyboard;

import java.util.Objects;

/**
 * Candidate-only safety policy for an asynchronous editing suggestion.
 * Holds only transient bounded editor snapshots (never logs/persists them).
 * This class never modifies editor contents or chooses replacement words.
 * A tap still requires CursorWordCorrectionPolicy.plan() on live text.
 */
final class CursorEditSnapshotGuard {
    static final int WINDOW=64;
    static final class Ticket {
        final long sessionVersion;
        final int selectionStart,selectionEnd;
        final String word, before, after;
        private Ticket(long version,int start,int end,
                       String expected,String left,String right) {
            sessionVersion=version;
            selectionStart=start;selectionEnd=end;
            word=expected;before=left;after=right;
        }
    }
    private CursorEditSnapshotGuard(){}

    private static String currentWord(CharSequence before,CharSequence after) {
        if(before==null||after==null||before.length()>WINDOW||after.length()>WINDOW)
            return null;
        int left=before.length();
        while(left>0&&Character.isLetter(before.charAt(left-1)))left--;
        int right=0;
        while(right<after.length()&&Character.isLetter(after.charAt(right)))right++;
        if(left==before.length()&&right==0)return null;
        if((left==0&&before.length()==WINDOW) ||
            (right==after.length()&&after.length()==WINDOW))return null;
        return before.subSequence(left,before.length()).toString()+
                after.subSequence(0,right).toString();
    }
    static Ticket capture(String expected,CharSequence before,CharSequence after,
                          long version,int start,int end,
                          boolean selected,boolean composing,boolean sensitive) {
        if(selected||composing||sensitive||expected==null||expected.length()<2||
            expected.length()>WINDOW||start>=0&&end>=0&&start!=end ||
            !expected.equals(currentWord(before,after)))return null;
        return new Ticket(version,start,end,expected,before.toString(),after.toString());
    }
    static boolean current(Ticket ticket,CharSequence liveBefore,CharSequence liveAfter,
                           long version,int start,int end,
                           boolean selected,boolean composing,boolean sensitive) {
        return ticket!=null && !selected && !composing && !sensitive &&
            ticket.sessionVersion==version &&
            ticket.selectionStart==start && ticket.selectionEnd==end &&
            liveBefore!=null && liveAfter!=null &&
            Objects.equals(ticket.before,liveBefore.toString()) &&
            Objects.equals(ticket.after,liveAfter.toString()) &&
            Objects.equals(ticket.word,currentWord(liveBefore,liveAfter));
    }
}

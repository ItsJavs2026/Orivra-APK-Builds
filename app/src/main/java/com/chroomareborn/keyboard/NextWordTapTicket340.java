package com.chroomareborn.keyboard;

import java.util.Objects;

/**
 * A transient check that a next-word/phrase chip still belongs to the same
 * editor and untouched caret boundary where it was rendered.
 * No text is persisted, logged or sent over a network.
 */
final class NextWordTapTicket340 {
    static final int BEFORE_LIMIT=160;
    private final String before;
    private final String after;
    private final long session;
    private final int cursorStart,cursorEnd;
    private NextWordTapTicket340(String left,String right,long generation,
                                 int selectionStart,int selectionEnd) {
        before=left;after=right;session=generation;
        cursorStart=selectionStart;cursorEnd=selectionEnd;
    }

    static NextWordTapTicket340 capture(CharSequence before,
            CharSequence after,long session,int start,int end,
            boolean sensitive,boolean selected,boolean textAfterCaret) {
        if(sensitive || selected || textAfterCaret ||
                before==null || after==null || start<0 || end<0 ||
                start!=end || before.length()>BEFORE_LIMIT ||
                after.length()>1 || after.length()>0)
            return null;
        String left=before.toString();
        if(!left.isEmpty()) {
            char last=left.charAt(left.length()-1);
            if(!Character.isWhitespace(last))return null;
        }
        return new NextWordTapTicket340(left,after.toString(),session,start,end);
    }

    boolean current(CharSequence beforeNow,CharSequence afterNow,
            long currentSession,int start,int end,boolean selected,
            boolean sensitive) {
        return !selected && !sensitive &&
                currentSession==session &&
                cursorStart==start && cursorEnd==end &&
                beforeNow!=null && afterNow!=null &&
                Objects.equals(before,beforeNow.toString()) &&
                Objects.equals(after,afterNow.toString());
    }
}

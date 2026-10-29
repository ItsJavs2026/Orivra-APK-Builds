package com.chroomareborn.keyboard;

/**
 * A bounded cursor offset calculator for a SurroundingText excerpt.
 * Operates on UTF-16 and never lands between a surrogate pair. It also
 * keeps trailing combining marks, variation selectors and ZWJ-linked
 * emoji together rather than stopping inside those common graphemes.
 * Editors with more complex grapheme rules retain Android DPAD fallback.
 */
final class CursorMovementPolicy {
    private CursorMovementPolicy(){}

    private static boolean extension(int codepoint) {
        int type=Character.getType(codepoint);
        return type==Character.NON_SPACING_MARK ||
                type==Character.COMBINING_SPACING_MARK ||
                type==Character.ENCLOSING_MARK ||
                codepoint==0x200D || // zero-width joiner
                (codepoint>=0xFE00 && codepoint<=0xFE0F) ||
                (codepoint>=0x1F3FB && codepoint<=0x1F3FF);
    }

    static int move(CharSequence text,int position,int steps) {
        if(text==null || position<0 || position>text.length())
            throw new IllegalArgumentException("Invalid cursor snapshot");
        if(steps>32)steps=32;
        if(steps<-32)steps=-32;
        int index=position;
        if(steps>0) {
            for(int i=0;i<steps && index<text.length();i++) {
                int cp=Character.codePointAt(text,index);
                index+=Character.charCount(cp);
                // Consume marks and connected emoji sequences.
                while(index<text.length()) {
                    int next=Character.codePointAt(text,index);
                    if(extension(next) || (index>0 &&
                           Character.codePointBefore(text,index)==0x200D)) {
                        index+=Character.charCount(next);
                    } else break;
                }
            }
        } else if(steps<0) {
            for(int i=0;i>steps && index>0;i--) {
                int cp=Character.codePointBefore(text,index);
                index-=Character.charCount(cp);
                while(index>0) {
                    int current=Character.codePointAt(text,index);
                    int previous=Character.codePointBefore(text,index);
                    if(extension(current) || previous==0x200D) {
                        index-=Character.charCount(previous);
                    } else break;
                }
            }
        }
        return index;
    }
}

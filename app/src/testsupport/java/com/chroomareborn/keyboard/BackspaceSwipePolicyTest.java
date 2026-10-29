package com.chroomareborn.keyboard;

import java.util.Random;

public final class BackspaceSwipePolicyTest {
    private static void equal(int expected, int actual) {
        if (actual != expected) throw new AssertionError(
                "expected " + expected + " but got " + actual);
    }
    private static void verifyBoundary(String text, int words) {
        int deleted = BackspaceSwipePolicy.charsToDelete(text,words);
        if(deleted<0 || deleted>text.length())
            throw new AssertionError("invalid deletion length: "+deleted);
        int keep=text.length()-deleted;
        if(keep>0 && !Character.isWhitespace(text.charAt(keep-1)))
            throw new AssertionError("partial word deletion at "+keep);
        // If the cursor is next to an emoji, do not split a UTF-16 pair.
        if(keep>0 && keep<text.length() &&
                Character.isHighSurrogate(text.charAt(keep-1)) &&
                Character.isLowSurrogate(text.charAt(keep)))
            throw new AssertionError("surrogate pair split");
    }
    public static void main(String[] args) {
        equal(0, BackspaceSwipePolicy.wordsForSwipe(10,0,32,75));
        equal(0, BackspaceSwipePolicy.wordsForSwipe(-55,0,32,75));
        equal(0, BackspaceSwipePolicy.wordsForSwipe(55,80,32,75));
        equal(1, BackspaceSwipePolicy.wordsForSwipe(35,3,32,75));
        equal(2, BackspaceSwipePolicy.wordsForSwipe(107,0,32,75));
        equal(8, BackspaceSwipePolicy.wordsForSwipe(1200,0,32,75));
        equal(0, BackspaceSwipePolicy.wordsForSwipe(Float.NaN,0,32,75));
        equal(0, BackspaceSwipePolicy.wordsForSwipe(Float.POSITIVE_INFINITY,0,32,75));
        equal(0, BackspaceSwipePolicy.wordsForSwipe(100,Float.NaN,32,75));
        equal(0, BackspaceSwipePolicy.wordsForSwipe(100,0,Float.NaN,75));
        equal(0, BackspaceSwipePolicy.wordsForSwipe(100,0,32,Float.NaN));
        equal(0, BackspaceSwipePolicy.wordsForSwipe(100,0,0,75));
        equal(0, BackspaceSwipePolicy.wordsForSwipe(100,0,32,Float.POSITIVE_INFINITY));
        equal(0, BackspaceSwipePolicy.wordsForSwipe(100,0,32,-75));
        Object editorA=new Object();
        Object editorB=new Object();
        if(!BackspaceSwipePolicy.sameEditorSession(12,12,editorA,editorA))
            throw new AssertionError("same-session editor rejected");
        if(BackspaceSwipePolicy.sameEditorSession(12,13,editorA,editorA))
            throw new AssertionError("editor change across input sessions accepted");
        if(BackspaceSwipePolicy.sameEditorSession(12,12,editorA,editorB))
            throw new AssertionError("different editor accepted");
        if(BackspaceSwipePolicy.sameEditorSession(12,12,null,null))
            throw new AssertionError("null editor accepted");
        equal(0, BackspaceSwipePolicy.charsToDelete("hello world",0));
        equal(5, BackspaceSwipePolicy.charsToDelete("hello world",1));
        equal(11, BackspaceSwipePolicy.charsToDelete("hello world",2));
        // Reported regression: delete a single uninterrupted word such as
        // "heythere" (including a live composing word without spaces).
        equal(8, BackspaceSwipePolicy.charsToDelete("heythere",1));
        equal(8, BackspaceSwipePolicy.charsToDelete("heythere",2));
        equal(8, BackspaceSwipePolicy.charsToDelete("heythere",8));
        equal(3, BackspaceSwipePolicy.charsToDelete("hey",1));
        equal(1, BackspaceSwipePolicy.charsToDelete("a",1));
        equal(0, BackspaceSwipePolicy.charsToDelete("heythere",0));
        equal(5, BackspaceSwipePolicy.charsToDelete("there",1));
        equal(9, BackspaceSwipePolicy.charsToDelete("heythere!",1));
        equal(14, BackspaceSwipePolicy.charsToDelete("hey-there-again",1));

        equal(6, BackspaceSwipePolicy.charsToDelete("hello world ",1));
        equal(4, BackspaceSwipePolicy.charsToDelete("a b ",2));
        equal(4, BackspaceSwipePolicy.charsToDelete("test",7));
        equal(0, BackspaceSwipePolicy.charsToDelete("",2));
        equal(0, BackspaceSwipePolicy.charsToDelete(null,2));
        equal(2, BackspaceSwipePolicy.charsToDelete("a 😊",1));
        equal(4, BackspaceSwipePolicy.charsToDelete("a 😊",2));
        equal(4, BackspaceSwipePolicy.charsToDelete("👍🏽",1));
        equal(4, BackspaceSwipePolicy.charsToDelete("hi 👍🏽",1));
        equal(6, BackspaceSwipePolicy.charsToDelete("hello!",1));
        equal(3, BackspaceSwipePolicy.charsToDelete("a\tb\nc",2));
        equal(5, BackspaceSwipePolicy.charsToDelete("a\tb\nc",3));
        // A deterministic stress test covers varied whitespace, Unicode,
        // punctuation, repeated separators and multiple-word requests.
        Random random=new Random(290L);
        String[] tokens={"hello","x","world!","😊","👍🏽","中文","café","123","_",""};
        String[] gaps={" ","  ","\n","\t","\r\n","   "};
        for(int sample=0;sample<12000;sample++) {
            StringBuilder buffer=new StringBuilder();
            int chunks=1+random.nextInt(18);
            for(int i=0;i<chunks;i++) {
                if(i>0)buffer.append(gaps[random.nextInt(gaps.length)]);
                buffer.append(tokens[random.nextInt(tokens.length)]);
            }
            if(random.nextBoolean())buffer.append(gaps[random.nextInt(gaps.length)]);
            String text=buffer.toString();
            int last=0;
            for(int words=1;words<=8;words++) {
                verifyBoundary(text,words);
                int deleted=BackspaceSwipePolicy.charsToDelete(text,words);
                if(deleted<last)throw new AssertionError("more words deleted less");
                last=deleted;
            }
        }
        int lastWords=0;
        for(int left=0;left<1000;left++) {
            int current=BackspaceSwipePolicy.wordsForSwipe(left,0,32,75);
            if(current<lastWords || current>8)throw new AssertionError("non-monotonic gesture");
            lastWords=current;
        }
        System.out.println("BackspaceSwipePolicyTest passed: 12000 deterministic input cases");
    }
}

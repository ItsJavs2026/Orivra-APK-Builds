package com.chroomareborn.keyboard;
public final class PunctuationSpacingTest {
    private static void check(String before, String next, boolean sensitive, boolean middle,
            boolean selection, boolean after, boolean expected) {
        boolean actual = PunctuationSpacing.insertSpaceBeforeLetter(before,next,sensitive,middle,selection,after);
        if (actual != expected) throw new AssertionError("before="+before+" next="+next);
    }
    private static void trim(String before,String mark,boolean sensitive,
            boolean selection,int expected) {
        int actual=PunctuationSpacing.spacesBeforeClosingPunctuation(
                before,mark,sensitive,selection);
        if(actual!=expected)
            throw new AssertionError("trim before="+before+" mark="+mark+
                    " expected="+expected+" actual="+actual);
    }
    public static void main(String[] args) {
        check("Hello,","h",false,false,false,false,true);
        check("Hello, ","h",false,false,false,false,false);
        check("Hello.","h",false,false,false,false,false);
        check("Hi!","N",false,false,false,false,true);
        check("Really?","y",false,false,false,false,true);
        check("Time:","n",false,false,false,false,true);
        check("Hi;","t",false,false,false,false,true);
        check("Hello,","1",false,false,false,false,false);
        check("Hello,","h",true,false,false,false,false);
        check("Hello,","h",false,true,false,false,false);
        check("Hello,","h",false,false,true,false,false);
        check("Hello,","h",false,false,false,true,false);
        check("","h",false,false,false,false,false);
        // Sentence punctuation must consume a trailing separator, including
        // auto-space following an accepted correction or prediction.
        trim("Hello ",".",false,false,1);
        trim("Hello ",",",false,false,1);
        trim("Hello ","!",false,false,1);
        trim("Hello ","?",false,false,1);
        trim("Hello ",";",false,false,1);
        trim("Hello ",":",false,false,1);
        trim("Hello   ",".",false,false,3);
        trim("Hello  ",")",false,false,2);
        trim("Hello\u00a0",".",false,false,1);
        trim("Hello",".",false,false,0);
        trim("Hello world ",",",false,false,1);
        trim("Hello\n  ",".",false,false,0);
        trim("   ",".",false,false,0);
        trim("Hello ","(",false,false,0);
        trim("Hello ","-",false,false,0);
        trim("Hello ",".",true,false,0);
        trim("Hello ",".",false,true,0);
        trim("Hello ","!!",false,false,0);
        if(!PunctuationSpacing.isClosingPunctuation(".") ||
                !PunctuationSpacing.isClosingPunctuation("!") ||
                PunctuationSpacing.isClosingPunctuation("("))
            throw new AssertionError("Wrong punctuation classification");
        for(String mark:new String[]{".","!","? ",","}) {
            String symbol=mark.trim();
            if(!PunctuationSpacing.appendSpaceAfterPunctuation(symbol,false,false,false))
                throw new AssertionError("Missing post-punctuation space: "+symbol);
        }
        for(String mark:new String[]{")","]","}","%"}) {
            if(PunctuationSpacing.appendSpaceAfterPunctuation(mark,false,false,false))
                throw new AssertionError("Unexpected post-punctuation space: "+mark);
        }
        if(PunctuationSpacing.appendSpaceAfterPunctuation(".",true,false,false) ||
                PunctuationSpacing.appendSpaceAfterPunctuation(".",false,true,false) ||
                PunctuationSpacing.appendSpaceAfterPunctuation(".",false,false,true))
            throw new AssertionError("Edited or private text must not auto-space");
        if(!PunctuationSpacing.startsNextSentence(".") ||
                !PunctuationSpacing.startsNextSentence("!") ||
                !PunctuationSpacing.startsNextSentence("?") ||
                PunctuationSpacing.startsNextSentence(","))
            throw new AssertionError("Sentence boundary mismatch");
        System.out.println("Punctuation spacing checks passed: 13 existing + 19 attachment + 12 spacing/boundary");
    }
}

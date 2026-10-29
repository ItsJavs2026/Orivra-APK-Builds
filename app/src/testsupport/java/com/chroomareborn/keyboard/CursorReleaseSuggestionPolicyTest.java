package com.chroomareborn.keyboard;

/** Boundary and safety cases for post-spacebar-release suggestions. */
public final class CursorReleaseSuggestionPolicyTest {
    private static int checks;
    private static void expect(boolean value,String description) {
        checks++;
        if(!value)throw new AssertionError(description);
    }
    private static void mode(String before,String after,
                             CursorReleaseSuggestionPolicy.Mode mode,String word) {
        CursorReleaseSuggestionPolicy.Decision result=
                CursorReleaseSuggestionPolicy.classify(before,after,false,false);
        expect(result.mode==mode && result.word.equals(word),
                "Wrong release suggestions for caret position");
    }

    public static void main(String[] args) {
        CursorReleaseSuggestionPolicy.Mode correction=
                CursorReleaseSuggestionPolicy.Mode.CORRECT_WORD;
        CursorReleaseSuggestionPolicy.Mode next=
                CursorReleaseSuggestionPolicy.Mode.NEXT_WORD;
        CursorReleaseSuggestionPolicy.Mode clear=
                CursorReleaseSuggestionPolicy.Mode.CLEAR;
        mode("i typed a wro","ng sentence",correction,"wrong");
        mode("i typed a ","wrong sentence",correction,"wrong");
        mode("i typed a wrong"," sentence",correction,"wrong");
        mode("this is wrng","",correction,"wrng");
        mode("","hello world",correction,"hello");
        mode("hello ","world",correction,"world");
        mode("hello world ","",next,"");
        mode("hello.","",next,"");
        mode("hello\n","",next,"");
        mode("","",next,"");
        mode("hello "," more",clear,"");
        mode("hello."," more",clear,"");
        mode("hello,\t","\nworld",clear,"");
        mode("caf","é please",correction,"café");
        mode("one  ","two",correction,"two");
        mode("one  "," two",clear,"");
        CursorReleaseSuggestionPolicy.Decision secret=
                CursorReleaseSuggestionPolicy.classify("pass","word",false,true);
        expect(secret.mode==clear && secret.word.isEmpty(),
                "Password field must not request correction");
        CursorReleaseSuggestionPolicy.Decision selection=
                CursorReleaseSuggestionPolicy.classify("wr","ong",true,false);
        expect(selection.mode==clear && selection.word.isEmpty(),
                "A selected range must not be overwritten");
        expect(CursorReleaseSuggestionPolicy.classify(null,"a",false,false).mode==clear,
                "Null before must be safe");
        expect(CursorReleaseSuggestionPolicy.classify("a",null,false,false).mode==clear,
                "Null after must be safe");
        String longWord="aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        expect(CursorReleaseSuggestionPolicy.classify(longWord,"",false,false).mode==clear,
                "Very long token must not start correction");
        System.out.println("CURSOR_RELEASE_SUGGESTIONS checks="+checks+" passed");
    }
}

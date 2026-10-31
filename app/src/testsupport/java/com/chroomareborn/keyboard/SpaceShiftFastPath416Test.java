package com.chroomareborn.keyboard;

/** Pure-Java semantic guard; no Android runtime or real typed content. */
public final class SpaceShiftFastPath416Test {
    private static int checks;
    private static void expect(boolean yes, String message) {
        checks++;
        if(!yes) throw new AssertionError(message);
    }
    private static boolean fast(String word) {
        return SpaceShiftFastPath416.canClearShift(word,
                true,false,false,false,false,false,false,false);
    }
    public static void main(String[] args) {
        expect(fast("hello"),"ordinary word");
        expect(fast("Hola"),"uppercase word");
        expect(fast("mañana"),"Spanish letter");
        expect(fast("don't"),"apostrophe inside word");
        expect(fast("end?maybe"),"letter at end");
        expect(!fast(""),"empty composition");
        expect(!fast(null),"unknown composition");
        expect(!fast("hi."),"punctuation suffix");
        expect(!fast("hi!"),"exclamation suffix");
        expect(!fast("hi?"),"question suffix");
        expect(!fast("hi "),"space suffix");
        expect(!fast("12"),"numeric suffix");
        expect(!fast("hello\n"),"newline suffix");
        boolean base = fast("hello");
        expect(base,"base eligible");
        expect(!SpaceShiftFastPath416.canClearShift(
                "hello",false,false,false,false,false,false,false,false),
                "stale editor or session");
        expect(!SpaceShiftFastPath416.canClearShift(
                "hello",true,true,false,false,false,false,false,false),
                "prior middle edit");
        expect(!SpaceShiftFastPath416.canClearShift(
                "hello",true,false,true,false,false,false,false,false),
                "text after cursor");
        expect(!SpaceShiftFastPath416.canClearShift(
                "hello",true,false,false,true,false,false,false,false),
                "selected text");
        expect(!SpaceShiftFastPath416.canClearShift(
                "hello",true,false,false,false,true,false,false,false),
                "private field");
        expect(!SpaceShiftFastPath416.canClearShift(
                "hello",true,false,false,false,false,true,false,false),
                "numeric field");
        expect(!SpaceShiftFastPath416.canClearShift(
                "hello",true,false,false,false,false,false,true,false),
                "symbols");
        expect(!SpaceShiftFastPath416.canClearShift(
                "hello",true,false,false,false,false,false,false,true),
                "caps lock");
        // Exact old pure policy for every allowed normal word + literal
        // trailing Space, including preceding sentence terminators.
        for(String before : new String[]{"", "Next? ", "Before. ", "\n",
                "Hola!\n", "Text and more "}) {
            for(String word : new String[]{"hello","mañana","don't","World"}) {
                checks++;
                if(!fast(word) ||
                        SentenceCapitalization.shouldCapitalize(before+word+" "))
                    throw new AssertionError("Fast path did not match policy");
            }
        }
        System.out.println("SpaceShiftFastPath416Test PASS ("+checks+" checks)");
    }
}

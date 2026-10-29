package com.chroomareborn.keyboard;

public final class CursorFallbackBoundaryPolicyTest {
    private static int checks;
    private static void check(boolean result,String label) {
        checks++;
        if(!result)throw new AssertionError(label);
    }
    private static void expect(int requested,CharSequence adjacent,int allowed,String label) {
        check(CursorFallbackBoundaryPolicy.allowedArrowEvents(requested,adjacent)
                ==allowed,label);
    }
    public static void main(String[] args) {
        expect(1,"",0,"no right arrow after final character");
        expect(32,"",0,"fast right swipe at end never escapes editor");
        expect(-32,"",0,"fast left swipe at beginning stays within editor");
        expect(8,null,0,"no snapshot must not send focus-changing keys");
        expect(-8,null,0,"unavailable left snapshot stays safe");
        expect(0,"abc",0,"zero movement");
        expect(1,"a",1,"right movement with known next letter");
        expect(2,"a",1,"one remaining character allows one event");
        expect(20,"abc",2,"bounded right movement");
        expect(-20,"xyz",2,"bounded left movement");
        expect(Integer.MIN_VALUE,"abcdef",2,"integer overflow protected");
        expect(Integer.MAX_VALUE,"abcdef",2,"maximum swipe bounded");
        expect(2,"\uD83D\uDE80",1,"one emoji is one codepoint");
        expect(2,"\uD83D\uDE80x",2,"emoji plus letter supports two");
        expect(2,"\u00E9",1,"accented letter supports one");
        expect(1,"hello world",1,"ordinary text navigation unchanged");
        System.out.println("CURSOR_FALLBACK_BOUNDARY checks="+checks+" passed");
    }
}

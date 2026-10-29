package com.chroomareborn.keyboard;

/** Deterministic gesture and UTF-16 cursor regression suite. */
public final class SpacebarCursorGestureTest {
    private static int tests;
    private static void expect(boolean ok,String name) {
        tests++;
        if(!ok)throw new AssertionError(name);
    }

    private static SpacebarCursorGesture gesture() {
        return new SpacebarCursorGesture(12f,16f,28f);
    }

    public static void main(String[] args) {
        SpacebarCursorGesture g=gesture();
        g.start(100,20);
        expect(g.isTouching(),"space touch starts");
        expect(g.move(105,20)==0 && !g.isNavigating(),"small drift is tap");
        expect(g.finish(),"short tap inserts one space");
        expect(!g.finish(),"release cannot insert twice");

        g.start(100,20);
        expect(g.hold() && g.isHeld(),"hold arms hint");
        expect(g.finish(),"long hold without drag still types a single space");

        g.start(100,20);
        expect(g.move(120,21)==1 && g.isNavigating(),"swipe right advances one");
        expect(g.move(151,21)==2,"fractional displacement accumulates");
        expect(g.move(131,21)==-1,"reverse can move left");
        expect(!g.finish(),"cursor swipe never inserts a space");

        g.start(100,20);
        expect(g.move(70,20)==-1,"swipe left");
        expect(g.move(38,20)==-2,"accelerated travel across key");
        expect(!g.finish(),"left swipe consumes tap");

        g.start(100,20);
        expect(g.move(100,59)==0 && g.isCancelled(),"vertical drag cancelled");
        expect(!g.finish(),"cancelled vertical gesture never inserts a space");

        g.start(100,20);
        expect(g.move(110,42)==0 && !g.isNavigating(),"diagonal drift ignored");
        expect(g.finish(),"diagonal subthreshold remains tap");

        g.start(100,20);
        g.cancel();
        expect(!g.finish(),"ACTION_CANCEL never inserts space");

        g.start(100,20);
        expect(g.move(112,20)==0 && g.isNavigating(),"activation alone");
        expect(!g.finish(),"activated swipe, even below 1 step, types no space");

        try {
            new SpacebarCursorGesture(0,16,28);
            throw new AssertionError("invalid step accepted");
        } catch(IllegalArgumentException expected) {tests++;}
        String ascii="some quick sentence";
        expect(CursorMovementPolicy.move(ascii,ascii.length(),-5)==ascii.length()-5,
                "backward ASCII offset");
        expect(CursorMovementPolicy.move(ascii,0,5)==5,"forward ASCII offset");
        expect(CursorMovementPolicy.move(ascii,0,-99)==0,"left boundary clamped");
        expect(CursorMovementPolicy.move(ascii,ascii.length(),99)==ascii.length(),
                "right boundary clamped");

        String emoji="a\uD83D\uDE80b";
        expect(CursorMovementPolicy.move(emoji,1,1)==3,"skip surrogate emoji");
        expect(CursorMovementPolicy.move(emoji,3,-1)==1,"reverse surrogate emoji");
        String accent="cafe\u0301 next";
        expect(CursorMovementPolicy.move(accent,3,1)==5,"combining accent together");
        expect(CursorMovementPolicy.move(accent,5,-1)==3,"reverse combining cluster");
        String joined="a\uD83D\uDC69\u200D\uD83D\uDCBBz";
        expect(CursorMovementPolicy.move(joined,1,1)==6,"ZWJ emoji as cluster");
        expect(CursorMovementPolicy.move(joined,6,-1)==1,"reverse joined emoji");
        expect(CursorMovementPolicy.move("hello",3,0)==3,"no movement");
        try {
            CursorMovementPolicy.move("hi",9,1);
            throw new AssertionError("invalid cursor position accepted");
        } catch(IllegalArgumentException expected) {tests++;}
        System.out.println("SPACEBAR_CURSOR_GESTURE checks="+tests+" passed");
    }
}

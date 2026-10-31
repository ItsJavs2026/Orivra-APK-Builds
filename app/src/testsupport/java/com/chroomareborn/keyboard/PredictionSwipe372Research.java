package com.chroomareborn.keyboard;

import java.util.List;
import java.util.Random;

/** Deterministic touch-path policy regressions; no editor or private text. */
public final class PredictionSwipe372Research {
    private static int checks=0;
    private static void eq(Object expected,Object found,String label) {
        checks++;
        if(!expected.equals(found))
            throw new AssertionError(label+" expected "+expected+" got "+found);
    }
    private static void valid(boolean truth,String label) {
        checks++;
        if(!truth)throw new AssertionError(label);
    }
    public static void main(String[] args) {
        List<String> strip=List.of("tekk","tell","test");
        List<String> rise=List.of("tell me","tell us");
        PredictionSwipe372.Direction L=PredictionSwipe372.Direction.LEFT;
        PredictionSwipe372.Direction R=PredictionSwipe372.Direction.RIGHT;
        PredictionSwipe372.Direction U=PredictionSwipe372.Direction.UP;
        PredictionSwipe372.Direction N=PredictionSwipe372.Direction.NONE;
        eq(L,PredictionSwipe372.direction(-55,3,12,false),"left flick");
        eq(R,PredictionSwipe372.direction(47,-4,12,false),"right flick");
        eq(U,PredictionSwipe372.direction(3,-65,12,true),"up to Rise");
        eq(N,PredictionSwipe372.direction(3,-65,12,false),"no Rise: up no-op");
        eq(N,PredictionSwipe372.direction(6,4,12,true),"tap not swipe");
        eq(N,PredictionSwipe372.direction(Float.NaN,45,12,true),"bad pointer");
        eq("tekk",PredictionSwipe372.choose(L,1,strip,rise,false),"left neighbor");
        eq("test",PredictionSwipe372.choose(R,1,strip,rise,false),"right neighbor");
        eq("tell me",PredictionSwipe372.choose(U,1,strip,rise,false),"Rise first");
        eq("tell me",PredictionSwipe372.choose(L,1,strip,rise,true),"Rise left");
        eq("tell us",PredictionSwipe372.choose(R,0,strip,rise,true),"Rise right");
        valid(PredictionSwipe372.choose(L,0,strip,rise,false)==null,"no wrap left");
        valid(PredictionSwipe372.choose(R,2,strip,rise,false)==null,"no wrap right");
        valid(PredictionSwipe372.choose(U,0,strip,rise,true)==null,"up from Rise no-op");
        eq("test",PredictionSwipe372.choose(R,0,
                List.of("tekk","","test"),rise,false),"skip empty slot");
        valid(PredictionSwipe372.choose(R,0,
                List.of("tekk","",""),rise,false)==null,"no invisible choices");
        valid(!PredictionSwipe372.safeRelease("tekk","tekk",
                true,true,true,true,true,false,false,false,true),
                "private editor blocked");
        valid(!PredictionSwipe372.safeRelease("tekk","tell",
                true,true,true,false,true,false,false,false,true),
                "editor text changed");
        valid(!PredictionSwipe372.safeRelease("tekk","tekk",
                false,true,true,false,true,false,false,false,true),
                "different editor");
        valid(!PredictionSwipe372.safeRelease("tekk","tekk",
                true,false,true,false,true,false,false,false,true),
                "different session");
        valid(!PredictionSwipe372.safeRelease("tekk","tekk",
                true,true,false,false,true,false,false,false,true),
                "moved selection");
        valid(!PredictionSwipe372.safeRelease("tekk","tekk",
                true,true,true,false,false,false,false,false,true),
                "suggestions off");
        valid(!PredictionSwipe372.safeRelease("tekk","tekk",
                true,true,true,false,true,true,false,false,true),
                "numeric field");
        valid(!PredictionSwipe372.safeRelease("tekk","tekk",
                true,true,true,false,true,false,true,false,true),
                "selected text");
        valid(!PredictionSwipe372.safeRelease("tekk","tekk",
                true,true,true,false,true,false,false,true,true),
                "symbols");
        valid(!PredictionSwipe372.safeRelease("tekk","tekk",
                true,true,true,false,true,false,false,false,false),
                "multi touch");
        valid(PredictionSwipe372.safeRelease("tekk","tekk",
                true,true,true,false,true,false,false,false,true),
                "verified gesture accepted");
        Random r=new Random(37220261030L);
        for(int i=0;i<80_000;i++) {
            float dx=r.nextInt(350)-175,dy=r.nextInt(350)-175;
            boolean up=r.nextBoolean();
            PredictionSwipe372.Direction d=
                    PredictionSwipe372.direction(dx,dy,12,up);
            if(d==U)valid(up && dy<0,"up requires Rise "+i);
            else if(d==L)valid(dx<0,"left is left "+i);
            else if(d==R)valid(dx>0,"right is right "+i);
            else valid(true,"neutral "+i);
        }
        System.out.println("PASS Research 372 swipe: "+checks+
                " direction, frozen-chip and privacy assertions");
    }
}

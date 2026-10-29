package com.chroomareborn.keyboard;

import java.util.Random;

/** Exact, synthetic editor snapshots; no actual user content. */
public final class CursorEditSnapshotGuardTest {
    private static int assertions=0;
    private static void ok(boolean p,String message){
        assertions++;if(!p)throw new AssertionError(message);
    }
    private static CursorEditSnapshotGuard.Ticket capture(String w,String b,String a,
                                                            long rev,int cursor) {
        return CursorEditSnapshotGuard.capture(w,b,a,rev,cursor,cursor,
                                                 false,false,false);
    }
    private static boolean current(CursorEditSnapshotGuard.Ticket t,String b,String a,
                                    long rev,int cursor) {
        return CursorEditSnapshotGuard.current(t,b,a,rev,cursor,cursor,
                                                false,false,false);
    }
    public static void main(String[] args) {
        String before="I typed the cor", after="ect in this field";
        var t=capture("corect",before,after,7,15);
        ok(t!=null,"capture midword");
        ok(current(t,before,after,7,15),"same snapshot accepted");
        ok(!current(t,before,after,8,15),"revision change rejected");
        ok(!current(t,before,after,7,31),"other cursor position rejected");
        ok(!current(t,"we typed the cor",after,7,15),"left text changed rejected");
        ok(!current(t,before,"ect in different field",7,15),"right text changed rejected");
        ok(!current(t,before,"eet in this field",7,15),"wrong current word rejected");
        ok(!current(t,null,after,7,15),"null before rejected");
        ok(!current(t,before,null,7,15),"null after rejected");
        ok(!CursorEditSnapshotGuard.current(t,before,after,7,15,16,
             true,false,false),"selection rejected");
        ok(!CursorEditSnapshotGuard.current(t,before,after,7,15,15,
             false,true,false),"composition rejected");
        ok(!CursorEditSnapshotGuard.current(t,before,after,7,15,15,
             false,false,true),"sensitive editor rejected");
        ok(!CursorEditSnapshotGuard.current(null,before,after,7,15,15,
             false,false,false),"null ticket rejected");
        ok(CursorEditSnapshotGuard.capture("corect",before,after,7,15,16,
             true,false,false)==null,"selected capture blocked");
        ok(CursorEditSnapshotGuard.capture("corect",before,after,7,15,15,
             false,true,false)==null,"composing capture blocked");
        ok(CursorEditSnapshotGuard.capture("corect",before,after,7,15,15,
             false,false,true)==null,"sensitive capture blocked");
        ok(capture("different",before,after,7,15)==null,"wrong expected word blocked");
        ok(capture("corect","x".repeat(64),after,7,15)==null,"64-char cut word blocked");
        ok(capture("corect",before,"x".repeat(64),7,15)==null,"64-char after cut blocked");
        ok(capture("corect",before+"x".repeat(64),after,7,15)==null,"long before blocked");
        ok(capture("corect",before,after+"x".repeat(64),7,15)==null,"long after blocked");
        ok(capture("CORECT","Some words COR","ECT further",7,15)!=null,
            "original case retained");
        ok(capture("corect","Some words Cor","ect further",7,15)==null,
            "case mismatch rejected");
        // Same word appears multiple times; a different selection or context
        // must never pass as a previously verified tap.
        var repeated=capture("corect","corect was the cor","ect again",7,23);
        ok(repeated!=null,"repeated word captured");
        ok(!current(repeated,"corect was the cor","ect again",7,7),
            "old ticket cannot target duplicate word elsewhere");
        ok(!current(repeated,"the cor","ect again",7,23),
            "matching word but different bounded context fails");
        Random rng=new Random(283);
        for(int k=0;k<15000;k++){
            String word=k%2==0?"corect":"keyboadr";
            int split=rng.nextInt(word.length()+1);
            String b="Please check this sample "+word.substring(0,split);
            String a=word.substring(split)+" with more words";
            long rev=k+100L;
            var request=capture(word,b,a,rev,split+100);
            ok(request!=null,"random valid ticket");
            ok(current(request,b,a,rev,split+100),"random current ticket");
            ok(!current(request,b,a,rev+1,split+100),"random stale epoch");
            ok(!current(request,b,a,rev,split+101),"random displaced cursor");
            ok(!current(request,b,a+"!",rev,split+100),"random edited text");
        }
        System.out.println("BUILD283_CURSOR_EDIT_SNAPSHOT_GUARD checks="+assertions+" passed");
    }
}

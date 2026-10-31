package com.chroomareborn.keyboard;

import java.util.Random;

/** Deterministic no-Android policy and lifecycle replay; no user text. */
public final class TypingRefreshGate401Research {
    private static long checks;
    private static void ok(boolean value,String message){
        checks++;
        if(!value)throw new AssertionError(message);
    }
    public static void main(String[] args) {
        Random random=new Random(40120261031L);
        Object editorA=new Object(),editorB=new Object();
        for(int run=0;run<60_000;run++){
            TypingRefreshGate401 gate=new TypingRefreshGate401();
            long session=run;
            Object editor=random.nextBoolean()?editorA:editorB;
            int taps=2+random.nextInt(24);
            int scheduled=0;
            for(int k=0;k<taps;k++) {
                if(gate.arm(session,editor))scheduled++;
                ok(gate.isPendingFor(session,editor),"same editor must keep gate");
            }
            ok(scheduled==1,"only one scheduled UI render in burst");
            ok(gate.consume(session,editor),"same field should refresh");
            ok(!gate.isPending(),"must consume and release editor reference");
            ok(!gate.consume(session,editor),"stale timer must not refresh");
        }
        for(int i=0;i<60_000;i++){
            TypingRefreshGate401 gate=new TypingRefreshGate401();
            ok(gate.arm(i,editorA),"arm");
            ok(!gate.consume(i+1,editorA),"different session must never render");
            ok(!gate.isPending(),"no dangling editor");
            ok(gate.arm(i+2,editorB),"new editor can arm safely");
            gate.cancel();
            ok(!gate.consume(i+2,editorB),"finish must cancel");
            ok(!gate.arm(i+3,null),"null editor rejected");
            ok(!gate.isPending(),"null editor is not held");
        }
        for(int i=0;i<100_000;i++) {
            boolean sensitive=(i&1)!=0,allowed=(i&2)!=0,numeric=(i&4)!=0,
                    symbols=(i&8)!=0,selection=(i&16)!=0;
            boolean expected=!sensitive&&allowed&&!numeric&&!symbols&&!selection;
            ok(expected==TypingRefreshGate401.safeToCoalesce(
                    sensitive,allowed,numeric,symbols,selection),
                    "editor privacy/selection gate");
        }
        ok(TypingRefreshGate401.WAIT_MS>=32L &&
                TypingRefreshGate401.WAIT_MS<=120L,
                "bounded window not a long pause");
        System.out.println("PASS TypingRefresh401 "+checks+
                " editor/session/privacy and single-pending-window assertions");
    }
}

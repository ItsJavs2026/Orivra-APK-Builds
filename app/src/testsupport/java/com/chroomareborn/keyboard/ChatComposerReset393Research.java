package com.chroomareborn.keyboard;
import java.util.Random;
/** Isolated reset policy stress: no user input or Android dependencies. */
public final class ChatComposerReset393Research {
    private static long checks;
    private static void ok(boolean cond,String label){
        checks++; if(!cond)throw new AssertionError(label);
    }
    public static void main(String[] args){
        Random random=new Random(39320261031L);
        for(int i=0;i<150000;i++){
            boolean atZero=random.nextBoolean();
            boolean emptyBefore=random.nextBoolean();
            boolean emptyAfter=random.nextBoolean();
            boolean draftStale=random.nextBoolean();
            boolean staleVisual=random.nextBoolean();
            boolean middle=random.nextBoolean();
            boolean idleCurrent=random.nextBoolean();
            int count=random.nextInt(4);
            boolean privacy=random.nextBoolean();
            boolean expected=atZero && emptyBefore && emptyAfter && !privacy &&
                    (draftStale || staleVisual || middle || count==0 || !idleCurrent);
            ok(ChatComposerReset393.shouldRecover(
                    atZero?0:1,atZero?0:1,
                    emptyBefore?"":"x",emptyAfter?"":"y",
                    draftStale?"test":"",staleVisual?"test":"",middle,
                    count,idleCurrent,privacy)==expected,
                    "only exact verified empty editor resets");
            ok(!ChatComposerReset393.shouldRecover(0,0,null,"",
                    "old","old",true,0,false,false),
                    "missing editor always fails closed");
        }
        ok(ChatComposerReset393.shouldRecover(0,0,"","","x","",false,
                3,true,false),"stale composition recognized");
        ok(ChatComposerReset393.shouldRecover(0,0,"","","","",false,
                3,false,false),"stale idle ticket recognized");
        ok(!ChatComposerReset393.shouldRecover(0,0,"","","","",false,
                3,true,false),"current idle ticket avoids refresh loop");
        System.out.println("PASS ChatComposerReset393 "+checks+
                " assertions over 150000 randomized editor states");
    }
}

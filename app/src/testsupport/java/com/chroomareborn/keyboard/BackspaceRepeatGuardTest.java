package com.chroomareborn.keyboard;

import java.util.Random;

public final class BackspaceRepeatGuardTest {
    private static long assertions;
    private static void check(boolean value,String label) {
        assertions++;
        if(!value)throw new AssertionError(label);
    }
    public static void main(String[] args) {
        Object editorA=new Object(), editorB=new Object();
        // Every combination of touch-active, repeat-armed, cancel, swipe,
        // motion, editor identity and input-generation changes.
        for(int mask=0;mask<128;mask++) {
            boolean finger=(mask&1)!=0;
            boolean armed=(mask&2)!=0;
            boolean cancelled=(mask&4)!=0;
            boolean swipe=(mask&8)!=0;
            boolean moved=(mask&16)!=0;
            boolean sameEditor=(mask&32)!=0;
            boolean sameSession=(mask&64)!=0;
            Object live=sameEditor?editorA:editorB;
            long generation=sameSession?9:10;
            boolean expectedStart=finger&&armed&&!cancelled&&!swipe&&!moved
                    &&sameEditor&&sameSession;
            check(BackspaceRepeatGuard.startAllowed(finger,armed,cancelled,
                            swipe,moved,9,generation,editorA,live)==expectedStart,
                    "unexpected hold start mask "+mask);
            boolean expectedRepeat=finger&&armed&&sameEditor&&sameSession;
            check(BackspaceRepeatGuard.repeatAllowed(
                            finger,armed,9,generation,editorA,live)==expectedRepeat,
                    "unexpected repeat mask "+mask);
            check(BackspaceRepeatGuard.mayRebuildKeys(finger,armed)==
                            (!finger&&!armed),
                    "touch target rebuild risk mask "+mask);
        }
        check(!BackspaceRepeatGuard.startAllowed(true,true,false,false,false,
                9,9,null,null),"null editor must not start");
        check(!BackspaceRepeatGuard.repeatAllowed(true,true,9,9,null,null),
                "null editor must not receive repeated deletions");
        check(!BackspaceRepeatGuard.mayRebuildKeys(true,false),
                "cancelled hold remains physically touched until ACTION_UP");
        check(BackspaceRepeatGuard.mayRebuildKeys(false,false),
                "released key must be allowed to refresh");
        check(!BackspaceRepeatGuard.startAllowed(true,true,false,false,true,
                9,9,editorA,editorA),"slow swipe must defeat hold");
        check(!BackspaceRepeatGuard.repeatAllowed(true,true,9,9,editorA,editorB),
                "editor focus changed during repeat");

        Random random=new Random(292178L);
        for(int sample=0;sample<100000;sample++) {
            boolean touch=true,armed=true;
            long session=random.nextInt(20);
            Object captured=new Object();
            Object current=captured;
            boolean cancelled=false,moved=false,swiping=false;
            boolean shouldRemainActive=true;
            int steps=1+random.nextInt(18);
            for(int i=0;i<steps;i++) {
                int event=random.nextInt(8);
                switch(event) {
                    case 0: // minor twitch
                        break;
                    case 1: // slide past tap slop: stop repeat but keep touch
                        moved=true;
                        armed=false;
                        break;
                    case 2: // enter whole-word swipe
                        swiping=true;
                        moved=true;
                        armed=false;
                        break;
                    case 3: // multiple pointers cancel but finger remains
                        cancelled=true;
                        armed=false;
                        break;
                    case 4: // different editor / focus
                        current=new Object();
                        break;
                    case 5: // session generation advanced
                        session++;
                        break;
                    case 6: // release
                    case 7: // motion cancel
                        touch=false;
                        armed=false;
                        shouldRemainActive=false;
                        break;
                    default:break;
                }
                boolean start=BackspaceRepeatGuard.startAllowed(
                        touch,armed,cancelled,swiping,moved,7,session,
                        captured,current);
                if(start)check(touch&&armed&&!cancelled&&!swiping&&!moved
                        &&session==7&&current==captured,"unsafe hold started");
                boolean repeat=BackspaceRepeatGuard.repeatAllowed(
                        touch,armed,7,session,captured,current);
                if(repeat)check(touch&&armed&&session==7&&current==captured,
                        "unsafe repeat after input change");
                if(shouldRemainActive)
                    check(!BackspaceRepeatGuard.mayRebuildKeys(touch,armed),
                            "view tree rebuilt while finger still held");
                else
                    check(BackspaceRepeatGuard.mayRebuildKeys(touch,armed),
                            "released view remained blocked");
                // A terminal release/cancel ends one physical gesture.
                if(!touch)break;
            }
        }
        System.out.println("BackspaceRepeatGuardTest passed: 128 boolean "+
                "combinations and 100000 seeded touch-event sequences, "+
                assertions+" checks");
    }
}

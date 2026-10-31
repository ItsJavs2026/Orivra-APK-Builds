package com.chroomareborn.keyboard;

import java.util.ArrayDeque;

/**
 * Research-only sequence barrier for mixed alphabet/Space two-thumb presses.
 *
 * Current Candidate 378 guarantees alphabet-to-alphabet down ordering, but
 * Space commits independently at ACTION_UP; a space released first can
 * overtake an earlier letter. This class models a proposed shared barrier.
 *
 * Never reads input text, touches an InputConnection, changes key layout,
 * persists input metadata or installs itself in the IME. A caller remains
 * responsible for checking editor/session identity before executing actions.
 */
final class MixedThumbOrder380 {
    private static final int MAX_PENDING=24;
    private static final class Press {
        final long id;
        boolean resolved;
        Runnable commit;
        Press(long id){this.id=id;}
    }
    private final ArrayDeque<Press> pending=new ArrayDeque<>();
    private long nextId=1;
    private boolean flushing;
    private int open;

    long down(){
        if(pending.size()>=MAX_PENDING)cancelAll();
        long id=nextId++;
        pending.addLast(new Press(id));
        open++;
        return id;
    }

    void up(long token,Runnable acceptedAction){
        for(Press press:pending){
            if(press.id==token){
                if(press.resolved)return;
                press.resolved=true;
                press.commit=acceptedAction;
                open--;
                flush();
                return;
            }
        }
    }

    void cancel(long token){up(token,null);}
    void cancelAll(){pending.clear();open=0;}
    boolean hasOpenPresses(){return open>0;}

    private void flush(){
        if(flushing)return;
        flushing=true;
        try{
            while(!pending.isEmpty() && pending.peekFirst().resolved){
                Press first=pending.removeFirst();
                Runnable action=first.commit;
                first.commit=null;
                if(action!=null)action.run();
            }
        }finally{flushing=false;}
    }
}

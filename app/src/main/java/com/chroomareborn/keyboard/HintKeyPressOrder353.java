package com.chroomareborn.keyboard;

import java.util.ArrayDeque;

/**
 * Main-thread-only, short-lived alpha-key touch order.
 *
 * Alphabet keys may finish out of order when two thumbs overlap. Commit
 * actions in ACTION_DOWN order, but do not commit a letter until its short
 * tap is confirmed on release (so long-press secondary symbols still work).
 * Cancels release the queue; a dropped pointer never traps subsequent keys.
 *
 * No typed text, telemetry or touch locations are stored. Each action is
 * one transient Runnable owned by a live IME editor/gesture.
 */
final class HintKeyPressOrder353 {
    private static final int MAX_PENDING=12;
    private static final class Pending {
        final long token;
        Runnable completion;
        boolean resolved;
        Pending(long token){this.token=token;}
    }
    private final ArrayDeque<Pending> queue=new ArrayDeque<>();
    private long nextToken=1L;
    private int open;
    private boolean flushing;

    long begin() {
        if(queue.size()>=MAX_PENDING) {
            // Fail closed on a malformed touch stream. Do not commit stale
            // letters; a new press starts a clean bounded transaction.
            cancelAll();
        }
        long token=nextToken++;
        queue.addLast(new Pending(token));
        open++;
        return token;
    }

    boolean hasOpenPresses(){return open>0;}

    void complete(long token,Runnable action){
        Pending chosen=null;
        for(Pending pending:queue)
            if(pending.token==token){chosen=pending;break;}
        if(chosen==null || chosen.resolved)return;
        chosen.resolved=true;
        chosen.completion=action;
        open--;
        flush();
    }

    void cancel(long token){complete(token,null);}

    void cancelAll(){
        queue.clear();
        open=0;
    }

    private void flush(){
        if(flushing)return;
        flushing=true;
        try{
            while(!queue.isEmpty() && queue.peekFirst().resolved){
                Pending first=queue.removeFirst();
                Runnable action=first.completion;
                first.completion=null;
                if(action!=null)action.run();
            }
        }finally{flushing=false;}
    }
}

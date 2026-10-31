package com.chroomareborn.keyboard;

import java.util.ArrayDeque;
import java.util.function.BooleanSupplier;

/**
 * Research-only bounded bridge between accepted multi-thumb presses and
 * deferred editor mutations. Never wired into RebornImeService.
 *
 * Main-thread only. Poster MUST preserve FIFO order, e.g. a single Android
 * main Looper Handler. Validity check MUST compare both editor instance and
 * active input-session generation at execution time.
 */
final class SessionBoundEditorOrder384 {
    interface Poster { boolean post(Runnable action); }
    private static final int MAX_OUTSTANDING=24;
    private static final class Pending {
        final long token;
        Runnable action;
        BooleanSupplier isValid;
        boolean resolved;
        Pending(long token){this.token=token;}
    }
    private final ArrayDeque<Pending> queue=new ArrayDeque<>();
    private final Poster poster;
    private long nextToken=1L;
    private long epoch=1L;
    private int open;
    SessionBoundEditorOrder384(Poster poster){
        if(poster==null)throw new IllegalArgumentException("Missing editor action poster");
        this.poster=poster;
    }
    long down(){
        if(queue.size()>=MAX_OUTSTANDING)cancelAll();
        long id=nextToken++;
        queue.addLast(new Pending(id));
        open++;
        return id;
    }
    boolean accepted(long token,Runnable editorAction,BooleanSupplier editorSessionValid){
        if(editorAction==null || editorSessionValid==null)return false;
        return resolve(token,editorAction,editorSessionValid);
    }
    boolean cancel(long token){return resolve(token,null,null);}
    private boolean resolve(long token,Runnable action,BooleanSupplier valid){
        Pending target=null;
        for(Pending item:queue)if(item.token==token){target=item;break;}
        if(target==null || target.resolved)return false;
        target.resolved=true;
        target.action=action;
        target.isValid=valid;
        open--;
        flush();
        return true;
    }
    void cancelAll(){
        epoch++; // invalidate even work already posted but not executed
        queue.clear();
        open=0;
    }
    boolean hasOpenPresses(){return open>0;}
    int queuedCount(){return queue.size();}

    private void flush(){
        while(!queue.isEmpty() && queue.peekFirst().resolved){
            Pending first=queue.removeFirst();
            if(first.action==null)continue;
            final long capturedEpoch=epoch;
            final Runnable action=first.action;
            final BooleanSupplier valid=first.isValid;
            final Runnable guarded=()->{
                if(epoch!=capturedEpoch)return;
                final boolean sameSession;
                try{sameSession=valid.getAsBoolean();}
                catch(RuntimeException detachedEditor){return;}
                if(sameSession)action.run();
            };
            final boolean queued;
            try{queued=poster.post(guarded);}
            catch(RuntimeException refused){cancelAll();return;}
            if(!queued){cancelAll();return;}
        }
    }
}

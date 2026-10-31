package com.chroomareborn.keyboard;

import android.content.Context;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A bounded, single-threaded queue for already verified, opt-in approved
 * Space-boundary word-learning persistence. Never performs editor IPC.
 * It does not observe keys or invent new learning candidates.
 */
final class ApprovedSpaceLearning413 implements AutoCloseable {
    private final ThreadPoolExecutor worker=new ThreadPoolExecutor(
            1,1,0L,TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(128),r->{
                // Android OS background priority avoids competing with
                // touch/key UI dispatch and music playback under CPU load.
                Runnable backgroundTask=()->{
                    android.os.Process.setThreadPriority(
                            android.os.Process.THREAD_PRIORITY_BACKGROUND);
                    r.run();
                };
                Thread t=new Thread(backgroundTask,"OrivraApprovedSpaceLearning");
                t.setDaemon(true);
                t.setPriority(Thread.NORM_PRIORITY-1);
                return t;
            },new ThreadPoolExecutor.DiscardPolicy());

    /**
     * The caller has already checked the editor, session, consent and Space
     * commit. Only bounded approved word pairs are queued. Model and personal
     * stores independently re-check user consent before persisting.
     */
    void record(Context context,String previous,String usedWord,
            String standaloneWord) {
        if(context==null ||
                (usedWord==null && standaloneWord==null))return;
        final Context app=context.getApplicationContext()==null?
                context:context.getApplicationContext();
        if(!PersonalLearning.enabled(app))return;
        try {
            worker.execute(()->{
                final long start=OrivraPerfAudit411.begin();
                try {
                    if(!PersonalLearning.enabled(app))return;
                    if(previous!=null && usedWord!=null)
                        PersonalContextLearning319.recordUsed(
                                app,previous,usedWord);
                    if(standaloneWord!=null)
                        PersonalWordFrequency328.recordCommitted(
                                app,standaloneWord);
                } finally {
                    OrivraPerfAudit411.end(
                            OrivraPerfAudit411.LEARNING_WORKER_413,start);
                }
            });
        } catch(RejectedExecutionException shuttingDown) {
            // No UI-thread fallback. A failed/overloaded diagnostic worker
            // must never stall typing or re-queue an old learning action.
        }
    }
    @Override public void close(){
        // Finish already queued approved learning; never block IME teardown.
        worker.shutdown();
    }
}

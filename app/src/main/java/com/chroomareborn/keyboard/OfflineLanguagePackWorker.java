package com.chroomareborn.keyboard;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Handler;
import android.os.Looper;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Research-only asynchronous Android asset loader. No IME/editor access,
 * no telemetry, no file writes, and no predictor injection.
 *
 * Class intentionally is not connected to RebornImeService in Build 302.
 * Future activation requires independent Samsung performance and privacy QA.
 */
final class OfflineLanguagePackWorker implements AutoCloseable {
    interface Listener {
        /** Always delivered on the Android main thread with no user text. */
        void complete(CompressedLanguagePack.Loaded loaded);
    }
    private final ExecutorService worker=Executors.newSingleThreadExecutor(task->{
        Thread thread=new Thread(task,"OrivraOfflineDictionaryIO");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });
    private final Handler main=new Handler(Looper.getMainLooper());
    private boolean closed;
    private boolean started;

    synchronized boolean start(Context context,Listener listener) {
        if(started||closed||context==null||listener==null)return false;
        started=true;
        // The only retained reference is the application AssetManager.
        final AssetManager assets=context.getApplicationContext().getAssets();
        worker.execute(()->{
            CompressedLanguagePack.Loaded ready=null;
            try(InputStream stream=assets.open(CompressedLanguagePack.ASSET_PATH)){
                ready=CompressedLanguagePack.tryRead(stream);
            }catch(IOException missingOrCorrupt){
                // Optional asset missing -> keep original predictor.
            }
            final CompressedLanguagePack.Loaded result=ready;
            main.post(()->{
                synchronized(OfflineLanguagePackWorker.this){
                    if(closed)return;
                }
                listener.complete(result);
            });
        });
        return true;
    }
    @Override public synchronized void close(){
        if(closed)return;
        closed=true;
        worker.shutdownNow();
        // No retained typed input, no lifecycle-dependent editor state.
    }
}

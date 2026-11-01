package com.chroomareborn.keyboard;

import android.content.Context;
import java.io.IOException;
import java.io.InputStream;

/**
 * Build 417: process-local, text-free dictionary loader diagnostics.
 *
 * No user input, packages, words, cursor state or memory dumps are retained.
 * State is intentionally not persisted, so a process restart cannot falsely
 * report yesterday's Ready state.
 */
final class OfflineDictionaryStatus417 {
    enum Phase { NOT_STARTED, LOADING, READY, FAILED, PAUSED_PRIVATE, DISABLED }

    private static volatile Phase phase=Phase.NOT_STARTED;
    private static volatile int verifiedWords=0;
    private static volatile long loadMillis=-1L;

    private OfflineDictionaryStatus417(){}

    static void loading(){
        phase=Phase.LOADING;
        verifiedWords=0;
        loadMillis=-1L;
    }
    static void ready(int count,long nanos){
        if(count!=CompressedLanguagePack.EXPECTED_WORDS){
            failed();
            return;
        }
        verifiedWords=count;
        loadMillis=Math.max(0L,nanos/1_000_000L);
        phase=Phase.READY;
    }
    static void failed(){
        phase=Phase.FAILED;
        verifiedWords=0;
        loadMillis=-1L;
    }
    static void disabled(){
        phase=Phase.DISABLED;
        verifiedWords=0;
        loadMillis=-1L;
    }
    static void protectedField(){ phase=Phase.PAUSED_PRIVATE; }
    static void resumedWithLoadedPack(){
        if(verifiedWords==CompressedLanguagePack.EXPECTED_WORDS)
            phase=Phase.READY;
    }

    /** Header check only, no decompression, performed from Settings. */
    static boolean assetBundled(Context context){
        if(context==null)return false;
        try(InputStream in=context.getAssets().open(
                CompressedLanguagePack.ASSET_PATH)){
            return in.read()==0x1f && in.read()==0x8b;
        }catch(IOException|RuntimeException missing){
            return false;
        }
    }

    static String label(Context context,boolean optedIn){
        boolean packaged=assetBundled(context);
        StringBuilder b=new StringBuilder();
        b.append("Pack in APK: ").append(packaged?"YES":"MISSING");
        b.append("\nWords in verified pack: ")
                .append(CompressedLanguagePack.EXPECTED_WORDS);
        b.append(" (exact English recognition, not fuzzy prediction)");
        if(!optedIn) {
            b.append("\nActivation: OFF (requires opt-in)");
            return b.toString();
        }
        b.append("\nActivation: ");
        if(!packaged){
            b.append("UNAVAILABLE — packaged asset missing or invalid header");
            return b.toString();
        }
        switch(phase){
            case LOADING: b.append("LOADING on background worker");break;
            case READY:
                b.append("READY — ").append(verifiedWords)
                        .append(" verified words, load ")
                        .append(loadMillis).append(" ms");
                break;
            case FAILED:
                b.append("FAILED — integrity/load error; using base dictionary");
                break;
            case PAUSED_PRIVATE:
                b.append("PAUSED in protected field");break;
            case DISABLED:
            case NOT_STARTED:
            default: b.append("NOT LOADED — open a normal text field");break;
        }
        return b.toString();
    }
}

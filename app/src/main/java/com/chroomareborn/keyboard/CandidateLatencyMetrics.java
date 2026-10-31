package com.chroomareborn.keyboard;

import java.util.Arrays;
import java.util.Locale;

/**
 * Orivra Candidate-only, explicitly opted-in, in-memory latency diagnostics.
 * Only durations and sample counts are retained: never keystrokes, editor
 * contents, suggestions, input lengths, wall-clock times, or identifiers.
 * All values vanish when the process ends or recording is disabled.
 */
final class CandidateLatencyMetrics {
    private CandidateLatencyMetrics() {}
    static final String CANDIDATE_PACKAGE="com.chroomareborn.keyboard.build400trial";
    private static final int CAPACITY=120;
    private static final long MAX_DURATION_NANOS=30_000_000_000L;
    private static final long[] compute=new long[CAPACITY];
    private static final long[] uiReady=new long[CAPACITY];
    private static final long[] nextFrame=new long[CAPACITY];
    private static final long[] keyHandler340=new long[CAPACITY];
    private static final long[] deferredUi401=new long[CAPACITY];
    private static int readySamples, frameSamples, keySamples340,deferredSamples401;
    // Sampling once per 16 keys avoids creating a memory profiler that
    // itself slows typing. Counts only; no text, timestamps or app identity.
    private static int heapSamples401;
    private static long lastJavaHeap401,peakJavaHeap401;
    private static boolean enabled;

    static boolean isCandidate(String packageName) {
        return CANDIDATE_PACKAGE.equals(packageName);
    }
    static synchronized boolean enabled(String packageName) {
        return isCandidate(packageName)&&enabled;
    }
    static synchronized boolean setEnabled(String packageName,boolean next) {
        if(!isCandidate(packageName))return false;
        enabled=next;
        reset();
        return enabled;
    }
    static synchronized void clear(String packageName) {
        if(isCandidate(packageName))reset();
    }
    private static void reset() {
        Arrays.fill(compute,0L);
        Arrays.fill(uiReady,0L);
        Arrays.fill(nextFrame,0L);
        Arrays.fill(keyHandler340,0L);
        Arrays.fill(deferredUi401,0L);
        readySamples=0;
        frameSamples=0;
        keySamples340=0;
        deferredSamples401=0;
        heapSamples401=0;
        lastJavaHeap401=0L;
        peakJavaHeap401=0L;
    }
    static synchronized long start(String packageName) {
        return enabled(packageName)?System.nanoTime():0L;
    }
    // The frame callbacks are independent: the next key may arrive before
    // a frame, and stale callbacks are dropped by the IME generation guard.
    static synchronized boolean recordReady(long start,long predictionEnd,long viewUpdated) {
        if(!enabled || start==0L || predictionEnd-start<0L ||
                viewUpdated-predictionEnd<0L ||
                viewUpdated-start>MAX_DURATION_NANOS)return false;
        int slot=readySamples%CAPACITY;
        compute[slot]=predictionEnd-start;
        uiReady[slot]=viewUpdated-start;
        readySamples++;
        return true;
    }
    /** Time spent synchronously handling a typed key, including all UI work.
     * In-memory duration only: no text, key identity or input length.
     */
    static synchronized void recordKeyHandler340(long start,long end) {
        if(!enabled || start==0L || end-start<0L ||
                end-start>MAX_DURATION_NANOS)return;
        keyHandler340[keySamples340%CAPACITY]=end-start;
        keySamples340++;
        if(keySamples340%16==0) {
            Runtime rt=Runtime.getRuntime();
            long used=Math.max(0L,rt.totalMemory()-rt.freeMemory());
            lastJavaHeap401=used;
            peakJavaHeap401=Math.max(peakJavaHeap401,used);
            heapSamples401++;
        }
    }
    /** One coalesced suggestion refresh, numeric duration only, not editor text. */
    static synchronized void recordDeferredUi401(long start,long end) {
        if(!enabled || start==0L || end-start<0L ||
                end-start>MAX_DURATION_NANOS)return;
        deferredUi401[deferredSamples401%CAPACITY]=end-start;
        deferredSamples401++;
    }
    static synchronized void recordFrame(long start,long frameAt) {
        if(!enabled || start==0L || frameAt-start<0L ||
                frameAt-start>MAX_DURATION_NANOS)return;
        nextFrame[frameSamples%CAPACITY]=frameAt-start;
        frameSamples++;
    }
    private static String stats(long[] values,int total) {
        int count=Math.min(total,CAPACITY);
        if(count==0)return "No samples yet";
        long[] selected=Arrays.copyOf(values,count);
        Arrays.sort(selected);
        double median=selected[count/2]/1_000_000.0;
        double p95=selected[Math.max(0,(int)Math.ceil(count*0.95)-1)]/1_000_000.0;
        double max=selected[count-1]/1_000_000.0;
        return String.format(Locale.US,"n=%d, median %.1f ms, p95 %.1f ms, max %.1f ms",
                count,median,p95,max);
    }
    private static String memoryStatus401() {
        Runtime rt=Runtime.getRuntime();
        long used=Math.max(0L,rt.totalMemory()-rt.freeMemory());
        long peak=Math.max(peakJavaHeap401,used);
        long nativeBytes=android.os.Debug.getNativeHeapAllocatedSize();
        return String.format(Locale.US,
                "Java heap now: %.1f MB (max %.1f MB)\n"+
                "Java heap peak sampled: %.1f MB (n=%d, every 16 key events)\n"+
                "Native heap allocated now: %.1f MB\n"+
                "Heap size changes do not by themselves prove a memory leak.\n",
                used/(1024.0*1024.0),
                rt.maxMemory()/(1024.0*1024.0),
                peak/(1024.0*1024.0),
                heapSamples401,nativeBytes/(1024.0*1024.0));
    }
    static synchronized String summary(String packageName) {
        if(!isCandidate(packageName))
            return "Diagnostics unavailable: only the separate Orivra Candidate may record timings.";
        if(!enabled)
            return "Recording OFF. No timing samples retained.";
        return "Recording ON (memory only, last 120 samples per measurement)\n"
                +"Prediction completed: "+stats(compute,readySamples)+"\n"
                +"Suggestion views updated: "+stats(uiReady,readySamples)+"\n"
                +"Next animation callback: "+stats(nextFrame,frameSamples)+"\n"
                +"Typed-key UI work: "+stats(keyHandler340,keySamples340)+"\n"
                +"Deferred suggestion UI work: "+
                    stats(deferredUi401,deferredSamples401)+"\n"
                +memoryStatus401()
                +"Prediction timing includes the 12-28 ms debounce. "
                +"The animation callback is before frame rendering, "
                +"not proof that pixels reached the screen.\n"
                +"No text, suggestions, app names, or input history is saved.";
    }
}

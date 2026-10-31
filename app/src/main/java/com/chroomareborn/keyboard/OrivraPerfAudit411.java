package com.chroomareborn.keyboard;

/**
 * Build 411: temporary on-device performance measurements. OFF by default.
 *
 * Only counts and elapsed times are collected in process memory.
 * No key codes, text, app package names, suggestions, paths, or network.
 * Data resets when the process exits or the user presses Reset.
 *
 * Profiling changes scheduling slightly; always compare the same test with
 * audit ON and OFF before concluding a culprit.
 */
final class OrivraPerfAudit411 {
    static final String PREFS="orivra_performance_audit_411";
    static final String ENABLED="enabled";
    static final int KEY=0, SPACE=1, BACKSPACE=2,
            SUGGESTION_ENTRY=3, SUGGESTION_RENDER=4, RISE_PANEL=5,
            KEY_LAYOUT=6, THEME=7, PREDICT_WORKER=8,
            CURSOR_READ=9, EDIT_PREPARE=10,
            SPLIT_ON_UI=11, CHIP_BIND=12, SPLIT_WORKER_412=13,
            KEY_INITIAL_EDITOR_READ_413=14, COMPOSE_MUTATION_413=15,
            IDLE_WORD_OBSERVATION_413=16, SPACE_APPROVED_LEARNING_413=17,
            SPACE_EDITOR_COMMIT_413=18, LEARNING_WORKER_413=19,
            SPACE_PRECOMMIT_CHECKS_413=20, SPACE_POSTCOMMIT_413=21,
            IDLE_WORD_VERIFICATION_414=22, HAPTIC_TOUCH_415=23,
            SPACE_SHIFT_415=24, SPACE_REFRESH_415=25;
    static final int SIZE=26;
    private static final String[] NAMES={
        "Letter key handler", "Space handler", "Backspace handler",
        "Suggestion scheduling", "Suggestion rendering", "Rise panel UI",
        "Keyboard layout refresh", "Theme/color update", "Predictor worker",
        "Word-at-cursor read", "Cursor/edit preparation",
        "Split-phrase calculations on UI", "Suggestion-chip binding",
        "Split-phrase calculations on worker",
        "Key initial editor read", "Verified composing edit",
        "Idle word observation", "Space learning dispatch",
        "Space finish/commit IPC", "Approved learning background worker",
        "Space boundary checks before commit", "Space processing after commit",
        "Idle word verification after typing stops", "Key haptic feedback",
        "Space cursor/sentence state", "Space suggestion refresh schedule"
    };
    private static volatile boolean enabled;
    private static final long[] count=new long[SIZE];
    private static final long[] nanos=new long[SIZE];
    private static final long[] maximumNanos=new long[SIZE];
    private static final long[] over8ms=new long[SIZE];
    private static final long[] over16ms=new long[SIZE];
    private static final long[] over32ms=new long[SIZE];
    private static long lastResetMillis=System.currentTimeMillis();
    private static long learningQueueOverflow413;
    private static long hiddenStaleChip414,discardedFrame414,
            recoveredFrame414;
    private static long predictionScheduled415,predictionPreempted415,
            predictionCompleted415;

    private OrivraPerfAudit411(){}

    static void setEnabled(boolean value){enabled=value;}
    static boolean isEnabled(){return enabled;}
    static long begin(){return enabled?System.nanoTime():0L;}
    static void end(int stage,long start){
        if(start==0L || !enabled || stage<0 || stage>=SIZE)return;
        long dt=System.nanoTime()-start;
        if(dt<0L)return;
        addNanos(stage,dt);
    }
    static synchronized void addNanos(int stage,long dt){
        if(!enabled || stage<0 || stage>=SIZE || dt<0L)return;
        count[stage]++;
        nanos[stage]+=dt;
        if(dt>maximumNanos[stage])maximumNanos[stage]=dt;
        if(dt>=8_000_000L)over8ms[stage]++;
        if(dt>=16_000_000L)over16ms[stage]++;
        if(dt>=32_000_000L)over32ms[stage]++;
    }
    static synchronized void recordLearningQueueOverflow413(){
        if(enabled)learningQueueOverflow413++;
    }
    static synchronized void recordHiddenStaleChip414(){
        if(enabled)hiddenStaleChip414++;
    }
    static synchronized void recordDiscardedFrame414(){
        if(enabled)discardedFrame414++;
    }
    static synchronized void recordRecoveredFrame414(){
        if(enabled)recoveredFrame414++;
    }
    static synchronized void recordPredictionScheduled415(){
        if(enabled)predictionScheduled415++;
    }
    static synchronized void recordPredictionPreempted415(){
        if(enabled)predictionPreempted415++;
    }
    static synchronized void recordPredictionCompleted415(){
        if(enabled)predictionCompleted415++;
    }
    static synchronized void reset(){
        learningQueueOverflow413=0;
        hiddenStaleChip414=discardedFrame414=recoveredFrame414=0;
        predictionScheduled415=predictionPreempted415=
                predictionCompleted415=0;
        for(int i=0;i<SIZE;i++){
            count[i]=0;nanos[i]=0;maximumNanos[i]=0;
            over8ms[i]=0;over16ms[i]=0;over32ms[i]=0;
        }
        lastResetMillis=System.currentTimeMillis();
    }
    private static String ms(long nanoseconds){
        // Locale-independent decimal output, accurate to 0.01ms.
        long hundredths=(nanoseconds+5_000L)/10_000L;
        return (hundredths/100L)+"."+
                (hundredths%100L<10L?"0":"")+(hundredths%100L);
    }
    static synchronized String report(String settings){
        StringBuilder b=new StringBuilder(900);
        b.append("ORIVRA BUILD 415 KEY/PREDICTION PERFORMANCE AUDIT\n");
        b.append("Counts and timings only. No typing stored or transmitted.\n");
        b.append("Audit: ").append(enabled?"ON":"OFF");
        b.append(" | elapsed since reset: ")
                .append(Math.max(0L,(System.currentTimeMillis()-
                        lastResetMillis)/1000L)).append(" s\n");
        if(settings!=null)b.append(settings).append('\n');
        for(int i=0;i<SIZE;i++){
            if(count[i]==0L)continue;
            b.append(NAMES[i]).append(": count=").append(count[i]);
            b.append(" avg=").append(ms(nanos[i]/count[i])).append("ms");
            b.append(" max=").append(ms(maximumNanos[i])).append("ms");
            b.append(" >=8/16/32ms=")
                    .append(over8ms[i]).append("/")
                    .append(over16ms[i]).append("/")
                    .append(over32ms[i]).append('\n');
        }
        b.append("Background learning queue overflow drops: ")
                .append(learningQueueOverflow413).append('\n');
        b.append("Stale untappable chips hidden: ")
                .append(hiddenStaleChip414).append('\n');
        b.append("Outdated prediction frames discarded: ")
                .append(discardedFrame414).append('\n');
        b.append("Prediction frame refresh retries: ")
                .append(recoveredFrame414).append('\n');
        b.append("Prediction worker jobs scheduled: ")
                .append(predictionScheduled415).append('\n');
        b.append("Prediction pending jobs preempted: ")
                .append(predictionPreempted415).append('\n');
        b.append("Prediction worker results delivered: ")
                .append(predictionCompleted415).append('\n');
        b.append("Note: render/panel timings overlap with suggestion "+
                "entry; don't add their times together.\n");
        b.append("Slow handlers do not establish physical touch-to-frame "+
                "latency; use Android System Trace if needed.");
        return b.toString();
    }
}

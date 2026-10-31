package com.chroomareborn.keyboard;

/** Pure-Java checks; no Android SDK, emulator, or personal data required. */
public final class PredictionIsolationMode409Test {
    private static int checks;
    private static void check(boolean condition,String name) {
        checks++;
        if(!condition)throw new AssertionError(name);
    }
    public static void main(String[] args) {
        final int off=PredictionIsolationMode409.OFF;
        final int worker=PredictionIsolationMode409.WORKER_ONLY;
        final int full=PredictionIsolationMode409.FULL;
        check(PredictionIsolationMode409.normalize(-1)==off,"invalid low");
        check(PredictionIsolationMode409.normalize(3)==off,"invalid high");
        check(PredictionIsolationMode409.next(off)==worker,"off->worker");
        check(PredictionIsolationMode409.next(worker)==full,"worker->full");
        check(PredictionIsolationMode409.next(full)==off,"full->off");
        check(!PredictionIsolationMode409.renders(off),"off not painted");
        check(!PredictionIsolationMode409.computes(off),"off not computed");
        check(!PredictionIsolationMode409.renders(worker),"worker not painted");
        check(PredictionIsolationMode409.computes(worker),"worker computes");
        check(PredictionIsolationMode409.renders(full),"full painted");
        check(PredictionIsolationMode409.computes(full),"full computes");
        check(PredictionIsolationMode409.label(off).contains("OFF"),"off label");
        check(PredictionIsolationMode409.label(worker).contains("WORKER"),"worker label");
        check(PredictionIsolationMode409.label(full).contains("FULL"),"full label");
        System.out.println("PredictionIsolationMode409Test PASS ("+checks+" checks)");
    }
}

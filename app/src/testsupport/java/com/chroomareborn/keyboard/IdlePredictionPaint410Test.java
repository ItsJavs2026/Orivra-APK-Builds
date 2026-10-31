package com.chroomareborn.keyboard;

/** Pure Java test; no Android SDK or personal typing data required. */
public final class IdlePredictionPaint410Test {
    private static int checks;
    private static void yes(boolean pass,String name){
        checks++;
        if(!pass)throw new AssertionError(name);
    }
    public static void main(String[] args){
        IdlePredictionPaint410 gate=new IdlePredictionPaint410();
        yes(gate.isQuiet(1000L),"fresh editor is quiet");
        yes(gate.onKey(1000L)==210L,"first key schedules 210ms");
        yes(!gate.isQuiet(1209L),"no UI before quiet threshold");
        yes(gate.isQuiet(1210L),"UI allowed after quiet threshold");
        yes(gate.onKey(1200L)==210L,"subsequent key restarts delay");
        yes(!gate.isQuiet(1409L),"new key moves deadline");
        yes(gate.isQuiet(1410L),"new deadline accepted");
        for(long t=1500L;t<4500L;t+=75L) {
            gate.onKey(t);
            yes(!gate.isQuiet(t+74L),"continuous typing never forces a paint");
        }
        yes(gate.isQuiet(4710L),"after sustained typing eventually paints");
        gate.reset();
        yes(gate.isQuiet(0L),"editor exit resets gate");
        gate.onKey(300L);
        yes(!gate.isQuiet(200L),"backwards clock does not paint");
        System.out.println("IdlePredictionPaint410Test PASS ("+checks+" checks)");
    }
}

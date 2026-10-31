package com.chroomareborn.keyboard;

/** Tests quicker post-typing recovery without forced processing mid-burst. */
public final class FastIdlePrediction415Test {
    private static int checks;
    private static void check(boolean yes,String message) {
        checks++; if(!yes)throw new AssertionError(message);
    }
    public static void main(String[] args) {
        IdlePredictionPaint410 gate=new IdlePredictionPaint410();
        check(gate.onKey(1000L)==210L,"legacy mode defaults to 210ms");
        check(!gate.isQuiet(1209L),"legacy does not paint early");
        check(gate.isQuiet(1210L),"legacy paint after pause");
        gate.reset();
        gate.setQuietAfterKeyMs(145L);
        check(gate.onKey(1000L)==145L,"fast mode 145ms");
        check(!gate.isQuiet(1144L),"fast mode not early");
        check(gate.isQuiet(1145L),"fast mode after quiet");
        for(long t=1200L;t<4000L;t+=70L) {
            gate.onKey(t);
            check(!gate.isQuiet(t+70L),"continuous typing never forces full prediction");
        }
        check(gate.isQuiet(4200L),"fast recovery after continuous burst");
        gate.reset();
        gate.setQuietAfterKeyMs(80L);
        check(gate.onKey(100L)==110L,"minimum quiet cap");
        gate.setQuietAfterKeyMs(500L);
        check(gate.onKey(100L)==210L,"maximum quiet cap");
        System.out.println("FastIdlePrediction415Test PASS ("+checks+" checks)");
    }
}

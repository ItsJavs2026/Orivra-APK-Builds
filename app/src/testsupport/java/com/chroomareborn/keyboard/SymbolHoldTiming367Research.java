package com.chroomareborn.keyboard;

/** Research only; no touching real editor content or timing claims. */
public final class SymbolHoldTiming367Research {
    private static int checks;
    private static void eq(int actual,int expected,String context) {
        checks++;
        if(actual!=expected)throw new AssertionError(context+
                ": expected "+expected+" ms, got "+actual+" ms");
    }
    public static void main(String[] args) {
        eq(SymbolHoldTiming367.delayMs(500,true),320,"Fast on 500ms platform");
        eq(SymbolHoldTiming367.delayMs(700,true),320,"Fast on 700ms platform");
        eq(SymbolHoldTiming367.delayMs(280,true),280,"Do not slow faster platform");
        eq(SymbolHoldTiming367.delayMs(500,false),500,"Opt-out retains Android");
        eq(SymbolHoldTiming367.delayMs(900,false),900,"Longer accessibility preference");
        eq(SymbolHoldTiming367.delayMs(0,true),320,"Invalid platform guard");
        eq(SymbolHoldTiming367.delayMs(-2,false),500,"Negative platform guard");
        eq(SymbolHoldTiming367.delayMs(4000,false),500,"Unreasonably long guard");
        // Stress deterministic user-settings edge cases, verifying no 0-ms
        // accidental secondary symbol and no delayed >platform fast mode.
        for(int system=0;system<3000;system++) {
            int selected=SymbolHoldTiming367.delayMs(system,true);
            int normal=SymbolHoldTiming367.delayMs(system,false);
            if(selected<180 || selected>320 || selected>normal)
                throw new AssertionError("Invalid fast threshold: "+system);
            checks++;
        }
        System.out.println("PASS Research 367: "+checks+
                " timing regressions, only special-character hint behavior");
    }
}

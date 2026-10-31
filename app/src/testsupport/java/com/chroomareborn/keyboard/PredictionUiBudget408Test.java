package com.chroomareborn.keyboard;

/** Run with javac/java; does not need Android SDK, emulator or device. */
public final class PredictionUiBudget408Test {
    private static int checks;
    private static void equal(long expected,long actual,String name) {
        checks++;
        if(expected!=actual)
            throw new AssertionError(name+": expected "+expected+", got "+actual);
    }
    public static void main(String[] args) {
        PredictionUiBudget408 budget=new PredictionUiBudget408();
        equal(150L,budget.delayAfterKey(1000L),"initial pause");
        equal(150L,budget.delayAfterKey(1040L),"typing restarts gap");
        equal(150L,budget.delayAfterKey(1200L),"within cap");
        equal(60L,budget.delayAfterKey(1300L),"approaching max burst");
        equal(1L,budget.delayAfterKey(1360L),"continuous typing has cap");
        equal(1L,budget.delayAfterKey(1500L),"never schedules zero delay");
        budget.reset();
        equal(150L,budget.delayAfterKey(1800L),"new burst after dispatch");
        budget.reset();
        equal(150L,budget.delayAfterKey(100L),"after editor switch");
        equal(150L,budget.delayAfterKey(90L),"backward clock resets burst");
        // The helper contains no text, editor reference, or pending work;
        // the IME cancels its Handler callback on input/session exit.
        System.out.println("PredictionUiBudget408Test PASS ("+checks+" checks)");
    }
}

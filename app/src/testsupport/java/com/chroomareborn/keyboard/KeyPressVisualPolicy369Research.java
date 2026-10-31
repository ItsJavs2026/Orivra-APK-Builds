package com.chroomareborn.keyboard;

/**
 * Pure synthetic A/B policy check; no real device-latency measurement.
 */
public final class KeyPressVisualPolicy369Research {
    public static void main(String[] args) {
        if(KeyPressVisualPolicy369.shouldScaleAnimate(true,true))
            throw new AssertionError("Instant mode must not schedule scale animations");
        if(KeyPressVisualPolicy369.shouldScaleAnimate(true,false))
            throw new AssertionError("Instant mode must respect OS motion disabled");
        if(!KeyPressVisualPolicy369.shouldScaleAnimate(false,true))
            throw new AssertionError("Baseline mode must retain Candidate 365 animation");
        if(KeyPressVisualPolicy369.shouldScaleAnimate(false,false))
            throw new AssertionError("OS disabled motion must remain disabled");
        System.out.println("PASS Research 369: instant visual A/B motion policy; no latency claim");
    }
}

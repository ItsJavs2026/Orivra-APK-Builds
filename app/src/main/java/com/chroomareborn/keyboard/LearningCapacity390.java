package com.chroomareborn.keyboard;

/**
 * Research 390 migration ceilings. Keep backup codec acceptance and live
 * learning-store limits in sync, without making any records public or
 * modifying existing preferences. Reuse existing 100KB checksum protection.
 *
 * Increase is bounded: 384/320/400, enough for the verified 378+386 union
 * (304 corrections, 242 context entries, 326 word frequencies) with
 * moderate headroom and without doubling the live scan budget.
 */
final class LearningCapacity390 {
    static final int MAX_CORRECTIONS=384;
    static final int MAX_CONTEXT_KEYS=320;
    static final int MAX_FREQUENCIES=400;
    private LearningCapacity390(){}

    static boolean fits(int correctionKeys,int contextKeys,int frequencyKeys){
        return correctionKeys>=0 && contextKeys>=0 && frequencyKeys>=0 &&
                correctionKeys<=MAX_CORRECTIONS &&
                contextKeys<=MAX_CONTEXT_KEYS &&
                frequencyKeys<=MAX_FREQUENCIES;
    }
}

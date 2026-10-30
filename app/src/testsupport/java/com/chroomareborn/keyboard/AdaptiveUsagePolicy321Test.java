package com.chroomareborn.keyboard;

import java.util.Map;

public final class AdaptiveUsagePolicy321Test {
    private static int checks=0;
    private static void ok(boolean v,String why){
        checks++; if(!v)throw new AssertionError(why);
    }
    public static void main(String[] args) {
        ok(!AdaptiveUsagePolicy321.suggestable(0,0),"no evidence");
        ok(!AdaptiveUsagePolicy321.suggestable(0,1),"single word not enough");
        ok(!AdaptiveUsagePolicy321.suggestable(0,2),"two typed uses not enough");
        ok(AdaptiveUsagePolicy321.suggestable(0,3),"3 real commits strengthen words");
        ok(!AdaptiveUsagePolicy321.suggestable(1,1),"1 tap+1 use still weak");
        ok(AdaptiveUsagePolicy321.suggestable(1,2),"1 tap and 2 uses reinforce");
        ok(AdaptiveUsagePolicy321.suggestable(2,0),"two explicit taps preserved");
        ok(AdaptiveUsagePolicy321.rank(3,0)>AdaptiveUsagePolicy321.rank(0,3),
                "explicit confirmation outranks passive use");
        ok("tell".equals(AdaptiveUsagePolicy321.reinforcedCorrection(
                "tekk",Map.of("tell",1),2)),
                "single confirmed tekk->tell then repeated tell use");
        ok(AdaptiveUsagePolicy321.reinforcedCorrection(
                "tekk",Map.of("tell",1),1)==null,
                "single used corrected word is not enough");
        ok(AdaptiveUsagePolicy321.reinforcedCorrection(
                "tekk",Map.of("tell",1,"test",1),25)==null,
                "competing corrections abstain even if one word common");
        ok(AdaptiveUsagePolicy321.reinforcedCorrection(
                "tell",Map.of("tell",1),25)==null,
                "correctly spelled word never silently rewritten");
        ok(AdaptiveUsagePolicy321.reinforcedCorrection(
                "tekk",Map.of("tell",2),25)==null,
                "two explicit taps follow legacy policy (no double boost)");
        ok("I'm".equals(AdaptiveUsagePolicy321.reinforcedCorrection(
                "im",Map.of("i'm",1),3)),
                "English first-person contraction remains capitalized");
        System.out.println("PASS: AdaptiveUsagePolicy321Test "+checks+" cases");
    }
}

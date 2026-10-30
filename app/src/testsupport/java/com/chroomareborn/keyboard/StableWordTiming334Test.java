package com.chroomareborn.keyboard;

public final class StableWordTiming334Test {
    static int cases=0;
    static void check(boolean ok,String reason) {
        cases++;if(!ok)throw new AssertionError(reason);
    }
    public static void main(String[] args) {
        check(StableWordTiming334.milliseconds("continue")==550L,
              "continue learns after a stable brief pause");
        check(StableWordTiming334.milliseconds("Continue")==550L,
              "sentence case does not change learning timing");
        check(StableWordTiming334.milliseconds("con")==1600L,
              "con often appears while typing continue");
        check(StableWordTiming334.milliseconds("my")==1600L,
              "short my does not receive speculative quick idle counts");
        check(StableWordTiming334.milliseconds("can")==1600L,
              "can is a valid word and a prefix of candy/cannot");
        check(StableWordTiming334.milliseconds("cant")==1100L,
              "four-letter fragment requires longer stability");
        check(StableWordTiming334.milliseconds("tell")==1100L,
              "ordinary word accuracy over speculative counts");
        check(StableWordTiming334.milliseconds("please")==800L,
              "medium words get a medium quiet window");
        check(StableWordTiming334.milliseconds("I'm")==1600L,
              "apostrophe isn't an extra letter");
        check(StableWordTiming334.milliseconds("12.00")==-1L,
              "bank amount is never a word candidate");
        check(StableWordTiming334.milliseconds("a")==-1L,
              "single character is not a completed-word frequency record");
        check(StableWordTiming334.milliseconds("please continue")==-1L,
              "never observe complete sentences");
        System.out.println("PASS StableWordTiming334Test "+cases+" checks");
    }
}

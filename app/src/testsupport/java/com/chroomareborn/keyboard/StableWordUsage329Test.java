package com.chroomareborn.keyboard;
public final class StableWordUsage329Test {
    private static int n;
    private static void ok(boolean result,String why) {
        n++;if(!result)throw new AssertionError(why);
    }
    public static void main(String[] args) {
        String continueWord=StableWordUsagePolicy329.candidate(
                "Continue",true,true,false,false,false,false,false,false);
        ok("continue".equals(continueWord),
                "standalone Continue may be learned without a trailing space");
        ok(StableWordUsagePolicy329.sameVisibleWord(
                continueWord,"Continue","Continue",true,true),
                "live committed text must equal composition");
        ok(StableWordUsagePolicy329.sameVisibleWord(
                "my","my","let my",true,true),
                "short words with whitespace boundary are eligible");
        ok(!StableWordUsagePolicy329.sameVisibleWord(
                "my","my","dummy",true,true),
                "never harvest a suffix of an unrelated word");
        ok(!StableWordUsagePolicy329.sameVisibleWord(
                "continue","continued","continued",true,true),
                "stale complete-word event must not count longer continuation");
        ok(!StableWordUsagePolicy329.sameVisibleWord(
                "continue","continue","continue",false,true),
                "changed editor rejected");
        ok(!StableWordUsagePolicy329.sameVisibleWord(
                "continue","continue","continue",true,false),
                "changed input session rejected");
        ok(!StableWordUsagePolicy329.sameVisibleWord(
                "continue","continue","continue",true,true)==false,
                "positive safety test");
        for(int deny=0;deny<7;deny++){
            boolean[] flags={true,true,false,false,false,false,false,false};
            switch(deny){
                case 0:flags[0]=false;break;
                case 1:flags[1]=false;break;
                case 2:flags[2]=true;break;
                case 3:flags[3]=true;break;
                case 4:flags[4]=true;break;
                case 5:flags[5]=true;break;
                case 6:flags[6]=true;break;
            }
            ok(StableWordUsagePolicy329.candidate("continue",
                    flags[0],flags[1],flags[2],flags[3],flags[4],
                    flags[5],flags[6],flags[7])==null,
                    "invalid editor condition "+deny);
        }
        ok(StableWordUsagePolicy329.candidate(
                "1234",true,true,false,false,false,false,false,false)==null,
                "numeric input never learned");
        ok(StableWordUsagePolicy329.candidate(
                "please continue",true,true,false,false,false,false,false,false)==null,
                "no sentence data recorded");
        ok(StableWordUsagePolicy329.candidate(
                "continued...",true,true,false,false,false,false,false,false)==null,
                "partial punctuation not stored");
        ok(StableWordUsagePolicy329.candidate(
                "can",true,true,false,false,false,false,false,true)==null,
                "banking amount field excluded");
        System.out.println("PASS StableWordUsage329Test "+n+" checks");
    }
}

package com.chroomareborn.keyboard;
public final class CommittedWordUsagePolicy321Test {
    static int checks;
    static void ok(boolean v,String message){checks++;
        if(!v)throw new AssertionError(message);}
    public static void main(String[] args){
        CommittedWordUsagePolicy321.Observation obs=
                CommittedWordUsagePolicy321.capture("please tell","tell",
                        true,true,false,false,false,false,false);
        ok(obs!=null && "please".equals(obs.prior) &&
                "tell".equals(obs.word),"normal Space learns composed word");
        ok(CommittedWordUsagePolicy321.capture("please tekk","tekk",
                true,true,false,false,false,false,false)!=null,
                "observe only after acceptedWord syntax; dictionary filters typo");
        ok(CommittedWordUsagePolicy321.capture("please tell","tekk",
                true,true,false,false,false,false,false)==null,
                "stale snapshot cannot teach a word");
        ok(CommittedWordUsagePolicy321.capture("privateName tell","tell",
                true,true,false,false,false,false,false)==null,
                "private previous context not stored");
        ok(CommittedWordUsagePolicy321.capture("please tell","tell",
                false,true,false,false,false,false,false)==null,
                "opt out");
        ok(CommittedWordUsagePolicy321.capture("please tell","tell",
                true,false,false,false,false,false,false)==null,
                "editor disables learning");
        ok(CommittedWordUsagePolicy321.capture("please tell","tell",
                true,true,true,false,false,false,false)==null,
                "sensitive editor");
        ok(CommittedWordUsagePolicy321.capture("please tell","tell",
                true,true,false,true,false,false,false)==null,
                "mid-document edit");
        ok(CommittedWordUsagePolicy321.capture("please tell","tell",
                true,true,false,false,true,false,false)==null,
                "selection");
        ok(CommittedWordUsagePolicy321.capture("please tell","tell",
                true,true,false,false,false,true,false)==null,
                "text after cursor");
        ok(CommittedWordUsagePolicy321.capture("please tell","tell",
                true,true,false,false,false,false,true)==null,
                "symbol layout");
        ok(CommittedWordUsagePolicy321.capture("please tell someone","tell someone",
                true,true,false,false,false,false,false)==null,
                "no full sentence in the learner");
        System.out.println("PASS: CommittedWordUsagePolicy321Test "+checks+" checks");
    }
}

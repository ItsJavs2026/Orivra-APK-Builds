package com.chroomareborn.keyboard;

/** Pure Java regression checks; no Android SDK, user data or editor required. */
public final class ComposingMutation413FastPathTest {
    private static int checks;
    private static void check(boolean condition,String label) {
        checks++;
        if(!condition)throw new AssertionError(label);
    }
    private static final class Fake implements ComposingMutation340.Editor {
        String before;
        int beforeCalls,composeCalls,commitCalls,finishCalls;
        boolean ack=true,mutates=true,sameSession=true;
        Fake(String initial){before=initial;}
        @Override public boolean sameSession(){return sameSession;}
        @Override public CharSequence before(){beforeCalls++;return before;}
        @Override public boolean setComposing(String next){
            composeCalls++;
            if(mutates)before=next;
            return ack;
        }
        @Override public boolean finishComposing(){finishCalls++;return true;}
        @Override public boolean commit(String addition){commitCalls++;before+=addition;return true;}
    }
    public static void main(String[] args) {
        Fake fast=new Fake("hel");
        check(ComposingMutation340.append(fast,"hel","l",true)==
                ComposingMutation340.Result.COMPOSING,"positive ACK success");
        check("hell".equals(fast.before),"one letter inserted");
        check(fast.beforeCalls==1,"fast ACK avoids second editor read");
        check(fast.composeCalls==1,"single composing mutation");
        Fake normal=new Fake("hel");
        check(ComposingMutation340.append(normal,"hel","l",false)==
                ComposingMutation340.Result.COMPOSING,"normal composing result");
        check(normal.beforeCalls==2,"default path retains postflight read");
        Fake negativeApplied=new Fake("hel");
        negativeApplied.ack=false;
        check(ComposingMutation340.append(negativeApplied,"hel","l",true)==
                ComposingMutation340.Result.COMMITTED,"negative but applied");
        check(negativeApplied.beforeCalls>=3,"negative ACK stays fully verified");
        check(negativeApplied.commitCalls==0,"no duplicated letter on false ACK");
        Fake negativeNoMutation=new Fake("hel");
        negativeNoMutation.ack=false;
        negativeNoMutation.mutates=false;
        check(ComposingMutation340.append(negativeNoMutation,"hel","l",true)==
                ComposingMutation340.Result.COMMITTED,"failed composing fallback");
        check(negativeNoMutation.commitCalls==1,"committed only after verified unchanged");
        check("hell".equals(negativeNoMutation.before),"fallback committed once");
        Fake mismatch=new Fake("other");
        check(ComposingMutation340.append(mismatch,"hel","l",true)==
                ComposingMutation340.Result.PRECONDITION,"stale caret rejected");
        check(mismatch.composeCalls==0,"no mutation after stale precondition");
        System.out.println("ComposingMutation413FastPathTest PASS ("+checks+" checks)");
    }
}

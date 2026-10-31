package com.chroomareborn.keyboard;

/** Pure Java no-SDK regression checks for immediate keypress candidate gate. */
public final class IdleWordCandidate414Test {
    private static int checks;
    private static void eq(String expected,String actual,String label) {
        checks++;
        if(!expected.equals(actual))
            throw new AssertionError(label+" expected "+expected+" got "+actual);
    }
    public static void main(String[] args) {
        eq("hello",IdleWordCandidate414.current("hello","stale",false,true),
                "compose has precedence");
        eq("hello",IdleWordCandidate414.current("","hello",true,true),
                "direct-commit editor current typed word");
        eq("",IdleWordCandidate414.current("","old",false,true),
                "no previous session reuse");
        eq("",IdleWordCandidate414.current("","hello",true,false),
                "deny sensitive or unsupported fields");
        eq("",IdleWordCandidate414.current(null,null,false,true),
                "no content yields no candidate");
        eq("jo'",IdleWordCandidate414.current("jo'",null,false,true),
                "incomplete contraction remains local");
        eq("",IdleWordCandidate414.current("hello","hello",true,false),
                "deny overrides local composing");
        System.out.println("IdleWordCandidate414Test PASS ("+checks+" checks)");
    }
}

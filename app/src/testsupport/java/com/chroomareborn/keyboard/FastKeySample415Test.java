package com.chroomareborn.keyboard;
public final class FastKeySample415Test {
    private static int n;
    private static void check(boolean value,String reason) {
        n++; if(!value)throw new AssertionError(reason);
    }
    private static boolean skip(String composing,String value) {
        return FastKeySample415.skipInitialBeforeRead(
                composing,value,false,false,false,false,false,true);
    }
    public static void main(String[] args) {
        check(skip("hello","a"),"normal composed lowercase append");
        check(skip("hello","Z"),"normal composed uppercase append");
        check(!skip("","a"),"first character must read editor boundary");
        check(!skip("hello","."),"punctuation still reads");
        check(!skip("hello","'"),"contraction punctuation still reads");
        check(!skip("hello","ab"),"multi-character still reads");
        check(!FastKeySample415.skipInitialBeforeRead(
                "hello","a",true,false,false,false,false,true),"midword reads");
        check(!FastKeySample415.skipInitialBeforeRead(
                "hello","a",false,true,false,false,false,true),"selection reads");
        check(!FastKeySample415.skipInitialBeforeRead(
                "hello","a",false,false,true,false,false,true),"private reads");
        check(!FastKeySample415.skipInitialBeforeRead(
                "hello","a",false,false,false,true,false,true),"number mode reads");
        check(!FastKeySample415.skipInitialBeforeRead(
                "hello","a",false,false,false,false,true,true),"symbol mode reads");
        check(!FastKeySample415.skipInitialBeforeRead(
                "hello","a",false,false,false,false,false,false),"disabled predictions reads");
        check(!skip(null,"a"),"null composition reads");
        System.out.println("FastKeySample415Test PASS ("+n+" checks)");
    }
}

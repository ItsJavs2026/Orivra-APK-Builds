package com.chroomareborn.keyboard;

/** Standalone Java test: the action must rebind when its meaning changes. */
public final class ChipActionBinding412Test {
    private static int checks;
    private static void verify(boolean ok,String message){
        checks++;
        if(!ok)throw new AssertionError(message);
    }
    public static void main(String[] args){
        ChipActionBinding412 b=new ChipActionBinding412(
                "tekk","tell",44L,false);
        verify(b.matches("tekk","tell",44L,false),"same action reused");
        verify(!b.matches("tekk","text",44L,false),"changed candidate");
        verify(!b.matches("tek","tell",44L,false),"changed input");
        verify(!b.matches("tekk","tell",45L,false),"changed editor session");
        verify(!b.matches("tekk","tell",44L,true),"changed long-press");
        verify(new ChipActionBinding412("thisis", "this is",1L,true)
                .matches("thisis","this is",1L,true),"phrase candidate");
        System.out.println("ChipActionBinding412Test PASS ("+checks+" checks)");
    }
}

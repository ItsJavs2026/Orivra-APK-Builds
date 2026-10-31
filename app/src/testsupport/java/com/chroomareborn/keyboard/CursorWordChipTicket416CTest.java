package com.chroomareborn.keyboard;

/** Pure-Java regression cases for current-word chip authorization. */
public final class CursorWordChipTicket416CTest {
    private static int checks;
    private static void check(boolean v,String name) {
        checks++;
        if(!v)throw new AssertionError(name);
    }
    private static boolean allow(String a,String b,long oldRev,long liveRev,
            int aStart,int aEnd,int bStart,int bEnd,
            boolean composing,boolean selected,boolean sensitive){
        return CursorWordChipTicket416C.preserve(a,b,oldRev,liveRev,
                aStart,aEnd,bStart,bEnd,composing,selected,sensitive);
    }
    public static void main(String[] args) {
        check(allow("typing","typing",9,9,21,21,21,21,false,false,false),"current");
        check(allow("can't","can't",9,9,21,21,21,21,false,false,false),"apostrophe");
        check(!allow("typing","typin",9,9,21,21,21,21,false,false,false),"word changed");
        check(!allow("typing","typing",9,10,21,21,21,21,false,false,false),"reopen epoch");
        check(!allow("typing","typing",9,9,21,21,22,22,false,false,false),"caret moved");
        check(!allow("typing","typing",9,9,21,21,21,22,false,true,false),"highlight");
        check(!allow("typing","typing",9,9,21,21,21,21,true,false,false),"composing");
        check(!allow("typing","typing",9,9,21,21,21,21,false,false,true),"protected");
        check(!allow("typing","",9,9,21,21,21,21,false,false,false),"idle row");
        check(!allow(null,"typing",9,9,21,21,21,21,false,false,false),"no ticket");
        for(int i=0;i<100;i++){
            check(allow("word","word",i,i,i,i,i,i,false,false,false),"valid frame");
            check(!allow("word","word",i,i+1,i,i,i,i,false,false,false),"stale frame");
        }
        System.out.println("CursorWordChipTicket416CTest: "+checks+" passed");
    }
}

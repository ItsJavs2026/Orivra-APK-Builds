package com.chroomareborn.keyboard;

import java.util.Random;

public final class TypingMotionPolicyTest {
    private static long checks;
    private static void ok(boolean v,String message) {
        checks++;
        if(!v)throw new AssertionError(message);
    }
    public static void main(String[] args) {
        ok(TypingMotionPolicy.scale(true,true)<1f,"press compresses visually");
        ok(TypingMotionPolicy.scale(false,true)==1f,"release returns exactly");
        ok(TypingMotionPolicy.scale(true,false)==1f,"reduced motion no scale");
        ok(TypingMotionPolicy.duration(true,false)==0,"reduced motion no duration");
        ok(TypingMotionPolicy.DOWN_MS<=40,"press must feel immediate");
        ok(TypingMotionPolicy.DOWN_SCALE>=.97f,
                "subtle compression must not feel sticky");
        ok(TypingMotionPolicy.RELEASE_MS<=65,
                "release animation must not trail the thumb");
        Random random=new Random(2942026L);
        for(int i=0;i<100000;i++) {
            boolean pressed=random.nextBoolean();
            boolean enabled=random.nextBoolean();
            float scale=TypingMotionPolicy.scale(pressed,enabled);
            long duration=TypingMotionPolicy.duration(pressed,enabled);
            ok(scale>0.90f && scale<=1f,"scale does not distort key");
            ok(duration>=0&&duration<=120,"animation nonblocking");
            ok(enabled||duration==0,"disabled motion must be instant");
            ok(!enabled||pressed||scale==1f,"released key has true size");
        }
        System.out.println("TypingMotionPolicyTest passed: 100k seeded "+
                "motion states, "+checks+" assertions");
    }
}

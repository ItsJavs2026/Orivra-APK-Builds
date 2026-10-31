package com.chroomareborn.keyboard;

import java.util.Locale;
import java.util.Random;

/** 200k+ pure Java randomized learning guards; no Android or user data. */
public final class FastStoredLearning395Research {
    private static long checks=0;
    private static void ok(boolean yes,String label) {
        checks++;if(!yes)throw new AssertionError(label);
    }
    static String sample(Random r){
        String alphabet="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ'' ’!- 123";
        int n=r.nextInt(30);
        StringBuilder word=new StringBuilder(n);
        for(int i=0;i<n;i++)word.append(alphabet.charAt(r.nextInt(alphabet.length())));
        return word.toString();
    }
    public static void main(String[] args){
        Random r=new Random(39520261031L);
        String[] prefixes={"a","th","con","we'","i","you","now","cat","let","the"};
        for(int i=0;i<210000;i++){
            String w=i%7==0?"the"+sample(r):sample(r);
            String prefix=prefixes[i%prefixes.length];
            int uses=1+r.nextInt(15);
            if(FastStoredLearningPolicy395.definitelyNotPrefix(prefix,w))
                ok(!AdaptivePrefixPolicy328.eligible(prefix,w,uses),
                        "Never omit originally eligible frequency key");
            String target=i%13==0?"we're":w;
            ok(FastStoredLearningPolicy395.canonicalContextTarget(target)==
                    target.equals(AcceptedContextPolicy319.acceptedWord(target)),
                    "Fast stored-context validator must match strict reference");
        }
        for(String word:new String[]{
                "we're","a'a","don't","the","i","I'm","a'","'ab",
                "ab''cd","ab’cd","re née","abc123","aa","abcdefghijklmnopqrstuvwxyz",
                "abcdefghijklmnopqrstuvwxyza","hello "
        }){
            ok(FastStoredLearningPolicy395.canonicalContextTarget(word)==
                    word.equals(AcceptedContextPolicy319.acceptedWord(word)),
                    "Known contraction and invalid-tail parity");
        }
        System.out.println("PASS FastStoredLearning395 "+checks+
                " assertions over 210000 synthetic frequency/context keys");
    }
}

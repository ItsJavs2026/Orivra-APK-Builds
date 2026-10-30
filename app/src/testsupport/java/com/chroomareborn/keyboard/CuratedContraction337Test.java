package com.chroomareborn.keyboard;

/** Trusted public contractions must work across learning and backup guards. */
public final class CuratedContraction337Test {
    private static int checks;
    static void yes(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
    public static void main(String[] args) {
        for(String form:new String[]{"can't","ain't","that's","we're",
                "don't","she's","i'm","i've","y'all","o'clock"}) {
            yes(EnglishApostrophePolicy.publicContraction(form),
                    "known exact contraction "+form);
            yes(PredictionLite.publicWordForContext(form),
                    "public learner accepts curated "+form);
        }
        for(String unapproved:new String[]{"javier's","someone's",
                "bank's","customer's","mary's","joe's","o'brien",
                "account@example.com","that's123","can't stop",
                "xx'yy","cant"}) {
            yes(!EnglishApostrophePolicy.publicContraction(unapproved),
                    "no uncurated possessives/numbers "+unapproved);
        }
        yes(!EnglishApostrophePolicy.publicContraction("Can't"),
                "learning uses canonical lowercase");
        yes(!EnglishApostrophePolicy.publicContraction(" that's"),
                "leading whitespace does not become a stored form");
        yes(!PredictionLite.publicWordForContext("javier's"),
                "public lexicon still rejects personal names");
        yes(PredictionLite.publicWordForContext("continue"),
                "known everyday words remain eligible");
        yes(PredictionLite.publicWordForContext("tell"),
                "previous tell correction vocabulary retained");
        System.out.println("PASS CuratedContraction337Test "+checks+" checks");
    }
}

package com.chroomareborn.keyboard;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public final class NextWordSeedModelTest {
    private static int checks;
    private static void check(boolean condition,String description) {
        checks++;
        if(!condition)throw new AssertionError(description);
    }
    public static void main(String[] args) throws Exception {
        String path="app/src/main/assets/lm/swiftfloris_en_bigrams.tsv";
        NextWordSeedModel seed=NextWordSeedModel.fromTsv(
                Files.newBufferedReader(Paths.get(path),StandardCharsets.UTF_8));
        check(seed.entries()>150,"shipped seed has meaningful coverage");
        check(seed.contexts()>20,"shipped seed covers contexts");
        check(seed.suggest("I",3).get(0).equals("am"),"common start i am");
        check(seed.suggest("We went to",3).get(0).equals("be"),
                "bigram fallback from last word");
        check(seed.suggest("in",3).get(0).equals("the"),
                "correct basic phrase");
        check(seed.suggest("unusedword",3).isEmpty(),"unknown word cold start");
        check(seed.suggest("the",1).size()==1,"max suggestions honored");
        check(seed.suggest("the",0).isEmpty(),"zero suggestions");
        // User-device regression: after selecting "am" and "testing", the
        // suggestion strip must not go blank just because the seed ends.
        check(seed.suggest("I am testing ",3).size()>=3,
                "continued suggestions after I am testing");
        check(seed.suggest("I am testing ",3).contains("the"),
                "testing has a useful object continuation");
        check(seed.suggest("I am testing this ",3).size()>0,
                "third-hop continuation after this");
        check(seed.suggest("I am working ",3).size()>0,
                "common gerund continuation");
        check(seed.suggest("I am waiting ",3).size()>0,
                "waiting continuation");
        check(seed.suggest("I am using ",3).size()>0,
                "using continuation");
        check(seed.suggest("zzunknownword",3).isEmpty(),
                "unknown context must not invent arbitrary suggestions");
        String fixture="Hello\\tworld\\t0.4\\n"
                +"hello\\tthere\\t0.8\\n"
                +"hello\\tworld\\t0.9\\n"
                +"hello\\tbad\\tNaN\\n"
                +"hello\\tnegative\\t-1\\n"
                +"hello\\tinvalid\\t1.3\\n"
                +"malformed\\n";
        // Convert escaped fixture separators into real tabs/newlines.
        fixture=fixture.replace("\\t","\t").replace("\\n","\n");
        NextWordSeedModel parsed=NextWordSeedModel.fromTsv(new StringReader(fixture));
        List<String> results=parsed.suggest("HELLO",3);
        check(results.size()==2 && results.get(0).equals("world")
                && results.get(1).equals("there"),
                "duplicate keeps strongest value; invalid rows rejected");
        check(parsed.entries()==2,"deduplicated entry count");
        System.out.println("NEXT_WORD_SEED_MODEL checks="+checks+" passed"
                +" entries="+seed.entries()+" contexts="+seed.contexts());
    }
}

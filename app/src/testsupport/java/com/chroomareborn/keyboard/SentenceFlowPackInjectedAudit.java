package com.chroomareborn.keyboard;

import java.io.BufferedReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * One JVM, one predictor variant, one benchmark suite. Reflective injection
 * keeps this same compiled harness compatible with untouched Candidate 300.
 * The only input is an immutable licensed wordlist read in CI; user text is
 * never inspected, stored or transmitted.
 */
public final class SentenceFlowPackInjectedAudit {
    private static long usedMemory() {
        Runtime r=Runtime.getRuntime();
        return r.totalMemory()-r.freeMemory();
    }
    public static void main(String[] args)throws Exception{
        if(args.length!=2)
            throw new IllegalArgumentException("Usage: <none|ESDB_file> <suite>");
        PredictionLite.suggestions("hello"); // separate warm class initialization
        if(!"none".equals(args[0])) {
            long start=System.nanoTime();
            System.gc();
            long before=usedMemory();
            Class<?> packClass=Class.forName(
                    "com.chroomareborn.keyboard.ExactOnlyLanguagePack");
            Method fromReader=packClass.getDeclaredMethod(
                    "fromReader",java.io.Reader.class);
            fromReader.setAccessible(true);
            Object pack;
            try(BufferedReader reader=Files.newBufferedReader(
                    Path.of(args[0]),StandardCharsets.UTF_8)) {
                pack=fromReader.invoke(null,reader);
            }
            Method size=packClass.getDeclaredMethod("size");
            size.setAccessible(true);
            int words=(Integer)size.invoke(pack);
            if(words<20000 || words>125000)
                throw new AssertionError("Unexpected exact-tier coverage "+words);
            Method setter=PredictionLite.class.getDeclaredMethod(
                    "setResearchExactOnlyPack",packClass);
            setter.setAccessible(true);
            setter.invoke(null,pack);
            long elapsed=(System.nanoTime()-start)/1_000_000;
            long heap=usedMemory()-before;
            System.out.printf(Locale.ROOT,
                    "RESEARCH_PACK words=%d loadMs=%d heapDeltaMiB=%.2f%n",
                    words,elapsed,heap/(1024.0*1024));
        } else {
            System.out.println("RESEARCH_PACK none (unchanged Candidate 300)");
        }
        switch(args[1]) {
            case "accuracy":
                SentenceFlowAccuracyBenchmark.main(new String[0]);break;
            case "holdout":
                SentenceFlowHoldoutBenchmark.main(new String[0]);break;
            case "independent":
                SentenceFlowIndependentAudit.main(new String[0]);break;
            case "blind":
                SentenceFlowBlind297Audit.main(new String[0]);break;
            default:throw new IllegalArgumentException("Unknown suite "+args[1]);
        }
    }
}

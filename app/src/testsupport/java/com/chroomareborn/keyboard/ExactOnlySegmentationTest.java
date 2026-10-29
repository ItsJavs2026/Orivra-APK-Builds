package com.chroomareborn.keyboard;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/** Synthetic deterministic guards; never reads user note contents. */
public final class ExactOnlySegmentationTest {
    private static int assertions;
    private static void expect(boolean good,String why){
        assertions++;
        if(!good)throw new AssertionError(why);
    }
    public static void main(String[] args)throws Exception{
        ExactOnlyLanguagePack pack=ExactOnlyLanguagePack.fromReader(
                new StringReader("sunrise\nheadphones\nnotes\npackage\n"));
        Map<String,Integer> core=new HashMap<>();
        for(String word:"the train arrived before sun ride your head phones sound very clear my package earlier today".split(" "))
            core.put(word,24);
        expect(pack.contains("sunrise"),"pack exact recognition");
        expect(!pack.contains("sun"),"pack must not invent lexemes");
        expect(!pack.contains("Sunrise"),"pack API accepts lower-case ASCII only");
        String joined="thetrainarrivedbeforesunrise";
        String old="the train arrived before sun ride";
        String corrected=ExactOnlySegmentation.propose(joined,old,core,pack);
        expect("the train arrived before sunrise".equals(corrected),
                "valid all-exact long word should improve fragmented text");
        expect(ExactOnlySegmentation.propose(joined,"the train arrived before sunrise",core,pack)==null,
                "do not replace an already good sentence");
        expect(ExactOnlySegmentation.propose("thepaxkaxearrivedearliertoday",
                "the package arrived earlier today",core,pack)==null,
                "a multiple-typo sentence already repaired must stay");
        expect(ExactOnlySegmentation.propose("website.com",
                "website com",core,pack)==null,
                "do not handle punctuation or website tokens");
        expect(ExactOnlySegmentation.propose("hello",old,core,pack)==null,
                "short standalone words never segmented");
        expect(ExactOnlySegmentation.propose(joined,old,core,null)==null,
                "missing pack leaves baseline unchanged");
        expect(ExactOnlySegmentation.propose(joined,old,core,pack)!=null,
                "candidate only: explicit tap remains required");
        System.out.println("PASS: ExactOnlySegmentationTest "+assertions+" assertions");
    }
}

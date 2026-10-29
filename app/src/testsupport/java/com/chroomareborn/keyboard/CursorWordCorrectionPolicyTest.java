package com.chroomareborn.keyboard;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class CursorWordCorrectionPolicyTest {
    private static int tests;
    private static void expect(boolean truth,String name) {
        tests++;
        if(!truth)throw new AssertionError(name);
    }
    private static void plan(String before,String after,String expected,
                             String replacement,int left,int right) {
        CursorWordCorrectionPolicy.Plan result=
                CursorWordCorrectionPolicy.plan(before,after,expected,replacement,
                        false,false);
        expect(result!=null,name(before,after,"plan exists"));
        expect(result.lettersBefore==left,name(before,after,"left count"));
        expect(result.lettersAfter==right,name(before,after,"right count"));
        expect(result.replacement.equals(replacement),
                name(before,after,"exact replacement; no added space"));
    }
    private static String name(String before,String after,String what) {
        return "Caret after '"+before+"' before '"+after+"': "+what;
    }
    private static void rejected(String before,String after,String expected,
                                 String replacement,String reason) {
        expect(CursorWordCorrectionPolicy.plan(before,after,expected,
                replacement,false,false)==null,reason);
    }
    public static void main(String[] args) {
        plan("i typed a wro","ng sentence","wrong","wring",3,2);
        plan("please fix ","wrng now","wrng","wrong",0,4);
        plan("this wrng","!","wrng","wrong",4,0);
        plan("wrng","","wrng","wrong",4,0);
        plan("café"," tomorrow","café","cafe",4,0);
        plan("dont"," stop","dont","don't",4,0);
        plan("Wro","ng sentence","Wrong","Wrung",3,2);
        rejected("some wrong"," text","different","right","stale word refused");
        rejected("some word"," next","word","two words","sentence suggestion refused");
        rejected("some word"," next","word","word\nattack","multiline replacement refused");
        rejected("some word"," next","word","word!","punctuation injection refused");
        rejected("wrong"," here","wrong","wrong","no redundant edit");
        rejected("at the "," end","missing","correct","no unrelated text deletion");
        String longPart="aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        rejected(longPart,"","a".repeat(64),"b".repeat(64),
                "truncated 64-letter context refused");
        expect(CursorWordCorrectionPolicy.plan("secret"," field","secret",
                "secrets",false,true)==null,"private field refused");
        expect(CursorWordCorrectionPolicy.plan("some wro","ng","wrong",
                "wring",true,false)==null,"selection refused");
        expect(CursorWordCorrectionPolicy.plan(null,"wrng","wrng",
                "wrong",false,false)==null,"null editor snapshot refused");
        List<String> safe=CursorWordCorrectionPolicy.safeOptions("wrng",
                Arrays.asList("wrng","wrong","wrong","WroNG",
                        "wrong again","w ro ng","word!","","wring"));
        expect(safe.size()==3 && safe.get(0).equals("wrng") &&
                safe.get(1).equals("wrong") && safe.get(2).equals("wring"),
                "single-word suggestions only, deduped, original visible");
        List<String> untouched=CursorWordCorrectionPolicy.safeOptions(
                "wrng",Collections.emptyList());
        expect(untouched.size()==1 && untouched.get(0).equals("wrng"),
                "the original word remains tappable");
        expect(CursorWordCorrectionPolicy.safeOptions(null,safe).isEmpty(),
                "no suggestions without an editor word");
        // Samsung Build 269 regression: tapping a mid-sentence correction
        // must not inherit stale auto-Shift and create "Correct" from corect.
        expect(CursorWordCorrectionPolicy.matchCase("corect","Correct")
                .equals("correct"),"lowercase typo stays lowercase");
        expect(CursorWordCorrectionPolicy.matchCase("Corect","correct")
                .equals("Correct"),"sentence-start initial case preserved");
        expect(CursorWordCorrectionPolicy.matchCase("CORECT","correct")
                .equals("CORRECT"),"all-caps original stays all caps");
        expect(CursorWordCorrectionPolicy.matchCase("cOrEcT","correct")
                .equals("correct"),"mixed-case original does not force Shift");
        expect(CursorWordCorrectionPolicy.matchCase("iPhne","iPhone")
                .equals("iPhone"),"mixed-case proper name preserves canonical spelling");
        expect(CursorWordCorrectionPolicy.matchCase("coréct","CORRECT")
                .equals("correct"),"accented lowercase original remains lowercase");
        expect(CursorWordCorrectionPolicy.matchCase("Érreur","erreur")
                .equals("Erreur"),"Unicode initial upper title case");
        expect(CursorWordCorrectionPolicy.matchCase("ÉRREUR","erreur")
                .equals("ERREUR"),"Unicode all-caps word");
        expect(CursorWordCorrectionPolicy.matchCase("coRect","Correct")
                .equals("Correct"),"mixed-case word does not blindly lowercase canonical choice");
        expect(CursorWordCorrectionPolicy.matchCase("", "Hello").equals("Hello"),
                "empty original keeps suggestion");
        expect(CursorWordCorrectionPolicy.matchCase("corect", null)==null,
                "missing candidate ignored");
        // Verify case-preserving plan as actually submitted at tap time.
        CursorWordCorrectionPolicy.Plan lowercase=CursorWordCorrectionPolicy.plan(
                "I need to cor","ect this sentence.","corect",
                CursorWordCorrectionPolicy.matchCase("corect","Correct"),
                false,false);
        expect(lowercase!=null && lowercase.replacement.equals("correct") &&
                lowercase.lettersBefore==3 && lowercase.lettersAfter==3,
                "tap changes ONLY corect, not surrounding text");
        CursorWordCorrectionPolicy.Plan title=CursorWordCorrectionPolicy.plan(
                "Core","ct this sentence.","Corect",
                CursorWordCorrectionPolicy.matchCase("Corect","correct"),
                false,false);
        expect(title!=null && title.replacement.equals("Correct"),
                "uppercase original retained in exact edit plan");

        System.out.println("CURSOR_WORD_CORRECTION checks="+tests+" passed");
    }
}

package com.chroomareborn.keyboard;

import java.util.Arrays;
import java.util.List;

/** Frozen synthetic contraction and privacy policy fixture, never device text. */
public final class EnglishApostropheAndLearningV2Test {
    static int checks;
    static void ok(boolean value,String desc){
        checks++;if(!value)throw new AssertionError(desc);
    }
    public static void main(String[] args){
        for(String[] pair:new String[][]{
                {"can't","can't"},{"can’t","can't"},
                {"Ain't","ain't"},{"AIN’T","ain't"},
                {"I'm","i'm"},{"we’re","we're"},
                {"y’all","y'all"},{"O’Connor","o'connor"},
                {"D'Arcy","d'arcy"}
        })ok(pair[1].equals(EnglishApostrophePolicy.canonical(pair[0])),
                "straight/curly apostrophe matching");
        for(String invalid:new String[]{
                "", " ", "'", "cant'", "'cant", "can''t",
                "a@b.com","http://a","test.com","I’m here",
                "1can't","can’t?","can't.","-can't","a-b",
                "5'10","can't🙂","can't\na","abc_def",
                "verylongword".repeat(5)
        })ok(EnglishApostrophePolicy.canonical(invalid)==null,
                "reject invalid word / privacy sentinel: "+invalid);
        ok("can'".equals(EnglishApostrophePolicy.prefix("can’")),
                "allow a composing trailing apostrophe");
        ok(EnglishApostrophePolicy.prefix("can''")==null,
                "reject multiple trailing apostrophes");
        ok(EnglishApostrophePolicy.canAppendApostrophe("can","'"),
                "apostrophe keeps composition open");
        ok(EnglishApostrophePolicy.canAppendApostrophe("ain","’"),
                "curly apostrophe keeps composition open");
        for(String invalid:new String[]{"can'","", "cannot type","12","can't"}) {
            ok(!EnglishApostrophePolicy.canAppendApostrophe(invalid,"'"),
                    "prevent repeated/unsafe composing punctuation");
        }
        for(String[] pair:new String[][] {
                {"can'","can't"},{"ain'","ain't"},
                {"don'","don't"},{"we'r","we're"},
                {"couldn'","couldn't"}
        })ok(EnglishApostrophePolicy.suggestions(pair[0]).contains(pair[1]),
                "complete partial contraction "+pair[0]);
        ok(EnglishApostrophePolicy.suggestions("can't").get(0).equals("can't"),
                "preserve exact user-entered apostrophized word");
        ok(EnglishApostrophePolicy.suggestions("can’t").get(0).equals("can’t"),
                "preserve exact curly user input");
        for(String word:new String[]{"its","well","shell","ill","id","cantilever"}) {
            ok(EnglishApostrophePolicy.suggestions(word).isEmpty(),
                    "ambiguous proper spelling must not convert automatically");
        }
        for(String[] pair:new String[][]{
                {"cant","can't"},{"aint","ain't"},{"wont","won't"},
                {"im","I'm"},{"youre","you're"},{"dont","don't"},
                {"theyre","they're"},{"shouldnt","shouldn't"}
        })ok(EnglishApostrophePolicy.suggestions(pair[0]).contains(pair[1]),
                "missing apostrophe must be a tappable suggestion");
        ok(EnglishApostrophePolicy.merge("cant",List.of("cant","can","can't"))
                .get(0).equals("cant"),"do not auto-replace valid literal cant");
        ok(EnglishApostrophePolicy.merge("were",List.of("were"))
                .equals(List.of("were","we're")),
                "preserve valid were and OFFER we're separately, never autocorrect");
        ok(EnglishApostrophePolicy.suggestions("hows").contains("how's"),
                "hows offers literal how's only on tap");
        ok(EnglishApostrophePolicy.suggestions("Were").contains("We're"),
                "sentence-case Were offers We're");
        ok(EnglishApostrophePolicy.suggestions("HOWS").contains("HOW'S"),
                "upper-case HOWS offers HOW'S");
        ok(EnglishApostrophePolicy.hasDistinctSuggestion("were"),
                "were has a distinct optional candidate");
        ok(EnglishApostrophePolicy.hasDistinctSuggestion("hows"),
                "hows has a distinct optional candidate");
        ok(!EnglishApostrophePolicy.hasDistinctSuggestion("we're"),
                "completed apostrophized word needs no missing-mark preview");
        ok(!EnglishApostrophePolicy.hasDistinctSuggestion("a@b.com"),
                "email is never processed as contraction");
        ok(EnglishApostrophePolicy.merge("can’t",List.of()).get(0).equals("can’t"),
                "curly word survives empty base predictor");
        ok(EnglishApostrophePolicy.merge("aint",List.of("ant","aint"))
                .contains("ain't"),"ain't always selectable");
        ok(EnglishApostrophePolicy.merge("can'",List.of())
                .contains("can't"),"incomplete word receives completion");

        LearningV2Policy.Pair cant=LearningV2Policy.extract("CANT","can't");
        ok(cant!=null && cant.typed.equals("cant") &&
                cant.accepted.equals("can't"),"learn explicit cant->can't correction");
        LearningV2Policy.Pair ai=LearningV2Policy.extract("AINT","ain’t");
        ok(ai!=null && ai.accepted.equals("ain't"),"learn curly apostrophe in output");
        LearningV2Policy.Pair sentence=LearningV2Policy.extract(
                "the package cant arrive","the package can't arrive");
        ok(sentence!=null && sentence.typed.equals("cant") &&
                sentence.accepted.equals("can't"),
                "learn exactly one word pair, not whole sentence");
        LearningV2Policy.Pair withPronoun=LearningV2Policy.extract(
                "I cant go","I can't go");
        ok(withPronoun!=null && withPronoun.typed.equals("cant") &&
                withPronoun.accepted.equals("can't"),
                "unchanged single-letter pronoun must permit one-word learning");
        LearningV2Policy.Pair withArticle=LearningV2Policy.extract(
                "a cant moment","a can't moment");
        ok(withArticle!=null && withArticle.typed.equals("cant"),
                "unchanged article must permit one-word learning");
        ok(LearningV2Policy.extract("I go","A go")==null,
                "never learn a one-letter correction");

        for(String[] input:new String[][]{
                {"thisisjoined","this is joined"},
                {"can you type","could you speak"},
                {"hello world","good bye"},
                {"password=secret","password=fixed"},
                {"send a@b.com","send a@c.com"},
                {"hello world","hello world"},
                {"we can't go","we can't go"},
                {"this has TWO typos","this had two types"}
        })ok(LearningV2Policy.extract(input[0],input[1])==null,
                "never persist joined phrases, PII strings or multiple word edits");
        ok(!LearningV2Policy.reinforced(1),"first explicit correction not strong enough");
        ok(LearningV2Policy.reinforced(2),"second acceptance reinforces local pair");
        ok(!LearningV2Policy.reinforced(0),"zero acceptances never promote");

        ok(PersonalDictionaryPolicy.canonical("can't").equals("can't"),
                "manual dictionary supports apostrophe");
        ok(PersonalDictionaryPolicy.canonical("can’t").equals("can't"),
                "manual dictionary dedupes curly apostrophe");
        ok(PersonalDictionaryPolicy.merge("can",List.of("can"),
                List.of("can't")).contains("can't"),
                "manual saved contraction available on normal prefix");
        ok(CursorWordCorrectionPolicy.plan("I can’t","","can’t","can't",
                false,false)!=null,"cursor word may replace curly with straight");
        ok(CursorWordCorrectionPolicy.plan("I can’t","","can’t","can't",
                false,true)==null,"private field cannot correct apostrophe");
        ok(CursorWordCorrectionPolicy.plan("I can't","","can't","can",
                false,false)!=null,"apostrophe-token bounded correction");
        ok(CursorWordCorrectionPolicy.plan("I can’t","","cant","can't",
                false,false)==null,"stale expected word must not delete user text");
        System.out.println("PASS: EnglishApostropheAndLearningV2Test "+checks+
                " assertions");
    }
}
